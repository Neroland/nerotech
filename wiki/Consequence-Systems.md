# Consequence Systems — Heat & Pollution

NeroTech couples throughput to two consequence axes so automation has trade-offs: **heat** (local, per
machine) and **pollution** (regional, periodic). Both are server-authoritative and fully server-config
tunable (`config/nerotech.properties`, managed by Core).

## Heat

Every machine carries a **heat** value (shown as the red gauge on the right of its GUI, and in the
colour of its glow in-world):

- **Accumulates** while the machine works — generators while burning, processors each work tick —
  scaled by the machine's [overclock preset](Overclock-Presets.md).
- **Flows**: since 0.1.0-beta.1, heat is a [full thermal model](Thermal-System.md) — adjacent
  machines conduct heat to each other, everything relaxes toward a per-dimension/biome **ambient**,
  and **coolant blocks** (water, ice, packed/blue ice, snow block, powder snow) vent it faster.
- **Throttles**: once heat reaches the threshold, a processing machine **stalls** until it cools back
  down. This is the counter-pressure that stops players trivially overclocking everything with Speed
  modules.

Heat is per-block-entity with cached neighbour links (no global scan) and persists across relog.
Tuning keys: `heatCapacity`, `heatPerOperation`, `heatDissipationPerTick`, `heatThrottleThreshold`,
plus the `thermal*` keys listed in [Thermal System](Thermal-System.md).

## Pollution

Industrial activity emits **pollution**, aggregated **regionally** (a coarse 64×64-block grid), never
per-block-per-tick:

- Running machines add to their region's total on a **batched interval** (spread across ticks per
  machine), so there is no per-tick hotspot. Generators and processors pollute; the **Solar Array is
  clean** (no heat, no pollution).
- A **periodic server sweep** decays every region's pollution and prunes emptied regions — it iterates
  a small map, not the world.

Tuning keys: `pollutionPerOperation`, `pollutionContributionIntervalTicks`,
`pollutionDecayIntervalTicks`, `pollutionDecayAmount`.

Since 0.1.0-beta.1, pollution can also be **actively cleaned** — see
[Pollution & Mitigation](Pollution-and-Mitigation.md) for the Scrubber, the Filter Cartridge cycle,
the Remediator, and the (dormant) regional threshold events.

### Per-player attribution & privacy (POPIA / GDPR)

By **default, pollution is aggregate-only** — no player data is stored. A server may opt in with
`pollutionPerPlayerAttribution=true`, which attributes pollution to the **placing player's UUID**.
When (and only when) this is enabled:

- Only **UUIDs** are stored (never names, IPs, chat, or location) — data minimisation.
- Attribution rows are **retention-pruned** after `pollutionAttributionRetentionDays`.
- A **`PlayerDataEraser`** is registered with Core's shared `data.PlayerDataErasure`, so a single
  erase request (or Core's inactivity sweep) purges a player's NeroTech attribution alongside every
  other Nero mod.

## See also

- [Tier-1 Machines](Machines.md)
- [Thermal System](Thermal-System.md)
- [Pollution & Mitigation](Pollution-and-Mitigation.md)
- [Home](Home.md)
