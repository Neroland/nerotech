package za.co.neroland.nerotech.machine;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.machine.AbstractMachineBlockEntity;

import za.co.neroland.nerotech.registry.ModBlockEntities;

/** Analytics Terminal block — directional, ticks its {@link AnalyticsTerminalBlockEntity}. */
public class AnalyticsTerminalBlock extends NeroTechMachineBlock {

    public static final MapCodec<AnalyticsTerminalBlock> CODEC = simpleCodec(AnalyticsTerminalBlock::new);

    public AnalyticsTerminalBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<AnalyticsTerminalBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AnalyticsTerminalBlockEntity(pos, state);
    }

    @Override
    protected BlockEntityType<? extends AbstractMachineBlockEntity> machineType() {
        return ModBlockEntities.ANALYTICS_TERMINAL.get();
    }
}
