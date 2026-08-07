# Changelog

All notable changes to **NeroTech** are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

_Nothing yet._

## [0.1.0-beta.1] - 2026-08-02

The big push from alpha to the first beta: every machine gets a real visual identity, the Fusion
Reactor becomes a scalable multiblock, heat becomes a full thermal model, pollution gets its
mitigation tech, every GUI gains analytics, presets and side-config tooling — and the tech tree
grows four whole tiers: a free-form **Particle Accelerator**, a **fluid & gas chain**, a six-block
**power tier**, automation QoL and an **exotic endgame**. NeroTech is **standalone-first**: every
machine, including the endgame, is reachable with NeroTech + Neroland Core alone, and pairing with
other Neroland mods accelerates progression rather than unlocking it.

### Added

**Particle Accelerator**

- A free-form endgame build and NeroTech's **standalone** route to space-grade materials: no
  multiblock, no fixed footprint. Lay **Accelerator Guide Coils** at the **Accelerator
  Controller**'s own Y level in any shape; right-click each (empty hand or Configurator) to cycle
  its bend **straight / 45° left / 45° right**. The controller ray-marches its facing up to
  `acceleratorMaxGap` blocks per hop, follows the bends, and a line that returns to itself is a
  **closed loop**. A virtual particle (server-side, no entity, drawn as an END_ROD streak) is
  injected from the first slot and boosted at every guide it passes.
- Ring **geometry is the progression axis**, via three unit-tested rules: the **gap rule** (a
  stretch too long for the current speed loses the particle; inverted, it sets the injection
  speed), the **bend rule** (a 45° turn survives at most `acceleratorBendSpeedBase` × the run-up
  before it — the shortest bend stretch caps the loop), and **collision energy**
  `E = 0.5·v²·acceleratorEnergyScale`. Put a second item in the collision slot and every lap
  through the controller attempts the collision.
- Collision recipes (`nerotech:collider`, datapack-driven) take **two order-free ingredients plus a
  `min_energy` floor** — an energy requirement is really a minimum ring size: 800 J / 1,500 J for
  the dust transmutations (a starter octagon), 3,000 J for **Starsteel Dust** (Netherite Scrap +
  Iron Dust) and **Void Crystal Dust** (Echo Shard + Amethyst Shard), 12,000 J for the **Antimatter
  Cell** (two Stellar Cells — a ~29×29 ring). Both blocks craft from vanilla + Tier-1 materials
  only, so the advanced tier (Fusion Reactor included) is reachable on Earth alone.
- The ring **reads itself back**: after every trace the controller writes each guide's outgoing
  beam direction into a display-only `heading` blockstate, lighting the coil's top with one of 24
  directional arrows (3 bends × 8 compass headings). An unlit coil is not part of a closed loop —
  the first debugging tool for a ring that will not close. Ships with a two-input JEI page, Tech
  Guide steps, advancements and the [Particle Accelerator](wiki/Particle-Collider.md) wiki page.
  _Mechanic inspired by Oritech's particle accelerator; clean-room implementation._

**Scalable Fusion Reactor multiblock**

- The Fusion Reactor is now a **multiblock**: a hollow 3×3×3, 5×5×5 or 7×7×7 shell of the new
  **Fusion Casing** and **Fusion Containment Glass**, controller set into one wall, strictly empty
  interior. **Inert until formed** — a spinning plasma torus telegraphs a live core.
- **Four fuel tiers**, all datapack tags: **Fusion Cell** (any shell) → **Plasma Cell** (Starsteel,
  5×5×5+) → **Stellar Cell** (Void Crystal + Starsteel, 7×7×7) → **Antimatter Cell** (see the
  exotic endgame below; the 7×7×7 alone contains it). Per-tier burn times
  (`fusionFuelTier1BurnTicks`..`Tier4`) and per-size output multipliers (1× / 4× / 12×,
  `fusionSizeOutputPermille`) are config.
- **Scaled risk**: bigger shells run hotter and melt down harder (blast radius 4/6/8;
  `fusionReactorMeltdownEnabled` still stalls instead when off). Breaking the shell **mid-burn** is
  a **containment breach**: the charge is lost and a pollution burst vents into the region.

**Power tier** _(the feature set once planned for a separate NeroPower mod — see Changed)_

