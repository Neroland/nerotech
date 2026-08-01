# Changelog

All notable changes to **NeroTech** are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- **Conveyor Belt** — the cheapest automation in the mod and the only NeroTech block with **no block
  entity at all**: a flat 4-pixel directional plate that nudges item entities along its facing at
  ~0.15 blocks/tick, capped so a chain never accelerates an item past belt speed. No power, no GUI,
  no filter, no merging — it applies motion and nothing else, so lines and corners form naturally
  from blocks that each only push their own way. Only item entities are affected; players and mobs
  walk over it. Crafts **6 at a time** from 3 Iron Dust + 2 Nero Coils + Redstone — the mod's main
  Iron Dust sink.
- **Robotic Arm** — a one-block item mover: once a second it lifts up to `roboticArmStackPerMove`
  (default 8) items out of the container **behind** it into the container **in front** of it, for
  `roboticArmNePerMove` (default 4) NE **per item moved**. Both ends are addressed through the same
  standard container surface a hopper uses, through the face pointing at the arm, so machine side
  configs, furnace sidedness and NeroTech's own per-face routing are all honoured on every loader.
  A single GUI-only **filter slot** restricts the move to one item type; it is never consumed and no
  face ever exposes it to automation. Two direct neighbour lookups on a phase-spread 20-tick
  cadence — never a world scan.
- **Configurator copy/paste, extended** — copying a machine now captures its **overclock preset**
  alongside its side config, and pasting is **no longer type-locked**: each copied channel is applied
  wherever the target declares that same channel, and the preset always applies. Stamp one
  processor's Item+Power layout and its Overdrive setting across a row, then keep going and drop just
  the Power layout and preset onto the generators feeding it. A paste with nothing compatible now
  reports the mismatch instead of silently doing nothing. Server-side mutation only; the clipboard
  lives on the item stack and holds routing modes, a preset ordinal and a machine-type id — never any
  player data.
- **Antimatter Cell** — **tier-4 fusion fuel**, and the only item the Particle Collider alone can
  make (Stellar Cell in, Antimatter Cell out; there is no crafting recipe). It burns for
  `fusionFuelTier4BurnTicks` (default 28,800 ticks — twice a Stellar Cell), and it is contained by
  the **7×7×7 shell only**: the maximal shell is the one that reaches a tier above its own, which is
  the entire reason tier 4 is reachable. While it burns the reactor's heat rate gains a flat **+2**
  on top of the shell scaling, so a 7³ reactor on antimatter is both the strongest generator in
  NeroTech and the one most likely to melt down. Tagged into the new
  `#nerotech:fusion_fuel/tier4` (and the plain `#nerotech:fusion_fuels`).
- **Singularity Vault** — bulk storage for **one** item type, up to `singularityVaultCapacity`
  (default 1,000,000), held as a virtual store (type + count) rather than as stacks. Automation sees
  a two-slot facade: slot 0 is drained into the store each tick while the type matches, slot 1 is
  kept topped up with a full stack so extraction never stalls however deep the store is. By hand:
  right-click deposits, crouch-right-click withdraws a stack, empty-handed right-click reports the
  fill; a fully drained vault forgets its type and can be re-assigned. A **comparator** reads fill as
  a fraction of capacity. It draws **no power** and is exempt from Grid Controller load shedding.
  Crafted from 4 Void Crystal + 4 Obsidian around an Antimatter Cell, which puts it behind both
  multiblocks. **Empty it before you break it** — the stored bulk does not drop.
- **Accelerator transmutation** — two lossy `nerotech:collider` recipes: **Copper Dust ×2 → Iron
  Dust** (800 J) and **Iron Dust ×2 → Gold Dust** (1,500 J). They pay their loss in items *and* in the
  run it takes to wind the beam up, so transmuted gold is the most expensive gold in the game. Both
  appear automatically in the JEI accelerator page.
- **Tech Guide chapter: Exotic Matter** — three steps (widen the accelerator ring until its stretches
  run eleven blocks or more, collide two Stellar Cells into antimatter, build a Singularity Vault)
  closing the guide's arc, plus the **Contained Annihilation** and **Room for Everything**
  advancements behind *Breaking Matter Open*.
- **New config keys** — `roboticArmNePerMove`, `roboticArmStackPerMove`, `singularityVaultCapacity`,
  `fusionFuelTier4BurnTicks`, and the accelerator set: `acceleratorMaxGap`, `acceleratorMaxGuides`,
  `acceleratorTickScale`, `acceleratorLaunchSpeed`, `acceleratorBoostPerGuide`,
  `acceleratorNePerGuide`, `acceleratorDragPerGuide`, `acceleratorMinGapAllowance`,
  `acceleratorGapPerSpeed`, `acceleratorBendSpeedBase`, `acceleratorEnergyScale`.
