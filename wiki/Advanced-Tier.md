# Advanced Tier (Tier 2/3)

NeroTech's late game is **paced by Starsteel**, and coupled to Nerospace only *softly* — through
Neroland Core's shared `c:` tags, never a direct dependency. With Nerospace installed, space is the fast
road to that Starsteel; without it, NeroTech now supplies its own.

## How the pacing works (possession)

Every advanced machine is **crafted with Starsteel** (`#c:ingots/starsteel`). Holding the material *is*
the gate: once you have an advanced machine it works like any other — it is never separately locked or
left inert. (This is the "planet-ore possession" gate from the Phase-3 plan; it replaced an earlier
`reached_orbit` use-lock that wrongly bricked the machines in standalone/creative play.)

### Standalone path

The advanced tier is **no longer space-only**. The [Particle Collider](Particle-Collider.md) — a ring of
Accelerator Coils around a Collider Core, built from vanilla materials and Tier-1 NeroTech components —
transmutes cheap catalysts into **Starsteel Dust** and **Void Crystal Dust**, which smelt into the
materials through Neroland Core's recipes. That makes the whole advanced tier, Fusion Reactor included,
reachable on Earth alone.

It is deliberately the *slow, expensive* road: with Nerospace installed, mining meteors and planet ores
stays much faster. The collider is the route that needs no other mod — and the only one you can fully
automate.

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

A higher-yield Ore Processor (`advancedOreProcessorYieldBonus` extra dust per operation), and the
sensible place to run the **bulk raw-ore-block** recipes (raw iron/gold/copper block → 18 dust) that
ship alongside the per-item ones.

### Particle Collider

The standalone source of space-grade dust: a horizontal 5×5 or 7×7 ring of Accelerator Coils around a
Collider Core, which turns a Netherite Scrap into Starsteel Dust and an Echo Shard into Void Crystal
Dust at an enormous energy and heat cost. The 7×7 ring halves the operation time. See the dedicated
[Particle Collider](Particle-Collider.md) page.

### Advanced Fabricator

Refines space materials into reactor fuel: **Void Crystal (`#c:gems/void_crystal`) → Fusion Cell**.

The advanced machines reuse the Tier-1 GUIs (their titles identify them).

## Reactor fuels — Fusion, Plasma & Stellar Cells

The **Fusion Cell** is tier 1 and is now **machine-exclusive**: its shaped crafting recipe is gone, so
the only source is the Advanced Fabricator (Void Crystal → Fusion Cell). Building the machine is the
price of entry to fusion fuel. The **Plasma Cell** (Starsteel, tier 2) and **Stellar Cell** (Void
Crystal + Starsteel, tier 3) keep their crafting recipes — they are multi-ingredient, and the
`advanced_fabricating` recipe type takes a single input — but each is built on the tier below it, so
the whole fuel ladder now starts at the Advanced Fabricator. Higher tiers demand bigger reactor
shells — see [Fusion Reactor](Fusion-Reactor.md).

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
- [Particle Collider](Particle-Collider.md)
- [Fusion Reactor](Fusion-Reactor.md)
- [Planets](Planets.md)
- [Consequence Systems](Consequence-Systems.md)
- [Home](Home.md)
