package za.co.neroland.nerotech.machine;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.machine.AbstractMachineBlockEntity;
import za.co.neroland.nerolandcore.progression.CoreGates;

import za.co.neroland.nerotech.registry.ModBlockEntities;

/** Advanced Ore Processor block — orbit-gated, ticks its {@link AdvancedOreProcessorBlockEntity}. */
public class AdvancedOreProcessorBlock extends GatedMachineBlock {

    public static final MapCodec<AdvancedOreProcessorBlock> CODEC = simpleCodec(AdvancedOreProcessorBlock::new);

    public AdvancedOreProcessorBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<AdvancedOreProcessorBlock> codec() {
        return CODEC;
    }

    @Override
    protected Identifier requiredGate() {
        return CoreGates.REACHED_ORBIT;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AdvancedOreProcessorBlockEntity(pos, state);
    }

    @Override
    protected BlockEntityType<? extends AbstractMachineBlockEntity> machineType() {
        return ModBlockEntities.ADVANCED_ORE_PROCESSOR.get();
    }
}
