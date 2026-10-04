package com.mrfuzzihead.vinery.core.block;

import java.util.List;

import net.minecraft.block.BlockLeaves;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Leaves, parameterised by the four textures that drive the decay stages.
 *
 * <p>
 * Extends {@link BlockLeaves} directly and implements the same three members vanilla's
 * {@code BlockOldLeaf}/{@code BlockNewLeaf} do: {@link #func_150125_e()} names the textures,
 * {@link #registerBlockIcons} resolves them into {@code field_150129_M}, and {@link #getIcon} picks
 * between the two variants by metadata.
 *
 * <p>
 * 1.7.10 metadata carries the variant: 0 fast (decaying), 1 slow (permanent). The icon array is
 * indexed by {@code meta & 3}, so the four stage textures the 1.21 models use map straight onto the
 * decay stages vanilla already tracks, giving progressive wear for free.
 */
public class FoliageBlock extends BlockLeaves {

    private final String[] textures;

    public FoliageBlock(String stage0, String stage1, String stage2, String stage3) {
        super();
        this.textures = new String[] { stage0, stage1, stage2, stage3 };
        setStepSound(soundTypeGrass);
        setHardness(0.2F);
        setLightOpacity(1);
    }

    @Override
    public String[] func_150125_e() {
        return textures;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void registerBlockIcons(IIconRegister iconRegister) {
        this.field_150129_M[0] = new IIcon[textures.length];
        this.field_150129_M[1] = new IIcon[textures.length];

        // Follows Et Futurum's BaseLeaves, the reference for modern-style leaves on 1.7.10: register
        // BOTH the cut-out art and an "_opaque" sibling, then pick between them in getIcon.
        //
        // Both are needed because 1.7.10 has no alpha testing at all -- grepping every patched
        // Minecraft class for "alphaFunc" or "AlphaTest" returns nothing -- so a cut-out texture on
        // a cube is drawn with GL blending and you see straight through it. Vanilla leaves dodge
        // this by shipping an opaque texture, and so does BaseLeaves.
        for (int stage = 0; stage < textures.length; stage++) {
            this.field_150129_M[0][stage] = iconRegister.registerIcon(textures[stage]);
            this.field_150129_M[1][stage] = iconRegister.registerIcon(textures[stage] + "_opaque");
        }

        // Block#registerBlockIcons assigns blockIcon, and this override replaces it wholesale, so
        // blockIcon has to be set by hand. Anything resolving through blockIcon -- the ItemBlock's
        // icon, the break-particle, Block#getBlockTextureName -- comes back null without it, and a
        // null icon makes RenderBlocks skip the face outright rather than drawing it black.
        //
        // The opaque variant, because the item icon is drawn in a flat GUI with no graphics setting
        // consulted, and a cut-out sprite there would be a rectangle of holes.
        this.blockIcon = this.field_150129_M[1][0];
    }

    @SideOnly(Side.CLIENT)
    @Override
    public IIcon getIcon(int side, int meta) {
        // Leaves carry their decay distance in metadata, which runs 0-7, and vanilla indexes the
        // stage table with meta & 3 — so the index reaches 3 even for a table with only three
        // stages. Clamping keeps the last stage in view instead of throwing
        // ArrayIndexOutOfBoundsException from inside the renderer.
        IIcon[] stage = this.field_150129_M[variantIndex()];
        int index = meta & 3;
        return stage[index < stage.length ? index : stage.length - 1];
    }

    /**
     * Which texture set to draw: 1 for the opaque variant, 0 for the cut-out art.
     *
     * <p>
     * Et Futurum derives this from {@code isOpaqueCube()}, which vanilla drives via
     * {@code setGraphicsLevel}. That does not work for us: RenderGlobal calls {@code setGraphicsLevel}
     * on exactly two hardcoded blocks, {@code Blocks.leaves} and {@code Blocks.leaves2}, so a modded
     * leaves block never receives the update and sits permanently at the constructor default
     * (non-opaque, i.e. cut-out) — see-through again.
     *
     * <p>
     * So the setting is read straight from GameSettings. This stays inside a
     * {@code @SideOnly(Side.CLIENT)} method on purpose: {@code isOpaqueCube()} is called from common
     * code, and touching {@code Minecraft} there would throw {@code NoClassDefFoundError} on a
     * dedicated server.
     */
    @SideOnly(Side.CLIENT)
    private int variantIndex() {
        // Index 1 holds the generated "_opaque" art, index 0 the 1.21 cut-out. Fancy Graphics shows
        // the cut-out, Fast Graphics the opaque variant -- the same choice Et Futurum's BaseLeaves
        // makes, just reached without going through isOpaqueCube().
        //
        // Read straight from GameSettings rather than from the block flag, because the flag is only
        // updated for vanilla's own two leaves blocks (see updateGraphicsLevel).
        return Minecraft.getMinecraft().gameSettings.fancyGraphics ? 0 : 1;
    }

    /**
     * Keeps {@code field_150121_P} in step with the player's graphics setting.
     *
     * <p>
     * {@link BlockLeaves#isOpaqueCube()} returns the <em>negation</em> of {@code field_150121_P},
     * and the constructor stores {@code false} there -- so an un-updated leaves block claims to be
     * an opaque cube. That is the actual cause of leaves being see-through: neighbours cull their
     * own faces against an "opaque" neighbour, so the block above and below lose the faces that
     * would otherwise be visible through the cut-out texture.
     *
     * <p>
     * Vanilla hides this by having {@code RenderGlobal} call {@code setGraphicsLevel} on exactly two
     * hardcoded blocks, {@code Blocks.leaves} and {@code Blocks.leaves2}. A modded leaves block is
     * never in that list, so the update has to be driven from here.
     *
     * <p>
     * The semantics are inverted from the names, which is worth stating once: Fast Graphics sets
     * {@code field_150121_P = false} and leaves report opaque (a solid cube, which is the fast thing
     * to draw); Fancy Graphics sets it true and leaves report non-opaque, which is what lets the
     * cut-out art show sky through its gaps. That matches Et Futurum, whose BaseLeaves delegates
     * both isOpaqueCube() and shouldSideBeRendered() to vanilla's leaves.
     *
     * <p>
     * Called from a client tick handler, so changing the setting in-game takes effect immediately
     * rather than on the next world load.
     */
    @SideOnly(Side.CLIENT)
    public void updateGraphicsLevel(boolean fancyGraphics) {
        if (lastGraphicsLevel == fancyGraphics && graphicsLevelKnown) {
            return;
        }
        setGraphicsLevel(fancyGraphics);
        lastGraphicsLevel = fancyGraphics;
        graphicsLevelKnown = true;
    }

    @SideOnly(Side.CLIENT)
    private boolean lastGraphicsLevel;

    @SideOnly(Side.CLIENT)
    private boolean graphicsLevelKnown;

    /**
     * Always draw every face, including the ones against solid neighbours.
     *
     * <p>
     * {@link BlockLeaves} inherits {@link Block#shouldSideBeRendered}, whose whole body is
     * {@code return !world.getBlock(x, y, z).isOpaqueCube()}. Once {@link #updateGraphicsLevel} has
     * the block reporting non-opaque in Fancy Graphics, that resolves correctly on its own, so this
     * override only guards the Fast Graphics case, where leaves really are opaque cubes and the
     * neighbour's culled face is the correct result. It is kept because the alternative is relying
     * on a flag that vanilla never updates for modded blocks.
     *
     * <p>
     * Et Futurum delegates this straight back to vanilla's leaves, which reproduces vanilla's
     * behaviour once its flag is in step.
     */
    @Override
    public boolean shouldSideBeRendered(IBlockAccess world, int x, int y, int z, int side) {
        return true;
    }

    @Override
    public void getSubBlocks(Item item, CreativeTabs tab, List<ItemStack> list) {
        list.add(new ItemStack(item, 1, 0));
        list.add(new ItemStack(item, 1, 1));
    }
}
