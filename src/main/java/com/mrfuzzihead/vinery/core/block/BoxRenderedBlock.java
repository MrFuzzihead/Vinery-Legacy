package com.mrfuzzihead.vinery.core.block;

import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDirectional;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import com.mrfuzzihead.vinery.client.render.BoxRenderer.Box;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Shared behaviour for every Vinery block drawn from axis-aligned boxes through {@link
 * com.mrfuzzihead.vinery.client.render.BoxRenderer}.
 *
 * <p>
 * This covers the wine racks and the furniture. They differ in geometry, in how much room they need
 * to be placed in and in whether they occupy one block or two — everything else is identical, and it
 * is exactly the sort of thing that goes wrong when copied: see {@link #onBlockPlacedBy}, where a
 * single {@code (int)} cast in place of {@code MathHelper.floor_double} pointed every rack a quarter
 * turn off for roughly half of all angles, because player yaw is routinely negative.
 *
 * <p>
 * Facing is raw metadata — bits 0-1 hold the facing, matching 1.7.10's {@link BlockDirectional}
 * convention, which leaves bit 2 and above free for subclasses (the chair uses bit 2 for its upper
 * half). 1.7.10 has no block-property system (BACKPORT_PLAN.md section 4.2).
 */
public abstract class BoxRenderedBlock extends BlockDirectional implements VineryCheckableBlock {

    /** Primary texture — the frame, and the item's icon. */
    private final String sideTexture;

    /** Secondary texture for the flat surfaces. */
    private final String topTexture;

    /** Third texture, or null when the shape has no distinct front. */
    private final String frontTexture;

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
     * All such blocks can share one id. Forge dispatches on {@link #getRenderType()} but hands the
     * actual {@link Block} to the handler, so a single registered handler serves them all and the
     * geometry is chosen per block via {@link #geometry(int)}.
     */
    private int renderId = -1;

    protected BoxRenderedBlock(String sideTexture, String topTexture) {
        this(sideTexture, topTexture, null);
    }

    /**
     * @param frontTexture the door texture, or null for a shape that is the same wood front to back.
     *                     Only the cabinet needs a distinct one.
     */
    protected BoxRenderedBlock(String sideTexture, String topTexture, String frontTexture) {
        super(Material.wood);
        this.sideTexture = sideTexture;
        this.topTexture = topTexture;
        this.frontTexture = frontTexture;
        // Block#getIcon returns blockIcon, which is what the ItemBlock uses for its inventory icon.
        // The box renderer draws from the per-face table below, but the item form and the block
        // particle still need a single texture, or the block renders as nothing at all.
        setBlockTextureName(sideTexture);
        setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
        setHardness(2.0F);
        setResistance(3.0F);
        setStepSound(soundTypeWood);
    }

    /**
     * The boxes this block draws, in 0-1 block space.
     *
     * @param metadata the block metadata, for the blocks that draw differently per state (the chair's
     *                 two halves). Blocks that ignore it may return one array unconditionally.
     */
    public abstract Box[] geometry(int metadata);

    /**
     * Whether this block's textures need alpha testing.
     *
     * <p>
     * False by default, which is correct whenever the textures are fully opaque — alpha testing then
     * changes nothing, and keeping it off means an alpha-tested path cannot regress the blocks already
     * verified in game. Blocks whose art is cut out override this.
     */
    public boolean usesAlphaTest() {
        return false;
    }

    @Override
    public String textureNameForTest() {
        return sideTexture;
    }

    /**
     * These blocks use two or three textures, so the per-face icons are held here.
     * {@code Block.getIcon} can only return one icon for all sides, which is why this class owns the
     * mapping the box renderer consumes.
     *
     * <p>
     * A shape that uses a further texture (only acacia's rack, whose bottom-panel underside points
     * at {@code acacia_drawer_bottom}) loses it: the vanilla cube renderer draws one texture per box,
     * so a single face cannot differ from the other five. It is the underside of the bottom panel,
     * visible only when looking up at the rack from below.
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

        IIcon front = frontTexture != null ? iconRegister.registerIcon(frontTexture) : side;

        // BoxRenderer face order: up, down, north, south, west, east.
        //
        // Two layouts exist because each geometry names its own indices. The big and small racks are
        // one wood front to back and keep the original order, leaving their verified SIDE_TEXTURE /
        // TOP_TEXTURE constants untouched. The cabinet needs a distinct front, so it puts the door on
        // north and shifts the side texture along — see WineRackGeometry's CABINET_* constants, which
        // match this second layout.
        this.faceIcons = frontTexture != null ? new IIcon[] { top, top, front, side, side, side }
            : new IIcon[] { top, top, side, side, side, side };
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

    /** Solid: players and mobs walk into furniture rather than through it. */
    @Override
    public boolean getBlocksMovement(IBlockAccess world, int x, int y, int z) {
        return true;
    }

    /**
     * Applies the facing rotation to a geometry array, for collision and picking.
     *
     * <p>
     * Same transform the renderer uses, via {@link Box#rotatedExtents(int)}. These paths used to read
     * {@code geometry()} directly and so used the <em>unrotated</em> boxes: the chair turned to face
     * the player while its hitbox stayed pointing the same way, leaving the backrest permanently on
     * the south side.
     */
    private static Box[] rotated(Box[] boxes, int facing) {
        Box[] out = new Box[boxes.length];
        for (int i = 0; i < boxes.length; i++) {
            out[i] = boxes[i].rotatedExtents(facing);
        }
        return out;
    }

    /**
     * Collision is derived from {@link #geometry(int)} rather than authored separately.
     *
     * <p>
     * This is what makes the hitbox follow the model. A separate collision shape drifts: it gets
     * updated when a shape moves and forgotten when it does not, and the symptom is punching a chair
     * while aiming at the air inside its block — or being unable to hit the chair at all. Deriving
     * both from one list means a box that is drawn is a box you can hit, exactly, with no solid air
     * and no unreachable slats. Vanilla's hoppers do the same thing.
     *
     * <p>
     * The offset matters: {@link #getCollisionBoundingBoxFromPool} and
     * {@link #addCollisionBoxesToList} deal in <em>world</em> coordinates, so every box is shifted by
     * the block position. Returning them unoffset is what once put the wine rack's collision at the
     * world origin.
     */
    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
        // Rotated for the facing, exactly as the renderer rotates what it draws. Reading the
        // unrotated geometry here is what left the chair's hitbox pointing one way while it faced
        // another.
        int facing = world.getBlockMetadata(x, y, z) & 3;
        Box[] boxes = rotated(geometry(world.getBlockMetadata(x, y, z)), facing);
        if (boxes.length == 0) {
            return null;
        }
        double minX = boxes[0].minX, minY = boxes[0].minY, minZ = boxes[0].minZ;
        double maxX = boxes[0].maxX, maxY = boxes[0].maxY, maxZ = boxes[0].maxZ;
        for (Box b : boxes) {
            minX = Math.min(minX, b.minX);
            minY = Math.min(minY, b.minY);
            minZ = Math.min(minZ, b.minZ);
            maxX = Math.max(maxX, b.maxX);
            maxY = Math.max(maxY, b.maxY);
            maxZ = Math.max(maxZ, b.maxZ);
        }
        return AxisAlignedBB.getBoundingBox(minX + x, minY + y, minZ + z, maxX + x, maxY + y, maxZ + z);
    }

    @Override
    public void addCollisionBoxesToList(World world, int x, int y, int z, AxisAlignedBB mask, List<AxisAlignedBB> boxes,
        Entity entity) {
        for (Box b : geometry(world.getBlockMetadata(x, y, z))) {
            AxisAlignedBB box = AxisAlignedBB
                .getBoundingBox(b.minX + x, b.minY + y, b.minZ + z, b.maxX + x, b.maxY + y, b.maxZ + z);
            if (mask.intersectsWith(box)) {
                boxes.add(box);
            }
        }
    }

    /** Placed against whatever they were clicked on, like doors and furnaces. */
    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase placer, ItemStack stack) {
        // 1.7.10 has no Facing enum, so the direction is derived from yaw the way BlockFurnace does.
        // MathHelper.floor_double, exactly as BlockFurnace does. A plain (int) cast truncates
        // toward zero, and player yaw is routinely negative (-177.6, -360.45 and so on), so
        // truncation silently yields -1 where flooring yields -2 — which after & 3 points the block
        // a quarter turn off for roughly half of all angles.
        //
        // Server-side only: the client also gets this callback, but with a yaw that has not been
        // normalised yet, so writing metadata there produces a wrong value for a frame until the
        // server's authoritative copy arrives.
        if (!world.isRemote) {
            int facing = MathHelper.floor_double(placer.rotationYaw * 4.0F / 360.0F + 0.5D) & 3;
            world.setBlockMetadataWithNotify(x, y, z, facing | (world.getBlockMetadata(x, y, z) & UPPER_BIT_MASK), 2);
        }
    }

    /**
     * Bits 0-1 hold the facing, so bit 2 and above belong to subclasses. A subclass writes its own
     * state here and this mask preserves it across {@link #onBlockPlacedBy}.
     */
    protected static final int UPPER_BIT_MASK = 0x4;

    /**
     * Picking follows the model too, not just collision.
     *
     * <p>
     * This matters separately from {@link #addCollisionBoxesToList}. Entity collision asks the
     * {@code World} for every box a block occupies, so a gap between the chair's legs is walkable
     * already. Picking does not: {@link Block#collisionRayTrace} ray-traces a <em>single</em> AABB,
     * namely the block's own bounds, so a block with several boxes is picked as their union and the
     * air inside it still registers a hit. Tracing each box here and returning the nearest is what
     * makes aiming at the gap between the legs a miss.
     *
     * <p>
     * Vanilla has the same limitation and accepts it — {@code BlockFence} is picked as a whole block
     * — which is the right call for a fence but not for a chair you can see through.
     */
    @Override
    public MovingObjectPosition collisionRayTrace(World world, int x, int y, int z, Vec3 start, Vec3 end) {
        MovingObjectPosition nearest = null;
        int facing = world.getBlockMetadata(x, y, z) & 3;
        for (Box b : rotated(geometry(world.getBlockMetadata(x, y, z)), facing)) {
            MovingObjectPosition hit = rayTraceBox(
                b.minX + x,
                b.minY + y,
                b.minZ + z,
                b.maxX + x,
                b.maxY + y,
                b.maxZ + z,
                start,
                end);
            if (hit != null && (nearest == null || distanceSq(hit.hitVec, start) < distanceSq(nearest.hitVec, start))) {
                nearest = hit;
            }
        }
        return nearest;
    }

    /**
     * Slab ray test for one box, returning the nearest face hit.
     *
     * <p>
     * Written out rather than delegating to {@link AxisAlignedBB#calculateIntercept}, which
     * <em>returns null when the ray's start point is inside the box</em> -- that is, exactly when
     * a player is close enough to the furniture to be aiming at it. Vanilla's own
     * {@link net.minecraft.block.Block#collisionRayTrace} hand-rolls the same test for this
     * reason. The self-test missed it because it fired its ray from outside the block.
     */
    private static MovingObjectPosition rayTraceBox(double minX, double minY, double minZ, double maxX, double maxY,
        double maxZ, Vec3 start, Vec3 end) {
        double dx = end.xCoord - start.xCoord;
        double dy = end.yCoord - start.yCoord;
        double dz = end.zCoord - start.zCoord;

        double tMin = 0.0D;
        double tMax = 1.0D;
        int side = -1;

        double[] origin = { start.xCoord, start.yCoord, start.zCoord };
        double[] dir = { dx, dy, dz };
        double[] low = { minX, minY, minZ };
        double[] high = { maxX, maxY, maxZ };

        for (int axis = 0; axis < 3; axis++) {
            if (Math.abs(dir[axis]) < 1.0E-8D) {
                // Parallel to this slab: a miss unless the origin already lies within it.
                if (origin[axis] < low[axis] || origin[axis] > high[axis]) {
                    return null;
                }
            } else {
                double inv = 1.0D / dir[axis];
                double t1 = (low[axis] - origin[axis]) * inv;
                double t2 = (high[axis] - origin[axis]) * inv;
                if (t1 > t2) {
                    double swap = t1;
                    t1 = t2;
                    t2 = swap;
                }
                if (t1 > tMin) {
                    tMin = t1;
                    side = axis * 2 + (dir[axis] >= 0.0D ? 0 : 1);
                }
                if (t2 < tMax) {
                    tMax = t2;
                }
                if (tMin > tMax) {
                    return null;
                }
            }
        }

        Vec3 hitVec = start.addVector(dx * tMin, dy * tMin, dz * tMin);
        return new MovingObjectPosition(
            (int) Math.floor(hitVec.xCoord),
            (int) Math.floor(hitVec.yCoord),
            (int) Math.floor(hitVec.zCoord),
            side,
            hitVec);
    }

    private static double distanceSq(Vec3 a, Vec3 b) {
        double dx = a.xCoord - b.xCoord;
        double dy = a.yCoord - b.yCoord;
        double dz = a.zCoord - b.zCoord;
        return dx * dx + dy * dy + dz * dz;
    }

    /**
     * The selection outline is one box, so it uses the same union the collision bounding box does.
     */
    @Override
    public AxisAlignedBB getSelectedBoundingBoxFromPool(World world, int x, int y, int z) {
        return getCollisionBoundingBoxFromPool(world, x, y, z);
    }

    @Override
    public int damageDropped(int meta) {
        return 0;
    }

    @Override
    public Item getItemDropped(int meta, Random random, int fortune) {
        return Item.getItemFromBlock(this);
    }

    /** The whole block, ignoring metadata: neither facing nor shape changes the drop. */
    @Override
    public int quantityDropped(Random random) {
        return 1;
    }
}
