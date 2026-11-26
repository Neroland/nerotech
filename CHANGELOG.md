# Changelog

All notable changes to **NeroTech** are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.10.1]

### Added

- Minecraft 1.20 through 1.20.4 on Fabric and Forge, and 1.20.4 and 1.20.5 on NeoForge.

- Sneak-use a Configurator on a Nero, Bio or Geothermal Generator to see its recent energy
  production in NE/t.

### Changed

- The NeroTech Configurator works on NeroLogistics conduits and Nerospace Universal Pipes.
  Configurators from those mods can cycle and inspect basic NeroTech machine faces.
- Requires Neroland Core 1.24.0 or a newer 1.x release.

### Fixed

- Fixed a crash when opening the Particle Accelerator or process recipe pages with older JEI
  builds on Minecraft 1.21.1.
- The wiki now lists the full Component Forge recipe for the Capacitor, including ingredient
  counts, sulfuric acid, output, processing time and energy cost.

## [0.10.0] - 2026-11-24

### Added

- Minecraft 1.20.5 on Fabric and 1.20.6 on Fabric, Forge and NeoForge. Both include the in-game
  Tech Guide. JEI and EMI integration remains available on the newer targets listed in the README.

### Changed

- Requires Neroland Core 1.23.0 or newer.

## [0.9.0] - 2026-11-22

### Added

- **Data export:** NeroTech registers with Neroland Core's per-player export (`/neroland data exportme`):
  your opt-in pollution attribution row, attribution opt-out and Tech Guide "seen" marks; machine ownership
  is described, not enumerated. Never written to disk or logged.

### Changed

- Requires Neroland Core **1.20.1**.
- `nerotech:inputs/deuterium` and `inputs/helium_3` now reference Nerospace's `#nerospace:gas_canisters/*`
  tags correctly (they named the tag without `#`, so it matched nothing), and `nerotech:fusion_fuels` also
  takes `#nerospace:gas_canisters/rocket_fuel`. All optional (`required: false`).

### Fixed

- **Crash reports from NeroTech were silently dropped** whenever another mod (Nero or not) initialised
  Sentry after it: every mod shares one bundled Sentry hub and each `Sentry.init` replaced the last.
  NeroTech now reports through its own isolated Sentry client and scopes and never touches the global hub.
  Same opt-out, filtering and scrubbing as before; handled-exception tags now reach the event.

## [0.8.1] - 2026-11-26

### Added

- **Add-on API** (requested by NeroPower): `api.MachineSettings` (`machineMaxTransfer`,
  `renderAnimationsEnabled`), `api.Configurator` (copy/paste mode and the pending link target of a
  Configurator stack), `api.Pollution.record(...)` (place only, or with owner attribution where the server
  and the player allow it), `api.PrivacyPrefs.ownDataOptedOut(...)` and `api.HeatAcceptor`, so any block
  entity can trade heat with Heat Pipes and Heat Exchangers without a NeroTech base class. The internal
  `machine.heat.HeatAcceptor` now extends the public one and is deprecated.
- `NeroTechMachineBlockEntity`, `NeroTechMachineBlock`, `MachineStatus` and `MachineEnergy` are now
  declared add-on stable: their signatures keep within a minor, like the `api` package.

### Changed

- **Electrolyzer:** Speed modules now raise the per-tick draw by the same factor they shorten an
  operation, so they no longer make hydrogen cheaper. With 8 Speed modules, Eco and the Efficiency floor
  a hydrogen unit cost about 83 NE, less than a Gas Turbine gets back from it; it now costs about 660 NE.
  Speed still makes it faster; Efficiency modules and the Eco preset still make it cheaper.

## [0.8.0] - 2026-11-21

**Offworld Industry.** Six production chains, 22 new machines and blocks, a component ladder, seven powered
tools, virtual deposits, heat you can pipe, a gas-fed fusion tier, a NeroTech module for the NeroLink app and
a walkable gallery. Requires **Neroland Core 1.19.0**. NeroTech stays complete on a Core-only server: every new
item has a standalone route at roughly three to five times the offworld cost. Nerospace is optional; with it,
the chains take its ores, ices and gases by tag.

### Added

