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

    // --- heat (Stage 3 consequence axis; full thermal model per Stage C decision 2026-07-10) ----
    private static final ConfigValue<Integer> HEAT_CAPACITY = SCHEMA.intRange("heatCapacity",
            1_000, 1, 1_000_000, true, "max heat units a machine can hold (gauge scale)");
    private static final ConfigValue<Integer> HEAT_PER_OP = SCHEMA.intRange("heatPerOperation",
            4, 0, 100_000, true, "heat added each working tick (generators: per burn tick)");
    private static final ConfigValue<Integer> HEAT_DISSIPATION = SCHEMA.intRange("heatDissipationPerTick",
            1, 0, 100_000, true, "extra heat shed per adjacent coolant block (water/ice/snow) each tick");
    private static final ConfigValue<Integer> HEAT_THROTTLE = SCHEMA.intRange("heatThrottleThreshold",
            800, 1, 1_000_000, true, "processing machines stall once heat reaches this (must cool to resume)");
    private static final ConfigValue<Integer> THERMAL_ENV_LOSS = SCHEMA.intRange("thermalEnvLossPermille",
            20, 0, 1_000, true, "permille of the machine-vs-ambient heat difference exchanged with the "
            + "environment each tick (machines relax toward ambient; 0 disables)");
    private static final ConfigValue<Integer> THERMAL_CONDUCTIVITY = SCHEMA.intRange("thermalConductivityPermille",
            100, 0, 1_000, true, "permille of the heat difference conducted between ADJACENT machines per "
            + "exchange (dense builds share heat; 0 disables machine-to-machine conduction)");
    private static final ConfigValue<Integer> THERMAL_EXCHANGE_INTERVAL = SCHEMA.intRange("thermalExchangeIntervalTicks",
            10, 1, 72_000, true, "how often (ticks) a machine conducts heat with cached adjacent machines "
            + "(interval + per-machine phase — never a per-tick neighbour scan)");
    private static final ConfigValue<Integer> THERMAL_AMBIENT_DEFAULT = SCHEMA.intRange("thermalAmbientDefault",
            0, -100_000, 100_000, true, "baseline ambient heat for dimensions not listed in thermalAmbientByDimension");
    private static final ConfigValue<String> THERMAL_AMBIENT_BY_DIMENSION = SCHEMA.string("thermalAmbientByDimension",
            "minecraft:the_nether=150", true, "per-dimension ambient heat: comma-list of dimensionId=heat, e.g. "
            + "minecraft:the_nether=150,nerospace:cindara=200 (FALLBACK since Stage H: nerospace.api planet "
            + "traits take precedence for known planets when Nerospace is installed; this covers everything else)");
    private static final ConfigValue<Integer> THERMAL_BIOME_SCALE = SCHEMA.intRange("thermalBiomeScale",
            50, 0, 100_000, true, "ambient heat added per full point of biome base temperature above vanilla "
            + "plains (0.8): deserts run hot, snowy biomes run cold (0 disables biome flavour)");

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
    private static final ConfigValue<Integer> POLLUTION_EVENT_THRESHOLD = SCHEMA.intRange("pollutionEventThreshold",
            1_000, 0, 1_000_000, true, "regional pollution level that publishes a Core threshold event when "
            + "crossed (rising and recovering; dormant until a listener like NeroEvents exists; 0 disables)");

    // --- pollution mitigation (Stage F: Scrubber + Remediator) ---------------
    private static final ConfigValue<Integer> SCRUBBER_NE_PER_OP = SCHEMA.intRange("scrubberNePerOp",
            120, 0, 1_000_000, true, "NE one Scrubber operation costs (batched on the contribution interval)");
    private static final ConfigValue<Integer> SCRUBBER_RATE = SCHEMA.intRange("scrubberPollutionPerOp",
            6, 1, 100_000, true, "pollution removed from the Scrubber's own region per operation");
    private static final ConfigValue<Integer> SCRUBBER_ADJACENT = SCHEMA.intRange("scrubberAdjacentPermille",
            250, 0, 1_000, true, "permille of the scrub rate also removed from each of the 8 adjacent regions "
            + "(default 25%; 0 = own region only)");
    private static final ConfigValue<Integer> SCRUBBER_FILTER_CAPACITY = SCHEMA.intRange("scrubberFilterCapacity",
            400, 1, 1_000_000, true, "pollution one Filter Cartridge absorbs before it fouls into a Dirty Filter");
    private static final ConfigValue<Integer> REMEDIATOR_NE_PER_OP = SCHEMA.intRange("remediatorNePerOp",
            600, 0, 10_000_000, true, "NE one Remediator operation costs — deliberately heavy; cleanup is a sink");
    private static final ConfigValue<Integer> REMEDIATOR_RATE = SCHEMA.intRange("remediatorPollutionPerOp",
            20, 1, 100_000, true, "pollution removed from the Remediator's own region per operation");

    // --- production analytics (Stage G: Analytics Terminal) ------------------
    private static final ConfigValue<Integer> ANALYTICS_RADIUS = SCHEMA.intRange("analyticsTerminalRadius",
            16, 4, 64, true, "block radius an Analytics Terminal scans for NeroTech machines (loaded "
            + "chunks only, batched every 100 ticks — large radii make each scan pass more expensive)");

    // --- Tier 2/3 (gated behind Nerospace / orbit) --------------------------
    private static final ConfigValue<Integer> FUSION_NE_PER_TICK = SCHEMA.intRange("fusionReactorNePerTick",
            400, 1, 10_000_000, true, "NE/tick the Fusion Reactor produces while running (high-output late-game)");
    private static final ConfigValue<Boolean> FUSION_FAILURE = SCHEMA.bool("fusionReactorMeltdownEnabled",
            true, true, "true: an unmanaged Fusion Reactor melts down destructively at max heat (telegraphed by "
            + "the red gauge); false (survival-friendly): it just stalls until it cools");
    private static final ConfigValue<String> FUSION_SIZE_OUTPUT = SCHEMA.string("fusionSizeOutputPermille",
            "3=1000,5=4000,7=12000", true, "multiblock output multiplier (permille of fusionReactorNePerTick) "
            + "per shell size: comma-list of size=permille for the 3/5/7 shells (Stage E multiblock)");
    private static final ConfigValue<Integer> FUSION_T1_BURN = SCHEMA.intRange("fusionFuelTier1BurnTicks",
            1_600, 1, 720_000, true, "burn ticks of one tier-1 fuel (Fusion Cell) charge");
    private static final ConfigValue<Integer> FUSION_T2_BURN = SCHEMA.intRange("fusionFuelTier2BurnTicks",
            4_800, 1, 720_000, true, "burn ticks of one tier-2 fuel (Plasma Cell) charge — needs a 5x5x5+ shell");
    private static final ConfigValue<Integer> FUSION_T3_BURN = SCHEMA.intRange("fusionFuelTier3BurnTicks",
            14_400, 1, 720_000, true, "burn ticks of one tier-3 fuel (Stellar Cell) charge — needs the 7x7x7 shell");
    private static final ConfigValue<Integer> ADVANCED_YIELD_BONUS = SCHEMA.intRange("advancedOreProcessorYieldBonus",
            1, 0, 64, true, "extra dust the Advanced Ore Processor yields over the Tier-1 processor");
    private static final ConfigValue<String> SOLAR_DIM_MULTIPLIERS = SCHEMA.string("solarDimensionMultipliers",
            "", true, "per-dimension solar multiplier FALLBACK (since Stage H, nerospace.api planet traits take "
            + "precedence for known planets when Nerospace is installed): comma-list of dimensionId=multiplier, "
            + "e.g. nerospace:greenxertz=1.5,nerospace:glacira=0.6 (Earth defaults to 1.0)");

    // --- telemetry (anonymous crash reporting; CLIENT-LOCAL opt-out, not server-synced) -----
    private static final ConfigValue<Boolean> TELEMETRY_ENABLED = SCHEMA.bool("telemetryEnabled",
            true, false, "anonymous error reporting to the developers (stack trace + mod/MC/loader/OS/Java "
            + "versions only — never names, UUIDs, IPs, or world data; POPIA/GDPR-compliant). Set false to "
            + "opt out");

    // --- client rendering (CLIENT-LOCAL quality toggle, not server-synced) ------------------
    private static final ConfigValue<Boolean> RENDER_ANIMATIONS = SCHEMA.bool("renderAnimationsEnabled",
            true, false, "animated machine visuals (spinning turbines/drums, sun-tracking solar deck, "
            + "fabricator arms, plasma torus, holograms). Set false on low-end clients: block-entity "
            + "renderers then draw a single static parked frame instead");

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

    public static int thermalEnvLossPermille() {
        return THERMAL_ENV_LOSS.get();
    }

    public static int thermalConductivityPermille() {
        return THERMAL_CONDUCTIVITY.get();
    }

    public static int thermalExchangeIntervalTicks() {
        return THERMAL_EXCHANGE_INTERVAL.get();
    }

    public static int thermalAmbientDefault() {
        return THERMAL_AMBIENT_DEFAULT.get();
    }

    public static String thermalAmbientByDimension() {
        return THERMAL_AMBIENT_BY_DIMENSION.get();
    }

    public static int thermalBiomeScale() {
        return THERMAL_BIOME_SCALE.get();
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

    public static int pollutionEventThreshold() {
        return POLLUTION_EVENT_THRESHOLD.get();
    }

    public static int scrubberNePerOp() {
        return SCRUBBER_NE_PER_OP.get();
    }

    public static int scrubberPollutionPerOp() {
        return SCRUBBER_RATE.get();
    }

    public static int scrubberAdjacentPermille() {
        return SCRUBBER_ADJACENT.get();
    }

    public static int scrubberFilterCapacity() {
        return SCRUBBER_FILTER_CAPACITY.get();
    }

    public static int remediatorNePerOp() {
        return REMEDIATOR_NE_PER_OP.get();
    }

    public static int remediatorPollutionPerOp() {
        return REMEDIATOR_RATE.get();
    }

    public static int analyticsTerminalRadius() {
        return ANALYTICS_RADIUS.get();
    }

    public static int fusionReactorNePerTick() {
        return FUSION_NE_PER_TICK.get();
    }

    public static boolean fusionReactorMeltdownEnabled() {
        return FUSION_FAILURE.get();
    }

    public static String fusionSizeOutputPermille() {
        return FUSION_SIZE_OUTPUT.get();
    }

    /** Burn ticks for a fuel tier (1..3); tiers outside the range clamp to their nearest neighbour. */
    public static int fusionFuelBurnTicks(int tier) {
        return switch (Math.max(1, Math.min(3, tier))) {
            case 2 -> FUSION_T2_BURN.get();
            case 3 -> FUSION_T3_BURN.get();
            default -> FUSION_T1_BURN.get();
        };
    }

    public static int advancedOreProcessorYieldBonus() {
        return ADVANCED_YIELD_BONUS.get();
    }

    public static String solarDimensionMultipliers() {
        return SOLAR_DIM_MULTIPLIERS.get();
    }

    public static boolean telemetryEnabled() {
        return TELEMETRY_ENABLED.get();
    }

    /** Client-local BER quality toggle: false = static parked frames (never affects gameplay/balance). */
    public static boolean renderAnimationsEnabled() {
        return RENDER_ANIMATIONS.get();
    }

    /** Register the schema with Core (reads/creates {@code nerotech.properties}). Idempotent. */
    public static synchronized void load() {
        ConfigManager.register(SCHEMA);
    }
}