- **New wiki page** — [Exotic Endgame](wiki/Exotic-Endgame.md), with the Conveyor Belt and Robotic
  Arm documented in [Automation](wiki/Automation.md), the transmutation and antimatter recipes in
  [Particle Accelerator](wiki/Particle-Collider.md), tier 4 in the
  [Fusion Reactor](wiki/Fusion-Reactor.md) fuel table, and the cross-type paste in
  [Side Config & the Configurator](wiki/Side-Config-and-Configurator.md).

- **Particle Accelerator** — a new endgame build and NeroTech's **standalone** route to space-grade
  materials, and a **free-form** one: no multiblock, no fixed footprint. **Accelerator Guide Coils**
  are laid at the **Accelerator Controller**'s own Y level in any shape at all, each right-clicked
  (empty hand or the Configurator) to cycle its `bend` blockstate **straight / 45° left / 45° right**;
  the controller ray-marches its facing heading up to `acceleratorMaxGap` blocks per hop, follows the
  bends, and if the line comes back into itself it has a **closed loop**. Guides need not touch and
  need not be square. A virtual particle (server-side state, no entity, drawn as a vanilla END_ROD
  streak so you can watch it lap) is injected from the first slot and boosted at every guide.
  Three rules — implemented as pure, unit-tested maths in `machine/AcceleratorMath` — make ring
  GEOMETRY the progression axis: the **gap rule** (a stretch too long for the current speed loses the
  particle; inverted, it sets the injection speed), the **bend rule** (a 45° turn survives at most
  `acceleratorBendSpeedBase` × the run-up before it, so the shortest bend stretch caps the loop), and
  **collision energy** `E = 0.5·v²·acceleratorEnergyScale`. Put a second item in the collision slot
  and every lap through the controller attempts the collision. The `nerotech:collider` recipe type is
  now its own `ColliderRecipe` class — **two order-free ingredients plus a `min_energy` floor** — so a
  recipe's energy requirement is really a minimum ring size: 800 J and 1,500 J for the dust
  transmutations (an 11×11 octagon), 3,000 J for **Starsteel Dust** (Netherite Scrap + Iron Dust) and
  **Void Crystal Dust** (Echo Shard + Amethyst Shard), and 12,000 J for the **Antimatter Cell** (two
  Stellar Cells — a 29×29 ring). Both blocks are still crafted from vanilla materials and Tier-1
  NeroTech components only, so the advanced tier (Fusion Reactor included) is reachable on Earth
  alone. The ring also **reads itself back**: after every trace the controller writes each guide's
  outgoing beam direction into a display-only `heading` blockstate, and the coil's top face lights up
  with a cyan arrow — one of 24 hand-painted tops (3 bends × 8 compass headings, the bend's 45° kink
  drawn in) — so you can walk a loop and see the beam's route. A coil with no arrow is not part of a
  closed line, which makes the arrows the first debugging tool for a ring that will not close. The
  writes are compare-before-set and carry no neighbour update, so a settled ring costs nothing to
  re-trace and the arrows never feed back into the physics. Ships with a dedicated two-input JEI page,
  a Tech Guide step, an advancement, and a rewritten
  [Particle Accelerator](wiki/Particle-Collider.md) wiki page. *Mechanic inspired by Oritech's
  particle accelerator; clean-room implementation.*
