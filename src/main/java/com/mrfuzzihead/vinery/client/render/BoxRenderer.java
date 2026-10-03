package com.mrfuzzihead.vinery.client.render;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.IIcon;

import org.lwjgl.opengl.GL11;

/**
 * Draws axis-aligned boxes with explicit texture UVs — the rendering primitive behind Vinery's
 * decorative blocks (wine racks, lattices, furniture).
 *
 * <p>
 * 1.7.10 has no JSON model system (BACKPORT_PLAN.md section 5), so there is nothing to lean on for
 * geometry that is not a vanilla cube. Instead of hand-rolling GL per block, every custom block
 * describes itself as a list of {@link Box}es and this class draws them.
 *
 * <p>
 * Shading is a flat per-face factor scaled by the world's light level rather than the smooth
 * per-vertex lighting {@code RenderBlocks} computes for standard blocks. That is the usual 1.7.10
 * custom-renderer trade-off: slightly less depth realism, but it stays on the normal vertex-colour
 * path, so it keeps working under every shader and texture pack.
 */
public final class BoxRenderer {

    /** Face order used everywhere: up, down, north, south, west, east. */
    public static final int UP = 0;
    public static final int DOWN = 1;
    public static final int NORTH = 2;
    public static final int SOUTH = 3;
    public static final int WEST = 4;
    public static final int EAST = 5;

    /** Vanilla's cube shading for each face, so custom geometry sits in the same tonal range. */
    private static final float[] FACE_SHADE = { 1.0F, 0.5F, 0.8F, 0.8F, 0.6F, 0.6F };

    /** Outward normals per face, same order as {@link #FACE_SHADE}. */
    private static final float[][] FACE_NORMAL = { { 0, 1, 0 }, { 0, -1, 0 }, { 0, 0, -1 }, { 0, 0, 1 }, { -1, 0, 0 },
        { 1, 0, 0 }, };

    private BoxRenderer() {}

    /** An axis-aligned box in 0-16 block space with a UV rectangle per face. */
    public static final class Box {

        final double x1;
        final double y1;
        final double z1;
        final double x2;
        final double y2;
        final double z2;

        /** UV rectangles as {@code u1,v1,u2,v2} in pixels, indexed by the face constants. */
        final double[][] uvs;

        public Box(double x1, double y1, double z1, double x2, double y2, double z2, double[][] uvs) {
            this.x1 = Math.min(x1, x2) / 16.0D;
            this.y1 = Math.min(y1, y2) / 16.0D;
            this.z1 = Math.min(z1, z2) / 16.0D;
            this.x2 = Math.max(x1, x2) / 16.0D;
            this.y2 = Math.max(y1, y2) / 16.0D;
            this.z2 = Math.max(z1, z2) / 16.0D;
            this.uvs = uvs;
        }

        /** Convenience for a box whose faces all share one UV rectangle. */
        public Box(double x1, double y1, double z1, double x2, double y2, double z2, double u1, double v1, double u2,
            double v2) {
            this(
                x1,
                y1,
                z1,
                x2,
                y2,
                z2,
                new double[][] { { u1, v1, u2, v2 }, { u1, v1, u2, v2 }, { u1, v1, u2, v2 }, { u1, v1, u2, v2 },
                    { u1, v1, u2, v2 }, { u1, v1, u2, v2 } });
        }
    }

    /**
     * Draws a block's boxes in world space.
     *
     * @param rotateY quarter turns applied around the block centre, so one box list serves all facings
     */
    public static void renderWorld(int x, int y, int z, Box[] boxes, IIcon[] textures, int brightness, int rotateY) {
        Tessellator tessellator = Tessellator.instance;

        GL11.glPushMatrix();
        // The atlas is already bound by RenderBlocks before this runs; our icons come from it too.
        GL11.glDisable(GL11.GL_CULL_FACE);

        // No startDrawing/draw here: Forge calls this from inside an already open Tessellator
        // session (RenderBlocks#renderBlockByRenderType is invoked mid-batch), and starting a
        // second one throws IllegalStateException("Already tesselating!"). A renderer just appends
        // its vertices and lets the enclosing session flush them.
        GL11.glTranslatef(x + 0.5F, y, z + 0.5F);
        if (rotateY != 0) {
            GL11.glRotatef(rotateY * 90.0F, 0.0F, 1.0F, 0.0F);
            GL11.glTranslatef(-0.5F, 0.0F, -0.5F);
        }

        // IBlockAccess#getLightBrightnessForSkyBlocks returns a *packed* value — sky light in
        // bits 20-23 and block light in bits 4-7 — not a 0-15 or 0-240 brightness. Dividing it
        // blindly makes every face saturate to full white.
        float sky = (brightness >> 20 & 15) / 15.0F;
        float blockLight = (brightness & 15) / 15.0F;
        float light = Math.max(sky, blockLight);
        for (Box box : boxes) {
            addBox(tessellator, box, textures, light);
        }

        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glPopMatrix();
    }

