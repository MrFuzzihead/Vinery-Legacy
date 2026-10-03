package com.mrfuzzihead.vinery.core.block;

import java.util.Random;

import net.minecraft.block.BlockDirectional;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The big wine rack — nine bottle cubbies, taken from the 1.21
 * {@code template_wine_rack_1} model.
 *
 * <p>
 * That model is nine axis-aligned cuboids with no rotations: two side posts, top, bottom and back
 * panels, two vertical dividers and two horizontal shelves. The box list lives in
 * {@link WineRackGeometry#BIG} so the renderer and the collision box agree on one source of truth.
 *
 * <p>
 * Facing is raw metadata — bits 0-1 hold the facing, matching 1.7.10's {@link BlockDirectional}
 * convention. 1.7.10 has no block-property system (BACKPORT_PLAN.md section 4.2).
 *
 * <p>
 * The nine bottle slots are Phase 3 work; this increment brings up the block and its geometry.
 */
public class NineBottleStorageBlock extends BlockDirectional implements VineryCheckableBlock {

    private static final String TEXTURE = "vinery:dark_cherry_cabinet_side";

    /** Index into faceIcons for the cabinet side texture; used by WineRackGeometry. */
    public static final int SIDE_TEXTURE = 2;

    /** Index into faceIcons for the cabinet top texture. */
    public static final int TOP_TEXTURE = 4;

    /**
     * Render id for the box renderer.
     *
     * <p>
     * 1.7.10 does not use -1 for "custom handler": RenderBlocks#renderBlockByRenderType returns
     * false immediately for -1 and never reaches Forge's handler lookup, so the block draws nothing.
     * A real id from RenderingRegistry#getNextAvailableRenderId is required, and it can only be
     * allocated on the client — hence the setter rather than a constructor argument.
     */
    private int renderId = -1;

    @Override
    public String textureNameForTest() {
        return TEXTURE;
    }

    @SideOnly(Side.CLIENT)
    private IIcon[] faceIcons;

    public NineBottleStorageBlock() {
        super(Material.wood);
        // Block#getIcon returns blockIcon, which is what the ItemBlock uses for its inventory icon.
        // The box renderer draws from the per-face table below, but the item form and the block
        // particle still need a single texture, or the block renders as nothing at all.
        setBlockTextureName(TEXTURE);
        setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
        setHardness(2.0F);
        setResistance(3.0F);
        setStepSound(soundTypeWood);
    }

    /**
     * The rack uses two textures — cabinet side for the frame, cabinet top for the shelves — so the
     * per-face icons are held here. {@code Block.getIcon} can only return one icon for all sides,
     * which is why this block owns the mapping the box renderer consumes.
     */
    @SideOnly(Side.CLIENT)
    @Override
    public void registerBlockIcons(IIconRegister iconRegister) {
        IIcon side = iconRegister.registerIcon("vinery:dark_cherry_cabinet_side");
        IIcon top = iconRegister.registerIcon("vinery:dark_cherry_cabinet_top");

        // Block#registerBlockIcons normally assigns blockIcon here, and this override replaces it
        // wholesale — so blockIcon has to be set by hand. Block#getIcon and
        // getBlockTextureFromSide both resolve through blockIcon, which is what the vanilla cube
        // renderer (and ItemBlock's own icon) use. Without this the block draws with a null icon:
        // invisible in the world, missing-texture square in the inventory.
        this.blockIcon = side;

        // BoxRenderer face order: up, down, north, south, west, east.
        this.faceIcons = new IIcon[] { top, top, side, side, side, side };
    }

    /** Per-face icons in BoxRenderer's order, or null on a server. */
    @SideOnly(Side.CLIENT)
    public IIcon[] faceIcons() {
        return faceIcons;
    }

    /**
     * Allocates this block's custom render id. Client-side only; called from the client proxy.
     *
     * <p>
     * 1.7.10 does not use -1 for "custom handler": RenderBlocks#renderBlockByRenderType returns
     * false immediately for -1 and never reaches Forge's handler lookup, so the block draws nothing.
     * A real id from RenderingRegistry#getNextAvailableRenderId is required, and that id can only be
     * allocated on a client, hence a setter rather than a constructor argument.
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
    public boolean getBlocksMovement(net.minecraft.world.IBlockAccess world, int x, int y, int z) {
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
        int facing = ((int) (placer.rotationYaw * 4.0F / 360.0F + 0.5F)) & 3;
        world.setBlockMetadataWithNotify(x, y, z, facing & 3, 2);
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
