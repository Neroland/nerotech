#!/usr/bin/env python3
"""Generate the still/flow textures for NeroTech's gases and placeable liquids.

NeroTech's gases (`nerotech:hydrogen`, the shared `nerospace:oxygen` as fluid `nerotech:oxygen`, and
the 0.8.0 process gases) are registered as fluids so that any mod's fluid pipe can carry them (see
wiki/Fluids-and-Gases.md). They are never placed in the world, so their textures only ever appear in
tank and pipe GUIs and in the Process Tank: a softly mottled tint per gas that drifts upward.

The three 0.8.0 liquids (`cryo_coolant`, `sulfuric_acid`, `polymer_resin`) ARE placed in the world
(source + flowing + bucket). Each has its own surface and its own pace:

* cryo_coolant   -- azure with frost flecks, fast thin flow;
* sulfuric_acid  -- yellow-green, slow oily flow, rising bubbles;
* polymer_resin  -- cream with darker swirls, the slowest flow (viscous).

Still textures are 16x16 frames. Liquid flow textures are 32x32 frames, the vanilla convention: the
liquid renderer samples half of a flow sprite, so a 32 px frame puts 16 texels on a block face.
Every texture tiles in both directions and every animation loops without a seam.

* Deterministic: every pattern seeds from the texture name, so re-runs are byte-stable.
* ADDITIVE-ONLY: skips any PNG that already exists; pass --force to replace them, or
  --only=name,name to limit a run to some media (``--only=cryo_coolant --force``).
* No Pillow needed -- writes the PNGs directly (zlib + struct), so `gradlew genAssets` stays
  green everywhere.
* Colours are the art-direction table (mirrored in tools/art/common.py ``MEDIA``). Every gauge prints
  the medium's name: colour is never the only cue.

Outputs into common/src/main/resources/assets/nerotech/textures/block -- the vanilla block atlas
already stitches every namespace's `textures/block/`, so no atlas source file is needed (an
`assets/nerotech/atlases/blocks.json` is never read: atlas definitions live under the atlas's own
`minecraft` namespace).
"""
import hashlib
import math
import os
import random
import struct
import sys
import zlib

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from nerotech_target import src_base, target_label  # noqa: E402

SIZE = 16
FRAMES = 4  # legacy gases: a 16x64 strip; the .mcmeta animates it slowly so a piped gas reads as "moving"

# The two original gases keep their original painter and stay byte-identical.
LEGACY_GASES = {
    "hydrogen": (176, 206, 235),
    "oxygen": (150, 226, 214),
}
ALPHA = 200

# 0.8.0 process gases: (rgb, mottle strength). Nitrogen is the common one and the dullest;
# carbon dioxide is the heavy one and mottles hardest.
GASES = {
    "deuterium": ((112, 146, 240), 1.0),
    "helium_3": ((244, 188, 200), 1.0),
    "methane": ((176, 224, 140), 1.0),
    "nitrogen": ((150, 162, 190), 0.6),
    "carbon_dioxide": ((168, 156, 146), 1.7),
}

# 0.8.0 placeable liquids (source + flowing, real world blocks): opaque so they read as a liquid on
# every loader without a translucent render layer.
LIQUIDS = {
    "cryo_coolant": (70, 170, 240),
    "sulfuric_acid": (188, 220, 60),
    "polymer_resin": (228, 200, 156),
}
LIQUID_ALPHA = 255
FROST = ((214, 240, 250), (176, 220, 240))

TAU = 2.0 * math.pi


# ---- helpers --------------------------------------------------------------------------------------
def _seed(name):
    return int(hashlib.md5(name.encode()).hexdigest(), 16) & 0xffffffff


def _clamp(v):
    return max(0, min(255, int(round(v))))


def _mix(a, b, t):
    return tuple(_clamp(a[i] + (b[i] - a[i]) * t) for i in range(3))