- **Six chains**, each with an offworld and a standalone route (see the wiki's Material Map and chain pages):
  - **Structural**: Cindersteel ingot, plate and rod, and titanium plate. Standalone cindersteel comes from
    iron dust, coal and blaze powder in the Plasma Crucible.
  - **Superconductor**: Superconductor Wire and Coil; **Synthetic Glacite** (`c:gems/glacite`) grown from Void
    Crystal, snow and packed ice for servers without Nerospace.
  - **Semiconductor**: Crystal Seed, Crystal Boule, Wafer, **Logic Chip** and **Quantum Chip**.
  - **Optics**: Quartz, Ember, Glacite and Void focus lenses.
  - **Chemistry**: Sulfur Dust (`c:dusts/sulfur`), sulfuric acid, polymer resin and Polymer Sheet. **Acid ore
    leaching** in the Reaction Vat: one raw iron, copper or gold ore (or raw hematite) and 500 mB of acid give
    four dusts.
  - **Fusion fuel**: deuterium, helium-3 (Centrifuge cascades standalone), Europium Dust, Europium Flux Lattice.
- **22 new blocks.** Metallurgy: Plasma Crucible, Plate Press (Plate, Rod and Wafer Dies, tag
  `nerotech:press_dies`), Filament Spooler, Blast Foundry with Foundry Brick and Foundry Heat Port (3x3x3 and
  5x5x5, heated only through its ports). Chemistry: Centrifuge (cascades of up to four), Cryo Distiller,
  Atmospheric Condenser (output follows the planet's air), Reaction Vat, Process Tank (one fluid or gas, columns
  of up to 16, comparator output). Components: Component Forge, Laser Etcher (the lens is never used up),
  Crystal Grower (grows slowly unpowered, faster in the cold). Heat: Heat Pipe, Heat Exchanger. Extraction: Deep
  Core Drill on three Drill Derricks. Fusion: Fuel Injector, Europium Flux Lattice. Utility: Induction Charger,
  Annunciator Panel.
- **New media**: gases deuterium, helium-3, methane, nitrogen and carbon dioxide; liquids cryo coolant,
  sulfuric acid and polymer resin, with buckets. Nerospace items enter through machines by
  `nerotech:inputs/*` tags, never straight into a tank.
- **Component ladder** above the tier-1 parts: Logic Chip, Quantum Chip, copper / cindersteel /
  superconductor wire and coil, Servo, Capacitor, Heat Sink, Polymer Sheet, Wafer, Crystal Seed, four lenses,
  Iron / Cindersteel / Glacite drill heads. Sibling tags: `nerotech:components/servos`, `/capacitors`,
  `/heat_sinks`, `nerotech:wafers`, `nerotech:crystal_boules`, `nerotech:crystal_seeds`,
  `nerotech:polymer_sheets`, `nerotech:processors/logic`, `nerotech:processors/quantum`, `nerotech:coils/tier1..3`,
  `nerotech:focus_lenses`.
- **Two upgrade modules**: Heat Sink Module (moves the throttle point towards the heat ceiling, at most half the
  headroom) and Scrubber Module (a fifth less pollution each, floor a quarter).
- **Seven tools**, all charged with Core energy and config-tunable: Power Drill (single / 3x3 / vein; vein only
  follows `nerotech:power_drill/vein`, by default ores, gravel, clay and glowstone), Cutting Lance (the fitted
  lens sets the cut), Arc Wrench (rotate, or pick up a machine with its contents and settings), Multimeter,
  Survey Scanner, Thermal Goggles, Filter Mask. Every break or placement goes through the loader's normal
  permission checks, so claim and protection mods can refuse it.
- **Virtual deposits**: every chunk has a deposit worked out from the world seed, dimension and chunk. The Deep
  Core Drill mines it without changing terrain; it depletes and recovers. Data-driven tables per dimension
  (`data/<ns>/nerotech_deposits/`), nine built in.
- **Heat as a resource**: Heat Pipe runs (no block entity, cached, at most 64 pipes) carry heat between
  machines and into Foundry Heat Ports; the Heat Exchanger moves heat between a machine and a Process Tank of
  coolant, lava or water. Heat never becomes energy.
- **Gas-fed fusion**: Fuel Injectors feed deuterium and helium-3 (2:1) to a 7x7x7 shell, drawn per charge, at
  1.5x a cell burn. Each Europium Flux Lattice takes 6% of the burn's heat, up to eight.
- **Induction Charger** and a public charging API, `api.WirelessCharging` (`offer(ServerLevel, BlockPos, long,
  boolean)`, `registerItemCharger`). No player data in the call.
- **Annunciator Panel**: one lamp per alarm (overheat, starved, full, polluting, failing) from the machines it
  touches, a comparator channel per panel, needs no power.
- **Gauge tooltips**: hovering any bar or gauge in a NeroTech screen shows what it is, how much, and the rate,
  plus the stall cause and its fix.
- **Pollution exposure**: a region at the severe band (twice `pollutionEventThreshold`) gives brief Weakness and
  Hunger unless a charged Filter Mask with a cartridge blocks it.
- **NeroLink link schema 2**: new `nearby` section (machines within 128 blocks of the online requester, in their
  dimension; no coordinates, no owner, opaque 30-second handles, shuffled), new public `catalogue` section
  (machines, materials and both routes of all six chains), `set_machine_preset` by handle (within 16 blocks,
  claim-checked, 10 actions per minute, one uniform refusal), and an `alarm` event. A NeroTech module in the
  NeroLink app: Nearby, Chain planner, Tech Guide progress and your own pollution.
- **Tech Guide**: ten new chapters (18 in total), entries for every new block and tool, and a paged chapter
  rail.
- **Quest pack**: twelve quests added to *Industrial Record*.
- **Milestone records**: `nerotech:first_foundry_pour`, `first_quantum_chip`, `first_deposit_tapped`,
  `gas_fed_ignition`, `meltdown_averted` and `vault_operated`, credited to the player operating the machine at
  that moment. The four older records are now credited the same way as well, so they open on a default server.
- **Map layer** for NeroMaps (Core map API): pollution bands around the viewer, in their own dimension.
- **Commands**: `/nerotech budget` (per-machine-type tick cost over 200 ticks, op level 2) and
  `/nerotech gallery tour [next|stop]`. The gallery gained a twelve-bay plant: one bay per chain, a heat loop
  into a Blast Foundry, a drilling rig on a demo deposit, a gas-fed reactor, alarms and a tool rack, an item
  catalogue and a tank farm. Gallery machines credit no milestone, trophy or statistic.
- **Config**: 76 new keys (deposits, the machine families, modules, tools, pollution exposure). Every key is on
  the wiki's Config page.
- New API, additive only: `api.Atmosphere` and `api.PlanetApi.atmosphereAt(Level, BlockPos)`;
  `PollutionManager.record` keeps its old signature as an overload. `MachineMenu` gained data slots.

### Changed

- **Defaults chosen for this release**: `pollutionExposureEffects=true` (severe band only);
  `inductionChargePlayers=true`; `turbineGasBurn` now also burns methane (`nerotech:hydrogen=2,nerotech:methane=4`);
  the Induction Charger is a tier-1 recipe; the four 0.8.0 acid-leaching recipes take 500 mB of acid and 240
  ticks (the older Chemical Processor is unchanged).
- **Pollution is now kept per dimension.** Existing regions load into the Overworld on first start. **Listeners:**
  the `nerotech:pollution` and `nerotech:pollution_decay` threshold-event scope is now
  `<dimension id>|<region key>`; before 0.8.0 it was the bare region key.
- **`/nerotech gallery` now needs permission level 2**, as its description always said, and stays player-only and
  creative-only.
- **Forge now sends NeroTech's machine recipes to the client** on 1.21.4 and later, so JEI and EMI pages are no
  longer empty on Forge. All sixteen recipe types have JEI and EMI pages.
- **Machines hand fluids and gases to their neighbours** by side configuration, like items.
- The Fusion Reactor runs on a shared multiblock base with the Blast Foundry and the Deep Core Drill. Existing
  reactors and saves load unchanged.
- No released id was renamed or removed, and no recipe from before 0.8.0 was changed.
- **Drill heads now stack to 1 and carry their own wear** (`nerotech:drill_head_wear`, shown as a bar). A
  part-worn head can be taken out and refitted without losing or resetting its wear. A drill saved with wear
  on the machine moves it onto the fitted head the first time it ticks.
- **Remote preset changes are owner-only.** `set_machine_preset` from the app now works only on machines you are
  the recorded owner of; `nearby` entries carry `ownedByYou`, and the app shows the preset control only on those.
- **The Arc Wrench asks the loader's break event before lifting a machine**, so protection mods that deny
  breaking also deny lifting.
- **Progress no longer carries over to a different recipe.** Swapping a machine's inputs for another recipe
  mid-run starts from zero.
- **Machines drop their contents when broken on 1.21.1 and 1.21.4**, as they already did on later lines.
- **Induction Charger accounting.** It now charges exactly what it hands out, however the request is split, and
  rounds energy conversions so no FE ratio makes energy.
- The gallery's endless demo deposit now serves only the exhibit drill; any other drill in that chunk reads the
  world's deposit.

### Privacy

- **The link `meltdown` event no longer carries coordinates and is no longer broadcast.** It used to go to every
  linked client with the reactor's x/y/z; it now goes only to players in range and to a recorded owner. Link
  schema 2; schema-1 sections keep their shape.
- 0.8.0 adds no store keyed by a player. Deposits are chunk-keyed world data; the Induction Charger and
  Annunciator Panel never record an owner; new milestones go to the online operator in the moment, which
  NeroTech holds only in memory; the gallery remembers only its dimension and origin (at most 16 sites, removed
  by `gallery clear`). `PRIVACY.md` and the new `Privacy` wiki page describe each one.
- Push notifications for NeroTech go only to a machine's recorded owner; everyone else's notices only refresh an
  open app. The app keeps the Nearby list in memory only.
- An erase request now also clears the link module's in-memory state (nearby handles, action rate window,
  alarm throttle) at once, and machines drop an erased player as their transient operator.
