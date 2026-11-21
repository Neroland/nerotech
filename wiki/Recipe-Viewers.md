# Recipe Viewers (JEI & EMI)

NeroTech's machine recipes show up in both **JEI** and **EMI**. Both are optional: NeroTech runs the same
without either.

## What you get

One page per machine recipe type, with the machines that run it as the page's workstations:

| Page | Recipe type | Workstations |
| --- | --- | --- |
| Ore Processing | `nerotech:ore_processing` | Ore Processor, Advanced Ore Processor |
| Fabricating | `nerotech:fabricating` | Fabricator |
| Advanced Fabricating | `nerotech:advanced_fabricating` | Advanced Fabricator |
| Particle Accelerator | `nerotech:collider` | Accelerator Controller (two inputs, plus the minimum collision energy) |
| Chemical Processing | `nerotech:chemical_processing` | Chemical Processor |
| Alloying | `nerotech:alloying` | Plasma Crucible |
| Foundry Casting | `nerotech:foundry_casting` | Blast Foundry |
| Pressing | `nerotech:pressing` | Plate Press |
| Drawing | `nerotech:drawing` | Filament Spooler |
| Centrifuging | `nerotech:centrifuging` | Centrifuge |
| Cryo Distilling | `nerotech:cryo_distilling` | Cryo Distiller |
| Condensing | `nerotech:condensing` | Atmospheric Condenser |
| Reacting | `nerotech:reacting` | Reaction Vat |
| Component Forging | `nerotech:component_forging` | Component Forge |
| Etching | `nerotech:etching` | Laser Etcher |
| Crystal Growing | `nerotech:crystal_growing` | Crystal Grower |

Vanilla crafting and smelting recipes need nothing from NeroTech; the viewers read those themselves.

## Datapack recipes

All sixteen types are datapack-driven, so a pack's added or overridden recipes appear on these pages too.

Since Minecraft 1.21.2 the client no longer receives the full recipe list, so NeroTech sends its machine
recipes to the client itself. Where each build gets them:

| Build | Recipe pages |
| --- | --- |
| Minecraft 1.21.1, every loader | Complete (the game still sends every recipe) |
| NeoForge, 1.21.4 and later | Complete |
| Forge, 1.21.4 and later | Complete since 0.8.0 (earlier releases sent nothing on Forge, so the pages were empty) |
| Fabric, 1.21.9 and later (the 1.21.10, 1.21.11 and 26.x builds) | Complete |
| **Fabric 1.21.4, 1.21.5 and 1.21.8** | **Can be empty.** Fabric has no way to send these recipes to the client on those versions. The machines work; only the viewer pages are missing. |

A server running an older NeroTech that does not send the recipes leaves the pages empty.

## EMI on Minecraft 26.x

Official EMI has no Minecraft 26.x release yet. NeroTech's EMI support is built against the community
**EMI Unofficial Port (Unstable)** on CurseForge, which keeps EMI's normal plugin API:

- **NeoForge and Fabric:** supported on 26.1.2, 26.2 and 26.3.
- **Forge:** the port has no Forge build, so there is no EMI on Forge.

With both JEI and EMI installed, EMI takes over the recipe screens and shows NeroTech's pages once, from
NeroTech's own EMI plugin rather than through EMI's JEI bridge.

## For contributors

- JEI plugin: `common/.../compat/jei/NeroTechJeiPlugin.java`. EMI plugin:
  `common/.../compat/emi/NeroTechEmiPlugin.java`. Both use only the viewer's loader-agnostic API, so each
  lives in `common/` and compiles into every cell.
- EMI pins are the port's CurseForge file ids (`emi_file_<loader>_<mc>` in `gradle.properties`), resolved
  through CurseMaven. The Forge cells compile against the NeoForge file.
- Dev clients load JEI by default. Add `-PwithEmi` to a `runClient` to load EMI as well.