- **Wind Turbine** — fuel-free, day and night: `windTurbineNePerTick` (default 25) scaled by an
  altitude curve (0.5× at y≤80 rising to 2× at y≥200) and `windDimensionMultipliers`; needs sky
  access above. An airless Nerospace planet gives zero — no atmosphere, no wind.
- **Geothermal Generator** — counts lava/magma in the 3×3 directly beneath (0–9) for
  `geothermalNePerTickPerSource` (default 8) each. Perfectly steady, runs hot, no pollution.
- **Bio Generator** — burns the new datapack-overridable `#nerotech:bio_fuels` item tag (seeded
  with dried kelp blocks; NeroAgriculture and packs add crops with no code change) at
  `bioGeneratorNePerBurnTick` (default 48) — 20% above the Nero Generator at **half** the
  pollution.
- **Battery Bank** — a single-block `batteryBankCapacity` (default 1,000,000) NE buffer with
  every-face I/O and auto-eject, FE interop included.
- **Grid Controller** — passive brownout protection: rescans `gridControllerRadius` every 100
  ticks; below `gridShedThresholdPermille` it drops non-generator machines to Eco until fill
  recovers past `gridRestorePermille` (hysteresis, so a grid at the line never flaps). It throttles
  demand, never supply.
- **Wireless Power Node** — pair two with the Configurator (crouch-use each end): up to
  `wirelessNodeTransferPerTick` (default 200) NE every 5 ticks across `wirelessNodeRange` (default
  32) blocks, lossless, never chunk-loading, self-unlinking on break.

**Fluid & gas chain**

- Neroland Core's fluid/gas substrate is now live, with NeroTech's first gases
  (`nerotech:hydrogen`, `nerotech:oxygen`). **Electrolyzer** splits stored water 2:1 (bucket or
  Core fluid capability in — no item slots) and hands both gases to adjacent gas blocks;
  **Gas Turbine** burns gas (`turbineGasBurn` config map) into NE with mild heat and **zero
  pollution** — clean power, not free power (electrolysis is a net energy sink by design);
  **Chemical Processor** washes raw ore with oxygen for **3** dust versus the Ore Processor's 2
  (`nerotech:chemical_processing` recipes). Every tank is on Core's shared `nerolandcore:fluid` /
  `nerolandcore:gas` capabilities, so Core's tanks and any mod on those surfaces interoperate.
- **Coolant loop** — active cooling as two blocks: the **Radiator** (passive, worth four natural
  coolant blocks, never melts) and the **Coolant Pump** (spends NE per thermal-exchange interval to
  pull heat out of every adjacent machine, scaled by Radiators within 3 blocks in a line). All
  scans event-driven and cached.

**Full thermal model**

- Machines **conduct heat to adjacent machines** and relax toward a **local ambient** derived from
  dimension (`thermalAmbientByDimension` — the Nether runs hot) plus a biome flavour term. Coolant
  blocks (water, ice, snow, Radiators) are cached alongside the neighbour links — no per-tick
  scanning anywhere. Six tuning keys (`thermalConductivityPermille` .. `thermalBiomeScale`).

**Pollution mitigation tech**

- **Scrubber** — cleans its own pollution region at full rate plus the 8 neighbours at a fraction;
  consumes **Filter Cartridges** that foul into **Dirty Filters** (reprocess for an iron refund).
- **Remediator** — heavy-duty slotless area cleaner: no consumables, steep NE cost, Starsteel-tier.
- Regional pollution publishes **threshold events** through Core 1.7.0
  (`pollutionEventThreshold`) — dormant until a listener mod (NeroEvents) exists.

**Automation & QoL**

- **Conveyor Belt** — the only NeroTech block with no block entity: a flat directional plate that
  nudges item entities along its facing (~0.15 blocks/tick, capped). No power, no GUI — lines and
  corners form naturally. Crafts 6 from Iron Dust + Nero Coils + Redstone.
- **Robotic Arm** — once a second moves up to `roboticArmStackPerMove` items from the container
  behind it into the one in front, for `roboticArmNePerMove` NE per item, through the standard
  hopper-style container surface (side configs and sidedness honoured on every loader). A GUI-only
  filter slot restricts it to one item type.
- **Auto Crafter recipe preview + grid lock** — the output well shows a server-matched ghost of the
  current grid, and a Lock toggle snapshots the grid as per-slot templates enforced through the GUI
  _and_ pipes/hoppers, so automation can never scramble the recipe.
