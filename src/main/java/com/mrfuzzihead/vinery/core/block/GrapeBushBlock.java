package com.mrfuzzihead.vinery.core.block;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.BlockBush;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import com.mrfuzzihead.vinery.core.registry.VineryItems;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * A wild grape bush, parameterised by biome and grape colour.
 *
 * <p>
 * The plains, savanna and taiga bushes are crossed sprites in the 1.21 models, which is exactly
 * what 1.7.10's {@link BlockBush} renders. Growth stage is plain metadata 0-3, the same way vanilla
 * wheat and carrots do it — 1.7.10 has no {@code IProperty} block-property system at all (see
 * BACKPORT_PLAN.md section 4.2).
 *
 * <p>
 * {@link #canPlaceBlockOn} is where the 1.21 version used a {@code PlantBlockMixin}; on 1.7.10 it
 * is a plain override that keeps bushes off farmland and onto soil.
 *
 * <p>
 * The jungle counterpart is <b>not</b> this shape: its 1.21 blockstate is a multipart climbing vine
 * with north/east/south/west/up faces plus a {@code sterilized} flag, so it needs its own block and is
 * not ported here.
 */
// verifier: dynamic-textures — stage names are assembled from the biome prefix at runtime.
public class GrapeBushBlock extends BlockBush {

    private static final int MAX_AGE = 3;

    private final String ripeTexture;
    private final boolean redGrapes;

    @SideOnly(Side.CLIENT)
    private IIcon[] icons;

    /**
     * @param texturePrefix one of "plains", "savanna" or "taiga"
     * @param redGrapes     whether the ripe stage is the red or the white variant
     * @param ripeDrop      the item a fully grown bush drops
     */
    public GrapeBushBlock(String texturePrefix, boolean redGrapes) {
        super();
        this.redGrapes = redGrapes;
        this.ripeTexture = "vinery:" + texturePrefix + "_grape_bush_stage3_" + (redGrapes ? "red" : "white");
    }

    /**
     * Resolved lazily rather than injected: blocks are registered before items, so the item fields
     * are still null while this constructor runs. getItemDropped only runs once both are populated.
     */
    private Item ripeDrop() {
        return redGrapes ? VineryItems.RED_GRAPE : VineryItems.WHITE_GRAPE;
    }

    @Override
    protected boolean canPlaceBlockOn(Block soil) {
        // 1.7.10 has no coarse dirt (added in 1.8), so only dirt and grass host bushes.
        return soil == Blocks.dirt || soil == Blocks.grass;
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public int damageDropped(int meta) {
        // Sub-types only, no damage value, matching vanilla wheat.
        return 0;
    }

    /** A ripe bush drops its grapes; anything younger drops seeds. */
    @Override
    public Item getItemDropped(int meta, Random random, int fortune) {
        return (meta & 3) == MAX_AGE ? ripeDrop() : VineryItems.GRAPE_SEEDS;
    }

    @Override
    public void updateTick(World world, int x, int y, int z, Random random) {
        super.updateTick(world, x, y, z, random);

        if (world.getBlockLightValue(x, y + 1, z) < 9) {
            return;
        }

        int age = world.getBlockMetadata(x, y, z) & 3;
        if (age >= MAX_AGE) {
            return;
        }

        // Same cadence as vanilla wheat: a random roll whose chance improves each stage.
        if (random.nextInt((int) (4.0F / (age + 1))) != 0) {
            return;
        }

        world.setBlockMetadataWithNotify(x, y, z, age + 1, 3);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void registerBlockIcons(IIconRegister iconRegister) {
        this.icons = new IIcon[MAX_AGE + 1];
        for (int stage = 0; stage < MAX_AGE; stage++) {
            this.icons[stage] = iconRegister.registerIcon(stageTexture(stage));
        }
        // The first three stages are shared by both colours; only age 3 differs, and that
        // difference is what picking the per-colour block instance selects.
        this.icons[MAX_AGE] = iconRegister.registerIcon(ripeTexture);
    }

    /** Builds the stage 0-2 texture name, which is shared by both colours of one biome. */
    private String stageTexture(int stage) {
        String prefix = ripeTexture.substring("vinery:".length(), ripeTexture.indexOf("_grape_bush_stage3"));
        return "vinery:" + prefix + "_grape_bush_stage" + stage;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public IIcon getIcon(int side, int meta) {
        int age = meta & 3;
        return age >= icons.length ? icons[icons.length - 1] : icons[age];
    }
}
