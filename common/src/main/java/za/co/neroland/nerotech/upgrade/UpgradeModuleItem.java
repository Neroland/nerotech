package za.co.neroland.nerotech.upgrade;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.upgrade.UpgradeContainer;
import za.co.neroland.nerolandcore.upgrade.UpgradeType;

/**
 * A machine upgrade-module card. One class backs every module variant; each registered item fixes its
 * Core {@link UpgradeType}, so the card is identified purely by its item (no data component) and is
 * portable across every NeroTech machine — and any other machine built on Core's upgrade framework.
 *
 * <p>NeroTech registers modules against Core's {@link UpgradeType} and reads aggregate effects through
 * Core's {@code UpgradeModifiers}; it invents no bespoke upgrade system.
 */
public class UpgradeModuleItem extends Item {

    /** The shared classifier every NeroTech machine hands to its Core {@link UpgradeContainer}. */
    public static final UpgradeContainer.Classifier CLASSIFIER = UpgradeModuleItem::typeOf;

    @Nullable
    private final UpgradeType type;
    @Nullable
    private final NeroTechModule extra;

    public UpgradeModuleItem(Properties properties, UpgradeType type) {
        super(properties);
        this.type = type;
        this.extra = null;
    }

    /**
     * A NeroTech-classified module (Heat Sink, Scrubber): Core's closed {@link UpgradeType} has no entry
     * for it, so {@link #moduleType()} is {@code null} and Core's modifiers never count it; the NeroTech
     * machine base reads it through {@link #extraTypeOf(ItemStack)}.
     */
    public UpgradeModuleItem(Properties properties, NeroTechModule extra) {
        super(properties);
        this.type = null;
        this.extra = extra;
    }

    /** The Core upgrade type, or {@code null} for a NeroTech-classified module. */
    @Nullable
    public UpgradeType moduleType() {
        return this.type;
    }

    /** The NeroTech module kind, or {@code null} for a Core-typed module. */
    @Nullable
    public NeroTechModule extraType() {
        return this.extra;
    }

    /** @return the NeroTech module kind of {@code stack}, or {@code null}. */
    @Nullable
    public static NeroTechModule extraTypeOf(ItemStack stack) {
        return stack.getItem() instanceof UpgradeModuleItem module ? module.extraType() : null;
    }

    /** @return the Core upgrade type of {@code stack}, or {@code null} if it has none (not a card, or a NeroTech-classified one). */
    @Nullable
    public static UpgradeType typeOf(ItemStack stack) {
        return stack.getItem() instanceof UpgradeModuleItem module ? module.moduleType() : null;
    }

    /** Any NeroTech module card, Core-typed or NeroTech-classified (what a machine's upgrade slots accept). */
    public static boolean isModule(ItemStack stack) {
        return stack.getItem() instanceof UpgradeModuleItem;
    }
}
