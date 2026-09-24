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

Vanilla crafting and smelting recipes need nothing from NeroTech; the viewers read those themselves.

## Datapack recipes

All five types are datapack-driven, so a pack's added or overridden recipes appear on these pages too.
Since Minecraft 26.x the client no longer receives the full recipe list, so NeroTech opts its machine
recipes into the server's recipe sync on every loader. A server running an older NeroTech that does not
send them leaves the pages empty.

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
