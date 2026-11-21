"""Shared machine sounds: the two every machine can make, whatever its family.

``recipe(k, rng)``: ``k`` is the synthesis kit (gen_sounds), ``rng`` is seeded from the event id.
"""


def stall(k, rng):
    """Machine stalls: two falling sine chirps. Played once on entering STALL, never looped."""
    def chirp(start, end, dur):
        tone = k.mix(k.sine((start, end), dur), k.gain(k.sine((start * 2.0, end * 2.0), dur), 0.18))
        return k.fade(k.env(tone, [(0.0, 0.0), (0.06, 1.0), (0.55, 0.6), (1.0, 0.0)]), 0.004, 0.02)

    return k.concat(chirp(620.0, 340.0, 0.16), k.silence(0.07), k.gain(chirp(470.0, 230.0, 0.22), 0.85))


def overheat(k, rng):
    """Machine overheats: filtered-noise hiss, rising in pitch and level, then venting off."""
    dur = 1.4
    hiss = k.bandpass(k.noise(rng, dur), 900.0, (1800.0, 9000.0))
    hiss = k.env(hiss, [(0.0, 0.0), (0.08, 0.25), (0.7, 1.0), (0.82, 0.9), (1.0, 0.0)])
    whistle = k.env(k.sine((1400.0, 2600.0), dur), [(0.0, 0.0), (0.4, 0.0), (0.75, 0.10), (1.0, 0.0)])
    return k.mix(hiss, whistle)


SOUNDS = [
    ("machine.stall", stall, {"subtitle": "Machine stalls"}),
    ("machine.overheat", overheat, {"subtitle": "Machine overheats"}),
]
