package za.co.neroland.nerotech.machine;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.machine.AbstractMachineBlockEntity;

import za.co.neroland.nerotech.item.ConfiguratorItem;
import za.co.neroland.nerotech.registry.ModBlockEntities;

/**
 * Singularity Vault block — the hand interface to {@link SingularityVaultBlockEntity}:
 * right-click with a stack deposits it, crouch-right-click takes a stack back, and an empty-handed
 * right-click reports the fill on the actionbar. It has no menu, so it never calls the machine
 * base's GUI-opening {@code useWithoutItem}.
 *
 * <p>Also the mod's only comparator source: the analog signal is the fill as a fraction of
 * {@code singularityVaultCapacity}, with a floor of 1 while anything at all is stored.
 */
public class SingularityVaultBlock extends NeroTechMachineBlock {

    public static final MapCodec<SingularityVaultBlock> CODEC = simpleCodec(SingularityVaultBlock::new);

    public SingularityVaultBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<SingularityVaultBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SingularityVaultBlockEntity(pos, state);
    }

    @Override
    protected BlockEntityType<? extends AbstractMachineBlockEntity> machineType() {
        return ModBlockEntities.SINGULARITY_VAULT.get();
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        // The Configurator still reaches its own useOn (the machine base's rule).
        if (stack.getItem() instanceof ConfiguratorItem) {
            return InteractionResult.PASS;
        }
        if (stack.isEmpty()) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof SingularityVaultBlockEntity vault) {
            if (player.isSecondaryUseActive()) {
                give(player, vault.extractStack());
            } else if (vault.deposit(stack) <= 0) {
                tell(player, Component.translatable("block.nerotech.singularity_vault.mismatch"));
            }
            report(player, vault);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hit) {
        if (!level.isClientSide()
                && level.getBlockEntity(pos) instanceof SingularityVaultBlockEntity vault) {
            if (player.isSecondaryUseActive()) {
                give(player, vault.extractStack());
            }
            report(player, vault);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof SingularityVaultBlockEntity vault
                ? vault.comparatorSignal() : 0;
    }

    /** Hand a stack to the player, dropping whatever will not fit. */
    private static void give(Player player, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    /** Actionbar readout: what is stored and how much of the capacity it fills. */
    private static void report(Player player, SingularityVaultBlockEntity vault) {
        ItemStack type = vault.storedType();
        tell(player, type.isEmpty()
                ? Component.translatable("block.nerotech.singularity_vault.empty")
                : Component.translatable("block.nerotech.singularity_vault.count",
                        type.getHoverName(), vault.totalStored(), SingularityVaultBlockEntity.capacity()));
    }

    /** Actionbar feedback — transient, so bulk deposits never flood the chat. */
    private static void tell(Player player, Component message) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(message, true);
        }
    }
}
