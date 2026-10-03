package com.mrfuzzihead.vinery.core.block;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDirectional;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import com.mrfuzzihead.vinery.client.render.BoxRenderer.Box;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Shared behaviour for the three wine rack sizes.
 *
 * <p>
 * The three racks differ only in geometry and in how many bottles they hold — the big one is a 3x3
 * grid of cubbies, the other two are single columns. Facing, drops, collision, icon registration and
 * render-id plumbing are identical, so they live here rather than being copied per rack: with ten
 * wood variants across three shapes, a copy-pasted {@code onBlockPlacedBy} is a bug waiting to
 * happen (see {@link #onBlockPlacedBy} for the yaw arithmetic, which is easy to get subtly wrong).
 *
 * <p>
 * Facing is raw metadata — bits 0-1 hold the facing, matching 1.7.10's {@link BlockDirectional}
 * convention. 1.7.10 has no block-property system (BACKPORT_PLAN.md section 4.2).
 */
public abstract class WineRackBlock extends BlockDirectional implements VineryCheckableBlock {

    /** Cabinet side texture — the frame, drawn on every face of the posts and shelves. */
    private final String sideTexture;

    /** Cabinet top texture — the flat surfaces (top panel, shelf tops). */
    private final String topTexture;

    /**
     * Render id for the box renderer.
     *
     * <p>
     * 1.7.10 does not use -1 for "custom handler": RenderBlocks#renderBlockByRenderType returns
     * false immediately for -1 and never reaches Forge's handler lookup, so the block draws nothing.
     * A real id from RenderingRegistry#getNextAvailableRenderId is required, and it can only be
     * allocated on the client — hence the setter rather than a constructor argument.
     *
     * <p>
     * Every rack shares one id. Forge dispatches on {@link #getRenderType()} but hands the actual
     * {@link Block} to the handler, so a single registered handler serves all of them and the
     * geometry is chosen per block via {@link #geometry()}.
     */
    private int renderId = -1;

    protected WineRackBlock(String sideTexture, String topTexture) {
        super(Material.wood);
        this.sideTexture = sideTexture;
        this.topTexture = topTexture;
        // Block#getIcon returns blockIcon, which is what the ItemBlock uses for its inventory icon.
        // The box renderer draws from the per-face table below, but the item form and the block
        // particle still need a single texture, or the block renders as nothing at all.
        setBlockTextureName(sideTexture);
        setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
        setHardness(2.0F);
        setResistance(3.0F);
        setStepSound(soundTypeWood);
    }

    /** The boxes this rack draws, in 0-1 block space. */
    public abstract Box[] geometry();

    @Override
    public String textureNameForTest() {
        return sideTexture;
    }

    /**
     * The rack uses two textures — cabinet side for the frame, cabinet top for the shelves — so the
     * per-face icons are held here. {@code Block.getIcon} can only return one icon for all sides,
     * which is why this block owns the mapping the box renderer consumes.
     *
     * <p>
     * A rack model that uses a third texture (only {@code acacia}, whose bottom-panel underside
     * points at {@code acacia_drawer_bottom}) loses it here: the vanilla cube renderer draws one
     * texture per box, so a single face cannot differ from the other five. It is the underside of
     * the bottom panel, visible only when looking up at the rack from below.
     */
    @SideOnly(Side.CLIENT)
    @Override
    public void registerBlockIcons(IIconRegister iconRegister) {
        IIcon side = iconRegister.registerIcon(sideTexture);
        IIcon top = iconRegister.registerIcon(topTexture);

        // Block#registerBlockIcons normally assigns blockIcon here, and this override replaces it
        // wholesale — so blockIcon has to be set by hand. Block#getIcon and
        // getBlockTextureFromSide both resolve through blockIcon, which is what the vanilla cube
        // renderer (and ItemBlock's own icon) use. Without this the block draws with a null icon:
        // invisible in the world, missing-texture square in the inventory.
        this.blockIcon = side;

        // BoxRenderer face order: up, down, north, south, west, east.
        this.faceIcons = new IIcon[] { top, top, side, side, side, side };
    }

    @SideOnly(Side.CLIENT)
    private IIcon[] faceIcons;

    /** Per-face icons in BoxRenderer's order, or null on a server. */
    @SideOnly(Side.CLIENT)
    public IIcon[] faceIcons() {
        return faceIcons;
    }

    /**
     * Allocates this block's custom render id. Client-side only; called from the client proxy.
     *
     * @see #renderId
     */
    public void setRenderId(int renderId) {
        this.renderId = renderId;
    }

    /**
     * Rendered through the custom box renderer, not the standard cube pipeline. Returns -1 on a
     * dedicated server, where nothing is rendered anyway.
     */
    @Override
    public int getRenderType() {
        return renderId;
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    /** Solid: players and mobs walk into the rack rather than through it. */
    @Override
    public boolean getBlocksMovement(IBlockAccess world, int x, int y, int z) {
        return true;
    }

    /**
     * Collision deliberately uses the inherited implementation.
     *
     * <p>
     * Block#getCollisionBoundingBoxFromPool already builds the box in world coordinates
     * (x + minX, y + minY, z + minZ ...), and addCollisionBoxesToList compares that against the
     * entity's mask without offsetting again. An override returning an unoffset 0,0,0-1,1,1 box puts
     * the collision volume at the world origin, which is why the rack had none.
     */

    /** Racks are placed against whatever they were clicked on, like doors and furnaces. */
    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase placer, ItemStack stack) {
        // 1.7.10 has no Facing enum, so the direction is derived from yaw the way BlockFurnace does.
        // MathHelper.floor_double, exactly as BlockFurnace does. A plain (int) cast truncates
        // toward zero, and player yaw is routinely negative (-177.6, -360.45 and so on), so
        // truncation silently yields -1 where flooring yields -2 — which after & 3 points the rack
        // a quarter turn off for roughly half of all angles.
        //
        // Server-side only: the client also gets this callback, but with a yaw that has not been
        // normalised yet, so writing metadata there produces a wrong value for a frame until the
        // server's authoritative copy arrives.
        if (!world.isRemote) {
            int facing = MathHelper.floor_double(placer.rotationYaw * 4.0F / 360.0F + 0.5D) & 3;
            world.setBlockMetadataWithNotify(x, y, z, facing, 2);
        }
    }

    @Override
    public int damageDropped(int meta) {
        return 0;
    }

    @Override
    public Item getItemDropped(int meta, Random random, int fortune) {
        return Item.getItemFromBlock(this);
    }

    /** The whole block, ignoring metadata: the facing does not change the drop. */
    @Override
    public int quantityDropped(Random random) {
        return 1;
    }

    @Override
    public boolean canPlaceBlockAt(World world, int x, int y, int z) {
        return world.getBlock(x, y - 1, z)
            .isOpaqueCube();
    }
}
