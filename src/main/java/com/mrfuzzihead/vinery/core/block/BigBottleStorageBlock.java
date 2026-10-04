package com.mrfuzzihead.vinery.core.block;

import com.mrfuzzihead.vinery.client.render.BoxRenderer.Box;

/**
 * The mid wine rack — a cabinet that holds a single large bottle.
 *
 * <p>
 * Despite sitting between the 9-slot and 4-slot racks in size, this is not a bigger rack. The 1.21
 * {@code BigBottleStorageBlock} holds <em>one</em> large bottle (its 21 {@code size()} returns 1 and
 * it accepts {@code LARGE_BOTTLE}, where the other two accept only small ones), it opens with
 * shift-right-click, and its translation key reads "Acacia Wine Rack" rather than "Mid Acacia Wine
 * Bottle Storage". The {@code _mid} registry name is just where it landed in the original mod's
 * naming, not a statement about capacity.
 *
 * <p>
 * That makes {@code template_wine_rack_3_closed} a **full opaque cube** — which is correct, not the
 * broken export it was first taken for. A closed cabinet is a solid cube. Only
 * {@code template_wine_rack_3_open} is unusable: it still contains that same cube, so opening it
 * reveals nothing. The door and the open/close behaviour are therefore Phase 3 work, deferred until
 * the storage inventory exists and the {@code open} state has something to mean; with no block
 * entity and no GUI the open state would be pure decoration, and the interior geometry it needs
 * cannot be recovered from these assets.
 *
 * <p>
 * The cube is drawn as six slabs rather than one, because 1.7.10's cube renderer applies a single
 * texture per box and this cabinet needs three: {@code cabinet_front} on the door, {@code cabinet_top}
 * on top, {@code cabinet_side} elsewhere. The slabs meet edge to edge with no two same-facing faces
 * coplanar, so nothing z-fights.
 *
 * @see WineRackBlock for the facing, drop and icon behaviour shared with the other rack sizes
 */
public class BigBottleStorageBlock extends WineRackBlock {

    public BigBottleStorageBlock(String sideTexture, String topTexture, String frontTexture) {
        super(sideTexture, topTexture, frontTexture);
    }

    @Override
    public Box[] geometry() {
        return WineRackGeometry.CABINET;
    }

    /**
     * The door is cut out as open slats — nine of the ten woods ship a \`cabinet_front\` with 36–95
     * fully transparent pixels out of 256 — so it needs alpha testing or the slats blend through to
     * whatever is behind the block.
     */
    @Override
    public boolean usesAlphaTest() {
        return true;
    }
}
