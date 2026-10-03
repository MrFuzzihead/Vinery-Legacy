package com.mrfuzzihead.vinery.core.block;

import com.mrfuzzihead.vinery.client.render.BoxRenderer.Box;

/**
 * The big wine rack — nine bottle cubbies, taken from the 1.21 {@code template_wine_rack_1} model.
 *
 * <p>
 * That model is nine axis-aligned cuboids with no rotations: two side posts, top, bottom and back
 * panels, two vertical dividers and two horizontal shelves. The box list lives in
 * {@link WineRackGeometry#BIG} so the renderer and the collision box agree on one source of truth.
 *
 * <p>
 * The nine bottle slots are Phase 3 work; this increment brings up the block and its geometry.
 *
 * @see WineRackBlock for the facing, drop and icon behaviour shared with the other two rack sizes
 */
public class NineBottleStorageBlock extends WineRackBlock {

    /** Index into faceIcons for the cabinet side texture; used by WineRackGeometry. */
    public static final int SIDE_TEXTURE = 2;

    /** Index into faceIcons for the cabinet top texture. */
    public static final int TOP_TEXTURE = 4;

    public NineBottleStorageBlock(String sideTexture, String topTexture) {
        super(sideTexture, topTexture);
    }

    @Override
    public Box[] geometry() {
        return WineRackGeometry.BIG;
    }
}
