# Material Map

Every material NeroTech 0.8.0 adds, where it comes from, what it makes and the tag other mods use to
take it. Each material has two routes:

- **Offworld** — with Nerospace installed. Recipes take Nerospace resources by tag, so they run
  cheaper and faster.
- **Standalone** — on a server with only Neroland Core and NeroTech. Every material can still be made
  from Overworld and Nether resources, at roughly three to five times the energy or time.

Recipes always name a tag, never another mod's item, so any mod that puts its own material under the
same tag works too. A recipe viewer (JEI or EMI) shows the exact numbers.

## The six chains at a glance

| Chain | Offworld source | Product | Standalone source |
|---|---|---|---|
| Structural | Cindrite with hematite or iron dust | Cindersteel ingot, plate, rod; titanium plate | Iron dust, coal and blaze powder in the Plasma Crucible |
| Superconductor | Glacite, packed cryo ice | Superconductor wire and coil | Synthetic Glacite grown from Void Crystal, snow and packed ice |
| Semiconductor | Xertz quartz, brine salt | Crystal Boule, Wafer, Logic and Quantum Processors | Nether quartz |
| Optics | Voidglass, crystal shards, cindrite, glacite | Four focus lenses | Void Crystal, blaze powder, Synthetic Glacite, quartz |
| Chemistry | Vent sulfide, Cindara sulfur, methane crystal, amber sap, dry ice | Sulfuric acid, polymer resin and sheet, carbon dioxide, methane | Gunpowder, logs, coal, hydrogen, Overworld and Nether air |
| Fusion fuel | Deuterium shards and cells, helium-3 canisters, raw europium | Deuterium, helium-3, Europium Dust, Europium Flux Lattice | Electrolyzer hydrogen, Centrifuge cascades, glowstone dust |

## Structural

### Cindersteel Ingot (`c:ingots/cindersteel`)

- **Offworld:** cindrite and iron dust in the Plasma Crucible, or cindrite with iron dust or raw
  hematite in the Blast Foundry (bulk).
- **Standalone:** two iron dust, one coal and one blaze powder in the Plasma Crucible, or the same in a
  heated Blast Foundry. Slower, so tier-2 machines are not locked behind the Particle Collider. Every
  ingot takes one blaze powder, so a standalone server wants a blaze farm.
- **Makes:** Cindersteel Plate and Rod (Plate Press), Cindersteel Wire (Filament Spooler), the Europium
  Flux Lattice, the Fuel Injector.
- **For other mods:** a tier-2 structural metal for hulls, frames and tool bodies. NeroPower reactor and
  battery casings, NeroLogistics drone frames, NeroColonies building parts, NeroCompanion chassis.

### Cindersteel Plate (`c:plates/cindersteel`) and Cindersteel Rod (`c:rods/cindersteel`)

- **From:** a Cindersteel Ingot in the Plate Press with the Plate Die or the Rod Die.
- **Makes:** plates go into the Blast Foundry, Deep Core Drill, Heat Pipe, Survey Scanner, Servo,
  Capacitor and Heat Sink; rods go into the Drill Derrick, drill heads and the Cindersteel Coil.
- **For other mods:** NeroPower generator housings, NeroLogistics rails and racks, NeroColonies
  structures, NeroCompanion frames.

### Titanium Plate (`c:plates/titanium`)

- **Offworld only:** a titanium ingot (smelted from Luna ilmenite) in the Plate Press with the Plate
  Die.
- **Standalone:** none. Titanium is an optional material: everything that takes it has a cindersteel
  version.
- **Makes:** two Heat Sinks, where cindersteel plate makes one.
- **For other mods:** a light alternative plate for NeroCompanion drones and NeroLogistics drones.

### Acid ore leaching

The Reaction Vat soaks one raw iron, copper or gold ore (or one raw hematite) in 500 mB of sulfuric
acid and returns **four** dusts: one more than the Chemical Processor or the Advanced Ore Processor.

## Superconductor

### Synthetic Glacite (`c:gems/glacite`)

