# NeroLink Companion App

NeroTech plugs into **NeroLink**, the Neroland companion-app bridge, through **Neroland Core's link API**.
When a server runs Core, NeroTech and the NeroLink bridge mod, the NeroLink phone and desktop app gains a
**NeroTech** section. If the server does not run NeroTech, the section stays hidden.

NeroTech itself ships **no** network server. It only *registers* what it exposes with Core's link registry;
the NeroLink bridge serves it to paired app clients. Without NeroLink none of this runs and NeroTech plays
exactly as before. The developer reference, with every field, is [Link Module](Link-Module.md) (link schema 2).

> **Privacy first (POPIA/GDPR).** Personal data in the app is **your own only**. Nothing in the NeroTech
> section gives another player's name, position or base, or a machine's coordinates. See [Privacy](Privacy.md).

## What the NeroTech section shows

| Screen | What it shows | Scope |
| --- | --- | --- |
| **Nearby** | NeroTech machines within 128 blocks of where you stand in game, while you are online: type, state (idle, working, starved, overheating, failing, full), alarms, and energy, heat and tank levels. No coordinates and no owner. | Where you are, now |
| **Chain planner** | Pick a product and see its chain step by step, with the standalone route (NeroTech and Core only) and the offworld route (with Nerospace). Built from the server's real recipes, so a pack's changes show up. | Public, the same for everyone |
| **Tech Guide** | Your [Tech Guide](Tech-Guide.md) progress: chapters started and steps seen. | Yours |
| **Pollution** | Your **own** attributed pollution total, only when the server has turned on per-player attribution and you have not opted out; otherwise a short note. While you are online, the coarse pollution band of the area you stand in. | Yours |
| **Wiki** | This wiki, rendered in the app. | Public |

The Nearby list is kept in the app's memory only and never written to the phone's storage. When a server
stops offering NeroTech, the app drops everything it cached for it.

## What you can do from the app

The server re-checks every action, and remote changes are narrower than what you can do in game. You get at
most ten actions a minute.

- **Opt out of pollution attribution** (or back in). Works while you are offline.
- **Set a machine's [overclock preset](Overclock-Presets.md)** (Eco, Balanced, Overdrive) from Nearby. You
  must be online, you must be the machine's recorded owner (owners are recorded only when per-player
  attribution was on at placement), and you must be close enough to use it in person: within 16 blocks and
  allowed to interact with that block (spawn protection and adventure mode apply). Machines with no recorded
  owner are shown in Nearby but cannot be changed from the app, because a claim or protection mod cannot see an
  app action. The app only shows the preset control on your own machines. A refused change always gives the
  same answer, so the app learns nothing about why.

## Live events and alerts

- **Machine alarms.** When a machine near you raises a new alarm (overheat, starved, full, polluting,
  failing), the app's Nearby view updates. At most one notice per machine type every 30 seconds.
- **Fusion meltdown or containment breach.** Players in range get a notice; the reactor's recorded owner also
  gets a **critical** alert. The event carries no coordinates. (Before 0.8.0 it was broadcast to every linked
  client with the reactor's position; that is gone.)
- **Pollution threshold.** When your own attributed total crosses the server's alert line (attribution on
  only), you get a warning.

**Push notifications** for the NeroTech category are off by default. Only a machine's recorded owner can be
pushed, and only after turning NeroTech notifications on in the app. A machine records an owner only under
the server's existing per-player attribution or operating-charge settings, both off by default, so on most
servers nobody is pushed and the notices only refresh an open app.

## Enabling it as a player

1. Play on a server that runs **Neroland Core**, **NeroTech** and the **NeroLink** bridge mod.
2. Pair the NeroLink app with the server (see the NeroLink docs). You sign in as your own player, so you only
   ever see your own data and what is near you.
3. Open the **NeroTech** section. To see your own pollution numbers, ask the admin to enable
   `pollutionPerPlayerAttribution`; you can opt out any time from the app. To keep it private, do nothing:
   attribution is off by default.

## Milestones

NeroTech never locks a machine behind a gate: the mod plays fully standalone, and pacing comes from recipes
and materials. It still opens Core's ecosystem milestone **`industrial_power`** when you place your first
machine, and records milestones such as your first foundry pour for quest packs to read. Other Neroland mods
may read those to add content, never to restrict NeroTech's.