- The Electrolyzer's old-fluid purge no longer logs the machine's position.
- New compliance tests: a source scan that fails on any unreviewed player-identity persistence, SavedData,
  data component, UUID-keyed map or identity in INFO+ logs, and Core's erasure conformance harness over
  every NeroTech player store.

### Cut from this release

- **Particle Accelerator routes to cindersteel, Synthetic Glacite and europium.** A ring's energy cap makes any
  such route as cheap as, or cheaper than, the offworld one. Slow process routes (Plasma Crucible, Crystal
  Grower, Centrifuge on glowstone) carry those materials instead.
- **Standalone deuterium** is a Centrifuge recipe on Electrolyzer hydrogen, not a trace output of the
  Electrolyzer itself. The Bio Generator has no sulfur by-product; gunpowder in the Centrifuge is the standalone
  sulfur.

### Known issues

- **Fabric 1.21.4, 1.21.5 and 1.21.8:** JEI and EMI pages for NeroTech's machine recipes can be empty, because
  Fabric has no way to send them to the client on those versions. The machines work. Every other build,
  including Fabric 1.21.1, 1.21.10, 1.21.11 and 26.x, is complete.
- **Tool polish still open:** the Thermal Goggles show heat motes but no colour legend on screen; Multimeter and
  Survey Scanner readings go to chat rather than a readout card; on Minecraft 1.21.1 tool models do not change
  with the tool's mode or charge.
