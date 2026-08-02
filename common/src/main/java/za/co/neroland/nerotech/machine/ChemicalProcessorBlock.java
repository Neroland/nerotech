package za.co.neroland.nerotech.machine;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.machine.AbstractMachineBlockEntity;

import za.co.neroland.nerotech.registry.ModBlockEntities;

/** Chemical Processor block — directional, ticks its {@link ChemicalProcessorBlockEntity}. */
public class ChemicalProcessorBlock extends NeroTechMachineBlock {

    public static final MapCodec<ChemicalProcessorBlock> CODEC = simpleCodec(ChemicalProcessorBlock::new);

    public ChemicalProcessorBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<ChemicalProcessorBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ChemicalProcessorBlockEntity(pos, state);
    }

    @Override
    protected BlockEntityType<? extends AbstractMachineBlockEntity> machineType() {
        return ModBlockEntities.CHEMICAL_PROCESSOR.get();
    }
}