- **Offworld:** not needed. Glacite is mined on Glacira and carries the same tag.
- **Standalone:** one Void Crystal, four snow blocks and two packed ice, grown in the Crystal Grower in
  100 mB of cryo coolant, **four** Synthetic Glacite out. It works anywhere a Crystal Grower does, and
  faster in the cold. No Silk Touch is needed: the Cryo Distiller makes packed ice from two snow blocks
  and one nitrogen. On a server without Nerospace the Void Crystal comes from the Particle Collider
  (an echo shard and an amethyst shard make Void Crystal dust, which smelts into the crystal), so echo
  shards from Ancient Cities are the real limit.
- **Makes:** everything glacite makes: Superconductor Wire, the Glacite Focus Lens, the Glacite Drill
  Head.
- **For other mods:** any recipe that takes `c:gems/glacite` accepts it.

### Superconductor Wire (`c:wires/superconductor`)

- **From:** two glacite (or Synthetic Glacite) and two Cindersteel Wire in the Filament Spooler, two
  wires out.
- **Makes:** the Superconductor Coil, the Quantum Processor, the Glacite Drill Head, the Europium Flux
  Lattice.
- **For other mods:** loss-free power links. NeroPower beamed power and grid parts, NeroLogistics
  network cabling.

### Coils (`nerotech:coils/tier1`, `nerotech:coils/tier2`, `nerotech:coils/tier3`)

- **Tier 1:** Nero Coil and Copper Coil. **Tier 2:** Cindersteel Coil (four Cindersteel Wire around a
  Cindersteel Rod). **Tier 3:** Superconductor Coil (four Superconductor Wire around a tier-2 coil,
  with a Heat Sink, cooled with 250 mB of cryo coolant in the Component Forge).
- **Makes:** the Copper Coil goes into the Servo; the Cindersteel Coil into the Superconductor Coil,
  the Deep Core Drill and the Heat Exchanger; the Superconductor Coil into the Fuel Injector and the
  Cutting Lance.
- **For other mods:** coil tiers for NeroPower generators and chargers, NeroCompanion charging bays.

### Cryo coolant (fluid, `nerotech:cryo_coolant`)

- **Offworld:** packed cryo ice or dry ice in the Cryo Distiller (with a little nitrogen), frost shards
  in the Cryo Distiller, or a brine bucket with blue ice and nitrogen in the Reaction Vat.
- **Standalone:** four snow blocks and water with nitrogen (from the Atmospheric Condenser) or carbon
  dioxide in the Reaction Vat. Snow comes off with any shovel, so no Silk Touch is needed.
- **Makes:** the Superconductor Coil, Synthetic Glacite, the Europium Flux Lattice (Component Forge
  route); it is also a machine coolant.
- **For other mods:** NeroPower reactor coolant loops.

## Semiconductor

### Crystal Seed (`nerotech:crystal_seeds`)

- **From:** an amethyst shard and a quartz (xertz or nether), crafted, two seeds out.
- **Makes:** a Crystal Boule.

### Crystal Boule (`nerotech:crystal_boules`) and Wafer (`nerotech:wafers`)

- **Offworld:** a seed and two xertz quartz doped with one brine salt, or a seed and four xertz quartz,
  grown in water in the Crystal Grower.
- **Standalone:** a seed and four nether quartz, grown in water.
- **Makes:** a boule presses into four Wafers (Plate Press, Wafer Die); wafers are etched into
  processors.
- **For other mods:** blank wafers for NeroLogistics and NeroCompanion electronics.

### Logic Chip (`nerotech:processors/logic`) and Quantum Processor (`nerotech:processors/quantum`)

- **Logic:** a Wafer and two copper wire with the Quartz Focus Lens, or a Wafer and two cindersteel
  wire with the Ember Focus Lens (Laser Etcher).
- **Logic** with the Ember Focus Lens is the fast one: half the time of the Quartz Focus Lens.
- **Quantum:** a Wafer, a Logic Chip and two Superconductor Wire with the Void Focus Lens, or two
  Wafers and two Superconductor Wire with the Glacite Focus Lens (no Logic Chip, slower).
- **Makes:** Logic goes into Quantum, the Survey Scanner, the Cutting Lance and the Heat Sink and Scrubber modules; Quantum goes into the Deep Core Drill, the Fuel Injector
  and the Europium Flux Lattice (Component Forge route).
- **For other mods:** controller parts. NeroPower reactor control, NeroLogistics routing,
  NeroCompanion chips, NeroColonies automation.

## Optics

### Focus lenses (`nerotech:focus_lenses`)

A lens sits in the Laser Etcher's lens slot and is never used up; it decides what can be etched.

