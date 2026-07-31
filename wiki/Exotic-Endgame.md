# Exotic Endgame

Past the [Fusion Reactor](Fusion-Reactor.md) and the [Particle Collider](Particle-Collider.md) sits
NeroTech's last tier: two blocks and one item that only exist because you already have both
multiblocks running. Nothing here is a straight upgrade — each one trades something real.

## Antimatter Cell

**Tier-4 fusion fuel**, and the only item in the mod the [Particle Collider](Particle-Collider.md)
alone can make: feed it a **Stellar Cell** and an **Antimatter Cell** comes out. There is no crafting
recipe and no other source.

| Property | Value |
| --- | --- |
| Fuel tier | 4 (`nerotech:fusion_fuel/tier4`) |
| Burns for | `fusionFuelTier4BurnTicks` — default **28 800 ticks** (24 minutes), twice a Stellar Cell |
| Shell required | the **7×7×7 only** |
| Heat | **+2** on the reactor's heat-rate multiplier while it burns |
| Stack size | 16 |

### The trade

A tier-N fuel normally needs a shell of tier ≥ N, which would put tier 4 out of reach entirely. The
rule that lets it burn at all is that the **maximal 7×7×7 shell reaches one tier above its own** — so
antimatter runs in the biggest reactor you can build and in nothing else.

And it runs it hot. The 7³ shell already burns at ×6 the base heat rate; antimatter pushes that to
×8, before [Overdrive](Overclock-Presets.md) doubles it again. A 7³ reactor on antimatter at Overdrive
is the highest sustained output in NeroTech and is genuinely likely to melt down if you leave it
alone. Plan the cooling first:

- Ring the controller with [Radiators and a Coolant Pump](Fluids-and-Gases.md) — passive ice walls
  will not keep up.
- Watch the core's glow: it shifts toward red and **strobes** past the throttle threshold.
- If you would rather not gamble a base on it, `fusionReactorMeltdownEnabled=false` turns meltdown
  into a stall.

Antimatter Cells are also in the plain `nerotech:fusion_fuels` tag, so any pack rule that matches
"fusion fuel" matches them.

## Singularity Vault

Bulk storage for **one** item type: up to `singularityVaultCapacity` (default **1 000 000**) of it in
a single block. The bulk is a **virtual store** — an item type plus a count — so a full vault costs
two numbers on disk, not fifteen thousand stacks.

### Using it by hand

- **Right-click with a stack** — deposits it. The first thing you put in **sets the vault's type**;
  after that it refuses anything else rather than voiding it.
- **Crouch-right-click** — takes a stack back out.
- **Right-click empty-handed** — reports what is stored and how full it is on the actionbar.
- A fully drained vault **forgets its type**, so you can re-assign it without breaking it.

### Using it with automation

The vault presents a **two-slot facade** to pipes, hoppers and the
[Robotic Arm](Automation.md#robotic-arm):

- **Slot 0 — input**: drained into the store every tick while the type matches.
- **Slot 1 — output**: kept topped up with a full stack of the stored item, so extraction never
  stalls no matter how deep the store is.

A **comparator** reads fill as a fraction of capacity (with a floor of 1 while anything at all is
held), which makes "vault nearly full" a one-redstone-line signal.

It uses **no power** — it is not on the energy surface at all, and a
[Grid Controller](Power-Generation.md) leaves it alone.

> **Empty it before you break it.** The stored bulk does **not** drop with the block; only the vault
> itself does. Pull the contents out first.

**Craft:** 4 Void Crystal (`#c:gems/void_crystal`) + 4 Obsidian around an **Antimatter Cell** — which
is what puts the vault behind both multiblocks.

## Transmutation

The collider also gains two lossy dust conversions at this tier — Copper Dust → Iron Dust and Iron
Dust → Gold Dust. They are 1 : 1 by item count and pay their "loss" in a full collider operation
each. See [Particle Collider → Transmutation](Particle-Collider.md#transmutation).

## See also

- [Particle Collider](Particle-Collider.md)
- [Fusion Reactor](Fusion-Reactor.md)
- [Automation & Handoff](Automation.md)
- [Thermal System](Thermal-System.md)
- [Home](Home.md)
