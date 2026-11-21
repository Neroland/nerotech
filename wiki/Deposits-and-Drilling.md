# Deposits and Drilling

Every chunk has a **virtual deposit**: an ore body deep under it that a Deep Core Drill can mine without
breaking a single block of ground. Deposits are worked out from the world seed, the dimension and the chunk,
so every server sees the same deposit in the same chunk, and nothing is stored for a chunk nobody has drilled.

## Deep Core Drill

A small multiblock: the **Deep Core Drill** controller on top of a column of three **Drill Derricks**. Fit a
drill head, give it power, and it pulls ore from the deposit under its chunk into its output slots.

- One operation takes `drillOperationTicks` (200) ticks at 1x speed and draws `drillEnergyPerTick` NE per
  tick. The deposit is read once per operation, never every tick.
- **Drill heads wear out.** Each tier lasts a number of operations and takes more from the deposit per
  operation:

  | Head | Operations | Deposit units per operation | Yield |
  | --- | --- | --- | --- |
  | Iron Drill Head | 64 | 4 | 1x |
  | Cindersteel Drill Head | 192 | 6 | 1.15x |
  | Glacite Drill Head | 512 | 8 | 1.3x |

  Each head keeps its own wear, shown as a bar on the item, so a part-worn head stays part-worn when you take
  it out, break the drill or move it with the [Arc Wrench](Tools.md#arc-wrench). Drill heads stack to 1.
- The drill makes pollution and heat like any machine.
- A depleted or empty deposit makes the drill wait `drillDepletedRetryTicks` before it tries again. Its status
  line says why it is idle.

The first ore a drill pulls records the **first deposit tapped** milestone for the player operating it.

## Depletion and recovery

Drilling a chunk's deposit uses it up (`depositDepletionEnabled`). A drilled deposit recovers
`depositRecoveryPerDay` units per in-game day, up to its full size. Once it is full again, its record is
removed. `depositYieldPermille` scales what every unit yields, and `depositDisabledDimensions` switches
deposits off for whole dimensions.

## What is down there

Deposit tables are datapack data, one file per dimension under `data/<namespace>/nerotech_deposits/`. Each
entry names an ore **by tag**, the item it yields, a weight, a size range and a richness range. An entry whose
tag is empty on your server is skipped, and a table only lists ores the dimension really generates.

| Dimension | Possible deposits |
| --- | --- |
| Overworld | coal, iron, copper, gold, redstone, lapis, diamond, emerald; nerosium and nerosteel with Nerospace |
| The Nether | quartz, gold (as nuggets) |
| Luna | ilmenite, nerosium, nerosteel |
| Mars | hematite, nerosium, nerosteel |
| Cindara | cindrite |
| Glacira | glacite, methane crystal |
| Greenxertz | xertz quartz, amber sap, nerosteel |
| Nyxara | voidglass, nerosium |
| Europa | deuterium shards, oxidant crystal, brine salt, vent sulfide, raw europium |

The Nerospace worlds exist only with Nerospace installed. Diamond and emerald deposits are rare and small.

## Survey Scanner

The [Survey Scanner](Tools.md) reads the deposit under the chunk you stand in: what it holds and how much is
left, in 20% bands, and whether it is recovering. It reports bands, never exact counts, and never a position.
Each survey costs `surveyScannerScanCost` NE with a `surveyScannerCooldownTicks` cooldown.

## Privacy and griefing

Deposits are world data keyed by dimension and chunk: how much is left and when it was last drilled. Nothing
about who drilled it is stored. Because no terrain is changed, a drill cannot dig into anyone's claim or base.

## See also

- [Material Map](Material-Map.md)
- [Config](Config.md#virtual-deposits-deep-core-drill--survey-scanner)
- [Privacy](Privacy.md)
- [Home](Home.md)