    /** Draws a block's boxes in an inventory or GUI context at a fixed light level. */
    public static void renderInventory(Box[] boxes, IIcon[] textures) {
        GL11.glPushMatrix();
        GL11.glDisable(GL11.GL_CULL_FACE);

        GL11.glTranslatef(0.5F, 0.5F, 0.5F);
        GL11.glRotatef(30.0F, 1.0F, 0.0F, 0.0F);
        GL11.glRotatef(225.0F, 0.0F, 1.0F, 0.0F);
        GL11.glScalef(0.625F, -0.625F, 0.625F);
        GL11.glTranslatef(-0.5F, -0.5F, -0.5F);

        // Unlike the world path, the inventory path runs after every vanilla startDrawing/draw pair
        // has closed, so this one owns its session.
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawing(3); // GL_QUADS
        for (Box box : boxes) {
            addBox(tessellator, box, textures, 1.0F);
        }
        tessellator.draw();

        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glPopMatrix();
    }

    private static void addBox(Tessellator tessellator, Box b, IIcon[] textures, float light) {
        for (int face = 0; face < 6; face++) {
            if (textures[face] == null || b.uvs[face] == null) continue;

            IIcon icon = textures[face];
            double[] uv = b.uvs[face];

            float u1 = icon.getInterpolatedU(uv[0]);
            float v1 = icon.getInterpolatedV(uv[1]);
            float u2 = icon.getInterpolatedU(uv[2]);
            float v2 = icon.getInterpolatedV(uv[3]);

            // Normals matter even though shading is done with vertex colours: GL lighting is on
            // during the block pass, so vertices without a normal are lit against garbage. This is
            // the same call RenderBlocks makes per face.
            float[] normal = FACE_NORMAL[face];
            tessellator.setNormal(normal[0], normal[1], normal[2]);

            float shade = FACE_SHADE[face] * light;
            tessellator.setColorRGBA_F(shade, shade, shade, 1.0F);

            // Corners are emitted per face in the winding order that matches FACE_SHADE's indexing.
            switch (face) {
                case UP -> emit(
                    tessellator,
                    b.x1,
                    b.y2,
                    b.z1,
                    u1,
                    v1,
                    b.x1,
                    b.y2,
                    b.z2,
                    u1,
                    v2,
                    b.x2,
                    b.y2,
                    b.z2,
                    u2,
                    v2,
                    b.x2,
                    b.y2,
                    b.z1,
                    u2,
                    v1);
                case DOWN -> emit(
                    tessellator,
                    b.x1,
                    b.y1,
                    b.z2,
                    u1,
                    v1,
                    b.x1,
                    b.y1,
                    b.z1,
                    u1,
                    v2,
                    b.x2,
                    b.y1,
                    b.z1,
                    u2,
                    v2,
                    b.x2,
                    b.y1,
                    b.z2,
                    u2,
                    v1);
                case NORTH -> emit(
                    tessellator,
                    b.x2,
                    b.y1,
                    b.z1,
                    u1,
                    v1,
                    b.x2,
                    b.y2,
                    b.z1,
                    u1,
                    v2,
                    b.x1,
                    b.y2,
                    b.z1,
                    u2,
                    v2,
                    b.x1,
                    b.y1,
                    b.z1,
                    u2,
                    v1);
                case SOUTH -> emit(
                    tessellator,
                    b.x1,
                    b.y1,
                    b.z2,
                    u1,
                    v1,
                    b.x1,
                    b.y2,
                    b.z2,
                    u1,
                    v2,
                    b.x2,
                    b.y2,
                    b.z2,
                    u2,
                    v2,
                    b.x2,
                    b.y1,
                    b.z2,
                    u2,
                    v1);
                case WEST -> emit(
                    tessellator,
                    b.x1,
                    b.y1,
                    b.z1,
                    u1,
                    v1,
                    b.x1,
                    b.y2,
                    b.z1,
                    u1,
                    v2,
                    b.x1,
                    b.y2,
                    b.z2,
                    u2,
                    v2,
                    b.x1,
                    b.y1,
                    b.z2,
                    u2,
                    v1);
                case EAST -> emit(
                    tessellator,
                    b.x2,
                    b.y1,
                    b.z2,
                    u1,
                    v1,
                    b.x2,
                    b.y2,
                    b.z2,
                    u1,
                    v2,
                    b.x2,
                    b.y2,
                    b.z1,
                    u2,
                    v2,
                    b.x2,
                    b.y1,
                    b.z1,
                    u2,
                    v1);
                default -> {
                    // no-op
                }
            }
        }
    }

    private static void emit(Tessellator t, double x0, double y0, double z0, double uA, double vA, double x1, double y1,
        double z1, double uB, double vB, double x2, double y2, double z2, double uC, double vC, double x3, double y3,
        double z3, double uD, double vD) {
        t.addVertexWithUV(x0, y0, z0, uA, vA);
        t.addVertexWithUV(x1, y1, z1, uB, vB);
        t.addVertexWithUV(x2, y2, z2, uC, vC);
        t.addVertexWithUV(x3, y3, z3, uD, vD);
    }

    /** Resolves the six face icons for a block, in BoxRenderer face order. */
    public static IIcon[] iconsOf(Block block, int metadata) {
        IIcon[] result = new IIcon[6];
        int[] sides = { 1, 0, 2, 3, 4, 5 };
        for (int face = 0; face < 6; face++) {
            result[face] = block.getIcon(sides[face], metadata);
        }
        return result;
    }
}