| Lens | Offworld | Standalone | Etches |
|---|---|---|---|
| Quartz | four xertz quartz and a glass pane | four nether quartz and a glass pane | Logic Chip |
| Ember | four cindrite and a glass pane | four blaze powder, two quartz and a glass pane, slowly | Logic Chip from cindersteel wire |
| Glacite | four glacite and a glass pane | four Synthetic Glacite and a glass pane | Quantum Processor from superconductor wire |
| Void | two voidglass or crystal shards and a glass pane | two Void Crystal and a glass pane, slowly | Quantum Processor |

All grow in the Crystal Grower in water. **For other mods:** optics parts for NeroPower beamed power
and NeroCompanion sensors.

## Chemistry

### Sulfur Dust (`c:dusts/sulfur`)

- **Offworld:** Cindara sulfur carries the same tag, or raw vent sulfide in the Centrifuge (two sulfur,
  sometimes iron dust).
- **Standalone:** two gunpowder in the Centrifuge.
- **Makes:** sulfuric acid. **For other mods:** any recipe that takes `c:dusts/sulfur`.

### Sulfuric acid (fluid, `c:sulfuric_acid`)

- **From:** sulfur, water and oxygen (from the Electrolyzer) in the Reaction Vat.
- **Makes:** acid ore leaching, the Capacitor.
- **For other mods:** NeroPower battery electrolyte.

### Polymer resin (fluid, `c:polymer_resin`) and Polymer Sheet (`nerotech:polymer_sheets`)

- **Offworld:** amber sap and methane in the Reaction Vat.
- **Standalone:** two logs and methane in the Reaction Vat.
- **Makes:** resin presses into Polymer Sheet (Plate Press, Plate Die); sheets go into the Capacitor
  and the Scrubber Module.
- **For other mods:** insulation and casings. NeroLogistics cable jackets, NeroCompanion shells.

### Gases

| Gas | Offworld | Standalone | Used in |
|---|---|---|---|
| Nitrogen | Atmospheric Condenser on Glacira, Greenxertz or Nyxara; Cryo Distiller on cryo ice | Atmospheric Condenser in the Overworld, slowly | Cryo coolant, packed ice |
| Carbon dioxide | dry ice in the Cryo Distiller; the Condenser on Mars or Cindara | coal burned with oxygen in the Cryo Distiller; the Condenser in the Nether | Cryo coolant, calcite |
| Methane | methane crystal in the Cryo Distiller | coal with hydrogen in the Cryo Distiller | Polymer resin, Gas Turbine fuel |

## Fusion fuel

### Deuterium and helium-3 (gases)

- **Offworld:** deuterium shards or cells in the Cryo Distiller (four units); helium-3 canisters in the
  Cryo Distiller (two units).
- **Standalone:** deuterium from Electrolyzer hydrogen and a little water in the Centrifuge, one unit per
  unit of hydrogen. Helium-3 from a **cascade** of three Centrifuges side by side, facing the same way:
  three deuterium into the first gives two, those two into the second give two, and those two into the
  third give one helium-3.
- **Makes:** the gas-fed fuel tier through the Fuel Injector.
- **For other mods:** NeroPower fuel for its own reactors.

### Europium Dust (`c:dusts/europium`)

- **Offworld:** raw europium from Europa's vent fields in the Centrifuge (sometimes gold dust too).
- **Standalone:** eight glowstone dust in the Centrifuge, slowly. Europium is the red glow in phosphors.
- **Makes:** the Europium Flux Lattice, by crafting (one) or in the Component Forge with a Quantum
  Processor, superconductor wire and cryo coolant (four).
- **For other mods:** NeroPower reactor control rods.

## Components

| Component | Tag | Made in | Goes into |
|---|---|---|---|
| Servo | `nerotech:components/servos` | Component Forge | Deep Core Drill, Power Drill |
| Capacitor | `nerotech:components/capacitors` | Component Forge, with sulfuric acid | Survey Scanner, Power Drill, Filter Mask, Thermal Goggles |
| Heat Sink | `nerotech:components/heat_sinks` | Component Forge, from cindersteel plate, or titanium plate (two out) | Superconductor Coil, Heat Sink Module |

**For other mods:** NeroPower, NeroLogistics, NeroColonies and NeroCompanion can take any of these by
tag for their own machines.
