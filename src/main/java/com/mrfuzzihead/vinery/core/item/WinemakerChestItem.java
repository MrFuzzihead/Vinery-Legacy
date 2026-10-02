package com.mrfuzzihead.vinery.core.item;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import org.jetbrains.annotations.NotNull;

import com.mrfuzzihead.vinery.core.registry.ArmorRegistryClient;

public class WinemakerChestItem extends ArmorItem {

    private final ResourceLocation chestplateTexture;

    public WinemakerChestItem(Holder<ArmorMaterial> armorMaterial, Type type, Properties properties,
        ResourceLocation chestplateTexture) {
        super(armorMaterial, type, properties);
        this.chestplateTexture = chestplateTexture;
    }

    public ResourceLocation getChestplateTexture() {
        return chestplateTexture;
    }

    @Override
    public @NotNull EquipmentSlot getEquipmentSlot() {
        return this.type.getSlot();
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, TooltipContext context, @NotNull List<Component> tooltip,
        @NotNull TooltipFlag flag) {
        if (Minecraft.getInstance().level != null && Minecraft.getInstance().level.isClientSide()) {
            ArmorRegistryClient.appendToolTip(tooltip);
        }
    }
}
