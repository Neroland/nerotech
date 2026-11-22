package za.co.neroland.nerotech.api;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

import za.co.neroland.nerotech.item.ConfiguratorItem;
import za.co.neroland.nerotech.item.ConfiguratorState;
import za.co.neroland.nerotech.registry.ModDataComponents;

/**
 * Read access to a Configurator stack, so add-ons that pair or configure blocks with NeroTech's wrench
 * do not import its item, state record or data-component registry. Reads only; NeroTech's own item
 * code is the only writer. A Configurator stores world data only (a mode, a side-config clipboard and a
 * pending link position): no player identity.
 */
public final class Configurator {

    /** A pending link end: the block position and the dimension id it was taken in. */
    public record LinkTarget(BlockPos pos, String dimension) {
    }

    private Configurator() {
    }

    /** Whether {@code stack} is a NeroTech Configurator. */
    public static boolean isConfigurator(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getItem() instanceof ConfiguratorItem;
    }

    /** Whether the Configurator is in copy/paste mode (false for a fresh tool or any other stack). */
    public static boolean isCopyPasteMode(ItemStack stack) {
        ConfiguratorState state = state(stack);
        return state != null && state.copyPaste();
    }

    /** The stored first end of a pending link, if the Configurator holds one. */
    public static Optional<LinkTarget> linkTarget(ItemStack stack) {
        ConfiguratorState state = state(stack);
        if (state == null) {
            return Optional.empty();
        }
        return state.linkSource().map(link -> new LinkTarget(link.toPos(), link.dimension()));
    }

    private static ConfiguratorState state(ItemStack stack) {
        if (!isConfigurator(stack)) {
            return null;
        }
        return stack.get(ModDataComponents.CONFIGURATOR_STATE.get());
    }
}
