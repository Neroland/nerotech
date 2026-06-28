# Power & the NeroPower Split

## Power in NeroTech

NeroTech owns power generation (Nero Generator, Solar Array, Fusion Reactor). Generation is kept
**deliberately thin**: generators talk only to Neroland Core's energy surface — `NeroEnergyStorage` /
`EnergyBuffer`, `EnergyConversions`, and the `EnergyLookup` seam — and to Core `c:` tags, never to
NeroTech-internal machine classes. That keeps a future extraction into a separate **NeroPower** mod a
package move rather than a rewrite.

## The split decision

The standing decision is **power stays inside NeroTech** until power gains enough depth to justify its
own mod. The trigger is **≥3** of the six criteria below holding (see the
[Phase 3 plan](../../neroland-mc-ecosystem/nerotech/PHASE-3-PLAN.md#the-neropower-split-decision--criteria-not-vibes)).

### Re-check — 2026-06-28 (end of Phase 3 Stage 6)

| # | Trigger | Status | Notes |
| --- | --- | --- | --- |
| 1 | Reactors are **multiblocks** with assembly validation | ❌ Not met | Fusion Reactor is a single block. |
| 2 | A real **heat/cooling subsystem** beyond a single scalar (active coolant loops) | ❌ Not met | Heat is one per-machine scalar with passive + adjacency cooling. |
| 3 | **Grid simulation** (transmission loss, per-line capacity, load balancing) | ❌ Not met | Energy is push-to-neighbour via Core `EnergyLookup`; no grid model. |
| 4 | **Fuel cycles** with tiers / breeding / waste | ❌ Not met | Single-tier fuel via the `nerotech:fusion_fuels` tag; no breeding/waste. |
| 5 | An **overload/failure model** worth balancing independently | ⚠️ Partial | The Fusion Reactor has a single admin-disableable meltdown threshold — not a standalone model. |
| 6 | **Planet-based generation efficiency** with its own config surface | ⚠️ Partial | One deferred Core-config fallback (`solarDimensionMultipliers`); minimal. |

**Outcome: 0 fully met (2 partial) → fewer than 3. Power stays in NeroTech.** Splitting now would add a
cross-mod boundary for no player benefit.

**Re-evaluate when** the Fusion Reactor multiblock lands (criterion 1) — the most likely first trigger —
or when a real coolant/grid/fuel-cycle system appears. The extraction path is already specified in the
[NeroPower DESIGN](../../neroland-mc-ecosystem/neropower/DESIGN.md).

## See also

- [Tier-1 Machines](Machines.md)
- [Advanced Tier](Advanced-Tier.md)
- [Consequence Systems](Consequence-Systems.md)
- [Home](Home.md)
