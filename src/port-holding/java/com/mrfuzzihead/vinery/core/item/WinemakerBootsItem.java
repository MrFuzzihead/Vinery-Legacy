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

public class WinemakerBootsItem extends ArmorItem {

    private final ResourceLocation bootsTexture;

    public WinemakerBootsItem(Holder<ArmorMaterial> armorMaterial, Type type, Properties properties,
        ResourceLocation bootsTexture) {
        super(armorMaterial, type, properties);
        this.bootsTexture = bootsTexture;
    }

    public ResourceLocation getBootsTexture() {
        return bootsTexture;
    }

    @Override
    public @NotNull EquipmentSlot getEquipmentSlot() {
        return this.type.getSlot();
    }

    @Override
    public void appendHoverText(ItemStack itemStack, TooltipContext tooltipContext, List<Component> list,
        TooltipFlag tooltipFlag) {
        if (Minecraft.getInstance().level != null && Minecraft.getInstance().level.isClientSide()) {
            ArmorRegistryClient.appendToolTip(list);
        }
    }
}
