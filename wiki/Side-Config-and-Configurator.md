# Side Config & the Configurator

Every NeroTech machine carries Neroland Core's **universal side-configuration** system: each of the
six faces can be set, per channel (**Power** / **Item**), to disabled, input, output or both. Faces
are **relative to the machine's facing**, so a layout travels with the block when it is rotated or
re-placed, and a face's capability is gated by its mode — pipes connect exactly where you allow
them.

There are two ways to work with it: the GUI tab, and (since 0.1.0-beta.1) the Configurator wrench.

## The Side Config tab

In the top-right of every machine GUI: a flattened cube of the six faces, per-channel mode buttons,
auto-eject / auto-input toggles, and reset / copy / paste. See
[Automation & Handoff](Automation.md#side-configuration) for the default layouts each machine
ships with.

## The Configurator (wrench)

A hand tool for reconfiguring machines **without opening a GUI** — crafted from iron ingots and a
Circuit Board:

- **Click a machine face** — cycles that face's mode (on the Item channel where the machine has
  one, otherwise Power).
- **Sneak-click** — reads out the machine's full side configuration to chat without changing
  anything.
- **Copy / paste** — sneak-click **away from a machine** toggles the wrench between *Configure* and
  *Copy/Paste* mode (the actionbar says which). In Copy/Paste mode, **click a machine** to copy its
  entire side configuration **and its [overclock preset](Overclock-Presets.md)** onto the wrench,
  then **sneak-click** another machine to apply it. Since the 0.1.0 beta, paste is **no longer
  type-locked**: each copied channel lands on the target only where the target declares that same
  channel, and the preset always lands. Copy an Ore Processor and you can stamp its Item + Power
  layout and its Overdrive setting across a whole row of processors, then keep going and drop just
  the Power layout and the preset onto the generators feeding them. If nothing on the clipboard
  applies to what you clicked, the wrench says so rather than silently doing nothing. The clipboard
  holds routing modes, a preset ordinal and a machine-type id — never any player data.
- **Wireless Power Nodes are the one exception** — sneak-clicking a node pairs or unpairs it instead
  of reading a layout. See [Power Generation](Power-Generation.md#wireless-power-node).

Since the beta, side configuration is also **visible in-world**: the Item Sorter's six port collars
tint live with their configured modes, so you can read a sorter's routing at a glance.

All changes — tab or wrench — are server-validated and synced, same as clicking in the GUI.

## See also

- [Automation & Handoff](Automation.md)
- [Tier-1 Machines](Machines.md)
- [Tech Guide](Tech-Guide.md)
- [Home](Home.md)
