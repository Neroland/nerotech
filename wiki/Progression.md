# Standalone Progression

NeroTech is fully playable with **only Neroland Core installed** — no Nerospace, no other tech mod.
Every item has a survival recipe built from vanilla materials, and nothing in NeroTech is locked
behind a progression gate: placing your first machine *opens* Core's `industrial_power` milestone,
but no machine, GUI or recipe ever *checks* a gate. Advanced content is paced purely by the
materials it costs.

This page is the full path from an empty inventory to antimatter, in order.

## 1. First components (crafting table, no power)

You need these before any machine. All three can be made by hand from vanilla ingots:

| Component | Hand recipe | Better later |
| --- | --- | --- |
| Machine Frame | 8 Iron Ingots in a ring (`III` / `I I` / `III`) | Fabricator: 1 Iron Ingot → 1 Frame |
| Nero Coil | Copper Ingot + Iron Ingot + Copper Ingot in a row | Fabricator: 1 Copper Dust → 2 Coils |
| Circuit Board | Redstone + Gold Ingot + Redstone in a row | Fabricator: 1 Gold Dust → 2 Boards |

Coils and boards also have hand recipes that take dust in place of the ingot (Copper Dust + Iron
Ingot + Copper Dust; Redstone + Gold Dust + Redstone) — the same one-for-one, but half the metal.

## 2. First power

Build any of these; each needs only a frame, the components above and vanilla blocks:

| Generator | Needs to run |
| --- | --- |
| [Nero Generator](Machines.md#nero-generator) | Coal, charcoal, coal block, blaze rod or dried kelp block |
| [Solar Array](Machines.md#solar-array) | Open sky and daylight |
| [Wind Turbine](Power-Generation.md) | Open sky; more output the higher you build it |
| [Geothermal Generator](Power-Generation.md) | Lava or magma blocks in the 3×3 directly beneath it |
| [Bio Generator](Power-Generation.md) | Dried kelp blocks |

The Nero Generator is the usual first pick: craft it, feed it coal, and it pushes power into every
machine touching it.

## 3. Processing and the dust economy

- **Ore Processor** — ore or raw metal → 2 dust; raw metal blocks → 18 dust. Smelt the dust for
  double ingots. See [Making Dust](Dusts.md).
- **Fabricator** — turns dust into two components apiece, and iron into frames for an eighth of
  the hand cost.
- **Electrolyzer → Chemical Processor** — water becomes hydrogen and oxygen; the oxygen washes raw
  metal into 3 dust. The hydrogen fuels the Gas Turbine. See [Fluids & Gases](Fluids-and-Gases.md).

## 4. Consequences and support

Heat, pollution, automation and analytics all use Tier-1 parts only: Radiator, Coolant Pump,
Scrubber (its Dirty Filters recycle into Iron Dust), Battery Bank, Grid Controller, Wireless Node,
Auto Crafter, Item Sorter, Conveyor Belt, Robotic Arm, Analytics Terminal, Configurator and the
upgrade modules.

## 5. Starsteel and Void Crystal — the Particle Accelerator

The advanced tier costs **Starsteel Ingots** and **Void Crystal**. Without Nerospace, the
[Particle Accelerator](Particle-Collider.md) makes them from vanilla materials:

| Collision | Product | Then |
| --- | --- | --- |
| Netherite Scrap + Iron Dust | Starsteel Dust | Smelt → Starsteel Ingot |
| Echo Shard + Amethyst Shard | Void Crystal Dust | Smelt → Void Crystal |

The Accelerator Controller and its Guide Coils are built from Tier-1 parts and a pair of diamonds,
so the ring is buildable before you own any Starsteel. Both collisions need 3,000 J — the smallest ring
that reaches it is a regular octagon about 15 × 15 blocks across.

What to gather: **Netherite Scrap** comes from Ancient Debris in the Nether; **Echo Shards** from
Ancient City chests in the Deep Dark; **Amethyst Shards** from amethyst geodes. Echo Shards are
finite in a world, so plan Void Crystal use — it feeds Fusion Cells, Containment Glass and the
Singularity Vault.

With Nerospace installed, space mining is simply a faster road to the same two materials.

## 6. Advanced tier and fusion

With Starsteel and Void Crystal in hand: Advanced Ore Processor, Advanced Fabricator (Void Crystal
→ Fusion Cell), Remediator, Fusion Casing and Containment Glass, the
[Fusion Reactor](Fusion-Reactor.md), and the Plasma and Stellar Cells.

> **Watch the heat.** Meltdown is on by default. A 3×3×3 shell runs safely uncooled, but larger
> shells need a Coolant Pump and Radiators on the controller (or the Eco preset) before you light
> them. See [Fusion Reactor](Fusion-Reactor.md).

## 7. Exotic endgame

Two Stellar Cells collided at 12,000 J (a 29 × 29 ring) make an **Antimatter Cell** — tier-4 fusion
fuel and the heart of the **Singularity Vault**. See [Exotic Endgame](Exotic-Endgame.md).

## See also

- [Making Dust](Dusts.md)
- [Tier-1 Machines](Machines.md)
- [Advanced Tier](Advanced-Tier.md)
- [Tech Guide](Tech-Guide.md)
- [Home](Home.md)
