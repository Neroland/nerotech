package za.co.neroland.nerotech.config;

import za.co.neroland.nerolandcore.config.ConfigManager;
import za.co.neroland.nerolandcore.config.ConfigSchema;
import za.co.neroland.nerolandcore.config.ConfigValue;

/**
 * NeroTech's config, backed by Neroland Core's shared {@link ConfigManager}. Core owns the single
 * {@code config/nerotech.properties} file (defaults, range validation, in-place key migration, the
 * {@code /neroland config reload} hot-reload, and server-authoritative client sync) — NeroTech just
 * declares the schema once and reads the typed {@link ConfigValue} handles through static getters.
 *
 * <p>Gameplay-balance values are <b>server-authoritative</b> (a connected client uses the server's
 * values). The snapshot Core syncs carries only config keys/values — never player data (POPIA/GDPR).
 */
public final class NeroTechConfig {

    private static final ConfigSchema SCHEMA = ConfigSchema.create("nerotech",
            "NeroTech config (managed by Neroland Core). Tier-1 Earth machine balance.");

    // --- energy I/O sizing --------------------------------------------------
    private static final ConfigValue<Integer> MACHINE_CAPACITY = SCHEMA.intRange("machineEnergyCapacity",
            100_000, 1_000, 100_000_000, true, "internal NE buffer of every Tier-1 machine");
    private static final ConfigValue<Integer> MACHINE_MAX_IO = SCHEMA.intRange("machineMaxTransfer",
            2_000, 1, 10_000_000, true, "max NE/tick a machine accepts or emits per side");

    // --- generators ---------------------------------------------------------
    private static final ConfigValue<Integer> GENERATOR_NE_PER_TICK = SCHEMA.intRange("neroGeneratorNePerTick",
            40, 1, 1_000_000, true, "NE/tick produced by the Nero Generator while burning fuel");
    private static final ConfigValue<Integer> SOLAR_NE_PER_TICK = SCHEMA.intRange("solarArrayNePerTick",
            10, 1, 1_000_000, true, "NE/tick produced by the Solar Array in full daylight with sky access");

    // --- processing machines ------------------------------------------------
    private static final ConfigValue<Integer> PROCESS_BASE_TICKS = SCHEMA.intRange("machineBaseProcessTicks",
            120, 1, 72_000, true, "base ticks one Ore Processor / Fabricator operation takes (before Speed modules)");
    private static final ConfigValue<Integer> PROCESS_NE_PER_TICK = SCHEMA.intRange("machineNePerTick",
            30, 0, 1_000_000, true, "base NE/tick a processing machine consumes while working (before Efficiency)");

    // --- heat (Stage 3 consequence axis) ------------------------------------
    private static final ConfigValue<Integer> HEAT_CAPACITY = SCHEMA.intRange("heatCapacity",
            1_000, 1, 1_000_000, true, "max heat units a machine can hold (gauge scale)");
    private static final ConfigValue<Integer> HEAT_PER_OP = SCHEMA.intRange("heatPerOperation",
            4, 0, 100_000, true, "heat added each working tick (generators: per burn tick)");
    private static final ConfigValue<Integer> HEAT_DISSIPATION = SCHEMA.intRange("heatDissipationPerTick",
            1, 0, 100_000, true, "heat shed passively each tick (extra when next to water/ice/snow)");
    private static final ConfigValue<Integer> HEAT_THROTTLE = SCHEMA.intRange("heatThrottleThreshold",
            800, 1, 1_000_000, true, "processing machines stall once heat reaches this (must cool to resume)");

    // --- pollution (regional, periodic aggregate) ---------------------------
    private static final ConfigValue<Integer> POLLUTION_PER_OP = SCHEMA.intRange("pollutionPerOperation",
            2, 0, 100_000, true, "pollution a machine emits per contribution (0 disables; solar emits none)");
    private static final ConfigValue<Integer> POLLUTION_CONTRIB_INTERVAL = SCHEMA.intRange("pollutionContributionIntervalTicks",
            40, 1, 72_000, true, "how often (ticks) a running machine adds to its region (batched, not per-tick)");
    private static final ConfigValue<Integer> POLLUTION_DECAY_INTERVAL = SCHEMA.intRange("pollutionDecayIntervalTicks",
            200, 1, 72_000, true, "how often (ticks) regional pollution decays server-wide");
    private static final ConfigValue<Integer> POLLUTION_DECAY_AMOUNT = SCHEMA.intRange("pollutionDecayAmount",
            1, 0, 100_000, true, "pollution removed from each region per decay step");
    private static final ConfigValue<Boolean> POLLUTION_ATTRIBUTION = SCHEMA.bool("pollutionPerPlayerAttribution",
            false, true, "OFF by default (privacy): when true, pollution is attributed to the placing player's "
            + "UUID (POPIA/GDPR: UUIDs only, retention-pruned, erasable via the shared data-erasure hook)");
    private static final ConfigValue<Integer> POLLUTION_RETENTION_DAYS = SCHEMA.intRange("pollutionAttributionRetentionDays",
            30, 0, 3_650, true, "days to keep per-player pollution attribution before pruning (0 = keep until erased)");

    private NeroTechConfig() {
    }

    public static int machineEnergyCapacity() {
        return MACHINE_CAPACITY.get();
    }

    public static int machineMaxTransfer() {
        return MACHINE_MAX_IO.get();
    }

    public static int neroGeneratorNePerTick() {
        return GENERATOR_NE_PER_TICK.get();
    }

    public static int solarArrayNePerTick() {
        return SOLAR_NE_PER_TICK.get();
    }

    public static int machineBaseProcessTicks() {
        return PROCESS_BASE_TICKS.get();
    }

    public static int machineNePerTick() {
        return PROCESS_NE_PER_TICK.get();
    }

    public static int heatCapacity() {
        return HEAT_CAPACITY.get();
    }

    public static int heatPerOperation() {
        return HEAT_PER_OP.get();
    }

    public static int heatDissipationPerTick() {
        return HEAT_DISSIPATION.get();
    }

    public static int heatThrottleThreshold() {
        return HEAT_THROTTLE.get();
    }

    public static int pollutionPerOperation() {
        return POLLUTION_PER_OP.get();
    }

    public static int pollutionContributionIntervalTicks() {
        return POLLUTION_CONTRIB_INTERVAL.get();
    }

    public static int pollutionDecayIntervalTicks() {
        return POLLUTION_DECAY_INTERVAL.get();
    }

    public static int pollutionDecayAmount() {
        return POLLUTION_DECAY_AMOUNT.get();
    }

    public static boolean pollutionPerPlayerAttribution() {
        return POLLUTION_ATTRIBUTION.get();
    }

    public static int pollutionAttributionRetentionDays() {
        return POLLUTION_RETENTION_DAYS.get();
    }

    /** Register the schema with Core (reads/creates {@code nerotech.properties}). Idempotent. */
    public static synchronized void load() {
        ConfigManager.register(SCHEMA);
    }
}
