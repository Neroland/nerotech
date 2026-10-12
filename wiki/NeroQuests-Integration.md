# NeroQuests Integration (optional)

NeroTech ships a quest-book chapter for **NeroQuests** as plain data inside its own jar. With NeroQuests
installed the chapter appears in the quest book; without it the files are inert and nothing changes.
**NeroTech has no dependency on NeroQuests in any form** — the pack is JSON plus language keys, and the
only runtime seam is Neroland Core (progression gates and threshold events).

## The chapter: Industrial Record

Chapter `nerotech:industrial_record`, 19 quests: 16 for each player and 3 server-wide "world log"
quests that complete once for the whole world.

The player line follows the tech tree: **First Components → First Spark** (Nero Generator + Core's
`industrial_power` milestone) **→ Ore Doubling / Fabrication → Clear Skies / The Grid → Heat and Gas /
Clean Conscience → Ignition → Stellar Fuel / Particle Physics → Antimatter**. Things a machine makes
(dust, circuit boards, fusion parts, cells) count when you **collect** them — machine output is never a
"craft".

Four **milestone quests** branch off it and complete when NeroTech records the matching milestone for
you (below): **Fusion Online**, **Grid Scale**, **Hands-On Cleanup** and **First Collision**. Each also
unlocks your next Tech Guide entry (see the hook below).

The three world-log quests read NeroTech's threshold channels:

| Quest | Channel | Means |
| --- | --- | --- |
| Reactor Cooled | `nerotech:machine_failure`, falling, stage 1 | A throttled reactor somewhere was brought back to safe heat. |
| Remediation | `nerotech:pollution`, falling | A Scrubber or Remediator pulled a region back under the pollution line. |
| Skies Recovered | `nerotech:pollution_decay`, falling | A region drifted back under the line on its own, by waiting. |

## Milestone record gates

NeroTech records four personal milestones as **player-scope Neroland Core gates**
(`data/nerotech/neroland_gates/`). They are records, not locks: nothing in NeroTech or any other mod is
allowed to check them for access.

| Gate | Recorded when |
| --- | --- |
| `nerotech:fusion_online` | Your Fusion Reactor's multiblock shell forms for the first time. |
| `nerotech:first_collision` | Your Particle Collider consumes a particle pair and lands its product. |
| `nerotech:remediated` | Your Remediator actually removes pollution from its region. |
| `nerotech:grid_scale` | The machines one of your Grid Controllers watches add up to `gridScaleMilestoneCapacity` NE of storage (default 5,000,000; `0` disables). |

"Your" means the machine's **owner** — the player who placed it. NeroTech only knows an owner while
per-player pollution attribution or operating charges are enabled (see [Consequence
Systems](Consequence-Systems.md) and [Operating Charges](Operating-Charges.md)), and an owner who has
opted out of attribution records nothing. The gate rows live in Core's progression store and are erased
through Core's shared data-erasure hook; see `PRIVACY.md`.

## The Tech Guide hook

A quest pack may reward a `mod_hook`, which asks the quest mod to open a **hook gate** in NeroTech's
namespace for the completing player. NeroTech ships one:

| Hook gate | Effect |
| --- | --- |
| `nerotech:hook/tech_guide_unlock` | Marks your next incomplete [Tech Guide](Tech-Guide.md) entry complete and tells you which one in chat. Information only — no recipe, item or progression is skipped. Once the guide is complete, only the chat line is sent. |

NeroTech listens for the gate through Core's progression events, applies the effect once, then closes
the gate so it can fire again. If it opened while you were offline it is delivered when you next log in.

## For pack authors and server owners

- Read the gates with a `gate_open` objective; read the channels with `custom_event` in a server-scope
  quest. Threshold scopes are places (a region key, or `machineId@dimension`) — never a player or a
  block position.
- Everything above is driven by Neroland Core 1.15.0 or later.
