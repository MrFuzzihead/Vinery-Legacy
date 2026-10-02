package com.mrfuzzihead.vinery.core.block;

import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;

public class FacingBlock extends HorizontalDirectionalBlock {

    public FacingBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(
            this.defaultBlockState()
                .setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return simpleCodec(FacingBlock::new);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return this.defaultBlockState()
            .setValue(
                FACING,
                ctx.getHorizontalDirection()
                    .getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
}
