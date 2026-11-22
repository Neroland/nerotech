# Config

Every NeroTech setting lives in `config/nerotech.properties`, managed by Neroland Core. Gameplay values are
**server-authoritative**: a connected client uses the server's values, and Core syncs only keys and values,
never player data. Change a value and run `/neroland config reload` (Core's command) to apply it without a
restart. Ranges are enforced; an out-of-range value falls back to the default.

This page lists every key with its default, its allowed range and what it does. Keys marked **new in 0.8.0**
arrived with the Offworld Industry release; `turbineGasBurn` kept its key but its default now also burns
methane.

A few 0.8.0 defaults are worth knowing before you open a server:

- `pollutionExposureEffects=true`: a region at the **severe** pollution band gives brief Weakness and Hunger
  unless a charged [Filter Mask](Tools.md#filter-mask-and-pollution-exposure) blocks it. Severe means twice
  `pollutionEventThreshold`, so only heavy, unmitigated industry reaches it.
- `inductionChargePlayers=true`: [Induction Chargers](Machines.md#utility-blocks) top up energy items of
  players standing in range. Proximity only; nothing about the player is stored.
- `pollutionPerPlayerAttribution=false` and `machineOperationChargeCost=0`: the two options that make a
  machine record its placer stay **off**.

Datapack data that is not config: deposit tables (`data/<namespace>/nerotech_deposits/<dimension>.json`,
see [Deposits and Drilling](Deposits-and-Drilling.md)), recipes, and the tags listed on the
[Material Map](Material-Map.md) (for example `nerotech:power_drill/vein`, the blocks the Power Drill's vein
mode may take).

## Energy I/O sizing

| Key | Default | Range | Meaning |
| --- | --- | --- | --- |
| `machineEnergyCapacity` | `100000` | 1,000 to 100,000,000 | internal NE buffer of every Tier-1 machine |
| `machineMaxTransfer` | `2000` | 1 to 10,000,000 | max NE/tick a machine accepts or emits per side |

## Generators

| Key | Default | Range | Meaning |
| --- | --- | --- | --- |
| `neroGeneratorNePerTick` | `40` | 1 to 1,000,000 | NE/tick produced by the Nero Generator while burning fuel |
| `solarArrayNePerTick` | `10` | 1 to 1,000,000 | NE/tick produced by the Solar Array in full daylight with sky access |

## Power tier

| Key | Default | Range | Meaning |
| --- | --- | --- | --- |
| `windTurbineNePerTick` | `25` | 1 to 1,000,000 | base NE/tick produced by the Wind Turbine before the height curve (0.5x at y=80 rising linearly to 2x at y=200) and the dimension multiplier |
| `windDimensionMultipliers` | `minecraft:overworld=1.0` | - | per-dimension wind multiplier: comma-list of dimensionId=multiplier (any dimension not listed defaults to 1.0). When Nerospace is installed an AIRLESS planet always yields 0 — no atmosphere, no wind — ahead of this table |
| `geothermalNePerTickPerSource` | `8` | 1 to 1,000,000 | NE/tick the Geothermal Generator produces per lava / magma block in the 3x3 directly beneath it (0-9 sources) |
| `bioGeneratorNePerBurnTick` | `48` | 1 to 1,000,000 | NE/tick produced by the Bio Generator while burning a #nerotech:bio_fuels item — 20% above the Nero Generator, for half the pollution |
| `batteryBankCapacity` | `1000000` | 1,000 to 1,000,000,000 | internal NE buffer of one Battery Bank |
| `gridControllerRadius` | `16` | 4 to 64 | block radius the Grid Controller watches for NeroTech machines (loaded chunks only, batched every 100 ticks — large radii make each scan pass more expensive) |
| `gridShedThresholdPermille` | `200` | 0 to 1000 | aggregate grid fill (permille) below which the Grid Controller sheds load by switching non-generator machines to the Eco preset |
| `gridRestorePermille` | `500` | 0 to 1000 | aggregate grid fill (permille) above which the Grid Controller restores each shed machine's previous preset (keep it above gridShedThresholdPermille to avoid flapping) |
| `gridScaleMilestoneCapacity` | `5000000` | 0 to 2,000,000,000 | summed NE capacity of the machines one Grid Controller watches at which the Grid Controller's owner earns the Grid Scale progression record (a player-scope Core gate quest packs read; 0 disables the record) |
| `wirelessNodeRange` | `32` | 1 to 256 | maximum block distance between two paired Wireless Power Nodes (same dimension only; a node never force-loads its partner's chunk) |
| `wirelessNodeTransferPerTick` | `200` | 0 to 10,000,000 | NE a Wireless Power Node sends to its partner per transfer pass (the pass runs every 5 ticks); transfer is lossless — the node's value is convenience, not gain |

## Processing machines

| Key | Default | Range | Meaning |
| --- | --- | --- | --- |
| `machineBaseProcessTicks` | `120` | 1 to 72,000 | base ticks one Ore Processor / Fabricator operation takes (before Speed modules) |
| `machineNePerTick` | `30` | 0 to 1,000,000 | base NE/tick a processing machine consumes while working (before Efficiency) |

## Optional per-operation charges (needs NeroEconomy)

| Key | Default | Range | Meaning |
| --- | --- | --- | --- |
| `machineOperationChargeCost` | `0` | 0 to 1,000,000 | credits an owned processing machine (Ore Processor, Fabricator, their Advanced tiers, Chemical Processor) charges its owner, in advance, for each batch of machineOperationChargeBatch operations. 0 (default) = off. Needs NeroEconomy; without it machines always run free. Charges go to the economy's Reserve; a machine never pays out. When above 0, machines record the placing player's UUID as owner (erasable; see PRIVACY.md) |
| `machineOperationChargeBatch` | `64` | 1 to 4,096 | operations one machineOperationChargeCost payment covers (64 = one stack). Charging per batch, never per tick, keeps a machine well inside the economy's rate limit |

## Heat and the thermal model

| Key | Default | Range | Meaning |
| --- | --- | --- | --- |
| `heatCapacity` | `1000` | 1 to 1,000,000 | max heat units a machine can hold (gauge scale) |
| `heatPerOperation` | `4` | 0 to 100,000 | heat added each working tick (generators: per burn tick) |
| `heatDissipationPerTick` | `1` | 0 to 100,000 | extra heat shed per adjacent coolant block (water/ice/snow) each tick |
| `heatThrottleThreshold` | `800` | 1 to 1,000,000 | processing machines stall once heat reaches this (must cool to resume) |
| `thermalEnvLossPermille` | `20` | 0 to 1,000 | permille of the machine-vs-ambient heat difference exchanged with the environment each tick (machines relax toward ambient; 0 disables) |
| `thermalConductivityPermille` | `100` | 0 to 1,000 | permille of the heat difference conducted between ADJACENT machines per exchange (dense builds share heat; 0 disables machine-to-machine conduction) |
| `thermalExchangeIntervalTicks` | `10` | 1 to 72,000 | how often (ticks) a machine conducts heat with cached adjacent machines (interval + per-machine phase — never a per-tick neighbour scan) |
| `thermalAmbientDefault` | `0` | -100,000 to 100,000 | baseline ambient heat for dimensions not listed in thermalAmbientByDimension |
| `thermalAmbientByDimension` | `minecraft:the_nether=150` | - | per-dimension ambient heat: comma-list of dimensionId=heat, e.g. minecraft:the_nether=150,nerospace:cindara=200 (fallback: nerospace.api planet traits take precedence for known planets when Nerospace is installed; this covers everything else) |
| `thermalBiomeScale` | `50` | 0 to 100,000 | ambient heat added per full point of biome base temperature above vanilla plains (0.8): deserts run hot, snowy biomes run cold (0 disables biome flavour) |

## Fluid and gas machines (Electrolyzer, Gas Turbine, Chemical Processor)

| Key | Default | Range | Meaning |
| --- | --- | --- | --- |
| `machineGasCapacity` | `4000` | 100 to 10,000,000 | capacity (mB) of each internal machine gas tank; one balance "unit" of gas is 100 mB (capacity changes apply when the machine next loads) |
| `machineFluidCapacity` | `4000` | 1,000 to 10,000,000 | capacity (mB) of each internal machine fluid tank — the Electrolyzer's water tank (4 buckets by default) |
| `electrolyzerNePerTick` | `40` | 0 to 1,000,000 | NE/tick the Electrolyzer draws while splitting water (before Efficiency; Speed modules multiply the draw) |
| `electrolyzerOperationTicks` | `200` | 1 to 72,000 | base ticks one electrolysis operation takes (before Speed modules) |
| `electrolyzerWaterPerOp` | `500` | 1 to 1,000,000 | water (mB) consumed per electrolysis operation (a bucket is 1000 mB) |
| `electrolyzerHydrogenPerOp` | `200` | 0 to 1,000,000 | hydrogen (mB) produced per operation — 2 units at the default 100 mB/unit |
| `electrolyzerOxygenPerOp` | `100` | 0 to 1,000,000 | oxygen (mB) produced per operation — 1 unit; the 2:1 split is the point |
| `gasTurbineNePerUnit` | `60` | 0 to 10,000,000 | NE one unit (100 mB) of gas yields, before the fuel's turbineGasBurn multiplier. Clean power, not free power: electrolysis is a net energy SINK by design |
| `gasTurbineTicksPerUnit` | `20` | 1 to 72,000 | ticks one unit of gas burns for (the NE yield is spread evenly across them) |
| `turbineGasBurn` (default changed in 0.8.0) | `nerotech:hydrogen=2,nerotech:methane=4` | - | which gases the Gas Turbine burns and how well: comma-list of gasId=multiplier, e.g. nerotech:hydrogen=2,nerospace:oxygen=1. Yield per unit is gasTurbineNePerUnit x multiplier; a gas not listed here is not accepted at all |
| `chemicalProcessorGasPerOp` | `250` | 0 to 1,000,000 | oxygen (mB) one Chemical Processor operation consumes on top of its NE |

## Coolant loop (Radiator, Coolant Pump)

| Key | Default | Range | Meaning |
| --- | --- | --- | --- |
| `coolantPumpNePerTick` | `20` | 0 to 1,000,000 | NE/tick the Coolant Pump draws while pumping (billed in one batch per thermalExchangeIntervalTicks, never per tick) |
| `coolantPumpHeatPerOp` | `20` | 1 to 1,000,000 | heat the Coolant Pump pulls from EACH adjacent machine per exchange, before the radiator multiplier (each Radiator within 3 blocks in a straight line adds +1x) |

## Automation (Robotic Arm) and Singularity Vault

| Key | Default | Range | Meaning |
| --- | --- | --- | --- |
| `roboticArmNePerMove` | `4` | 0 to 1,000,000 | NE the Robotic Arm spends per ITEM moved (it runs one transfer pass per second; a pass moves at most roboticArmStackPerMove items) |
| `roboticArmStackPerMove` | `8` | 1 to 1,000 | how many items one Robotic Arm transfer pass moves (before Speed modules) |
| `singularityVaultCapacity` | `1000000` | 64 to 1,000,000,000 | how many of ONE item type a Singularity Vault holds (the two facade slots sit on top of this; a comparator reads fill against it) |

## Pollution (regional, periodic aggregate)

| Key | Default | Range | Meaning |
| --- | --- | --- | --- |
| `pollutionPerOperation` | `2` | 0 to 100,000 | pollution a machine emits per contribution (0 disables; solar emits none) |
| `pollutionContributionIntervalTicks` | `40` | 1 to 72,000 | how often (ticks) a running machine adds to its region (batched, not per-tick) |
| `annunciatorPollutionThreshold` | `8` | 0 to 100,000 | pollution a machine may emit in 5 s before its 'polluting' alarm lights (Annunciator Panel, link alarm event); a Balanced processor emits about 5, Overdrive about 10 |
| `pollutionDecayIntervalTicks` | `200` | 1 to 72,000 | how often (ticks) regional pollution decays server-wide |
| `pollutionDecayAmount` | `1` | 0 to 100,000 | pollution removed from each region per decay step |
| `pollutionPerPlayerAttribution` | `false` | - | OFF by default (privacy): when true, pollution is attributed to the placing player's UUID (POPIA/GDPR: UUIDs only, retention-pruned, erasable via the shared data-erasure hook) |
| `pollutionAttributionRetentionDays` | `30` | 0 to 3,650 | days to keep per-player pollution attribution before pruning (0 = keep until erased) |
| `pollutionEventThreshold` | `1000` | 0 to 1,000,000 | regional pollution level that publishes a Core threshold event when crossed (rising and recovering; dormant until a listener like NeroEvents exists; 0 disables) |

## Pollution mitigation (Scrubber, Remediator)

| Key | Default | Range | Meaning |
| --- | --- | --- | --- |
| `scrubberNePerOp` | `120` | 0 to 1,000,000 | NE one Scrubber operation costs (batched on the contribution interval) |
| `scrubberPollutionPerOp` | `6` | 1 to 100,000 | pollution removed from the Scrubber's own region per operation |
| `scrubberAdjacentPermille` | `250` | 0 to 1,000 | permille of the scrub rate also removed from each of the 8 adjacent regions (default 25%; 0 = own region only) |
| `scrubberFilterCapacity` | `400` | 1 to 1,000,000 | pollution one Filter Cartridge absorbs before it fouls into a Dirty Filter |
| `remediatorNePerOp` | `600` | 0 to 10,000,000 | NE one Remediator operation costs — deliberately heavy; cleanup is a sink |
| `remediatorPollutionPerOp` | `20` | 1 to 100,000 | pollution removed from the Remediator's own region per operation |

## Production analytics (Analytics Terminal)

| Key | Default | Range | Meaning |
| --- | --- | --- | --- |
| `analyticsTerminalRadius` | `16` | 4 to 64 | block radius an Analytics Terminal scans for NeroTech machines (loaded chunks only, batched every 100 ticks — large radii make each scan pass more expensive) |

## Tier 2/3 (no gate: paced by Starsteel-tier recipes alone)

| Key | Default | Range | Meaning |
| --- | --- | --- | --- |
| `fusionReactorNePerTick` | `400` | 1 to 10,000,000 | NE/tick the Fusion Reactor produces while running (high-output late-game) |
| `fusionReactorMeltdownEnabled` | `true` | - | true: an unmanaged Fusion Reactor melts down destructively at max heat (telegraphed by the red gauge); false (survival-friendly): it just stalls until it cools |
| `fusionMeltdownTerrainDamage` | `auto` | - | whether a Fusion Reactor meltdown destroys terrain: on / off / auto (auto = off on a dedicated server, on in singleplayer and LAN); off keeps the blast (damage, knockback, the reactor itself is still lost) but breaks no blocks |
| `fusionMeltdownRadiusCap` | `8` | 1 to 16 | hard cap on a Fusion Reactor meltdown's blast radius (blocks); the radius is shell size + 1 (4/6/8 for the 3/5/7 shells), never more than this |
| `fusionSizeOutputPermille` | `3=1000,5=4000,7=12000` | - | multiblock output multiplier (permille of fusionReactorNePerTick) per shell size: comma-list of size=permille for the 3/5/7 shells |
| `fusionFuelTier1BurnTicks` | `1600` | 1 to 720,000 | burn ticks of one tier-1 fuel (Fusion Cell) charge |
| `fusionFuelTier2BurnTicks` | `4800` | 1 to 720,000 | burn ticks of one tier-2 fuel (Plasma Cell) charge — needs a 5x5x5+ shell |
| `fusionFuelTier3BurnTicks` | `14400` | 1 to 720,000 | burn ticks of one tier-3 fuel (Stellar Cell) charge — needs the 7x7x7 shell |
| `fusionFuelTier4BurnTicks` | `28800` | 1 to 720,000 | burn ticks of one tier-4 fuel (Antimatter Cell) charge — twice a Stellar Cell, ONLY the 7x7x7 shell will contain it, and burning it adds +2 to the reactor's heat rate (the meltdown risk is the price) |
| `advancedOreProcessorYieldBonus` | `1` | 0 to 64 | extra dust the Advanced Ore Processor yields over the Tier-1 processor |

## Particle Accelerator (free-form ring; the standalone route to space-grade dusts)

| Key | Default | Range | Meaning |
| --- | --- | --- | --- |
| `acceleratorMaxGap` | `16` | 1 to 64 | blocks the Accelerator Controller marches along the current heading looking for the next Accelerator Guide Coil; nothing found within this and the beam line is OPEN |
| `acceleratorMaxGuides` | `256` | 4 to 4,096 | safety cap on guides in one traced beam line (a pathological build stops here and reads as an open line); the trace costs O(guides x acceleratorMaxGap) block reads |
| `acceleratorTickScale` | `0.05` | 0.001 to 1.0 | blocks a particle travels per tick per unit of speed (0.05 = speed 100 covers 5 blocks/tick) |
| `acceleratorLaunchSpeed` | `10.0` | 0.1 to 10,000.0 | floor on the injection speed; the controller actually injects at the slowest speed the loop's LONGEST stretch tolerates (see acceleratorMinGapAllowance), so a wide ring starts fast and a tight one crawls |
| `acceleratorBoostPerGuide` | `2.0` | 0.0 to 10,000.0 | speed a powered Accelerator Guide Coil adds as the particle passes |
| `acceleratorNePerGuide` | `50` | 0 to 10,000,000 | NE the controller spends per guide passed (and once to inject); with no NE the particle coasts instead of accelerating |
| `acceleratorDragPerGuide` | `0.5` | 0.0 to 10,000.0 | speed an UNPOWERED guide bleeds off a coasting particle — coast long enough and it drops below the gap rule and is lost |
| `acceleratorMinGapAllowance` | `4.0` | 0.0 to 256.0 | gap rule: the longest stretch a particle at speed 0 could cross. The real allowance is this + speed x acceleratorGapPerSpeed — slow particles need close guides |
| `acceleratorGapPerSpeed` | `0.12` | 0.0 to 100.0 | gap rule: extra blocks of allowed stretch per unit of speed |
| `acceleratorBendSpeedBase` | `20.0` | 0.1 to 100,000.0 | bend rule: a 45 degree turn survives a speed of at most this x the length of the stretch before it — the shortest bend stretch in a loop caps the whole loop's top speed, so big rings are what buy big speeds |
| `acceleratorEnergyScale` | `500` | 1 to 1,000,000 | permille scale in the collision energy formula E = 0.5 x speed^2 x scale; collider recipes gate on the resulting joules |
| `solarDimensionMultipliers` | (empty) | - | per-dimension solar multiplier fallback (nerospace.api planet traits take precedence for known planets when Nerospace is installed): comma-list of dimensionId=multiplier, e.g. nerospace:greenxertz=1.5,nerospace:glacira=0.6 (Earth defaults to 1.0) |

## Virtual deposits (Deep Core Drill / Survey Scanner)

| Key | Default | Range | Meaning |
| --- | --- | --- | --- |
| `depositYieldPermille` (new in 0.8.0) | `1000` | 0 to 100,000 | permille scale on what a drilled deposit unit yields (1000 = the table's richness as written; 0 = drills extract nothing) |
| `depositDepletionEnabled` (new in 0.8.0) | `true` | - | whether drilling depletes a chunk's deposit (false = deposits never run out and no depletion record is ever stored) |
| `depositRecoveryPerDay` (new in 0.8.0) | `200` | 0 to 1,000,000 | units a depleted deposit recovers per in-game day (24000 ticks), up to its capacity; 0 = deposits never recover |
| `depositDisabledDimensions` (new in 0.8.0) | (empty) | - | comma-list of dimension ids with no deposits at all (surveys read empty, drills idle), e.g. minecraft:the_end |

## 0.8.0 machine families, modules, tools and pollution exposure

### Metallurgy

| Key | Default | Range | Meaning |
| --- | --- | --- | --- |
| `plasmaCrucibleFiringEnergy` (new in 0.8.0) | `60` | 1 to 100,000 | NE per tick the Plasma Crucible spends firing its arc while it is below a recipe's min_heat (scaled by Efficiency modules and the overclock preset) |
| `plasmaCrucibleFiringHeat` (new in 0.8.0) | `12` | 1 to 100,000 | heat one firing tick adds (firing never reaches heatThrottleThreshold) |
| `plasmaCrucibleHoldIntervalTicks` (new in 0.8.0) | `20` | 1 to 72,000 | an idle Plasma Crucible exchanges heat with its surroundings only once every this many ticks, so it holds its temperature between jobs (1 = no insulation) |

### Foundry

| Key | Default | Range | Meaning |
| --- | --- | --- | --- |
| `foundryBatchUnits` (new in 0.8.0) | `4` | 1 to 64 | recipe units a 3x3x3 Blast Foundry runs per cycle (energy per tick scales with the units) |
| `foundryLargeBatchMultiplier` (new in 0.8.0) | `4` | 1 to 16 | how many times the 3x3x3 batch a 5x5x5 Blast Foundry runs per cycle |
| `foundryHeatPortCapacity` (new in 0.8.0) | `500` | 1 to 100,000 | heat a Foundry Heat Port buffers for its controller to drain |
| `foundryHeatLossPermille` (new in 0.8.0) | `5` | 0 to 1,000 | permille of the furnace's heat above ambient lost per tick (hot dimensions lose less; the furnace never warms toward ambient) |
| `foundryHeatDrawPerTick` (new in 0.8.0) | `2` | 0 to 100,000 | furnace heat a running batch uses each tick |
| `foundryHeatIntakeIntervalTicks` (new in 0.8.0) | `5` | 1 to 200 | how often (ticks) the controller drains its heat ports |
| `foundryLitHoldTicks` (new in 0.8.0) | `40` | 0 to 1,200 | ticks the shell stays lit after the foundry stops working (avoids flicker on short stalls) |

### Chemistry

| Key | Default | Range | Meaning |
| --- | --- | --- | --- |
| `condenserAtmospheres` (new in 0.8.0) | `minecraft:the_nether=SULFUROUS,minecraft:the_end=VACUUM` | - | Atmospheric Condenser air per dimension when Nerospace has no say (absent, api drift, or not a Nerospace planet): comma-list of dimensionId=ATMOSPHERE with ATMOSPHERE one of VACUUM, THIN_CO2, SULFUROUS, NITROGEN_METHANE, NITROGEN_OXYGEN, EARTHLIKE. Unlisted dimensions are EARTHLIKE |
| `cryoColdBonusPerHeatPermille` (new in 0.8.0) | `5` | 0 to 1,000 | Cryo Distiller speed change per point of ambient heat, permille: factor = 1 - ambient x this / 1000 (Glacira -80 gives 1.4) |
| `cryoSpeedMinPermille` (new in 0.8.0) | `500` | 1 to 1,000 | Cryo Distiller speed floor in hot places, permille of the recipe rate |
| `cryoSpeedMaxPermille` (new in 0.8.0) | `2000` | 1,000 to 10,000 | Cryo Distiller speed cap in cold places, permille of the recipe rate |
| `centrifugeCascadePushPerTick` (new in 0.8.0) | `200` | 1 to 100,000 | gas (mB) a Centrifuge passes to the next one in its cascade each tick |

### Components

| Key | Default | Range | Meaning |
| --- | --- | --- | --- |
| `crystalGrowerUnpoweredPermille` (new in 0.8.0) | `250` | 0 to 1,000 | growth speed of the Crystal Grower with no power, as permille of its powered speed (0 = it stalls unpowered like any other machine) |
| `crystalGrowerColdThreshold` (new in 0.8.0) | `0` | -100,000 to 100,000 | ambient heat at or above which the Crystal Grower gets no cold bonus |
| `crystalGrowerColdBonusPerHeatPermille` (new in 0.8.0) | `10` | 0 to 1,000 | extra Crystal Grower speed, in permille, per point of ambient heat below the threshold |
| `crystalGrowerColdBonusCapPermille` (new in 0.8.0) | `1000` | 0 to 4,000 | the most the cold bonus can add, in permille (1000 = twice as fast) |

### Heat

| Key | Default | Range | Meaning |
| --- | --- | --- | --- |
| `heatPipeMaxRun` (new in 0.8.0) | `64` | 1 to 256 | most Heat Pipes one run resolves (flood-fill cap; pipes beyond it form their own run) |
| `heatPipeConductivityPermille` (new in 0.8.0) | `250` | 0 to 1,000 | permille of each endpoint-vs-run temperature difference a Heat Pipe run moves per exchange (0 disables pipes) |
| `heatPipeLossPermille` (new in 0.8.0) | `1` | 0 to 1,000 | permille of each pipe's heat above ambient lost per exchange, so long runs carry less |
| `heatPipeExchangeIntervalTicks` (new in 0.8.0) | `10` | 1 to 72,000 | ticks between a run's exchanges (only the run's anchor pipe ticks) |
| `heatPipeContactDamage` (new in 0.8.0) | `true` | - | whether a Heat Pipe at heat level 2 or more hurts entities standing on it (sneaking avoids it) |
| `heatPipeContactDamageAmount` (new in 0.8.0) | `1.0` | 0.0 to 20.0 | damage per contact hit at level 2 (doubled at level 3) |
| `heatExchangerMaxPerOp` (new in 0.8.0) | `60` | 1 to 100,000 | most heat one Heat Exchanger exchange moves, before Speed modules and the overclock preset |
| `heatExchangerConductivityPermille` (new in 0.8.0) | `300` | 0 to 1,000 | permille of the machine-vs-tank difference moved per exchange for a storing medium (water and anything not listed as a coolant or hot medium) |
| `heatExchangerCoolants` (new in 0.8.0) | `nerotech:cryo_coolant=4,nerotech:nitrogen=2` | - | coolant media as id=heatPerMb: absorb machine heat above ambient and are used up |
| `heatExchangerHotMedia` (new in 0.8.0) | `minecraft:lava=2` | - | hot media as id=heatPerMb: heat the machine and are used up |
| `heatExchangerHotTemperature` (new in 0.8.0) | `700` | 0 to 1,000,000 | heat a hot medium raises a machine to, at most |

### Extraction

| Key | Default | Range | Meaning |
| --- | --- | --- | --- |
| `drillOperationTicks` (new in 0.8.0) | `200` | 20 to 12,000 | ticks one Deep Core Drill operation takes at 1x speed; the deposit is read once per operation |
| `drillEnergyPerTick` (new in 0.8.0) | `64` | 0 to 100,000 | NE per tick the drill draws while it works, before modules and preset |
| `drillDepletedRetryTicks` (new in 0.8.0) | `1200` | 20 to 72,000 | ticks a drill waits before it retries a depleted or missing deposit |
| `surveyScannerCapacity` (new in 0.8.0) | `10000` | 100 to 1,000,000 | NE the Survey Scanner holds |
| `surveyScannerScanCost` (new in 0.8.0) | `500` | 0 to 1,000,000 | NE one survey costs |
| `surveyScannerCooldownTicks` (new in 0.8.0) | `40` | 0 to 1,200 | ticks between two surveys with the same scanner |

### Fusion

| Key | Default | Range | Meaning |
| --- | --- | --- | --- |
| `fusionGasMinShellSize` (new in 0.8.0) | `7` | 3 to 7 | smallest fusion shell edge that can contain the gas-fed burn (gas is the top fuel tier, so by default only the 7x7x7 shell takes it) |
| `fusionGasChargeTicks` (new in 0.8.0) | `20` | 1 to 1,200 | ticks of burn bought by one gas draw; gas is drawn per charge across the shell's Fuel Injectors, never per tick |
| `fusionGasDeuteriumPerTick` (new in 0.8.0) | `2` | 1 to 1,000 | mB of deuterium burned per tick of gas-fed burn |
| `fusionGasHelium3PerTick` (new in 0.8.0) | `1` | 1 to 1,000 | mB of helium-3 burned per tick of gas-fed burn; a charge needs both gases and a surplus of one never covers the other |
| `fusionGasOutputPermille` (new in 0.8.0) | `1500` | 0 to 10,000 | output of a gas-fed burn against a fuel-cell burn of the same shell (1500 = x1.5), on top of fusionSizeOutputPermille |
| `fuelInjectorGasCapacity` (new in 0.8.0) | `8000` | 100 to 1,000,000 | mB of each gas one Fuel Injector holds (two tanks: deuterium, helium-3) |
| `fusionLatticeHeatReductionPermille` (new in 0.8.0) | `60` | 0 to 1,000 | share of the reactor's heat per burning tick that each counted Europium Flux Lattice removes |
| `fusionLatticeMaxCount` (new in 0.8.0) | `8` | 0 to 98 | lattices beyond this many in one shell add nothing (default cap: 8 x 60 = 48% less heat) |

### Utility

| Key | Default | Range | Meaning |
| --- | --- | --- | --- |
| `inductionChargePlayers` (new in 0.8.0) | `true` | - | Induction Chargers charge energy items held or worn by players standing in range. Proximity only; nothing about the player is stored. Off leaves the entity charging API working |
| `inductionChargerRadius` (new in 0.8.0) | `4` | 0 to 16 | reach of an Induction Charger in blocks on every axis (a bounded box) |
| `inductionChargerTransferPerTick` (new in 0.8.0) | `256` | 0 to 100,000 | NE one charger may hand out per tick (scaled by Speed modules and the overclock preset); player charging and the entity API share it |
| `annunciatorAlarmSound` (new in 0.8.0) | `true` | - | an Annunciator Panel beeps once when a new alarm lights |
| `annunciatorRefreshTicks` (new in 0.8.0) | `10` | 1 to 200 | how often an Annunciator Panel re-reads the machines touching it |

### Upgrade modules

| Key | Default | Range | Meaning |
| --- | --- | --- | --- |
| `heatSinkModuleHeadroomPermille` (new in 0.8.0) | `125` | 0 to 250 | share of the headroom between heatThrottleThreshold and heatCapacity each Heat Sink Module opens (at most 4 count, and at most half the headroom opens in total) |
| `scrubberModulePollutionPermille` (new in 0.8.0) | `200` | 0 to 1,000 | pollution per operation each Scrubber Module removes, permille of normal (at most 4 count) |
| `scrubberModuleFloorPermille` (new in 0.8.0) | `250` | 0 to 1,000 | Scrubber Modules never cut pollution below this permille of normal |

### Tools

| Key | Default | Range | Meaning |
| --- | --- | --- | --- |
| `powerDrillCapacity` (new in 0.8.0) | `40000` | 100 to 10,000,000 | NE the Power Drill holds |
| `powerDrillEnergyPerBlock` (new in 0.8.0) | `80` | 0 to 100,000 | NE the Power Drill spends per block it breaks (each extra block of an area or vein too) |
| `powerDrillVeinMaxBlocks` (new in 0.8.0) | `32` | 1 to 64 | most blocks one vein-mode break may take, the first included (hard cap 64) |
| `cuttingLanceCapacity` (new in 0.8.0) | `40000` | 100 to 10,000,000 | NE the Cutting Lance holds |
| `cuttingLanceEnergyPerShot` (new in 0.8.0) | `300` | 0 to 100,000 | NE one Cutting Lance cut costs before the lens factor |
| `cuttingLanceCooldownTicks` (new in 0.8.0) | `12` | 0 to 200 | ticks between two cuts before the lens factor |
| `cuttingLanceRange` (new in 0.8.0) | `16` | 1 to 32 | reach of the Cutting Lance in blocks (the Void lens doubles it; never past 32) |
| `arcWrenchCapacity` (new in 0.8.0) | `10000` | 100 to 10,000,000 | NE the Arc Wrench holds |
| `arcWrenchEnergyPerUse` (new in 0.8.0) | `200` | 0 to 100,000 | NE one Arc Wrench rotation, pick-up or placement costs |
| `arcWrenchCooldownTicks` (new in 0.8.0) | `10` | 0 to 200 | ticks between two Arc Wrench actions |
| `multimeterCapacity` (new in 0.8.0) | `5000` | 100 to 10,000,000 | NE the Multimeter holds |
| `multimeterEnergyPerReading` (new in 0.8.0) | `25` | 0 to 100,000 | NE one Multimeter reading costs |
| `multimeterCooldownTicks` (new in 0.8.0) | `10` | 0 to 200 | ticks between two Multimeter readings |
| `thermalGogglesCapacity` (new in 0.8.0) | `20000` | 100 to 10,000,000 | NE the Thermal Goggles hold |
| `thermalGogglesDrainPerSecond` (new in 0.8.0) | `5` | 0 to 100,000 | NE worn Thermal Goggles drain per second; the overlay shows only while charged |
| `filterMaskCapacity` (new in 0.8.0) | `10000` | 100 to 10,000,000 | NE the Filter Mask holds |
| `filterMaskEnergyPerPulse` (new in 0.8.0) | `20` | 0 to 100,000 | NE a worn Filter Mask spends each time it blocks a pollution exposure pulse |
| `filterMaskPulsesPerCartridge` (new in 0.8.0) | `36` | 1 to 10,000 | exposure pulses one fitted Filter Cartridge blocks before it fouls into a Dirty Filter |

### Pollution exposure (what the Filter Mask blocks)

| Key | Default | Range | Meaning |
| --- | --- | --- | --- |
| `pollutionExposureEffects` (new in 0.8.0) | `true` | - | players standing in a region at the severe pollution band (twice pollutionEventThreshold) get brief Weakness and Hunger each pulse unless a charged Filter Mask with a cartridge blocks it; nothing is stored |
| `pollutionExposureIntervalTicks` (new in 0.8.0) | `100` | 20 to 6,000 | ticks between two pollution exposure pulses (also the worn-tool drain tick) |

## Telemetry (anonymous crash reporting; client-local opt-out, not synced)

| Key | Default | Range | Meaning |
| --- | --- | --- | --- |
| `telemetryEnabled` | `true` | - | anonymous error reporting to the developers (stack trace + mod/MC/loader/OS/Java versions only — never names, UUIDs, IPs, or world data; POPIA/GDPR-compliant). On by default; set false to opt out (client-local, takes effect on restart). See PRIVACY.md |

## Client rendering (client-local, not synced)

| Key | Default | Range | Meaning |
| --- | --- | --- | --- |
| `renderAnimationsEnabled` | `true` | - | animated machine visuals (spinning turbines/drums, sun-tracking solar deck, fabricator arms, plasma torus, holograms). Set false on low-end clients: block-entity renderers then draw a single static parked frame instead |

## See also

- [Commands](Commands.md)
- [Privacy](Privacy.md)
- [Home](Home.md)
