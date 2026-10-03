package com.mrfuzzihead.vinery.core.block;

import com.mrfuzzihead.vinery.client.render.BoxRenderer.Box;

/**
 * Geometry transcribed from the 1.21 wine rack templates.
 *
 * <p>
 * Keeping it here rather than inside the block means the renderer, the collision box and any future
 * hit-box test all read the same numbers. Coordinates and UV rectangles are copied straight from the
 * model JSONs under {@code assets/vinery/models/block/}, so the original files stay the readable
 * reference and this is the 1.7.10 transcription.
 *
 * <p>
 * Face order is BoxRenderer's: up, down, north, south, west, east.
 */
public final class WineRackGeometry {

    private WineRackGeometry() {}

    /**
     * The big rack, from {@code template_wine_rack_1}: nine axis-aligned cuboids, no rotations.
     * Two side posts, top/bottom/back panels, two vertical dividers, two horizontal shelves — a 3x3
     * grid of cubbies, which is where the nine slots come from.
     */
    public static final Box[] BIG = {
        // Top panel
        box(
            1,
            15,
            0,
            15,
            16,
            16,
            new double[][] { uv(1, 0, 15, 1), uv(1, 0, 15, 1), uv(1, 0, 15, 16), uv(1, 0, 15, 16), uv(1, 0, 15, 16),
                uv(1, 0, 15, 16), }),
        // Back panel
        box(
            1,
            1,
            15,
            15,
            15,
            16,
            new double[][] { uv(1, 1, 15, 15), uv(1, 1, 15, 15), uv(0, 0, 0.5, 7.25), uv(0, 0, 0.5, 7.25),
                uv(0, 0, 6.5, 0.5), uv(0, 0, 6.5, 0.5), }),
        // Left post
        box(
            0,
            0,
            0,
            1,
            16,
            16,
            new double[][] { uv(15, 0, 16, 16), uv(0, 0, 16, 16), uv(15, 0, 16, 16), uv(0, 0, 1, 16), uv(15, 0, 16, 16),
                uv(16, 0, 15, 16), }),
        // Right post
        box(
            15,
            0,
            0,
            16,
            16,
            16,
            new double[][] { uv(15, 0, 16, 16), uv(15, 0, 16, 16), uv(0, 0, 16, 16), uv(0, 0, 16, 16), uv(0, 0, 1, 16),
                uv(16, 0, 15, 16), }),
        // Bottom panel
        box(
            1,
            0,
            0,
            15,
            1,
            16,
            new double[][] { uv(1, 0, 15, 1), uv(1, 0, 15, 1), uv(0, 0, 0, 0), uv(0, 0, 0, 0), uv(1, 0, 15, 16),
                uv(1, 0, 15, 16), }),
        // Vertical divider, left slot
        box(
            5,
            1,
            0.875,
            6,
            15,
            15.875,
            new double[][] { uv(15, 2, 16, 16), uv(0, 15, 15, 0), uv(1, 1, 15, 15), uv(1, 1, 15, 15), uv(0, 7, 16, 8),
                uv(0, 4, 16, 5), }),
        // Vertical divider, right slot
        box(
            10,
            1,
            0.875,
            11,
            15,
            15.875,
            new double[][] { uv(15, 2, 16, 16), uv(0, 15, 15, 0), uv(1, 1, 15, 15), uv(1, 1, 15, 15), uv(0, 7, 16, 8),
                uv(0, 4, 16, 5), }),
        // Horizontal shelf, upper
        box(
            1.001,
            9.75,
            1.375,
            15.001,
            10.75,
            15.375,
            new double[][] { uv(1, 1, 15, 2), uv(0, 13, 16, 14), uv(0, 4, 16, 5), uv(0, 7, 16, 8), uv(1, 1, 15, 15),
                uv(1, 1, 15, 16), }),
        // Horizontal shelf, lower
        box(
            1.001,
            5,
            1.375,
            15.001,
            6,
            15.375,
            new double[][] { uv(1, 1, 15, 2), uv(0, 13, 16, 14), uv(0, 4, 16, 5), uv(0, 7, 16, 8), uv(1, 1, 15, 15),
                uv(1, 1, 15, 16), }), };

    private static Box box(double x1, double y1, double z1, double x2, double y2, double z2, double[][] uvs) {
        return new Box(x1, y1, z1, x2, y2, z2, uvs);
    }

    private static double[] uv(double u1, double v1, double u2, double v2) {
        return new double[] { u1, v1, u2, v2 };
    }
}
