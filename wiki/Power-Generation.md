# Power Generation

The Nero Generator and the Solar Array get a factory started; they do not keep one running. This
page covers NeroTech's **wider power tier** — three more generators, a grid buffer, a grid manager
and a wireless link — plus the Analytics Terminal's new power-history strip.

These blocks are the feature set once planned for a separate **NeroPower** mod. That split was
retired on 2026-07-31 and the features live here instead; see
[Power & the NeroPower Split](Power-and-NeroPower-Split.md) for the decision. Everything below sits
on Neroland Core's shared energy surface, so it interoperates with Core storage, with the rest of
the ecosystem, and with Forge Energy at the I/O boundary — with no cross-mod dependency.

## The line-up at a glance

| Block | Fuel | Heat | Pollution | Niche |
| --- | --- | --- | --- | --- |
| Wind Turbine | None | None | None | Remote, clean, works at night |
| Geothermal Generator | Lava / magma beneath it | Yes | None | The steady baseline |
| Bio Generator | `#nerotech:bio_fuels` items | Yes | Half a Nero Generator's | Farmable and cleaner-burning |
| Battery Bank | — | None | None | Buffering surplus |
| Grid Controller | — | None | None | Brownout protection |
| Wireless Power Node | — | None | None | Convenience, at no loss |

## Wind Turbine

