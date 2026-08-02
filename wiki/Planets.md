# Planets (Nerospace Interop)

NeroTech is planet-aware — **optionally**. With **Nerospace 1.0.0-beta.7 or newer** installed,
machines automatically read each world's planet traits; without it, NeroTech runs fully standalone
on Earth-style defaults with a config fallback. Nerospace is never required.

## What planet traits change

Two things respond to the world you build on:

- **Solar Array output.** Airless worlds (no atmosphere to scatter light) boost solar by ×1.25;
  hot-hazard worlds add ×1.2; cold-hazard worlds halve-and-then-some at ×0.5. So a Solar Array on
  scorched **Cindara** produces 1.5× its Earth output, while one on frozen **Glacira** manages
  about 0.625×.
- **Thermal ambient.** Hot-hazard planets set a high ambient (machines start warm and cool
  poorly — around 200 on the heat scale); cold-hazard planets set a deep negative ambient (around
  −80 — free cooling for your [fusion plant](Fusion-Reactor.md), if you can get it there). See the
  [Thermal System](Thermal-System.md).

## Precedence & the config fallback

For both solar and ambient, NeroTech resolves values in this order:

1. **Nerospace planet traits** — when Nerospace is installed and the dimension is one of its
   planets;
2. **per-dimension server config** — `solarDimensionMultipliers` and `thermalAmbientByDimension`
   (comma-lists of `dimensionId=value`; the Nether ships hot by default), which also lets servers
   tune vanilla or third-party dimensions;
3. **defaults** — multiplier 1.0 and `thermalAmbientDefault`.

So a server without Nerospace can still make its custom mining dimension run hot, and a server with
Nerospace gets sensible planet behaviour with zero configuration.

## How the coupling works

The integration is deliberately soft: NeroTech only queries Nerospace's stable planet API when
Nerospace is actually present, and the advanced tier's material gate (Starsteel, Void Crystal) runs
through shared `c:` tags as before — there is no hard dependency in either direction. See
[Advanced Tier](Advanced-Tier.md) for how space gates the late game.

## See also

- [Thermal System](Thermal-System.md)
- [Advanced Tier](Advanced-Tier.md)
- [Tier-1 Machines](Machines.md)
- [Home](Home.md)
