package com.mrfuzzihead.vinery.client.render;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;

import org.lwjgl.opengl.GL11;

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

        public final double minX;
        public final double minY;
        public final double minZ;
        public final double maxX;
        public final double maxY;
        public final double maxZ;

        /** Texture index into the owner's icon array, or -1 to use the block's own icon. */
        public final int texture;

        public Box(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
            this(minX, minY, minZ, maxX, maxY, maxZ, -1);
        }

        public Box(double minX, double minY, double minZ, double maxX, double maxY, double maxZ, int texture) {
            this(minX, minY, minZ, maxX, maxY, maxZ, texture, null, null);
        }

        /**
         * @param faceUvs      per-face UV rectangles in 0-1 of the icon, indexed down, up, north, south,
         *                     west, east; or null when every face maps the whole icon
         * @param faceTextures per-face texture indices, or null when every face uses {@code texture}
         */
        public Box(double minX, double minY, double minZ, double maxX, double maxY, double maxZ, int texture,
            double[][] faceUvs, int[] faceTextures) {
            this.minX = Math.min(minX, maxX);
            this.minY = Math.min(minY, maxY);
            this.minZ = Math.min(minZ, maxZ);
            this.maxX = Math.max(minX, maxX);
            this.maxY = Math.max(maxY, maxY);
            this.maxZ = Math.max(minZ, maxZ);
            this.texture = texture;
            this.faceUvs = faceUvs;
            this.faceTextures = faceTextures;
        }

        /**
         * Per-face UV rectangles in 0-1 of the icon, or null when every face maps the whole icon.
         *
         * <p>
         * 1.21 models address <em>sub-rectangles</em> of a texture: the chair samples
         * {@code [3,3,13,13]} for the seat and {@code [0,8,2,16]} for a leg, out of 16x16 sheets that
         * pack several parts with transparent padding around them.
         * {@code RenderBlocks#renderStandardBlock} can only map an entire icon onto a face, so a box
         * with sub-rect UVs is drawn by the quad path instead, which honours them.
         */
        public final double[][] faceUvs;

        /** Per-face texture indices, or null when every face uses {@link #texture}. */
        public final int[] faceTextures;

        private static double rotX(double x, double z, int facing) {
            return switch (facing & 3) {
                case 1 -> 1.0D - z;
                case 2 -> 1.0D - x;
                case 3 -> z;
                default -> x;
            };
        }

        private static double rotZ(double x, double z, int facing) {
            return switch (facing & 3) {
                case 1 -> x;
                case 2 -> 1.0D - z;
                case 3 -> 1.0D - x;
                default -> z;
            };
        }

        /**
         * This box's extents after the facing rotation — the same clockwise quarter-turn the cube
         * renderer applies, and the same one the quad path applies per vertex.
         *
         * <p>
         * Rendering, collision and picking all call this. They used to carry their own copies of the
         * rotation, and when collision was derived from `geometry()` it quietly used the
         * <em>unrotated</em> boxes: the chair turned to face the player while its hitbox stayed
         * pointing the same way, so the backrest was always south.
         *
         * <p>
         * Only the extents are rotated. Per-face UVs are not meaningful once a box has been turned —
         * the quad path needs the unrotated box and rotates each vertex instead — so this is for
         * callers that use the AABB alone.
         */
        public Box rotatedExtents(int facing) {
            int f = facing & 3;
            if (f == 0) {
                return this;
            }
            double rx0 = rotX(minX, minZ, f);
            double rx1 = rotX(maxX, maxZ, f);
            double rz0 = rotZ(minX, minZ, f);
            double rz1 = rotZ(maxX, maxZ, f);
            return new Box(
                Math.min(rx0, rx1),
                minY,
                Math.min(rz0, rz1),
                Math.max(rx0, rx1),
                maxY,
                Math.max(rz0, rz1),
                texture);
        }

        /** Whether any face needs the quad path, i.e. a sub-rect UV or its own texture. */
        public boolean needsQuadPath() {
            return faceUvs != null || faceTextures != null;
        }

        /** Constructor taking 0-16 model coordinates, as used by the block model JSONs. */
        public Box(double x1, double y1, double z1, double x2, double y2, double z2, int texture,
            boolean fromModelSpace) {
            this(x1 / 16.0D, y1 / 16.0D, z1 / 16.0D, x2 / 16.0D, y2 / 16.0D, z2 / 16.0D, texture);
        }
    }

    /**
     * Rotates a point around the block centre by facing quarter-turns, clockwise seen from above.
     *
     * <p>
     * Matches Minecraft blockstate `y` semantics: a 90-degree turn maps north to east, so
     * facing 1 (east) turns the authored model, which opens toward north, by one quarter.
     */
    private static double rotateX(double x, double z, int facing) {
        return switch (facing & 3) {
            case 1 -> 1.0D - z;
            case 2 -> 1.0D - x;
            case 3 -> z;
            default -> x;
        };
    }

    private static double rotateZ(double x, double z, int facing) {
        return switch (facing & 3) {
            case 1 -> x;
            case 2 -> 1.0D - z;
            case 3 -> 1.0D - x;
            default -> z;
        };
    }

    /**
     * Alpha cutoff used when {@code alphaTest} is on.
     *
     * <p>
     * 0.1 is the value vanilla 1.8 uses for its own cut-out blocks, so the threshold is one
     * Minecraft shipped rather than one tuned here.
     *
     * <p>
     * The comparison is {@code GL_GEQUAL}, not {@code GL_FALSLF}. 1.8 is on LWJGL 3, where
     * {@code glAlphaFunc(ref, GL_FALSLF, r)} is the spelling; 1.7.10 is on LWJGL 2, which has no
     * {@code GL_FALSLF} at all. {@code GL_GEQUAL} with the same reference is the same test --
     * "keep the fragment when alpha >= 0.1".
     */
    private static final float ALPHA_CUTOFF = 0.1F;

    /**
     * Draws a block's boxes in the world.
     *
     * @param alphaTest discard fully transparent texels instead of blending them through. 1.7.10 has
     *                  no alpha testing anywhere -- searching every patched Minecraft class for
     *                  {@code alphaFunc} or {@code AlphaTest} returns nothing -- so a cut-out texture on a cube is
     *                  drawn with GL blending and its transparent pixels let the world through. Blocks whose
     *                  textures are fully opaque draw identically either way.
     */
    private static final java.util.Set<String> LOGGED_QUAD_PATH = new java.util.HashSet<>();

    public static void renderWorld(IBlockAccess world, int x, int y, int z, Block block, int metadata, Box[] boxes,
        IIcon[] textures, boolean alphaTest, RenderBlocks renderer) {
        // Icon order here is up, down, north, south, west, east. Boxes index it by face constant.
        //
        // renderStandardBlock only draws axis-aligned boxes from the render bounds, so facing is
        // applied by rotating those bounds rather than the vertices — the box list stays authored
        // once and every facing reuses it.
        int facing = metadata & 3;

        // Every face must be drawn. RenderBlocks culls a face when the neighbouring *block* at the
        // block's own coordinates is opaque, and it has no idea a box is not the whole block: all
        // nine boxes are submitted as `renderStandardBlock(block, x, y, z)`, so the top panel's
        // underside is tested against the block *below the rack*. On the ground that is solid, the
        // face is culled, and the rack's interior ceiling is missing — you look up into the cubbies
        // and see the sky. renderAllFaces disables the culling entirely.
        //
        // RenderBlocks is shared with vanilla's own renderers, so the previous value is restored.
        boolean previousRenderAllFaces = renderer.renderAllFaces;
        boolean previousAlphaTest = GL11.glIsEnabled(GL11.GL_ALPHA_TEST);
        renderer.renderAllFaces = true;
        if (alphaTest) {
            GL11.glEnable(GL11.GL_ALPHA_TEST);
            GL11.glAlphaFunc(GL11.GL_GEQUAL, ALPHA_CUTOFF);
        }
        int quadFaces = 0;
        int quadBoxes = 0;
        try {
            for (Box box : boxes) {
                // A box with per-face UV rects or per-face textures cannot go through
                // renderStandardBlock, which maps a whole icon onto every face. Those go to the quad
                // path instead, so a block may mix both freely — the racks are all cube-path boxes.
                if (box.needsQuadPath()) {
                    quadBoxes++;
                    quadFaces += QuadRenderer.renderBox(world, x, y, z, block, metadata, box, textures);
                    continue;
                }
                renderer.overrideBlockTexture = box.texture >= 0 && textures.length > box.texture
                    ? textures[box.texture]
                    : null;

                Box r = box.rotatedExtents(facing);
                renderer.setRenderBounds(r.minX, r.minY, r.minZ, r.maxX, r.maxY, r.maxZ);
                renderer.renderStandardBlock(block, x, y, z);
            }
        } finally {
            if (quadBoxes > 0 && LOGGED_QUAD_PATH.add(
                block.getClass()
                    .getSimpleName())) {
                com.mrfuzzihead.vinery.Vinery.LOG.warn(
                    "[Vinery] quad path for {}: {} box(es) supplied, {} face(s) emitted",
                    block.getClass()
                        .getSimpleName(),
                    quadBoxes,
                    quadFaces);
            }
            // Restored rather than merely disabled, because another mod may have set its own func.
            if (!previousAlphaTest) {
                GL11.glDisable(GL11.GL_ALPHA_TEST);
                GL11.glAlphaFunc(GL11.GL_GREATER, ALPHA_CUTOFF);
            }
            renderer.renderAllFaces = previousRenderAllFaces;
            renderer.clearOverrideBlockTexture();
        }
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

        // Same reasoning as renderWorld: the boxes are not the whole block, so vanilla's
        // block-coordinate face culling would drop interior faces.
        boolean previousRenderAllFaces = renderer.renderAllFaces;
        renderer.renderAllFaces = true;
        try {
            for (Box box : boxes) {
                renderer.overrideBlockTexture = box.texture >= 0 && textures.length > box.texture
                    ? textures[box.texture]
                    : null;
                renderer.setRenderBounds(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
                renderer.renderStandardBlock(block, 0, 0, 0);
            }
        } finally {
            renderer.renderAllFaces = previousRenderAllFaces;
            renderer.clearOverrideBlockTexture();
        }
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
