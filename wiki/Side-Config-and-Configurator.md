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
- **Copy / paste** — the Configurator can store a machine's entire side configuration and apply it
  to other machines **of the same type**: sneak-click to read also captures the layout, then click
  another machine of that type to paste. Perfect for stamping one processor's layout across a whole
  row.

Since the beta, side configuration is also **visible in-world**: the Item Sorter's six port collars
tint live with their configured modes, so you can read a sorter's routing at a glance.

All changes — tab or wrench — are server-validated and synced, same as clicking in the GUI.

## See also

- [Automation & Handoff](Automation.md)
- [Tier-1 Machines](Machines.md)
- [Tech Guide](Tech-Guide.md)
- [Home](Home.md)
