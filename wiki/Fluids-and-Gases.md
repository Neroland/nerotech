# Fluids & Gases

NeroTech's **gas chain** turns water and electricity into two working gases — **hydrogen** and
**oxygen** — and then spends them on clean power and better ore yields. Alongside it sits the
**coolant loop**, the active answer to the [thermal system](Thermal-System.md)'s heat.

Everything here is built on Neroland Core's shared fluid and gas contracts, so NeroTech's tanks
interoperate with Core's own Fluid Tank and Gas Tank — and with any other mod on those surfaces —
with no cross-mod dependency in either direction.

## The chain at a glance

```text
                        ┌──────────────┐  hydrogen ──▶  Gas Turbine ──▶ NE
   water bucket ──▶ ────┤ Electrolyzer ├──
        (or fluid pipe) └──────────────┘  oxygen   ──▶  Chemical Processor ──▶ 3x dust
                               ▲
                              NE
```

Gas is measured in **millibuckets (mB)**, exactly like fluids. One balance **unit** is 100 mB — the
config numbers are written in units so they stay readable.

## Electrolyzer

Splits stored water into hydrogen and oxygen at the electrolytic 2:1 ratio.

| | |
| --- | --- |
| **Water in** | Right-click with a water bucket (1000 mB), or push water in through Core's fluid capability on any face |
| **Power** | `electrolyzerNePerTick` (default 40) while running |
| **Operation** | `electrolyzerOperationTicks` (default 200), consuming `electrolyzerWaterPerOp` (default 500 mB) |
| **Output** | `electrolyzerHydrogenPerOp` (default 200 mB = 2 units) + `electrolyzerOxygenPerOp` (default 100 mB = 1 unit) |
| **Tanks** | `machineFluidCapacity` water, `machineGasCapacity` per gas (defaults 4000 mB each) |
| **Pollution** | None — the gas chain is NeroTech's clean branch |

It has **no item slots**: its entire I/O is fluid in, gas out. Once a second it hands both products
to any adjacent gas-accepting block — a Core Gas Tank, a Gas Turbine, a Chemical Processor. On the
capability itself the two products are split by face: the **bottom** face serves oxygen, every other
face serves hydrogen.

The GUI shows three extra gauges beside energy and heat: water (blue), hydrogen (pale), oxygen (teal).

## Gas Turbine

Burns gas into NE and pushes it to its neighbours exactly the way the Nero Generator does — **mild
heat, zero pollution**. That is the whole point of it: clean power, not free power.

| | |
| --- | --- |
| **Fuel** | Whatever `turbineGasBurn` lists — default `nerotech:hydrogen=2` |
| **Yield** | `gasTurbineNePerUnit` (default 60) × the fuel's multiplier, per unit (100 mB) |
| **Burn time** | `gasTurbineTicksPerUnit` (default 20) per unit, with the yield spread evenly |
| **Slots** | None — its fuel is a gas, pushed in through Core's gas capability |

The tank refuses any gas that is **not** on the fuel list, so a stray gas can never jam it. Adding a
fuel is a config edit, not a code change: `turbineGasBurn=nerotech:hydrogen=2,othermod:methane=5`.

> **Balance note.** Electrolysis is deliberately a *net energy sink* — you spend far more NE
> splitting water than the turbine gives back. The turbine's value is that it is clean, storable
> and buffered, not that it multiplies power. Retune `gasTurbineNePerUnit` if your pack wants a
> different trade.

## Chemical Processor

Washes raw ore with oxygen. Same one-in/one-out shape as the Ore Processor, but each completed
operation also consumes `chemicalProcessorGasPerOp` (default 250 mB) of oxygen — and pays you back
in yield:

| Input | Ore Processor | Chemical Processor |
| --- | --- | --- |
| Raw Iron | 2 Iron Dust | **3** Iron Dust |
| Raw Gold | 2 Gold Dust | **3** Gold Dust |
| Raw Copper | 2 Copper Dust | **3** Copper Dust |

Recipes are datapack-driven (`nerotech:chemical_processing`) and appear on their own JEI page, so a
pack can add washes for any item. With a dry oxygen tank the machine reports **Starved** and draws
no power at all.

## The coolant loop

