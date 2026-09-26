# Changelog

All notable changes to **NeroTech** are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.4.0-beta.2] - 2026-09-26

Redeploy to Maven Local and GitHub Packages.

## [0.4.0-beta.1] - 2026-09-26

The add-on seam for NeroPower, and four safety fixes. Requires **Neroland Core 1.13.0** (was
`1.13.0`).

### Added

- **Public API package `za.co.neroland.nerotech.api`** — stable within a 0.x minor; NeroPower depends
  on it. Everything outside it stays internal.
  - `PowerMachine`: the read/steer surface every `NeroTechMachineBlockEntity` now implements
    (energy buffer, heat / capacity / add / extract / ambient / overheated, preset get + set,
    `shedable()`, `owner()`, `reportStatus`). `addHeat`, `extractHeat` and `reportStatus` are public on
    the base now; `heatCapacity()`, `ambient()`, `owner()` and `clearOwner()` are new.
  - `MachineTypeRegistry`: register a block-entity type on the energy / item / gas / fluid surface
    once and it is wired on every loader like NeroTech's own machines. NeroTech seeds it first from
    `NeroTechCommon.init()`; `ModBlockEntities.*MachineTypes()` now return registry snapshots.
    NeoForge reads the registry in `RegisterCapabilitiesEvent` (add-ons register from their mod
    constructor); Fabric subscribes with `onRegistered` listeners so a registration from a later
    initializer is still wired; Forge already attaches by `instanceof`.
  - `MachineFailureEvents`: failure stages (1 warning/throttled, 2 unstable, 3 failure/meltdown,
    4 containment breach) published on Core's `ThresholdEvents` bus, channel
    `nerotech:machine_failure`, scope `machineId@dimension:pos` — never a player. The Fusion Reactor
    fires stage 1 once per overheat episode (rising and falling), stage 3 on meltdown and stage 4
    on a breach.
  - `PlanetApi`: `solarMultiplier(Level)`, `windMultiplier(Level)` and `ambientAt(Level, BlockPos)` —
    the same Nerospace-aware per-planet model NeroTech's generators and thermal model use.
