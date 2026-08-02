# Automation & Handoff

NeroTech's automation machines turn a manual production line into a self-running one — and, crucially,
they expose a **standard item-handoff surface** so a logistics mod (NeroLogistics) or any pipe/hopper
can move items in and out **with no dependency on NeroTech**.

## The handoff surface

Every NeroTech machine exposes its inventory through the **standard loader item capability** (NeoForge
`Capabilities.Item.BLOCK`, Forge `ITEM_HANDLER`, Fabric `ItemStorage.SIDED`), wired **sided**:

- **Inputs are insertable, outputs are extractable**, and the internal upgrade-module slots are never
  exposed to automation.
- Machines are also discoverable through Core's `c:` tags.

So a generic capability/tag client routes NeroTech production endpoints generically — exactly what
NeroLogistics needs. NeroTech does **not** implement cross-block routing itself; endpoint discovery and
routing live in the logistics layer (Core deliberately doesn't own item lookup).

## Side configuration

Every NeroTech machine now carries Neroland Core's **universal side-configuration** system. Open a
machine and use the **Side Config tab** in the top-right of its GUI: a flattened cube of the six faces,
each of which you set per channel (Power / Item) to **disabled, input, output** or **both**. Faces are
**relative to the machine's facing**, so a configuration travels with the block when it is rotated or
re-placed. The tab also has **auto-eject / auto-input** toggles, and **reset / copy / paste** (the
in-GUI clipboard pastes between machines of the same type; the
[Configurator wrench](Side-Config-and-Configurator.md) pastes across types).

Side config is **server-authoritative**: clicks send intents to the server, which validates and syncs
the result back. A face's capability is **gated by its mode** — a disabled face exposes nothing, an
input face only accepts, an output face only emits — so pipes connect exactly where you allow them.
Since 0.1.0-beta.1 you can also cycle, read and copy-paste side configurations in-world with the
[Configurator wrench](Side-Config-and-Configurator.md), and the Item Sorter shows its port modes as
live-tinted collars.

Each machine ships with a sensible default layout:

- **Generators** (Nero Generator, Fusion Reactor, Solar Array) — **Power out** on every face; generators
  also accept **fuel in** on every face. Power auto-ejects to adjacent storage/machines by default.
- **Processors** (Ore Processor, Fabricator and their Advanced tiers, Auto Crafter) — **items in** on
  every face except the **bottom**, which is the **output**; **Power in** on every face.
- **Item Sorter** — **input** on top and bottom; each of the four horizontal faces **outputs** its own
  filtered buffer (the directional routing below). Faces can't be set to both-ways here.

## Auto Crafter

Assembles a **vanilla crafting recipe** from its 3×3 grid into the output slot, powered by NE. It's
**demand-driven**: it only attempts a craft on a batched interval and only when ingredients are present,
so it never per-tick-scans. Pipes fill the grid and pull the output via the item capability.

**Recipe preview:** the output well shows a ghost of what the current grid would craft (server-matched,
kept current even while unpowered), so you can lay out a recipe and see the result before any power or
crafting happens.

**Grid lock:** the **Lock** button (title bar) snapshots the current grid as a per-slot template. While
locked, each grid slot only accepts its template item — in the GUI *and* through pipes/hoppers — so
automation keeps refilling exactly the right ingredients and can never scramble the recipe, even when a
slot runs completely empty (its ghost stays visible in the emptied slot). Press **Unlock** to clear the
template. The template is world/block state only — no player data.

## Item Sorter

A **directional filter sorter**: an input slot plus four horizontal faces, each with a filter and an
output buffer. Items matching a face's filter are routed into that face's buffer, where a pipe (or
NeroLogistics) on that side extracts them — sorting by *where* it's pulled, no neighbour scanning.
Routing runs on a batched interval. (v1 filters match by item; tag-based filters are a planned
enhancement.) The per-face exposure is driven by the [Side Config tab](#side-configuration): each
horizontal face outputs its own filtered buffer, and the top/bottom default to input.

## Conveyor Belt

The cheapest automation in the mod, and the only NeroTech block with **no block entity at all**: a flat
plate, four pixels high, that pushes **item entities** riding it along the direction it faces (about
0.15 blocks per tick, capped — a chain of belts never accelerates an item past belt speed).

- **No power, no GUI, no filter.** It applies motion and nothing else: no pickup, no merging, no
  inserting into inventories.
- **Lines and corners form naturally** — each belt only ever pushes along its own facing, so a corner is
  just two belts facing different ways. There is no belt "network" and no path-finding.
- Only **item entities** are moved; players and mobs walk over a belt normally.
- Feed the end of a run into a hopper, a machine face or a **Robotic Arm** to get items back into an
  inventory.

Placed facing **away** from you, so a run builds in the direction you are walking.

**Craft:** 3 Iron Dust over 2 Nero Coils around Redstone → **6 belts**. This is the mod's main Iron Dust
sink.

## Robotic Arm

A one-block item mover: once a second it lifts up to `roboticArmStackPerMove` (default 8) items out of
the container **behind** it and puts them into the container **in front** of it, spending
`roboticArmNePerMove` (default 4) NE **per item moved**.

- Both ends are addressed through the same standard container surface a hopper uses, through the face
  pointing at the arm — so **machine side configs, furnace sidedness and NeroTech's own per-face
  routing are all honoured**.
- **One filter slot** in the GUI: while it holds an item, only matching items move. The filter is a
  template — it is never consumed, and no face ever exposes it to automation.
- Speed modules raise the items-per-pass; Efficiency modules and the Eco preset cut the NE per item.
- Never a world scan: two direct neighbour lookups on a 20-tick cadence, phase-spread across arms.

**Craft:** Machine Frame, 2 Circuit Boards, 2 Nero Coils and 2 Iron Dust.

## See also

- [Tier-1 Machines](Machines.md)
- [Advanced Tier](Advanced-Tier.md)
- [Exotic Endgame](Exotic-Endgame.md)
- [Side Config & the Configurator](Side-Config-and-Configurator.md)
- [Home](Home.md)
