package za.co.neroland.nerotech.compat.neroeconomy;

import java.util.UUID;

import net.minecraft.server.MinecraftServer;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerotech.NeroTechCommon;
import za.co.neroland.nerotech.platform.Services;

/**
 * Optional per-operation machine charges through NeroEconomy (0.3.0+, {@code SiblingEconomy} API 1).
 * This façade imports nothing from NeroEconomy: it touches {@link NeroEconomyBridge} — the only class
 * that does — strictly after {@code isModLoaded("neroeconomy")}, so NeroTech builds, loads and runs
 * with NeroEconomy absent (every charge is then free).
 *
 * <p>Charges are made in batches (see {@code machineOperationChargeBatch}), never per tick, and only
 * for a machine that records an owner. They move credits from the owner to the economy's Reserve; no
 * machine ever pays out. <b>Privacy (POPIA/GDPR):</b> the owner's UUID is passed only for the charge
 * and is never logged with the amount.
 */
public final class MachineCharges {

    /** The one reason code NeroTech claims. */
    public static final String REASON = "machine_operation";

    /** Lang key for a refusal with no economy-specific reason (Core {@code CurrencyApi} fallback). */
    public static final String GENERIC_REFUSAL_KEY = "nerotech.machine_charge.refused";

    private static volatile boolean active;

    private MachineCharges() {
    }

    /** What a charge attempt decided: whether the batch may run and, when refused, a lang key. */
    public record Outcome(boolean ok, @Nullable String refusalKey) {
        static final Outcome FREE = new Outcome(true, null);
        static final Outcome PAID = new Outcome(true, null);

        static Outcome refused(String key) {
            return new Outcome(false, key);
        }
    }

    /** Bind NeroEconomy when it is loaded. Call once from common init (NeroEconomy claims close at server start). */
    public static void init() {
        if (!Services.PLATFORM.isModLoaded("neroeconomy")) {
            return;
        }
        try {
            active = NeroEconomyBridge.init();
        } catch (LinkageError | RuntimeException e) {
            // No player data here: only the failure type, so an operator can see why charges are off.
            NeroTechCommon.LOGGER.warn("[NeroTech] NeroEconomy is installed but its API could not be bound; "
                    + "machine charges stay off ({})", e.getClass().getSimpleName());
            active = false;
        }
    }

    /** Whether charges can be taken at all (NeroEconomy present and bound). */
    public static boolean active() {
        return active;
    }

    /**
     * Charge {@code owner} {@code amount} credits for one batch of operations. Free (ok, nothing moves)
     * when charges are inactive or the amount is not positive.
     */
    public static Outcome charge(MinecraftServer server, UUID owner, long amount) {
        if (!active || amount <= 0) {
            return Outcome.FREE;
        }
        try {
            return NeroEconomyBridge.charge(server, owner, amount);
        } catch (LinkageError | RuntimeException e) {
            NeroTechCommon.LOGGER.warn("[NeroTech] machine charge failed; charges are now off for this session ({})",
                    e.getClass().getSimpleName());
            active = false;
            return Outcome.FREE;
        }
    }
}
