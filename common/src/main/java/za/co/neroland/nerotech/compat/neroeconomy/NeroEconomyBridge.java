package za.co.neroland.nerotech.compat.neroeconomy;

import java.util.Set;
import java.util.UUID;

import net.minecraft.server.MinecraftServer;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.economy.CoreCurrencies;
import za.co.neroland.nerolandcore.economy.CurrencyApi;
import za.co.neroland.neroeconomy.api.EconomyCaller;
import za.co.neroland.neroeconomy.api.EconomyResult;
import za.co.neroland.neroeconomy.api.SiblingEconomy;

import za.co.neroland.nerotech.NeroTechCommon;

/**
 * The ONLY class in NeroTech that imports {@code za.co.neroland.neroeconomy}. Loaded solely through
 * {@link MachineCharges} after {@code isModLoaded("neroeconomy")}; compiled {@code compileOnly}.
 *
 * <p>With NeroEconomy's sibling API (version 1+, read reflectively because javac inlines the constant)
 * a charge is {@code EconomyCaller.charge(..., "machine_operation", null)}: budgeted, refused before
 * anything moves, sunk into the Reserve. With an older NeroEconomy the bridge falls back to Core's
 * {@link CurrencyApi#withdraw} and checks its boolean result.
 */
final class NeroEconomyBridge {

    @Nullable
    private static EconomyCaller caller;

    private NeroEconomyBridge() {
    }

    /** Claim NeroTech's caller handle (common init only). Returns whether charges can be taken. */
    static boolean init() {
        int api;
        try {
            api = SiblingEconomy.class.getField("API_VERSION").getInt(null);
        } catch (ReflectiveOperationException | LinkageError e) {
            api = 0;
        }
        if (api >= 1) {
            caller = SiblingEconomy.claim(NeroTechCommon.MOD_ID, Set.of(MachineCharges.REASON));
        }
        // api < 1: an older NeroEconomy — stay on Core's CurrencyApi (checked per charge below).
        return true;
    }

    static MachineCharges.Outcome charge(MinecraftServer server, UUID owner, long amount) {
        EconomyCaller handle = caller;
        if (handle != null) {
            // No request key: the machine's own prepaid-batch counter already guarantees once-only.
            EconomyResult result = handle.charge(server, owner, amount, MachineCharges.REASON, null);
            return result.ok() ? MachineCharges.Outcome.PAID : MachineCharges.Outcome.refused(result.translationKey());
        }
        if (!CurrencyApi.hasRealProvider()) {
            return MachineCharges.Outcome.FREE;
        }
        return CurrencyApi.withdraw(owner, CoreCurrencies.CREDITS, amount)
                ? MachineCharges.Outcome.PAID
                : MachineCharges.Outcome.refused(MachineCharges.GENERIC_REFUSAL_KEY);
    }
}
