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
they work. Generation talks only to Core's energy surface, never to NeroTech-internal classes, so it
interoperates across the ecosystem by default; NE bridges to Forge Energy (FE) at I/O boundaries via
Core's conversion ratio. Power lives in NeroTech permanently — the once-planned NeroPower split was
[retired on 2026-07-31](Power-and-NeroPower-Split.md).

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
the [Advanced Fabricator](Advanced-Tier.md). The Advanced Ore Processor additionally takes whole **raw
ore blocks** (raw iron/gold/copper block → 18 dust) on top of its yield bonus.

## Upgrade modules

Machines have four upgrade-module slots. Modules are Core upgrade types — **Speed** (more work per
cycle / faster processing), **Efficiency** (less NE per unit of work), **Capacity** (larger buffer),
and **Range** — read as aggregate modifiers each tick. NeroTech invents no bespoke upgrade system;
modules are interchangeable with any machine built on Core's framework. Modules stack with the
machine's [overclock preset](Overclock-Presets.md), and every machine GUI also carries an
[Analytics tab](Analytics.md) and a [Side Config tab](Side-Config-and-Configurator.md).

## Progression

Placing your first NeroTech machine opens Core's **`industrial_power`** progression gate — NeroTech is
the canonical opener of that milestone. That gate is a milestone other Neroland mods may read; it never
locks anything in NeroTech. The [Advanced Tier](Advanced-Tier.md) is paced purely by materials
(Starsteel/Void Crystal possession) — no use-lock, fully standalone-friendly.

## See also

- [Materials & Components](Materials-and-Components.md)
- [Thermal System](Thermal-System.md)
- [Overclock Presets](Overclock-Presets.md)
- [Analytics](Analytics.md)
- [Tech Guide](Tech-Guide.md)
- [Home](Home.md)
