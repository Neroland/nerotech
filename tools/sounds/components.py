"""A4 Components sounds: Component Forge, Laser Etcher, Crystal Grower.

``recipe(k, rng)``: ``k`` is the synthesis kit (gen_sounds), ``rng`` is seeded from the event id.
All three are synthesised here from oscillators and noise; nothing is sampled.
"""


def assemble(k, rng):
    """Forge assembles: three servo ticks (short pitched whirrs, each a little higher) and a click."""
    def tick(pitch):
        whirr = k.saw((pitch * 0.8, pitch), 0.07)
        whirr = k.lowpass(k.mix(whirr, k.gain(k.sine((pitch * 2.0, pitch * 2.5), 0.07), 0.3)), 2600.0)
        return k.fade(k.env(whirr, [(0.0, 0.0), (0.15, 1.0), (0.8, 0.7), (1.0, 0.0)]), 0.003, 0.01)

    click = k.decay(k.highpass(k.noise(rng, 0.05), 2200.0), 0.006)
    seat = k.decay(k.sine((900.0, 520.0), 0.09), 0.02)
    return k.concat(
        k.gain(tick(310.0), 0.7), k.silence(0.06),
        k.gain(tick(350.0), 0.75), k.silence(0.06),
        k.gain(tick(395.0), 0.8), k.silence(0.09),
        k.mix(click, k.gain(seat, 0.6)))


def etch(k, rng):
    """Etcher hums: a thin high saw with tremolo, over a faint hiss. Seamless loop (WORK only)."""
    dur = 1.65
    saw = k.highpass(k.saw(1480.0, dur), 900.0)
    body = k.mix(k.gain(saw, 0.5), k.gain(k.sine(2960.0, dur), 0.18), k.gain(k.sine(740.0, dur), 0.12))
    body = k.tremolo(body, 10.0, 0.45)            # the kept 1.5 s holds 15 whole cycles: the loop point is clean
    hiss = k.gain(k.bandpass(k.noise(rng, dur), 5000.0, 9000.0), 0.06)
    return k.loopable(k.mix(body, hiss))


def chime(k, rng):
    """Crystal chimes: one glassy FM bell with a soft octave-and-fifth halo. Completion only."""
    dur = 1.5
    bell = k.decay(k.fm(1318.5, 3.01, 2.2, dur), 0.22)
    halo = k.decay(k.sine(1975.5, dur), 0.35)
    ping = k.decay(k.highpass(k.noise(rng, 0.03), 6000.0), 0.004)
    return k.mix(bell, k.gain(halo, 0.25), k.gain(ping, 0.2))


SOUNDS = [
    ("component_forge.assemble", assemble, {"subtitle": "Forge assembles"}),
    ("laser_etcher.etch", etch, {"subtitle": "Etcher hums", "loop": True}),
    ("crystal_grower.chime", chime, {"subtitle": "Crystal chimes"}),
]
