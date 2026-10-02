package com.mrfuzzihead.vinery.client.render.block.storage;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mrfuzzihead.vinery.core.block.entity.StorageBlockEntity;

public interface StorageTypeRenderer {

    void render(StorageBlockEntity entity, PoseStack matrices, MultiBufferSource vertexConsumers,
        NonNullList<ItemStack> itemStacks);
}
