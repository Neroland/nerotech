package za.co.neroland.nerotech.machine;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.machine.AbstractMachineBlockEntity;

import za.co.neroland.nerotech.registry.ModBlockEntities;

/** Battery Bank block (Stage D) — directional, ticks its {@link BatteryBankBlockEntity}. */
public class BatteryBankBlock extends NeroTechMachineBlock {

    public static final MapCodec<BatteryBankBlock> CODEC = simpleCodec(BatteryBankBlock::new);

    public BatteryBankBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<BatteryBankBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BatteryBankBlockEntity(pos, state);
    }

    @Override
    protected BlockEntityType<? extends AbstractMachineBlockEntity> machineType() {
        return ModBlockEntities.BATTERY_BANK.get();
    }
}
