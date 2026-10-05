package com.mrfuzzihead.vinery.core.block;

import com.mrfuzzihead.vinery.client.render.BoxRenderer.Box;

/**
 * Geometry transcribed from the 1.21 chair models.
 *
 * <p>
 * The chair occupies two blocks. {@code dark_cherry_chair_bottom} is the seat and legs and fills
 * the lower block; {@code dark_cherry_chair_top} is the backrest and sits in the block above, drawn at
 * y 0..0.3125 of its own block, which is why the whole chair is 1.3125 blocks tall.
 *
 * <p>
 * <b>Element 7 is deliberately omitted.</b> It reads {@code [11,4,4] -> [3,6,12]} — bounds inverted,
 * x running from 11 down to 3 — which normalises to the same box as element 6, {@code
 * [4,4,4] -> [12,6,12]}, just 8 units wide instead of 8. It is a duplicated cushion left in the
 * export. The 1.21 {@code ChairBlock} confirms this independently: {@code SHAPE_LOWER} contains
 * exactly seven boxes, matching elements 0 through 6, and has no eighth. Those same shapes are
 * transcribed verbatim into {@link ChairBlock}'s collision.
 *
 * <p>
 * Only two textures are involved, both cut out: {@code chair_top} (72 transparent pixels of 256) for
 * the frame and {@code chair_base} (156 transparent) for the seat and backrest faces. Because one
 * box carries one texture in 1.7.10, the seat box draws entirely as {@code chair_base} even though
 * only its top and bottom use that texture in the model, and the backrest likewise — its broad north
 * and south faces are {@code chair_base} while its top and edges are {@code chair_top}.
 */
public final class ChairGeometry {

    private ChairGeometry() {}

    /**
     * Geometry generated from the 1.21 chair models by tools/gen-box-geometry.mjs — do not hand-edit.
     *
     * <p>
     * The chair occupies two blocks: <code>dark_cherry_chair_bottom</code> is the seat and legs and fills
     * the lower block; <code>dark_cherry_chair_top</code> is the backrest and sits in the block above at
     * y 0..0.3125 of its own block, making the chair 1.3125 blocks tall.
     *
     * <p>
     * The 1.21 models address sub-rectangles of packed 16x16 sheets, so every box carries per-face UV
     * rectangles and per-face textures and is drawn by the quad path rather than by
     * {@code renderStandardBlock}, which can only map a whole icon onto a face.
     *
     * <p>
     * Regenerating:
     *
     * <pre>
     * node tools/regen-chair.mjs
     * </pre>
     *
     * <p>
     * Element 7 of the lower half is skipped. It reads <code>[11,4,4] -&gt; [3,6,12]</code> — bounds
     * inverted, x running from 11 down to 3 — and normalises to element 6&apos;s cushion. The 1.21
     * {@code ChairBlock} confirms it independently: SHAPE_LOWER holds exactly seven boxes, matching
     * elements 0 through 6, with no eighth.
     */