- **Power tier** — six new blocks that absorb the whole feature set once planned for a separate
  **NeroPower** mod. Three generators: the **Wind Turbine** is fuel-free and works day *and* night —
  `windTurbineNePerTick` (default 25) scaled by an altitude curve (0.5× at or below y=80 rising
  linearly to 2× at or above y=200) and a per-dimension multiplier (`windDimensionMultipliers`),
  needing only sky access directly above (an airless Nerospace planet gives zero: no atmosphere, no
  wind); the **Geothermal Generator** counts lava/magma in the 3×3 layer directly beneath it (0–9)
  for `geothermalNePerTickPerSource` (default 8) NE/tick each — perfectly steady, runs hot, no
  pollution, with the count cached and re-checked on neighbour change or every 100 ticks; the **Bio
  Generator** burns anything in the new datapack-overridable `#nerotech:bio_fuels` item tag for 4,000
  ticks at `bioGeneratorNePerBurnTick` (default 48) NE/tick — 20% above the Nero Generator at **half**
  the pollution per operation. Plus three grid blocks: the **Battery Bank**, a single-block
  `batteryBankCapacity` (default 1,000,000) NE buffer with every-face I/O and auto-eject (Core's
  STORAGE preset, FE interop via `EnergyLookup`); the **Grid Controller**, a passive console that
  rescans `gridControllerRadius` (default 16) every 100 ticks and, below `gridShedThresholdPermille`
  (default 200), drops every non-generator machine to the Eco preset until fill recovers past
  `gridRestorePermille` (default 500) — deliberate hysteresis so a grid at the line does not flap,
  with generators, Battery Banks, Wireless Nodes and consoles exempt (it throttles demand, never
  supply); and the **Wireless Power Node**, paired with the Configurator by crouch-use, passing up to
  `wirelessNodeTransferPerTick` (default 200) NE every 5 ticks to a partner within
  `wirelessNodeRange` (default 32) blocks in the same dimension — **lossless**, never force-loading a
  chunk, and self-unlinking when either end breaks. All six craft from vanilla and Tier-1 NeroTech
  materials only. Ships with the **Charge Reserve** advancement (craft a Battery Bank) and the new
  [Power Generation](wiki/Power-Generation.md) wiki page.
- **Analytics Terminal power history** — the dashboard now keeps a rolling 60-sample window of the
  *net change* in aggregate stored NE across the machines it watches, one sample per 100-tick rescan
  (the last five minutes), rendered as a bar sparkline: green above the midline where the watched
  machines gained charge, red below where they drained it, self-scaling to the window's peak. The
  machine list drops from 12 rows to the **10** nearest to make room.
- **New config keys** — `windTurbineNePerTick`, `windDimensionMultipliers`,
  `geothermalNePerTickPerSource`, `bioGeneratorNePerBurnTick`, `batteryBankCapacity`,
  `gridControllerRadius`, `gridShedThresholdPermille`, `gridRestorePermille`, `wirelessNodeRange`,
  `wirelessNodeTransferPerTick`.
- **`#nerotech:bio_fuels` item tag** — datapack-overridable Bio Generator feedstock, seeded with
  `minecraft:dried_kelp_block` so NeroAgriculture and packs can add their own crops without a code
  change.
