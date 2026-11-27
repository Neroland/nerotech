# Project context for AI coding agents — nerotech

> Modrinth is temporarily being removed from the upload and is no longer being referenced. Avoid making references to modrinth.

> `CLAUDE.md` and `AGENTS.md` are kept identical; update both together.

## The mod

- **NeroTech** — part of the Neroland sci-fi Minecraft mod ecosystem, built on **Neroland Core**.
  This repository carries the **implemented Phase-3 content** (machines, heat, pollution, and
  milestone records); add shared content under `common/` and wire it through each loader entry point.
- **Standalone-first.** Milestone records exist only to feed quests, trophies and the Tech Guide; they
  never lock a machine, recipe or item. Do not add use-locks.
- Mod id: **`nerotech`** (matches the registry namespace + every loader manifest). Package root:
  `za.co.neroland.nerotech`. Author: **Neroland**.
- Version: **0.8.0** "Offworld Industry" (requires Neroland Core **1.19.0**; `nerolandcore_version` in `gradle.properties`).
- Targets **MC 1.20 through 26.3** (`mc_versions`) on **NeoForge (1.20.2+), MinecraftForge/Forge (all but
  1.20.5), and Fabric** → **54 cells**, the same matrix as Neroland Core (the source is written for 26.x; older lines use `//?` blocks and the
  `legacyRenames` list in `stonecutter.gradle`). **Java 25** for 26.x, **Java 21** for 1.20.5-1.21.x, **Java 17** for 1.20-1.20.4. Mappings = official Mojang names (26.x ships de-obfuscated; no Parchment).

## Working rules

- **Keep responses concise and direct** — minimal verbosity, minimal formatting.
- **POPIA & GDPR**: keep all logging/telemetry/scripts compliant — only public version strings, never
  personal data; minimise data, set retention limits, support export/erasure and opt-out.
- **NEVER commit or push automatically.** Leave changes **staged**; the developer reviews and commits
  with native git (the source of truth).
- **Use relative paths only** — never hard-code machine-specific absolute paths in committed files.
- **Never run commands against production databases.** Treat any DB command as illustrative.

## Repo layout — flattened cross-loader build

- **The build IS the repo root.** `common/` (shared source spliced into every node), `neoforge/`
  (ModDevGradle), `forge/` (ForgeGradle), `fabric/` (Fabric Loom). Root build files: `settings.gradle`,
  `stonecutter.gradle` (the REAL root build script — Stonecutter repoints `buildFileName` here; the root
  `build.gradle` is inert), `gradle.properties`, `gradlew`, `gradle/`.
- **Version/loader axis = Stonecutter.** Each loader×MC is a real node `:<loader>:<mc>` — 54 in all
  (`:{fabric,neoforge,forge}:<mc>` for every `mc_versions` entry, minus NeoForge 1.20/1.20.1 and Forge 1.20.5). `common` is
  NOT a node — its source is spliced via `rootProject.ext.commonJava` / `commonResources`. Dependency pins
  live in `gradle.properties` as `*_version_<mc>` keys; `mc_versions=1.20,1.20.1,1.20.2,1.20.3,1.20.4,1.20.5,1.20.6,1.21.1,1.21.3,1.21.4,1.21.5,1.21.8,1.21.10,1.21.11,26.1,26.1.1,26.1.2,26.2,26.3`.
- **Version-specific code in `common/`.** Non-active nodes run `common/` through Stonecutter (`stonecutterProcessCommon`), so shared code uses the same `//? if >=26.3 {` blocks as the loader `src/` trees. Keep the files in the vcsVersion state, and never put a `*/` inside a disabled block. Datapack files whose format differs by version go in `common/src/main/resources-<mc>/`, which is merged over `common/src/main/resources` for every node at or above `<mc>` (`mergeCommonResources`).

## Build & verify

- **Never run plain `./gradlew build`** — the root loader modules are vestigial and fail. Build the
  Stonecutter cells by name. The full set is **54 cells** plus ecjCheck. The 1.21.x/26.x core of it (26.x first, then 1.21.11 down
  to 1.21.1; add the 1.20.x, 1.21.3, 26.1 and 26.1.1 cells the same way):

  ```text
  :fabric:26.1.2:build :neoforge:26.1.2:build :forge:26.1.2:build
  :fabric:26.2:build :neoforge:26.2:build :forge:26.2:build
  :fabric:26.3:build :neoforge:26.3:build :forge:26.3:build
  :fabric:1.21.11:build :neoforge:1.21.11:build :forge:1.21.11:build
  :fabric:1.21.10:build :neoforge:1.21.10:build :forge:1.21.10:build
  :fabric:1.21.8:build :neoforge:1.21.8:build :forge:1.21.8:build
  :fabric:1.21.5:build :neoforge:1.21.5:build :forge:1.21.5:build
  :fabric:1.21.4:build :neoforge:1.21.4:build :forge:1.21.4:build
  :fabric:1.21.1:build :neoforge:1.21.1:build :forge:1.21.1:build
  :fabric:26.3:ecjCheck
  ```

- A single cell: `./gradlew :fabric:26.3:build`.
- Static analysis: `./gradlew :fabric:26.3:ecjCheck` (the VS Code Problems panel, via `tools/ecj.prefs`).
  The task only FAILS on errors.
- A Cowork agent sandbox cannot decompile Minecraft — run builds natively (or via the local gradle MCP)
  on the developer's machine.