    public static final Box[] LOWER = {
        // element 0
        new Box(
            0.18750,
            0.93750,
            0.68750,
            0.81250,
            1.00000,
            0.81250,
            -1,
            new double[][] { new double[] { 0.8750, 0.6250, 0.2500, 0.5000 }, null,
                new double[] { 0.2500, 0.4375, 0.8750, 0.5000 }, new double[] { 0.2500, 0.4375, 0.8750, 0.5000 },
                new double[] { 0.1250, 0.4375, 0.2500, 0.5000 }, new double[] { 0.0000, 0.4375, 0.1250, 0.5000 } },
            new int[] { 2, -1, 2, 2, 2, 2 }),
        // element 1
        new Box(
            0.18750,
            0.50000,
            0.18750,
            0.81250,
            0.62500,
            0.81250,
            -1,
            new double[][] { new double[] { 0.1875, 0.1875, 0.8125, 0.8125 },
                new double[] { 0.1875, 0.1875, 0.8125, 0.8125 }, new double[] { 0.3125, 0.1250, 0.9375, 0.0000 },
                new double[] { 0.2500, 0.6250, 0.8750, 0.7500 }, new double[] { 0.3750, 0.5000, 1.0000, 0.6250 },
                new double[] { 1.0000, 0.5000, 0.3750, 0.6250 } },
            new int[] { 0, 0, 2, 2, 2, 2 }),
        // element 2
        new Box(
            0.18750,
            0.00000,
            0.68750,
            0.31250,
            0.93750,
            0.81250,
            -1,
            new double[][] { new double[] { 1.0000, 0.1250, 0.8750, 0.2500 }, null,
                new double[] { 0.1250, 0.0625, 0.2500, 1.0000 }, new double[] { 0.0000, 0.0625, 0.1250, 1.0000 },
                new double[] { 0.1250, 0.0625, 0.2500, 1.0000 }, new double[] { 0.0625, 0.0625, 0.1875, 1.0000 } },
            new int[] { 2, -1, 2, 2, 2, 2 }),
        // element 3
        new Box(
            0.18750,
            0.00000,
            0.18750,
            0.31250,
            0.50000,
            0.31250,
            -1,
            new double[][] { new double[] { 1.0000, 0.1250, 0.8750, 0.2500 }, null,
                new double[] { 0.0000, 0.5000, 0.1250, 1.0000 }, new double[] { 0.0000, 0.5000, 0.1250, 1.0000 },
                new double[] { 0.1250, 0.5000, 0.2500, 1.0000 }, new double[] { 0.1250, 0.5000, 0.2500, 1.0000 } },
            new int[] { 2, -1, 2, 2, 2, 2 }),
        // element 4
        new Box(
            0.68750,
            0.00000,
            0.18750,
            0.81250,
            0.50000,
            0.31250,
            -1,
            new double[][] { new double[] { 1.0000, 0.1250, 0.8750, 0.2500 }, null,
                new double[] { 0.1250, 0.5000, 0.2500, 1.0000 }, new double[] { 0.1250, 0.5000, 0.2500, 1.0000 },
                new double[] { 0.0000, 0.5000, 0.1250, 1.0000 }, new double[] { 0.0000, 0.5000, 0.1250, 1.0000 } },
            new int[] { 2, -1, 2, 2, 2, 2 }),
        // element 5
        new Box(
            0.68750,
            0.00000,
            0.68750,
            0.81250,
            0.93750,
            0.81250,
            -1,
            new double[][] { new double[] { 1.0000, 0.1250, 0.8750, 0.2500 }, null,
                new double[] { 0.0000, 0.0625, 0.1250, 1.0000 }, new double[] { 0.1250, 0.0625, 0.2500, 1.0000 },
                new double[] { 0.1250, 0.0625, 0.2500, 1.0000 }, new double[] { 0.0000, 0.0625, 0.1250, 1.0000 } },
            new int[] { 2, -1, 2, 2, 2, 2 }),
        // element 6
        new Box(
            0.25000,
            0.25000,
            0.25000,
            0.75000,
            0.37500,
            0.75000,
            -1,
            new double[][] { null, null, new double[] { 0.1875, 0.5000, 0.8125, 0.6250 },
                new double[] { 0.1875, 0.5000, 0.8125, 0.6250 }, new double[] { 0.2500, 0.5000, 0.7500, 0.6250 },
                new double[] { 0.2500, 0.5000, 0.7500, 0.6250 } },
            new int[] { -1, -1, 2, 2, 2, 2 }),
        // element 7 (inverted bounds, normalised; duplicates element -1)
        new Box(
            0.18750,
            0.25000,
            0.25000,
            0.68750,
            0.37500,
            0.75000,
            -1,
            new double[][] { null, null, new double[] { 0.1875, 0.5000, 0.8125, 0.6250 },
                new double[] { 0.1875, 0.5000, 0.8125, 0.6250 }, new double[] { 0.2500, 0.5000, 0.7500, 0.6250 },
                new double[] { 0.2500, 0.5000, 0.7500, 0.6250 } },
            new int[] { -1, -1, 2, 2, 2, 2 }), };

    public static final Box[] UPPER = {
        // element 0
        new Box(
            0.18750,
            0.00000,
            0.68750,
            0.81250,
            0.31250,
            0.81250,
            -1,
            new double[][] { null, new double[] { 1.0000, 0.1250, 0.2500, 0.0000 },
                new double[] { 0.1875, 0.1875, 0.8125, 0.5000 }, new double[] { 0.1875, 0.1875, 0.8125, 0.5000 },
                new double[] { 0.1250, 0.1250, 0.0000, 0.4375 }, new double[] { 0.2500, 0.1250, 0.1250, 0.4375 } },
            new int[] { -1, 2, 0, 0, 2, 2 }), };
}
