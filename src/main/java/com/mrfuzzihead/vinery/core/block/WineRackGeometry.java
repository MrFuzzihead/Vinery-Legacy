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
    /**
     * The small rack, from `template_wine_rack_2`: four bottle slots.
     *
     * <p>
     * The first shape in the port that is not purely axis-aligned. Five elements are plain boxes;
     * the remaining two are a 1px-thick diagonal brace each, rotated -45 degrees about z, which
     * together form an X across the face.
     *
     * <p>
     * The braces are transcribed as unit steps rather than true rotated quads. Two things make this
     * faithful enough rather than a compromise: the diagonal is exactly 45 degrees and exactly one
     * pixel thick, so a staircase of unit boxes lands on the same pixels a true diagonal would
     * occupy — which is how a diagonal is drawn in pixel art anyway. The alternative was to issue
     * the quads by hand, which is the path BACKPORT_PLAN.md section 5.3 documents as failing
     * silently in four separate ways.
     *
     * <p>
     * Rotating about an element's own centre is wrong here and sends the brace outside the block:
     * Minecraft rotates about the element's `rotation.origin`, which these two elements set
     * explicitly. With the correct pivot the braces run (0.75,0.93)-(15.25,15.42) and
     * (0.93,15.42)-(15.42,0.93), both inside the block.
     */
    public static final com.mrfuzzihead.vinery.client.render.BoxRenderer.Box[] SMALL = {
        // Top panel (1,15,0 -> 15,16,16)
        new Box(1, 15, 0, 15, 16, 16, TOP_TEXTURE, true),
        // Back panel (1,1,15 -> 15,15,16)
        new Box(1, 1, 15, 15, 15, 16, SIDE_TEXTURE, true),
        // Left post (0,0,0 -> 1,16,16)
        new Box(0, 0, 0, 1, 16, 16, SIDE_TEXTURE, true),
        // Right post (15,0,0 -> 16,16,16)
        new Box(15, 0, 0, 16, 16, 16, SIDE_TEXTURE, true),
        // Bottom panel (1,0,0 -> 15,1,16)
        new Box(1, 0, 0, 15, 1, 16, SIDE_TEXTURE, true),

        // Diagonal brace, bottom-left to top-right (element 5, 15 unit steps).
        new Box(0.75, 0.93, 0.5, 1.75, 1.93, 15.5, TOP_TEXTURE, true),
        new Box(1.75, 1.93, 0.5, 2.75, 2.93, 15.5, TOP_TEXTURE, true),
        new Box(2.75, 2.93, 0.5, 3.75, 3.93, 15.5, TOP_TEXTURE, true),
        new Box(3.75, 3.93, 0.5, 4.75, 4.93, 15.5, TOP_TEXTURE, true),
        new Box(4.75, 4.93, 0.5, 5.75, 5.93, 15.5, TOP_TEXTURE, true),
        new Box(5.75, 5.93, 0.5, 6.75, 6.93, 15.5, TOP_TEXTURE, true),
        new Box(6.75, 6.93, 0.5, 7.75, 7.93, 15.5, TOP_TEXTURE, true),
        new Box(7.75, 7.93, 0.5, 8.75, 8.93, 15.5, TOP_TEXTURE, true),
        new Box(8.75, 8.93, 0.5, 9.75, 9.93, 15.5, TOP_TEXTURE, true),
        new Box(9.75, 9.93, 0.5, 10.75, 10.93, 15.5, TOP_TEXTURE, true),
        new Box(10.75, 10.93, 0.5, 11.75, 11.93, 15.5, TOP_TEXTURE, true),
        new Box(11.75, 11.93, 0.5, 12.75, 12.93, 15.5, TOP_TEXTURE, true),
        new Box(12.75, 12.93, 0.5, 13.75, 13.93, 15.5, TOP_TEXTURE, true),
        new Box(13.75, 13.93, 0.5, 14.75, 14.93, 15.5, TOP_TEXTURE, true),
        new Box(14.75, 14.93, 0.5, 15.75, 15.93, 15.5, TOP_TEXTURE, true),

        // Diagonal brace, top-left to bottom-right (element 6, 15 unit steps).
        new Box(0.93, 14.42, 0.75, 1.93, 15.42, 15.75, TOP_TEXTURE, true),
        new Box(1.93, 13.42, 0.75, 2.93, 14.42, 15.75, TOP_TEXTURE, true),
        new Box(2.93, 12.42, 0.75, 3.93, 13.42, 15.75, TOP_TEXTURE, true),
        new Box(3.93, 11.42, 0.75, 4.93, 12.42, 15.75, TOP_TEXTURE, true),
        new Box(4.93, 10.42, 0.75, 5.93, 11.42, 15.75, TOP_TEXTURE, true),
        new Box(5.93, 9.42, 0.75, 6.93, 10.42, 15.75, TOP_TEXTURE, true),
        new Box(6.93, 8.42, 0.75, 7.93, 9.42, 15.75, TOP_TEXTURE, true),
        new Box(7.93, 7.42, 0.75, 8.93, 8.42, 15.75, TOP_TEXTURE, true),
        new Box(8.93, 6.42, 0.75, 9.93, 7.42, 15.75, TOP_TEXTURE, true),
        new Box(9.93, 5.42, 0.75, 10.93, 6.42, 15.75, TOP_TEXTURE, true),
        new Box(10.93, 4.42, 0.75, 11.93, 5.42, 15.75, TOP_TEXTURE, true),
        new Box(11.93, 3.42, 0.75, 12.93, 4.42, 15.75, TOP_TEXTURE, true),
        new Box(12.93, 2.42, 0.75, 13.93, 3.42, 15.75, TOP_TEXTURE, true),
        new Box(13.93, 1.42, 0.75, 14.93, 2.42, 15.75, TOP_TEXTURE, true),
        new Box(14.93, 0.42, 0.75, 15.93, 1.42, 15.75, TOP_TEXTURE, true), };
}
