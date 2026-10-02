package com.mrfuzzihead.vinery.core.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import com.mrfuzzihead.vinery.core.registry.EntityTypeRegistry;
import org.jetbrains.annotations.NotNull;

public class DarkCherryHangingSignBlockEntity extends DarkCherrySignBlockEntity {

    public DarkCherryHangingSignBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(EntityTypeRegistry.MOD_HANGING_SIGN.get(), blockPos, blockState);
    }

    @Override
    public @NotNull BlockEntityType<?> getType() {
        return EntityTypeRegistry.MOD_HANGING_SIGN.get();
    }
}
