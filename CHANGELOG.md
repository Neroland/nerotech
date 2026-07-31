# Changelog

All notable changes to **NeroTech** are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- **Auto Crafter recipe preview** — the output well shows a server-matched ghost of what the
  current grid would craft, kept current even while the machine is unpowered.
- **Auto Crafter grid lock** — a Lock/Unlock toggle snapshots the grid as a per-slot template;
  while locked, each grid slot only accepts its template item through the GUI *and* through
  pipes/hoppers, so automation can never scramble the recipe. Template state is world/block
  state only — no player data (POPIA/GDPR).
- **Solar Array niche tooltip** — clarifies it as NeroTech's basic single-block panel versus
  Nerospace's tiered, poolable Solar Panels.
- **Auto-assign workflow** — assigns the maintainer to newly opened issues and PRs, gated on a
  collaborator write-access check so it never fails silently.

**NeroLink companion-app integration**

- NeroTech now registers a full **link module** with Neroland Core's link API, so the **NeroLink**
  companion app auto-serves a NeroTech section. Read sections: `pollution` (your own attributed
  total, only when attribution is on and you have not opted out — otherwise an opt-out note),
  `guide` (your Tech Guide progress), and a public `wiki` section that renders the wiki pages
  in-app. All personal data is own-data-only, scoped to your UUID (POPIA/GDPR).
- **Actions**: `set_pollution_attribution` (your own privacy opt-out, stored as a UUID-keyed
  preference layered on the server's global attribution flag and wired into the shared data-erasure
  hook) and `set_machine_preset` (remote overclock preset — the server re-checks online + ownership,
  and only works on machines whose owner was recorded, i.e. placed with attribution on).
- **Live events/alerts** (module `nerotech`): a WARN alert + event when your own attributed
  pollution crosses the alert threshold; a CRITICAL alert + world `meltdown` broadcast on a Fusion
  Reactor meltdown or containment breach.

**Progression gates removed by design**

- The briefly-added `orbit_fabrication` / `fusion_online` use-lock gates are **gone** (never
  released). They required `nerolandcore:reached_orbit`, which only Nerospace/NeroQuests can open, so
  standalone play permanently bricked the orbit-tier machine GUIs — the exact failure the earlier
  `reached_orbit` use-lock removal fixed. NeroTech is standalone-first: **no progression gate ever
  locks a machine**; pacing is via recipes/materials, and cross-mod pairing enhances rather than
  restricts. NeroTech still opens Core's `industrial_power` milestone on first machine placement for
  other mods to read.

### Fixed

