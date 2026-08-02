# Power & the NeroPower Split (retired)

## The decision — 2026-07-31

**NeroPower will not ship as a separate mod.** The planned split is retired, and NeroPower's entire
intended feature set — extra generators, grid buffering, load management and wireless transfer — is
**absorbed into NeroTech**.

Those features are live now: see **[Power Generation](Power-Generation.md)** for the Wind Turbine,
Geothermal Generator, Bio Generator, Battery Bank, Grid Controller and Wireless Power Node, and
[Tier-1 Machines](Machines.md) for the Nero Generator and Solar Array that came before them.

## Why this is better for players

One mod, one power network, nothing to reason about across a mod boundary. You install NeroTech and
you have the whole power story — generation, storage, load shedding and transfer — balanced against
the same [heat](Thermal-System.md) and [pollution](Pollution-and-Mitigation.md) systems that the
rest of the mod uses. A separate NeroPower would have meant a second download, a second config file,
a version-compatibility pair to keep straight, and a permanent seam through the middle of a system
that players experience as one thing. The upside it offered was tidier source layout — a developer
benefit, paid for by everyone else.

## What this means going forward

- Power generation stays in NeroTech, permanently. Later power features land here.
- Generation still talks only to Neroland Core's energy surface (`NeroEnergyStorage` /
  `EnergyBuffer`, `EnergyConversions`, the `EnergyLookup` seam) and to Core `c:` tags. That was
  originally there to keep an extraction cheap; it is kept because it is simply the right coupling —
  it is what makes NE bridge to Forge Energy and interoperate across the ecosystem.
- The criteria table below no longer gates anything.

## Archived: the split criteria (historical)

> **Archived.** Everything from here down is preserved for context only. It records how the split
> decision was evaluated between 2026-06-28 and 2026-07-10, before the 2026-07-31 decision to retire
> the split. **It no longer governs anything.**

The standing rule at the time was that power stayed inside NeroTech until power gained enough depth
to justify its own mod, with the trigger being **≥3** of the six criteria below holding.

### Re-check — 2026-06-28 (end of Phase 3 Stage 6)

| # | Trigger | Status | Notes |
| --- | --- | --- | --- |
| 1 | Reactors are **multiblocks** with assembly validation | ❌ Not met | Fusion Reactor is a single block. |
| 2 | A real **heat/cooling subsystem** beyond a single scalar (active coolant loops) | ❌ Not met | Heat is one per-machine scalar with passive + adjacency cooling. |
| 3 | **Grid simulation** (transmission loss, per-line capacity, load balancing) | ❌ Not met | Energy is push-to-neighbour via Core `EnergyLookup`; no grid model. |
| 4 | **Fuel cycles** with tiers / breeding / waste | ❌ Not met | Single-tier fuel via the `nerotech:fusion_fuels` tag; no breeding/waste. |
| 5 | An **overload/failure model** worth balancing independently | ⚠️ Partial | The Fusion Reactor has a single admin-disableable meltdown threshold — not a standalone model. |
| 6 | **Planet-based generation efficiency** with its own config surface | ⚠️ Partial | One deferred Core-config fallback (`solarDimensionMultipliers`); minimal. |

Outcome at the time: 0 fully met (2 partial) → fewer than 3; power stayed in NeroTech.

### Re-check — 2026-07-10 (0.1.0-beta.1: fusion multiblock landed)

| # | Trigger | Status | Notes |
| --- | --- | --- | --- |
| 1 | Reactors are **multiblocks** with assembly validation | ✅ Met | The [Fusion Reactor](Fusion-Reactor.md) is a validated 3³/5³/7³ shell multiblock, inert until formed. |
| 2 | A real **heat/cooling subsystem** beyond a single scalar (active coolant loops) | ⚠️ Partial | The [full thermal model](Thermal-System.md) adds conduction + per-location ambient + cached coolant adjacency — but no active coolant loops yet. |
| 3 | **Grid simulation** (transmission loss, per-line capacity, load balancing) | ❌ Not met | Energy is still push-to-neighbour via Core `EnergyLookup`; no grid model. |
| 4 | **Fuel cycles** with tiers / breeding / waste | ✅ Met | Three fuel tiers (Fusion → Plasma → Stellar Cell) via datapack tags, size-gated per shell. |
| 5 | An **overload/failure model** worth balancing independently | ✅ Met | Size-scaled meltdown (blast 4/6/8), containment breach on a broken burning shell, overdrive-preset interaction. |
| 6 | **Planet-based generation efficiency** with its own config surface | ⚠️ Partial | [Nerospace planet traits](Planets.md) now drive solar/ambient with a config fallback — a real surface, but a thin one. |

Outcome at the time: 3 fully met (+2 partial) → the threshold was crossed and an extraction was
considered warranted but deferred past 0.1.0-beta. That deferred extraction is what the 2026-07-31
decision above cancels.

## See also

- [Power Generation](Power-Generation.md)
- [Tier-1 Machines](Machines.md)
- [Advanced Tier](Advanced-Tier.md)
- [Consequence Systems](Consequence-Systems.md)
- [Home](Home.md)
