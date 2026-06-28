# Advanced Tier (Tier 2/3)

NeroTech's late game is **gated behind reaching space**, but coupled to Nerospace only *softly* — through
Neroland Core's shared `c:` tags and progression gates, never a direct dependency. With Nerospace
absent, the Earth tier still plays fully standalone; the advanced tier is simply unreachable.

## How the gate works (two layers)

1. **Possession (crafting):** every advanced machine is crafted with **Starsteel**
   (`#c:ingots/starsteel`) — a material only space supplies (via Nerospace). On Earth-only play there is
   no Starsteel source, so the advanced tier can't be crafted at all.
2. **Activation (use):** opening an advanced machine checks Core's **`reached_orbit`** gate for the
   player (`ProgressionGates.isOpen`). Until it's open the machine stays inert and tells you so. NeroTech
   also reacts to the gate opening via `CoreEvents.onProgression`.

Both checks are server-authoritative.

## Machines

### Fusion Reactor

Late-game high-output generation. Burns a fuel from the **`nerotech:fusion_fuels`** tag — a
datapack-overridable tag, so NeroTech's Fusion Cell, a Nerospace fuel, or a Mekanism product can all
power it (recognised by tag, never by class). It runs very hot; the red heat gauge telegraphs the
danger. Left unmanaged it reaches max heat and:

- with **`fusionReactorMeltdownEnabled=true`** (default) — **melts down destructively** (a contained
  explosion), or
- with it **false** (survival-friendly servers) — simply **stalls** until it cools.

### Advanced Ore Processor

A higher-yield Ore Processor (`advancedOreProcessorYieldBonus` extra dust per operation).

### Advanced Fabricator

Refines space materials into reactor fuel: **Void Crystal (`#c:gems/void_crystal`) → Fusion Cell**.

The advanced machines reuse the Tier-1 GUIs (their titles identify them).

## Reactor fuel — the Fusion Cell

Crafted from Starsteel + Void Crystal, or produced by the Advanced Fabricator. Tagged into
`nerotech:fusion_fuels`.

## Deferred: per-planet generation

Per-planet generation modifiers (e.g. Solar Array output by world) wait on a published `nerospace.api`
planet-trait query. Until then NeroTech reads a **Core-config fallback** keyed by dimension id
(`solarDimensionMultipliers`, e.g. `nerospace:greenxertz=1.5`); Earth (overworld) defaults to 1.0. No
Nerospace import is involved.

## See also

- [Tier-1 Machines](Machines.md)
- [Consequence Systems](Consequence-Systems.md)
- [Home](Home.md)
