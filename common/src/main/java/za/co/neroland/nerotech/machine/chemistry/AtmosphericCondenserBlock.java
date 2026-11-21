package za.co.neroland.nerotech.machine.chemistry;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.machine.AbstractMachineBlockEntity;
import za.co.neroland.nerolandcore.registry.BlockCodecs;

import za.co.neroland.nerotech.machine.NeroTechMachineBlock;

/** The atmospheric condenser block — directional, ticks its {@link AtmosphericCondenserBlockEntity}. */
public class AtmosphericCondenserBlock extends NeroTechMachineBlock {

    public static final MapCodec<AtmosphericCondenserBlock> CODEC = BlockCodecs.simple(AtmosphericCondenserBlock::new);

    public AtmosphericCondenserBlock(Properties properties) {
        super(properties);
    }

    protected MapCodec<AtmosphericCondenserBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AtmosphericCondenserBlockEntity(pos, state);
    }

    @Override
    protected BlockEntityType<? extends AbstractMachineBlockEntity> machineType() {
        return ChemistryContent.ATMOSPHERIC_CONDENSER_BE.get();
    }
}
