# Components

The component ladder sits above the tier-1 parts (Circuit Board, Machine Frame, Nero Coil), which stay as they
are. Three blocks make it:

| Block | What it does |
| --- | --- |
| **Component Forge** | Builds servos, capacitors, heat sinks, the Superconductor Coil and Europium Flux Lattices from up to six parts and one fluid. It takes no patterns and works from its own slots only. |
| **Laser Etcher** | Etches wafers into processors. The focus lens in its lens slot decides what it can etch and is never used up. |
| **Crystal Grower** | Grows crystal boules, focus lenses and Synthetic Glacite from a seed or feedstock and a fluid. Keeps growing slowly without power; cold places grow faster. |

## The ladder

| Tier | Processors | Wire and coil | Parts |
| --- | --- | --- | --- |
| 1 | Circuit Board | Copper Wire, Copper Coil, Nero Coil | Machine Frame |
| 2 | **Logic Chip** | Cindersteel Wire, Cindersteel Coil | Servo, Capacitor, Heat Sink, Polymer Sheet |
| 3 | **Quantum Chip** | Superconductor Wire, Superconductor Coil | Europium Flux Lattice |

Supporting items: Crystal Seed, Crystal Boule, Wafer, four focus lenses (Quartz, Ember, Glacite, Void), three
drill heads (Iron, Cindersteel, Glacite) and two upgrade modules (Heat Sink Module, Scrubber Module). Tags for
other mods are on the [Material Map](Material-Map.md#components).

## Laser Etcher and lenses

| Lens | Etches |
| --- | --- |
| Quartz | Logic Chip from a Wafer and copper wire |
| Ember | Logic Chip from a Wafer and cindersteel wire, in half the time |
| Void | Quantum Chip from a Wafer, a Logic Chip and superconductor wire |
| Glacite | Quantum Chip from two Wafers and superconductor wire (no Logic Chip, slower) |

The lens is a tool: it stays in the slot. The same lenses fit the [Cutting Lance](Tools.md#cutting-lance-and-focus-lenses).
The first Quantum Chip etched records the **first quantum chip** milestone for the player
operating the etcher.

## Crystal Grower

Works without power at `crystalGrowerUnpoweredPermille` (a quarter) of its powered speed. Power and cold make
it faster: below `crystalGrowerColdThreshold` ambient heat each point adds
`crystalGrowerColdBonusPerHeatPermille`, up to `crystalGrowerColdBonusCapPermille` (twice as fast). Glacira is
the best place for a crystal farm.

Growth carried between ticks is kept only while the chunk is loaded: unloading a grower mid-crystal loses less
than one tick of growth.

## Component Forge

Up to six item inputs and one fluid. Recipes include:

- **Servo**: copper coil, cindersteel plate and parts.
- **Capacitor** uses two polymer sheets, two copper wires, two redstone dust, two gold nuggets,
  one iron ingot, one cindersteel plate and 250 mB of sulfuric acid. The Component Forge produces two
  capacitors in 200 ticks at 40 energy per tick.
- **Heat Sink**: cindersteel plate, or titanium plate (two out).
- **Superconductor Coil**: four superconductor wire round a tier-2 coil, with a Heat Sink and 250 mB of cryo
  coolant.
- **Europium Flux Lattice**: fusion casing, europium dust, cindersteel, a Quantum Chip, superconductor
  wire and cryo coolant, four out (the crafting-table recipe makes one).

A recipe viewer shows every recipe with exact counts.

## See also

- [Semiconductor chain](Chain-Semiconductor.md), [Optics chain](Chain-Optics.md)
- [Machines](Machines.md#upgrade-modules) for the two new modules
- [Home](Home.md)