- **Configurator wrench** — click a machine face to cycle its side mode, sneak-click to read the
  layout, and **copy/paste**: copying captures side config **and** overclock preset, and pasting is
  cross-type — each copied channel applies wherever the target declares it. Also pairs Wireless
  Power Nodes and sets accelerator coil bends.

**Exotic endgame**

- **Antimatter Cell** — tier-4 fusion fuel, made only by the accelerator (no crafting recipe).
  Burns `fusionFuelTier4BurnTicks` (default 28,800 — twice a Stellar Cell) with a flat +2 heat-rate
  surcharge: the strongest generator in NeroTech and the likeliest to melt down.
- **Singularity Vault** — bulk storage for **one** item type up to `singularityVaultCapacity`
  (default 1,000,000), held as a virtual store. Automation sees a two-slot facade (input drained
  in, output kept stocked); by hand right-click deposits, crouch-right-click withdraws; a
  comparator reads the fill. Crafted around an Antimatter Cell. **Empty it before you break it** —
  the stored bulk does not drop.
- **Accelerator transmutation** — lossy collider recipes: Copper Dust ×2 → Iron Dust (800 J), Iron
  Dust ×2 → Gold Dust (1,500 J). Transmuted gold is the most expensive gold in the game.

**Production analytics**

- Every machine GUI gains an **Analytics tab**: a status line naming the limiting cause, live heat/
  energy/pollution/efficiency readouts, and 60-second heat and energy sparklines.
- New **Analytics Terminal** — a passive console scanning `analyticsTerminalRadius` (default 16):
  counts by status, the hottest machine, the 10 nearest with their causes, and a rolling
  five-minute **power-history sparkline** of net stored-NE change across the watched machines.

**Overclock presets**

- Every processing machine and generator gets **Eco / Balanced / Overdrive** (−25% speed & −50%
  energy/heat/pollution ↔ +50% & +100%). Presets stack with upgrade modules and persist — an
  overdriven reactor courts its own meltdown threshold.

**Tech Guide**

- New **Tech Guide pedestal** (and hand-held datapad): a guided tour of **8 chapters / 22 steps** —
  First Power → Processing → Consequences → Mitigation → Automation → Analytics → Fusion → Exotic
  Matter — driven entirely by advancements. A loaded pedestal projects your next step as a hologram.

**Recipes & progression**

- Machine processing runs on **real datapack recipe types** (`nerotech:ore_processing`,
  `fabricating`, `advanced_fabricating`, `chemical_processing`, `collider`) — packs add, replace or
  remove recipes as plain JSON.
- The dust economy closes: Iron/Gold/Copper Dust **smelt and blast into ingots** at vanilla
  times/XP, and **machines beat the workbench** — the Fabricator returns 2 Circuit Boards / Nero
  Coils per dust versus crafting's 1, and a Machine Frame costs 1 iron in the machine versus 8 by
  hand. **Fusion Cells are machine-exclusive** (Advanced Fabricator only). Raw metal **blocks**
  bulk-process into 18 dust. Iron Dust gains real sinks (Conveyor Belts, Robotic Arms).
- **Just Enough Items support** — a shared JEI plugin with pages for every machine recipe type and
  each machine as a crafting station (the client opts into recipe sync; recipe definitions only).
- A connected advancement tree grows with every tier, including **Charge Reserve**, **Contained
  Annihilation** and **Room for Everything**.

**NeroLink companion-app integration**

- NeroTech registers a full **link module** with Core's link API, so the NeroLink companion app
  auto-serves a NeroTech section: `pollution` (your own attributed total, only when attribution is
  on and you have not opted out), `guide` (your Tech Guide progress) and a public `wiki` section
  rendering these wiki pages in-app. All personal data is own-data-only, UUID-scoped (POPIA/GDPR).
- **Actions**: `set_pollution_attribution` (your own privacy opt-out, wired into the shared
  data-erasure hook) and `set_machine_preset` (remote preset; server re-checks online + ownership).
- **Live events/alerts**: a WARN alert when your attributed pollution crosses the threshold; a
  CRITICAL alert + world `meltdown` broadcast on a reactor meltdown or containment breach.

**A complete visual identity**

