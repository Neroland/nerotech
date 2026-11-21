"""A6 Fusion + Utility sounds: Fuel Injector, Induction Charger, Annunciator Panel.

``recipe(k, rng)``: ``k`` is the synthesis kit (gen_sounds), ``rng`` is seeded from the event id.
All three are built from oscillators and filtered noise only.
"""
import math


def injector_pulse(k, rng):
    """Injector pulses: a soft double puff, one per gas, the second a little higher and quieter."""
    def puff(low, high, thump, dur):
        air = k.bandpass(k.noise(rng, dur), low, high)
        air = k.env(air, [(0.0, 0.0), (0.12, 1.0), (0.4, 0.45), (1.0, 0.0)])
        body = k.env(k.sine((thump, thump * 0.6), dur), [(0.0, 0.0), (0.08, 1.0), (0.5, 0.25), (1.0, 0.0)])
        return k.fade(k.mix(air, k.gain(body, 0.55)), 0.004, 0.03)

    first = puff(300.0, 1500.0, 96.0, 0.13)
    second = puff(420.0, 2100.0, 128.0, 0.11)
    return k.mix(first, k.at(k.gain(second, 0.75), 0.17))


def charger_hum(k, rng):
    """Charger hums (loop, 1.6 s): mains-style hum with its harmonics and a slow glassy shimmer.

    50 Hz over 1.6 s is 80 whole cycles and the 5 Hz shimmer is 8, so every tone closes on the loop
    point; only the faint air bed relies on the cross-fade.
    """
    dur = 1.6 + 0.15
    hum = k.mix(k.sine(50.0, dur), k.gain(k.sine(100.0, dur), 0.55), k.gain(k.sine(150.0, dur), 0.30),
                k.gain(k.osc("tri", 250.0, dur), 0.10))
    hum = k.tremolo(hum, 2.5, 0.12)
    shimmer = k.mix(k.sine(2400.0, dur), k.gain(k.sine(3600.0, dur), 0.5))
    shimmer = k.gain(k.tremolo(shimmer, 5.0, 0.9), 0.045)
    air = k.gain(k.bandpass(k.noise(rng, dur), 1800.0, 5200.0), 0.025)
    return k.loopable(k.mix(k.gain(hum, 0.7), shimmer, air), 0.15)


def panel_alarm(k, rng):
    """Alarm sounds: a two-tone beep, high then low, once per new alarm."""
    def beep(freq, dur):
        tone = k.mix(k.osc("square", freq, dur), k.gain(k.sine(freq * 2.0, dur), 0.3))
        tone = k.lowpass(tone, 2600.0)
        return k.fade(k.env(tone, [(0.0, 0.0), (0.04, 1.0), (0.85, 0.9), (1.0, 0.0)]), 0.004, 0.012)

    return k.concat(beep(932.0, 0.15), k.silence(0.05), beep(698.0, 0.21))


SOUNDS = [
    ("fuel_injector.pulse", injector_pulse, {"subtitle": "Injector pulses"}),
    ("induction_charger.hum", charger_hum, {"subtitle": "Charger hums", "loop": True}),
    ("annunciator_panel.alarm", panel_alarm, {"subtitle": "Alarm sounds"}),
]
