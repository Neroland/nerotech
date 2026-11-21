"""A2 Foundry sounds: the Blast Foundry sealing, roaring and pouring.

``recipe(k, rng)``: ``k`` is the synthesis kit (gen_sounds), ``rng`` is seeded from the event id.
All three are built from noise and oscillators here; nothing is sampled.
"""


def form(k, rng):
    """Foundry seals: a rising rumble that closes into a low bell. One-shot, on forming."""
    rise = 0.95
    rumble = k.lowpass(k.noise(rng, rise, "brown"), (90.0, 420.0))
    rumble = k.env(rumble, [(0.0, 0.0), (0.15, 0.25), (0.85, 1.0), (1.0, 0.55)])
    swell = k.env(k.sine((38.0, 92.0), rise), [(0.0, 0.0), (0.7, 0.8), (1.0, 0.4)])
    thud = k.decay(k.sine((120.0, 48.0), 0.22), 0.05)
    bell_len = 1.7
    bell = k.fm(98.0, 1.41, lambda p: 3.2 * (1.0 - p) ** 2, bell_len)
    bell = k.mix(bell, k.gain(k.sine(196.0, bell_len), 0.35), k.gain(k.sine(293.5, bell_len), 0.12))
    bell = k.decay(bell, 0.36)
    tail = k.gain(k.decay(k.lowpass(k.noise(rng, bell_len, "brown"), 260.0), 0.25), 0.5)
    return k.mix(k.gain(rumble, 0.9), k.gain(swell, 0.6), k.at(k.gain(thud, 0.9), rise - 0.02),
                 k.at(k.gain(bell, 0.8), rise), k.at(tail, rise))


def roar(k, rng):
    """Foundry roars: a brown-noise loop with one slow swell per turn. Looped while working."""
    body, overlap = 2.0, 0.15
    dur = body + overlap
    low = k.lowpass(k.noise(rng, dur, "brown"), 380.0)
    air = k.gain(k.bandpass(k.noise(rng, dur), 500.0, 1700.0), 0.16)
    flutter = k.tremolo(k.gain(k.lowpass(k.noise(rng, dur, "brown"), 120.0), 0.6), 7.0, 0.35)
    # 0.5 Hz swell: exactly one cycle per 2 s body, so the cross-faded tail is in phase with the head.
    return k.loopable(k.tremolo(k.mix(low, air, flutter), 0.5, 0.45), overlap)


def pour(k, rng):
    """Metal pours: a descending noise sweep (the stream) under a crackling sizzle that dies away."""
    dur = 1.7
    n = k.seconds(dur)
    stream = k.bandpass(k.noise(rng, dur), (900.0, 160.0), (4200.0, 620.0))
    stream = k.env(stream, [(0.0, 0.0), (0.05, 1.0), (0.55, 0.7), (0.8, 0.2), (1.0, 0.0)])
    glug = k.env(k.sine((210.0, 70.0), dur), [(0.0, 0.0), (0.08, 0.35), (0.6, 0.2), (1.0, 0.0)])
    hiss = k.highpass(k.noise(rng, dur), 5200.0)
    crackle = [0.0] * n
    level = 0.0
    for i in range(n):
        if rng.random() < 0.004:
            level = rng.uniform(0.5, 1.0)
        level *= 0.9988
        crackle[i] = hiss[i] * (0.18 + level)
    crackle = k.env(crackle, [(0.0, 0.0), (0.1, 0.5), (0.35, 1.0), (1.0, 0.0)])
    return k.mix(k.gain(stream, 1.0), k.gain(glug, 0.5), k.gain(crackle, 0.45))


SOUNDS = [
    ("blast_foundry.form", form, {"subtitle": "Foundry seals"}),
    ("blast_foundry.roar", roar, {"subtitle": "Foundry roars", "loop": True}),
    ("blast_foundry.pour", pour, {"subtitle": "Metal pours"}),
]