- Every machine re-modelled and re-textured in a **32× teal/plasma** art language, and a subset of
  them — eleven **animated block-entity renderers** — moves: spinning turbine, sun-tracking solar
  deck, crusher drums, fabricator arms, the fusion torus, Auto Crafter hologram + press,
  live-tinted sorter ports, fouling scrubber cartridge, remediator boom, shimmering terminal, Tech
  Guide hologram. The blocks added by this release's expansion tiers (accelerator, gas chain, power
  tier, automation, endgame) ship **static models** for now; animated renderers for them are still
  to come. Machine glow shifts cyan → orange/red as heat climbs. Client-only
  `renderAnimationsEnabled` turns it all off for low-end machines.
- **`/nerotech gallery`** (creative/OP) builds a running showcase of the entire mod — every block
  on a display grid plus live exhibits: a colliding accelerator octagon, the gas chain, a coolant
  loop on a 5×5×5 reactor, the full power park with a linked wireless pair, and an automation lane
  with moving items and a stocked vault. `gallery clear` removes it. Records nothing about the
  player.

**Optional Nerospace planet interop**

- With **Nerospace** installed, solar output, **wind** and thermal ambient automatically follow
  planet traits (airless boosts solar and kills wind; hot/cold planets shift ambient). Resolved by
  **pure reflection at runtime — Nerospace is never a build or load requirement**; without it the
  per-dimension config keys apply and NeroTech remains fully standalone.

**Quality of life**

- NeroTech items carry Core's coloured **inventory highlights**: upgrade modules (green), the
  Configurator (violet), fuels and filters (teal). The Solar Array tooltip explains its niche
  versus Nerospace's tiered Solar Panels.

### Changed

- **Neroland Core 1.7.0 or newer is required** (was 1.3.x in the alpha). Update Core first — older
  versions refuse to load with this release.
- **Standalone-first progression, by design** — no progression gate ever locks a NeroTech machine;
  pacing comes from recipes and materials (the accelerator's energy floors, the Starsteel tier).
  NeroTech still opens Core's `industrial_power` milestone on first machine placement for other
  Neroland mods to _read_ — a milestone, never a lock.
- **The NeroPower split is retired** — NeroPower will not ship as a separate mod; its planned
  feature set is the power tier above. One mod, one power network. Generation still talks only to
  Core's energy surface. [Power & the NeroPower Split](wiki/Power-and-NeroPower-Split.md) records
  the decision, with the old criteria archived.
- Machine recipes moved from in-code tables to datapack JSON; behaviour is unchanged out of the
  box, but packs overriding the alpha's fixed tables should switch to recipe files.
- Side configuration is synced to clients (the Item Sorter shows its port modes in-world) and
  machine GUIs open reliably from any distance and angle.
- Unit tests run on all six loader×version cells, and loader capability registration is generic —
  a new machine is wired everywhere by adding it to one list. NeroTech builds from a bare clone
  (Core resolves from GitHub Packages; no sibling repos needed).

### Fixed

- **Machine blocks now drop themselves when mined** — all machines were missing from
  `minecraft:mineable/pickaxe`, so no tool counted as correct and they dropped nothing.
- **GUI gauges now show live values** — the base machine menu never registered its container data,
  so energy/heat/progress gauges silently read zeros. Latent since the first alpha.
- Machine models no longer black out the faces of adjacent blocks (non-cube shapes are correctly
  non-occluding).

### Migration notes (from 0.0.1-alpha.x)

- **Fusion Reactors placed in the alpha become inert** until a valid casing shell is built around
  them (nothing is deleted). A 3×3×3 shell brings an existing reactor back online.
- Requires **Neroland Core >= 1.7.0** (breaking floor bump — see Changed).
- **Nerospace is optional** — only needed for planet-trait modifiers, and no longer referenced at
  build time at all.
- Energized Power interop (optional, tag-based) is unchanged.
- Telemetry is unchanged: anonymous, opt-out (`telemetryEnabled=false`), never any personal data.

## [0.0.1-alpha.2]

- Phase 3 alpha: Tier-1 machines (Nero Generator, Solar Array, Ore Processor, Fabricator),
  heat + regional pollution consequence systems, orbit-gated advanced tier (Advanced Ore
  Processor, Advanced Fabricator, single-block Fusion Reactor), Auto Crafter + Item Sorter
  automation, Core side-config migration, and opt-out Sentry telemetry.

## [0.0.1-alpha.1]

- Initial multiloader skeleton on Neroland Core (NeoForge / Forge / Fabric × MC 26.1.2 /
  26.2).