Fuel-free generation that does **not** care about the time of day. Where the
[Solar Array](Machines.md#solar-array) folds flat at dusk, the turbine keeps turning.

| | |
| --- | --- |
| **Base output** | `windTurbineNePerTick` (default 25) |
| **Altitude curve** | 0.5× at or below y=80, rising linearly to 2× at or above y=200 |
| **Dimension** | × the multiplier from `windDimensionMultipliers` (default `minecraft:overworld=1.0`) |
| **Requirement** | Sky access on the block **directly above** — no daylight requirement |
| **Slots** | None. No fuel, no heat, no pollution |

So a turbine at bedrock level with sky access above it makes roughly 12 NE/tick, one at y=200 makes
50, and the climb between the two is smooth. Building tall is the upgrade path — a turbine farm on a
mountain ridge is worth more than the same blocks at sea level.

With [Nerospace](Planets.md) installed, an **airless** planet gives **zero**: no atmosphere, no
wind. Tune other dimensions by adding entries to `windDimensionMultipliers`.

## Geothermal Generator

Reads the 3×3 layer **directly beneath itself** and counts lava source blocks and magma blocks
(0–9), producing `geothermalNePerTickPerSource` (default 8) NE/tick for each one — up to 72 NE/tick
over a full 3×3 lava bed.

| | |
| --- | --- |
| **Sources** | Lava and magma blocks in the 3×3 layer one block below |
| **Output** | `geothermalNePerTickPerSource` (default 8) × source count |
| **Steadiness** | Perfectly flat — no time of day, no weather, no altitude |
| **Heat** | Runs hot, adding heat like the Nero Generator |
| **Pollution** | None |

The source count is **cached** and re-checked on a neighbour change or every 100 ticks, so a lava
bed under a wall of turbines costs nothing per tick.

It is the only fuel-free generator that runs hot, so plan for the
[thermal system](Thermal-System.md): give it airflow, coolant blocks, or a
[Coolant Pump](Fluids-and-Gases.md#coolant-pump). A lava lake in the Nether under a bank of these is
the classic build; a hand-built 3×3 pool of buckets works just as well.

## Bio Generator

The Nero Generator's renewable sibling. It burns anything in the datapack-overridable item tag
`#nerotech:bio_fuels`, which NeroTech seeds with `minecraft:dried_kelp_block`; NeroAgriculture and
packs add their own feedstock by adding to the tag — no code change, no config edit.

| | |
| --- | --- |
| **Fuel** | Any item in `#nerotech:bio_fuels` (one fuel slot) |
| **Burn time** | 4,000 ticks per item |
| **Output** | `bioGeneratorNePerBurnTick` (default 48) NE/tick — 20% above the Nero Generator |
| **Pollution** | **Half** the Nero Generator's per operation |
| **Heat** | Runs hot, like any combustion generator |

That combination is the point: it out-produces the Nero Generator on a fuel you can *farm*, and it
is the generator that keeps a region's [pollution](Pollution-and-Mitigation.md) survivable while
still burning something. It does not remove pollution — it just halves the bill, which is often the
difference between needing a Scrubber and not.

## Battery Bank

A single-block grid buffer. It generates nothing and consumes nothing; it holds
`batteryBankCapacity` (default 1,000,000) NE — an order of magnitude above a normal machine's
buffer.

| | |
| --- | --- |
| **Capacity** | `batteryBankCapacity` (default 1,000,000) NE |
| **Faces** | Every face is input **and** output by default (Core's STORAGE side preset) |
| **Auto-eject** | On by default — it pushes to neighbours that want charge |
| **Slots** | None |

Reconfigure any face with the [Configurator](Side-Config-and-Configurator.md) if you want a
one-way bank — input on the generator side, output on the factory side. Forge Energy interop is
automatic through Core's `EnergyLookup` seam, so an FE cable or an FE machine sees it as a battery.

Its job is **time-shifting**: store what your turbines and panels make at noon, spend it after dark.
Pair it with the Grid Controller and the factory rides out the night at reduced speed instead of
stopping.

## Grid Controller

A passive console — zero NE, no slots — that watches your power grid and sheds load before it
collapses.

| | |
| --- | --- |
| **Radius** | `gridControllerRadius` (default 16), rescanned every 100 ticks |
| **Measure** | Stored NE ÷ capacity summed across every NeroTech machine it finds |
| **Shed below** | `gridShedThresholdPermille` (default 200 = 20%) |
| **Restore above** | `gridRestorePermille` (default 500 = 50%) |

Below the shed threshold it drops every non-generator machine to the **Eco**
[overclock preset](Overclock-Presets.md); above the restore threshold it puts each machine back on
the preset it had. **Generators, Battery Banks, Wireless Power Nodes and consoles are exempt** — the
controller throttles *demand*, never supply.

The gap between the two thresholds is deliberate **hysteresis**. A grid hovering exactly at the line
would otherwise flap between Eco and Overdrive every rescan; with a 20%/50% band it settles.

Its GUI reports the number of machines watched, the grid fill percentage, and whether shedding is
currently in force.

> **Known limitation.** The remembered presets are held **in memory**. Machines that were shed
> before a server restart stay on Eco until the next shed/restore cycle runs or you change their
> preset by hand.

## Wireless Power Node

A pair of nodes moves power across a gap without a cable run.

**Pairing** uses the [Configurator](Side-Config-and-Configurator.md):

1. Crouch-use node **A** — the tool stores it.
2. Crouch-use node **B** — the two are paired.
3. Crouch-use an already-linked node to **unlink** it.

| | |
| --- | --- |
| **Range** | `wirelessNodeRange` (default 32) blocks, **same dimension only** |
| **Rate** | Up to `wirelessNodeTransferPerTick` (default 200) NE, passed every 5 ticks |
| **Loss** | **None** — the transfer is lossless |
| **Chunks** | Never force-loaded; an unloaded partner simply means the pass is skipped |

The node buys **convenience, never throughput**: it is flat-rate and lossless, so it can neither
beat a cable nor tax you for using one. Breaking either end unlinks the survivor automatically — the
link is revalidated on every pass.

## Analytics Terminal — power history

The [Analytics Terminal](Analytics.md#analytics-terminal) now keeps a rolling **60-sample** window of
the *net change* in aggregate stored NE across the machines it watches, one sample per 100-tick
rescan — so the strip covers the **last five minutes**.

It renders at the bottom of the dashboard as a bar sparkline: **green above the midline** where the
watched machines gained charge, **red below** where they drained it, self-scaling to the window's
peak. A wall of red means you are running a deficit and a Battery Bank is emptying somewhere.

To make room, the dashboard now lists the **10** nearest machines instead of 12.

## Which generator should I build?

- **Wind Turbine** — remote, clean and nocturnal. No fuel logistics, no heat, no pollution, and it
  works at 3am. The reward for building tall. Weakest per block at low altitude.
- **Geothermal Generator** — the steady baseline. Perfectly flat output you can plan a factory
  around, if you can find (or pour) lava and manage the heat.
- **Bio Generator** — farmable fuel and half the pollution. The right answer when you want more
  power than wind gives and less consequence than the Nero Generator costs.
- **Solar Array** — daylight only; see [Tier-1 Machines](Machines.md#solar-array).
- **Nero Generator** — the starter: cheap, dirty, and always available.

And around them:

- **Battery Bank** — buffering. It makes no power; it makes the power you already have arrive at the
  right time.
- **Grid Controller** — brownout protection. Under load the factory slows down instead of stalling
  dead.
- **Wireless Power Node** — convenience at **no loss**. Skip the cable run across the ravine; the
  transfer is flat-rate and lossless, so it is never a throughput upgrade.

A mature base usually runs all three renewables: turbines for the night, geothermal for the floor,
and a Bio Generator on standby for the peaks — with Battery Banks between them and a Grid Controller
watching the total.

## Crafting

All shaped 3×3 recipes, from vanilla and Tier-1
[NeroTech components](Materials-and-Components.md) only — the whole power tier is reachable on Earth
with no other mod installed.

| Block | Recipe |
| --- | --- |
| Wind Turbine | Machine Frame + 2 Nero Coils + 4 Iron Ingots |
| Geothermal Generator | Machine Frame + 2 Circuit Boards + 2 Obsidian + 2 Copper Ingots |
| Bio Generator | Machine Frame + Nero Coil + Composter + 2 Hay Blocks |
| Battery Bank | Machine Frame + 4 Nero Coils + 2 Circuit Boards + Copper Block |
| Grid Controller | Machine Frame + 2 Circuit Boards + Comparator |
| Wireless Power Node | Nero Coil + Circuit Board + Ender Pearl + 4 Iron Ingots |

Crafting a Battery Bank earns the **Charge Reserve** advancement, a child of the Solar Array
advancement.

## Config keys

`windTurbineNePerTick`, `windDimensionMultipliers`, `geothermalNePerTickPerSource`,
`bioGeneratorNePerBurnTick`, `batteryBankCapacity`, `gridControllerRadius`,
`gridShedThresholdPermille`, `gridRestorePermille`, `wirelessNodeRange`,
`wirelessNodeTransferPerTick`.

All live in `config/nerotech.properties` and hot-reload with `/neroland config reload`, except
`batteryBankCapacity`, which applies when a bank next loads.

---

See also: [Tier-1 Machines](Machines.md) · [Thermal System](Thermal-System.md) ·
[Pollution & Mitigation](Pollution-and-Mitigation.md) · [Overclock Presets](Overclock-Presets.md) ·
[Side Config & the Configurator](Side-Config-and-Configurator.md) · [Analytics](Analytics.md) ·
[Materials & Components](Materials-and-Components.md) · [Planets](Planets.md)
