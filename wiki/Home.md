# NeroTech Wiki

Player- and contributor-facing documentation for **NeroTech**, part of the **Neroland ecosystem**
of sci-fi mods. Built on **Neroland Core**.

> Requires **Neroland Core 1.24.0 or newer** (below 2.0). Minecraft 1.20 through 1.20.6 runs on Fabric, 1.20 through 1.20.4 and 1.20.6 on Forge, and 1.20.4 through 1.20.6 on NeoForge. The NeoForge build for 1.20.5 uses loader 20.5.21-beta.
> These versions use the in-game Tech Guide for recipe help.
> Offworld Industry adds six production chains, 22 new
> machines and blocks, a component ladder, seven powered tools, virtual deposits, heat you can pipe, a
> gas-fed fusion tier, a NeroTech module in the NeroLink app and a walkable gallery of all of it.
> NeroTech plays fully standalone with Neroland Core; Nerospace is optional and makes every chain
> cheaper.

## Getting started

- [Standalone Progression](Progression.md) — the full path from first components to antimatter with
  only Neroland Core installed: every recipe from vanilla materials, no gates.
- [Tech Guide](Tech-Guide.md) — the in-game handbook, now 18 chapters from first power to the gas-fed
  fusion tier and the tools.
- [Materials & Components](Materials-and-Components.md) — Tier-1 crafting components and
  Earth-metal processing dusts, with their `c:` tags.
- [Making Dust](Dusts.md) — every way to make dust: Ore Processor, Chemical Processor, Advanced
  Ore Processor, acid leaching, Dirty Filter recycling and the Particle Accelerator.
- [Machines](Machines.md) — the Tier-1 machines, power and upgrade modules, the 0.8.0 roster, the
  Induction Charger and Annunciator Panel, and gauge tooltips.
- [Power Generation](Power-Generation.md) — the wider power tier: Wind Turbine, Geothermal and Bio
  Generators, Battery Bank, Grid Controller, Wireless Power Node.

## Offworld Industry (0.8.0)

- [Material Map](Material-Map.md) — every 0.8.0 material, both routes, and the tags other mods use.
- The six chains: [Structural](Chain-Structural.md), [Superconductor](Chain-Superconductor.md),
  [Semiconductor](Chain-Semiconductor.md), [Optics](Chain-Optics.md), [Chemistry](Chain-Chemistry.md),
  [Fusion fuel](Chain-Fusion-Fuel.md).
- [Metallurgy](Metallurgy.md) — Plasma Crucible, Plate Press and its dies, Filament Spooler, Blast Foundry.
- [Chemistry and Cryogenics](Chemistry-and-Cryogenics.md) — Centrifuge cascades, Cryo Distiller,
  Atmospheric Condenser, Reaction Vat, Process Tank, and the new fluids and gases.
- [Components](Components.md) — the component ladder, Component Forge, Laser Etcher and lenses, Crystal
  Grower.
- [Heat as a Resource](Heat-as-a-Resource.md) — Heat Pipes, the Heat Exchanger and heat loops.
- [Deposits and Drilling](Deposits-and-Drilling.md) — virtual deposits, the Deep Core Drill and the Survey
  Scanner.
- [Fusion Fuel](Fusion-Fuel.md) — Fuel Injectors and Europium Flux Lattices.
- [Tools](Tools.md) — Power Drill, Cutting Lance, Arc Wrench, Multimeter, Survey Scanner, Thermal
  Goggles and Filter Mask.

## Systems

- [Thermal System](Thermal-System.md) — conduction between machines, dimension/biome ambient,
  coolants, and throttling.
- [Fluids & Gases](Fluids-and-Gases.md) — the Electrolyzer chain (water → hydrogen + oxygen), the
  Gas Turbine, the Chemical Processor's oxygen wash, and the Radiator/Coolant Pump loop.
- [Consequence Systems](Consequence-Systems.md) — heat and regional pollution overview, with
  opt-in per-player attribution and POPIA/GDPR handling.
- [Pollution & Mitigation](Pollution-and-Mitigation.md) — Scrubber, the Filter Cartridge cycle,
  the Remediator, exposure and the Filter Mask.
- [Analytics](Analytics.md) — the per-machine Analytics tab and the Analytics Terminal dashboard.
- [Operating Charges](Operating-Charges.md) — optional, server-configured credit charges for running
  processing machines (needs NeroEconomy; off by default).
- [NeroQuests Integration](NeroQuests-Integration.md) — the *Industrial Record* quest chapter NeroTech
  ships as inert data, the milestone record gates behind it, and the Tech Guide quest hook.
- [Overclock Presets](Overclock-Presets.md) — Eco / Balanced / Overdrive on every machine.
- [Side Config & the Configurator](Side-Config-and-Configurator.md) — per-face I/O and the wrench.
- [Recipe Viewers (JEI & EMI)](Recipe-Viewers.md) — the machine recipe pages on every loader.

## Advanced & automation

- [Advanced Tier](Advanced-Tier.md) — the Starsteel-gated tier (soft Nerospace coupling via Core
  tags/gates).
- [Particle Accelerator](Particle-Collider.md) — the free-form guide-coil ring: the standalone route
  to Starsteel and Void Crystal dust, where ring size *is* the tier.
- [Fusion Reactor](Fusion-Reactor.md) — the 3³/5³/7³ multiblock: shells, fuel tiers, meltdown and
  containment breach.
- [Automation & Handoff](Automation.md) — Auto Crafter, Item Sorter, Conveyor Belt, Robotic Arm, and
  the standard item-capability handoff surface NeroLogistics routes through.
- [Exotic Endgame](Exotic-Endgame.md) — the Antimatter Cell (tier-4 fusion fuel), the Singularity
  Vault, and collider transmutation.
- [Planets](Planets.md) — how solar output, ambient heat and the Atmospheric Condenser follow
  Nerospace planet traits, with a config fallback.

## Server admins

- [Commands](Commands.md) — `/nerotech budget` and the `/nerotech gallery` showcase and tour.
- [Config](Config.md) — every key in `config/nerotech.properties`, with defaults.

## Companion app

- [NeroLink Companion App](Companion-App.md) — what the NeroTech section of the NeroLink app shows:
  machines near you, the chain planner, your Tech Guide progress and your own pollution.
- [Link Module](Link-Module.md) — the full link schema (version 2) for developers.
- [Privacy](Privacy.md) — what NeroTech stores about players (very little), what it never stores,
  and how erasure and opt-out work.

## Project

- [Power and NeroPower](Power-and-NeroPower-Split.md) — what NeroTech's power tier covers, and what
  the optional NeroPower add-on builds on top of it.

Add one page per block, item, machine, or system as it is built, and link it here. Keep this
page as the index.
