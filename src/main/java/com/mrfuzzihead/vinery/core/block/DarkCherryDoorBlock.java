package com.mrfuzzihead.vinery.core.block;

import net.minecraft.block.BlockDoor;
import net.minecraft.block.material.Material;

/**
 * Dark cherry door.
 *
 * <p>
 * {@link BlockDoor#registerBlockIcons} derives its two icons from the texture name plus
 * {@code _upper} and {@code _lower}, so the textures were renamed to
 * {@code dark_cherry_door_upper} / {@code dark_cherry_door_lower} to line up with that convention.
 * Its icon fields are private, so overriding them from a subclass is not an option.
 */
public class DarkCherryDoorBlock extends BlockDoor {

    public DarkCherryDoorBlock() {
        super(Material.wood);
        setBlockTextureName("vinery:dark_cherry_door");
        setHardness(3.0F);
        setResistance(3.0F);
        setStepSound(soundTypeWood);
    }
}