- **Scrubber / Remediator unpowerable on Fabric and NeoForge** — both pollution machines (and the
  Scrubber's filter slots) were missing from the loader capability registrations, so generators could
  never push NE to them and pipes could not automate the Scrubber on those loaders. Forge was
  unaffected (blanket instanceof attach).

**Docs**

- New wiki page **[Companion App](wiki/Companion-App.md)** documenting the exposed sections, actions,
  events and the opt-in privacy posture; linked from the wiki Home index.

## [0.1.0-beta.1] - 2026-07-11

The "big push" from alpha to a feature-complete beta: every machine gets a real visual
identity, the Fusion Reactor becomes a scalable multiblock, heat becomes a full thermal
model, pollution gets its mitigation tech, and every GUI gains analytics, presets and
side-config tooling.

### Added

**Scalable Fusion Reactor multiblock**

- The Fusion Reactor is now a **multiblock**: build a hollow 3×3×3, 5×5×5 or 7×7×7 shell
  from the new **Fusion Casing** and **Fusion Containment Glass** blocks, with the reactor
  controller set into the centre of one wall facing outward and a strictly empty interior.
  The controller is **inert until the shell is formed** — a spinning plasma torus, visible
  through the containment glass, telegraphs a live core.
- **Three fuel tiers**, all datapack tags: **Fusion Cell** (tier 1, any shell) → new
  **Plasma Cell** (Starsteel, needs 5×5×5+) → new **Stellar Cell** (Void Crystal +
  Starsteel, needs the 7×7×7). Per-tier burn times (`fusionFuelTier1BurnTicks`..`Tier3`)
  and per-size output multipliers (default 1× / 4× / 12×, `fusionSizeOutputPermille`) are
  config.
- **Scaled risk**: bigger shells run hotter (×4/×5/×6 heat rate) and melt down harder
  (blast radius 4/6/8 at the shell's interior centre, still admin-disableable via
  `fusionReactorMeltdownEnabled`). Breaking the shell **mid-burn** is a **containment
  breach**: the charge is lost and a pollution burst vents into the region.

**Full thermal model**

- Machines now **conduct heat to adjacent machines** — dense builds share heat — and relax
  toward a **local ambient** derived from the dimension (`thermalAmbientByDimension`, e.g.
  the Nether runs hot) plus a biome flavour term (deserts hot, snowy biomes cold).
- Coolant blocks (water, ice, snow) still help, and are now cached alongside the neighbour
  links — no per-tick scanning anywhere in the model.
- Six tuning keys: `thermalConductivityPermille`, `thermalEnvLossPermille`,
  `thermalExchangeIntervalTicks`, `thermalAmbientDefault`, `thermalAmbientByDimension`,
  `thermalBiomeScale`.

**Pollution mitigation tech**

- **Scrubber** — powered machine that cleans its own pollution region at full rate plus the
  8 surrounding regions at a configurable fraction (default 25%). It consumes **Filter
  Cartridges** (craft 4 from paper + iron), which foul into **Dirty Filters**; reprocess
  those in an Ore Processor for an iron-dust refund.
- **Remediator** — heavy-duty, slotless area cleaner: no consumables, steep NE cost per
  operation, works its own region down. Gated behind Starsteel.
- Regional pollution can now publish **threshold events** through Neroland Core 1.7.0
  (`pollutionEventThreshold`) — dormant until a listener mod (NeroEvents) exists.

**Production analytics**

- Every machine GUI gains an **Analytics tab**: a colour-coded status line naming the
  limiting cause (running / idle / starved / blocked / throttled / no energy / unformed),
  live heat, energy, pollution and efficiency readouts, and two 60-second sparklines
  (heat, energy).
- New **Analytics Terminal** block — a passive console that scans nearby machines
  (`analyticsTerminalRadius`, default 16) and shows a dashboard: counts by status, the
  hottest machine, and the nearest machines with their causes.

**Overclock presets**

- Every processing machine and generator gets a preset selector: **Eco** (−25% speed, −50%
  energy/heat/pollution), **Balanced**, or **Overdrive** (+50% speed, +100%
  energy/heat/pollution). Presets stack with upgrade modules and persist on the machine —
  an overdriven Fusion Reactor courts its own meltdown threshold. (Auto Crafter and Item
  Sorter are one-shot machines and take no preset.)

**Configurator wrench**

- New tool for working with side configuration in-world: **click** a machine face to cycle
  its mode, **sneak-click** to read the full layout, and **copy/paste** a machine's entire
  side configuration onto others of the same type.

**Tech Guide**

- New **Tech Guide pedestal** (and a hand-held datapad fallback): an in-game guided tour of
  7 chapters and 19 steps — First Power → Processing → Consequences → Mitigation →
  Automation → Analytics → Fusion — with progress driven entirely by advancements
  (including 7 new ones). A loaded pedestal projects your next incomplete step as a
  hologram.

**Datapack machine recipes**

- Ore Processor, Fabricator and their Advanced tiers now run on **real datapack recipe
  types** (`nerotech:ore_processing`, `nerotech:fabricating`,
  `nerotech:advanced_fabricating`) instead of fixed in-code tables. Packs can add, replace
  or remove machine recipes like any other recipe JSON; baseline recipes are tag-matched
  where cross-mod interop wants it.

**A complete visual identity**

- Every machine re-modelled and re-textured in a **32× teal/plasma** art language, with
  animated block-entity renderers: spinning generator turbine, sun-tracking solar deck,
  counter-rotating crusher drums, traversing fabricator arms, the fusion torus, Auto
  Crafter hologram + press, live-tinted Item Sorter ports, scrubber fan with a visibly
  fouling cartridge, remediator boom sweep, and a shimmering Analytics Terminal.
- Machine glow shifts from cyan toward warning orange/red as heat climbs, and the Fusion
  Reactor strobes past its throttle threshold — heat is readable at a glance.
- New client-only `renderAnimationsEnabled` config: turn it off on low-end machines for
  static parked frames.
- New **`/nerotech gallery`** command (creative/OP): lays out every block and item for
  screenshots.

**Optional Nerospace planet interop**

- With **Nerospace 1.0.0-beta.7+** installed, solar output and thermal ambient
  automatically follow planet traits (airless worlds boost solar, hot/cold planets shift
  both). Without Nerospace, the per-dimension config keys apply; NeroTech remains fully
  standalone.

**Quality of life**

- NeroTech items now carry Neroland Core's coloured **inventory highlights**: upgrade
  modules (green), the Configurator (violet), fuels and filters (teal).

### Changed

- **Neroland Core 1.7.0 or newer is required** (was 1.3.x in the alpha). Update Core before
  updating NeroTech — older Core versions will refuse to load with this release.
- Machine processing recipes moved from in-code tables to datapack JSON (see Added); mod
  behaviour is unchanged out of the box, but packs that expected the alpha's fixed tables
  should switch to overriding the recipe files.
- Side configuration is now synced to clients (the Item Sorter shows its port modes
  in-world) and machine GUIs open reliably from any distance/angle.

### Fixed

- **Machine blocks now drop themselves when mined.** All machines were missing from
  `minecraft:mineable/pickaxe`, so no tool counted as correct and they dropped nothing.
- **GUI gauges now show live values.** The base machine menu never registered its container
  data, so energy/heat/progress gauges silently read zeros — latent since the first alpha.
- Machine models no longer black out the faces of adjacent blocks (non-cube machine shapes
  now correctly mark themselves non-occluding).

### Migration notes (from 0.0.1-alpha.x)

- **Fusion Reactors placed in the alpha become inert** until a valid casing shell is built
  around them (the reactor block itself is preserved; nothing is deleted). Build a 3×3×3
  shell around an existing reactor to bring it back online.
- Requires **Neroland Core >= 1.7.0** (breaking floor bump — see Changed).
- **Nerospace >= 1.0.0-beta.7** is optional and only needed for planet-trait modifiers.
- Energized Power interop (optional, tag-based) is unchanged.
- Telemetry is unchanged: anonymous, opt-out (`telemetryEnabled=false`), never any
  personal data.

## [0.0.1-alpha.2]

- Phase 3 alpha: Tier-1 machines (Nero Generator, Solar Array, Ore Processor, Fabricator),
  heat + regional pollution consequence systems, orbit-gated advanced tier (Advanced Ore
  Processor, Advanced Fabricator, single-block Fusion Reactor), Auto Crafter + Item Sorter
  automation, Core side-config migration, and opt-out Sentry telemetry.

## [0.0.1-alpha.1]

- Initial multiloader skeleton on Neroland Core (NeoForge / Forge / Fabric × MC 26.1.2 /
  26.2).
