package za.co.neroland.nerotech.machine;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.machine.AbstractMachineBlockEntity;

import za.co.neroland.nerotech.registry.ModBlockEntities;

/**
 * Coolant Pump block — ticks its {@link CoolantPumpBlockEntity} and, on any neighbour change, tells
 * it to rescan for Radiators (so adding or breaking a radiator run registers immediately instead of
 * riding a stale cache). The pump has no GUI, so right-click passes straight through.
 */
public class CoolantPumpBlock extends NeroTechMachineBlock {

    public static final MapCodec<CoolantPumpBlock> CODEC = simpleCodec(CoolantPumpBlock::new);

    public CoolantPumpBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<CoolantPumpBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CoolantPumpBlockEntity(pos, state);
    }

    @Override
    protected BlockEntityType<? extends AbstractMachineBlockEntity> machineType() {
        return ModBlockEntities.COOLANT_PUMP.get();
    }

    /** No menu: never swallow the interaction the way a GUI machine does. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hit) {
        return InteractionResult.PASS;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
            @Nullable net.minecraft.world.level.redstone.Orientation orientation, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, orientation, movedByPiston);
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof CoolantPumpBlockEntity pump) {
            pump.invalidateRadiators();
        }
    }
}
