package com.mrfuzzihead.vinery.core.block;

import net.minecraft.block.BlockButtonWood;

/** Dark cherry button. 1.7.10's wooden button already carries the correct pressed/unpressed sound. */
public class DarkCherryButtonBlock extends BlockButtonWood {

    public DarkCherryButtonBlock() {
        setBlockTextureName("vinery:dark_cherry_planks");
        setHardness(0.5F);
        setStepSound(soundTypeWood);
    }
}
