package za.co.neroland.nerotech.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.machine.AbstractMachineBlockEntity;
import za.co.neroland.nerolandcore.progression.CoreGates;
import za.co.neroland.nerolandcore.progression.ProgressionGates;

import za.co.neroland.nerotech.config.NeroTechConfig;
import za.co.neroland.nerotech.item.ConfiguratorItem;
import za.co.neroland.nerotech.menu.MachineMenu;
import za.co.neroland.nerotech.network.MachineMenuPosPayload;
import za.co.neroland.nerotech.network.NeroTechNetwork;

/**
 * Shared base for every NeroTech Tier-1 machine block. Centralises the directional state, the
 * server-side menu open, the block-entity ticker (driving Core's
 * {@link AbstractMachineBlockEntity#tick}), and the canonical opening of
 * {@link CoreGates#INDUSTRIAL_POWER} the first time a player places a NeroTech machine — NeroTech is
 * the canonical opener of that gate.
 *
 * <p>Subclasses supply their concrete block-entity ({@link #newBlockEntity}), its type
 * ({@link #machineType()}) and a {@link #codec()}.
 */
public abstract class NeroTechMachineBlock extends BaseEntityBlock {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    @SuppressWarnings("this-escape")
    protected NeroTechMachineBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    /** The concrete machine block-entity type, for the ticker. */
    protected abstract BlockEntityType<? extends AbstractMachineBlockEntity> machineType();

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    /**
     * The Configurator wrench must reach its own {@code useOn} instead of opening the machine GUI:
     * returning plain {@code PASS} (not {@code TRY_WITH_EMPTY_HAND}) skips {@link #useWithoutItem}
     * and lets the interaction fall through to the held item.
     */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.getItem() instanceof ConfiguratorItem) {
            return InteractionResult.PASS;
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof MenuProvider provider) {
            serverPlayer.openMenu(provider).ifPresent(containerId -> {
                // NeroTech menus are plain vanilla MenuTypes, so the client never learns the machine's
                // position on its own. Tell it which machine this menu belongs to, keyed by container id,
                // so the Side Config widget needs no ray-trace. Block position only — no player data
                // (POPIA/GDPR).
                if (serverPlayer.containerMenu instanceof MachineMenu menu && menu.containerId == containerId) {
                    NeroTechNetwork.sendToPlayer(serverPlayer, new MachineMenuPosPayload(containerId, pos));
                }
            });
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer,
            ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        // NeroTech is the canonical opener of INDUSTRIAL_POWER: building your first machine is the milestone.
        if (!level.isClientSide() && placer instanceof ServerPlayer serverPlayer) {
            ProgressionGates.tryOpen(serverPlayer, CoreGates.INDUSTRIAL_POWER);
            // Record machine ownership ONLY when per-player pollution attribution is opted in (POPIA/GDPR:
            // default off = no player data stored). UUID only, erasable via the shared data-erasure hook.
            if (NeroTechConfig.pollutionPerPlayerAttribution()
                    && level.getBlockEntity(pos) instanceof NeroTechMachineBlockEntity machine) {
                machine.setOwner(serverPlayer.getUUID());
            }
        }
    }

    /**
     * Thermal-link cache invalidation (full thermal model): any neighbour change may add/remove an
     * adjacent machine or coolant block, so drop the BE's cached conduction links; they rebuild on
     * the next exchange pass. This keeps conduction event-driven — never a per-tick neighbour scan.
     */
    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
            @Nullable net.minecraft.world.level.redstone.Orientation orientation, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, orientation, movedByPiston);
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof NeroTechMachineBlockEntity machine) {
            machine.invalidateThermalLinks();
        }
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
            BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(type, machineType(),
                (lvl, pos, st, be) -> AbstractMachineBlockEntity.tick(lvl, pos, st, be));
    }
}
