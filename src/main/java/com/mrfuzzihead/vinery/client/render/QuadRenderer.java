package com.mrfuzzihead.vinery.client.render;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;

import org.lwjgl.opengl.GL11;

import com.mrfuzzihead.vinery.client.render.BoxRenderer.Box;

/**
 * Draws boxes as explicit quads, honouring each face's own UV rectangle and texture.
 *
 * <p>
 * {@link BoxRenderer}'s normal path calls {@code RenderBlocks#renderStandardBlock}, which is right
 * for a block whose faces use whole textures — the racks do — but it can only map an entire icon
 * onto a face. The 1.21 models are authored the other way round: the chair samples
 * {@code [3,3,13,13]} for the seat and {@code [0,8,2,16]} for a leg, out of 16x16 sheets that pack
 * several parts with transparent padding around them. Through the cube renderer those faces show the
 * wrong part of the sheet at the wrong scale, which is why the chair did not look like the model.
 *
 * <p>
 * So a box carrying sub-rect UVs is drawn here instead, and a block uses this path only for the
 * boxes that need it.
 *
 * <p>
 * Everything runs inside the Tessellator batch Forge has already opened, whose captured translation
 * already places the block, so vertices are block-local 0..1. The block atlas is bound by the chunk
 * renderer and every icon comes from it, so nothing is rebound — one of the ways the hand-rolled
 * renderer deleted earlier went wrong.
 */
final class QuadRenderer {

    private QuadRenderer() {}

    // Face order matches BoxRenderer: down, up, north, south, west, east. Each entry is the four
    // corners in CCW order seen from outside the face, as (a, b, c) selector triples over the box.
    /**
     * Diagnostic switch. When true, the quad path draws with back-face culling disabled, which
     * makes every emitted quad visible regardless of its winding.
     *
     * <p>
     * Settles winding questions in a single run: if a block appears with this on and vanishes
     * with it off, its winding is wrong, and the per-face corner order is where to fix it.
     */
    static final boolean NO_CULL_DIAGNOSTIC = false; // TEMP: culling confirmed NOT the cause

    /** Logs the first vertex emitted for a given block position, once per position. */
    static final boolean VERTEX_DIAGNOSTIC = Boolean.getBoolean("vinery.vertexDiag");

    private static final java.util.Set<String> LOGGED_POSITIONS = new java.util.HashSet<>();

    private static final double[][][] CORNERS = { { { 0, 0, 0 }, { 1, 0, 0 }, { 1, 0, 1 }, { 0, 0, 1 } },
        { { 0, 1, 1 }, { 1, 1, 1 }, { 1, 1, 0 }, { 0, 1, 0 } }, { { 0, 1, 0 }, { 1, 1, 0 }, { 1, 0, 0 }, { 0, 0, 0 } },
        { { 1, 1, 1 }, { 0, 1, 1 }, { 0, 0, 1 }, { 1, 0, 1 } }, { { 0, 1, 1 }, { 0, 1, 0 }, { 0, 0, 0 }, { 0, 0, 1 } },
        { { 1, 1, 0 }, { 1, 1, 1 }, { 1, 0, 1 }, { 1, 0, 0 } } };

    /** Corner (u, v) parameter pairs, in the same order as CORNERS. */
    private static final double[][] AB = { { 0, 1 }, { 1, 1 }, { 1, 0 }, { 0, 0 } };

    private static final double[] FULL_UV = { 0.0D, 0.0D, 1.0D, 1.0D };

    // Per-face brightness. RenderBlocks shades a cube's faces differently, and that variation is
    // most of what makes it read as a solid object rather than a flat cut-out. Setting one colour
    // for the whole box flattened the chair into a silhouette.
    private static final float[] FACE_SHADE = { 0.5F, 1.0F, 0.8F, 0.8F, 0.6F, 0.6F };

    /**
     * Debug: {@code -Dvinery.debugFaces} tints each face differently, so a single screenshot shows
     * exactly which faces draw.
     *
     * <p>
     * down red, up green, north blue, south yellow, west magenta, east cyan. The chair renders with
     * holes where the backrest and seat should be solid, and guessing which faces are missing has
     * cost several rounds; a face that should be present and is not will simply be absent, with
     * grass showing through where it was.
     */
    private static final boolean DEBUG_FACE_COLOUR = false; // TEMP: inconclusive, GL_MODULATE crushes it

    private static final float[][] DEBUG_FACE_RGB = { { 1.0F, 0.0F, 0.0F }, // down red
        { 0.0F, 1.0F, 0.0F }, // up green
        { 0.0F, 0.0F, 1.0F }, // north blue
        { 1.0F, 1.0F, 0.0F }, // south yellow
        { 1.0F, 0.0F, 1.0F }, // west magenta
        { 0.0F, 1.0F, 1.0F }, // east cyan
    };

