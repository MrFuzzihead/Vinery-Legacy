package com.mrfuzzihead.vinery.core.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockStairs;

/**
 * Dark cherry stairs.
 *
 * <p>
 * 1.7.10's {@link BlockStairs} hardcodes sides 0/1 for bottom and top and side 2 for the
 * verticals, and {@link #getIcon} delegates straight to the block handed to the constructor — so
 * passing the planks block gives correctly textured stairs with no texture override at all. That
 * also matches the 1.21 model, which uses {@code dark_cherry_planks} for all three faces.
 */
public class DarkCherryStairsBlock extends BlockStairs {

    public DarkCherryStairsBlock(Block baseBlock) {
        super(baseBlock, 0);
    }
}
