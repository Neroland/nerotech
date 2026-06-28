package za.co.neroland.nerotech.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import za.co.neroland.nerolandcore.progression.ProgressionGates;

/**
 * Base for NeroTech's advanced (Tier 2/3) machines. Server-authoritative progression gate: a player may
 * only open the machine once the required Core gate is open for them (default {@code reached_orbit}).
 * Possession is gated separately by recipes that require Starsteel ({@code c:ingots/starsteel}), which
 * only space (Nerospace) supplies — so on Earth-only play the advanced tier is simply uncraftable, and
 * Earth tier still plays fully standalone.
 */
public abstract class GatedMachineBlock extends NeroTechMachineBlock {

    protected GatedMachineBlock(Properties properties) {
        super(properties);
    }

    /** The Core gate that must be open to use this machine. */
    protected abstract Identifier requiredGate();

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            if (!ProgressionGates.isOpen(serverPlayer, requiredGate())) {
                serverPlayer.sendSystemMessage(Component.translatable("message.nerotech.requires_orbit"));
                return InteractionResult.SUCCESS;
            }
            if (level.getBlockEntity(pos) instanceof MenuProvider provider) {
                serverPlayer.openMenu(provider);
            }
        }
        return InteractionResult.SUCCESS;
    }
}
