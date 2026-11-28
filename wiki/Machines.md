# Machines

This page starts with NeroTech's first, Tier-1 machines, then lists the 0.8.0 roster, the utility blocks
and what every machine screen shows.

## Tier-1 machines (Earth)

NeroTech's first machines run on Earth ores and **Nero Energy (NE)** stored in each machine's
internal buffer. They are built on Neroland Core's machine/energy/upgrade framework, so energy and
upgrade modules behave identically across the ecosystem. Every machine is directional, opens a GUI on
right-click, and survives relog (state is saved server-side).

A working Earth-tier chain is **generator → ore processor → fabricator**: a Nero Generator (or Solar
Array) powers the processing machines, which it feeds via Core's shared energy network.

## Power & energy

Machines store **NE** in a Core `EnergyBuffer`. Generators produce NE and **push** it to adjacent
machines/storage through Core's loader-neutral energy lookup; consumers pull from their own buffer as
they work. Generation talks only to Core's energy surface, never to NeroTech-internal classes, so it
interoperates across the ecosystem by default; NE bridges to Forge Energy (FE) at I/O boundaries via
Core's conversion ratio. The optional NeroPower add-on builds deeper power engineering on top of
NeroTech — see [Power and NeroPower](Power-and-NeroPower-Split.md).

All throughput, capacities and rates are **server-config** (`config/nerotech.properties`, managed by
Core; server-authoritative and synced to clients).

## Generators

The two starter generators are below; the Wind Turbine, Geothermal Generator, Bio Generator and the
grid blocks that go with them are on [Power Generation](Power-Generation.md).

### Nero Generator

The entry-level power source. Burns a solid fuel (coal, charcoal, coal block, blaze rod, dried kelp
block) into NE, then pushes it to neighbours. One fuel slot; four upgrade slots.

### Solar Array

Daytime, fuel-free generation. Output scales with daylight and requires sky access (it stops at night
or when roofed over — watch the panel deck track the sun and fold flat at night). On Earth it runs
fully standalone; on other worlds output follows [planet traits or per-dimension config](Planets.md).

**Niche:** the Solar Array is NeroTech's *basic* single-block panel — low output, but it takes upgrade
modules and side config like every Tier-1 machine. It is deliberately not the same product as
Nerospace's Solar Panels (three tiers, adjacent-panel array pooling, airless-dimension 2× bonus): those
are the scalable solar line. Both generate onto the same Core energy network, so they complement rather
than duplicate each other.

## Processing machines

### Ore Processor

Turns raw Earth ore into **2 dust** — the core of the material economy (iron/copper/gold ore, deepslate
ore, and raw metal all yield the matching `c:dusts/<metal>`). One input slot, one output slot. Consumes
NE per tick while working; stalls cleanly when unpowered.

### Fabricator

Converts refined materials into NeroTech components (e.g. iron ingot → Machine Frame, copper dust →
Nero Coil, gold dust → Circuit Board). Same powered, recipe-driven behaviour as the Ore Processor.

> Since 0.1.0-beta.1, machine recipes are **real datapack recipes** — types `nerotech:ore_processing`,
> `nerotech:fabricating` and `nerotech:advanced_fabricating` — so packs can add, replace or remove
> them like any other recipe JSON.

## Processing chains

The dust economy now closes back into vanilla. Every NeroTech dust **smelts or blasts into its ingot**
(Iron, Gold and Copper Dust → the matching vanilla ingot, at vanilla furnace times and XP), so the full
loop is *ore → Ore Processor → 2 dust → furnace → 2 ingots*: double yield for the cost of a second
smelt.

Machines are also deliberately **better than the workbench**, not just faster. Where a component has
both a crafting recipe and a Fabricator recipe, the Fabricator wins: a Machine Frame costs eight iron
ingots by hand but a single ingot in the Fabricator, and one Gold or Copper Dust yields **two** Circuit
Boards / Nero Coils instead of one. Building the machine pays for itself.

Some recipes are machine-only: **Fusion Cells no longer have a crafting recipe** and come solely from
the [Advanced Fabricator](Advanced-Tier.md). Both ore processors also take whole **raw ore blocks**
(raw iron/gold/copper block → 18 dust); the Advanced Ore Processor adds its yield bonus on top.

Nothing in the Tier-1 line needs dust *before* you have power: the Nero Coil and Circuit Board have
hand recipes from vanilla ingots (Copper Ingot + Iron Ingot + Copper Ingot; Redstone + Gold Ingot +
Redstone), so the first generator is always craftable. The dust recipes and the Fabricator are the
cheaper routes once the processor is running. See [Standalone Progression](Progression.md).

## The 0.8.0 roster

0.8.0 ("Offworld Industry") adds 22 blocks. Each machine has a GUI, side configuration, analytics, overclock
presets, upgrade slots and a [Tech Guide](Tech-Guide.md) entry, like every machine before it, and a default side
preset so it works the moment it is placed.

