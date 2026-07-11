# Power & the NeroPower Split

## Power in NeroTech

NeroTech owns power generation (Nero Generator, Solar Array, Fusion Reactor). Generation is kept
**deliberately thin**: generators talk only to Neroland Core's energy surface — `NeroEnergyStorage` /
`EnergyBuffer`, `EnergyConversions`, and the `EnergyLookup` seam — and to Core `c:` tags, never to
NeroTech-internal machine classes. That keeps a future extraction into a separate **NeroPower** mod a
package move rather than a rewrite.

## The split decision

The standing decision is **power stays inside NeroTech** until power gains enough depth to justify its
own mod. The trigger is **≥3** of the six criteria below holding — criteria, not vibes.

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
NeroPower design docs.

### Re-check — 2026-07-10 (0.1.0-beta.1: fusion multiblock landed)

| # | Trigger | Status | Notes |
| --- | --- | --- | --- |
| 1 | Reactors are **multiblocks** with assembly validation | ✅ Met | The [Fusion Reactor](Fusion-Reactor.md) is a validated 3³/5³/7³ shell multiblock, inert until formed. |
| 2 | A real **heat/cooling subsystem** beyond a single scalar (active coolant loops) | ⚠️ Partial | The [full thermal model](Thermal-System.md) adds conduction + per-location ambient + cached coolant adjacency — but no active coolant loops yet. |
| 3 | **Grid simulation** (transmission loss, per-line capacity, load balancing) | ❌ Not met | Energy is still push-to-neighbour via Core `EnergyLookup`; no grid model. |
| 4 | **Fuel cycles** with tiers / breeding / waste | ✅ Met | Three fuel tiers (Fusion → Plasma → Stellar Cell) via datapack tags, size-gated per shell. |
| 5 | An **overload/failure model** worth balancing independently | ✅ Met | Size-scaled meltdown (blast 4/6/8), containment breach on a broken burning shell, overdrive-preset interaction. |
| 6 | **Planet-based generation efficiency** with its own config surface | ⚠️ Partial | [Nerospace planet traits](Planets.md) now drive solar/ambient with a config fallback — a real surface, but a thin one. |

**Outcome: 3 fully met (+ 2 partial) → threshold crossed. Extraction is warranted but deferred past
0.1.0-beta** — a NeroPower extraction plan is to be drafted once the beta ships. Until then power stays
in NeroTech, and generation stays thin so the eventual move remains a package move.

## See also

- [Tier-1 Machines](Machines.md)
- [Advanced Tier](Advanced-Tier.md)
- [Consequence Systems](Consequence-Systems.md)
- [Home](Home.md)
