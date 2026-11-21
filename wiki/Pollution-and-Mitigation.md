# Pollution & Mitigation

Industrial activity emits **pollution**, tracked per **region** (a coarse 64×64-block grid) in each
**dimension**: since 0.8.0 a region on Mars and the same region in the Overworld are separate. It
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

The [Multimeter](Tools.md#multimeter) reads the band (none, low, high, severe) of the region you point at, and
NeroMaps (when installed) has a pollution layer on its world map: the regions around you, in your own
dimension, shaded light, heavy or severe. It never shows a raw reading or who polluted.

The Analytics tab on each machine shows a **pollution rate**. Note that displayed rates are
**nominal** — derived from the machine's configured per-operation values and its current preset —
rather than a live measurement of the region.

## Threshold events

When a region's pollution crosses a configurable line (`pollutionEventThreshold`; `0` disables),
NeroTech publishes a **threshold event** through Neroland Core, on two channels so a listener can tell
cleanup from waiting:

| Channel | Fires |
| --- | --- |
| `nerotech:pollution` | **Rising** when machine emissions push a region past the line; **falling** only when a Scrubber or Remediator actively removes enough to bring it back under. |
| `nerotech:pollution_decay` | **Falling** when the periodic decay sweep alone carries a region back under the line. Never rises. |

Both are keyed by the place, never by a player. Since 0.8.0 the scope is `<dimension id>|<region key>`
(for example `minecraft:overworld|<key>`); before 0.8.0 it was the bare region key, so a listener that
parsed the old form needs updating. Nothing in NeroTech reacts to them itself; quest
packs and event mods listen (the shipped quest chapter's world-log quests, for instance — see
[NeroQuests Integration](NeroQuests-Integration.md)). A Remediator that actually removes pollution also
records the **Remediated** milestone for the player operating it at that moment (and for its recorded owner,
if it has one).

## Exposure and the Filter Mask

A region at the **severe** band (twice `pollutionEventThreshold`) gives players standing in it brief
Weakness and Hunger every `pollutionExposureIntervalTicks` (`pollutionExposureEffects`, on by default).
A worn, charged [Filter Mask](Tools.md#filter-mask-and-pollution-exposure) with a Filter Cartridge
blocks it, and the cartridge fouls into a Dirty Filter after `filterMaskPulsesPerCartridge` exposures.
The **Scrubber Module** cuts a machine's own emissions (see [Machines](Machines.md#upgrade-modules)).

## Config keys

`scrubberNePerOp`, `scrubberPollutionPerOp`, `scrubberAdjacentPermille`, `scrubberFilterCapacity`,
`remediatorNePerOp`, `remediatorPollutionPerOp`, `pollutionEventThreshold`, `pollutionExposureEffects`,
`pollutionExposureIntervalTicks`, `scrubberModulePollutionPermille`, `scrubberModuleFloorPermille` — alongside the base
pollution keys listed in [Consequence Systems](Consequence-Systems.md).

## See also

- [Consequence Systems](Consequence-Systems.md)
- [Thermal System](Thermal-System.md)
- [Analytics](Analytics.md)
- [Home](Home.md)
