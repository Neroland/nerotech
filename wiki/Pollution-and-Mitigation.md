# Pollution & Mitigation

Industrial activity emits **pollution**, tracked per **region** (a coarse 64×64-block grid). It
builds while your machines run, decays slowly on its own — and, since 0.1.0-beta.1, can be actively
cleaned up with dedicated mitigation machines. Cleanup is designed as a genuine resource sink: it
costs power and materials, not just patience.

For how pollution accumulates, decays, and the (opt-in, privacy-guarded) per-player attribution,
see [Consequence Systems](Consequence-Systems.md).

## Scrubber

The workhorse cleaner. A powered machine that, on each operation:

- removes pollution from **its own region** at the full rate (`scrubberPollutionPerOp`), and
- removes a fraction of that rate from **each of the 8 surrounding regions**
  (`scrubberAdjacentPermille`, default 25%) — so one well-placed Scrubber softens a whole
  neighbourhood, but only truly cleans the region it stands in.

The Scrubber consumes **Filter Cartridges** (crafted from paper + iron, 4 per craft). A cartridge
absorbs a set amount of pollution (`scrubberFilterCapacity`) and then **fouls into a Dirty
Filter** — watch the cartridge visibly darken in the machine's filter bay. Dirty Filters are not
waste: run them through an **Ore Processor** to recover iron dust. Each operation also costs NE
(`scrubberNePerOp`).

## Remediator

The heavy-duty option for a region you have badly poisoned. The Remediator is **slotless** — no
filters, no consumables — and simply works its own region's pollution down, at a deliberately steep
energy price (`remediatorNePerOp`, default 5× the Scrubber's, for `remediatorPollutionPerOp`
pollution per operation). It is gated behind **Starsteel**, so it arrives with the advanced tier.

Both machines are full citizens of the machine framework: they take
[upgrade modules](Machines.md#upgrade-modules), participate in the
[thermal system](Thermal-System.md), honour [side configuration](Side-Config-and-Configurator.md),
and report through the [Analytics tab](Analytics.md). Like all pollution work, their operations run
on a batched interval — never per-tick.

## Reading pollution numbers

The Analytics tab on each machine shows a **pollution rate**. Note that displayed rates are
**nominal** — derived from the machine's configured per-operation values and its current preset —
rather than a live measurement of the region.

## Threshold events

When a region's pollution crosses a configurable line (`pollutionEventThreshold`; `0` disables),
NeroTech publishes a **threshold event** through Neroland Core — both when rising past it and when
recovering below it. Nothing in NeroTech reacts to these yet: the hook lies dormant until a
listener mod (planned: NeroEvents, e.g. smog events) picks them up. Events are keyed by region,
never by player.

## Config keys

`scrubberNePerOp`, `scrubberPollutionPerOp`, `scrubberAdjacentPermille`, `scrubberFilterCapacity`,
`remediatorNePerOp`, `remediatorPollutionPerOp`, `pollutionEventThreshold` — alongside the base
pollution keys listed in [Consequence Systems](Consequence-Systems.md).

## See also

- [Consequence Systems](Consequence-Systems.md)
- [Thermal System](Thermal-System.md)
- [Analytics](Analytics.md)
- [Home](Home.md)
