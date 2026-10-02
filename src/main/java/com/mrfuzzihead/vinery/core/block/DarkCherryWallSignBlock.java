package com.mrfuzzihead.vinery.core.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;
import com.mrfuzzihead.vinery.core.block.entity.DarkCherrySignBlockEntity;

public class DarkCherryWallSignBlock extends WallSignBlock {
    public DarkCherryWallSignBlock(Properties properties, WoodType type) {
        super(type, properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new DarkCherrySignBlockEntity(pPos, pState);
    }
}
