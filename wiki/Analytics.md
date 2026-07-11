# Analytics

Since 0.1.0-beta.1, NeroTech tells you **why** a production line is underperforming instead of
making you guess. Analytics comes in two layers: a tab on every machine, and a dashboard block for
the whole factory floor.

## The Analytics tab (every machine)

Next to the Side Config tab on each machine GUI:

- A colour-coded **status line naming the limiting cause**:
  **Running** · **Idle** (no work queued) · **Starved** (missing input) · **Blocked** (output
  full) · **Throttled** (over the [heat threshold](Thermal-System.md)) · **No energy** ·
  **Unformed** (a [Fusion Reactor](Fusion-Reactor.md) without a valid shell). Machines report
  their own causes — a Scrubber out of filter cartridges says so.
- Current **heat**, **energy** and **operations rate**, plus **pollution** and **efficiency**
  readouts. Displayed pollution/efficiency rates are **nominal** — computed from the machine's
  config values and its active [preset](Overclock-Presets.md) — not live world measurements.
- Two **60-second sparklines** (heat and energy), so you can see a machine riding its heat limit
  or browning out as a sawtooth rather than a single number.
- The active overclock preset in the header.

Sampling is server-side and cheap: data streams to your client only while you have the GUI open.

## Analytics Terminal

A passive console block (it uses no energy) that scans the machines around it
(`analyticsTerminalRadius`, default 16 blocks, loaded chunks only) and shows a factory dashboard:

- **counts by status** — how many machines are running, starved, blocked, throttled…,
- the **hottest machine** in range, and
- rows for the nearest machines with their individual statuses.

Place one at the heart of each production floor; its holographic shimmer and pulsing screen mark it
out. Data is machine-scoped only — positions, statuses and rates; nothing about players is
collected or stored.

## See also

- [Thermal System](Thermal-System.md)
- [Overclock Presets](Overclock-Presets.md)
- [Pollution & Mitigation](Pollution-and-Mitigation.md)
- [Tier-1 Machines](Machines.md)
- [Home](Home.md)
