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

## Auto Crafter

Assembles a **vanilla crafting recipe** from its 3×3 grid into the output slot, powered by NE. It's
**demand-driven**: it only attempts a craft on a batched interval and only when ingredients are present,
so it never per-tick-scans. Pipes fill the grid and pull the output via the item capability.

## Item Sorter

A **directional filter sorter**: an input slot plus four horizontal faces, each with a filter and an
output buffer. Items matching a face's filter are routed into that face's buffer, where a pipe (or
NeroLogistics) on that side extracts them — sorting by *where* it's pulled, no neighbour scanning.
Routing runs on a batched interval. (v1 filters match by item; tag-based filters are a planned
enhancement.)

## See also

- [Tier-1 Machines](Machines.md)
- [Advanced Tier](Advanced-Tier.md)
- [Home](Home.md)
