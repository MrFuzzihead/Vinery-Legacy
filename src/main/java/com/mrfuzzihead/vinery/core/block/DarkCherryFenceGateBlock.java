package com.mrfuzzihead.vinery.core.block;

import net.minecraft.block.BlockFenceGate;

/** Dark cherry fence gate. 1.7.10 handles all four facings, both states and redstone support. */
public class DarkCherryFenceGateBlock extends BlockFenceGate {

    public DarkCherryFenceGateBlock() {
        setBlockTextureName("vinery:dark_cherry_planks");
        setHardness(2.0F);
        setResistance(3.0F);
        setStepSound(soundTypeWood);
    }
}
