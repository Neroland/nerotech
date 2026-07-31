# Thermal System

Since 0.1.0-beta.1, heat is a **full thermal model**, not just a per-machine counter. Every machine
still carries a heat value (the red gauge on the right of its GUI, and the colour of its glow
in-world), but that value now responds to the machine's **neighbours**, its **environment**, and its
**settings** — so where you build matters as much as what you build.

## How a machine heats and cools

Each machine's heat is the balance of four flows:

- **Work generates heat.** Generators heat while burning, processors heat each working tick
  (`heatPerOperation`). The [overclock preset](Overclock-Presets.md) scales this: Eco halves it,
  Overdrive doubles it.
- **Adjacent machines conduct heat to each other.** A fraction of the heat difference between two
  touching machines is exchanged on a regular interval (`thermalConductivityPermille`,
  `thermalExchangeIntervalTicks`) — a hot fusion reactor warms the machines packed against it, and a
  wall of processors run back-to-back shares one heat budget. Spacing machines out keeps them
  independent.
- **Everything relaxes toward the local ambient.** Machines shed (or absorb!) heat toward their
  surroundings each tick (`thermalEnvLossPermille`). Ambient comes from the **dimension** — the
  Nether runs hot by default, and servers can tune any dimension via `thermalAmbientByDimension` —
  plus a **biome** flavour term (`thermalBiomeScale`): deserts run warm, snowy biomes run cold. On
  worlds with Nerospace installed, [planet traits set the ambient directly](Planets.md).
- **Coolant blocks vent heat faster.** Water, ice, packed/blue ice, snow blocks and powder snow
  placed against a machine each add extra dissipation (`heatDissipationPerTick`).
- **Radiators are purpose-built coolant.** A [Radiator](Fluids-and-Gases.md#the-coolant-loop) placed
  against a machine counts as **four** natural coolant blocks — and, unlike ice, it never melts.
- **Coolant Pumps cool actively.** A [Coolant Pump](Fluids-and-Gases.md#the-coolant-loop) spends NE
  to pull heat out of *every* adjacent machine, scaled by how many Radiators feed it. This is how you
  keep a Fusion Reactor or Particle Collider running flat out without a wall of ice.

## Throttling

Nothing has changed about the consequence: when a processing machine's heat reaches
`heatThrottleThreshold` it **stalls until it cools back down**. The [Analytics tab](Analytics.md)
names this state explicitly (*Throttled*) and its heat sparkline shows the sawtooth of a machine
riding its limit.

The practical upshot of the full model: a snowy mountain base genuinely out-cools a Nether workshop,
cramming machines together is a real trade-off, and cooling is now a base-layout puzzle rather than
just "put water next to it".

## Performance

The model is deliberately cheap: neighbour and coolant links are **cached** and only rebuilt when an
adjacent block actually changes, and exchange runs on a phase-spread interval — there is no per-tick
neighbour scanning anywhere.

## Config keys

`heatCapacity`, `heatPerOperation`, `heatDissipationPerTick`, `heatThrottleThreshold`,
`thermalConductivityPermille`, `thermalEnvLossPermille`, `thermalExchangeIntervalTicks`,
`thermalAmbientDefault`, `thermalAmbientByDimension`, `thermalBiomeScale` — all in
`config/nerotech.properties`, server-authoritative.

## See also

- [Consequence Systems](Consequence-Systems.md)
- [Overclock Presets](Overclock-Presets.md)
- [Analytics](Analytics.md)
- [Planets](Planets.md)
- [Home](Home.md)
