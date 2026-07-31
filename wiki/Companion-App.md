# NeroLink Companion App

NeroTech plugs into **[NeroLink](../nerolink)** — the Neroland companion-app bridge — through
**Neroland Core's link API**. When a server runs Core, NeroTech and the NeroLink bridge mod, the
NeroLink phone/desktop app automatically gains a **NeroTech** section: your own machine progress,
pollution, gates, live meltdown warnings, and this very wiki, rendered in-app.

NeroTech itself ships **no** network server. It only *registers* what it exposes with Core's link
registry (`za.co.neroland.nerolandcore.link`); the NeroLink bridge is the thing that serves it to
app clients over its local API. If NeroLink is not installed, none of this runs and NeroTech plays
exactly as before.

> **Privacy first (POPIA/GDPR).** Everything personal the app can read is **your own data only**,
> scoped to your player UUID on the server. Pollution attribution is **opt-out and off by default**,
> and one data-erasure request purges every per-player thing NeroTech stores (see
> [Consequence Systems](Consequence-Systems.md)).

## What the app can read (data sections)

| Section | What it shows | Scope |
|---------|---------------|-------|
| `pollution` | Your **own** attributed pollution total and the retention window — **only** when a server admin has turned on per-player attribution and you have not opted out. Otherwise a note explaining the opt-out posture; never regional/aggregate pollution as "yours". | Your UUID |
| `guide` | Your [Tech Guide](Tech-Guide.md) progress: chapters, chapters started, total steps and steps seen. | Your UUID |
| `gates` | Your unlock state for the three NeroTech-relevant progression gates: `industrial_power`, `orbit_fabrication`, `fusion_online`. | Your UUID |
| `wiki` | This wiki, rendered in-app: a page index and each page's markdown. | Public (same for everyone) |

## What the app can do (actions)

The server re-validates **every** action — the app holds no authority, and a compromised app can
do no more than you could do in-game.

- **`set_pollution_attribution`** `{ "enabled": bool }` — your **own** privacy control. `enabled:
  false` opts you out of having your pollution attributed; `enabled: true` opts back in. Works while
  you are offline. Because the server config exposes only a single global attribution flag, this is
  stored as a per-player **opt-out** layered on that flag: attribution happens only when the server
  has it on **and** you have not opted out. Your preference is erasable like any other player data.
- **`set_machine_preset`** `{ "dim", "x", "y", "z", "preset": "ECO"|"BALANCED"|"OVERDRIVE" }` — set a
  machine's [overclock preset](Overclock-Presets.md) remotely. The server requires that you are
  **online**, that the target is a live NeroTech machine, that **you own it**, and that any
  progression gate the machine needs is open. Ownership is taken from the machine's own owner
  record, which is only captured **when per-player attribution was on at placement** — so a machine
  placed with attribution off cannot be re-preset remotely (it is refused as not-owned). It reuses
  the exact same server-side preset path as the in-game GUI selector.

## Live events & alerts

NeroTech pushes live deltas onto Core's link event bus, and raises Core **alerts** (keyed to the
`nerotech` module, surfaced in the app's alerts view):

- **Pollution threshold** — when your **own** attributed pollution first crosses the server's alert
  threshold (attribution on only): a player-scoped `pollution` event plus a **WARN** alert.
- **Fusion meltdown / containment breach** — a server-wide `meltdown` broadcast (world coordinates
  only, no personal data) plus a **CRITICAL** alert to the reactor's owner. See
  [Fusion Reactor](Fusion-Reactor.md).
- **Gate unlocked** — an optional player-scoped `gate` event when `orbit_fabrication` or
  `fusion_online` opens for you.

## Progression gates

NeroTech now backs its tiers with **real Core progression gates** (datapack JSON under
`data/nerotech/neroland_gates/`), layered on top of the existing recipe/tag gating:

- **`industrial_power`** (Core) — opened when you place your first NeroTech machine.
- **`orbit_fabrication`** — requires Core's `reached_orbit`; opened on first use of an orbit-tier
  machine (Advanced Fabricator / Advanced Ore Processor / Fusion Reactor). Using one before it opens
  is denied with an in-game message.
- **`fusion_online`** — requires `orbit_fabrication`; opened for a reactor's owner the first time
  their Fusion Reactor ignites.

## Enabling it as a player

1. Play on a server that runs **Neroland Core**, **NeroTech** and the **NeroLink** bridge mod.
2. Pair the NeroLink app with the server (see the NeroLink docs) — you authenticate as your own
   player, so you only ever see your own data.
3. Open the **NeroTech** section. To share your pollution numbers, ask the admin to enable
   `pollutionPerPlayerAttribution`; you can still opt out any time from the app. To leave it private,
   do nothing — attribution is off by default.
