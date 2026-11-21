# Fusion Fuel: the gas-fed tier

0.8.0 gives the [Fusion Reactor](Fusion-Reactor.md) a fuel you pipe in instead of crafting cells, and a way to
calm a hot shell. Both are blocks you build into the reactor's shell.

| Block | What it does |
| --- | --- |
| **Fuel Injector** | A shell port that holds deuterium and helium-3 and feeds them to the reactor. It feeds the Fusion Reactor only. |
| **Europium Flux Lattice** | A shell block that replaces Fusion Casing. Each one in a formed shell takes heat out of the burn. |

## Gas-fed burn

- Put one or more Fuel Injectors in the shell walls in place of casing, and pipe **deuterium** and
  **helium-3** into them (each injector has a tank of `fuelInjectorGasCapacity` mB of each gas).
- The burn draws gas a charge at a time, not every tick: each charge buys `fusionGasChargeTicks` ticks of burn
  and takes `fusionGasDeuteriumPerTick` deuterium and `fusionGasHelium3PerTick` helium-3 per tick of it, so two
  parts deuterium to one part helium-3. A charge needs both gases; a surplus of one never covers the other.
- A gas-fed burn produces `fusionGasOutputPermille` (1.5x) of a fuel-cell burn in the same shell, on top of the
  shell size multiplier.
- Gas is the top fuel tier, so by default only a **7x7x7** shell can hold it (`fusionGasMinShellSize`).

The first gas-fed ignition records the **gas-fed ignition** milestone for the player operating the reactor.

## Europium Flux Lattice

Each lattice in a formed shell removes `fusionLatticeHeatReductionPermille` (6%) of the reactor's heat per
burning tick. Only the first `fusionLatticeMaxCount` (8) count, so the most a shell gains is 48% less heat.
Lattices make a big shell safer to run on Overdrive; they do not stop a meltdown if the reactor is left without
cooling.

## Where the gases come from

| Gas | Offworld (with Nerospace) | Standalone |
| --- | --- | --- |
| Deuterium | deuterium shards or cells in the Cryo Distiller | Electrolyzer hydrogen and a little water in the Centrifuge |
| Helium-3 | helium-3 canisters in the Cryo Distiller | a three-stage [Centrifuge cascade](Chemistry-and-Cryogenics.md#centrifuge-and-cascades) on deuterium |

Helium-3 is deliberately the slowest thing to make without Nerospace. Europium dust for the lattices comes
from raw europium (Europa) or glowstone dust in the Centrifuge. See the [Fusion fuel chain](Chain-Fusion-Fuel.md).

## See also

- [Fusion Reactor](Fusion-Reactor.md)
- [Heat as a Resource](Heat-as-a-Resource.md)
- [Config](Config.md)
- [Home](Home.md)
