# Link Module

NeroTech's module for the **NeroLink** companion app. It plugs into Neroland Core's link API under the id
`nerotech`. NeroTech runs no network server of its own: the NeroLink bridge mod serves these sections and actions to
paired app clients. Without the bridge none of this runs. For the player-facing overview, see
[Companion App](Companion-App.md).

**Link schema: 2.** Schema 2 adds the `nearby` and `catalogue` sections, a second body for `set_machine_preset` and
the `alarm` event, and changes the `meltdown` event (see below). The schema-1 sections `pollution`, `guide` and `wiki`
keep their shape, so an app written for schema 1 keeps working.

## Sections

| Section | Scope | What it holds |
| --- | --- | --- |
| `pollution` | The requester only | `attributionEnabled`; when on: `optedOut`, `retentionDays`, and `attributed` (your own total). **`attributed` is left out entirely when you have opted out.** While you are online: `localRegion` with `dimension` and a coarse `band` for the area you stand in. When attribution is off: a `note` and nothing personal. |
| `guide` | The requester only | `chapters`, `chaptersStarted`, `totalSteps`, `seenSteps`: totals only. |
| `wiki` | Public | Page index, or one page as markdown (`?page=<slug>`). |
| `nearby` | Proximity | Machines near where you stand, while you are online. Details below. |
| `catalogue` | Public | Machines, materials and the six chains, the same for every player. Details below. |

### `nearby`

Lists the NeroTech machines that stand near you. **Walls, floors and claims do not hide a machine:** anything in
range is listed whether or not you could see it. You must be **online** in game. The server looks at
**already-loaded chunks only** (a 9 x 9 chunk square around you; it never loads a chunk to answer), in your
**current dimension**, inside a box reaching **128 blocks** from you on every axis, up to **128 machines**.
The list comes back in a random order every time, and when more than 128 machines are in range, which 128 you get
is random too, so the order says nothing about where a machine is.

```json
{
  "asOf": 1763700000000,
  "ttlSeconds": 30,
  "truncated": false,
  "machines": [
    { "type": "nerotech:blast_foundry", "state": "overheating", "alarms": ["overheat", "full"],
      "energyPct": 82, "heatPct": 97, "tankPct": null, "h": "3f9a1c2b7d40-0" }
  ]
}
```

| Field | Meaning |
| --- | --- |
| `type` | Block id of the machine. |
| `state` | `idle`, `working`, `starved`, `overheating`, `failing` or `full`. |
| `alarms` | Raised Annunciator alarms: `overheat`, `starved`, `full`, `polluting`, `failing`. Annunciator Panels always list none (they only repeat their neighbours). |
| `energyPct`, `heatPct` | 0 to 100. |
| `tankPct` | 0 to 100 for the machine's fluid tank (else its gas tank), or `null` when it has no tank. |
| `h` | An opaque handle for `set_machine_preset`. Valid for 30 seconds, only for you, and only until your next `nearby` request. |
| `ownedByYou` | `true` when you are the machine's recorded owner, the only machines `set_machine_preset` accepts; otherwise `false`. It never says who else owns a machine, or whether anyone does. |

**Never included:** coordinates, direction, distance or dimension of a machine, who owns or placed it, or any other
player. When you are offline, or nothing is in range, the list is empty. The two cases look the same on purpose.

### `catalogue`

Public recipe data, identical for every player and rebuilt once per datapack reload.

- `machines`: `id`, `family` (such as `metallurgy`, `chemistry` or `power`) and `descKey`, the lang key of a one-line description.
- `materials`: `id`, `kind` (`item`, `fluid` or `gas`) and the `tags` the item actually carries on this server.
- `chains`: the six chains (`structural`, `superconductor`, `semiconductor`, `optics`, `chemistry`, `fusion_fuel`).
  Each chain has a `nameKey` and a list of `products`. Each product has a `standalone` route (NeroTech and Core only)
  and an `offworld` route (with Nerospace materials). A route is an ordered list of steps, each with a `machine`,
  `inputs` and `outputs` (`id` + `kind`; tag ids start with `#`). An empty route means there is no route of that kind.
  A recipe a datapack has removed is left out of its route.

## Actions

NeroTech allows **10 actions per player per minute**. The count is kept in memory and expires a minute after your
last action (logging out does not reset it). Turning pollution attribution **off** has its own budget of 30 a
minute, so it is never refused in normal use. The server re-checks every action, and a remote change is narrower than
what you could do standing at the machine in game.

When a well-formed request is refused, the answer is always the same: `NOT_OWNER` with the message `unavailable`.
It does not say whether the machine is missing, too far, someone else's, or in a chunk that is not loaded.

- **`set_pollution_attribution`** `{ "enabled": bool }`: your own opt-out. Works while offline.
- **`set_machine_preset`**: sets a machine's [overclock preset](Overclock-Presets.md) (`ECO`, `BALANCED`,
  `OVERDRIVE`). You must be online. Two bodies are accepted:
  - **Schema 2:** `{ "h": "<handle>", "preset": "ECO" }`. The handle must come from your own latest `nearby` response
    and be under 30 seconds old. At the moment of the change you must be able to do it in person: the machine is in
    your dimension, **within 16 blocks** of you (not the 128 of `nearby`), its chunk is loaded, you may interact with
    that block (spawn protection, world border, adventure mode and the game's own build checks), and you are its
    recorded owner. A machine with no recorded owner cannot be changed from the app at all.
  - **Schema 1:** `{ "dim", "x", "y", "z", "preset" }`, unchanged. Only for machines you own. Ownership is recorded
    only when per-player attribution was on at placement.

## Events

| Event | Sent to | Body |
| --- | --- | --- |
| `pollution` | You, when your own attributed total first crosses the server's alert threshold (attribution on only). Also raises a WARN alert. | `attributed`, `threshold` |
| `alarm` | The machine's recorded owner, when a machine raises a new alarm. At most one per machine type per player every 30 seconds. | `type`, `alarm` |
| `meltdown` | The reactor's recorded owner. The owner also gets a CRITICAL alert. | `kind` (`meltdown` or `containment_breach`), `shell` |
| `alerts` with `"event": "nearby"` | Every other online player who would see that machine in `nearby` (same dimension and reach), for the same alarms and meltdowns. | `event`, `source` (`alarm` or `meltdown`), then the same body as the event it stands for |

Every event goes to named players. None is broadcast to every client.

- **Changed in schema 2 (privacy fix):** `meltdown` used to be broadcast to every linked client with the reactor's
  x/y/z. It now carries no coordinates and goes only to players in range and the owner.
- **Push notifications** are off by default for the `nerotech` category in the app. Only a recorded owner's
  `alarm` and `meltdown` events can become a push; the owner's `alarm` carries notification text (the machine type
  and the alarm, no place and no numbers). Everyone else gets the `nearby` notice, which NeroLink never sends as a
  push: it only refreshes an app that is open. An owner is recorded only under the server's existing per-player
  attribution setting, so a machine without one pushes nobody.
- A notice tells you that a machine of that type near you changed state, and when. That is the same thing you would
  notice standing there, and the 30-second limit per machine type keeps it coarse.

## Privacy

- Nothing in `nearby`, `catalogue`, `alarm` or `meltdown` names a player or a place.
- Handles, rate-limit counters and alarm throttles live in memory only. Handles and alarm throttles are dropped
  when you log out; rate-limit counters expire a minute after your last action. NeroTech writes none of them to disk.
- Your own pollution total and Tech Guide progress are erasable with every other NeroTech player record through
  Neroland Core's data-erasure hook.
