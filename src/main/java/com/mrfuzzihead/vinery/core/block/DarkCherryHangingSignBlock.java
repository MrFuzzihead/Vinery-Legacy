package com.mrfuzzihead.vinery.core.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.CeilingHangingSignBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;
import com.mrfuzzihead.vinery.core.block.entity.DarkCherryHangingSignBlockEntity;
import org.jetbrains.annotations.NotNull;

public class DarkCherryHangingSignBlock extends CeilingHangingSignBlock {
    public DarkCherryHangingSignBlock(Properties properties, WoodType type) {
        super(type, properties);
    }

    @Override
    public @NotNull BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DarkCherryHangingSignBlockEntity(pos, state);
    }
}
