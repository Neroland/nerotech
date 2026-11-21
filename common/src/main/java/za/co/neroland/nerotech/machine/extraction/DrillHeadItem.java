package za.co.neroland.nerotech.machine.extraction;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * A Deep Core Drill head. Its wear (operations used) is item data in {@code nerotech:drill_head_wear}, shown
 * as a bar like durability; it is not a vanilla damage value, so anvils, Mending and Unbreaking do not touch
 * it. One per stack, since every head carries its own wear.
 */
public class DrillHeadItem extends Item {

    public DrillHeadItem(Properties properties) {
        super(properties);
    }

    /** Operations {@code stack} has used (0 for a fresh head or anything that is not a head). */
    public static int wear(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        Integer wear = stack.get(ExtractionContent.DRILL_HEAD_WEAR.get());
        return wear == null ? 0 : Math.max(0, wear);
    }

    /** Record {@code used} operations on {@code stack}; 0 removes the component (a fresh head stacks as new). */
    public static void setWear(ItemStack stack, int used) {
        if (stack.isEmpty()) {
            return;
        }
        if (used <= 0) {
            stack.remove(ExtractionContent.DRILL_HEAD_WEAR.get());
        } else {
            stack.set(ExtractionContent.DRILL_HEAD_WEAR.get(), used);
        }
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return wear(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        DrillHeadTier tier = DeepCoreDrillBlockEntity.tierOf(stack);
        return tier == null ? 0 : DrillRules.wearBarWidth(wear(stack), tier);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x9AA4AE; // worn steel
    }
}
