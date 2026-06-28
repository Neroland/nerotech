package za.co.neroland.nerotech.machine;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.machine.AbstractMachineBlockEntity;

import za.co.neroland.nerotech.registry.ModBlockEntities;

/** Ore Processor block — directional, ticks its {@link OreProcessorBlockEntity}. */
public class OreProcessorBlock extends NeroTechMachineBlock {

    public static final MapCodec<OreProcessorBlock> CODEC = simpleCodec(OreProcessorBlock::new);

    public OreProcessorBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<OreProcessorBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new OreProcessorBlockEntity(pos, state);
    }

    @Override
    protected BlockEntityType<? extends AbstractMachineBlockEntity> machineType() {
        return ModBlockEntities.ORE_PROCESSOR.get();
    }
}
