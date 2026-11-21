package za.co.neroland.nerotech.machine.foundry;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.machine.AbstractMachineBlockEntity;
import za.co.neroland.nerolandcore.registry.BlockCodecs;

import za.co.neroland.nerotech.machine.NeroTechMachineBlock;

/** Blast Foundry controller block: directional, sits in the centre of one wall of its brick shell. */
public class BlastFoundryBlock extends NeroTechMachineBlock {

    public static final MapCodec<BlastFoundryBlock> CODEC = BlockCodecs.simple(BlastFoundryBlock::new);

    public BlastFoundryBlock(Properties properties) {
        super(properties);
    }

    protected MapCodec<BlastFoundryBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlastFoundryBlockEntity(pos, state);
    }

    @Override
    protected BlockEntityType<? extends AbstractMachineBlockEntity> machineType() {
        return FoundryContent.BLAST_FOUNDRY_BE.get();
    }

    //? if <1.21.5 {
    /*// Before 1.21.5 removal side effects run from the block (block entities get preRemoveSideEffects later).
    @Override
    protected void onRemove(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, BlockState newState,
            boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof BlastFoundryBlockEntity be) {
            be.preRemoveSideEffects(pos, state);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
    *///?}
}
