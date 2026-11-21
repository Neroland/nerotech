package za.co.neroland.nerotech.machine.multiblock;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.registry.BlockCodecs;

/**
 * Base for multiblock shell and structure blocks (Foundry Brick, Foundry Heat Port, Drill Derrick,
 * Europium Flux Lattice, Heat Pipe; machine contract, Classes): placing, removing or changing next to
 * one asks every nearby controller to revalidate through
 * {@link MultiblockController#requestRevalidate}, so structures validate on change and on cadence,
 * never by a per-tick world scan. No block entity, no player data.
 *
 * <p>Subclasses add their own state (a {@code lit} or {@code part} property) and must call
 * {@code super} from any of the three hooks they override.
 */
public class ShellBlock extends Block {

    public static final MapCodec<ShellBlock> CODEC = BlockCodecs.simple(ShellBlock::new);

    public ShellBlock(Properties properties) {
        super(properties);
    }

    // No @Override: 26.3 dropped Block#codec (the existing machine blocks keep it the same way).
    protected MapCodec<? extends ShellBlock> codec() {
        return CODEC;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!oldState.is(state.getBlock())) {
            MultiblockController.requestRevalidate(level, pos);
        }
    }

    //? if >=1.21.5 {
    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, net.minecraft.server.level.ServerLevel level,
            BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        MultiblockController.requestRevalidate(level, pos);
    }
    //?} else {
    /*@Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
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
        MultiblockController.requestRevalidate(level, pos);
    }
}
