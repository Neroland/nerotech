# Making Dust

Dust is the backbone of NeroTech's material economy: it doubles (or triples) your ore, feeds the
Fabricator's components, and is the route to Starsteel and Void Crystal without leaving Earth. This
page gathers every way to make it in one place.

| Dust | Item ID | Tag |
| --- | --- | --- |
| Iron Dust | `nerotech:iron_dust` | `c:dusts/iron` |
| Copper Dust | `nerotech:copper_dust` | `c:dusts/copper` |
| Gold Dust | `nerotech:gold_dust` | `c:dusts/gold` |
| Starsteel Dust | `nerolandcore:starsteel_dust` | `c:dusts/starsteel` (Neroland Core item) |
| Void Crystal Dust | `nerolandcore:void_crystal_dust` | `c:dusts/void_crystal` (Neroland Core item) |

There is **no crafting-table recipe** for any dust — it always comes out of a powered machine.

## Quick start: your first dust

1. **Machine Frame** — 8 iron ingots in a ring (`III` / `I I` / `III`).
2. **Ore Processor** — craft it around the frame:

   ```
   I P I      I = Iron Ingot     P = Piston
   R F R      R = Redstone       F = Machine Frame
   I I I
   ```

3. **Power** — place a [Nero Generator](Machines.md#nero-generator) (burns coal, charcoal, etc.) or a
   [Solar Array](Machines.md#solar-array) next to the Ore Processor. Generators push Nero Energy (NE)
   into adjacent machines automatically.
4. **Process** — put ore or raw metal in the input slot. Each operation gives **2 dust**:

| Input | Output |
| --- | --- |
| Iron Ore, Deepslate Iron Ore or Raw Iron | 2 Iron Dust |
| Copper Ore, Deepslate Copper Ore or Raw Copper | 2 Copper Dust |
| Gold Ore, Deepslate Gold Ore or Raw Gold | 2 Gold Dust |

5. **Smelt** — every Iron, Copper and Gold Dust smelts (200 ticks) or blasts (100 ticks) into its
   vanilla ingot, at vanilla XP. So *1 raw ore → 2 dust → 2 ingots*: double yield.

The machine stalls cleanly when it runs out of power — no lost items. If nothing is happening, check
the energy bar first.

## Better yields

### Chemical Processor — 3 dust per raw metal

Washes raw metal with oxygen for a third more dust. Each operation also burns
`chemicalProcessorGasPerOp` (default 250 mB) of oxygen, which you make by splitting water in the
[Electrolyzer](Fluids-and-Gases.md).

```
G B G      G = Glass
G F G      B = Circuit Board
G B G      F = Machine Frame
```

| Input | Output |
| --- | --- |
| Raw Iron | 3 Iron Dust |
| Raw Copper | 3 Copper Dust |
| Raw Gold | 3 Gold Dust |

It only accepts **raw metal**, not ore blocks — mine with a non-Silk-Touch pickaxe (Fortune helps
too). With an empty oxygen tank it reports **Starved** and uses no power. See
[Fluids & Gases](Fluids-and-Gases.md#chemical-processor) for piping oxygen in.

### Advanced Ore Processor — bonus dust and bulk blocks

An Ore Processor surrounded by eight Starsteel Ingots. It adds `advancedOreProcessorYieldBonus` extra
dust to every operation and is the place to run the **bulk raw-block** recipes:

| Input | Output |
| --- | --- |
| Block of Raw Iron | 18 Iron Dust |
| Block of Raw Copper | 18 Copper Dust |
| Block of Raw Gold | 18 Gold Dust |

A raw block is nine raw metal, so 18 dust matches the per-item rate while taking a ninth of the
operations. See [Advanced Tier](Advanced-Tier.md#advanced-ore-processor).

## Recycling: Dirty Filters

The [Scrubber](Pollution-and-Mitigation.md) fouls its Filter Cartridges into **Dirty Filters**. Put a
Dirty Filter through the Ore Processor to recover **1 Iron Dust** — the iron you spent making it.

## Particle Accelerator: Starsteel, Void Crystal and transmutation

The [Particle Accelerator](Particle-Collider.md) collides two items at speed. It is the standalone
(no-Nerospace) way to get the two space-grade dusts, and it can turn one metal dust into another.

| Collision | Product | Minimum energy |
| --- | --- | --- |
| Netherite Scrap + Iron Dust | Starsteel Dust | 3,000 J |
| Echo Shard + Amethyst Shard | Void Crystal Dust | 3,000 J |
| Copper Dust + Copper Dust | Iron Dust | 800 J |
| Iron Dust + Iron Dust | Gold Dust | 1,500 J |

Bigger rings reach higher energies — see the accelerator page for ring sizes. Starsteel Dust and Void
Crystal Dust smelt into Starsteel Ingots and Void Crystal through Neroland Core's recipes. With
Nerospace installed, mining meteors and planet ores is the faster road to those materials; the
accelerator is the slow route that needs no other mod.

Transmutation is two-for-one, so it is a way to use up surplus copper or iron, not a cheap source of
gold.

## What dust is used for

- **Ingots** — smelt or blast back into vanilla ingots (the doubling loop above).
- **Components** — in the [Fabricator](Machines.md#fabricator), 1 Copper Dust → 2 Nero Coils and
  1 Gold Dust → 2 Circuit Boards.
- **Advanced tier** — Starsteel and Void Crystal gate every [advanced machine](Advanced-Tier.md) and
  the Fusion Cell.
- **Other mods** — the `c:dusts/*` tags let any mod that processes dust by tag accept NeroTech's.

## For pack makers

Every dust recipe is a datapack JSON, so you can add, replace or remove them:

| Machine | Recipe type |
| --- | --- |
| Ore Processor / Advanced Ore Processor | `nerotech:ore_processing` |
| Chemical Processor | `nerotech:chemical_processing` |
| Particle Accelerator | `nerotech:collider` |

```json
{
  "type": "nerotech:ore_processing",
  "ingredient": ["minecraft:iron_ore", "minecraft:deepslate_iron_ore", "minecraft:raw_iron"],
  "result": { "id": "nerotech:iron_dust", "count": 2 }
}
```

## See also

- [Tier-1 Machines](Machines.md)
- [Materials & Components](Materials-and-Components.md)
- [Fluids & Gases](Fluids-and-Gases.md)
- [Particle Accelerator](Particle-Collider.md)
- [Home](Home.md)
