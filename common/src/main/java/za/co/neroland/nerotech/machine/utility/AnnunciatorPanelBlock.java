package za.co.neroland.nerotech.machine.utility;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.machine.AbstractMachineBlockEntity;
import za.co.neroland.nerolandcore.registry.BlockCodecs;

import za.co.neroland.nerotech.machine.NeroTechMachineBlock;

/**
 * Annunciator Panel block: a thin wall panel (model {@code [1,1,14]->[15,15,16]}, front = facing).
 * Crouch-use with an empty hand cycles the comparator channel (decision 013); a plain use opens the
 * status screen. Comparators read {@link AnnunciatorPanelBlockEntity#comparatorSignal()}.
 */
public class AnnunciatorPanelBlock extends NeroTechMachineBlock {

    public static final MapCodec<AnnunciatorPanelBlock> CODEC = BlockCodecs.simple(AnnunciatorPanelBlock::new);

    private static final VoxelShape NORTH = Block.box(1, 1, 14, 15, 15, 16);
    private static final VoxelShape SOUTH = Block.box(1, 1, 0, 15, 15, 2);
    private static final VoxelShape EAST = Block.box(0, 1, 1, 2, 15, 15);
    private static final VoxelShape WEST = Block.box(14, 1, 1, 16, 15, 15);

    public AnnunciatorPanelBlock(Properties properties) {
        super(properties);
    }

    protected MapCodec<AnnunciatorPanelBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(FACING);
        return switch (facing) {
            case SOUTH -> SOUTH;
            case EAST -> EAST;
            case WEST -> WEST;
            default -> NORTH;
        };
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hit) {
        if (player.isShiftKeyDown()) {
            noteOperator(level, pos, player);
            if (!level.isClientSide() && level.getBlockEntity(pos) instanceof AnnunciatorPanelBlockEntity panel
                    && player instanceof ServerPlayer serverPlayer) {
                int channel = panel.cycleChannel();
                serverPlayer.sendSystemMessage(Component.translatable("gui.nerotech.annunciator_panel.channel",
                        Component.translatable("gui.nerotech.annunciator_panel.channel."
                                + AnnunciatorMath.channelId(channel))), true);
            }
            return InteractionResult.SUCCESS;
        }
        return super.useWithoutItem(state, level, pos, player, hit);
    }

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
        return level.getBlockEntity(pos) instanceof AnnunciatorPanelBlockEntity panel ? panel.comparatorSignal() : 0;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AnnunciatorPanelBlockEntity(pos, state);
    }

    @Override
    protected BlockEntityType<? extends AbstractMachineBlockEntity> machineType() {
        return UtilityContent.ANNUNCIATOR_PANEL_BE.get();
    }
}
