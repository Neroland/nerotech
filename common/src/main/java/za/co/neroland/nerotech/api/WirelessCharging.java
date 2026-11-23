package za.co.neroland.nerotech.api;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.energy.EnergyConversions;
import za.co.neroland.nerolandcore.energy.ItemEnergyLookup;
import za.co.neroland.nerolandcore.energy.NeroEnergyStorage;

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
 * <p><b>Items:</b> a charger fills a player's held or worn items through the {@link ItemCharger} adapters
 * registered here first (a mod registers one for the items it owns; NeroTech's loader modules may register
 * one for their loader's item-energy capability). If none accepts the stack, the charger fills whatever
 * Neroland Core's {@link ItemEnergyLookup} resolves for it, so any item that registers there charges with
 * no NeroTech adapter and no dependency on the item's mod. That storage is Nero energy (NE); this API still
 * speaks Forge Energy (FE) and converts with Core's ratio. With neither an accepting adapter nor a lookup
 * storage, the stack is not charged.
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
     * Charge one item: registered adapters first, then Neroland Core's {@link ItemEnergyLookup} (what a
     * charger does to a player's held and worn items). Returns FE accepted, never more than {@code maxFe}.
     */
    public static long chargeItem(ItemStack stack, long maxFe, boolean simulate) {
        if (stack == null || stack.isEmpty() || maxFe <= 0) {
            return 0L;
        }
        return chargeHeld(stack, ItemEnergyLookup.find(stack), maxFe, simulate, EnergyConversions.neroToForgeRatio());
    }

    /**
     * Adapters first; {@code lookedUp} is what {@link ItemEnergyLookup#find} returned for the same stack
     * (null when the stack holds no Core item energy). Package-private so a plain-JVM test can pass a
     * lookup result without a bootstrapped item. Returns FE accepted, never more than {@code maxFe}.
     */
    static long chargeHeld(@Nullable ItemStack stack, @Nullable NeroEnergyStorage lookedUp, long maxFe,
            boolean simulate, double ratio) {
        if (maxFe <= 0) {
            return 0L;
        }
        for (ItemCharger charger : ITEM_CHARGERS) {
            long accepted = charger.charge(stack, maxFe, simulate);
            if (accepted > 0) {
                return Math.min(accepted, maxFe);
            }
        }
        return fillStorage(lookedUp, maxFe, simulate, ratio);
    }

    /**
     * Insert up to {@code maxFe} FE into a Core item-energy storage, in NE. Returns the FE to report,
     * capped at the offer, so the charger pays for what the item gained and nothing is created.
     */
    private static long fillStorage(@Nullable NeroEnergyStorage storage, long maxFe, boolean simulate, double ratio) {
        if (storage == null) {
            return 0L;
        }
        long maxNe = ChargeMath.feOfferToNe(maxFe, ratio);
        if (maxNe <= 0) {
            return 0L;
        }
        long inserted = storage.insert(maxNe, simulate);
        if (inserted <= 0) {
            return 0L;
        }
        return ChargeMath.acceptedFe(Math.min(inserted, maxNe), ratio, maxFe);
    }

    /**
     * Whether any item adapter is registered. Player item charging does not depend on this: Core's
     * item-energy lookup is always tried after the adapters.
     */
    public static boolean hasItemChargers() {
        return !ITEM_CHARGERS.isEmpty();
    }

    /** Test teardown only. */
    static void clearChargersForTests() {
        ITEM_CHARGERS.clear();
    }
}
