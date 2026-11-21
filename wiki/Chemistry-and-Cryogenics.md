# Chemistry and Cryogenics

Five blocks handle gases, liquids and the things you distil, spin or react out of them. Gases use the same
unit as the Electrolyzer's hydrogen and oxygen: one gas unit is 100 mB. See
[Fluids & Gases](Fluids-and-Gases.md) for the older gas chain.

| Block | What it does |
| --- | --- |
| **Centrifuge** | Spins dusts, slurries and gases into by-products. Up to four side by side form a cascade. |
| **Cryo Distiller** | Distils ices and gases into fractions. Faster in cold places, slower in hot ones. |
| **Atmospheric Condenser** | Slowly draws the air around it into gas. What it collects depends on the world. |
| **Reaction Vat** | Reacts a fluid with a gas or items into acid, resin or coolant. |
| **Process Tank** | Holds one fluid or one gas. Stacks into a column with one shared level. |

## New fluids and gases

| Medium | Kind | Main uses |
| --- | --- | --- |
| Nitrogen | gas | cryo coolant, packed ice, Heat Exchanger coolant |
| Carbon dioxide | gas | cryo coolant, calcite |
| Methane | gas | polymer resin, Gas Turbine fuel |
| Deuterium | gas | the gas-fed fusion tier, helium-3 cascades |
| Helium-3 | gas | the gas-fed fusion tier |
| Cryo coolant | liquid, with bucket | Superconductor Coil, Synthetic Glacite, Flux Lattices, Heat Exchanger coolant |
| Sulfuric acid | liquid, with bucket | acid ore leaching, Capacitors |
| Polymer resin | liquid, with bucket | Polymer Sheet |

Nerospace's ices, shards and canisters never go straight into a tank: they enter through a machine (the Cryo
Distiller or the Centrifuge) by tag and come out as NeroTech gases or liquids.

## Centrifuge and cascades

A single Centrifuge splits a dust or slurry into by-products (vent sulfide into sulfur, glowstone into
europium dust, gunpowder into sulfur). Place up to four Centrifuges **side by side, facing the same way**, and
they form a cascade: each passes its gas output to the next (`centrifugeCascadePushPerTick` mB per tick). This
is how helium-3 is made without Nerospace: three deuterium into the first stage give two, the second gives
two, the third gives one helium-3 (see [Fusion fuel chain](Chain-Fusion-Fuel.md)).

## Cryo Distiller

Turns ices into gases and coolant, and coal with hydrogen or oxygen into methane or carbon dioxide. Its speed
follows the ambient heat: `factor = 1 - ambient x cryoColdBonusPerHeatPermille / 1000`, between
`cryoSpeedMinPermille` and `cryoSpeedMaxPermille`. On Glacira it runs about 1.4 times as fast; in the Nether
it runs at half speed.

## Atmospheric Condenser

Draws the dimension's air into gas, slowly. With Nerospace installed, the planet decides:

| World | Air | Collects |
| --- | --- | --- |
| Orbital stations, Luna, Europa's surface, the End | vacuum | nothing (the Condenser shows "no atmosphere" and idles) |
| Mars | thin carbon dioxide | carbon dioxide |
| Cindara, the Nether | sulfurous | carbon dioxide, and sometimes sulfur dust |
| Glacira | nitrogen and methane | nitrogen, fastest of all |
| Greenxertz | nitrogen and oxygen | nitrogen |
| Nyxara | nitrogen and oxygen, at half rate | nitrogen |
| Overworld and anything else | Earth-like | nitrogen, slowly |

Without Nerospace (or for any other mod's dimension) the `condenserAtmospheres` config decides, as a list of
`dimensionId=ATMOSPHERE`. Unlisted dimensions are Earth-like. The amounts are data in `nerotech:condensing`
recipes, one per atmosphere.

## Reaction Vat

Takes a fluid plus a gas or items and makes a fluid: sulfuric acid (sulfur, water, oxygen), polymer resin
(amber sap or logs with methane), cryo coolant (snow or blue ice with water and nitrogen or carbon dioxide).
It also runs **acid ore leaching**: one raw iron, copper or gold ore (or raw hematite) in 500 mB of sulfuric
acid gives four dusts, one more than the Chemical Processor. Filled buckets come back empty. The older
Chemical Processor still washes ore with oxygen; the two do not share recipes.

## Process Tank

Holds one medium at a time, fluid or gas. Stack up to 16 tanks and they form one column with one level; the
column's capacity is the sum. Tanks expose standard fluid and gas handlers on every loader, and give a
comparator signal for the fill level. Machines next to a tank push their fluid and gas outputs into it and
pull inputs from it according to their side configuration.

- Sneak-use with an empty hand vents one tank's worth.
- Broken with a pickaxe, a tank keeps its contents on the item.
- The [Heat Exchanger](Heat-as-a-Resource.md#heat-exchanger) uses the tank on its back as a heat store or
  coolant supply.

## See also

- [Chemistry chain](Chain-Chemistry.md), [Fusion fuel chain](Chain-Fusion-Fuel.md)
- [Planets](Planets.md)
- [Material Map](Material-Map.md)
- [Home](Home.md)
