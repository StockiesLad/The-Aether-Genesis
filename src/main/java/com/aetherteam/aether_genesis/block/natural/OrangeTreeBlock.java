package com.aetherteam.aether_genesis.block.natural;

import com.aetherteam.aether.block.AetherBlockStateProperties;
import com.aetherteam.aether.block.natural.AetherBushBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.ForgeHooks;

import javax.annotation.Nullable;

public class OrangeTreeBlock extends AetherBushBlock implements BonemealableBlock {
    public static final int SINGLE_AGE_MAX = 1;
    public static final int DOUBLE_AGE_MAX = 4;

    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    public static final IntegerProperty AGE = BlockStateProperties.AGE_4;

    private static final VoxelShape AGE_0_BOTTOM_SHAPE = Block.box(4.0D, 0.0D, 4.0D, 12.0D, 10.0D, 12.0D);
    private static final VoxelShape AGE_1_BOTTOM_SHAPE = Block.box(4.0D, 0.0D, 4.0D, 12.0D, 15.0D, 12.0D);
    private static final VoxelShape AGE_2_TOP_SHAPE = Block.box(2.0D, 0.0D, 2.0D, 14.0D, 10.0D, 14.0D);
    private static final VoxelShape GENERAL_TOP_SHAPE = Block.box(2.0D, 0.0D, 2.0D, 14.0D, 14.0D, 14.0D);
    private static final VoxelShape GENERAL_BOTTOM_SHAPE = Block.box(2.0D, 0.0D, 2.0D, 14.0D, 16.0D, 14.0D);