Two blocks, and a deliberate simplification: there are no coolant *pipes* to lay. A **Coolant Pump**
cools every machine it touches, and **Radiators** near the pump make it stronger.

### Radiator

A passive block with no GUI and no power draw. Two jobs:

- Placed **against a machine**, it counts as **four** natural coolant blocks for the thermal model's
  passive dissipation — and unlike ice, it never melts.
- Placed **near a Coolant Pump**, it scales that pump's pull rate.

### Coolant Pump

On every thermal exchange interval it spends NE to pull heat out of each adjacent machine that is
above ambient, and deletes it — that deletion *is* the loop.

| | |
| --- | --- |
| **Power** | `coolantPumpNePerTick` (default 20), billed in one batch per exchange interval |
| **Pull** | `coolantPumpHeatPerOp` (default 20) from **each** adjacent machine, × (1 + radiators) |
| **Radiator reach** | Up to 3 blocks in a straight line on each of the six axes, counting until the run breaks |
| **GUI** | None — it is slotless and has nothing to configure |

So a bare pump pulls 20 heat per exchange from each neighbour; a pump with a 3-block radiator run on
two opposite sides (6 radiators) pulls 140. Cooling never drops a machine below its local ambient.

Both the radiator scan and the machine scan are **event-driven**: the pump rescans only after a
neighbour change, and never sweeps per tick.

## Crafting

| Block | Recipe |
| --- | --- |
| Electrolyzer | Machine Frame + 4 Circuit Boards + 4 Copper Ingots |
| Gas Turbine | Machine Frame + 2 Nero Coils + 4 Iron Ingots |
| Chemical Processor | Machine Frame + 2 Circuit Boards + 6 Glass |
| Radiator (×2) | 4 Iron Ingots + 4 Copper Ingots + a Water Bucket |
| Coolant Pump | Machine Frame + 2 Nero Coils + 4 Iron Ingots + 2 Buckets |

All vanilla and Tier-1 NeroTech materials — the whole gas chain and coolant loop are reachable on
Earth with no other mod installed.

## Interop

NeroTech declares its own gases (`nerotech:hydrogen`, `nerotech:oxygen`) because Neroland Core
deliberately ships none — Core's gas layer identifies a gas generically by id and leaves the actual
gases to content mods. Storage and transfer go through Core's `NeroGasStorage` / `NeroFluidStorage`
contracts and the shared fluid/gas capabilities, so:

- Core's **Gas Tank** and **Fluid Tank** work with these machines out of the box.
- Another mod's gas (say a Nerospace oxygen) can be fed to the turbine by adding it to
  `turbineGasBurn`.
- There is **no NeroTech gas network** — handoff is direct block-to-block adjacency, once a second.
  Long-distance routing is a job for pipes, not for this mod.

## Known simplifications

- The coolant loop is **two blocks, not a plumbed circuit**. A pipe-and-fitting loop was designed and
  cut: the pump-plus-radiators model gives the same gameplay lever (spend power and space to cool
  hard) with none of the network bookkeeping.
- The turbine's fuel roster is a **config map**, not a tag. Core's gas layer has no gas *registry* to
  tag, so a `nerotech:turbine_fuels` tag surface has to wait for one.
- The Chemical Processor's oxygen cost is a **single config value**, not a per-recipe field, so every
  wash costs the same reagent.

## Config keys

`machineGasCapacity`, `machineFluidCapacity`, `electrolyzerNePerTick`, `electrolyzerOperationTicks`,
`electrolyzerWaterPerOp`, `electrolyzerHydrogenPerOp`, `electrolyzerOxygenPerOp`,
`gasTurbineNePerUnit`, `gasTurbineTicksPerUnit`, `turbineGasBurn`, `chemicalProcessorGasPerOp`,
`coolantPumpNePerTick`, `coolantPumpHeatPerOp`.

All live in `config/nerotech.properties` and hot-reload with `/neroland config reload`, except the
two tank capacities, which apply when a machine next loads.

---

See also: [Thermal System](Thermal-System.md) · [Tier-1 Machines](Machines.md) ·
[Overclock Presets](Overclock-Presets.md) · [Side Config & the Configurator](Side-Config-and-Configurator.md)
