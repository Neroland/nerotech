package za.co.neroland.nerotech.api;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

import za.co.neroland.nerotech.machine.utility.ChargeMath;
import za.co.neroland.nerotech.machine.utility.InductionChargerBlockEntity;
import za.co.neroland.nerotech.machine.utility.InductionChargers;

/**
 * Wireless charging from NeroTech's Induction Chargers, for mods that keep their own energy on an entity
 * (NeroCompanion's companions) or in an item.
 *
 * <p><b>Entities:</b> call {@link #offer} from the entity's own tick with the block it stands on. Every
 * loaded Induction Charger whose bounded reach ({@code inductionChargerRadius} blocks on each axis) covers
 * that block contributes, nearest first, within its per-tick output cap. Use {@code simulate = true} to
 * ask how much is on offer without taking it.
 *
 * <p><b>Items:</b> Neroland Core has no item-energy API yet, so a charger fills a player's held or worn
 * items through the {@link ItemCharger} adapters registered here (a mod registers one for the items it
 * owns; NeroTech's loader modules may register one for their loader's item-energy capability). With no
 * adapter, players' items are simply not charged.
 *
 * <p>Privacy (POPIA/GDPR): no call here takes, returns or stores player data — a level, a block position
 * and an amount only. Server thread only. Stable within the 0.x minor, like the rest of this package.
 */
public final class WirelessCharging {

    /** Fills one energy item. */
    @FunctionalInterface
    public interface ItemCharger {
        /**
         * Insert up to {@code maxFe} FE into {@code stack}, changing it in place unless {@code simulate}.
         *
         * @return FE accepted; 0 when the stack is not an energy item this adapter knows, or is full
         */
        long charge(ItemStack stack, long maxFe, boolean simulate);
    }

    private static final List<ItemCharger> ITEM_CHARGERS = new CopyOnWriteArrayList<>();

    private WirelessCharging() {
    }

    /**
     * FE that the Induction Chargers in range of {@code near} can give right now, up to {@code maxFe}.
     * Unless {@code simulate}, that energy is taken from the chargers and the caller must store it.
     * Never more than {@code maxFe}; 0 when nothing is in range, a charger is empty, or {@code maxFe <= 0}.
     */
    public static long offer(ServerLevel level, BlockPos near, long maxFe, boolean simulate) {
        if (level == null || near == null || maxFe <= 0) {
            return 0L;
        }
        List<InductionChargerBlockEntity> chargers = InductionChargers.reaching(level, near);
        if (chargers.isEmpty()) {
            return 0L;
        }
        long now = level.getGameTime();
        List<ChargeMath.Source> sources = new ArrayList<>(chargers.size());
        for (InductionChargerBlockEntity charger : chargers) {
            sources.add(charger.feSource(now));
        }
        return ChargeMath.offer(maxFe, simulate, sources);
    }

    /** Whether any loaded Induction Charger reaches {@code near} (cheap; no energy is read). */
    public static boolean inRange(ServerLevel level, BlockPos near) {
        return level != null && near != null && !InductionChargers.reaching(level, near).isEmpty();
    }

    /** Register an item adapter. Adapters are asked in registration order; the first that accepts wins. */
    public static void registerItemCharger(ItemCharger charger) {
        if (charger != null && !ITEM_CHARGERS.contains(charger)) {
            ITEM_CHARGERS.add(charger);
        }
    }

    /**
     * Charge one item through the registered adapters (what a charger does to a player's held and worn
     * items). Returns FE accepted, never more than {@code maxFe}.
     */
    public static long chargeItem(ItemStack stack, long maxFe, boolean simulate) {
        if (stack == null || stack.isEmpty() || maxFe <= 0) {
            return 0L;
        }
        for (ItemCharger charger : ITEM_CHARGERS) {
            long accepted = charger.charge(stack, maxFe, simulate);
            if (accepted > 0) {
                return Math.min(accepted, maxFe);
            }
        }
        return 0L;
    }

    /** Whether any item adapter is registered (player charging is inert without one). */
    public static boolean hasItemChargers() {
        return !ITEM_CHARGERS.isEmpty();
    }
}
