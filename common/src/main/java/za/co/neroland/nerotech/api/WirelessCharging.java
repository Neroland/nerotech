package za.co.neroland.nerotech.api;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

import za.co.neroland.nerotech.machine.utility.ChargeMath;
import za.co.neroland.nerotech.machine.utility.CoreItemEnergyCharger;
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
 * <p><b>Items:</b> a charger fills a player's held or worn items through the {@link ItemCharger} adapters
 * registered here (NeroTech registers one for its own tools; a mod may register one for items it owns),
 * then, for any item none of them claims, through Neroland Core's item-energy seam
 * ({@code za.co.neroland.nerolandcore.energy.ItemEnergyLookup}). So any item a mod exposes through Core
 * (a battery, a suit, a tool) is charged with no adapter here and no dependency on NeroTech. That Core
 * fallback is always asked last, after every registered adapter, whenever it was registered.
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

    /** Asked after every registered adapter: items that store Neroland Core energy (since 0.12.0). */
    private static final ItemCharger CORE_ITEM_ENERGY = CoreItemEnergyCharger::charge;

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

    /**
     * Register an item adapter. Adapters are asked in registration order; the first that accepts wins.
     * Items no adapter accepts fall back to Neroland Core's item-energy seam, which is always asked last.
     */
    public static void registerItemCharger(ItemCharger charger) {
        if (charger != null && !ITEM_CHARGERS.contains(charger)) {
            ITEM_CHARGERS.add(charger);
        }
    }

    /**
     * Charge one item through the registered adapters, then Neroland Core's item-energy seam (what a
     * charger does to a player's held and worn items). Returns FE accepted, never more than {@code maxFe}.
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
        long accepted = CORE_ITEM_ENERGY.charge(stack, maxFe, simulate);
        return accepted > 0 ? Math.min(accepted, maxFe) : 0L;
    }

    /**
     * Whether players' items can be charged at all. Always {@code true} since 0.12.0: the Neroland Core
     * item-energy fallback is always present, even with no adapter registered.
     */
    public static boolean hasItemChargers() {
        return true;
    }
}
