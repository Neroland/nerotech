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

    private final UpgradeType type;

    public UpgradeModuleItem(Properties properties, UpgradeType type) {
        super(properties);
        this.type = type;
    }

    public UpgradeType moduleType() {
        return this.type;
    }

    /** @return the upgrade type of {@code stack}, or {@code null} if it is not a NeroTech module card. */
    @Nullable
    public static UpgradeType typeOf(ItemStack stack) {
        return stack.getItem() instanceof UpgradeModuleItem module ? module.moduleType() : null;
    }

    public static boolean isModule(ItemStack stack) {
        return stack.getItem() instanceof UpgradeModuleItem;
    }
}
