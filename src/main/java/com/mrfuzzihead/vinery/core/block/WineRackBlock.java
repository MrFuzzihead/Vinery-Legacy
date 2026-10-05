package com.mrfuzzihead.vinery.core.block;

import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

/**
 * Shared behaviour for the three wine rack sizes.
 *
 * <p>
 * The three racks differ only in geometry and in how many bottles they hold — the big one is a 3x3
 * grid of cubbies, the other two are single columns — so the facing, drop, icon and render-id
 * behaviour lives in {@link BoxRenderedBlock} rather than being copied per rack. With ten wood
 * variants across three shapes, a copy-pasted {@code onBlockPlacedBy} is a bug waiting to happen.
 *
 * <p>
 * All three stand on the ground, so {@link #canPlaceBlockAt} requires a solid block beneath.
 */
public abstract class WineRackBlock extends BoxRenderedBlock {

    protected WineRackBlock(String sideTexture, String topTexture) {
        super(sideTexture, topTexture);
    }

    /**
     * @param frontTexture the door texture, or null for a shape that is the same wood front to back
     *                     (the big and small racks). Only the cabinet needs a distinct one.
     */
    protected WineRackBlock(String sideTexture, String topTexture, String frontTexture) {
        super(sideTexture, topTexture, frontTexture);
    }

    /**
     * Every rack draws one shape whatever its metadata, since the facing is applied by rotation, so
     * the metadata is simply not consulted. Subclasses implement {@link #geometry(int)}.
     */

    @Override
    public boolean canPlaceBlockAt(World world, int x, int y, int z) {
        return world.getBlock(x, y - 1, z)
            .isOpaqueCube();
    }

    /**
     * Racks stay solid across the whole block, overriding the geometry-derived hitbox below.
     *
     * <p>
     * A hitbox that follows the model is right for furniture — you should not be able to punch a
     * chair while aiming at the air inside it. It is wrong for a rack: the big one is an open 3x3
     * grid of cubbies and the small one is mostly X-brace, so per-box collision would let a player
     * walk into the very openings the bottles go in. These blocks have always collided as full
     * blocks, and that is the behaviour worth keeping.
     */
    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
        return AxisAlignedBB.getBoundingBox(x, y, z, x + 1, y + 1, z + 1);
    }

    @Override
    public void addCollisionBoxesToList(World world, int x, int y, int z, AxisAlignedBB mask, List<AxisAlignedBB> boxes,
        Entity entity) {
        AxisAlignedBB full = AxisAlignedBB.getBoundingBox(x, y, z, x + 1, y + 1, z + 1);
        if (mask.intersectsWith(full)) {
            boxes.add(full);
        }
    }

    /**
     * Racks are picked as a whole block, for the same reason they collide as one: the big one is an
     * open grid of cubbies, and aiming between its posts should hit the rack rather than pass
     * through to the world behind.
     */
    @Override
    public MovingObjectPosition collisionRayTrace(World world, int x, int y, int z, Vec3 start, Vec3 end) {
        return AxisAlignedBB.getBoundingBox(x, y, z, x + 1, y + 1, z + 1)
            .calculateIntercept(start, end);
    }
}