- **Centrifuge cascades:** gas piped straight into the last centrifuge of a cascade skips the first stages. The
  shortcut still costs more than three times the offworld route and still needs three linked centrifuges.
- A Crystal Grower loses less than one tick of growth when its chunk unloads mid-crystal.
- Europium Flux Lattices keep their "stable" look for a moment after their reactor controller is broken, until
  the next structure check.
- Annunciator Panels light no lamp for an unformed multiblock or an unpaid operating charge.
- Machine input tanks have no dump button yet; a wrong leftover fluid has to be drawn out by automation. A
  Process Tank can be vented by sneak-using it with an empty hand.
- `/nerotech budget` numbers for a large base are still to be measured on a live server.

## [0.7.1] - 2026-11-21

### Changed

- Transitioning to stable versions: the alpha/beta label is dropped from the version number, so this release is **0.7.1** (previously 0.7.1-beta.1). Nothing else changes with the version label.

### Dependencies

- Updated Neroland Core from 1.17.0 to 1.17.6.
- Updated NeroEconomy from 0.5.0-beta.1 to 0.5.1-beta.1.
- Updated Fabric Loader from 0.19.5 to 0.20.1.
- Updated Fabric API (Minecraft 1.21.1) from 0.116.17+1.21.1 to 0.116.18+1.21.1.
- Updated Fabric API (Minecraft 26.3) from 0.162.0+26.3 to 0.166.2+26.3.
- Updated NeoForge (Minecraft 1.21.1) from 21.1.265 to 21.1.266.
- Updated NeoForge (Minecraft 26.1.2) from 26.1.2.116 to 26.1.2.126.
- Updated NeoForge (Minecraft 26.2) from 26.2.0.90 to 26.2.0.93.
- Updated NeoForge (Minecraft 26.3) from 26.3.0.76 to 26.3.0.103.
- Updated Forge (Minecraft 1.21.1) from 1.21.1-52.1.20 to 1.21.1-52.1.21.
- Updated Forge (Minecraft 1.21.11) from 1.21.11-61.2.4 to 1.21.11-61.2.5.
- Updated Forge (Minecraft 26.1.2) from 26.1.2-64.1.4 to 26.1.2-64.1.6.
- Updated Forge (Minecraft 26.2) from 26.2-65.1.4 to 26.2-65.1.6.
- Updated Forge (Minecraft 26.3) from 26.3-66.0.11 to 26.3-66.1.8.
- Updated JEI (Minecraft 1.21.1) from 19.70.0.468 to 19.70.0.469.
- Updated JEI (Minecraft 1.21.11) from 27.54.0.115 to 27.54.0.116.
- Updated JEI (Minecraft 26.1.2) from 29.47.0.109 to 29.53.0.115.
- Updated JEI (Minecraft 26.2) from 30.42.0.236 to 30.48.0.244.
- Updated JEI (Minecraft 26.3) from 31.12.0.65 to 31.17.0.113.
- Updated the EMI port for Minecraft 26.3 to build+62 on Fabric and NeoForge.
- Updated Stonecutter from 0.9.8 to 0.10.
- Updated the Gradle distribution from 9.8.1 to 9.9.1.
- Updated the Sentry Java SDK from 8.60.0 to 8.64.0.

