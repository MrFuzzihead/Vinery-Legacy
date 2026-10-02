package com.mrfuzzihead.vinery.core.block;

import net.minecraft.block.BlockPressurePlateWeighted;
import net.minecraft.block.material.Material;

/**
 * Dark cherry weighted pressure plate.
 *
 * <p>
 * 1.7.10's {@link BlockPressurePlateWeighted} takes the texture name as a constructor argument,
 * and its entity pressure check already accounts for an {@code EntityPlayer}'s food level, so a
 * hungry player presses the plate the way the 1.21 version intends.
 */
public class DarkCherryPressurePlateBlock extends BlockPressurePlateWeighted {

    public DarkCherryPressurePlateBlock() {
        super("vinery:dark_cherry_pressure_plate", Material.wood, 10);
        setHardness(0.5F);
        setStepSound(soundTypeWood);
    }
}
