# NeroTech Wiki

Player- and contributor-facing documentation for **NeroTech**, part of the **Neroland ecosystem**
of sci-fi mods. Built on **Neroland Core**.

> **Status:** beta (version `0.1.0-beta.1`), built on Neroland Core (1.7.0+) across the six
> cross-loader cells. The beta push is feature-complete: a full thermal model, the scalable Fusion
> Reactor multiblock, pollution mitigation tech, production analytics, overclock presets, the
> Configurator wrench, the Tech Guide, datapack machine recipes, optional Nerospace planet interop,
> and an animated visual identity on every machine. In-game runtime verification is ongoing.

## Getting started

- [Tech Guide](Tech-Guide.md) — the in-game handbook: 8 chapters from first power to exotic matter.
- [Materials & Components](Materials-and-Components.md) — Tier-1 crafting components and
  Earth-metal processing dusts, with their `c:` tags.
- [Tier-1 Machines](Machines.md) — Nero Generator, Solar Array, Ore Processor, Fabricator;
  power/energy, upgrade modules, and the `industrial_power` gate.
- [Power Generation](Power-Generation.md) — the wider power tier: Wind Turbine, Geothermal and Bio
  Generators, Battery Bank, Grid Controller, Wireless Power Node.

## Systems

- [Thermal System](Thermal-System.md) — conduction between machines, dimension/biome ambient,
  coolants, and throttling.
- [Fluids & Gases](Fluids-and-Gases.md) — the Electrolyzer chain (water → hydrogen + oxygen), the
  Gas Turbine, the Chemical Processor's oxygen wash, and the Radiator/Coolant Pump loop.
- [Consequence Systems](Consequence-Systems.md) — heat and regional pollution overview, with
  opt-in per-player attribution and POPIA/GDPR handling.
- [Pollution & Mitigation](Pollution-and-Mitigation.md) — Scrubber, the Filter Cartridge cycle,
  and the Remediator.
- [Analytics](Analytics.md) — the per-machine Analytics tab and the Analytics Terminal dashboard.
- [Overclock Presets](Overclock-Presets.md) — Eco / Balanced / Overdrive on every machine.
- [Side Config & the Configurator](Side-Config-and-Configurator.md) — per-face I/O and the wrench.

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
- [Planets](Planets.md) — how solar output and ambient heat follow Nerospace planet traits, with
  a config fallback.

## Companion app

- [NeroLink Companion App](Companion-App.md) — what NeroTech exposes to the NeroLink phone/desktop
  app through Neroland Core's link API: your own pollution/guide data, remote overclock
  presets, live meltdown alerts, the in-app wiki, and the opt-in privacy posture.

## Project

- [Power & the NeroPower Split](Power-and-NeroPower-Split.md) — why NeroPower will not ship as a
  separate mod, and the archived criteria that once governed the split.

Add one page per block, item, machine, or system as it is built, and link it here. Keep this
page as the index.