## [0.7.1-beta.1] - 2026-10-31

### Changed

- Release uploads now target CurseForge and GitHub Releases only.
- Removed automatic project description and gallery uploads.
- Renamed the website overview source to `art/description.md` and refreshed public documentation links.

## [0.7.0-beta.1] - 2026-10-30

Minecraft 1.21.x support. Requires **Neroland Core 1.17.0**.

### Added

- **Builds for Minecraft 1.21.x** on NeoForge, Fabric and Forge: 1.21.1, 1.21.4, 1.21.5, 1.21.8 (also
  runs on 1.21.7), 1.21.10 (also runs on 1.21.9) and 1.21.11, next to the existing 26.1.2, 26.2 and 26.3
  builds. The 1.21.x builds need Java 21. Machines, recipes, power, gases, the Tech Guide, the analytics
  screens, NeroEconomy machine charges and the machine animations are the same on every version.
- **JEI recipe pages on every 1.21.x build**, and **EMI pages on 1.21.1** (official EMI only exists for
  1.21.1 among the 1.21.x versions). On Fabric 1.21.4 to 1.21.8 the JEI pages can be empty, because
  Fabric has no way to send the machine recipes to the client on those versions.

### Changed

- Now requires Neroland Core 1.17.0 or later.

## [0.6.2-beta.1] - 2026-10-26

### Fixed

- EMI recipe registration now reads NeroTech's synced recipe cache instead of EMI registry methods that differ between unofficial port builds, preventing `NoSuchMethodError` during recipe reload.

## [0.6.1-beta.1] - 2026-10-26

### Fixed

- Crafting recipes now unlock in the recipe book the first time a player picks up one of their ingredients (51 recipe-unlock advancements, generated by `tools/gen_recipe_unlocks.py`; run it again after adding a recipe, `--check` verifies).
- **Block and loot drops work as intended on Minecraft 26.3** (NeoForge, Fabric and Forge). Minecraft
  26.3 changed the loot-table file format and silently ignores the old one, so conditions and drop
  modifiers (explosion survival, door/slab block-state checks, drop counts, random chances) were skipped.
  The 26.3 builds now ship every loot table in the new format. 26.1.2 and 26.2 were not affected.
- **Breaking a Singularity Vault no longer deletes what it holds.** A vault holding up to 64 stacks
  now spills them as normal items. A fuller vault drops as a single vault item that keeps its whole
  store (the tooltip shows what is inside); place it again to get everything back. The vault's
  tooltip, Tech Guide step and wiki page no longer tell you to empty it first.

## [0.6.0-beta.1] - 2026-10-11

Quest pack, personal milestone records and cleaner world signals. Requires **Neroland Core 1.15.0**
(player gates by UUID, sourced threshold crossings). No new dependency: the quest pack is inert data.

### Added

- **NeroQuests quest pack** — chapter `nerotech:industrial_record` "Industrial Record", 19 quests
  (16 player, 3 server world-log) — that appears in the quest book when NeroQuests is installed; inert
  otherwise, no dependency in any form. The player line runs components → generator →
  fabricator → ore processor → solar/wind → grid → scrubber and remediator → heat and gas → fusion
  parts → stellar fuel → collider parts → antimatter, plus four milestone quests (below). Machine
  outputs are `collect_item` objectives, never crafts.
