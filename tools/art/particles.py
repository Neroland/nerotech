"""Particle sprites (8x8) and particle definitions for NeroTech 0.8.0.

Four sprite sets, everything else is drawn by block-entity renderers:

* ``steam_wisp``  -- HOT state on every machine (4 frames: puff grows and thins)
* ``frost_mote``  -- Cryo Distiller, cryo coolant (3 frames: twinkling flake)
* ``spark``       -- teal; Plasma Crucible, Arc Wrench, Component Forge (3 frames: flash to ember)
* ``rock_chip``   -- Deep Core Drill (3 frames: tumbling chip)

Sprites go to ``textures/particle/<name>_<n>.png`` and the definitions to
``particles/<name>.json``. Registering the particle types and their providers is integration work.
Spawn rules (client side): at most 2 per machine per second, none with renderAnimationsEnabled=false.
"""
import math

from PIL import Image

from art import common as K

P = 8

# name -> frame count
SETS = {"steam_wisp": 4, "frost_mote": 3, "spark": 3, "rock_chip": 3}

STEAM = [(236, 242, 246, 255), (206, 216, 224, 255), (176, 188, 198, 255)]
ROCK = [(112, 106, 100, 255), (84, 80, 78, 255), (146, 138, 128, 255)]


def _img():
    return Image.new("RGBA", (P, P), K.CLEAR)


def _steam(frame, rng):
    img = _img()
    px = img.load()
    radius = 1.6 + frame * 0.75                 # grows
    keep = 1.0 - frame * 0.22                   # thins
    for y in range(P):
        for x in range(P):
            d = math.hypot(x - 3.5, y - 3.5 + frame * 0.3)
            if d <= radius and rng.random() < keep:
                px[x, y] = STEAM[0] if d < radius * 0.5 else STEAM[1 + (x + y) % 2]
    return img


def _frost(frame, rng):
    img = _img()
    px = img.load()
    arm = (3, 2, 1)[frame]
    for i in range(-arm, arm + 1):
        px[3 + i, 3] = K.FROST[1]
        px[3, 3 + i] = K.FROST[1]
    if frame < 2:
        for dx, dy in ((-1, -1), (1, -1), (-1, 1), (1, 1)):
            px[3 + dx, 3 + dy] = K.FROST[0] if frame == 0 else K.FROST[1]
    px[3, 3] = (255, 255, 255, 255)
    return img


def _spark(frame, rng):
    img = _img()
    px = img.load()
    if frame == 0:                               # flash: plus shape with a glow core
        for i in range(-2, 3):
            px[3 + i, 3] = K.T_PLASMA
            px[3, 3 + i] = K.T_PLASMA
        px[3, 3] = K.T_GLOW
    elif frame == 1:
        for dx, dy in ((0, 0), (1, 0), (0, 1), (-1, 0), (0, -1)):
            px[3 + dx, 3 + dy] = K.T_CYAN
        px[3, 3] = K.T_PLASMA
    else:                                        # ember
        px[3, 3] = K.T_CYAN
        px[4, 3] = K.T_TEAL
    return img


def _rock(frame, rng):
    img = _img()
    px = img.load()
    shapes = (
        ((2, 2), (3, 2), (4, 2), (2, 3), (3, 3), (4, 3), (5, 3), (3, 4), (4, 4)),
        ((3, 1), (2, 2), (3, 2), (4, 2), (3, 3), (4, 3), (3, 4), (4, 4), (4, 5)),
        ((2, 3), (3, 3), (4, 3), (5, 3), (3, 2), (4, 2), (2, 4), (3, 4), (4, 4)),
    )
    for (x, y) in shapes[frame]:
        px[x, y] = rng.choice(ROCK[:2])
    x, y = shapes[frame][0]
    px[x, y] = ROCK[2]                           # one catch-light
    return img


_PAINTERS = {"steam_wisp": _steam, "frost_mote": _frost, "spark": _spark, "rock_chip": _rock}


def paint():
    for name, frames in SETS.items():
        for frame in range(frames):
            sprite = "%s_%d" % (name, frame)
            K.save_asset(_PAINTERS[name](frame, K.rng_for(sprite)), "textures/particle/%s.png" % sprite)
        K.save_json({"textures": ["nerotech:%s_%d" % (name, f) for f in range(frames)]},
                    "particles/%s.json" % name)


PULSE = []
