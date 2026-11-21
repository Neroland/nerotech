# Metallurgy

Four machines turn metals into the parts the rest of NeroTech is built from. All of them work on a server with
only Neroland Core and NeroTech; with Nerospace, its cindrite, hematite and ilmenite make the same products
faster and cheaper. Every input is taken by tag, so another mod's material under the same tag works too.

| Block | What it does |
| --- | --- |
| **Plasma Crucible** | Melts two or three inputs into an alloy. It holds its heat between jobs; a cold start spends energy heating up first. |
| **Plate Press** | Presses ingots into plates or rods, boules into wafers and resin into polymer sheet. The die in its die slot picks the shape. |
| **Filament Spooler** | Draws ingots into wire, and winds wire round a rod or ingot core into coils. |
| **Blast Foundry** | A multiblock for bulk alloying: a controller in a 3x3x3 or 5x5x5 shell of **Foundry Brick**, heated through **Foundry Heat Ports**. |

## Plasma Crucible

A recipe can ask for a minimum heat. Below it, the crucible fires its arc: it spends
`plasmaCrucibleFiringEnergy` NE per tick and gains `plasmaCrucibleFiringHeat` heat per tick until it is hot
enough, then alloys. When idle it exchanges heat with its surroundings only every
`plasmaCrucibleHoldIntervalTicks`, so it stays warm between jobs. Run it back to back and you pay the warm-up
once. Hot worlds help: it loses less heat there.

Cindersteel comes from here first. Standalone, two iron dust, a coal and a blaze powder make one ingot
(see [Structural chain](Chain-Structural.md)).

## Plate Press and its dies

The die slot takes one of three dies, crafted from vanilla iron so the press works before your first alloy:

| Die | Shapes |
| --- | --- |
| **Plate Die** | ingot → plate; polymer resin → polymer sheet |
| **Rod Die** | ingot → rods |
| **Wafer Die** | crystal boule → wafers |

An empty die slot matches nothing. The die is never used up. Packs can add their own dies to the
`nerotech:press_dies` tag.

## Filament Spooler

Ingots become wire: copper, cindersteel, and superconductor wire from glacite and cindersteel wire. Wire wound
round a core becomes the coil tiers (Copper Coil, Cindersteel Coil). The tier-3 Superconductor Coil is built
in the [Component Forge](Components.md).

## Blast Foundry

Build a hollow cube of Foundry Brick, 3x3x3 or 5x5x5, set the Blast Foundry controller into the centre of one
wall, and swap any wall bricks you like for Foundry Heat Ports. The structure is checked when a block of it
changes, not every tick.

- A 3x3x3 foundry runs `foundryBatchUnits` (4) recipe units per cycle; a 5x5x5 runs
  `foundryLargeBatchMultiplier` (4) times that, so 16.
- **It runs only above a heat threshold, and the only way to heat it is through its Heat Ports.** A port
  takes heat from an adjacent [Heat Pipe](Heat-as-a-Resource.md) or Heat Exchanger and buffers up to
  `foundryHeatPortCapacity`. The controller drains its ports every `foundryHeatIntakeIntervalTicks`.
- The furnace loses `foundryHeatLossPermille` of its heat above ambient each tick (less in hot worlds) and a
  running batch uses `foundryHeatDrawPerTick`.
- The shell glows while it works and for `foundryLitHoldTicks` after it stops.
- Breaking a shell block stops the foundry; nothing in it is lost or duplicated.

The status line says why a foundry is not running, and hovering a gauge shows the amount and the rate. Its first finished batch records the **first foundry pour** milestone for the player operating
it.

## See also

- [Structural chain](Chain-Structural.md), [Superconductor chain](Chain-Superconductor.md)
- [Heat as a Resource](Heat-as-a-Resource.md)
- [Material Map](Material-Map.md)
- [Config](Config.md#080-machine-families-modules-tools-and-pollution-exposure)
- [Home](Home.md)
