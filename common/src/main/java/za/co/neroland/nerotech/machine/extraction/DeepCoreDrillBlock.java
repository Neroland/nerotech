package za.co.neroland.nerotech.machine.extraction;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.machine.AbstractMachineBlockEntity;
import za.co.neroland.nerolandcore.registry.BlockCodecs;

import za.co.neroland.nerotech.machine.NeroTechMachineBlock;

/** Deep Core Drill controller — sits on a column of three Drill Derricks; ticks its block entity. */
public class DeepCoreDrillBlock extends NeroTechMachineBlock {

    public static final MapCodec<DeepCoreDrillBlock> CODEC = BlockCodecs.simple(DeepCoreDrillBlock::new);

    public DeepCoreDrillBlock(Properties properties) {
        super(properties);
    }

    protected MapCodec<DeepCoreDrillBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DeepCoreDrillBlockEntity(pos, state);
    }

    @Override
    protected BlockEntityType<? extends AbstractMachineBlockEntity> machineType() {
        return ExtractionContent.DEEP_CORE_DRILL_BE.get();
    }

    // Head wear is item data on the head, so breaking the drill needs no hook of its own: the contents drop
    // through NeroTechMachineBlock (pre-1.21.5) or vanilla's preRemoveSideEffects (1.21.5+).
}
