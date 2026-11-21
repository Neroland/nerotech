package za.co.neroland.nerotech.machine.heat;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.registry.BlockCodecs;

import za.co.neroland.nerotech.machine.multiblock.MultiblockController;
import za.co.neroland.nerotech.registry.ModSounds;

/**
 * Heat Pipe: conducts heat along a run between machines and Foundry Heat Ports (decision 012). No block
 * entity: a run of connected pipes resolves into a cached link set held by {@link HeatPipeNetwork}, and only
 * the run's anchor pipe ticks, on the exchange interval. Blockstate {@code heat} (0..3) shows the run's
 * temperature and changes only when that level changes; six booleans drive the connected multipart model.
 * A pipe at heat 2 or more hurts to stand on (config {@code heatPipeContactDamage}).
 */
public class HeatPipeBlock extends Block {

    public static final MapCodec<HeatPipeBlock> CODEC = BlockCodecs.simple(HeatPipeBlock::new);

    public static final IntegerProperty HEAT = IntegerProperty.create("heat", 0, HeatRunMath.MAX_LEVEL);
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;

    private static final VoxelShape[] SHAPES = buildShapes();

    public HeatPipeBlock(Properties properties) {
        super(properties);
        BlockState state = this.stateDefinition.any().setValue(HEAT, 0);
        for (Direction dir : Direction.values()) {
            state = state.setValue(property(dir), false);
        }
        registerDefaultState(state);
    }

    // No @Override: 26.3 dropped Block#codec (the existing machine blocks keep it the same way).
    protected MapCodec<? extends HeatPipeBlock> codec() {
        return CODEC;
    }

    /** The connection property for {@code dir}. */
    public static BooleanProperty property(Direction dir) {
        return switch (dir) {
            case NORTH -> NORTH;
            case EAST -> EAST;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            case UP -> UP;
            case DOWN -> DOWN;
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HEAT, NORTH, EAST, SOUTH, WEST, UP, DOWN);
    }

    /**
     * Client display tick: a level-3 (hottest) pipe ticks now and then as the metal moves (008 §8
     * {@code heat_pipe.tick}). Rare and random, played locally; reads only the blockstate.
     */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(HEAT) >= HeatRunMath.MAX_LEVEL && random.nextInt(80) == 0) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, ModSounds.HEAT_PIPE_TICK.get(),
                    SoundSource.BLOCKS, 0.35F, 0.9F + random.nextFloat() * 0.2F, false);
        }
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return connections(defaultBlockState(), context.getLevel(), context.getClickedPos());
    }

    /** {@code state} with its six connections recomputed from the neighbours (six reads, on change only). */
    private BlockState connections(BlockState state, Level level, BlockPos pos) {
        BlockState out = state;
        for (Direction dir : Direction.values()) {
            BlockPos at = pos.relative(dir);
            boolean connect = level.getBlockState(at).is(this) || HeatNodes.isEndpoint(level, at, dir.getOpposite());
            out = out.setValue(property(dir), connect);
        }
        return out;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int mask = 0;
        for (Direction dir : Direction.values()) {
            if (state.getValue(property(dir))) {
                mask |= 1 << dir.ordinal();
            }
        }
        return SHAPES[mask];
    }

    private static VoxelShape[] buildShapes() {
        VoxelShape core = Block.box(5, 5, 5, 11, 11, 11);
        VoxelShape[] arms = new VoxelShape[6];
        arms[Direction.DOWN.ordinal()] = Block.box(5, 0, 5, 11, 5, 11);
        arms[Direction.UP.ordinal()] = Block.box(5, 11, 5, 11, 16, 11);
        arms[Direction.NORTH.ordinal()] = Block.box(5, 5, 0, 11, 11, 5);
        arms[Direction.SOUTH.ordinal()] = Block.box(5, 5, 11, 11, 11, 16);
        arms[Direction.WEST.ordinal()] = Block.box(0, 5, 5, 5, 11, 11);
        arms[Direction.EAST.ordinal()] = Block.box(11, 5, 5, 16, 11, 11);
        VoxelShape[] shapes = new VoxelShape[64];
        for (int mask = 0; mask < 64; mask++) {
            VoxelShape shape = core;
            for (int i = 0; i < 6; i++) {
                if ((mask & (1 << i)) != 0) {
                    shape = Shapes.or(shape, arms[i]);
                }
            }
            shapes[mask] = shape;
        }
        return shapes;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!oldState.is(state.getBlock()) && level instanceof ServerLevel server) {
            HeatPipeNetwork.invalidate(server, pos, this);
            MultiblockController.requestRevalidate(level, pos);
        }
    }

    //? if >=1.21.5 {
    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos,
            boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        HeatPipeNetwork.index(level).remove(pos.asLong(), level.getGameTime());
        MultiblockController.requestRevalidate(level, pos);
    }
    //?} else {
    /*@Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel server) {
            HeatPipeNetwork.index(server).remove(pos.asLong(), server.getGameTime());
            MultiblockController.requestRevalidate(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
    *///?}

    @Override
    //? if >=1.21.2 {
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
            @Nullable net.minecraft.world.level.redstone.Orientation orientation, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, orientation, movedByPiston);
    //?} else {
    /*protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
            BlockPos fromPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, fromPos, movedByPiston);
    *///?}
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        BlockState updated = connections(state, level, pos);
        if (updated != state) {
            // Only a change in what the pipe touches changes the run: a neighbour that merely updates its
            // own state (a machine lighting up) costs six reads and nothing else.
            level.setBlock(pos, updated, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            HeatPipeNetwork.invalidate(server, pos, this);
            MultiblockController.requestRevalidate(level, pos);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        HeatPipeNetwork.tick(level, pos, this);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        HeatPipeNetwork.heal(level, pos, this);
    }

    /** A hot pipe hurts to stand on, like a magma block (sneaking avoids it; fire-immune mobs ignore it). */
    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (level instanceof ServerLevel server && entity instanceof LivingEntity living
                && !entity.isSteppingCarefully()
                && HeatRunMath.hurtsToTouch(state.getValue(HEAT), HeatSettings.heatPipeContactDamage())) {
            float amount = HeatSettings.heatPipeContactDamageAmount() * (state.getValue(HEAT) >= HeatRunMath.MAX_LEVEL
                    ? 2.0F : 1.0F);
            //? if >=1.21.2 {
            living.hurtServer(server, server.damageSources().hotFloor(), amount);
            //?} else {
            /*living.hurt(server.damageSources().hotFloor(), amount);
            *///?}
        }
        super.stepOn(level, pos, state, entity);
    }
}
