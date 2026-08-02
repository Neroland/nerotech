# Overclock Presets

Since 0.1.0-beta.1, every generator and processing machine has a **preset selector** — a cycle
button on its GUI that trades speed against energy, heat and pollution. Presets are free to switch
at any time and persist on the machine (they survive relog and being mined).

## The three presets

| Preset | Speed | Energy · Heat · Pollution | Button |
| --- | --- | --- | --- |
| **Eco** | −25% | −50% | teal |
| **Balanced** (default) | — | — | white |
| **Overdrive** | +50% | +100% | amber |

Presets stack with [upgrade modules](Machines.md#upgrade-modules): an Overdriven machine full of
Speed modules is very fast, very hungry, and very hot.

## When to use what

- **Eco** for machines near their [heat limit](Thermal-System.md), power-starved outposts, or when
  a region's [pollution](Pollution-and-Mitigation.md) is creeping up — half the heat and half the
  pollution for a quarter less speed is usually a good trade.
- **Overdrive** when throughput matters and you have the cooling and scrubbing to absorb it.
  Beware the **Fusion Reactor**: Overdrive doubles its already-scaled heat rate, and an unmanaged
  overdriven reactor will find its own meltdown threshold. The heat-shifted glow and warning
  strobe telegraph it.

The active preset shows in the machine's [Analytics tab](Analytics.md) header, and the analytics
rates update to reflect it.

## Exempt machines

The **Auto Crafter** and **Item Sorter** take no preset — they are one-shot, on-demand machines
with nothing meaningful to over- or underclock.

## See also

- [Thermal System](Thermal-System.md)
- [Analytics](Analytics.md)
- [Tier-1 Machines](Machines.md)
- [Home](Home.md)
