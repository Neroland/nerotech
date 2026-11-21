"""A1 Metallurgy sounds: the Plasma Crucible arc loop, the Plate Press stamp and the Filament Spooler
winding loop. All synthesised here from oscillators and noise; nothing sampled.

``recipe(k, rng)``: ``k`` is the synthesis kit (gen_sounds), ``rng`` is seeded from the event id.
"""
import math


def crucible_arc(k, rng):
    """Crucible arcs (loop, 1.6 s): a buzzing saw under gated noise crackle.

    The buzz is a 100 Hz saw (with a fifth above it) through a slowly moving low-pass, wobbled by a
    tremolo; the crackle is short bright noise bursts at random moments. ``loopable`` cross-fades
    the seam.
    """
    dur = 1.6 + 0.15
    buzz = k.mix(k.saw(100.0, dur), k.gain(k.saw(150.0, dur, 0.31), 0.45), k.gain(k.osc("square", 50.0, dur), 0.25))
    buzz = k.lowpass(buzz, lambda p: 1500.0 + 900.0 * math.sin(2.0 * math.pi * p * 2.0))
    buzz = k.tremolo(buzz, 13.0, 0.45)
    crackle = k.silence(dur)
    hiss = k.highpass(k.noise(rng, dur), 2600.0)
    t = 0.03
    while t < dur - 0.06:
        length = rng.uniform(0.008, 0.035)
        start, n = k.seconds(t), k.seconds(length)
        level = rng.uniform(0.5, 1.0)
        for i in range(n):
            crackle[start + i] = hiss[start + i] * level * (1.0 - i / n)
        t += length + rng.uniform(0.015, 0.11)
    fizz = k.gain(k.bandpass(k.noise(rng, dur), 3000.0, 7000.0), 0.10)
    return k.loopable(k.mix(k.gain(buzz, 0.55), k.gain(crackle, 0.8), fizz), 0.15)


def press_stamp(k, rng):
    """Press stamps (one shot, about 0.65 s): a low thud, then a short metallic ring."""
    thud = k.decay(k.sine((120.0, 42.0), 0.32), 0.05)
    knock = k.decay(k.lowpass(k.noise(rng, 0.08), 900.0), 0.012)
    ring_dur = 0.62
    ring = k.silence(ring_dur)
    for freq, level, half in ((523.0, 1.0, 0.09), (1217.0, 0.55, 0.06), (1889.0, 0.35, 0.04), (2741.0, 0.2, 0.03)):
        ring = k.mix(ring, k.gain(k.decay(k.sine(freq, ring_dur, rng.random()), half), level))
    ring = k.fade(ring, 0.002, 0.05)
    hiss = k.decay(k.highpass(k.noise(rng, 0.25), 3000.0), 0.04)       # hydraulic release
    return k.mix(thud, k.gain(knock, 0.8), k.at(k.gain(ring, 0.30), 0.012), k.at(k.gain(hiss, 0.12), 0.16))


def spooler_spin(k, rng):
    """Spooler winds (loop, 1.5 s): a motor whirr with a 6 Hz flutter and a faint wire sing.

    6 Hz over 1.5 s is nine whole flutter cycles, so the flutter closes on the loop point.
    """
    dur = 1.5 + 0.15

    def flutter(p):
        return 1.0 + 0.035 * math.sin(2.0 * math.pi * 6.0 * p * dur)

    whirr = k.mix(k.saw(lambda p: 182.0 * flutter(p), dur),
                  k.gain(k.osc("tri", lambda p: 364.0 * flutter(p), dur), 0.5),
                  k.gain(k.sine(lambda p: 91.0 * flutter(p), dur), 0.6))
    whirr = k.lowpass(whirr, 1400.0)
    whirr = k.tremolo(whirr, 6.0, 0.35)
    air = k.gain(k.tremolo(k.bandpass(k.noise(rng, dur), 700.0, 2600.0), 6.0, 0.6), 0.16)
    sing = k.gain(k.tremolo(k.sine(lambda p: 1460.0 * flutter(p), dur), 12.0, 0.5), 0.05)
    return k.loopable(k.mix(k.gain(whirr, 0.6), air, sing), 0.15)


SOUNDS = [
    ("plasma_crucible.arc", crucible_arc, {"subtitle": "Crucible arcs", "loop": True}),
    ("plate_press.stamp", press_stamp, {"subtitle": "Press stamps"}),
    ("filament_spooler.spin", spooler_spin, {"subtitle": "Spooler winds", "loop": True}),
]