- **Maven publishing** of the nine loader cells as
  `za.co.neroland.nerotech:nerotech-<loader>-<mc>:<version>` to Maven Local and GitHub Packages
  (mirrors Core's setup; see `MANUAL-PUBLISH-YML.md` for the workflow step).
- Config: `fusionMeltdownTerrainDamage` (`auto` | `on` | `off`, default `auto`) and
  `fusionMeltdownRadiusCap` (1..16, default 8).

### Fixed

- **Wireless Power Node pairing is authorised.** Pairing or unlinking with the Configurator now
  requires that the acting player may interact with the block at both endpoints (spawn protection,
  world border, adventure rules) and, when both nodes have an owner, that they own both or hold
  gamemaster permission. Refusals show `item.nerotech.configurator.link_denied`. The actor is
  always the item holder, never a nearest-player lookup.
- **Wireless pairing auth hardened.** The rule now covers *every* node an action touches — the
  clicked node, the pending node, and any node whose current link would be broken (the clicked
  node's partner on unlink, either end's old partner on pair). Each must be in a loaded chunk
  (an unloaded endpoint can't be verified and is refused), interactable by the player, and — when
  it has an owner — owned by the player unless they are a gamemaster; one owned endpoint is enough
  to require ownership (previously only when both were owned, and an unloaded partner skipped the
  owner check entirely). The decision lives in the Minecraft-free `item.LinkAuth`.
- **Tests:** `WirelessPairingAuthTest` (pairing/unlink authorisation matrix) and
  `MeltdownTerrainModeTest` (`fusionMeltdownTerrainDamage` on / off / auto × dedicated /
  singleplayer, plus unrecognised values reading as `auto`); the resolver moved to the pure
  `MeltdownMath.terrainDamage`.
- **Fusion meltdown terrain damage is off on dedicated servers by default.** `auto` resolves to no
  block damage on a dedicated server (the blast still hurts and knocks back, and the reactor is
  still lost) and full damage in singleplayer / LAN; the radius is `min(shellSize + 1,
  fusionMeltdownRadiusCap)`.
- **Saved-data stores recover instead of crashing.** The pollution, attribution-preference and
  Tech Guide "seen" stores load through Core's `SavedDataRecovery` (last-known-good backup, then a
  fresh store), and each erasure refreshes that backup at once.
- **Data erasure reaches machine ownership.** An erase request through Core's `PlayerDataErasure`
  hook now also drops the placing-player UUID from every machine the player owns: loaded machines
  react on their next tick, unloaded ones on their next load, from a pending-erasure set kept in the
  attribution-preference store for 30 days.

## [0.3.0-beta.1] - 2026-09-24

EMI support and working JEI pages on Fabric for the machine pages, and NeroTech can now be started
without dust.

### Added

- **EMI support.** The five machine recipe pages JEI already had (ore processing, fabricating, advanced
  fabricating, particle accelerator, chemical processing) now come from a native EMI plugin too. Official
  EMI has no Minecraft 26.x release, so this targets the community EMI Unofficial Port (Unstable) on
  NeoForge and Fabric; that port has no Forge build. With JEI and EMI both installed, the pages show once.
  EMI stays optional. See the wiki's Recipe Viewers page.

### Fixed

- **JEI pages now appear on Fabric.** JEI on Fabric only loads plugins listed under the `jei_mod_plugin`
  entrypoint and ignores the `@JeiPlugin` annotation, so NeroTech's five machine pages never showed
  there. NeoForge was not affected.

**NeroTech can be started without dust**

- The first generator could not be crafted in survival. Every generator needs a Nero Coil or a
  Circuit Board, both needed dust, and dust needs a powered Ore Processor. The Nero Coil now also
  crafts from Copper Ingot + Iron Ingot + Copper Ingot, and the Circuit Board from Redstone + Gold
  Ingot + Redstone. The dust recipes stay, and the Fabricator still makes two per dust.
- Wiki: new Standalone Progression page covering the full path with only Neroland Core installed.
  The Making Dust page now notes that both ore processors take raw ore blocks and gives the Advanced
  Ore Processor's default yield bonus.

## [0.2.1-beta.1] - 2026-09-24

Gases move through pipes, one oxygen shared with Nerospace, and the gas textures load. Pair it with
Nerospace 1.1.1, whose Universal Pipe now connects to NeroTech's gas machines.

### Fixed

**Gases move through gas pipes, and the Electrolyzer drains** ([#9](https://github.com/Neroland/nerotech/issues/9))

- **One oxygen, shared with Nerospace.** NeroTech's oxygen is now `nerospace:oxygen`, the same gas
  as the Nerospace Oxygen Generator and life support. The old separate `nerotech:oxygen` gas meant
  the Chemical Processor refused Oxygen Generator oxygen even when a pipe delivered it. NeroTech
  still does not depend on Nerospace: the id is only a name.
- **Legacy oxygen migrates automatically.** Tanks saved with `nerotech:oxygen` load as
  `nerospace:oxygen`. NeroTech machines accept `nerotech:oxygen` from any source, such as a Core Gas
  Tank filled by an older build, and store it as the shared gas. A `turbineGasBurn` entry for the
  old id counts for the new one. The transport fluid keeps its registry id `nerotech:oxygen`, so
  other mods' pipes and tanks keep their contents. It now stands for `nerospace:oxygen`.
- **Gas pipes carry NeroTech gases.** With a pipe on Core's `nerolandcore:gas` capability (such as
  the Nerospace Universal Pipe from Nerospace 1.1.1), the Electrolyzer's products
  flow out and into the Chemical Processor, the Gas Turbine or a Core Gas Tank. Those machines
  accept gas on every face by default and pull from an adjacent pipe or tank on their own.
- **The Electrolyzer's gas faces are output-only.** An I/O gas face used to take gas back, so a pipe
  that both pulls and pushes returned every millibucket and the product tanks never emptied. The
  machine then stopped with full tanks. On Core's gas capability, I/O now means output for the
  Electrolyzer, and an input face takes nothing.

**Crash reporting**

- Crash reports no longer include vanilla's "Invalid block entity" message. Minecraft logs it when a
  chunk's saved NeroTech machine data no longer matches the block there, for example after another mod
  remapped blocks. Vanilla discards the stale data and the world keeps working. Exceptions thrown inside
  another mod's own code are no longer reported either.
- **Hydrogen and oxygen fluid textures render.** The sprites lived in `textures/fluid/`, listed by
  an `assets/nerotech/atlases/blocks.json` that Minecraft never reads (atlas definitions are read
  from the `minecraft` namespace only). The sprites were never stitched, so both gas fluids drew
  the missing texture in tank and pipe GUIs and JEI. They now live in `textures/block/`, which the
  block atlas always includes.

## [0.2.0-beta.1] - 2026-09-20

Minecraft **26.3** support, plus one recipe fix.

### Added

- **Minecraft 26.3** as a new Stonecutter node on every loader — NeoForge `26.3.0.7-beta`,
  Forge `26.3-66.0.2` and Fabric (fabric-api `0.161.0+26.3`, NeoForm `26.3-1`) — built alongside
  26.1.2 and 26.2, so every release now ships **nine** loader × version jars.

### Changed

- VS Code run/debug configurations (`.vscode/launch.json`, `.vscode/tasks.json`) gain the three
  26.3 cells; the "Build all" task now builds all nine.
- CI (`multiloader.yml`, `publish.yml`) builds, attaches and publishes the 26.3 jars.
- Requires **Neroland Core 1.13.0** (was `1.12.0`) — the first Core release with a 26.3
  build. The loader range still derives from the pin (`[${nerolandcore_version},2.0)`).
- JEI pins moved to the newest published builds on each Minecraft version: `29.40.0.101` (26.1.2), `30.35.0.223` (26.2) and `31.3.0.18` (26.3). Compile-time API only — JEI remains a soft dependency and the shipped jar gains no hard requirement. The `compat/jei` plugin compiles unchanged against all three.

### 26.3 port notes

- Block classes build their codecs through Core's `BlockCodecs` (26.3 removed block-type codecs); `codec()` is kept without `@Override` so one source compiles on every version.
- 26.3 API differences are handled with Stonecutter blocks: `PoseStack#rotate` (was `mulPose`), the new `Prediction` argument on `drop` / `placeItemBackInInventory`, `setPermanentlyInvulnerable`, and similar renames.
- JEI recipe sync keeps a version-neutral list, because 26.3's `RecipeMap` can no longer be built from a collection.
- Fixed: the Configurator recipe used the invalid category `tools`, so it failed to load on every version. It is now `equipment`.
- Build: the shared `common/` Java source is now preprocessed by Stonecutter for every non-active node (`stonecutterProcessCommon`), so common code can carry `//? if >=26.3 {` blocks, and `common/src/main/resources-<mc>` overlay folders are merged over the shared resources for matching nodes (`mergeCommonResources`). The active node still compiles the raw `common/` folder.
- Build plugins aligned with Neroland Core: ModDevGradle `2.0.147` (the older 2.0.141 cannot set up NeoForge 26.3), ForgeGradle `7.0.40`, Stonecutter `0.9.8`.
- NeoForge metadata: the deprecated `logoFile` property is replaced by `iconFile` on 26.2+ (the logo is a square 256x256 PNG) while 26.1.2, whose FML only understands the old key, still gets `logoFile` — the key is chosen per cell when the manifest is expanded. This clears NeoForge 26.2+'s dev-only "uses the deprecated `logoFile` property" warning screen. The Forge manifest is unchanged: `logoFile` is still the only key Forge supports.

## [0.1.0-beta.2] - 2026-09-19

### Fixed

**The Electrolyzer accepts water from pipes, not only from buckets** ([#9](https://github.com/Neroland/nerotech/issues/9))

- NeroTech exposed its fluid tanks on Neroland Core's `nerolandcore:fluid` capability only, which is
  a Nero-private surface: no third-party pipe could see the Electrolyzer's water tank, so the machine
  could be filled by bucket and nothing else, even while the same pipe network happily carried power
  (Core bridges energy to Forge Energy, and had no fluid equivalent). Fluid machines are now
  registered on the **standard** fluid capability of every loader as well — NeoForge
  `Capabilities.Fluid.BLOCK`, Forge `FLUID_HANDLER`, Fabric `FluidStorage.SIDED` — through the
  adapters added in Neroland Core `1.12.0`, which this release requires.
- The water tank is now a filtered `MachineFluidTank` (the fluid twin of `MachineGasTank`): it
  accepts water and refuses everything else. Without the filter, the first millibucket of lava from
  someone's pipe would have latched the tank and starved the machine permanently. A tank left holding
  a non-water fluid by an older build is emptied on load, with a line in the log saying where — that
  water could not have been removed any other way, and the machine would have read STARVED for good.

### Added

**Hydrogen and oxygen are real fluids, so pipes can carry them**

- NeroTech's two gases are now registered as Minecraft fluids as well — `nerotech:hydrogen` and
  `nerotech:oxygen` — purely as a **transport identity**. A Core gas is an `Identifier`, a surface no
  other mod speaks, which is why oxygen could never leave the block that made it except by touching
  its consumer; a `Fluid` is a name every mod already understands, so any fluid pipe can move it.
- Every gas tank is exposed on the loaders' standard fluid capability wearing that fluid, alongside
  the machine's own fluid tank — a machine with both offers one tank per index. The gameplay
  contract is unchanged: tanks are still `NeroGasStorage`, still millibuckets, still filtered, and
  Nero blocks still talk to them through Core's gas capability.
- The fluids are **not placeable**: no fluid block, no flowing variant, no bucket. They exist in
  tanks and pipes only. Textures are generated by `tools/gen_fluid_textures.py` and registered on
  every loader, so third-party tank GUIs draw them instead of a missing texture.

**Fluid and gas side-config channels**

- The **Electrolyzer** declares `FLUID` and `GAS` channels alongside `ENERGY`, so its side-config
  screen finally has the Fluid and Gas tabs other machines have. Water is accepted on every face,
  both gases are extractable on every face (the handoff the machine always had), and fluid
  **auto-input** is on by default — the Electrolyzer now pulls water from an adjacent tank or pipe
  end rather than waiting to be fed.
- The **Chemical Processor** and **Gas Turbine** declare a `GAS` channel with auto-input on, so each
  pulls its reagent/fuel from an adjacent source instead of relying on the producer to push.
- Gas handoff honours the side config: gas leaves only through faces whose GAS mode is extractable.

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
