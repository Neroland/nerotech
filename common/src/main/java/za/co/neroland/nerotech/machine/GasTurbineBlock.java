package za.co.neroland.nerotech.machine;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.registry.BlockCodecs;
import za.co.neroland.nerolandcore.machine.AbstractMachineBlockEntity;

import za.co.neroland.nerotech.registry.ModBlockEntities;

/** Gas Turbine block — directional, ticks its {@link GasTurbineBlockEntity}. */
public class GasTurbineBlock extends NeroTechMachineBlock {

    public static final MapCodec<GasTurbineBlock> CODEC = BlockCodecs.simple(GasTurbineBlock::new);

    public GasTurbineBlock(Properties properties) {
        super(properties);
    }

    protected MapCodec<GasTurbineBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GasTurbineBlockEntity(pos, state);
    }

    @Override
    protected BlockEntityType<? extends AbstractMachineBlockEntity> machineType() {
        return ModBlockEntities.GAS_TURBINE.get();
    }
}
