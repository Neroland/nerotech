# Privacy

NeroTech is built so that a server stores as little about players as possible, in line with the GDPR (EU)
and POPIA (South Africa). The full disclosure, including error reporting, lives in
[PRIVACY.md](https://github.com/Neroland/nerotech/blob/main/PRIVACY.md). This page is the player-facing
summary.

## What NeroTech never stores

- Player names. Where a player has to be recorded at all, only their UUID is kept.
- Positions of players, chat, or anything a player types.
- Anything about you in logs at INFO level or above: no UUIDs, names, coordinates or player-written text.

## What is stored, and when

Nothing below is about you unless the server has switched on one of the opt-in options.

| What | Kept where | About a player? |
|---|---|---|
| Machine state (progress, heat, tanks, presets, side config) | the machine, in the world save | No. The placing player's UUID is kept only if the server turned on [operating charges](Operating-Charges.md) or per-player pollution attribution. |
| Regional pollution | the world save, keyed by dimension and region | No. |
| Per-player pollution totals | the world save | Only with `pollutionPerPlayerAttribution=true` (off by default). UUID only, pruned after `pollutionAttributionRetentionDays`. You can opt yourself out. |
| Deep Core Drill deposits | the world save, keyed by dimension and chunk | No. Only how much is left and when it was last drilled. Removed once the deposit has recovered. |
| Tech Guide "seen" marks | the world save, keyed by UUID | Yes: which guide steps you have clicked. Nothing else. |
| Milestone records | Neroland Core's progression store | Yes: gate ids under your UUID, no times or places. |
| Tool data | on the item | No. Charge, mode, fitted lens or filter only. A machine carried by the Arc Wrench loses its owner before it goes on the item. |

## Induction Charger

The Induction Charger tops up energy items you hold or wear while you stand near it. It looks for players
nearby, charges them, and forgets them. It never records an owner, saves nothing about who it charged and
logs nothing about it.

## Milestones credited "in the moment"

Some milestones (first foundry pour, first quantum processor, first deposit tapped, gas-fed fusion
ignition) go to whoever is operating the machine at that moment, if they are online and have not opted out
of attribution. The machine remembers that player only in memory, never in the save, and forgets them when
its chunk unloads. The record itself is a Neroland Core progression gate.

## Pollution exposure

Standing in a severely polluted region gives brief Weakness and Hunger unless you wear a charged
[Filter Mask](Tools.md#filter-mask-and-pollution-exposure). It is worked out on the spot from the region's
pollution level. Nothing about the exposed player is recorded.

## NeroLink companion app

See [Link Module](Link-Module.md) for the full schema. In short:

- **Nearby** shows machines within 128 blocks of you, in your dimension, while you are online: type, state,
  alarms and levels. No coordinates and no owner. Machines are addressed by short-lived handles that mean
  nothing to anyone else.
- **Catalogue** is public recipe data, the same for every player.
- **Alarm** and **meltdown** events go only to online players in range and to a machine's recorded owner,
  and carry no coordinates.
- Your own pollution total and Tech Guide progress are visible only to you.
- Handles, the action rate limit and the alarm throttle live in memory only and are cleared when you log
  out.

## Erasure and opt-out

All of NeroTech's player records are registered with Neroland Core's shared erasure hook. One erase request
(or the server's inactivity sweep) removes your pollution total, your opt-out preference, your Tech Guide
marks, your in-memory link state and your ownership of machines, together with your records in every other
Neroland mod. Machines in unloaded chunks drop you as owner the next time they load.

Anonymous error reporting is on by default and opt-out: set `telemetryEnabled=false` in
`config/nerotech.properties`. Reports carry no IP, username, UUID, coordinates or world data.

## See also

- [Link Module](Link-Module.md)
- [Pollution & Mitigation](Pollution-and-Mitigation.md)
- [Operating Charges](Operating-Charges.md)
- [Home](Home.md)
