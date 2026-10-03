package com.mrfuzzihead.vinery.client.render;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;

/**
 * Draws axis-aligned boxes for Vinery's decorative blocks (wine racks, lattices, furniture).
 *
 * <p>
 * 1.7.10 has no JSON model system (BACKPORT_PLAN.md section 5), so custom geometry has to be
 * drawn by hand. The obvious approach — appending quads to the {@code Tessellator} — is a trap:
 * Forge calls a block renderer from <em>inside</em> an already-open Tessellator batch,
 * {@code Tessellator#addVertex} never reads the modelview matrix (so a {@code glTranslatef} does
 * nothing), and nothing binds the block atlas for a custom handler. Each of those produced a
 * silently invisible block.
 *
 * <p>
 * Instead this renders through vanilla's own cube path, which is the documented way to draw
 * arbitrary boxes on 1.7.10 and is what mods such as TFC do:
 *
 * <pre>
 * renderer.setRenderBounds(minX, minY, minZ, maxX, maxY, maxZ);
 * renderer.renderStandardBlock(block, x, y, z);
 * </pre>
 *
 * <p>
 * That hands coordinates, lighting, ambient occlusion, mipmapping and atlas binding to vanilla.
 * Per-box texture selection uses {@code overrideBlockTexture}, the same mechanism TFC uses for its
 * multi-texture piles.
 */
public final class BoxRenderer {

    private BoxRenderer() {}

    /** An axis-aligned box in 0-1 block space, plus the texture to draw it with. */
    public static final class Box {

        final double minX;
        final double minY;
        final double minZ;
        final double maxX;
        final double maxY;
        final double maxZ;

        /** Texture index into the owner's icon array, or -1 to use the block's own icon. */
        final int texture;

        public Box(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
            this(minX, minY, minZ, maxX, maxY, maxZ, -1);
        }

        public Box(double minX, double minY, double minZ, double maxX, double maxY, double maxZ, int texture) {
            this.minX = Math.min(minX, maxX);
            this.minY = Math.min(minY, maxY);
            this.minZ = Math.min(minZ, maxZ);
            this.maxX = Math.max(minX, maxX);
            this.maxY = Math.max(minY, maxY);
            this.maxZ = Math.max(minZ, maxZ);
            this.texture = texture;
        }

        /** Constructor taking 0-16 model coordinates, as used by the block model JSONs. */
        public Box(double x1, double y1, double z1, double x2, double y2, double z2, int texture,
            boolean fromModelSpace) {
            this(x1 / 16.0D, y1 / 16.0D, z1 / 16.0D, x2 / 16.0D, y2 / 16.0D, z2 / 16.0D, texture);
        }
    }

    /** Draws a block's boxes in the world. */
    public static void renderWorld(IBlockAccess world, int x, int y, int z, Block block, int metadata, Box[] boxes,
        IIcon[] textures, RenderBlocks renderer) {
        // Icon order here is up, down, north, south, west, east. Boxes index it by face constant.
        for (Box box : boxes) {
            renderer.overrideBlockTexture = box.texture >= 0 && textures.length > box.texture ? textures[box.texture]
                : null;

            renderer.setRenderBounds(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
            renderer.renderStandardBlock(block, x, y, z);
        }

        renderer.clearOverrideBlockTexture();
    }

    /**
     * Draws a block's boxes in an inventory or GUI context.
     *
     * <p>
     * No camera transform here: RenderBlocks#renderBlockInventory already applies the isometric
     * item view, and every GL call in this path risks the same class of silent bug as the world path.
     */
    public static void renderInventory(Block block, int metadata, Box[] boxes, IIcon[] textures,
        RenderBlocks renderer) {
        if (renderer.blockAccess == null) {
            // No world to sample lighting from; GuiContainerCreative and friends leave blockAccess
            // unset. RenderBlocks#renderStandardBlock would NPE in getMixedBrightnessForBlock.
            return;
        }

        for (Box box : boxes) {
            renderer.overrideBlockTexture = box.texture >= 0 && textures.length > box.texture ? textures[box.texture]
                : null;
            renderer.setRenderBounds(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
            renderer.renderStandardBlock(block, 0, 0, 0);
        }
        renderer.clearOverrideBlockTexture();
    }

    /** Resolves a block's icons, ordered up, down, north, south, west, east. */
    public static IIcon[] iconsOf(Block block, int metadata) {
        IIcon[] result = new IIcon[6];
        int[] sides = { 1, 0, 2, 3, 4, 5 };
        for (int face = 0; face < 6; face++) {
            result[face] = block.getIcon(sides[face], metadata);
        }
        return result;
    }
}
