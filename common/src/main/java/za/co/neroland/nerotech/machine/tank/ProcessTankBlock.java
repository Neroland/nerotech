package za.co.neroland.nerotech.machine.tank;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.machine.AbstractMachineBlockEntity;
import za.co.neroland.nerolandcore.registry.BlockCodecs;

import za.co.neroland.nerotech.machine.NeroTechMachineBlock;
import za.co.neroland.nerotech.registry.ModBlockEntities;

/**
 * Process Tank block: stacks into columns (see {@link ProcessTankBlockEntity}), reports its column on
 * an empty-handed right-click, and drives a comparator from the column's fill. It has no menu.
 */
public class ProcessTankBlock extends NeroTechMachineBlock {

    public static final MapCodec<ProcessTankBlock> CODEC = BlockCodecs.simple(ProcessTankBlock::new);

    /**
     * Where this tank sits in its column (machine contract C2): the models drop the inner rings where
     * tanks join. Set by the block entity when its column resolves, only on a real change.
     */
    public enum Column implements StringRepresentable {
        SINGLE,
        BOTTOM,
        MIDDLE,
        TOP;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }

        /** The part for member {@code index} of a column of {@code size} tanks (bottom-up). */
        public static Column of(int index, int size) {
            if (size <= 1) {
                return SINGLE;
            }
            if (index <= 0) {
                return BOTTOM;
            }
            return index >= size - 1 ? TOP : MIDDLE;
        }
    }

    public static final EnumProperty<Column> COLUMN = EnumProperty.create("column", Column.class);

    @SuppressWarnings("this-escape")
    public ProcessTankBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(COLUMN, Column.SINGLE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(COLUMN);
    }

    protected MapCodec<ProcessTankBlock> codec() {
        return CODEC;
    }

    //? if <1.20.5 {
    /*@Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
            @Nullable net.minecraft.world.entity.LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof ProcessTankBlockEntity blockEntity) {
            blockEntity.readCarriedContents(stack);
            blockEntity.setChanged();
        }
    }
    *///?}

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ProcessTankBlockEntity(pos, state);
    }

    @Override
    protected BlockEntityType<? extends AbstractMachineBlockEntity> machineType() {
        return ModBlockEntities.PROCESS_TANK.get();
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof ProcessTankBlockEntity tank) {
            if (serverPlayer.isSecondaryUseActive() && !tank.columnMedium().isEmpty()
                    && level.mayInteract(serverPlayer, pos)) {
                // Sneak-use with an empty hand vents one tank's worth, so a wrong medium can be cleared.
                TankMedium vented = tank.columnMedium();
                long removed = tank.drainColumn(ProcessTankBlockEntity.CAPACITY_MB, false);
                serverPlayer.sendSystemMessage(Component.translatable("block.nerotech.process_tank.vented",
                        removed, ProcessTankBlockEntity.displayName(vented)), true);
                return InteractionResult.SUCCESS;
            }
            TankMedium medium = tank.columnMedium();
            Component message = medium.isEmpty()
                    ? Component.translatable("block.nerotech.process_tank.report_empty", tank.column().size())
                    : Component.translatable("block.nerotech.process_tank.report",
                            ProcessTankBlockEntity.displayName(medium), tank.columnAmount(),
                            tank.columnCapacity(), tank.column().size());
            serverPlayer.sendSystemMessage(message, true);
        }
        return InteractionResult.SUCCESS;
    }

    /** Any neighbour change may join or split columns: drop the cached column of the whole stack. */
    @Override
    //? if >=1.21.2 {
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
            @Nullable net.minecraft.world.level.redstone.Orientation orientation, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, orientation, movedByPiston);
    //?} else {
    /*protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
            BlockPos fromPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, fromPos, movedByPiston);
    *///?}
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof ProcessTankBlockEntity tank) {
            tank.invalidateStack();
        }
    }

    /**
     * A tank that still held a share when removal began drops its own item from
     * {@link ProcessTankBlockEntity#preRemoveSideEffects} (carrying whatever did not fit back into its
     * column), so the loot's plain tank is withheld and exactly one tank drops. Covers both orders
     * vanilla uses: a player break removes the block before rolling loot, an explosion rolls loot
     * first. Side-effect free.
     */
    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = super.getDrops(state, params);
        if (params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof ProcessTankBlockEntity tank
                && (tank.carrierDropped() || tank.ownAmount() > 0)) {
            drops = new ArrayList<>(drops);
            drops.removeIf(drop -> drop.is(asItem()));
        }
        return drops;
    }

    //? if <1.21.5 {
    /*// Before 1.21.5 removal side effects run from the block (block entities get preRemoveSideEffects later).
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof ProcessTankBlockEntity be) {
            be.preRemoveSideEffects(pos, state);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    *///?}
    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    //? if >=1.21.9 {
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
    //?} else {
    /*protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
    *///?}
        return level.getBlockEntity(pos) instanceof ProcessTankBlockEntity tank ? tank.comparatorSignal() : 0;
    }
}
