package za.co.neroland.nerotech.machine.fusion;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.machine.AbstractMachineBlockEntity;
import za.co.neroland.nerolandcore.registry.BlockCodecs;

import za.co.neroland.nerotech.machine.NeroTechMachineBlock;
import za.co.neroland.nerotech.machine.multiblock.MultiblockController;

/**
 * Fuel Injector — a fusion reactor shell port, allowed in place of a casing. Directional like every
 * machine; placing, removing or changing next to it asks the nearby reactor to revalidate, exactly as a
 * shell block does, so the reactor learns of a new injector on change rather than by a per-tick scan.
 */
public class FuelInjectorBlock extends NeroTechMachineBlock {

    public static final MapCodec<FuelInjectorBlock> CODEC = BlockCodecs.simple(FuelInjectorBlock::new);

    public FuelInjectorBlock(Properties properties) {
        super(properties);
    }

    // No @Override: 26.3 dropped Block#codec (the existing machine blocks keep it the same way).
    protected MapCodec<FuelInjectorBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FuelInjectorBlockEntity(pos, state);
    }

    @Override
    protected BlockEntityType<? extends AbstractMachineBlockEntity> machineType() {
        return FusionContent.FUEL_INJECTOR_BE.get();
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
