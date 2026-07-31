# NeroTech

> Part of the [Neroland](../neroland-mc-ecosystem) sci-fi Minecraft mod ecosystem, built on **Neroland Core**.

**Status:** beta — version `0.1.0-beta.1` (feature-complete, pending runtime verification). 13 machines including the scalable multiblock Fusion Reactor (3³/5³/7³ shells), Scrubber/Remediator pollution mitigation, Auto Crafter with recipe preview + grid lock, Item Sorter, Analytics Terminal and the Tech Guide pedestal; a full thermal model, regional pollution with opt-in per-player attribution (POPIA/GDPR-compliant), overclock presets, side-config tooling, a NeroLink companion-app module, and a complete content pipeline (models, textures, recipes, advancements, lang). **Standalone-first:** no progression gate ever locks a machine — pacing is via recipes/materials, and pairing with other Neroland mods enhances rather than restricts. NeroTech opens Core's `INDUSTRIAL_POWER` milestone for the wider ecosystem.

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

## Planning docs

Design, feature and dependency docs for this mod live in the umbrella repo under
[`../neroland-mc-ecosystem/nerotech`](../neroland-mc-ecosystem/nerotech).
