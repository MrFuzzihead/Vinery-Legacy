package com.mrfuzzihead.vinery.core.block;

import com.mrfuzzihead.vinery.client.render.BoxRenderer.Box;

/**
 * The small wine rack — four bottle slots, taken from the 1.21 {@code template_wine_rack_2} model.
 *
 * <p>
 * A single column of four cubbies framed by two diagonal braces. The braces are the port's first
 * genuinely rotated geometry; {@link WineRackGeometry#SMALL} explains how they are drawn.
 *
 * <p>
 * The four bottle slots are Phase 3 work; this increment brings up the block and its geometry.
 *
 * @see WineRackBlock for the facing, drop and icon behaviour shared with the other rack sizes
 */
public class FourBottleStorageBlock extends WineRackBlock {

    public FourBottleStorageBlock(String sideTexture, String topTexture) {
        super(sideTexture, topTexture);
    }

    @Override
    public Box[] geometry() {
        return WineRackGeometry.SMALL;
    }
}
