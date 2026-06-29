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
re-placed. The tab also has **auto-eject / auto-input** toggles, and **reset / copy / paste** (paste
only applies between machines of the same type).

Side config is **server-authoritative**: clicks send intents to the server, which validates and syncs
the result back. A face's capability is **gated by its mode** — a disabled face exposes nothing, an
input face only accepts, an output face only emits — so pipes connect exactly where you allow them.

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

## Item Sorter

A **directional filter sorter**: an input slot plus four horizontal faces, each with a filter and an
output buffer. Items matching a face's filter are routed into that face's buffer, where a pipe (or
NeroLogistics) on that side extracts them — sorting by *where* it's pulled, no neighbour scanning.
Routing runs on a batched interval. (v1 filters match by item; tag-based filters are a planned
enhancement.) The per-face exposure is driven by the [Side Config tab](#side-configuration): each
horizontal face outputs its own filtered buffer, and the top/bottom default to input.

## See also

- [Tier-1 Machines](Machines.md)
- [Advanced Tier](Advanced-Tier.md)
- [Home](Home.md)
