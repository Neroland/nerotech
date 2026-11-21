"""Chemistry machine sounds (A3): centrifuge, cryo distiller, atmospheric condenser, reaction vat.

``recipe(k, rng)``: ``k`` is the synthesis kit (gen_sounds), ``rng`` is seeded from the event id.
All four are built from oscillators and noise only.
"""
import math


def centrifuge_spin(k, rng):
    """Centrifuge whines: a sine sweep up, held, and back down, over a faint motor hum."""
    dur = 1.9

    def pitch(p):
        if p < 0.3:
            return 180.0 + (640.0 - 180.0) * (p / 0.3) ** 0.7
        if p < 0.72:
            return 640.0 + 6.0 * math.sin(p * 60.0)
        return 640.0 - (640.0 - 210.0) * ((p - 0.72) / 0.28) ** 1.3

    whine = k.mix(k.sine(pitch, dur), k.gain(k.sine(lambda p: pitch(p) * 2.0, dur), 0.22),
                  k.gain(k.sine(lambda p: pitch(p) * 3.01, dur), 0.08))
    hum = k.gain(k.lowpass(k.saw(lambda p: pitch(p) * 0.25, dur), 420.0), 0.25)
    air = k.gain(k.bandpass(k.noise(rng, dur), 1200.0, 4200.0), 0.05)
    out = k.mix(whine, hum, air)
    return k.env(out, [(0.0, 0.0), (0.06, 0.55), (0.3, 1.0), (0.72, 0.95), (1.0, 0.0)])


def cryo_vent(k, rng):
    """Distiller vents: a short bright hiss with a cold, glassy tail."""
    dur = 0.55
    hiss = k.highpass(k.noise(rng, dur), (5200.0, 2600.0))
    hiss = k.env(hiss, [(0.0, 0.0), (0.03, 1.0), (0.25, 0.55), (1.0, 0.0)])
    ping = k.decay(k.fm(2350.0, 1.41, 1.2, dur), 0.05)
    return k.mix(hiss, k.gain(ping, 0.12))


def condenser_draw(k, rng):
    """Condenser draws air: a breath-like band-passed noise loop, in and out."""
    dur = 2.0 + 0.15                       # loopable() drops the 0.15 s overlap
    breath = k.bandpass(k.noise(rng, dur), 260.0,
                        lambda p: 900.0 + 700.0 * math.sin(math.pi * ((p * dur / 2.0) % 1.0)))
    breath = [v * (0.45 + 0.55 * math.sin(math.pi * ((i / k.RATE / 2.0) % 1.0)) ** 2)
              for i, v in enumerate(breath)]
    rumble = k.gain(k.lowpass(k.noise(rng, dur, "brown"), 140.0), 0.35)
    return k.loopable(k.mix(breath, rumble), 0.15)


def vat_bubble(k, rng):
    """Vat bubbles: random pitched blips, each a quick upward chirp, over a low simmer."""
    dur = 1.6
    tracks = [k.gain(k.lowpass(k.noise(rng, dur, "brown"), 300.0), 0.22)]
    t = 0.03
    while t < dur - 0.16:
        base = rng.uniform(260.0, 760.0)
        length = rng.uniform(0.05, 0.11)
        blip = k.sine((base, base * rng.uniform(1.5, 2.1)), length)
        blip = k.fade(k.env(blip, [(0.0, 0.0), (0.2, 1.0), (1.0, 0.0)]), 0.003, 0.01)
        tracks.append(k.at(k.gain(blip, rng.uniform(0.4, 0.9)), t))
        t += rng.uniform(0.07, 0.24)
    return k.env(k.mix(*tracks), [(0.0, 0.0), (0.04, 1.0), (0.9, 1.0), (1.0, 0.0)])


SOUNDS = [
    ("centrifuge.spin", centrifuge_spin, {"subtitle": "Centrifuge whines"}),
    ("cryo_distiller.vent", cryo_vent, {"subtitle": "Distiller vents"}),
    ("atmospheric_condenser.draw", condenser_draw, {"subtitle": "Condenser draws air", "loop": True}),
    ("reaction_vat.bubble", vat_bubble, {"subtitle": "Vat bubbles"}),
]
