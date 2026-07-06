# Changelog

All notable changes to **NeroTech** are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

_Nothing yet._

## [0.0.1-alpha.3] - 2026-07-04

Development alpha — machine side configuration and test coverage.

### Added

**Universal machine side configuration**

- All NeroTech machines adopted **Neroland Core 1.3.0's side-config system**: per-face,
  per-channel routing (item / fluid / gas / energy) with presets, capability gating, and
  the reusable side-config widget integrated into the machine screens.

**Test coverage**

- New tests for the advancement tree and for **pollution data erasure** (the per-player
  opt-in pollution attribution correctly purges through Core's `PlayerDataErasure` hook —
  POPIA/GDPR).

### Changed

- **Neroland Core is now resolved from GitHub Packages** in CI and fresh clones
  (Maven Local remains the local-dev path).
- Bumped loader/API dependency pins within the current Minecraft line.

### Build & CI

- Modrinth version environment metadata is patched automatically after publish.
- `/forge/versions` is now ignored.

[0.0.1-alpha.3]: https://github.com/Neroland/nerotech/releases/tag/v0.0.1-alpha.3

## [0.0.1-alpha.2] - 2026-06-29

First tagged development alpha of **NeroTech** — the industry/tech member of the Neroland
lineup, multi-loader from day one (**NeoForge, Forge, Fabric** × **Minecraft 26.1.2 / 26.2**),
built on **Neroland Core**.

### Added

**Machines & energy**

- Cross-loader registration scaffolding and the **Tier-1 machine set** with Nero energy
  and GUIs.
- **Advanced-tier machines** building on the Tier-1 set.
- **Auto Crafter** and **Item Sorter** machines.
- Machine UI and slot-layout refinements; the **NeroTech creative tab**; the orbit machine
  lock was dropped.

**Heat & pollution**

- **Heat** and **regional pollution** systems — pollution is tracked regionally;
  any per-player attribution is strictly **opt-in**.

**Compliance & telemetry**

- **GDPR/POPIA-compliant crash telemetry via Sentry** — opt-out, scrubbed, no personal
  data; player data routes through Neroland Core's shared erasure hook.

**Project infrastructure**

- Multiloader repo scaffold (Stonecutter version axis), CI automation with per-loader
  publishing and a Discord release notification, the **Neroland Core** dependency across
  all loaders, logos, directional block models, docs, and the wiki skeleton.

[0.0.1-alpha.2]: https://github.com/Neroland/nerotech/releases/tag/v0.0.1-alpha.2
