# Power and NeroPower

NeroTech is a complete power mod on its own. **NeroPower** is an optional **add-on** that builds on
top of it for players who want deeper power engineering. You never need NeroPower to play NeroTech,
and nothing in NeroTech is locked behind it.

## What NeroTech owns

Every power block below ships in NeroTech and stays here:

- **Generation** — the [Nero Generator and Solar Array](Machines.md), then the
  [Wind Turbine, Geothermal Generator and Bio Generator](Power-Generation.md), up to the
  [Fusion Reactor](Fusion-Reactor.md).
- **Storage and distribution** — the [Battery Bank, Grid Controller and Wireless Power Node](Power-Generation.md).
- **Cooling** — the [coolant loop](Fluids-and-Gases.md) (Coolant Pump and Radiators).

All of it sits on Neroland Core's shared energy surface, so it interoperates with Core storage, with
the rest of the ecosystem and with Forge Energy at the I/O boundary.

## What NeroPower adds

NeroPower is a separate download that **requires Neroland Core and NeroTech**. It adds:

- **Fission Reactor** — a Fission Core in a Fission Casing shell, throttled by Control Rod
  Assemblies, with a **fuel cycle**: Uranium Pellets become Fuel Rods, burn down to Spent Fuel Rods,
  and are reprocessed into Reprocessed Pellets.
- **Staged failure** — reactors climb a visible ladder (Stable, Warning, Unstable, Failure) as heat
  rises, with time to act at every rung.
- **Tiered pooled battery banks** — Basic, Advanced and Elite Battery Cells pooled into one store by
  a Battery Bank Controller.
- **Beamed power** — Beam Transmitters, Receivers and Relays move energy along a line of sight; an
  Orbital Receiver accepts a beam from another dimension.
- **RTG and Stirling generators** — the Radioisotope Generator (Isotope Pellets) and the Stirling
  Generator.

NeroPower builds on NeroTech's public add-on API and on Neroland Core's shared energy surface, so
its machines exchange power with NeroTech's generators and storage. See NeroPower's own wiki for its
blocks, numbers and configuration.

## History

NeroPower was first planned as a split that would move power out of NeroTech. That split was
dropped on 2026-07-31: the basic power tier moved into NeroTech, where it remains. NeroPower was
later revived with a different job — adding depth on top of NeroTech rather than taking power
away from it.

## See also

- [Power Generation](Power-Generation.md)
- [Tier-1 Machines](Machines.md)
- [Advanced Tier](Advanced-Tier.md)
- [Consequence Systems](Consequence-Systems.md)
- [Home](Home.md)
