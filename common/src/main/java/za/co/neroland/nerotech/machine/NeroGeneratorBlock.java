package za.co.neroland.nerotech.machine;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.machine.AbstractMachineBlockEntity;

import za.co.neroland.nerotech.registry.ModBlockEntities;

/** Nero Generator block — directional, ticks its {@link NeroGeneratorBlockEntity}. */
public class NeroGeneratorBlock extends NeroTechMachineBlock {

    public static final MapCodec<NeroGeneratorBlock> CODEC = simpleCodec(NeroGeneratorBlock::new);

    public NeroGeneratorBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<NeroGeneratorBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NeroGeneratorBlockEntity(pos, state);
    }

    @Override
    protected BlockEntityType<? extends AbstractMachineBlockEntity> machineType() {
        return ModBlockEntities.NERO_GENERATOR.get();
    }
}
