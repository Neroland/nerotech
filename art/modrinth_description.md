# NeroTech

**Power it, process it, automate it — industrial machines for the Neroland universe, where throughput runs hot and dirty.**

NeroTech is the **industry & automation** mod of the Neroland ecosystem. Build a full Earth-tier production line — burn fuel or catch sunlight for power, double your ores, fabricate components, and wire it all into auto crafters and sorters — then push past the atmosphere into a gated late game with the **Fusion Reactor** and advanced machines. Scaling up is a real decision: machines run **hot**, and industry leaves **pollution**.

Built on **Neroland Core**, so its Nero energy, upgrade modules, progression gates, and material tags are shared with the rest of the lineup. *(Early alpha — actively developed.)*

---

## The production line

1. **Components.** Craft Machine Frames, Circuit Boards and Nero Coils; grind iron, copper and gold into dusts (`c:dusts/*`, so other tech mods can use them too).
2. **Power.** The **Nero Generator** burns solid fuel and the **Solar Array** catches daylight, producing **Nero Energy (NE)** and pushing it to neighbouring machines and storage.
3. **Processing.** The **Ore Processor** turns raw ore into doubled dust; the **Fabricator** turns refined materials into components — both recipe-driven.
4. **Upgrades.** Slot **Speed / Efficiency / Capacity / Range** modules into any machine — Core's shared upgrade framework, so modules are interchangeable across every Neroland mod.
5. **Industrial Power.** Placing your first machine opens Core's **Industrial Power** progression gate — your first milestone.
6. **Reach space.** Crafted from **Starsteel** (a space-only material): the high-output **Fusion Reactor** (which runs dangerously hot), the **Advanced Ore Processor**, and the **Advanced Fabricator**.
7. **Automate.** The **Auto Crafter** assembles recipes on demand, and the directional **Item Sorter** routes items by filter — both exposed on standard item capabilities for clean logistics handoff.

## Consequence systems

- 🔥 **Heat** — every machine runs hot and **throttles** when it overheats; park coolant blocks (water, ice, snow) alongside to shed it faster. The Fusion Reactor will **melt down** at maximum heat — admin-disableable for survival-friendly servers.
- 🌫️ **Pollution** — aggregated **regionally and periodically** (built for megabase performance, never a per-tick scan), so heavy industry has a footprint without tanking your TPS.
- ⚙️ **Portable power** — generation talks only to Core's energy surface, so machines from across the ecosystem share one network.

## Why it fits the ecosystem

- 🧩 **Built on Neroland Core** — one energy type, one upgrade-module system, one progression arc, and shared `c:` material tags. NeroTech ships in its own creative tab.
- 🚀 **Soft synergy with Nerospace** — the advanced tier is unlocked by **Starsteel / planet ores** through shared Core tags (no hard dependency). With Nerospace absent the Earth tier plays **fully standalone**, and the advanced tier is simply uncraftable.
- 🔌 **Logistics-ready** — every machine exposes a standard, *sided* item capability (inputs in, outputs out), ready for **NeroLogistics**, hoppers, pipes, and recipe viewers with no NeroTech dependency.
- 🧱 **Cross-loader** — NeoForge, Forge, and Fabric on Minecraft **26.1.2** and **26.2**.

## Requirements & compatibility

- **Requires [Neroland Core](https://modrinth.com/mod/nerolandcore)** — install it alongside NeroTech (it loads first).
- Optional but recommended: **[Nerospace](https://modrinth.com/mod/nerospace)** for the space materials that unlock the advanced tier.
- Conventional `c:` tags on materials and loader-native item/energy capabilities on every machine face, so Create, AE2, Mekanism, and recipe viewers interoperate as the 26.x ecosystem fills in — no hard dependency on any of them.
- **Modpacks are allowed and encouraged** — any platform, no need to ask. Use the official files and credit *NeroTech by Neroland* with links to the [CurseForge page](https://www.curseforge.com/minecraft/mc-mods/nerotech) and the [GitHub repository](https://github.com/Neroland/nerotech). Full terms: [LICENSE](https://github.com/Neroland/nerotech/blob/main/LICENSE).

## Privacy (POPIA / GDPR)

NeroTech stores **no personal data by default**. Optional per-player **pollution attribution** is off unless a server turns it on; when enabled it keeps **UUIDs only** (never names), prunes on a configurable retention window, and registers with Core's shared data-erasure hook — so a single erase request clears your NeroTech data alongside every other Neroland mod.

NeroTech also includes optional, anonymous **crash telemetry** (stack trace + mod/MC/loader/OS/Java versions only — never IPs, usernames, UUIDs, or world data) via Sentry on EU servers, so bugs can be fixed. Opt out any time with `telemetryEnabled = false` in `config/nerotech.properties`. Full details: **[PRIVACY.md](https://github.com/Neroland/nerotech/blob/main/PRIVACY.md)**.

## Links

- 📖 **[Wiki](https://github.com/Neroland/nerotech/wiki)** — every block, machine, and system documented.
- 💬 **[Discord](https://discord.gg/ArPXvYUzJG)** — chat, help, and sneak peeks.
- 🐞 **[Issues](https://github.com/Neroland/nerotech/issues)** — bug reports and feature requests.
- 🗒️ **[Changelog](https://github.com/Neroland/nerotech/blob/main/CHANGELOG.md)**
- 🔥 **[Also on CurseForge](https://www.curseforge.com/minecraft/mc-mods/nerotech)**

---

*Created by Neroland. The project logo was made with the help of AI image tools; in-game art is generated by the project's own tooling and refined by hand.*
