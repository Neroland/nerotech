# Particle Collider

The **Particle Collider** is NeroTech's endgame multiblock and its **standalone** route to
space-grade dusts. It slams a cheap catalyst apart and reassembles the pieces as **Starsteel Dust**
or **Void Crystal Dust** — the materials the [Advanced Tier](Advanced-Tier.md) is built from —
without needing any other mod installed.

It is deliberately *expensive*, not *fast*. See [Balance](#balance) below.

## Structure

A **horizontal hollow square ring** of **Accelerator Coils** at the Collider Core's own Y level.
The Collider Core takes exactly one ring position (an edge or a corner), every other ring position
is a coil — **corners included** — and every position **inside** the ring must be **air**.

Two sizes are valid: **5×5** and **7×7** (outer edge).

```text
5x5 ring (top-down, y = the core's level)      7x7 ring (top-down)

  C C C C C                                      C C C C C C C
  C . . . C     C = Accelerator Coil             C . . . . . C
  C . . . C     # = Collider Core                C . . . . . C
  C . . . C     . = air (must stay empty)        C . . . . . C
  C C # C C                                      C . . . . . C
                                                 C . . . . . C
                                                 C C C # C C C
```

The core may sit anywhere on the perimeter — the diagrams just show one convenient spot.

An **unformed** collider is fully inert: it draws no power, emits nothing, and its Analytics status
reads *Structure unformed*. Break a single coil and the ring collapses mid-run — the partial
operation is lost. Re-forming the ring restarts it. Validation is event-driven (a neighbour change
re-checks immediately) plus a cheap cadence, so a running collider never scans its ring every tick.

### Ring size = throughput

| Ring | Throughput | Operation time (default config) |
| ---- | ---------- | ------------------------------- |
| 5×5  | 1×         | 1200 ticks (60 s)               |
| 7×7  | 2×         | 600 ticks (30 s)                |

Speed Modules and the [Overclock Presets](Overclock-Presets.md) stack on top of the ring bonus, as
on every other machine.

## Running it

- **Slots**: one catalyst input, one dust output, plus the usual four upgrade slots.
- **Sides**: items in/out and energy in on the standard `PROCESSOR` preset — see
  [Side Config & the Configurator](Side-Config-and-Configurator.md).
- **Energy**: `colliderNePerTick` (default **400 NE/tick**) for the whole operation. At the 5×5
  default that is **480 000 NE per dust**. Bring a Fusion Reactor or a very large solar/battery bank.
- **Heat**: **3×** the base per-tick heat of a Tier-1 processor, so plan coolant (see
  [Thermal System](Thermal-System.md)) — an overheated collider throttles until it cools.
- **Pollution**: emits at the normal per-operation rate; the [Scrubber](Pollution-and-Mitigation.md)
  handles it.

## Recipes

### Collider recipes (`nerotech:collider`)

Datapack-driven like every NeroTech machine recipe — override or extend them file-by-file.

| Catalyst                    | Output                              |
| --------------------------- | ----------------------------------- |
| `minecraft:netherite_scrap` | `nerolandcore:starsteel_dust` ×1    |
| `minecraft:echo_shard`      | `nerolandcore:void_crystal_dust` ×1 |
| `nerotech:stellar_cell`     | `nerotech:antimatter_cell` ×1       |
| `nerotech:copper_dust`      | `nerotech:iron_dust` ×1             |
| `nerotech:iron_dust`        | `nerotech:gold_dust` ×1             |

Both dusts smelt into their material (Starsteel Ingot / Void Crystal) through Neroland Core's own
blasting/smelting recipes, so the collider feeds straight into the advanced tier.

> The catalysts are **single items** because the machine recipe format is one ingredient in, one
> result out. The balance is carried by the energy and time cost, not by a long ingredient list.

### Antimatter

The collider is the **only** source of the **Antimatter Cell** — feed it a Stellar Cell and it comes
back out as tier-4 fusion fuel. See [Exotic Endgame](Exotic-Endgame.md) for what the reactor does
with it (and what it does to the reactor).

### Transmutation

Two of the recipes above turn one metal dust into another. They are **1 : 1 by item count** but very
far from free: every conversion costs a full collider operation — 480 000 NE on a 5×5 ring at the
defaults — so the "loss" is paid in power and time rather than in items.

- **Copper Dust → Iron Dust** — turn a surplus of the cheap metal into the useful one.
- **Iron Dust → Gold Dust** — the classic transmutation, and the most expensive gold in the game.

Both are ordinary datapack recipes: a pack that wants a different ladder (or a yield penalty) can
override the files directly.

### Crafting

Deliberately **vanilla + NeroTech only** — no Starsteel or Void Crystal anywhere, because the
collider has to be buildable *before* you have any.

**Accelerator Coil** ×2

```text
C I C     C = Nero Coil
I F I     I = Iron Ingot
C I C     F = Machine Frame
```

**Collider Core** ×1

```text
B C B     B = Circuit Board
D F D     C = Nero Coil
B C B     D = Diamond, F = Machine Frame
```

A 5×5 ring needs 24 coils (12 crafts); a 7×7 ring needs 48.

## Balance

The collider is the **standalone / automation** route, never the cheapest one:

- **With Nerospace installed**, mining meteors and planet ores is the *faster* source of Starsteel
  and Void Crystal by a wide margin. Keep mining.
- **Without it**, the collider is the *only* source — the advanced tier and the Fusion Reactor are
  now reachable on Earth alone, at the cost of an enormous power budget.
- Even where both exist, the collider still earns its place as the **fully automatable** source: it
  is a machine in a pipe network, not an expedition.

Servers can retune it entirely from config: `colliderNePerTick` and `colliderOperationTicks`.

## See also

- [Advanced Tier](Advanced-Tier.md)
- [Fusion Reactor](Fusion-Reactor.md)
- [Materials & Components](Materials-and-Components.md)
- [Thermal System](Thermal-System.md)
- [Home](Home.md)
