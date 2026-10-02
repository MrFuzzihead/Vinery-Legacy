package com.mrfuzzihead.vinery.core.block.state.properties;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;
import com.mrfuzzihead.vinery.Vinery;

public class VineryWoodType {
    public static final WoodType DARK_CHERRY = WoodType.register(new WoodType(ResourceLocation.fromNamespaceAndPath(Vinery.MOD_ID, "dark_cherry").toString(), BlockSetType.OAK));
}
