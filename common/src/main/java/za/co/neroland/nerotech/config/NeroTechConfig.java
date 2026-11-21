package za.co.neroland.nerotech.config;

import za.co.neroland.nerolandcore.config.ConfigManager;
import za.co.neroland.nerolandcore.config.ConfigSchema;
import za.co.neroland.nerolandcore.config.ConfigValue;

import za.co.neroland.nerotech.machine.MeltdownMath;

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

    // --- Stage D power tech (the absorbed NeroPower feature set) -------------
    private static final ConfigValue<Integer> WIND_NE_PER_TICK = SCHEMA.intRange("windTurbineNePerTick",
            25, 1, 1_000_000, true, "base NE/tick produced by the Wind Turbine before the height curve "
            + "(0.5x at y=80 rising linearly to 2x at y=200) and the dimension multiplier");
    private static final ConfigValue<String> WIND_DIM_MULTIPLIERS = SCHEMA.string("windDimensionMultipliers",
            "minecraft:overworld=1.0", true, "per-dimension wind multiplier: comma-list of "
            + "dimensionId=multiplier (any dimension not listed defaults to 1.0). When Nerospace is "
            + "installed an AIRLESS planet always yields 0 — no atmosphere, no wind — ahead of this table");
    private static final ConfigValue<Integer> GEOTHERMAL_NE_PER_SOURCE = SCHEMA.intRange(
            "geothermalNePerTickPerSource", 8, 1, 1_000_000, true, "NE/tick the Geothermal Generator "
            + "produces per lava / magma block in the 3x3 directly beneath it (0-9 sources)");
    private static final ConfigValue<Integer> BIO_NE_PER_BURN_TICK = SCHEMA.intRange("bioGeneratorNePerBurnTick",
            48, 1, 1_000_000, true, "NE/tick produced by the Bio Generator while burning a "
            + "#nerotech:bio_fuels item — 20% above the Nero Generator, for half the pollution");
    private static final ConfigValue<Integer> BATTERY_BANK_CAPACITY = SCHEMA.intRange("batteryBankCapacity",
            1_000_000, 1_000, 1_000_000_000, true, "internal NE buffer of one Battery Bank");
    private static final ConfigValue<Integer> GRID_RADIUS = SCHEMA.intRange("gridControllerRadius",
            16, 4, 64, true, "block radius the Grid Controller watches for NeroTech machines (loaded "
            + "chunks only, batched every 100 ticks — large radii make each scan pass more expensive)");
    private static final ConfigValue<Integer> GRID_SHED_PERMILLE = SCHEMA.intRange("gridShedThresholdPermille",
            200, 0, 1000, true, "aggregate grid fill (permille) below which the Grid Controller sheds load "
            + "by switching non-generator machines to the Eco preset");
    private static final ConfigValue<Integer> GRID_RESTORE_PERMILLE = SCHEMA.intRange("gridRestorePermille",
            500, 0, 1000, true, "aggregate grid fill (permille) above which the Grid Controller restores "
            + "each shed machine's previous preset (keep it above gridShedThresholdPermille to avoid flapping)");
    private static final ConfigValue<Integer> GRID_SCALE_CAPACITY = SCHEMA.intRange("gridScaleMilestoneCapacity",
            5_000_000, 0, 2_000_000_000, true, "summed NE capacity of the machines one Grid Controller watches "
            + "at which the Grid Controller's owner earns the Grid Scale progression record (a player-scope "
            + "Core gate quest packs read; 0 disables the record)");
    private static final ConfigValue<Integer> WIRELESS_RANGE = SCHEMA.intRange("wirelessNodeRange",
            32, 1, 256, true, "maximum block distance between two paired Wireless Power Nodes "
            + "(same dimension only; a node never force-loads its partner's chunk)");
    private static final ConfigValue<Integer> WIRELESS_TRANSFER = SCHEMA.intRange("wirelessNodeTransferPerTick",
            200, 0, 10_000_000, true, "NE a Wireless Power Node sends to its partner per transfer pass "
            + "(the pass runs every 5 ticks); transfer is lossless — the node's value is convenience, not gain");

    // --- processing machines ------------------------------------------------
    private static final ConfigValue<Integer> PROCESS_BASE_TICKS = SCHEMA.intRange("machineBaseProcessTicks",
            120, 1, 72_000, true, "base ticks one Ore Processor / Fabricator operation takes (before Speed modules)");
    private static final ConfigValue<Integer> PROCESS_NE_PER_TICK = SCHEMA.intRange("machineNePerTick",
            30, 0, 1_000_000, true, "base NE/tick a processing machine consumes while working (before Efficiency)");

    // --- optional per-operation charges (compat/neroeconomy; needs NeroEconomy 0.3.0+) ---------
    private static final ConfigValue<Integer> OP_CHARGE_COST = SCHEMA.intRange("machineOperationChargeCost",
            0, 0, 1_000_000, true, "credits an owned processing machine (Ore Processor, Fabricator, their Advanced "
            + "tiers, Chemical Processor) charges its owner, in advance, for each batch of "
            + "machineOperationChargeBatch operations. 0 (default) = off. Needs NeroEconomy; without it "
            + "machines always run free. Charges go to the economy's Reserve; a machine never pays out. "
            + "When above 0, machines record the placing player's UUID as owner (erasable; see PRIVACY.md)");
    private static final ConfigValue<Integer> OP_CHARGE_BATCH = SCHEMA.intRange("machineOperationChargeBatch",
            64, 1, 4_096, true, "operations one machineOperationChargeCost payment covers (64 = one stack). "
            + "Charging per batch, never per tick, keeps a machine well inside the economy's rate limit");

    // --- heat (the consequence axis: the full thermal model) ----
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

    // --- fluid & gas machines (Stage C: Electrolyzer, Gas Turbine, Chemical Processor) ----------
    private static final ConfigValue<Integer> MACHINE_GAS_CAPACITY = SCHEMA.intRange("machineGasCapacity",
            4_000, 100, 10_000_000, true, "capacity (mB) of each internal machine gas tank; one balance "
            + "\"unit\" of gas is 100 mB (capacity changes apply when the machine next loads)");
    private static final ConfigValue<Integer> MACHINE_FLUID_CAPACITY = SCHEMA.intRange("machineFluidCapacity",
            4_000, 1_000, 10_000_000, true, "capacity (mB) of each internal machine fluid tank — the "
            + "Electrolyzer's water tank (4 buckets by default)");
    private static final ConfigValue<Integer> ELECTROLYZER_NE_PER_TICK = SCHEMA.intRange("electrolyzerNePerTick",
            40, 0, 1_000_000, true, "NE/tick the Electrolyzer draws while splitting water (before Efficiency)");
    private static final ConfigValue<Integer> ELECTROLYZER_TICKS = SCHEMA.intRange("electrolyzerOperationTicks",
            200, 1, 72_000, true, "base ticks one electrolysis operation takes (before Speed modules)");
    private static final ConfigValue<Integer> ELECTROLYZER_WATER = SCHEMA.intRange("electrolyzerWaterPerOp",
            500, 1, 1_000_000, true, "water (mB) consumed per electrolysis operation (a bucket is 1000 mB)");
    private static final ConfigValue<Integer> ELECTROLYZER_HYDROGEN = SCHEMA.intRange("electrolyzerHydrogenPerOp",
            200, 0, 1_000_000, true, "hydrogen (mB) produced per operation — 2 units at the default 100 mB/unit");
    private static final ConfigValue<Integer> ELECTROLYZER_OXYGEN = SCHEMA.intRange("electrolyzerOxygenPerOp",
            100, 0, 1_000_000, true, "oxygen (mB) produced per operation — 1 unit; the 2:1 split is the point");
    private static final ConfigValue<Integer> GAS_TURBINE_NE_PER_UNIT = SCHEMA.intRange("gasTurbineNePerUnit",
            60, 0, 10_000_000, true, "NE one unit (100 mB) of gas yields, before the fuel's turbineGasBurn "
            + "multiplier. Clean power, not free power: electrolysis is a net energy SINK by design");
    private static final ConfigValue<Integer> GAS_TURBINE_TICKS_PER_UNIT = SCHEMA.intRange("gasTurbineTicksPerUnit",
            20, 1, 72_000, true, "ticks one unit of gas burns for (the NE yield is spread evenly across them)");
    private static final ConfigValue<String> TURBINE_GAS_BURN = SCHEMA.string("turbineGasBurn",
            "nerotech:hydrogen=2,nerotech:methane=4", true, "which gases the Gas Turbine burns and how well: comma-list of "
            + "gasId=multiplier, e.g. nerotech:hydrogen=2,nerospace:oxygen=1. Yield per unit is "
            + "gasTurbineNePerUnit x multiplier; a gas not listed here is not accepted at all");
    private static final ConfigValue<Integer> CHEMICAL_GAS_PER_OP = SCHEMA.intRange("chemicalProcessorGasPerOp",
            250, 0, 1_000_000, true, "oxygen (mB) one Chemical Processor operation consumes on top of its NE");

    // --- coolant loop (Stage C: Radiator + Coolant Pump) ------------------------
    private static final ConfigValue<Integer> COOLANT_PUMP_NE_PER_TICK = SCHEMA.intRange("coolantPumpNePerTick",
            20, 0, 1_000_000, true, "NE/tick the Coolant Pump draws while pumping (billed in one batch per "
            + "thermalExchangeIntervalTicks, never per tick)");
    private static final ConfigValue<Integer> COOLANT_PUMP_HEAT_PER_OP = SCHEMA.intRange("coolantPumpHeatPerOp",
            20, 1, 1_000_000, true, "heat the Coolant Pump pulls from EACH adjacent machine per exchange, "
            + "before the radiator multiplier (each Radiator within 3 blocks in a straight line adds +1x)");

    // --- automation & QoL (Stage E: Robotic Arm) / exotic endgame (Stage F: Singularity Vault) ---
    private static final ConfigValue<Integer> ARM_NE_PER_MOVE = SCHEMA.intRange("roboticArmNePerMove",
            4, 0, 1_000_000, true, "NE the Robotic Arm spends per ITEM moved (it runs one transfer "
            + "pass per second; a pass moves at most roboticArmStackPerMove items)");
    private static final ConfigValue<Integer> ARM_STACK_PER_MOVE = SCHEMA.intRange("roboticArmStackPerMove",
            8, 1, 1_000, true, "how many items one Robotic Arm transfer pass moves (before Speed modules)");
    private static final ConfigValue<Integer> VAULT_CAPACITY = SCHEMA.intRange("singularityVaultCapacity",
            1_000_000, 64, 1_000_000_000, true, "how many of ONE item type a Singularity Vault holds "
            + "(the two facade slots sit on top of this; a comparator reads fill against it)");

    // --- pollution (regional, periodic aggregate) ---------------------------
    private static final ConfigValue<Integer> POLLUTION_PER_OP = SCHEMA.intRange("pollutionPerOperation",
            2, 0, 100_000, true, "pollution a machine emits per contribution (0 disables; solar emits none)");
    private static final ConfigValue<Integer> POLLUTION_CONTRIB_INTERVAL = SCHEMA.intRange("pollutionContributionIntervalTicks",
            40, 1, 72_000, true, "how often (ticks) a running machine adds to its region (batched, not per-tick)");
    private static final ConfigValue<Integer> ANNUNCIATOR_POLLUTION_THRESHOLD = SCHEMA.intRange(
            "annunciatorPollutionThreshold", 8, 0, 100_000, true, "pollution a machine may emit in 5 s before "
            + "its 'polluting' alarm lights (Annunciator Panel, link alarm event); a Balanced processor "
            + "emits about 5, Overdrive about 10");
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

    // --- Tier 2/3 (no gate: paced by Starsteel-tier recipes alone) ----------
    private static final ConfigValue<Integer> FUSION_NE_PER_TICK = SCHEMA.intRange("fusionReactorNePerTick",
            400, 1, 10_000_000, true, "NE/tick the Fusion Reactor produces while running (high-output late-game)");
    private static final ConfigValue<Boolean> FUSION_FAILURE = SCHEMA.bool("fusionReactorMeltdownEnabled",
            true, true, "true: an unmanaged Fusion Reactor melts down destructively at max heat (telegraphed by "
            + "the red gauge); false (survival-friendly): it just stalls until it cools");
    private static final ConfigValue<String> FUSION_MELTDOWN_TERRAIN = SCHEMA.string("fusionMeltdownTerrainDamage",
            "auto", true, "whether a Fusion Reactor meltdown destroys terrain: on / off / auto (auto = off on a "
            + "dedicated server, on in singleplayer and LAN); off keeps the blast (damage, knockback, the "
            + "reactor itself is still lost) but breaks no blocks");
    private static final ConfigValue<Integer> FUSION_MELTDOWN_RADIUS_CAP = SCHEMA.intRange("fusionMeltdownRadiusCap",
            8, 1, 16, true, "hard cap on a Fusion Reactor meltdown's blast radius (blocks); the radius is "
            + "shell size + 1 (4/6/8 for the 3/5/7 shells), never more than this");
    private static final ConfigValue<String> FUSION_SIZE_OUTPUT = SCHEMA.string("fusionSizeOutputPermille",
            "3=1000,5=4000,7=12000", true, "multiblock output multiplier (permille of fusionReactorNePerTick) "
            + "per shell size: comma-list of size=permille for the 3/5/7 shells (Stage E multiblock)");
    private static final ConfigValue<Integer> FUSION_T1_BURN = SCHEMA.intRange("fusionFuelTier1BurnTicks",
            1_600, 1, 720_000, true, "burn ticks of one tier-1 fuel (Fusion Cell) charge");
    private static final ConfigValue<Integer> FUSION_T2_BURN = SCHEMA.intRange("fusionFuelTier2BurnTicks",
            4_800, 1, 720_000, true, "burn ticks of one tier-2 fuel (Plasma Cell) charge — needs a 5x5x5+ shell");
    private static final ConfigValue<Integer> FUSION_T3_BURN = SCHEMA.intRange("fusionFuelTier3BurnTicks",
            14_400, 1, 720_000, true, "burn ticks of one tier-3 fuel (Stellar Cell) charge — needs the 7x7x7 shell");
    private static final ConfigValue<Integer> FUSION_T4_BURN = SCHEMA.intRange("fusionFuelTier4BurnTicks",
            28_800, 1, 720_000, true, "burn ticks of one tier-4 fuel (Antimatter Cell) charge — twice a "
            + "Stellar Cell, ONLY the 7x7x7 shell will contain it, and burning it adds +2 to the "
            + "reactor's heat rate (the meltdown risk is the price)");
    private static final ConfigValue<Integer> ADVANCED_YIELD_BONUS = SCHEMA.intRange("advancedOreProcessorYieldBonus",
            1, 0, 64, true, "extra dust the Advanced Ore Processor yields over the Tier-1 processor");
    // --- Particle Accelerator (free-form ring; the standalone route to space-grade dusts) -----
    private static final ConfigValue<Integer> ACCEL_MAX_GAP = SCHEMA.intRange("acceleratorMaxGap",
            16, 1, 64, true, "blocks the Accelerator Controller marches along the current heading looking "
            + "for the next Accelerator Guide Coil; nothing found within this and the beam line is OPEN");
    private static final ConfigValue<Integer> ACCEL_MAX_GUIDES = SCHEMA.intRange("acceleratorMaxGuides",
            256, 4, 4_096, true, "safety cap on guides in one traced beam line (a pathological build stops "
            + "here and reads as an open line); the trace costs O(guides x acceleratorMaxGap) block reads");
    private static final ConfigValue<Double> ACCEL_TICK_SCALE = SCHEMA.doubleRange("acceleratorTickScale",
            0.05D, 0.001D, 1.0D, true, "blocks a particle travels per tick per unit of speed "
            + "(0.05 = speed 100 covers 5 blocks/tick)");
    private static final ConfigValue<Double> ACCEL_LAUNCH_SPEED = SCHEMA.doubleRange("acceleratorLaunchSpeed",
            10.0D, 0.1D, 10_000.0D, true, "floor on the injection speed; the controller actually injects at "
            + "the slowest speed the loop's LONGEST stretch tolerates (see acceleratorMinGapAllowance), so a "
            + "wide ring starts fast and a tight one crawls");
    private static final ConfigValue<Double> ACCEL_BOOST_PER_GUIDE = SCHEMA.doubleRange("acceleratorBoostPerGuide",
            2.0D, 0.0D, 10_000.0D, true, "speed a powered Accelerator Guide Coil adds as the particle passes");
    private static final ConfigValue<Integer> ACCEL_NE_PER_GUIDE = SCHEMA.intRange("acceleratorNePerGuide",
            50, 0, 10_000_000, true, "NE the controller spends per guide passed (and once to inject); with no "
            + "NE the particle coasts instead of accelerating");
    private static final ConfigValue<Double> ACCEL_DRAG_PER_GUIDE = SCHEMA.doubleRange("acceleratorDragPerGuide",
            0.5D, 0.0D, 10_000.0D, true, "speed an UNPOWERED guide bleeds off a coasting particle — coast long "
            + "enough and it drops below the gap rule and is lost");
    private static final ConfigValue<Double> ACCEL_MIN_GAP_ALLOWANCE = SCHEMA.doubleRange(
            "acceleratorMinGapAllowance", 4.0D, 0.0D, 256.0D, true, "gap rule: the longest stretch a particle "
            + "at speed 0 could cross. The real allowance is this + speed x acceleratorGapPerSpeed — slow "
            + "particles need close guides");
    private static final ConfigValue<Double> ACCEL_GAP_PER_SPEED = SCHEMA.doubleRange("acceleratorGapPerSpeed",
            0.12D, 0.0D, 100.0D, true, "gap rule: extra blocks of allowed stretch per unit of speed");
    private static final ConfigValue<Double> ACCEL_BEND_SPEED_BASE = SCHEMA.doubleRange(
            "acceleratorBendSpeedBase", 20.0D, 0.1D, 100_000.0D, true, "bend rule: a 45 degree turn survives a "
            + "speed of at most this x the length of the stretch before it — the shortest bend stretch in a "
            + "loop caps the whole loop's top speed, so big rings are what buy big speeds");
    private static final ConfigValue<Integer> ACCEL_ENERGY_SCALE = SCHEMA.intRange("acceleratorEnergyScale",
            500, 1, 1_000_000, true, "permille scale in the collision energy formula E = 0.5 x speed^2 x scale; "
            + "collider recipes gate on the resulting joules");

    private static final ConfigValue<String> SOLAR_DIM_MULTIPLIERS = SCHEMA.string("solarDimensionMultipliers",
            "", true, "per-dimension solar multiplier FALLBACK (since Stage H, nerospace.api planet traits take "
            + "precedence for known planets when Nerospace is installed): comma-list of dimensionId=multiplier, "
            + "e.g. nerospace:greenxertz=1.5,nerospace:glacira=0.6 (Earth defaults to 1.0)");

    // --- virtual deposits (Deep Core Drill / Survey Scanner) -------------------------------
    private static final ConfigValue<Integer> DEPOSIT_YIELD_PERMILLE = SCHEMA.intRange("depositYieldPermille",
            1_000, 0, 100_000, true, "permille scale on what a drilled deposit unit yields (1000 = the table's "
            + "richness as written; 0 = drills extract nothing)");
    private static final ConfigValue<Boolean> DEPOSIT_DEPLETION_ENABLED = SCHEMA.bool("depositDepletionEnabled",
            true, true, "whether drilling depletes a chunk's deposit (false = deposits never run out and no "
            + "depletion record is ever stored)");
    private static final ConfigValue<Integer> DEPOSIT_RECOVERY_PER_DAY = SCHEMA.intRange("depositRecoveryPerDay",
            200, 0, 1_000_000, true, "units a depleted deposit recovers per in-game day (24000 ticks), up to "
            + "its capacity; 0 = deposits never recover");
    private static final ConfigValue<String> DEPOSIT_DISABLED_DIMENSIONS = SCHEMA.string("depositDisabledDimensions",
            "", true, "comma-list of dimension ids with no deposits at all (surveys read empty, drills idle), "
            + "e.g. minecraft:the_end");

    // --- 0.8.0 machine families (Stage 2) ------------------------------------------------
    // metallurgy
    private static final ConfigValue<Integer> PLASMA_CRUCIBLE_FIRING_ENERGY = SCHEMA.intRange("plasmaCrucibleFiringEnergy",
            60, 1, 100_000, true, "NE per tick the Plasma Crucible spends firing its arc while it is below a recipe's "
            + "min_heat (scaled by Efficiency modules and the overclock preset)");
    private static final ConfigValue<Integer> PLASMA_CRUCIBLE_FIRING_HEAT = SCHEMA.intRange("plasmaCrucibleFiringHeat",
            12, 1, 100_000, true, "heat one firing tick adds (firing never reaches heatThrottleThreshold)");
    private static final ConfigValue<Integer> PLASMA_CRUCIBLE_HOLD_INTERVAL_TICKS = SCHEMA.intRange("plasmaCrucibleHoldIntervalTicks",
            20, 1, 72_000, true, "an idle Plasma Crucible exchanges heat with its surroundings only once every this many "
            + "ticks, so it holds its temperature between jobs (1 = no insulation)");
    // foundry
    private static final ConfigValue<Integer> FOUNDRY_BATCH_UNITS = SCHEMA.intRange("foundryBatchUnits",
            4, 1, 64, true, "recipe units a 3x3x3 Blast Foundry runs per cycle (energy per tick scales with the "
            + "units)");
    private static final ConfigValue<Integer> FOUNDRY_LARGE_BATCH_MULTIPLIER = SCHEMA.intRange("foundryLargeBatchMultiplier",
            4, 1, 16, true, "how many times the 3x3x3 batch a 5x5x5 Blast Foundry runs per cycle");
    private static final ConfigValue<Integer> FOUNDRY_HEAT_PORT_CAPACITY = SCHEMA.intRange("foundryHeatPortCapacity",
            500, 1, 100_000, true, "heat a Foundry Heat Port buffers for its controller to drain");
    private static final ConfigValue<Integer> FOUNDRY_HEAT_LOSS_PERMILLE = SCHEMA.intRange("foundryHeatLossPermille",
            5, 0, 1_000, true, "permille of the furnace's heat above ambient lost per tick (hot dimensions lose less; "
            + "the furnace never warms toward ambient)");
    private static final ConfigValue<Integer> FOUNDRY_HEAT_DRAW_PER_TICK = SCHEMA.intRange("foundryHeatDrawPerTick",
            2, 0, 100_000, true, "furnace heat a running batch uses each tick");
    private static final ConfigValue<Integer> FOUNDRY_HEAT_INTAKE_INTERVAL_TICKS = SCHEMA.intRange("foundryHeatIntakeIntervalTicks",
            5, 1, 200, true, "how often (ticks) the controller drains its heat ports");
    private static final ConfigValue<Integer> FOUNDRY_LIT_HOLD_TICKS = SCHEMA.intRange("foundryLitHoldTicks",
            40, 0, 1_200, true, "ticks the shell stays lit after the foundry stops working (avoids flicker on short "
            + "stalls)");
    // chemistry
    private static final ConfigValue<String> CONDENSER_ATMOSPHERES = SCHEMA.string("condenserAtmospheres",
            "minecraft:the_nether=SULFUROUS,minecraft:the_end=VACUUM", true, "Atmospheric Condenser air per dimension when Nerospace has no say (absent, api drift, or "
            + "not a Nerospace planet): comma-list of dimensionId=ATMOSPHERE with ATMOSPHERE one of "
            + "VACUUM, THIN_CO2, SULFUROUS, NITROGEN_METHANE, NITROGEN_OXYGEN, EARTHLIKE. Unlisted "
            + "dimensions are EARTHLIKE");
    private static final ConfigValue<Integer> CRYO_COLD_BONUS_PER_HEAT_PERMILLE = SCHEMA.intRange("cryoColdBonusPerHeatPermille",
            5, 0, 1_000, true, "Cryo Distiller speed change per point of ambient heat, permille: factor = 1 - ambient x "
            + "this / 1000 (Glacira -80 gives 1.4)");
    private static final ConfigValue<Integer> CRYO_SPEED_MIN_PERMILLE = SCHEMA.intRange("cryoSpeedMinPermille",
            500, 1, 1_000, true, "Cryo Distiller speed floor in hot places, permille of the recipe rate");
    private static final ConfigValue<Integer> CRYO_SPEED_MAX_PERMILLE = SCHEMA.intRange("cryoSpeedMaxPermille",
            2_000, 1_000, 10_000, true, "Cryo Distiller speed cap in cold places, permille of the recipe rate");
    private static final ConfigValue<Integer> CENTRIFUGE_CASCADE_PUSH_PER_TICK = SCHEMA.intRange("centrifugeCascadePushPerTick",
            200, 1, 100_000, true, "gas (mB) a Centrifuge passes to the next one in its cascade each tick");
    // components
    private static final ConfigValue<Integer> CRYSTAL_GROWER_UNPOWERED_PERMILLE = SCHEMA.intRange("crystalGrowerUnpoweredPermille",
            250, 0, 1_000, true, "growth speed of the Crystal Grower with no power, as permille of its powered speed (0 = "
            + "it stalls unpowered like any other machine)");
    private static final ConfigValue<Integer> CRYSTAL_GROWER_COLD_THRESHOLD = SCHEMA.intRange("crystalGrowerColdThreshold",
            0, -100_000, 100_000, true, "ambient heat at or above which the Crystal Grower gets no cold bonus");
    private static final ConfigValue<Integer> CRYSTAL_GROWER_COLD_BONUS_PER_HEAT_PERMILLE = SCHEMA.intRange("crystalGrowerColdBonusPerHeatPermille",
            10, 0, 1_000, true, "extra Crystal Grower speed, in permille, per point of ambient heat below the threshold");
    private static final ConfigValue<Integer> CRYSTAL_GROWER_COLD_BONUS_CAP_PERMILLE = SCHEMA.intRange("crystalGrowerColdBonusCapPermille",
            1_000, 0, 4_000, true, "the most the cold bonus can add, in permille (1000 = twice as fast)");
    // heat
    private static final ConfigValue<Integer> HEAT_PIPE_MAX_RUN = SCHEMA.intRange("heatPipeMaxRun",
            64, 1, 256, true, "most Heat Pipes one run resolves (flood-fill cap; pipes beyond it form their own run)");
    private static final ConfigValue<Integer> HEAT_PIPE_CONDUCTIVITY_PERMILLE = SCHEMA.intRange("heatPipeConductivityPermille",
            250, 0, 1_000, true, "permille of each endpoint-vs-run temperature difference a Heat Pipe run moves per "
            + "exchange (0 disables pipes)");
    private static final ConfigValue<Integer> HEAT_PIPE_LOSS_PERMILLE = SCHEMA.intRange("heatPipeLossPermille",
            1, 0, 1_000, true, "permille of each pipe's heat above ambient lost per exchange, so long runs carry less");
    private static final ConfigValue<Integer> HEAT_PIPE_EXCHANGE_INTERVAL_TICKS = SCHEMA.intRange("heatPipeExchangeIntervalTicks",
            10, 1, 72_000, true, "ticks between a run's exchanges (only the run's anchor pipe ticks)");
    private static final ConfigValue<Boolean> HEAT_PIPE_CONTACT_DAMAGE = SCHEMA.bool("heatPipeContactDamage",
            true, true, "whether a Heat Pipe at heat level 2 or more hurts entities standing on it (sneaking "
            + "avoids it)");
    private static final ConfigValue<Double> HEAT_PIPE_CONTACT_DAMAGE_AMOUNT = SCHEMA.doubleRange("heatPipeContactDamageAmount",
            1.0D, 0.0D, 20.0D, true, "damage per contact hit at level 2 (doubled at level 3)");
    private static final ConfigValue<Integer> HEAT_EXCHANGER_MAX_PER_OP = SCHEMA.intRange("heatExchangerMaxPerOp",
            60, 1, 100_000, true, "most heat one Heat Exchanger exchange moves, before Speed modules and the overclock "
            + "preset");
    private static final ConfigValue<Integer> HEAT_EXCHANGER_CONDUCTIVITY_PERMILLE = SCHEMA.intRange("heatExchangerConductivityPermille",
            300, 0, 1_000, true, "permille of the machine-vs-tank difference moved per exchange for a storing medium "
            + "(water and anything not listed as a coolant or hot medium)");
    private static final ConfigValue<String> HEAT_EXCHANGER_COOLANTS = SCHEMA.string("heatExchangerCoolants",
            "nerotech:cryo_coolant=4,nerotech:nitrogen=2", true, "coolant media as id=heatPerMb: absorb machine heat above ambient and are used up");
    private static final ConfigValue<String> HEAT_EXCHANGER_HOT_MEDIA = SCHEMA.string("heatExchangerHotMedia",
            "minecraft:lava=2", true, "hot media as id=heatPerMb: heat the machine and are used up");
    private static final ConfigValue<Integer> HEAT_EXCHANGER_HOT_TEMPERATURE = SCHEMA.intRange("heatExchangerHotTemperature",
            700, 0, 1_000_000, true, "heat a hot medium raises a machine to, at most");
    // extraction
    private static final ConfigValue<Integer> DRILL_OPERATION_TICKS = SCHEMA.intRange("drillOperationTicks",
            200, 20, 12_000, true, "ticks one Deep Core Drill operation takes at 1x speed; the deposit is read once per "
            + "operation");
    private static final ConfigValue<Integer> DRILL_ENERGY_PER_TICK = SCHEMA.intRange("drillEnergyPerTick",
            64, 0, 100_000, true, "NE per tick the drill draws while it works, before modules and preset");
    private static final ConfigValue<Integer> DRILL_DEPLETED_RETRY_TICKS = SCHEMA.intRange("drillDepletedRetryTicks",
            1_200, 20, 72_000, true, "ticks a drill waits before it retries a depleted or missing deposit");
    private static final ConfigValue<Integer> SURVEY_SCANNER_CAPACITY = SCHEMA.intRange("surveyScannerCapacity",
            10_000, 100, 1_000_000, true, "NE the Survey Scanner holds");
    private static final ConfigValue<Integer> SURVEY_SCANNER_SCAN_COST = SCHEMA.intRange("surveyScannerScanCost",
            500, 0, 1_000_000, true, "NE one survey costs");
    private static final ConfigValue<Integer> SURVEY_SCANNER_COOLDOWN_TICKS = SCHEMA.intRange("surveyScannerCooldownTicks",
            40, 0, 1_200, true, "ticks between two surveys with the same scanner");
    // fusion
    private static final ConfigValue<Integer> FUSION_GAS_MIN_SHELL_SIZE = SCHEMA.intRange("fusionGasMinShellSize",
            7, 3, 7, true, "smallest fusion shell edge that can contain the gas-fed burn (gas is the top fuel tier, "
            + "so by default only the 7x7x7 shell takes it)");
    private static final ConfigValue<Integer> FUSION_GAS_CHARGE_TICKS = SCHEMA.intRange("fusionGasChargeTicks",
            20, 1, 1_200, true, "ticks of burn bought by one gas draw; gas is drawn per charge across the shell's Fuel "
            + "Injectors, never per tick");
    private static final ConfigValue<Integer> FUSION_GAS_DEUTERIUM_PER_TICK = SCHEMA.intRange("fusionGasDeuteriumPerTick",
            2, 1, 1_000, true, "mB of deuterium burned per tick of gas-fed burn");
    private static final ConfigValue<Integer> FUSION_GAS_HELIUM3_PER_TICK = SCHEMA.intRange("fusionGasHelium3PerTick",
            1, 1, 1_000, true, "mB of helium-3 burned per tick of gas-fed burn; a charge needs both gases and a surplus "
            + "of one never covers the other");
    private static final ConfigValue<Integer> FUSION_GAS_OUTPUT_PERMILLE = SCHEMA.intRange("fusionGasOutputPermille",
            1_500, 0, 10_000, true, "output of a gas-fed burn against a fuel-cell burn of the same shell (1500 = x1.5), on "
            + "top of fusionSizeOutputPermille");
    private static final ConfigValue<Integer> FUEL_INJECTOR_GAS_CAPACITY = SCHEMA.intRange("fuelInjectorGasCapacity",
            8_000, 100, 1_000_000, true, "mB of each gas one Fuel Injector holds (two tanks: deuterium, helium-3)");
    private static final ConfigValue<Integer> FUSION_LATTICE_HEAT_REDUCTION_PERMILLE = SCHEMA.intRange("fusionLatticeHeatReductionPermille",
            60, 0, 1_000, true, "share of the reactor's heat per burning tick that each counted Europium Flux Lattice "
            + "removes");
    private static final ConfigValue<Integer> FUSION_LATTICE_MAX_COUNT = SCHEMA.intRange("fusionLatticeMaxCount",
            8, 0, 98, true, "lattices beyond this many in one shell add nothing (default cap: 8 x 60 = 48% less heat)");
    // utility
    private static final ConfigValue<Boolean> INDUCTION_CHARGE_PLAYERS = SCHEMA.bool("inductionChargePlayers",
            true, true, "Induction Chargers charge energy items held or worn by players standing in range. "
            + "Proximity only; nothing about the player is stored. Off leaves the entity charging API "
            + "working");
    private static final ConfigValue<Integer> INDUCTION_CHARGER_RADIUS = SCHEMA.intRange("inductionChargerRadius",
            4, 0, 16, true, "reach of an Induction Charger in blocks on every axis (a bounded box)");
    private static final ConfigValue<Integer> INDUCTION_CHARGER_TRANSFER_PER_TICK = SCHEMA.intRange("inductionChargerTransferPerTick",
            256, 0, 100_000, true, "NE one charger may hand out per tick (scaled by Speed modules and the overclock preset); "
            + "player charging and the entity API share it");
    private static final ConfigValue<Boolean> ANNUNCIATOR_ALARM_SOUND = SCHEMA.bool("annunciatorAlarmSound",
            true, true, "an Annunciator Panel beeps once when a new alarm lights");
    private static final ConfigValue<Integer> ANNUNCIATOR_REFRESH_TICKS = SCHEMA.intRange("annunciatorRefreshTicks",
            10, 1, 200, true, "how often an Annunciator Panel re-reads the machines touching it");
    // upgrade modules (NeroTech-classified; Core's UpgradeType is closed)
    private static final ConfigValue<Integer> HEAT_SINK_MODULE_HEADROOM_PERMILLE = SCHEMA.intRange("heatSinkModuleHeadroomPermille",
            125, 0, 250, true, "share of the headroom between heatThrottleThreshold and heatCapacity each Heat Sink Module "
            + "opens (at most 4 count, and at most half the headroom opens in total)");
    private static final ConfigValue<Integer> SCRUBBER_MODULE_POLLUTION_PERMILLE = SCHEMA.intRange("scrubberModulePollutionPermille",
            200, 0, 1_000, true, "pollution per operation each Scrubber Module removes, permille of normal (at most 4 count)");
    private static final ConfigValue<Integer> SCRUBBER_MODULE_FLOOR_PERMILLE = SCHEMA.intRange("scrubberModuleFloorPermille",
            250, 0, 1_000, true, "Scrubber Modules never cut pollution below this permille of normal");
    // tools (all charge through Core energy; the Induction Charger fills them)
    private static final ConfigValue<Integer> POWER_DRILL_CAPACITY = SCHEMA.intRange("powerDrillCapacity",
            40_000, 100, 10_000_000, true, "NE the Power Drill holds");
    private static final ConfigValue<Integer> POWER_DRILL_ENERGY_PER_BLOCK = SCHEMA.intRange("powerDrillEnergyPerBlock",
            80, 0, 100_000, true, "NE the Power Drill spends per block it breaks (each extra block of an area or vein too)");
    private static final ConfigValue<Integer> POWER_DRILL_VEIN_MAX = SCHEMA.intRange("powerDrillVeinMaxBlocks",
            32, 1, 64, true, "most blocks one vein-mode break may take, the first included (hard cap 64)");
    private static final ConfigValue<Integer> CUTTING_LANCE_CAPACITY = SCHEMA.intRange("cuttingLanceCapacity",
            40_000, 100, 10_000_000, true, "NE the Cutting Lance holds");
    private static final ConfigValue<Integer> CUTTING_LANCE_ENERGY_PER_SHOT = SCHEMA.intRange("cuttingLanceEnergyPerShot",
            300, 0, 100_000, true, "NE one Cutting Lance cut costs before the lens factor");
    private static final ConfigValue<Integer> CUTTING_LANCE_COOLDOWN_TICKS = SCHEMA.intRange("cuttingLanceCooldownTicks",
            12, 0, 200, true, "ticks between two cuts before the lens factor");
    private static final ConfigValue<Integer> CUTTING_LANCE_RANGE = SCHEMA.intRange("cuttingLanceRange",
            16, 1, 32, true, "reach of the Cutting Lance in blocks (the Void lens doubles it; never past 32)");
    private static final ConfigValue<Integer> ARC_WRENCH_CAPACITY = SCHEMA.intRange("arcWrenchCapacity",
            10_000, 100, 10_000_000, true, "NE the Arc Wrench holds");
    private static final ConfigValue<Integer> ARC_WRENCH_ENERGY_PER_USE = SCHEMA.intRange("arcWrenchEnergyPerUse",
            200, 0, 100_000, true, "NE one Arc Wrench rotation, pick-up or placement costs");
    private static final ConfigValue<Integer> ARC_WRENCH_COOLDOWN_TICKS = SCHEMA.intRange("arcWrenchCooldownTicks",
            10, 0, 200, true, "ticks between two Arc Wrench actions");
    private static final ConfigValue<Integer> MULTIMETER_CAPACITY = SCHEMA.intRange("multimeterCapacity",
            5_000, 100, 10_000_000, true, "NE the Multimeter holds");
    private static final ConfigValue<Integer> MULTIMETER_ENERGY_PER_READING = SCHEMA.intRange("multimeterEnergyPerReading",
            25, 0, 100_000, true, "NE one Multimeter reading costs");
    private static final ConfigValue<Integer> MULTIMETER_COOLDOWN_TICKS = SCHEMA.intRange("multimeterCooldownTicks",
            10, 0, 200, true, "ticks between two Multimeter readings");
    private static final ConfigValue<Integer> THERMAL_GOGGLES_CAPACITY = SCHEMA.intRange("thermalGogglesCapacity",
            20_000, 100, 10_000_000, true, "NE the Thermal Goggles hold");
    private static final ConfigValue<Integer> THERMAL_GOGGLES_DRAIN = SCHEMA.intRange("thermalGogglesDrainPerSecond",
            5, 0, 100_000, true, "NE worn Thermal Goggles drain per second; the overlay shows only while charged");
    private static final ConfigValue<Integer> FILTER_MASK_CAPACITY = SCHEMA.intRange("filterMaskCapacity",
            10_000, 100, 10_000_000, true, "NE the Filter Mask holds");
    private static final ConfigValue<Integer> FILTER_MASK_ENERGY_PER_PULSE = SCHEMA.intRange("filterMaskEnergyPerPulse",
            20, 0, 100_000, true, "NE a worn Filter Mask spends each time it blocks a pollution exposure pulse");
    private static final ConfigValue<Integer> FILTER_MASK_PULSES_PER_CARTRIDGE = SCHEMA.intRange("filterMaskPulsesPerCartridge",
            36, 1, 10_000, true, "exposure pulses one fitted Filter Cartridge blocks before it fouls into a Dirty Filter");
    // pollution exposure (what the Filter Mask blocks)
    private static final ConfigValue<Boolean> POLLUTION_EXPOSURE_EFFECTS = SCHEMA.bool("pollutionExposureEffects",
            true, true, "players standing in a region at the severe pollution band (twice pollutionEventThreshold) get "
            + "brief Weakness and Hunger each pulse unless a charged Filter Mask with a cartridge blocks it; nothing is stored");
    private static final ConfigValue<Integer> POLLUTION_EXPOSURE_INTERVAL_TICKS = SCHEMA.intRange("pollutionExposureIntervalTicks",
            100, 20, 6_000, true, "ticks between two pollution exposure pulses (also the worn-tool drain tick)");

    // --- telemetry (anonymous crash reporting; CLIENT-LOCAL opt-out, not server-synced) -----
    private static final ConfigValue<Boolean> TELEMETRY_ENABLED = SCHEMA.bool("telemetryEnabled",
            true, false, "anonymous error reporting to the developers (stack trace + mod/MC/loader/OS/Java "
            + "versions only — never names, UUIDs, IPs, or world data; POPIA/GDPR-compliant). On by default; "
            + "set false to opt out (client-local, takes effect on restart). See PRIVACY.md");

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

    // --- Stage D power tech --------------------------------------------------

    public static int windTurbineNePerTick() {
        return WIND_NE_PER_TICK.get();
    }

    public static String windDimensionMultipliers() {
        return WIND_DIM_MULTIPLIERS.get();
    }

    public static int geothermalNePerTickPerSource() {
        return GEOTHERMAL_NE_PER_SOURCE.get();
    }

    public static int bioGeneratorNePerBurnTick() {
        return BIO_NE_PER_BURN_TICK.get();
    }

    public static int batteryBankCapacity() {
        return BATTERY_BANK_CAPACITY.get();
    }

    public static int gridControllerRadius() {
        return GRID_RADIUS.get();
    }

    public static int gridShedThresholdPermille() {
        return GRID_SHED_PERMILLE.get();
    }

    public static int gridRestorePermille() {
        return GRID_RESTORE_PERMILLE.get();
    }

    /** Summed grid capacity (NE) at which a Grid Controller records the Grid Scale milestone; 0 = never. */
    public static int gridScaleMilestoneCapacity() {
        return GRID_SCALE_CAPACITY.get();
    }

    public static int wirelessNodeRange() {
        return WIRELESS_RANGE.get();
    }

    public static int wirelessNodeTransferPerTick() {
        return WIRELESS_TRANSFER.get();
    }

    public static int machineBaseProcessTicks() {
        return PROCESS_BASE_TICKS.get();
    }

    public static int machineNePerTick() {
        return PROCESS_NE_PER_TICK.get();
    }

    /** Credits per batch of operations an owned processing machine charges its owner (0 = off). */
    public static int machineOperationChargeCost() {
        return OP_CHARGE_COST.get();
    }

    /** Operations one {@link #machineOperationChargeCost()} payment covers. */
    public static int machineOperationChargeBatch() {
        return OP_CHARGE_BATCH.get();
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

    public static int machineGasCapacity() {
        return MACHINE_GAS_CAPACITY.get();
    }

    public static int machineFluidCapacity() {
        return MACHINE_FLUID_CAPACITY.get();
    }

    public static int electrolyzerNePerTick() {
        return ELECTROLYZER_NE_PER_TICK.get();
    }

    public static int electrolyzerOperationTicks() {
        return ELECTROLYZER_TICKS.get();
    }

    public static int electrolyzerWaterPerOp() {
        return ELECTROLYZER_WATER.get();
    }

    public static int electrolyzerHydrogenPerOp() {
        return ELECTROLYZER_HYDROGEN.get();
    }

    public static int electrolyzerOxygenPerOp() {
        return ELECTROLYZER_OXYGEN.get();
    }

    public static int gasTurbineNePerUnit() {
        return GAS_TURBINE_NE_PER_UNIT.get();
    }

    public static int gasTurbineTicksPerUnit() {
        return GAS_TURBINE_TICKS_PER_UNIT.get();
    }

    /** Raw {@code gasId=multiplier} fuel map; parsed and cached by {@code gas.TurbineFuels}. */
    public static String turbineGasBurn() {
        return TURBINE_GAS_BURN.get();
    }

    public static int chemicalProcessorGasPerOp() {
        return CHEMICAL_GAS_PER_OP.get();
    }

    public static int coolantPumpNePerTick() {
        return COOLANT_PUMP_NE_PER_TICK.get();
    }

    public static int coolantPumpHeatPerOp() {
        return COOLANT_PUMP_HEAT_PER_OP.get();
    }

    /** Pollution a machine may emit in 5 s before its {@code polluting} alarm bit lights. */
    public static int annunciatorPollutionThreshold() {
        return ANNUNCIATOR_POLLUTION_THRESHOLD.get();
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

    public static int roboticArmNePerMove() {
        return ARM_NE_PER_MOVE.get();
    }

    public static int roboticArmStackPerMove() {
        return ARM_STACK_PER_MOVE.get();
    }

    public static int singularityVaultCapacity() {
        return VAULT_CAPACITY.get();
    }

    public static int fusionReactorNePerTick() {
        return FUSION_NE_PER_TICK.get();
    }

    public static boolean fusionReactorMeltdownEnabled() {
        return FUSION_FAILURE.get();
    }

    /**
     * Raw {@code fusionMeltdownTerrainDamage} setting: {@code "on"}, {@code "off"} or {@code "auto"}
     * (anything unrecognised reads as {@code "auto"}). Resolve it with
     * {@link #fusionMeltdownTerrainDamage(boolean)}.
     */
    public static String fusionMeltdownTerrainDamageMode() {
        String raw = FUSION_MELTDOWN_TERRAIN.get();
        if (raw == null) {
            return "auto";
        }
        String mode = raw.trim().toLowerCase(java.util.Locale.ROOT);
        return switch (mode) {
            case "on", "off" -> mode;
            default -> "auto";
        };
    }

    /**
     * Whether a Fusion Reactor meltdown may break blocks, resolved for this runtime: {@code "on"} /
     * {@code "off"} are explicit; {@code "auto"} (the default) is <b>off on a dedicated server</b> —
     * where one unattended reactor would otherwise crater a shared world — and on in singleplayer /
     * LAN, where the blast is the player's own consequence to keep.
     *
     * <p>The resolution itself lives in the Minecraft-free
     * {@link MeltdownMath#terrainDamage(String, boolean)} so it is
     * unit-tested without a loader.
     *
     * @param dedicatedServer {@code MinecraftServer.isDedicatedServer()} for the running server
     */
    public static boolean fusionMeltdownTerrainDamage(boolean dedicatedServer) {
        return MeltdownMath.terrainDamage(
                fusionMeltdownTerrainDamageMode(), dedicatedServer);
    }

    /** Hard cap on a Fusion Reactor meltdown's blast radius (blocks, 1..16). */
    public static int fusionMeltdownRadiusCap() {
        return FUSION_MELTDOWN_RADIUS_CAP.get();
    }

    public static String fusionSizeOutputPermille() {
        return FUSION_SIZE_OUTPUT.get();
    }

    /** Burn ticks for a fuel tier (1..4); tiers outside the range clamp to their nearest neighbour. */
    public static int fusionFuelBurnTicks(int tier) {
        return switch (Math.max(1, Math.min(4, tier))) {
            case 2 -> FUSION_T2_BURN.get();
            case 3 -> FUSION_T3_BURN.get();
            case 4 -> FUSION_T4_BURN.get();
            default -> FUSION_T1_BURN.get();
        };
    }

    public static int advancedOreProcessorYieldBonus() {
        return ADVANCED_YIELD_BONUS.get();
    }

    public static int acceleratorMaxGap() {
        return ACCEL_MAX_GAP.get();
    }

    public static int acceleratorMaxGuides() {
        return ACCEL_MAX_GUIDES.get();
    }

    public static double acceleratorTickScale() {
        return ACCEL_TICK_SCALE.get();
    }

    public static double acceleratorLaunchSpeed() {
        return ACCEL_LAUNCH_SPEED.get();
    }

    public static double acceleratorBoostPerGuide() {
        return ACCEL_BOOST_PER_GUIDE.get();
    }

    public static int acceleratorNePerGuide() {
        return ACCEL_NE_PER_GUIDE.get();
    }

    public static double acceleratorDragPerGuide() {
        return ACCEL_DRAG_PER_GUIDE.get();
    }

    public static double acceleratorMinGapAllowance() {
        return ACCEL_MIN_GAP_ALLOWANCE.get();
    }

    public static double acceleratorGapPerSpeed() {
        return ACCEL_GAP_PER_SPEED.get();
    }

    public static double acceleratorBendSpeedBase() {
        return ACCEL_BEND_SPEED_BASE.get();
    }

    public static int acceleratorEnergyScale() {
        return ACCEL_ENERGY_SCALE.get();
    }

    public static String solarDimensionMultipliers() {
        return SOLAR_DIM_MULTIPLIERS.get();
    }

    /** Permille scale on drilled deposit yield. */
    public static int depositYieldPermille() {
        return DEPOSIT_YIELD_PERMILLE.get();
    }

    /** Whether drilling depletes deposits. */
    public static boolean depositDepletionEnabled() {
        return DEPOSIT_DEPLETION_ENABLED.get();
    }

    /** Units a depleted deposit recovers per in-game day. */
    public static int depositRecoveryPerDay() {
        return DEPOSIT_RECOVERY_PER_DAY.get();
    }

    /** Comma-list of dimension ids with no deposits. */
    public static String depositDisabledDimensions() {
        return DEPOSIT_DISABLED_DIMENSIONS.get();
    }

    // --- 0.8.0 machine families (Stage 2) ------------------------------------------------

    public static int plasmaCrucibleFiringEnergy() {
        return PLASMA_CRUCIBLE_FIRING_ENERGY.get();
    }

    public static int plasmaCrucibleFiringHeat() {
        return PLASMA_CRUCIBLE_FIRING_HEAT.get();
    }

    public static int plasmaCrucibleHoldIntervalTicks() {
        return PLASMA_CRUCIBLE_HOLD_INTERVAL_TICKS.get();
    }

    public static int foundryBatchUnits() {
        return FOUNDRY_BATCH_UNITS.get();
    }

    public static int foundryLargeBatchMultiplier() {
        return FOUNDRY_LARGE_BATCH_MULTIPLIER.get();
    }

    public static int foundryHeatPortCapacity() {
        return FOUNDRY_HEAT_PORT_CAPACITY.get();
    }

    public static int foundryHeatLossPermille() {
        return FOUNDRY_HEAT_LOSS_PERMILLE.get();
    }

    public static int foundryHeatDrawPerTick() {
        return FOUNDRY_HEAT_DRAW_PER_TICK.get();
    }

    public static int foundryHeatIntakeIntervalTicks() {
        return FOUNDRY_HEAT_INTAKE_INTERVAL_TICKS.get();
    }

    public static int foundryLitHoldTicks() {
        return FOUNDRY_LIT_HOLD_TICKS.get();
    }

    public static String condenserAtmospheres() {
        return CONDENSER_ATMOSPHERES.get();
    }

    public static int cryoColdBonusPerHeatPermille() {
        return CRYO_COLD_BONUS_PER_HEAT_PERMILLE.get();
    }

    public static int cryoSpeedMinPermille() {
        return CRYO_SPEED_MIN_PERMILLE.get();
    }

    public static int cryoSpeedMaxPermille() {
        return CRYO_SPEED_MAX_PERMILLE.get();
    }

    public static int centrifugeCascadePushPerTick() {
        return CENTRIFUGE_CASCADE_PUSH_PER_TICK.get();
    }

    public static int crystalGrowerUnpoweredPermille() {
        return CRYSTAL_GROWER_UNPOWERED_PERMILLE.get();
    }

    public static int crystalGrowerColdThreshold() {
        return CRYSTAL_GROWER_COLD_THRESHOLD.get();
    }

    public static int crystalGrowerColdBonusPerHeatPermille() {
        return CRYSTAL_GROWER_COLD_BONUS_PER_HEAT_PERMILLE.get();
    }

    public static int crystalGrowerColdBonusCapPermille() {
        return CRYSTAL_GROWER_COLD_BONUS_CAP_PERMILLE.get();
    }

    public static int heatPipeMaxRun() {
        return HEAT_PIPE_MAX_RUN.get();
    }

    public static int heatPipeConductivityPermille() {
        return HEAT_PIPE_CONDUCTIVITY_PERMILLE.get();
    }

    public static int heatPipeLossPermille() {
        return HEAT_PIPE_LOSS_PERMILLE.get();
    }

    public static int heatPipeExchangeIntervalTicks() {
        return HEAT_PIPE_EXCHANGE_INTERVAL_TICKS.get();
    }

    public static boolean heatPipeContactDamage() {
        return HEAT_PIPE_CONTACT_DAMAGE.get();
    }

    public static double heatPipeContactDamageAmount() {
        return HEAT_PIPE_CONTACT_DAMAGE_AMOUNT.get();
    }

    public static int heatExchangerMaxPerOp() {
        return HEAT_EXCHANGER_MAX_PER_OP.get();
    }

    public static int heatExchangerConductivityPermille() {
        return HEAT_EXCHANGER_CONDUCTIVITY_PERMILLE.get();
    }

    public static String heatExchangerCoolants() {
        return HEAT_EXCHANGER_COOLANTS.get();
    }

    public static String heatExchangerHotMedia() {
        return HEAT_EXCHANGER_HOT_MEDIA.get();
    }

    public static int heatExchangerHotTemperature() {
        return HEAT_EXCHANGER_HOT_TEMPERATURE.get();
    }

    public static int drillOperationTicks() {
        return DRILL_OPERATION_TICKS.get();
    }

    public static int drillEnergyPerTick() {
        return DRILL_ENERGY_PER_TICK.get();
    }

    public static int drillDepletedRetryTicks() {
        return DRILL_DEPLETED_RETRY_TICKS.get();
    }

    public static int surveyScannerCapacity() {
        return SURVEY_SCANNER_CAPACITY.get();
    }

    public static int surveyScannerScanCost() {
        return SURVEY_SCANNER_SCAN_COST.get();
    }

    public static int surveyScannerCooldownTicks() {
        return SURVEY_SCANNER_COOLDOWN_TICKS.get();
    }

    public static int fusionGasMinShellSize() {
        return FUSION_GAS_MIN_SHELL_SIZE.get();
    }

    public static int fusionGasChargeTicks() {
        return FUSION_GAS_CHARGE_TICKS.get();
    }

    public static int fusionGasDeuteriumPerTick() {
        return FUSION_GAS_DEUTERIUM_PER_TICK.get();
    }

    public static int fusionGasHelium3PerTick() {
        return FUSION_GAS_HELIUM3_PER_TICK.get();
    }

    public static int fusionGasOutputPermille() {
        return FUSION_GAS_OUTPUT_PERMILLE.get();
    }

    public static long fuelInjectorGasCapacity() {
        return FUEL_INJECTOR_GAS_CAPACITY.get();
    }

    public static int fusionLatticeHeatReductionPermille() {
        return FUSION_LATTICE_HEAT_REDUCTION_PERMILLE.get();
    }

    public static int fusionLatticeMaxCount() {
        return FUSION_LATTICE_MAX_COUNT.get();
    }

    public static boolean inductionChargePlayers() {
        return INDUCTION_CHARGE_PLAYERS.get();
    }

    public static int inductionChargerRadius() {
        return INDUCTION_CHARGER_RADIUS.get();
    }

    public static int inductionChargerTransferPerTick() {
        return INDUCTION_CHARGER_TRANSFER_PER_TICK.get();
    }

    public static boolean annunciatorAlarmSound() {
        return ANNUNCIATOR_ALARM_SOUND.get();
    }

    public static int annunciatorRefreshTicks() {
        return ANNUNCIATOR_REFRESH_TICKS.get();
    }

    public static int heatSinkModuleHeadroomPermille() {
        return HEAT_SINK_MODULE_HEADROOM_PERMILLE.get();
    }

    public static int scrubberModulePollutionPermille() {
        return SCRUBBER_MODULE_POLLUTION_PERMILLE.get();
    }

    public static int scrubberModuleFloorPermille() {
        return SCRUBBER_MODULE_FLOOR_PERMILLE.get();
    }

    public static int powerDrillCapacity() {
        return POWER_DRILL_CAPACITY.get();
    }

    public static int powerDrillEnergyPerBlock() {
        return POWER_DRILL_ENERGY_PER_BLOCK.get();
    }

    public static int powerDrillVeinMaxBlocks() {
        return POWER_DRILL_VEIN_MAX.get();
    }

    public static int cuttingLanceCapacity() {
        return CUTTING_LANCE_CAPACITY.get();
    }

    public static int cuttingLanceEnergyPerShot() {
        return CUTTING_LANCE_ENERGY_PER_SHOT.get();
    }

    public static int cuttingLanceCooldownTicks() {
        return CUTTING_LANCE_COOLDOWN_TICKS.get();
    }

    public static int cuttingLanceRange() {
        return CUTTING_LANCE_RANGE.get();
    }

    public static int arcWrenchCapacity() {
        return ARC_WRENCH_CAPACITY.get();
    }

    public static int arcWrenchEnergyPerUse() {
        return ARC_WRENCH_ENERGY_PER_USE.get();
    }

    public static int arcWrenchCooldownTicks() {
        return ARC_WRENCH_COOLDOWN_TICKS.get();
    }

    public static int multimeterCapacity() {
        return MULTIMETER_CAPACITY.get();
    }

    public static int multimeterEnergyPerReading() {
        return MULTIMETER_ENERGY_PER_READING.get();
    }

    public static int multimeterCooldownTicks() {
        return MULTIMETER_COOLDOWN_TICKS.get();
    }

    public static int thermalGogglesCapacity() {
        return THERMAL_GOGGLES_CAPACITY.get();
    }

    public static int thermalGogglesDrainPerSecond() {
        return THERMAL_GOGGLES_DRAIN.get();
    }

    public static int filterMaskCapacity() {
        return FILTER_MASK_CAPACITY.get();
    }

    public static int filterMaskEnergyPerPulse() {
        return FILTER_MASK_ENERGY_PER_PULSE.get();
    }

    public static int filterMaskPulsesPerCartridge() {
        return FILTER_MASK_PULSES_PER_CARTRIDGE.get();
    }

    public static boolean pollutionExposureEffects() {
        return POLLUTION_EXPOSURE_EFFECTS.get();
    }

    public static int pollutionExposureIntervalTicks() {
        return POLLUTION_EXPOSURE_INTERVAL_TICKS.get();
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
