package com.mrfuzzihead.vinery.core.block;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import com.mrfuzzihead.vinery.client.render.BoxRenderer.Box;

/**
 * The dark cherry chair — a two-block-tall seat.
 *
 * <p>
 * 1.7.10 has no block-property system, so the half is a metadata bit: bits 0-1 hold the facing, as
 * every {@link net.minecraft.block.BlockDirectional} does, and bit 2 marks the upper half. The
 * 1.21 original is a {@code DoubleBlockHalf} property; bit 2 is 1.7.10's equivalent.
 *
 * <p>
 * Placement mirrors vanilla's two-part blocks such as {@code BlockBed}: {@link #canPlaceBlockAt}
 * requires room above, {@link #onBlockPlacedBy} writes the upper half, and {@link #breakBlock}
 * removes the other half so neither can be left orphaned. Because the two halves share one block
 * class and one ItemBlock, breaking the top drops the same item and breaking the bottom takes the top
 * with it.
 *
 * <p>
 * Collision is not written here at all — {@link BoxRenderedBlock} derives it from
 * {@link #geometry(int)}, so the hitbox is exactly the boxes that get drawn.
 *
 * <p>
 * Sitting is Phase 3: 1.21 pairs this block with a {@code ChairEntity}, which is how a player rides
 * it. Nothing here mounts anything yet.
 */
public class ChairBlock extends BoxRenderedBlock {

    /** Metadata bit marking the backrest half. */
    private static final int HALF_UPPER = 0x4;

    public ChairBlock(String frameTexture, String panelTexture) {
        super(frameTexture, panelTexture);
    }

    private static boolean isUpper(int metadata) {
        return (metadata & HALF_UPPER) != 0;
    }

    @Override
    public Box[] geometry(int metadata) {
        return isUpper(metadata) ? ChairGeometry.UPPER : ChairGeometry.LOWER;
    }

    /** Both chair textures are cut out, so this is the first block that needs alpha testing. */
    @Override
    public boolean usesAlphaTest() {
        return false; // TEMPORARY: alpha test off, to see whether it is discarding faces
    }

    /** Needs the block above to be free, so a chair cannot be placed jammed under a ceiling. */
    @Override
    public boolean canPlaceBlockAt(World world, int x, int y, int z) {
        return world.isAirBlock(x, y + 1, z);
    }

    /** Places the backrest above, keeping the facing that the base class just wrote. */
    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase placer, ItemStack stack) {
        super.onBlockPlacedBy(world, x, y, z, placer, stack);
        if (world.isRemote) {
            return;
        }
        int facing = world.getBlockMetadata(x, y, z) & 3;
        world.setBlock(x, y + 1, z, this, facing | HALF_UPPER, 3);
    }

    /** Breaking either half takes the other, so no orphaned backrest is left floating. */
    @Override
    public void breakBlock(World world, int x, int y, int z, net.minecraft.block.Block broken, int meta) {
        if (!world.isRemote) {
            int dy = isUpper(meta) ? -1 : 1;
            if (world.getBlock(x, y + dy, z) == this
                && isUpper(world.getBlockMetadata(x, y + dy, z)) != isUpper(meta)) {
                world.setBlockToAir(x, y + dy, z);
            }
        }
        super.breakBlock(world, x, y, z, broken, meta);
    }

    @Override
    public int damageDropped(int meta) {
        return 0;
    }
}