- **Verify the cells build before marking a task done.** Never sign off on an uncompiled change.

## Conventions (cross-loader)

- **Resources are HAND-AUTHORED in `common/src/main/resources`** — the multiloader does not run datagen.
  Validate JSON after edits.
- **Platform seams via ServiceLoader (no Architectury).** Put loader-agnostic code in `common/`; ship one
  impl per loader plus a `META-INF/services` entry. Keep `common/` free of `net.neoforged.*` /
  `net.fabricmc.*` / `net.minecraftforge.*` imports.
- Loader entry points: `NeroTechFabric` (+ `NeroTechFabricClient`), `NeroTechForge`,
  `NeroTechNeoForge` — each calls `NeroTechCommon.init()` during construction.
- **Public add-on API = `za.co.neroland.nerotech.api`** (`PowerMachine`, `MachineTypeRegistry`,
  `MachineFailureEvents`, `PlanetApi` incl. `atmosphereAt`, `Atmosphere`, `WirelessCharging`): stable within a 0.x minor, NeroPower depends on it — a breaking
  change there bumps the minor and goes in `CHANGELOG.md`. Everything else is internal. Capability wiring
  on every loader reads `MachineTypeRegistry` (seeded first in `NeroTechCommon.init()`), not hand lists.
- Each loader cell publishes to Maven (`za.co.neroland.nerotech:nerotech-<loader>-<mc>:<version>`,
  `./gradlew publishToMavenLocal` / GitHub Packages) so add-ons resolve NeroTech like NeroTech resolves Core.
- NeoForge/Forge debug tasks use `-PnerotechDebug`; Fabric Loom honours Gradle `--debug-jvm`.
- NeroPower also compiles against the machine base classes, `MachineMenu`/`MachineScreen`, `NeroTechConfig`,
  `ModBlocks`, `ModDataComponents`, `ConfiguratorItem`/`ConfiguratorState` and `PollutionManager`: treat those
  as API too (additive changes only).

## Package map (0.8.0 additions)

- `machine/<family>/` — `metallurgy`, `foundry`, `chemistry`, `components`, `heat`, `extraction`, `fusion`,
  `utility`, each with a `<Family>Content` registering its blocks, block entities, menus and items through
  `registry/FamilyRegistry`; shared `machine/ProcessingMachineBlockEntity` (data-driven process recipes,
  readout, alarms, transient `lastOperator`), `machine/tank/` (Process Tank columns, fluid-or-gas handler) and
  `machine/multiblock/` (`StructureShape`, `MultiblockValidator`, `MultiblockController`; the Fusion Reactor,
  Blast Foundry and Deep Core Drill shapes).
- `recipe/ProcessRecipe` + the eleven 0.8.0 recipe types in `registry/ModRecipeTypes`; client sync on every
  loader (Forge `ForgeRecipeSync`; Fabric only on >=1.21.9). `RecipeGraphTest` (`recipe/graph/`) proves every
  item is reachable standalone and offworld at 3–5x.
- `deposit/` — virtual deposits (`DepositSeed`, `DepositTable`, `DepositState` behind Core `SavedDataRecovery`,
  `DepositService`); tables in `data/nerotech/nerotech_deposits/`.
- `item/tools/` — the seven tools; `upgrade/` — Heat Sink and Scrubber modules.
- `link/` — link schema 2 (`nearby`, `catalogue`, handle-based preset action, alarm events); `map/` — the
  NeroMaps pollution layer; `command/gallery/` — the gallery plant and tour; `command/TickBudget` —
  `/nerotech budget`.
- `client/GaugeMath` + `GaugeTooltips` — gauge hover tooltips on every screen.
- Compliance tests in `common/src/test/.../compliance/` (no new player-keyed data; Core erasure conformance).

## IDE (VS Code) run & debug

- Workspace: **`nerotech.code-workspace`** (single-root `"."`). Import the Stonecutter nodes as **static
  Eclipse projects**: `./gradlew eclipse` (live Buildship/Loom import is disabled —
  `java.import.gradle.enabled=false`). Re-run `./gradlew eclipse` after dependency changes, then reload
  VS Code. Per-node Eclipse project names are `nerotech-<loader>-<mc>`.
- **Run/Debug** a cell from `tasks.json` / `launch.json`.

## Wiki — keep `wiki/` updated

- This mod has its own **dedicated wiki** in `wiki/` at the repo root: the player- and
  contributor-facing docs for NeroTech (features, blocks/items, machines, progression, recipes, FAQ).
- **Whenever you add, change, or remove a feature, update `wiki/` in the same change** — treat the
  wiki as part of "done"; code without a matching wiki update is incomplete.
- One page per topic; keep `wiki/Home.md` as the index that links every page, with relative links
  between pages. Validate Markdown via the gradle MCP `markdown_check` (honours `.markdownlint.json`).
- The wiki is **per-mod** — document only NeroTech here; cross-mod / ecosystem concepts belong in the
  relevant other mod's own wiki.

## DO NOT

- Commit or push automatically — leave changes staged for the developer.
- Hard-code absolute machine paths in committed files.
- Add loader-specific code to `common/` — use the platform seams.

## Translation workflow

English is the source language for new and changed text. Community translations are intended to be contributed through Weblate; other-language translations are not required when implementing a feature. Preserve existing translations. This supersedes older instructions that require translating every change.
