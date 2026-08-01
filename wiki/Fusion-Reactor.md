# Fusion Reactor (Multiblock)

Since 0.1.0-beta.1 the Fusion Reactor is a **scalable multiblock**: a hollow cube shell of Fusion
Casing and Fusion Containment Glass with the reactor controller set into one wall. The controller
block alone is **inert** — it does nothing until a valid shell is formed around it, and a reactor
placed before the beta stays dormant until you build it a shell.

## Building a shell

Three sizes are valid — **3×3×3**, **5×5×5** and **7×7×7** (outer dimensions):

1. Build a hollow cube of **Fusion Casing** (crafted from Starsteel); any wall block may be swapped
   for **Fusion Containment Glass** if you want to see the core — the two mix freely.
2. Set the **Fusion Reactor** (controller) into the **centre of one wall face**, facing outward. It
   replaces that casing position and counts as part of the shell.
3. Keep the **interior strictly empty** — any block inside (including fluids) invalidates the
   structure.

The reactor checks its structure automatically (largest shell first, so a 7×7×7 wins if several are
possible). When the shell forms, the core lights up: a plasma torus spins at the interior centre,
scaled to the shell and visible through containment glass. Breaking any shell block unforms the
reactor — it goes dark and stops.

## Fuel tiers

Fuel is matched by **datapack tags**, so packs can add their own: anything in
`nerotech:fusion_fuels` burns; membership of `nerotech:fusion_fuel/tier2`, `/tier3` or `/tier4`
raises its tier. The shell size gates the tier:

| Tier | Fuel | Made from | Minimum shell |
| --- | --- | --- | --- |
| 1 | **Fusion Cell** (any `nerotech:fusion_fuels` entry not tagged tier 2/3/4) | Starsteel + Void Crystal (or the Advanced Fabricator) | 3×3×3 |
| 2 | **Plasma Cell** (`nerotech:fusion_fuel/tier2`) | Starsteel | 5×5×5 |
| 3 | **Stellar Cell** (`nerotech:fusion_fuel/tier3`) | Void Crystal + Starsteel | 7×7×7 |
| 4 | **Antimatter Cell** (`nerotech:fusion_fuel/tier4`) | two Stellar Cells collided at 12,000 J in the [Particle Accelerator](Particle-Collider.md) | 7×7×7 **only** |

Tier 4 is the one exception to "tier ≤ shell tier": the **maximal 7×7×7 shell reaches one tier
further than its own**, which is exactly why antimatter is burnable there and nowhere else. Burning
it also adds a flat **+2** to the reactor's heat rate on top of the shell scaling — the meltdown risk
is the price of the longest burn in the mod. See [Exotic Endgame](Exotic-Endgame.md).

Higher tiers burn far longer per cell (`fusionFuelTier1BurnTicks` / `Tier2` / `Tier3` / `Tier4`,
the last defaulting to twice tier 3), and bigger
shells multiply output: the base rate (`fusionReactorNePerTick`) is scaled **1× / 4× / 12×** for the
3/5/7 shells (`fusionSizeOutputPermille`). A 7×7×7 on Stellar Cells is the strongest generator in
the mod.

## Heat, meltdown and containment breach

Power scales — so does the danger:

- **Bigger shells run hotter** (roughly ×4/×5/×6 heat rate by size), and an
  [Overdrive preset](Overclock-Presets.md) doubles heat on top. The core's glow shifts from cyan
  toward red as heat climbs, and it **strobes** past the throttle threshold — that is your last
  warning.
- **Meltdown**: at max heat an unmanaged reactor melts down destructively, with a blast scaled to
  the shell (radius 4/6/8, centred inside the shell). Servers can disable this
  (`fusionReactorMeltdownEnabled=false`), in which case the reactor just stalls until it cools.
- **Containment breach**: breaking a shell block **while the reactor is burning** loses the entire
  fuel charge and vents a burst of [pollution](Pollution-and-Mitigation.md) into the region. Shut
  the reactor down before resizing or moving it.

Cooling a big reactor is a genuine [thermal-system](Thermal-System.md) exercise: ambient, coolant
blocks and spacing all matter.

## See also

- [Advanced Tier](Advanced-Tier.md)
- [Thermal System](Thermal-System.md)
- [Pollution & Mitigation](Pollution-and-Mitigation.md)
- [Overclock Presets](Overclock-Presets.md)
- [Home](Home.md)