| Family | Blocks | Page |
| --- | --- | --- |
| Metallurgy | Plasma Crucible, Plate Press, Filament Spooler, Blast Foundry, Foundry Brick, Foundry Heat Port | [Metallurgy](Metallurgy.md) |
| Chemistry | Centrifuge, Cryo Distiller, Atmospheric Condenser, Reaction Vat, Process Tank | [Chemistry and Cryogenics](Chemistry-and-Cryogenics.md) |
| Components | Component Forge, Laser Etcher, Crystal Grower | [Components](Components.md) |
| Heat | Heat Pipe, Heat Exchanger | [Heat as a Resource](Heat-as-a-Resource.md) |
| Extraction | Deep Core Drill, Drill Derrick | [Deposits and Drilling](Deposits-and-Drilling.md) |
| Fusion | Fuel Injector, Europium Flux Lattice | [Fusion Fuel](Fusion-Fuel.md) |
| Utility | Induction Charger, Annunciator Panel | below |

Machines next to each other hand over fluids and gases as well as items: a machine pushes its fluid or gas
output into the neighbour on an output face and pulls inputs from one on an input face, following the side
configuration, the same way items move.

Every 0.8.0 machine shows its state without opening it: a status lamp on the model tells idle, working,
starved and overheating apart, and working machines have their own sounds (with subtitles).

## Utility blocks

### Induction Charger

A charging pad. It tops up energy items held or worn by players standing within `inductionChargerRadius`
(4) blocks, and charges companions and other entities from mods that use NeroTech's charging API. It hands
out at most `inductionChargerTransferPerTick` NE per tick, shared between everything in reach, and takes power
from any side. Every NeroTech tool charges here, and so does any item from another mod that stores Neroland
Core energy, such as a powered suit or a battery. Players can be left out with
`inductionChargePlayers=false`; entity charging keeps working.

The charger never records an owner and stores nothing about who it charged.

### Annunciator Panel

An alarm board for a base. It reads the NeroTech machines touching its six faces and lights one lamp per
alarm: **overheat**, **starved**, **full**, **polluting** and **failing**. It needs no power and no GUI to read;
a lamp blinks for a few seconds when it first lights, and the panel beeps once (`annunciatorAlarmSound`).

Comparator output depends on the panel's channel. Crouch-use with an empty hand cycles it:

| Channel | Comparator strength |
| --- | --- |
| `any` (default) | 3 per lit alarm: 0, 3, 6, 9, 12 or 15 |
| `overheat`, `starved`, `full`, `polluting`, `failing` | 15 while that alarm is lit, else 0 |

For one redstone line per alarm, put one panel per alarm on the same machine. Panels do not read other
panels, so they do not chain. A plain use opens the status screen.

## Gauge tooltips

Hover any bar or gauge in a NeroTech machine screen to see what it is, how much it holds against its
capacity, and its rate: energy in and out per tick, heat and how close it is to the throttle point, tank fill
or drain, and progress with the time left. Where the machine is stalled, the tooltip names the cause and what
fixes it. The numbers are what your own screen already shows; nothing extra is sent about other players.

## Upgrade modules

Machines have four upgrade-module slots. Modules are Core upgrade types — **Speed** (more work per
cycle / faster processing), **Efficiency** (less NE per unit of work), **Capacity** (larger buffer),
and **Range** — read as aggregate modifiers each tick. NeroTech invents no bespoke upgrade system;
modules are interchangeable with any machine built on Core's framework. Modules stack with the
machine's [overclock preset](Overclock-Presets.md), and every machine GUI also carries an
[Analytics tab](Analytics.md) and a [Side Config tab](Side-Config-and-Configurator.md).

Two more modules go in the same slots:

- **Heat Sink Module** — widens the safe heat band: each one moves the point where the machine
  throttles (`heatThrottleThreshold`) an eighth of the way toward the heat ceiling
  (`heatSinkModuleHeadroomPermille`). At most four count, and together they never open more than half
  of that headroom, so a module never brings a machine to its ceiling or a reactor to meltdown.
- **Scrubber Module** — each one cuts the pollution a machine emits per operation by a fifth
  (`scrubberModulePollutionPermille`), down to a quarter of normal at most (`scrubberModuleFloorPermille`).

These two are NeroTech's own modules: Core's upgrade framework holds them like any other card, and
NeroTech's machines read them.

Tools you carry (the Power Drill, Cutting Lance, Arc Wrench, Multimeter, Survey Scanner, Thermal Goggles
and Filter Mask) are on the [Tools](Tools.md) page.

## Progression

Placing your first NeroTech machine opens Core's **`industrial_power`** milestone — NeroTech is
the canonical opener of that milestone. That gate is a milestone other Neroland mods may read; it never
locks anything in NeroTech. The [Advanced Tier](Advanced-Tier.md) is paced purely by materials
(Starsteel/Void Crystal possession) — no use-lock, fully standalone-friendly.

## See also

- [Materials & Components](Materials-and-Components.md)
- [Material Map](Material-Map.md)
- [Config](Config.md)
- [Standalone Progression](Progression.md)
- [Thermal System](Thermal-System.md)
- [Overclock Presets](Overclock-Presets.md)
- [Analytics](Analytics.md)
- [Tech Guide](Tech-Guide.md)
- [Home](Home.md)
