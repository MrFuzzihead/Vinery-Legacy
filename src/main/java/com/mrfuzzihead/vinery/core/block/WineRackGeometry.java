package com.mrfuzzihead.vinery.core.block;

import com.mrfuzzihead.vinery.client.render.BoxRenderer.Box;

/**
 * Geometry transcribed from the 1.21 wine rack templates.
 *
 * <p>
 * Kept apart from the block so the renderer and any future hit-box test read the same numbers,
 * and so the original JSONs under {@code assets/vinery/models/block/} stay the readable reference.
 *
 * <p>
 * Coordinates are converted from the model's 0-16 space to 0-1 block space by
 * {@link com.mrfuzzihead.vinery.client.render.BoxRenderer.Box}.
 *
 * <p>
 * Texture indices are {@link com.mrfuzzihead.vinery.core.block.NineBottleStorageBlock#SIDE_TEXTURE}
 * and {@link #TOP_TEXTURE} — the cabinet side and cabinet top textures the 1.21 models use for the
 * frame and the shelves respectively. 1.7.10's vanilla cube renderer draws one texture per box, so a
 * box that mixed both in the original model picks its dominant face; per-face fidelity is tracked as
 * a known simplification.
 */
public final class WineRackGeometry {

    private WineRackGeometry() {}

    /** Side texture index, matching NineBottleStorageBlock#faceIcons ordering. */
    public static final int SIDE_TEXTURE = 2;

    /** Top texture index. */
    public static final int TOP_TEXTURE = 4;

    /**
     * The big rack, from {@code template_wine_rack_1}: nine axis-aligned cuboids with no rotations —
     * two side posts, top, bottom and back panels, two vertical dividers and two horizontal shelves.
     * Those form a 3x3 grid of cubbies, which is where the nine bottle slots come from.
     */
    public static final com.mrfuzzihead.vinery.client.render.BoxRenderer.Box[] BIG = {
        // Top panel
        new Box(1, 15, 0, 15, 16, 16, TOP_TEXTURE, true),
        // Back panel
        new Box(1, 1, 15, 15, 15, 16, SIDE_TEXTURE, true),
        // Left post
        new Box(0, 0, 0, 1, 16, 16, SIDE_TEXTURE, true),
        // Right post
        new Box(15, 0, 0, 16, 16, 16, SIDE_TEXTURE, true),
        // Bottom panel
        new Box(1, 0, 0, 15, 1, 16, SIDE_TEXTURE, true),
        // Vertical divider, left slot
        new Box(5, 1, 0.875, 6, 15, 15.875, SIDE_TEXTURE, true),
        // Vertical divider, right slot
        new Box(10, 1, 0.875, 11, 15, 15.875, SIDE_TEXTURE, true),
        // Horizontal shelf, upper
        new Box(1.001, 9.75, 1.375, 15.001, 10.75, 15.375, SIDE_TEXTURE, true),
        // Horizontal shelf, lower
        new Box(1.001, 5, 1.375, 15.001, 6, 15.375, SIDE_TEXTURE, true), };
}