    static int renderBox(IBlockAccess world, int x, int y, int z, Block block, int metadata, Box box,
        IIcon[] textures) {
        int facesDrawn = 0;
        int facing = metadata & 3;
        int packedBrightness = block.getMixedBrightnessForBlock(world, x, y, z);

        Tessellator tess = Tessellator.instance;

        boolean previousCull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        if (NO_CULL_DIAGNOSTIC) {
            GL11.glDisable(GL11.GL_CULL_FACE);
        }

        for (int face = 0; face < 6; face++) {
            if (box.faceUvs != null && box.faceUvs[face] == null) {
                continue; // the model gives this face no texture
            }
            double[] rect = box.faceUvs != null ? box.faceUvs[face] : FULL_UV;

            int index = box.faceTextures != null ? box.faceTextures[face] : box.texture;
            if (index < 0 || index >= textures.length || textures[index] == null) {
                continue;
            }
            IIcon icon = textures[index];

            // Shading is delegated to the Tessellator exactly as RenderBlocks does it:
            // setBrightness takes the packed sky/block value and unpacks and scales it itself,
            // and the colour applied is the ambient-occlusion factor on all three channels.
            //
            // Hand-rolling this is what made the chair invisible: an earlier version read sky
            // light from the wrong bit of the packed value and never scaled 0-15 up to 0-255, so
            // every face came out at RGB 15, which is effectively black.
            // setBrightness takes the packed sky/block value and unpacks and scales it itself.
            // White times this face's shade -- NOT getAmbientOcclusionLightValue(), which is the
            // per-vertex smoothing factor and is 0.2 for a non-opaque block. Using that here drew
            // every quad at 20% brightness.
            float faceShade = FACE_SHADE[face];
            if (DEBUG_FACE_COLOUR) {
                // setBrightness MULTIPLIES the vertex colour. In debug mode that would darken the
                // face tints toward the material and hide which face is which, so it is skipped and
                // the raw RGB goes through untouched.
                tess.setColorOpaque_F(DEBUG_FACE_RGB[face][0], DEBUG_FACE_RGB[face][1], DEBUG_FACE_RGB[face][2]);
            } else {
                tess.setBrightness(packedBrightness);
                tess.setColorOpaque_F(faceShade, faceShade, faceShade);
            }

            double[][] corners = CORNERS[face];
            for (int v = 0; v < 4; v++) {
                double px = lerp(box.minX, box.maxX, corners[v][0]);
                double py = lerp(box.minY, box.maxY, corners[v][1]);
                double pz = lerp(box.minZ, box.maxZ, corners[v][2]);

                // Facing is applied to the vertices here, because the quad path cannot rotate render
                // bounds the way renderStandardBlock does.
                double rx = rotateX(px, pz, facing);
                double rz = rotateZ(px, pz, facing);

                double u = lerp(rect[0], rect[2], AB[v][0]);
                double vv = lerp(rect[1], rect[3], AB[v][1]);

                // Tessellator#addVertexWithUV takes WORLD coordinates, and vanilla adds the block
                // position to each vertex itself -- in RenderBlocks' quad emitter this is literally
                // "iload_2; getfield renderMinX; dadd", i.e. x + minX -- so a box rendered at
                // (-6, 4, 490) is emitted at (-5.1875, 4.9375, 490.3125).
                //
                // Emitting the block-local 0..1 value instead draws every box a few centimetres from
                // the world origin. That is why the chair was invisible while its geometry, UVs and
                // winding were all correct: the whole thing was simply somewhere else.
                tess.addVertexWithUV(rx + x, py + y, rz + z, icon.getInterpolatedU(u), icon.getInterpolatedV(vv));
                facesDrawn++;

                if (VERTEX_DIAGNOSTIC && v == 0 && facesDrawn == 1 && LOGGED_POSITIONS.add(x + "," + y + "," + z)) {
                    com.mrfuzzihead.vinery.Vinery.LOG.info(
                        "[quad-vertex] {} at {},{},{} face={} icon={} uvRect=[{},{},{},{}] -> vertex ({}, {}, {}) uv ({}, {}) meta={}",
                        block.getClass()
                            .getSimpleName(),
                        x,
                        y,
                        z,
                        face,
                        icon.getIconName(),
                        rect[0],
                        rect[1],
                        rect[2],
                        rect[3],
                        rx + x,
                        py + y,
                        rz + z,
                        icon.getInterpolatedU(u),
                        icon.getInterpolatedV(vv),
                        metadata);
                }
            }
        }
        if (NO_CULL_DIAGNOSTIC && previousCull) {
            GL11.glEnable(GL11.GL_CULL_FACE);
        }
        return facesDrawn;
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    /** Clockwise seen from above, matching blockstate `y`: a quarter turn maps north to east. */
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
}
