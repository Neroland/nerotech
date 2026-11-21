# Tools

NeroTech's hand tools all run on **Core energy held on the item** (the bar under the icon). Every one
of them is filled by an **Induction Charger** while you stand in its reach, holding the tool or
wearing it. Most can also be charged by sneak-using them on any block that exposes Core energy (a
Battery Bank, a generator, any Nero machine); the Arc Wrench charges only at an Induction Charger,
because sneak-use on a machine picks it up. Every action costs charge, and most have a short cooldown;
all costs, cooldowns and bounds are server config keys (below). With no charge a tool does nothing
special. Nothing about you is stored on a tool, and readings are shown only to you.

| Tool | What it does |
| --- | --- |
| **Power Drill** | Mines stone and earth fast. Sneak-use in the air cycles **single**, **area** (the 3×3 plane facing you) and **vein** (connected blocks of the same kind, up to 32 by default, never more than 64; ores, gravel, clay and glowstone only). |
| **Cutting Lance** | Use it to cut the block you look at, up to 16 blocks away. The fitted focus lens sets what a cut does. |
| **Arc Wrench** | Use on a machine to rotate it; sneak-use to pick it up with its contents and settings, then use on a block face to put it down. |
| **Multimeter** | Right-click any block: energy, heat, tank levels and the pollution band of the region. |
| **Survey Scanner** | Reads the deposit under the chunk you stand in (see [Material Map](Material-Map.md)). |
| **Thermal Goggles** | Worn on your head: machines and Heat Pipes in view show their heat as a coloured mote, blue (cold) to red (at the ceiling). |
| **Filter Mask** | Worn on your head with a Filter Cartridge fitted: blocks the effects of severe pollution. |

## Power Drill

The drill mines anything a pickaxe or shovel mines, at up to diamond tier, and every block costs
charge (each extra block of an area or vein too). It stops when it runs out. **Protected ground stays
protected:** every extra block goes through the same break path as a block you mine by hand, so a
claim or protection mod can refuse any one of them, and the drill never takes a block you could not
break yourself, a block with contents (chests, machines), or a block in an unloaded chunk.

Vein mode only follows blocks in the `nerotech:power_drill/vein` block tag (by default `#c:ores`, gravel,
clay and glowstone), so it cannot strip a hillside; area mode still mines stone. Packs can change the tag.

## Cutting Lance and focus lenses

Hold a focus lens in your other hand and sneak-use the lance in the air to fit it; the lens you had
fitted comes back to you. A cut goes through the same break path as the drill, and costs nothing if
it is refused.

| Lens | Effect |
| --- | --- |
| none | 16 blocks of reach. |
| Quartz | Precise: half the cooldown between cuts. |
| Glacite | Cold: ice, packed ice, blue ice, glass and glass panes come away whole, and cut ice leaves no water. |
| Ember | Hot: the drops come out smelted (1.5× the charge). |
| Void | Long: twice the reach, up to 32 blocks (2× the charge). |

## Arc Wrench

Rotating turns a machine a quarter turn; its side configuration follows it, because sides are set
relative to the machine's front. Picking up keeps everything: items, upgrade modules, energy, heat,
tank contents, side configuration and preset. The wrench carries one machine at a time; put it down
before picking up another.

The wrench refuses:

- anything that is not a NeroTech machine;
- the **Process Tank** and the **Singularity Vault** — they already keep their contents on their own
  item when broken with a pickaxe;
- a formed multiblock controller (Fusion Reactor, Blast Foundry, Deep Core Drill), and any block
  inside a multiblock's footprint — take the structure apart first.

Drill head wear lives on the head item, so a Deep Core Drill moved with the wrench, or broken with a
pickaxe, gives its head back with the wear it had.

Lifting a machine counts as breaking it: a claim or protection mod that stops you breaking blocks there also
stops the wrench lifting machines. Rotating is not affected.

## Multimeter

The reading goes to your chat: the block's energy (any mod's block that exposes Core energy), a
machine's heat with the point it throttles at, a Heat Pipe's heat level, fluid and gas tanks or a
Process Tank column, and the region's pollution band (none, low, high, severe).

## Thermal Goggles

While charged and worn, the goggles show the heat of every NeroTech machine in view within 24 blocks,
and of Heat Pipes within 8 blocks, as coloured motes. They drain a little charge every second while
worn. The view is drawn on your client from what it already knows; nothing is sent for it.

## Filter Mask and pollution exposure

A region at the **severe** pollution band (twice `pollutionEventThreshold`) gives anyone standing in it
brief Weakness and Hunger every few seconds (`pollutionExposureEffects`, on by default; creative and
spectator players are not affected). A worn, charged Filter Mask with a cartridge blocks it. Use the
mask with a Filter Cartridge in your other hand to fit one; it blocks 36 exposures and then fouls into
a **Dirty Filter**, which goes into your inventory — the same cycle as the
[Scrubber](Pollution-and-Mitigation.md), and the Ore Processor still refunds it.

## Charging

The Induction Charger is a tier-1 block, so you can charge tools early, before you have an Arc Wrench to
move machines with.

## Config keys

`powerDrillCapacity`, `powerDrillEnergyPerBlock`, `powerDrillVeinMaxBlocks`, `cuttingLanceCapacity`,
`cuttingLanceEnergyPerShot`, `cuttingLanceCooldownTicks`, `cuttingLanceRange`, `arcWrenchCapacity`,
`arcWrenchEnergyPerUse`, `arcWrenchCooldownTicks`, `multimeterCapacity`, `multimeterEnergyPerReading`,
`multimeterCooldownTicks`, `thermalGogglesCapacity`, `thermalGogglesDrainPerSecond`,
`filterMaskCapacity`, `filterMaskEnergyPerPulse`, `filterMaskPulsesPerCartridge`,
`pollutionExposureEffects`, `pollutionExposureIntervalTicks`, and the Survey Scanner's
`surveyScannerCapacity`, `surveyScannerScanCost`, `surveyScannerCooldownTicks`.

## See also

- [Machines](Machines.md#upgrade-modules)
- [Pollution & Mitigation](Pollution-and-Mitigation.md)
- [Thermal System](Thermal-System.md)
- [Home](Home.md)
