# NeroTech

> Part of the Neroland sci-fi Minecraft mod ecosystem, built on **Neroland Core**.

**Status:** beta — version `0.1.0-beta.1` (feature-complete, pending runtime verification). 31 blocks, 26 of them block-entity machines, including the scalable multiblock Fusion Reactor (3³/5³/7³ shells), Scrubber/Remediator pollution mitigation, Auto Crafter with recipe preview + grid lock, Item Sorter, Analytics Terminal and the Tech Guide pedestal; a full thermal model, regional pollution with opt-in per-player attribution (POPIA/GDPR-compliant), overclock presets, side-config tooling, a NeroLink companion-app module, and a complete content pipeline (models, textures, recipes, advancements, lang). **Standalone-first:** no progression gate ever locks a machine — pacing is via recipes/materials, and pairing with other Neroland mods enhances rather than restricts. NeroTech opens Core's `INDUSTRIAL_POWER` milestone for the wider ecosystem.

The 0.1.0-beta.1 tech tree also covers:

- **Particle accelerator** — free-form rings of **Accelerator Guide Coils** traced by an **Accelerator Controller**, with two-input, energy-floored collision recipes.
- **Fluid & gas chain** — **Electrolyzer**, **Gas Turbine**, **Chemical Processor**, plus the **Coolant Pump** / **Radiator** cooling loop.
- **Power tier** — **Wind Turbine**, **Geothermal Generator**, **Bio Generator**, **Solar Array**, **Battery Bank**, **Grid Controller** and **Wireless Power Node**.
- **Automation** — **Conveyor Belt** and **Robotic Arm**.
- **Exotic endgame** — the accelerator-only **Antimatter Cell** (tier-4 fusion fuel) and the **Singularity Vault**.

## Build targets

- **Minecraft:** 26.1.2 and 26.2
- **Loaders:** NeoForge, MinecraftForge/Forge, Fabric (the "6 cells")
- **Java:** 25
- Mod id: `nerotech` · package `za.co.neroland.nerotech`

## Layout

The build is the repo root, with a flattened cross-loader structure driven by Stonecutter:

- `common/` — shared, loader-agnostic source spliced into every loader node
- `fabric/` — Fabric Loom
- `forge/` — ForgeGradle
- `neoforge/` — ModDevGradle
- `stonecutter.gradle` — the real root build script; `build.gradle` is intentionally inert

## Building

```sh
./gradlew :fabric:26.2:build          # one cell
./gradlew :neoforge:26.1.2:build :neoforge:26.2:build \
          :forge:26.1.2:build :forge:26.2:build \
          :fabric:26.1.2:build :fabric:26.2:build   # all six
```

See [`AGENTS.md`](AGENTS.md) / [`CLAUDE.md`](CLAUDE.md) for agent and contributor context.
