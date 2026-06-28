# Materials & Components

NeroTech's Tier-1 crafting components and processing intermediates. These are the
building blocks the Tier-1 machines (Stage 2) are crafted from and produce.

NeroTech does **not** add its own base metals — the shared alloys (Nero Alloy,
Starsteel, Void Crystal, Plasma Glass) live in **Neroland Core**. NeroTech only adds
the industrial components and the Earth-metal processing dusts.

All items appear in the shared **Neroland** creative tab (contributed via Core's
`CoreCreativeTab`), alongside Core's materials and every other Nero mod's content.

## Machine components

| Item | Role | Tag |
| --- | --- | --- |
| Machine Frame | Universal machine casing — the crafting base shared by every Tier-1 machine. | `nerotech:machine_components` |
| Circuit Board | Basic electronic component — the "logic" ingredient in machine recipes. | `nerotech:machine_components` |
| Nero Coil | Wound conductive coil — the "power" ingredient in generator/motor recipes. | `nerotech:machine_components` |

`nerotech:machine_components` is an internal grouping tag used by NeroTech's own
recipe checks; it is not part of the cross-mod `c:` convention.

## Processing dusts

The Tier-1 Ore Processor (Stage 2) turns raw Earth ores into doubled refined output;
these dusts are the intermediate step (ore → dust → ingot). They carry the
cross-mod `c:` tags so Mekanism, Create, and other processing chains interoperate
with NeroTech without a hard dependency.

| Item | Tag |
| --- | --- |
| Iron Dust | `c:dusts/iron` (and `c:dusts`) |
| Copper Dust | `c:dusts/copper` (and `c:dusts`) |
| Gold Dust | `c:dusts/gold` (and `c:dusts`) |

NeroTech's `c:dusts` entries merge with Core's (Nero Alloy, Starsteel, Void Crystal)
and any other mod's, so `#c:dusts/iron` resolves to whichever iron dust is present.

## Tags & datapacks

All tags are hand-authored under `common/src/main/resources/data/` (the multiloader
runs no datagen). The `c:` tags are datapack-overridable, so packs can fold NeroTech
dusts into their own processing equivalences.

## See also

- [Home](Home.md)
- [Build & contributor context](../AGENTS.md)
- Neroland Core materials and the `c:` / `neroland:` tag conventions (see Core's
  `docs/TAGS-AND-DATAPACKS.md`).
