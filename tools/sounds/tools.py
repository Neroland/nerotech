"""A8 Tools sounds: Power Drill, Cutting Lance and Arc Wrench (the Survey Scanner's ping is A5's).

``recipe(k, rng)``: ``k`` is the synthesis kit (gen_sounds), ``rng`` is seeded from the event id.
All three are one-shots synthesised here from oscillators and noise; nothing is sampled.
"""


def power_drill(k, rng):
    """Drill whirs: a small motor spinning up, biting, and winding down."""
    dur = 0.52

    def rpm(p):                                   # spin up fast, hold, sag as it stops
        if p < 0.18:
            return 150.0 + 170.0 * (p / 0.18)
        if p < 0.78:
            return 320.0
        return 320.0 - 120.0 * ((p - 0.78) / 0.22)

    motor = k.lowpass(k.saw(rpm, dur), 1900.0)
    whine = k.gain(k.sine(lambda p: rpm(p) * 6.0, dur), 0.16)
    gears = k.gain(k.bandpass(k.noise(rng, dur), 1800.0, 5200.0), 0.22)
    body = k.tremolo(k.mix(k.gain(motor, 0.6), whine, gears), 43.0, 0.45)
    return k.env(body, [(0.0, 0.0), (0.06, 1.0), (0.78, 0.9), (1.0, 0.0)])


def cutting_lance(k, rng):
    """Lance fires: a bright zap sweeping down, with a short crackle on top of it."""
    dur = 0.34
    beam = k.mix(k.gain(k.saw((2600.0, 340.0), dur), 0.45), k.gain(k.sine((5200.0, 680.0), dur), 0.4))
    beam = k.lowpass(beam, (9000.0, 1800.0))
    buzz = k.gain(k.tremolo(k.sine((1300.0, 170.0), dur), 96.0, 0.9), 0.3)
    crackle = k.decay(k.highpass(k.noise(rng, 0.12), 4200.0), 0.03)
    tail = k.decay(k.mix(beam, buzz), 0.085)
    return k.mix(tail, k.gain(crackle, 0.5))


def arc_wrench(k, rng):
    """Wrench arcs: a dry snap across the gap, a brief mains-like buzz, then the jaw's click."""
    snap = k.decay(k.highpass(k.noise(rng, 0.04), 5200.0), 0.004)
    arc = k.osc("square", 118.0, 0.11)
    arc = k.decay(k.bandpass(k.mix(arc, k.gain(k.noise(rng, 0.11), 0.6)), 900.0, 6400.0), 0.035)
    click = k.decay(k.fm(1720.0, 1.48, 1.2, 0.09), 0.014)
    return k.mix(snap, k.gain(arc, 0.55), k.at(k.gain(click, 0.4), 0.13))


SOUNDS = [
    ("tool.power_drill", power_drill, {"subtitle": "Drill whirs"}),
    ("tool.cutting_lance", cutting_lance, {"subtitle": "Lance fires"}),
    ("tool.arc_wrench", arc_wrench, {"subtitle": "Wrench arcs"}),
]