def _shades(base, spread=1.0):
    """Five steps around ``base``, dark to light. The dark end leans cool, the light end to white."""
    deep = tuple(c * 0.70 for c in base)
    return [_mix(base, deep, 0.75 * spread), _mix(base, deep, 0.38 * spread), tuple(base),
            _mix(base, (255, 255, 255), 0.16 * spread), _mix(base, (255, 255, 255), 0.34 * spread)]


class _Field:
    """A smooth scalar field that tiles in x, y and time: a few sines with whole-number periods."""

    def __init__(self, name, waves=5, fx=(1, 2), fy=(1, 2), ft=(0, 1)):
        rng = random.Random(_seed(name))
        self.terms = []
        for _ in range(waves):
            kx = rng.randint(fx[0], fx[1]) * rng.choice((-1, 1))
            ky = rng.randint(fy[0], fy[1]) * rng.choice((-1, 1))
            kt = rng.randint(ft[0], ft[1]) * rng.choice((-1, 1))
            self.terms.append((kx, ky, kt, rng.random() * TAU, 0.6 + 0.4 * rng.random()))
        self.norm = sum(t[4] for t in self.terms)

    def at(self, u, v, w):
        """u, v, w in 0..1 (fractions of the tile width, height and loop). Returns -1..1."""
        total = 0.0
        for kx, ky, kt, phase, amp in self.terms:
            total += amp * math.sin(TAU * (kx * u + ky * v + kt * w) + phase)
        return total / self.norm


def _level(value, steps, gain=1.6):
    """Quantise a -1..1 field value to an index 0..steps-1 (pixel-art banding, no gradients)."""
    v = max(-1.0, min(1.0, value * gain))
    return max(0, min(steps - 1, int((v + 1.0) * 0.5 * steps)))


def _grain(name, x, y, size):
    """A stable -1/0/+1 speck per texel of the scrolling pattern (keeps flat bands from looking flat)."""
    h = hashlib.md5(("%s:%d:%d" % (name, x % size, y % size)).encode()).digest()[0]
    return -1 if h < 40 else (1 if h > 215 else 0)


def _rows(pixels, alpha):
    return [b"".join(bytes(p) + bytes((alpha,)) for p in row) for row in pixels]


# ---- legacy gases (hydrogen, oxygen): unchanged ----------------------------------------------------
def _mottle(base, seed, alpha=None):
    """One frame of softly mottled pixels around `base`."""
    rng = random.Random(seed)
    rows = []
    for _ in range(SIZE):
        row = bytearray()
        for _ in range(SIZE):
            jitter = rng.randint(-12, 12)
            row += bytes(max(0, min(255, channel + jitter)) for channel in base)
            row.append(ALPHA if alpha is None else alpha)
        rows.append(bytes(row))
    return rows


# ---- 0.8.0 gases -----------------------------------------------------------------------------------
def _gas(name, base, strength, frames, step):
    """Mottled cloud drifting UP ``step`` px a frame while it slowly changes shape."""
    shades = _shades(base, 0.55 * strength)
    field = _Field(name, waves=5, fx=(1, 2), fy=(1, 2), ft=(0, 0))
    morph = _Field(name + ":morph", waves=3, fx=(1, 1), fy=(1, 1), ft=(1, 1))
    out = []
    for f in range(frames):
        w = f / float(frames)
        frame = []
        for y in range(SIZE):
            sy = (y + f * step) % SIZE            # sample lower rows later: the cloud rises
            row = []
            for x in range(SIZE):
                v = field.at(x / 16.0, sy / 16.0, 0.0) + 0.45 * morph.at(x / 16.0, sy / 16.0, w)
                idx = _level(v, 5, 1.5)
                g = _grain(name, x, sy, SIZE)
                idx = max(0, min(4, idx + (g if strength > 1.2 else 0)))
                col = shades[idx]
                if g and strength <= 1.2:
                    col = tuple(_clamp(c + 3 * g) for c in col)
                row.append(col)
            frame.append(row)
        out.extend(_rows(frame, ALPHA))
    return out


# ---- 0.8.0 liquids ---------------------------------------------------------------------------------
def _bubble(frame, x, y, size, ring, core):
    """A 2x2 bubble: bright top-left, pale body."""
    for dx, dy, col in ((0, 0, core), (1, 0, ring), (0, 1, ring), (1, 1, ring)):
        frame[(y + dy) % size][(x + dx) % size] = col


