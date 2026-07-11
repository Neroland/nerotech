# Advanced Tier (Tier 2/3)

NeroTech's late game is **gated behind reaching space**, but coupled to Nerospace only *softly* — through
Neroland Core's shared `c:` tags, never a direct dependency. With Nerospace absent the Earth tier still
plays fully standalone, and the advanced tier is simply uncraftable.

## How the gate works (possession)

Every advanced machine is **crafted with Starsteel** (`#c:ingots/starsteel`) — a material only space
supplies (via Nerospace). On Earth-only play there is no Starsteel source, so the advanced tier can't be
crafted at all; once you *have* an advanced machine (you reached space to get the Starsteel), it works
like any other machine — it is never separately locked or left inert. (This is the "planet-ore
possession" gate from the Phase-3 plan; it replaced an earlier `reached_orbit` use-lock that wrongly
bricked the machines in standalone/creative play.)

## Machines

### Fusion Reactor

Late-game high-output generation — and, since 0.1.0-beta.1, a **scalable multiblock**: a hollow
3×3×3, 5×5×5 or 7×7×7 shell of Fusion Casing / Containment Glass around a controller that is inert
until the shell forms. It burns fuels from the **`nerotech:fusion_fuels`** tag family — a
datapack-overridable surface, so NeroTech's cells, a Nerospace fuel, or a Mekanism product can all
power it (recognised by tag, never by class) — with three fuel tiers gated by shell size. It runs
very hot, and left unmanaged melts down with a shell-scaled blast (`fusionReactorMeltdownEnabled=false`
makes it merely stall instead). See the dedicated [Fusion Reactor](Fusion-Reactor.md) page for shell
sizes, fuel tiers, meltdown and containment breach.

### Advanced Ore Processor

A higher-yield Ore Processor (`advancedOreProcessorYieldBonus` extra dust per operation).

### Advanced Fabricator

Refines space materials into reactor fuel: **Void Crystal (`#c:gems/void_crystal`) → Fusion Cell**.

The advanced machines reuse the Tier-1 GUIs (their titles identify them).

## Reactor fuels — Fusion, Plasma & Stellar Cells

The **Fusion Cell** (Starsteel + Void Crystal, or produced by the Advanced Fabricator) is tier 1;
0.1.0-beta.1 adds the **Plasma Cell** (Starsteel, tier 2) and **Stellar Cell** (Void Crystal +
Starsteel, tier 3). Higher tiers demand bigger reactor shells — see
[Fusion Reactor](Fusion-Reactor.md).

The **Remediator** — the heavy pollution cleaner from
[Pollution & Mitigation](Pollution-and-Mitigation.md) — is also Starsteel-gated and arrives with
this tier.

## Per-planet generation

Shipped in 0.1.0-beta.1: with Nerospace installed, Solar Array output and thermal ambient follow
**planet traits** automatically; without it, the per-dimension config fallback
(`solarDimensionMultipliers`, `thermalAmbientByDimension`) applies. See [Planets](Planets.md).
No Nerospace import is involved either way — the coupling is runtime-optional.

## See also

- [Tier-1 Machines](Machines.md)
- [Fusion Reactor](Fusion-Reactor.md)
- [Planets](Planets.md)
- [Consequence Systems](Consequence-Systems.md)
- [Home](Home.md)
