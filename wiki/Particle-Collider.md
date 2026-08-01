# Particle Accelerator

The **Particle Accelerator** is NeroTech's endgame build and its **standalone** route to space-grade
dusts. It is not a multiblock: you lay **Accelerator Guide Coils** wherever you like, in whatever
shape you like, and the **Accelerator Controller** fires a particle round the loop, winds it up to
speed, and smashes it into a second particle.

The whole progression axis is **ring geometry**. Bigger rings hold faster particles; faster particles
carry more collision energy; richer recipes demand more energy. There is no tier list of coils to
craft — there is only how much room you are willing to give the beam.

> Mechanic inspired by [Oritech](https://modrinth.com/mod/oritech)'s particle accelerator. NeroTech's
> implementation is clean-room — no code or assets are shared.

## Building a ring

Three rules, and then you are done:

1. Every **Accelerator Guide Coil** sits at the **same Y level** as the Accelerator Controller.
2. The beam leaves the controller in the direction the controller **faces**, and marches in a
   straight line — one of the eight compass headings — until it hits a guide, up to
   `acceleratorMaxGap` (default **16**) blocks away. Guides do not need to touch; the space between
   them can be anything.
3. **Right-click a guide** (empty hand, or with the [Configurator](Side-Config-and-Configurator.md))
   to cycle its bend: **straight → 45° left → 45° right**. The actionbar names the new setting.

Chain the bends so the beam comes back into the controller and you have a **closed loop**. Anything
else is an **open** line and the particle would simply fly off the end — the controller says so.

### Your first octagon

Eight vertices, all bends set **right**, the controller facing **east**. Straight runs of 4 blocks
alternate with diagonal runs of 3.

```text
z=0    . . . C . . . G . . .      C = Accelerator Controller (facing east)
z=1    . . . . . . . . . . .      G = Accelerator Guide Coil (bend = right)
z=2    . . . . . . . . . . .      . = anything at all (the beam ignores it)
z=3    G . . . . . . . . . G
z=4    . . . . . . . . . . .      Footprint: 11 x 11
z=5    . . . . . . . . . . .      Guides needed: 7
z=6    . . . . . . . . . . .      Straight runs: 4 blocks
z=7    G . . . . . . . . . G      Diagonal runs: 3 steps (4.24 blocks)
z=8    . . . . . . . . . . .
z=9    . . . . . . . . . . .
z=10   . . . G . . . G . . .
```

Follow it round: east 4 → right → south-east 3 → right → south 4 → right → south-west 3 → right →
west 4 → right → north-west 3 → right → north 4 → right → north-east 3 → back into the controller.

Nothing between the guides matters. Build the loop through your base, around a mountain, over a lake
— the beam only cares about the guides it meets.

### Reading the ring

Once a controller closes a loop, every guide on it **lights up**: a cyan arrow appears on the coil's
top face showing the direction the beam *leaves* that coil on, with the 45° kink of its bend drawn
in. Walk the ring and you can read the beam's whole route off the floor.

A coil with **no arrow** is not part of any closed beam line — it was missed by the march, or the
line it sits on never came home. That makes the arrows your first debugging tool: build the loop,
look for the dark coil, fix the bend that points away from it. The arrows refresh whenever the
controller re-traces (within about five seconds of a change, or instantly when something next to the
controller changes), so a bend you have just cycled by hand may read stale for a moment.

## The three rules the beam obeys

| Rule          | What it says                                                        | Config                                                 |
| ------------- | ------------------------------------------------------------------- | ------------------------------------------------------ |
| **Gap rule**  | A stretch `L` is survivable at speed `s` only when `L ≤ 4 + 0.12·s` | `acceleratorMinGapAllowance`, `acceleratorGapPerSpeed` |
| **Bend rule** | A 45° turn survives at most `20 × L`, `L` = the stretch before it   | `acceleratorBendSpeedBase`                             |
| **Energy**    | A collision carries `E = 0.5 · s² · 0.5` joules                     | `acceleratorEnergyScale`                               |

Read together, those three are the whole game:

- **Injection.** The controller injects at the slowest speed the loop's *longest* stretch tolerates
  (the gap rule inverted), floored at `acceleratorLaunchSpeed` (default 10). Wide rings start fast.
- **Top speed.** The *shortest* stretch that ends in a bend caps the loop, at `20 ×` its length. Push
  past it and the particle **crashes** — a puff of smoke, a heat spike, and a lost particle.
- **Losing power.** Every guide passed costs `acceleratorNePerGuide` (default 50 NE) and adds
  `acceleratorBoostPerGuide` (default +2) to the speed. With an empty buffer the particle **coasts**
  and bleeds `acceleratorDragPerGuide` (default 0.5) per guide instead — coast long enough and it
  drops below the gap rule and **fizzles** on the long stretch.

### The ring-size ladder

Regular octagons, at the default config. "Runs" are the straight / diagonal stretches between guides.

| Runs (straight / diagonal) | Footprint | Shortest bend stretch | Top speed | Max collision energy |
| -------------------------- | --------- | --------------------- | --------- | -------------------- |
| 4 / 3                      | 11 × 11   | 4.00                  | 80        | **1,600 J**          |
| 6 / 4                      | 15 × 15   | 5.66                  | 113       | **3,198 J**          |
| 8 / 6                      | 21 × 21   | 8.00                  | 160       | **6,400 J**          |
| 12 / 8                     | 29 × 29   | 11.31                 | 226       | **12,803 J**         |

Every one of them costs the same 7 guides. The only thing you are spending is **space**.

## Running it

- **Slots**: injection (left), collision target (middle), output (right), plus the usual four upgrade
  slots.
- **Sides**: items in/out and energy in on the standard `PROCESSOR` preset — see
  [Side Config & the Configurator](Side-Config-and-Configurator.md).
- **Starting a run**: automatic. With a closed loop, an item in the injection slot and NE in the
  buffer, the particle launches. There is no button.
- **Colliding**: put the partner item in the collision slot. Every lap the beam passes the
  controller it looks for a matching recipe and checks the current energy against the recipe's floor.
  Under it, the particles **miss** and the beam keeps lapping — just wait. Over it, both are
  consumed, the product lands in the output slot and the controller flashes.
- **Readouts**: the GUI shows beam speed (m/s), current collision energy (J), the guide count and
  whether the loop closes, and a status line naming exactly what went wrong.
- **Heat**: **3×** the base per-guide heat of a Tier-1 processor, plus a spike on a crash — plan
  coolant (see [Thermal System](Thermal-System.md)). Too hot and it refuses to inject.
- **Pollution**: emits at the normal per-operation rate; the
  [Scrubber](Pollution-and-Mitigation.md) handles it.

You can watch the particle: it draws a streak of white sparks along the beam line, so a lap is
visible from the ground.

## Recipes

### Collision recipes (`nerotech:collider`)

Datapack-driven like every NeroTech machine recipe — override or extend them file-by-file. Both
inputs are consumed one at a time, and the pair is **order-free** (either item may be the injected
one). `min_energy` is in the same joules the GUI reads.

| Collision                   | Product                          | Min energy   | Smallest octagon |
| --------------------------- | -------------------------------- | ------------ | ---------------- |
| Copper Dust + Copper Dust   | `nerotech:iron_dust`             | **800 J**    | 4 / 3            |
| Iron Dust + Iron Dust       | `nerotech:gold_dust`             | **1,500 J**  | 4 / 3            |
| Netherite Scrap + Iron Dust | `nerolandcore:starsteel_dust`    | **3,000 J**  | 6 / 4            |
| Echo Shard + Amethyst Shard | `nerolandcore:void_crystal_dust` | **3,000 J**  | 6 / 4            |
| Stellar Cell + Stellar Cell | `nerotech:antimatter_cell`       | **12,000 J** | 12 / 8           |

Both dusts smelt into their material (Starsteel Ingot / Void Crystal) through Neroland Core's own
blasting/smelting recipes, so the accelerator feeds straight into the advanced tier.

A datapack recipe looks like this:

```json
{
  "type": "nerotech:collider",
  "input_a": "minecraft:netherite_scrap",
  "input_b": "nerotech:iron_dust",
  "min_energy": 3000,
  "result": { "id": "nerolandcore:starsteel_dust" }
}
```

### Antimatter

The accelerator is the **only** source of the **Antimatter Cell**, and it is the one recipe that
demands a genuinely large ring — 12,000 J means a 29 × 29 loop. See
[Exotic Endgame](Exotic-Endgame.md) for what the reactor does with it (and what it does to the
reactor).

### Transmutation

Two of the recipes above turn one metal dust into another, **two in for one out**, paid for in the
power and the real estate a run costs.

- **Copper Dust ×2 → Iron Dust** — turn a surplus of the cheap metal into the useful one.
- **Iron Dust ×2 → Gold Dust** — the classic transmutation, and the most expensive gold in the game.

### Crafting

Deliberately **vanilla + NeroTech only** — no Starsteel or Void Crystal anywhere, because the
accelerator has to be buildable *before* you have any.

**Accelerator Guide Coil** ×2

```text
C I C     C = Nero Coil
I F I     I = Iron Ingot
C I C     F = Machine Frame
```

**Accelerator Controller** ×1

```text
B C B     B = Circuit Board
D F D     C = Nero Coil
B C B     D = Diamond, F = Machine Frame
```

Seven guides is four crafts, whatever size ring you build.

## Balance

The accelerator is the **standalone / automation** route, never the cheapest one:

- **With Nerospace installed**, mining meteors and planet ores is the *faster* source of Starsteel
  and Void Crystal by a wide margin. Keep mining.
- **Without it**, the accelerator is the *only* source — the advanced tier and the Fusion Reactor are
  reachable on Earth alone, at the cost of land and a power budget.
- Even where both exist, it earns its place as the **fully automatable** source: pipes in, pipes out.

Servers can retune the whole system from config: `acceleratorMaxGap`, `acceleratorMaxGuides`,
`acceleratorTickScale`, `acceleratorLaunchSpeed`, `acceleratorBoostPerGuide`, `acceleratorNePerGuide`,
`acceleratorDragPerGuide`, `acceleratorMinGapAllowance`, `acceleratorGapPerSpeed`,
`acceleratorBendSpeedBase` and `acceleratorEnergyScale`.

## Troubleshooting

| The GUI says                           | What to do                                                            |
| -------------------------------------- | --------------------------------------------------------------------- |
| *No guides found*                      | Nothing within 16 blocks along the controller's facing, at its own Y. |
| *Beam line does not close*             | Follow the arrows round; the first unlit coil is where the line dies. |
| *Particle lost — guides too far apart* | A stretch outran the gap rule. Shorten it, or check you have power.   |
| *Particle crashed — bend too sharp*    | The loop hit its top speed. Lengthen the stretch before that bend.    |
| *Output slot full*                     | The collision succeeded but had nowhere to put the product.           |

## See also

- [Advanced Tier](Advanced-Tier.md)
- [Exotic Endgame](Exotic-Endgame.md)
- [Fusion Reactor](Fusion-Reactor.md)
- [Materials & Components](Materials-and-Components.md)
- [Thermal System](Thermal-System.md)
- [Home](Home.md)