def _cryo(name, base, size, frames, step):
    """Azure, thin fast streaks, frost flecks. ``step`` px of downward scroll a frame (0 = still)."""
    shades = _shades(base, 0.8)
    streaks = _Field(name, waves=5, fx=(2, 4), fy=(1, 1), ft=(0, 0))
    ripple = _Field(name + ":r", waves=3, fx=(1, 2), fy=(1, 2), ft=(1, 1))
    rng = random.Random(_seed(name + ":flecks"))
    flecks = [(rng.randrange(size), rng.randrange(size), rng.randrange(frames))
              for _ in range(size * size // 36)]
    out = []
    for f in range(frames):
        w = f / float(frames)
        frame = []
        for y in range(size):
            sy = (y - f * step) % size
            row = []
            for x in range(size):
                v = 0.75 * streaks.at(x / float(size), sy / float(size), 0.0) \
                    + 0.5 * ripple.at(x / float(size), sy / float(size), w)
                row.append(shades[_level(v, 5, 1.5)])
            frame.append(row)
        for fx, fy, phase in flecks:            # flecks ride the flow and twinkle
            age = (f - phase) % frames
            y = (fy + f * step) % size
            if age < frames * 0.6:
                frame[y][fx] = FROST[0]
                if age < frames * 0.3:
                    frame[y][(fx + 1) % size] = FROST[1]
            else:
                frame[y][fx] = FROST[1]
        out.extend(_rows(frame, LIQUID_ALPHA))
    return out


def _acid(name, base, size, frames, step):
    """Yellow-green, broad oily bands, bubbles rising against the flow."""
    shades = _shades(base, 0.9)
    oil = _Field(name, waves=4, fx=(1, 2), fy=(1, 1), ft=(0, 0))
    slick = _Field(name + ":s", waves=3, fx=(1, 1), fy=(1, 2), ft=(1, 1))
    rng = random.Random(_seed(name + ":bubbles"))
    bubbles = [(rng.randrange(size), rng.randrange(size)) for _ in range(max(3, size * size // 80))]
    ring = _mix(base, (255, 255, 255), 0.45)
    core = _mix(base, (255, 255, 255), 0.8)
    out = []
    for f in range(frames):
        w = f / float(frames)
        frame = []
        for y in range(size):
            sy = (y - f * step) % size
            row = []
            for x in range(size):
                v = 0.8 * oil.at(x / float(size), sy / float(size), 0.0) \
                    + 0.5 * slick.at(x / float(size), sy / float(size), w)
                row.append(shades[_level(v, 5, 1.35)])
            frame.append(row)
        rise = int(round(f * size / float(frames)))      # one full lap up per loop
        for bx, by in bubbles:
            _bubble(frame, bx, by - rise, size, ring, core)
        out.extend(_rows(frame, LIQUID_ALPHA))
    return out


def _resin(name, base, size, frames, step):
    """Cream with darker swirl lines that fold slowly; thick, so the bands are wide and soft."""
    cream = _shades(base, 0.5)
    swirl_dark = _mix(base, (150, 112, 70), 0.55)
    swirl_mid = _mix(base, (150, 112, 70), 0.28)
    body = _Field(name, waves=3, fx=(1, 1), fy=(1, 1), ft=(0, 0))
    bend = _Field(name + ":b", waves=2, fx=(1, 1), fy=(1, 2), ft=(1, 1))
    out = []
    for f in range(frames):
        w = f / float(frames)
        frame = []
        for y in range(size):
            sy = (y - f * step) % size
            row = []
            for x in range(size):
                u, v = x / float(size), sy / float(size)
                fold = 0.16 * bend.at(u, v, w) + 0.10 * math.sin(TAU * (v + w))
                line = math.sin(TAU * (2 * u + v + fold * 3.0))
                if line > 0.86:
                    col = swirl_dark
                elif line > 0.62:
                    col = swirl_mid
                else:
                    col = cream[1 + _level(body.at(u, v, 0.0), 3, 1.4)]
                row.append(col)
            frame.append(row)
        out.extend(_rows(frame, LIQUID_ALPHA))
    return out


# (painter, still frames, still frametime, flow frames, flow px/frame, flow frametime, interpolate)
LIQUID_STYLE = {
    "cryo_coolant": (_cryo, 16, 2, 8, 4, 2, False),
    "sulfuric_acid": (_acid, 32, 4, 32, 1, 3, True),
    "polymer_resin": (_resin, 32, 6, 32, 1, 5, True),
}


# ---- output ----------------------------------------------------------------------------------------
def _png(path, rows, width=SIZE):
    raw = b"".join(b"\x00" + row for row in rows)
    height = len(rows)

    def chunk(tag, payload):
        body = tag + payload
        return struct.pack(">I", len(payload)) + body + struct.pack(">I", zlib.crc32(body))

    data = (b"\x89PNG\r\n\x1a\n"
            + chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0))
            + chunk(b"IDAT", zlib.compress(raw, 9))
            + chunk(b"IEND", b""))
    with open(path, "wb") as handle:
        handle.write(data)


def _meta(path, frametime, interpolate=False):
    with open(path, "w", encoding="utf-8", newline="\n") as handle:
        if interpolate:
            handle.write('{\n  "animation": {\n    "frametime": %d,\n    "interpolate": true\n  }\n}\n'
                         % frametime)
        else:
            handle.write('{\n  "animation": {\n    "frametime": %d\n  }\n}\n' % frametime)


def jobs():
    """Every texture as ``(name, width, frametime, interpolate, make_rows)``."""
    out = []
    for gas, colour in LEGACY_GASES.items():
        for kind in ("still", "flow"):
            name = "%s_%s" % (gas, kind)

            def legacy(name=name, colour=colour):
                rows = []
                for frame in range(FRAMES):
                    rows.extend(_mottle(colour, "%s:%d" % (name, frame)))
                return rows
            out.append((name, SIZE, 6 if kind == "flow" else 10, False, legacy))
    for gas, (colour, strength) in GASES.items():
        out.append((gas + "_still", SIZE, 6, False,
                    lambda n=gas + "_still", c=colour, s=strength: _gas(n, c, s, 16, 1)))
        out.append((gas + "_flow", SIZE, 3, False,
                    lambda n=gas + "_flow", c=colour, s=strength: _gas(n, c, s, 8, 2)))
    for liquid, colour in LIQUIDS.items():
        paint, s_frames, s_time, f_frames, f_step, f_time, lerp = LIQUID_STYLE[liquid]
        out.append((liquid + "_still", SIZE, s_time, lerp,
                    lambda p=paint, n=liquid + "_still", c=colour, fr=s_frames: p(n, c, SIZE, fr, 0)))
        out.append((liquid + "_flow", 2 * SIZE, f_time, lerp,
                    lambda p=paint, n=liquid + "_flow", c=colour, fr=f_frames, st=f_step:
                    p(n, c, 2 * SIZE, fr, st)))
    return out


def main():
    force = "--force" in sys.argv
    only = None
    for arg in sys.argv[1:]:
        if arg.startswith("--only="):
            only = set(arg[len("--only="):].split(","))
    out_dir = os.path.join(src_base(), "src/main/resources/assets/nerotech/textures/block")
    os.makedirs(out_dir, exist_ok=True)
    print("gen_fluid_textures: target = %s" % target_label())

    for name, width, frametime, interpolate, make in jobs():
        medium = name.rsplit("_", 1)[0]
        if only is not None and medium not in only:
            continue
        png = os.path.join(out_dir, name + ".png")
        if os.path.exists(png) and not force:
            print("  skip %s (exists)" % name)
            continue
        rows = make()
        _png(png, rows, width)
        _meta(png + ".mcmeta", frametime, interpolate)
        print("  wrote %s (%dx%d, %d frames)" % (name, width, len(rows), len(rows) // width))


if __name__ == "__main__":
    main()
