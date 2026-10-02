package com.mrfuzzihead.vinery.core.block;

import net.minecraft.block.BlockTrapDoor;
import net.minecraft.block.material.Material;

/** Dark cherry trapdoor. 1.7.10 handles the four facings and both open/closed states. */
public class DarkCherryTrapDoorBlock extends BlockTrapDoor {

    public DarkCherryTrapDoorBlock() {
        super(Material.wood);
        setBlockTextureName("vinery:dark_cherry_trapdoor");
        setHardness(3.0F);
        setResistance(3.0F);
        setStepSound(soundTypeWood);
    }
}
