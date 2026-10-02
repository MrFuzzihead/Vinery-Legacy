package com.mrfuzzihead.vinery.core.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import com.mrfuzzihead.vinery.core.registry.EntityTypeRegistry;

public class StoragePotBlockEntity extends CabinetBlockEntity {

    public StoragePotBlockEntity(BlockPos pos, BlockState state) {
        super(EntityTypeRegistry.STORAGE_POT_ENTITY.get(), pos, state);
    }

}
