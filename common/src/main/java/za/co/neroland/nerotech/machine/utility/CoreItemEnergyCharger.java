package za.co.neroland.nerotech.machine.utility;

import java.util.concurrent.atomic.AtomicBoolean;

import net.minecraft.world.item.ItemStack;

import za.co.neroland.nerolandcore.energy.EnergyConversions;
import za.co.neroland.nerolandcore.energy.ItemEnergyLookup;
import za.co.neroland.nerolandcore.energy.NeroEnergyStorage;

import za.co.neroland.nerotech.NeroTechCommon;

/**
 * The fallback item adapter behind {@code api.WirelessCharging}: fills any item that stores Neroland Core
 * energy, found through Core's {@link ItemEnergyLookup} (Core 1.21.0+). Items from other mods that expose
 * Core item energy, such as a powered suit or a battery, are supported this way with no adapter of their
 * own and no dependency either way.
 *
 * <p>{@code WirelessCharging} asks it after every registered adapter, so NeroTech's own tools and any
 * mod-specific adapter keep their priority. The offer is in FE (what the charger can pay for this call);
 * the item's storage is in NE, converted at Core's ratio and capped at the item's free room
 * ({@link ChargeMath#coreItemRequestNe}). The FE reported back is rounded up and never above the offer,
 * so the charger is always paid at least what the item gained.
 *
 * <p>Privacy (POPIA/GDPR): an item stack and an amount only. A storage that throws is skipped and logged
 * once with its class name only; nothing about the player is logged or kept.
 */
public final class CoreItemEnergyCharger {

    private static final AtomicBoolean REPORTED = new AtomicBoolean();

    private CoreItemEnergyCharger() {
    }

    /** Insert up to {@code maxFe} FE into {@code stack}'s Core energy; FE accepted, 0 if not a Core energy item. */
    public static long charge(ItemStack stack, long maxFe, boolean simulate) {
        if (stack == null || stack.isEmpty() || maxFe <= 0) {
            return 0L;
        }
        NeroEnergyStorage storage = ItemEnergyLookup.find(stack);
        if (storage == null) {
            return 0L;
        }
        try {
            double ratio = EnergyConversions.neroToForgeRatio();
            long request = ChargeMath.coreItemRequestNe(maxFe, ratio, storage.getAmount(), storage.getCapacity());
            if (request <= 0) {
                return 0L;
            }
            long accepted = ChargeMath.clampAccepted(storage.insert(request, simulate), request);
            return ChargeMath.acceptedFe(accepted, ratio, maxFe);
        } catch (RuntimeException failure) {
            if (REPORTED.compareAndSet(false, true)) {
                NeroTechCommon.LOGGER.warn("[NeroTech] Item energy storage {} failed while charging ({}); skipping it",
                        storage.getClass().getName(), failure.getClass().getName());
            }
            return 0L;
        }
    }
}
