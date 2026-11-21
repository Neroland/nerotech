"""A5 Heat + Extraction sounds: Heat Pipe, Deep Core Drill and the Survey Scanner.

``recipe(k, rng)``: ``k`` is the synthesis kit (gen_sounds), ``rng`` is seeded from the event id.
All three are synthesised here from oscillators and noise; nothing is sampled.
"""


def tick(k, rng):
    """Pipe ticks: one dry metallic tick with a short ring, as hot copper creeps. Level-3 pipes, rarely."""
    snap = k.decay(k.highpass(k.noise(rng, 0.03), 3200.0), 0.003)
    ring = k.decay(k.fm(2140.0, 1.41, 1.6, 0.22), 0.028)
    body = k.decay(k.sine((760.0, 690.0), 0.12), 0.018)
    return k.mix(snap, k.gain(ring, 0.55), k.gain(body, 0.35))


def grind(k, rng):
    """Drill grinds: a low saw under a brown-noise rumble, with the bit's slow chatter. Seamless loop."""
    dur = 1.75                                    # loopable() keeps 1.6 s: whole cycles of 5, 57.5 and 86.875 Hz
    saw = k.lowpass(k.saw(57.5, dur), 420.0)
    growl = k.lowpass(k.saw(86.875, dur), 300.0)
    rumble = k.lowpass(k.noise(rng, dur, "brown"), 180.0)
    grit = k.gain(k.bandpass(k.noise(rng, dur), 700.0, 2200.0), 0.10)
    body = k.mix(k.gain(saw, 0.55), k.gain(growl, 0.25), k.gain(rumble, 0.8), grit)
    return k.loopable(k.tremolo(body, 5.0, 0.35))


def scan(k, rng):
    """Scanner pings: a sonar blip going out and a softer, lower echo coming back."""
    def blip(freq, dur, half):
        tone = k.mix(k.sine((freq * 1.04, freq), dur), k.gain(k.sine(freq * 2.0, dur), 0.12))
        return k.fade(k.decay(tone, half), 0.004, 0.03)

    out = blip(1180.0, 0.5, 0.09)
    back = k.lowpass(blip(885.0, 0.45, 0.11), 2400.0)
    return k.concat(out, k.silence(0.05), k.gain(back, 0.35))


SOUNDS = [
    ("heat_pipe.tick", tick, {"subtitle": "Pipe ticks"}),
    ("deep_core_drill.grind", grind, {"subtitle": "Drill grinds", "loop": True}),
    ("tool.scan", scan, {"subtitle": "Scanner pings"}),
]
