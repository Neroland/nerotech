# NeroTech

> Part of the Neroland sci-fi Minecraft mod ecosystem, built on **Neroland Core**.

Requires Neroland Core 1.23.0 or newer (below 2.0). NeroPower builds on the public add-on API in `za.co.neroland.nerotech.api`. NeroTech includes 31 blocks, 26 of them block-entity machines, including the scalable multiblock Fusion Reactor (3³/5³/7³ shells), Scrubber/Remediator pollution mitigation, Auto Crafter with recipe preview + grid lock, Item Sorter, Analytics Terminal and the Tech Guide pedestal; a full thermal model, regional pollution with opt-in per-player attribution (POPIA/GDPR-compliant), overclock presets, side-config tooling, a NeroLink companion-app module, and a complete content pipeline (models, textures, recipes, advancements, lang). **Standalone-first:** no progression gate ever locks a machine — pacing is via recipes/materials, and pairing with other Neroland mods enhances rather than restricts. NeroTech opens Core's `INDUSTRIAL_POWER` milestone for the wider ecosystem.

The 0.1.0-beta.1 tech tree also covers:

- **Particle accelerator** — free-form rings of **Accelerator Guide Coils** traced by an **Accelerator Controller**, with two-input, energy-floored collision recipes.
- **Fluid & gas chain** — **Electrolyzer**, **Gas Turbine**, **Chemical Processor**, plus the **Coolant Pump** / **Radiator** cooling loop.
- **Power tier** — **Wind Turbine**, **Geothermal Generator**, **Bio Generator**, **Solar Array**, **Battery Bank**, **Grid Controller** and **Wireless Power Node**.
- **Automation** — **Conveyor Belt** and **Robotic Arm**.
- **Exotic endgame** — the accelerator-only **Antimatter Cell** (tier-4 fusion fuel) and the **Singularity Vault**.

## Optional integrations

- **NeroQuests** — NeroTech ships a quest-book chapter, *Industrial Record* (`nerotech:industrial_record`,
  19 quests), as plain data inside its own jar. It appears when NeroQuests is installed and is inert
  otherwise; NeroTech has no dependency on NeroQuests. Personal milestones (fusion online, first
  collision, remediated, grid scale) are player-scope Neroland Core record gates; world events ride
  Core's threshold channels (`nerotech:pollution`, `nerotech:pollution_decay`, `nerotech:machine_failure`).
  See [`wiki/NeroQuests-Integration.md`](wiki/NeroQuests-Integration.md).
- **NeroEconomy** — optional per-operation machine charges (off by default); see
  [`wiki/Operating-Charges.md`](wiki/Operating-Charges.md).
- **NeroLink** — the companion-app module; see [`wiki/Companion-App.md`](wiki/Companion-App.md).

## Build targets

- **Minecraft:** 1.21.1, 1.21.4, 1.21.5, 1.21.8 (also 1.21.7), 1.21.10 (also 1.21.9), 1.21.11, 26.1.2, 26.2 and 26.3
- **Loaders:** Fabric, Forge and NeoForge for the versions above.
- **Minecraft 1.20.5** on Fabric and **1.20.6** on Fabric, Forge and NeoForge.
- **Java:** 21 for 1.20.5 through 1.21.x, and 25 for 26.x.
- The Tech Guide covers every build. JEI recipe pages are available from 1.21.1, with EMI pages on 1.21.1 and 26.x.
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
./gradlew :neoforge:26.1.2:build :neoforge:26.2:build :neoforge:26.3:build \
          :forge:26.1.2:build :forge:26.2:build :forge:26.3:build \
          :fabric:26.1.2:build :fabric:26.2:build :fabric:26.3:build   # the 26.x cells
# 1.21.x cells: :<loader>:1.21.1:build ... :<loader>:1.21.11:build (see mc_versions in gradle.properties)
```

See [`AGENTS.md`](AGENTS.md) / [`CLAUDE.md`](CLAUDE.md) for agent and contributor context.

## Privacy

NeroTech sends anonymous crash reports (stack trace + mod/game versions only — never IPs, usernames,
UUIDs, or world data) via Sentry on EU servers. It is **on by default** and **opt-out**: set
`telemetryEnabled=false` in `config/nerotech.properties` (client-local, takes effect on next launch).
Optional per-player pollution attribution is off by default. Full disclosure: [`PRIVACY.md`](PRIVACY.md).
