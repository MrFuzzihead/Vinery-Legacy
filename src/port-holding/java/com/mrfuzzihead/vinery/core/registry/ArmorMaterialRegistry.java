package com.mrfuzzihead.vinery.core.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ArmorMaterials;

import com.mrfuzzihead.vinery.Vinery;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;

public class ArmorMaterialRegistry {

    private static final ArmorMaterial LEATHER = ArmorMaterials.LEATHER.value();
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS = DeferredRegister
        .create(Vinery.MOD_ID, Registries.ARMOR_MATERIAL);
    public static final RegistrySupplier<ArmorMaterial> WINEMAKER_ARMOR = ARMOR_MATERIALS.register(
        "winemaker",
        () -> new ArmorMaterial(
            LEATHER.defense(),
            LEATHER.enchantmentValue(),
            LEATHER.equipSound(),
            LEATHER.repairIngredient(),
            LEATHER.layers(),
            LEATHER.toughness(),
            LEATHER.knockbackResistance()));
}
