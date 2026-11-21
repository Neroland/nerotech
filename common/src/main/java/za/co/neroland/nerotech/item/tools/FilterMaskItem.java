package za.co.neroland.nerotech.item.tools;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import za.co.neroland.nerotech.config.NeroTechConfig;
import za.co.neroland.nerotech.registry.ModItems;

/**
 * Filter Mask — worn in the head slot, charged and holding a Filter Cartridge, it blocks the pollution
 * exposure pulse ({@link WornTools}). Use it with a Filter Cartridge in the other hand to fit one (the
 * cartridge is taken); it then blocks {@code filterMaskPulsesPerCartridge} pulses, and the pulse that
 * uses the last one fouls it into a Dirty Filter handed to the wearer — the existing Scrubber cycle. The
 * uses left live in the {@code nerotech:fitted_filter} component. No player data.
 */
public class FilterMaskItem extends HeadToolItem {

    public FilterMaskItem(Properties properties) {
        super(properties);
    }

    @Override
    public int capacity() {
        return NeroTechConfig.filterMaskCapacity();
    }

    public static int usesLeft(ItemStack stack) {
        Integer left = stack.get(ToolsContent.FITTED_FILTER.get());
        return left == null ? 0 : Math.max(0, left);
    }

    @Override
    protected boolean useInAir(Level level, Player player, InteractionHand hand) {
        InteractionHand other = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack cartridge = player.getItemInHand(other);
        if (!cartridge.is(ModItems.FILTER_CARTRIDGE.get())) {
            return false;
        }
        ItemStack mask = player.getItemInHand(hand);
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            if (usesLeft(mask) > 0) {
                tell(serverPlayer, Component.translatable("message.nerotech.filter_mask.already_fitted", usesLeft(mask)));
                return true;
            }
            cartridge.shrink(1);
            mask.set(ToolsContent.FITTED_FILTER.get(), NeroTechConfig.filterMaskPulsesPerCartridge());
            tell(serverPlayer, Component.translatable("message.nerotech.filter_mask.fitted", usesLeft(mask)));
        }
        return true;
    }
}
