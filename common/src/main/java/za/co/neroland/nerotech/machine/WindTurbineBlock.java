package za.co.neroland.nerotech.machine;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.machine.AbstractMachineBlockEntity;

import za.co.neroland.nerotech.registry.ModBlockEntities;

/** Wind Turbine block (Stage D) — directional, ticks its {@link WindTurbineBlockEntity}. */
public class WindTurbineBlock extends NeroTechMachineBlock {

    public static final MapCodec<WindTurbineBlock> CODEC = simpleCodec(WindTurbineBlock::new);

    public WindTurbineBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<WindTurbineBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WindTurbineBlockEntity(pos, state);
    }

    @Override
    protected BlockEntityType<? extends AbstractMachineBlockEntity> machineType() {
        return ModBlockEntities.WIND_TURBINE.get();
    }
}
