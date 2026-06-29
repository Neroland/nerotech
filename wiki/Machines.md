# Tier-1 Machines (Earth)

NeroTech's first machines run on Earth ores and **Nero Energy (NE)** stored in each machine's
internal buffer. They are built on Neroland Core's machine/energy/upgrade framework, so energy and
upgrade modules behave identically across the ecosystem. Every machine is directional, opens a GUI on
right-click, and survives relog (state is saved server-side).

A working Earth-tier chain is **generator → ore processor → fabricator**: a Nero Generator (or Solar
Array) powers the processing machines, which it feeds via Core's shared energy network.

## Power & energy

Machines store **NE** in a Core `EnergyBuffer`. Generators produce NE and **push** it to adjacent
machines/storage through Core's loader-neutral energy lookup; consumers pull from their own buffer as
they work. Because generation talks only to Core's energy surface (never to NeroTech-internal classes),
power can later migrate to NeroPower as a package move rather than a rewrite. NE bridges to Forge
Energy (FE) at I/O boundaries via Core's conversion ratio.

All throughput, capacities and rates are **server-config** (`config/nerotech.properties`, managed by
Core; server-authoritative and synced to clients).

## Generators

### Nero Generator

The entry-level power source. Burns a solid fuel (coal, charcoal, coal block, blaze rod, dried kelp
block) into NE, then pushes it to neighbours. One fuel slot; four upgrade slots.

### Solar Array

Daytime, fuel-free generation. Output scales with daylight and requires sky access (it stops at night
or when roofed over). On Earth it runs fully standalone; per-planet output modifiers arrive with the
Nerospace tier (Stage 4).

## Processing machines

### Ore Processor

Turns raw Earth ore into **2 dust** — the core of the material economy (iron/copper/gold ore, deepslate
ore, and raw metal all yield the matching `c:dusts/<metal>`). One input slot, one output slot. Consumes
NE per tick while working; stalls cleanly when unpowered.

### Fabricator

Converts refined materials into NeroTech components (e.g. iron ingot → Machine Frame, copper dust →
Nero Coil, gold dust → Circuit Board). Same powered, recipe-driven behaviour as the Ore Processor.

> Recipes for the Ore Processor and Fabricator currently ship as an in-code baseline (isolated for a
> later datapack `RecipeType`/`RecipeSerializer` swap), mirroring the Nerospace approach.

## Upgrade modules

Machines have four upgrade-module slots. Modules are Core upgrade types — **Speed** (more work per
cycle / faster processing), **Efficiency** (less NE per unit of work), **Capacity** (larger buffer),
and **Range** — read as aggregate modifiers each tick. NeroTech invents no bespoke upgrade system;
modules are interchangeable with any machine built on Core's framework.

## Progression

Placing your first NeroTech machine opens Core's **`industrial_power`** progression gate — NeroTech is
the canonical opener of that milestone. Later tiers gate behind `reached_orbit` (Stage 4).

## See also

- [Materials & Components](Materials-and-Components.md)
- [Home](Home.md)
