[![NeroLink App — Beta](https://img.shields.io/badge/NeroLink_App-Now_in_Beta-60d4e8?style=for-the-badge)](https://nerolandmc.net/nerolink/#beta) [![Explore the Neroland ecosystem](https://img.shields.io/badge/Explore-The_Neroland_Ecosystem-1a5a6c?style=for-the-badge)](https://nerolandmc.net/ecosystem/)

> 📱 **NeroLink App Beta — your Neroland world on your phone.** Check energy, alerts and machines live, claim quest rewards and search your storage without logging in. **[Join the beta at nerolandmc.net →](https://nerolandmc.net/nerolink/#beta)**
>
> 🌌 **Explore the Neroland ecosystem.** See how NeroTech fits together with the rest of the Nero mods — every mod, wiki and changelog in one place. **[View the ecosystem at nerolandmc.net →](https://nerolandmc.net/ecosystem/)** · [NeroTech on the website](https://nerolandmc.net/mods/nerotech/)

---

# NeroTech

**Power it, process it, automate it: industrial machines for the Neroland universe, where throughput runs hot and dirty.**

NeroTech is the **industry & automation** mod of the Neroland ecosystem. Start with a generator and an ore processor, grow into alloy foundries, chemistry, chip fabs and deep-core drills, and finish with a gas-fed fusion reactor. Scaling up is a real decision: machines run **hot**, and industry leaves **pollution**.

**It plays complete on its own.** All you need is **Neroland Core**: every item in NeroTech has a standalone route from Overworld and Nether resources. Add **Nerospace** and the same chains run on planet ores, ices and gases, three to five times cheaper.

---

## New in 0.8.0: Offworld Industry

- 🏭 **Six production chains**, each ending in something you want:
  - **Structural**: Cindersteel and titanium plate for tier-2 hulls, drill heads and tool bodies.
  - **Superconductor**: glacite wire and tier-3 coils.
  - **Semiconductor**: crystal boules, wafers, **Logic Chips** and **Quantum Chips**.
  - **Optics**: four focus lenses that set what a Laser Etcher etches and what a Cutting Lance cuts.
  - **Chemistry**: sulfuric acid (four dusts from one ore), polymer resin, methane and CO2.
  - **Fusion fuel**: deuterium, helium-3 and the Europium Flux Lattice.
- ⚙️ **22 new machines and blocks**: Plasma Crucible, Plate Press, Filament Spooler, the **Blast Foundry** multiblock (3³ and 5³), Centrifuge cascades, Cryo Distiller, Atmospheric Condenser, Reaction Vat, stackable Process Tanks, Component Forge, Laser Etcher, Crystal Grower, Heat Pipes and the Heat Exchanger, the **Deep Core Drill**, Fuel Injector, Europium Flux Lattice, Induction Charger and Annunciator Panel.
- 🔥 **Heat as a resource**: pipe a reactor's waste heat into a Blast Foundry instead of fighting it.
- ⛏️ **Virtual deposits**: every chunk has an ore body deep below. The Deep Core Drill mines it without breaking any ground, the deposit runs down and slowly recovers, and the Survey Scanner tells you what is left.
- ☢️ **Gas-fed fusion**: pipe deuterium and helium-3 into a 7³ reactor, no cells to craft, and calm it with Flux Lattices.
- 🧰 **Seven powered tools**: Power Drill, Cutting Lance, Arc Wrench, Multimeter, Survey Scanner, Thermal Goggles and Filter Mask. Tools respect claims and protected ground, and charge at the Induction Charger.
- 🚨 **Readable bases**: an Annunciator Panel lights one lamp per alarm with a comparator output, every machine shows its state on its model, and hovering any gauge tells you what it is, how much, and the rate.
- 📱 **NeroLink app module**: machines near you, a chain planner with both routes, your Tech Guide progress and your own pollution, on your phone.
- 🏛️ **A walkable gallery**: `/nerotech gallery` builds every machine and chain running, with a guided tour.

## The production line

1. **Components.** Machine Frames, Circuit Boards and Nero Coils; grind iron, copper and gold into dusts (`c:dusts/*`).
2. **Power.** Nero Generator, Solar Array, wind, geothermal and bio generators, Battery Banks, a Grid Controller and wireless power nodes, all on **Nero Energy (NE)**.
3. **Processing.** Ore Processor, Fabricator and their Advanced tiers, then the 0.8.0 chains.
4. **Upgrades.** Speed, Efficiency, Capacity and Range modules from Core's shared framework, plus NeroTech's Heat Sink and Scrubber modules, and Eco / Balanced / Overdrive presets on every machine.
5. **Automate.** Auto Crafter, Item Sorter, Conveyor Belts and Robotic Arms, all on standard item, fluid and gas capabilities, ready for NeroLogistics, hoppers and pipes.
6. **Endgame.** The scalable **Fusion Reactor** (3³/5³/7³), the free-form **Particle Accelerator**, antimatter and the Singularity Vault.

## Consequence systems

- 🔥 **Heat**: machines heat their neighbours, follow the climate of the world they are in, and **throttle** when they overheat. A Fusion Reactor will **melt down** at maximum heat (servers can turn that off).
- 🌫️ **Pollution**: tracked per region in each dimension, batched for big bases. Severe pollution makes you sick unless you wear a Filter Mask; Scrubbers and Remediators clean it up.

## Why it fits the ecosystem

- 🧩 **Built on Neroland Core**: one energy type, one upgrade system, shared material tags and milestone records. NeroTech has its own creative tab.
- 🚀 **Optional Nerospace synergy**: planet ores, ices and gases feed every chain by tag. No hard dependency either way.
- 🔌 **Interop by tag**: every 0.8.0 material carries a `c:` or `nerotech:` tag so NeroPower, NeroLogistics, NeroColonies and NeroCompanion (and other mods) can use it.
- 🧱 **Cross-loader**: NeoForge, Forge and Fabric on Minecraft **1.21.1, 1.21.4, 1.21.5, 1.21.8, 1.21.10, 1.21.11, 26.1.2, 26.2 and 26.3**.

## Requirements & compatibility

- **Requires [Neroland Core](https://nerolandmc.net/mods/neroland-core/) 1.19.0 or later**: install it alongside NeroTech (it loads first).
- Optional: **[Nerospace](https://nerolandmc.net/mods/nerospace/)** for the offworld routes and the Starsteel advanced tier. **NeroLink** for the companion app. JEI or EMI for recipe pages.
- **Modpacks are allowed and encouraged**: any platform, no need to ask. Use the official files and credit *NeroTech by Neroland* with links to the [CurseForge page](https://www.curseforge.com/minecraft/mc-mods/nerotech) and the [GitHub repository](https://github.com/Neroland/nerotech). Full terms: [LICENSE](https://github.com/Neroland/nerotech/blob/main/LICENSE).

## Privacy (POPIA / GDPR)

NeroTech stores **no personal data by default**. 0.8.0 adds no store keyed by a player: deposits are world data, the Induction Charger and Annunciator Panel never record an owner, and the app's nearby view shows no coordinates and no owners. Optional per-player **pollution attribution** is off unless a server turns it on; when enabled it keeps **UUIDs only** (never names), prunes on a retention window, and registers with Core's shared data-erasure hook, so one erase request clears your NeroTech data along with every other Neroland mod.

NeroTech also includes anonymous **crash telemetry** (stack trace + mod/MC/loader/OS/Java versions only, never IPs, usernames, UUIDs or world data) via Sentry on EU servers, so bugs can be fixed. **On by default**: opt out any time with `telemetryEnabled = false` in `config/nerotech.properties`. Full details: **[PRIVACY.md](https://github.com/Neroland/nerotech/blob/main/PRIVACY.md)**.

## Links

- 📖 **[Wiki](https://github.com/Neroland/nerotech/wiki)**: every block, machine and system documented.
- 💬 **[Discord](https://discord.gg/ArPXvYUzJG)**: chat, help and sneak peeks.
- 🐞 **[Issues](https://github.com/Neroland/nerotech/issues)**: bug reports and feature requests.
- 🗒️ **[Changelog](https://github.com/Neroland/nerotech/blob/main/CHANGELOG.md)**
- 🔥 **[Also on CurseForge](https://www.curseforge.com/minecraft/mc-mods/nerotech)**

---

*Created by Neroland. The project logo was made with the help of AI image tools; in-game art is generated by the project's own tooling and refined by hand.*