- **Fluid & gas chain** — Neroland Core's dormant fluid/gas substrate is now wired up, and NeroTech
  brings the first concrete gases (`nerotech:hydrogen`, `nerotech:oxygen`; Core ships none by
  design). Three new machines: the **Electrolyzer** splits stored water into hydrogen and oxygen at
  the 2:1 ratio (water in by bucket *or* through Core's fluid capability — no item slots at all) and
  hands both products to adjacent gas blocks once a second; the **Gas Turbine** burns gas into NE and
  pushes it like the Nero Generator, with **mild heat and zero pollution** — clean power, not free
  power (electrolysis is a net energy sink by design); the **Chemical Processor** washes raw ore with
  oxygen for **3** dust where the Ore Processor gives 2, via the new datapack-driven
  `nerotech:chemical_processing` recipe type. Every tank is exposed on Core's shared
  `nerolandcore:fluid` / `nerolandcore:gas` capabilities on all six cells, so Core's Fluid Tank and
  Gas Tank — and any other mod on those surfaces — interoperate with no cross-mod dependency. Which
  gases the turbine burns is config-driven (`turbineGasBurn`, default `nerotech:hydrogen=2`). Ships
  with a JEI page, an advancement, and the [Fluids & Gases](wiki/Fluids-and-Gases.md) wiki page.
- **Coolant loop** — active cooling for reactors and colliders, as two blocks rather than a plumbed
  circuit (a deliberate simplification: same lever, none of the network bookkeeping). The
  **Radiator** is a passive block that counts as four natural coolant blocks against a machine and
  never melts; the **Coolant Pump** spends NE each thermal-exchange interval to pull heat out of
  every adjacent machine and delete it, at a rate scaled by how many Radiators sit within 3 blocks in
  a straight line. Both the radiator scan and the machine scan are event-driven and cached — no
  per-tick sweeps. The pump is slotless and menu-less; its block-item tooltip explains the scaling.
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

### Changed

- **`/nerotech gallery` now demonstrates the full 0.2.0 lineup** — the creative-only showcase gains
  five more spokes, each running the way survival would wire it rather than posed: a **closed
  octagonal Particle Accelerator** (eight RIGHT guide coils, sized so the beam reaches ~1045 J on its
  third lap and transmutes Copper Dust into Iron Dust for ever without crashing a bend) with its own
  Coolant Pump tower; the **gas chain** — an Electrolyzer flanked by a Gas Turbine burning its
  hydrogen and a Chemical Processor washing raw iron with its oxygen; a **coolant loop** (pump plus
  two Radiators) draining the 5×5×5 Fusion Reactor; a **power park** with a Wind Turbine on a
  15-block mast, a Geothermal Generator over a walled 3×3 lava basin, a Bio Generator, a Battery Bank
  taking both their pushes, a Grid Controller and a **linked Wireless Node pair** powering an Ore
  Processor ten blocks away with no cable; and an **automation lane** — a Conveyor Belt run with a
  corner carrying live item entities, a Robotic Arm shuttling chest to chest, and a Singularity Vault
  holding 10 000 Iron Dust. `gallery clear` covers the enlarged footprint, including the mast and the
  lava basin (removed without letting the lava flow). The command still records nothing about the
  player.
- **The NeroPower split is retired** — as of 2026-07-31, NeroPower will not ship as a separate mod
  and its planned feature set is absorbed into NeroTech (see the power tier above). One mod, one
  power network, no cross-mod dependency to reason about. Generation still talks only to Core's
  energy surface — kept because it is the right coupling, not to keep an extraction cheap. The wiki
  page [Power & the NeroPower Split](wiki/Power-and-NeroPower-Split.md) is rewritten around the
  decision, with the old split criteria preserved as a clearly-marked archived section.

**Stage A — foundation cleanup & recipe graph**

- **Capability registration is now generic** — `ModBlockEntities` publishes the energy and item
  machine lists, and the Fabric/NeoForge entry points iterate them instead of hand-listing types. A
  new machine is wired on every loader by adding it to one list. The Analytics Terminal and Tech
  Guide stay out by design (zero NE, no slots).
- **Unit tests run on all six cells** — the shared `common/src/test/java` suite is now wired into the
  Fabric and Forge nodes too, not just NeoForge, so `build`/`check` runs it everywhere.
- **Dust smelts back to metal** — Iron, Gold and Copper Dust each gain smelting *and* blasting
  recipes to the matching vanilla ingot at vanilla times/XP, closing the *ore → dust → ingot* loop.
- **Machines beat the workbench** — the Fabricator now returns **2** Circuit Boards / Nero Coils per
  dust (crafting still gives 1); the Machine Frame recipe already cost 1 iron in the machine versus 8
  by hand and is unchanged.
- **Fusion Cells are machine-exclusive** — the shaped crafting recipe is removed; the Advanced
  Fabricator is the only source. Plasma and Stellar Cells keep their crafting recipes (they are
  multi-ingredient, and `advanced_fabricating` takes a single input).
- **Bulk raw-ore-block processing** — raw iron/gold/copper blocks process into 18 dust, on top of the
  Advanced Ore Processor's yield bonus. Its Tech Guide/advancement text no longer over-promises
  "Starsteel and planet ores".
- **Just Enough Items support** — a shared JEI plugin adds Ore Processing, Fabricating and Advanced
  Fabricating pages with each machine as a crafting station. Because 26.x clients hold no full recipe
  list, NeroTech now opts into each loader's recipe sync (recipe definitions only — no player data).
- **Three new advancements** — Tech Guide Datapad, Plasma Cell and Filter Cartridge.

### Fixed

- **Nerospace is no longer a build requirement** — CI failed on every cell because the planet-trait
  compat compiled against `za.co.neroland.nerospace:nerospace-<loader>-<mc>` (`compileOnly`), an
  artifact that only ever existed in the developer's local Maven. `NerospacePlanetCompat` is now
  **pure reflection**: the api facade is resolved by name at runtime, absent-Nerospace returns
  empty (config tables keep authority), and api drift degrades softly with a debug log instead of a
  crash. The dependency, its `nerospace_version` pin and the Maven wiring are gone — NeroTech now
  builds from a bare clone with no sibling repos published anywhere. Runtime behaviour is unchanged
  (planet-aware solar/wind/thermal when Nerospace is installed).
- **Gallery labels hijacked pick-block** — the invisible armor stands `/nerotech gallery` spawns as
  floating labels kept a full hitbox, so middle-clicking a machine they overlapped returned an Armor
  Stand item. Labels are now marker stands (zero-size hitbox — unpickable, unhittable; the flag is
  applied via `Entity#load` because `ArmorStand#setMarker` is private in 26.x). Run
  `/nerotech gallery clear` and rebuild to replace stands from an older gallery.
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
