package za.co.neroland.nerotech.gas;

import java.util.Map;

import net.minecraft.resources.Identifier;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerotech.config.NeroTechConfig;

/**
 * Which gases the Gas Turbine burns, and how well — the config-driven fuel table
 * ({@code turbineGasBurn}, default {@code nerotech:hydrogen=2}). One unit of a fuel yields
 * {@code gasTurbineNePerUnit × multiplier} NE, so packs retune both the ladder and the roster
 * without a code change.
 *
 * <p>Parse-and-cache follows the {@code ThermalEnvironment} recipe: the string is re-parsed only
 * when the config value actually changes (hot-reload safe), never per tick.
 *
 * <p><b>Follow-up</b>: a tag-driven roster ({@code nerotech:turbine_fuels} over a gas registry)
 * is the eventual surface — Core's gas layer identifies gases by id only and ships no gas
 * registry to tag, so the config map is the launch mechanism.
 */
public final class TurbineFuels {

    private static volatile String parsedFrom;
    private static volatile Map<String, Integer> cache = Map.of();

    private TurbineFuels() {
    }

    /** Burn multiplier for a gas, or 0 when it is not a turbine fuel. */
    public static int burnMultiplier(@Nullable Identifier gas) {
        if (gas == null) {
            return 0;
        }
        return table().getOrDefault(gas.toString(), 0);
    }

    /** Whether the turbine accepts this gas at all (the tank's fill filter). */
    public static boolean burns(@Nullable Identifier gas) {
        return burnMultiplier(gas) > 0;
    }

    private static Map<String, Integer> table() {
        String raw = NeroTechConfig.turbineGasBurn();
        if (raw.equals(parsedFrom)) {
            return cache;
        }
        cache = GasFuelMap.parse(raw);
        parsedFrom = raw;
        return cache;
    }
}