    public OrangeTreeBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(AetherBlockStateProperties.DOUBLE_DROPS, false).setValue(HALF, DoubleBlockHalf.LOWER).setValue(AGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AetherBlockStateProperties.DOUBLE_DROPS, HALF, AGE);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int age = state.getValue(AGE);
        DoubleBlockHalf doubleBlockHalf = state.getValue(HALF);
        if (doubleBlockHalf == DoubleBlockHalf.LOWER) {
            switch(age) {
                case 0 -> {
                    return AGE_0_BOTTOM_SHAPE;
                }
                case 1 -> {
                    return AGE_1_BOTTOM_SHAPE;
                }
                default -> {
                    return GENERAL_BOTTOM_SHAPE;
                }
            }
        } else {
            if (age == 2) {
                return AGE_2_TOP_SHAPE;
            } else {
                return GENERAL_TOP_SHAPE;
            }
        }
    }

    @Override
    public BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor level, BlockPos currentPos, BlockPos facingPos) {
        DoubleBlockHalf doubleBlockHalf = state.getValue(HALF);
        if (facing.getAxis() != Direction.Axis.Y || doubleBlockHalf == DoubleBlockHalf.LOWER != (facing == Direction.UP) || facingState.is(this) && facingState.getValue(HALF) != doubleBlockHalf) {
            return doubleBlockHalf == DoubleBlockHalf.LOWER && facing == Direction.DOWN && !state.canSurvive(level, currentPos) ? Blocks.AIR.defaultBlockState() : super.updateShape(state, facing, facingState, level, currentPos, facingPos);
        } else {
            return Blocks.AIR.defaultBlockState();
        }
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return state.getValue(AGE) < DOUBLE_AGE_MAX;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int age = state.getValue(AGE);
        if (age < DOUBLE_AGE_MAX && level.getRawBrightness(pos.above(), 0) >= 9 && ForgeHooks.onCropsGrowPre(level, pos, state, random.nextInt(85) == 0)) {
            age += 1;
            if (age > SINGLE_AGE_MAX) {
                if (level.isEmptyBlock(pos.above())) {
                    BlockState blockState = state.setValue(AetherBlockStateProperties.DOUBLE_DROPS, state.getValue(AetherBlockStateProperties.DOUBLE_DROPS)).setValue(AGE, age);
                    OrangeTreeBlock.placeAt(level, blockState, pos, 2);
                    level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(blockState));
                    ForgeHooks.onCropsGrowPost(level, pos, state);
                }
            } else {
                BlockState blockState = state.setValue(AetherBlockStateProperties.DOUBLE_DROPS, state.getValue(AetherBlockStateProperties.DOUBLE_DROPS)).setValue(AGE, age);
                level.setBlock(pos, blockState, 2);
                level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(blockState));
                ForgeHooks.onCropsGrowPost(level, pos, state);
            }
        }
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (state.getValue(HALF) != DoubleBlockHalf.UPPER) {
            return super.canSurvive(state, level, pos);
        } else {
            BlockState blockstate = level.getBlockState(pos.below());
            if (state.getBlock() != this) return super.canSurvive(state, level, pos);
            return blockstate.is(this) && blockstate.getValue(HALF) == DoubleBlockHalf.LOWER;
        }
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        int age = state.getValue(AGE);
        if (age > SINGLE_AGE_MAX) {
            if (!level.isClientSide()) {
                if (player.isCreative() && age < DOUBLE_AGE_MAX) {
                    preventCreativeDropFromBottomPart(level, pos, state, player);
                } else {
                    dropResources(state, level, pos, null, player, player.getMainHandItem());
                }
            }
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, ItemStack stack) {
        DoubleBlockHalf doubleBlockHalf = state.getValue(HALF);
        int age = state.getValue(AGE);
        if (age > SINGLE_AGE_MAX) {
            super.playerDestroy(level, player, pos, Blocks.AIR.defaultBlockState(), blockEntity, stack);
            if (age == DOUBLE_AGE_MAX) {
                if (doubleBlockHalf == DoubleBlockHalf.UPPER) {
                    pos = pos.below();
                }
                OrangeTreeBlock.placeAt(level, state.setValue(AetherBlockStateProperties.DOUBLE_DROPS, state.getValue(AetherBlockStateProperties.DOUBLE_DROPS)).setValue(AGE, age - 1), pos, 2);
            }
        } else {
            super.playerDestroy(level, player, pos, state, blockEntity, stack);
        }
    }

    @Override
    public void onBlockExploded(BlockState state, Level level, BlockPos pos, Explosion explosion) {
        super.onBlockExploded(state, level, pos, explosion);
        int age = state.getValue(AGE);
        if (age == DOUBLE_AGE_MAX) {
            OrangeTreeBlock.placeAt(level, state.setValue(AetherBlockStateProperties.DOUBLE_DROPS, state.getValue(AetherBlockStateProperties.DOUBLE_DROPS)).setValue(AGE, age - 1), pos, 2);
        }
    }

    protected static void preventCreativeDropFromBottomPart(Level level, BlockPos pos, BlockState state, Player player) {
        DoubleBlockHalf doubleBlockHalf = state.getValue(HALF);
        if (doubleBlockHalf == DoubleBlockHalf.UPPER) {
            BlockPos blockPos = pos.below();
            BlockState blockState = level.getBlockState(blockPos);
            if (blockState.is(state.getBlock()) && blockState.getValue(HALF) == DoubleBlockHalf.LOWER) {
                level.setBlock(blockPos, Blocks.AIR.defaultBlockState(), 35);
                level.levelEvent(player, 2001, blockPos, Block.getId(blockState));
            }
        }
    }

    public static void placeAt(LevelAccessor level, BlockState state, BlockPos pos, int flags) {
        BlockPos blockPos = pos.above();
        level.setBlock(pos, state.setValue(HALF, DoubleBlockHalf.LOWER), flags);
        level.setBlock(blockPos, state.setValue(HALF, DoubleBlockHalf.UPPER), flags);
    }

    @Override
    public long getSeed(BlockState state, BlockPos pos) {
        return Mth.getSeed(pos.getX(), pos.below(state.getValue(HALF) == DoubleBlockHalf.LOWER ? 0 : 1).getY(), pos.getZ());
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, boolean isClient) {
        return state.getValue(AGE) < DOUBLE_AGE_MAX;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        DoubleBlockHalf doubleBlockHalf = state.getValue(HALF);
        int i = Math.min(DOUBLE_AGE_MAX, state.getValue(AGE) + 1);
        if (i > SINGLE_AGE_MAX && (level.isEmptyBlock(pos.above()) || level.getBlockState(pos.above()).is(this))) {
            if (doubleBlockHalf == DoubleBlockHalf.UPPER) {
                pos = pos.below();
            }
            OrangeTreeBlock.placeAt(level, state.setValue(AetherBlockStateProperties.DOUBLE_DROPS, state.getValue(AetherBlockStateProperties.DOUBLE_DROPS)).setValue(AGE, i), pos, 2);
        } else {
            level.setBlock(pos, state.setValue(AGE, i), 2);
        }
    }
}