- **Milestone record gates** (`data/nerotech/neroland_gates/`, player scope, no `requires`, checked by
  nothing): `nerotech:fusion_online` (the Fusion Reactor shell forms), `nerotech:first_collision`
  (the Collider lands its first product), `nerotech:remediated` (a Remediator actually removes
  pollution) and `nerotech:grid_scale` (a Grid Controller's summed machine capacity reaches the new
  `gridScaleMilestoneCapacity`, default 5,000,000 NE; `0` disables). A machine opens them for its
  stored owner, online or offline (`progression/MilestoneGates`); an unowned machine or an owner who
  opted out of attribution records nothing.
- **`nerotech:hook/tech_guide_unlock`** hook gate + `progression/QuestHooks`: when a quest mod opens
  it for a player, NeroTech marks that player's next incomplete Tech Guide entry complete and says
  which in chat, then closes the gate to re-arm it. Information only — NeroTech advancements carry no
  recipe or item rewards. Delivered on next login when it opened while offline.
- **`nerotech:pollution_decay`** threshold channel: a region that drifts back under
  `pollutionEventThreshold` through the periodic decay sweep now fires here (falling, scope = region
  key). `nerotech:pollution` falling is reserved for active Scrubber/Remediator removal, so a listener
  can tell cleanup from waiting.
- `NeroTechCommon.serverTick(MinecraftServer)`: the loaders' end-of-tick fan-out (pollution sweep +
  quest-hook delivery).

### Changed

- **`nerotech:machine_failure` scope is now `machineId@dimension`** — the block position is gone.
  On a shared server a base's coordinates identify its builder; listeners filter on the stage
  (`value`/`threshold`) and `rising`, never on the scope. `MachineFailureEvents.fire(...)` and
  `scope(...)` keep their 0.4/0.5 signatures (the `BlockPos` is ignored) and gain position-free
  overloads.
- Every NeroTech threshold crossing is published with `nerotech` as its source mod (Core 1.15.0
  `ThresholdEvents.fire(String, ThresholdCrossing)`).
- Version `0.6.0-beta.1`; Core pin `1.13.0` → `1.15.0`.

### Privacy

- The milestone and hook gates are rows in Neroland Core's progression store (UUID-keyed, covered by
  Core's privacy notice and erased through Core's shared erasure hook). NeroTech writes them only
  for a machine's stored owner and never logs or broadcasts the UUID. `PRIVACY.md` updated.

## [0.5.0-beta.1] - 2026-10-11

Optional NeroEconomy operating charges. Requires **Neroland Core 1.13.0** (unchanged). NeroEconomy stays
optional: NeroTech builds, loads and runs without it.

### Added

- **Per-operation machine charges** (`compat/neroeconomy`). With NeroEconomy 0.3.0+ installed and
  `machineOperationChargeCost` above 0 (default `0` = off), an owned Ore Processor, Fabricator, Advanced
  Ore Processor, Advanced Fabricator or Chemical Processor charges its owner in advance for each batch
  of `machineOperationChargeBatch` operations (default 64), under NeroEconomy caller `nerotech`, reason
  `machine_operation`. Charges sink into the Reserve; a machine never pays out. A refused charge idles
  the machine with the new **Unpaid** analytics status, showing NeroEconomy's reason, and is retried
  every 30 seconds — never per tick. An unowned machine is never charged. With an older NeroEconomy the
  bridge falls back to Core's `CurrencyApi.withdraw` and checks its result.
- `MachineStatus.UNPAID` (appended; ordinals unchanged) and an optional status detail lang key on the
  analytics payload.
- `IPlatformHelper.isModLoaded(String)` on every loader.
- NeroEconomy `compileOnly` dependency (`neroeconomy_version=0.3.1-beta.1`, never bundled); optional in
  every loader manifest (`suggests` / `type = "optional"` / `mandatory = false`, floor `0.3.0-beta.1`).
  Its GitHub Packages repository is declared only when credentials are present.

### Changed

- Machines also record the placing player's UUID as owner while operating charges are enabled (before:
  only with `pollutionPerPlayerAttribution`). Same storage and erasure path; see `PRIVACY.md`.
- The machine block-entity update tag sent to clients no longer carries the owner's UUID.

### Notes

- No processing recipe type or ore/scrubber bonus changed in this release. Iron, copper and gold dust
  already carry their `c:dusts/*` tags.

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
