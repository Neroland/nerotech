"""Shared art kit for the NeroTech 0.8.0 painter packages (``tools/art/<pkg>.py``).

Everything a package needs comes from here, so a package never edits ``gen_textures.py``:

* the base palette and painters, re-exported from ``gen_textures`` (same objects, same ledger);
* the 0.8.0 palette extension (family trim and material ramps, all NON-emissive and outside the
  PULSE teal hue window; teal/plasma stays the only emissive ramp);
* ``status_socket()``  -- the dark socket every machine front carries for the status lamp;
* ``family_trim()``    -- the 2 px family band under the top bevel;
* ``glass_pane()``     -- translucent glass with glints;
* ``machine_face()``   -- base + trim + socket in one call;
* ``save_block()``     -- ``save()`` plus the "no heat colour in block base art" gate;
* ``paint_kit()``      -- the sprites the shared renderer helpers draw (lamp, shimmer, fluid, gas).

Usage in a package module::

    from art import common as K

    def paint():
        img = K.machine_face("plate_press_front", "metallurgy", front=True)
        px = img.load()
        K.led(px, 6, 20)
        K.save_block(img, "plate_press_front")

    PULSE = [K.pulse("plate_press_front")]

Status lamp socket. It sits top right AS YOU FACE the machine: face pixels x 26..29, y 4..7 of the
32x front texture, which is face coordinates [13,12]-[15,14] counted from the face's own bottom
left. A model authored facing north sees east on the viewer's left, so in MODEL space that is
x 1..3 (not 13..15), y 12..14. Take the element from ``status_socket_element()`` instead of typing
it; the renderer (``StatusLampRenderer``) uses the same numbers.
"""
import json
import os
import sys

_TOOLS = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
if _TOOLS not in sys.path:
    sys.path.insert(0, _TOOLS)

import gen_textures as G  # noqa: E402  (aliased to __main__ by gen_textures.main, so one ledger)
from gen_textures import (  # noqa: E402,F401  (re-exported for the packages)
    S, CLEAR, A_DARK, ALLOY, A_LIGHT, T_DEEP, T_TEAL, T_CYAN, T_PLASMA, T_GLOW, TEAL_RAMP,
    HAZ_Y, HAZ_K, GRAY_D, GRAY, GRAY_L, IRON, COPPER, GOLD,
    rng_for, new_img, noise_fill, bevel, rivets, machine_base, recess, led, haz_band, save)

mix = G._mix

# ---- Palette extension (decision: art direction, section 5; table mirrored in the model docs) ----
CINDER = [(74, 62, 60, 255), (98, 80, 74, 255), (52, 44, 44, 255), (128, 104, 94, 255)]
BRICK = [(92, 58, 48, 255), (110, 70, 56, 255), (74, 48, 42, 255)]
MORTAR = (40, 34, 34, 255)
MORTAR_LIT = (168, 96, 52, 255)        # the ONE warm static colour: foundry_brick lit=true only
ENAMEL = [(196, 214, 220, 255), (164, 184, 192, 255)]
GLASS = (120, 168, 180, 70)
GLASS_HI = (220, 244, 248, 150)
LAG = [(70, 74, 78, 255), (88, 92, 96, 255)]
EU_ROD = [(214, 220, 208, 255), (176, 184, 170, 255)]
FROST = [(214, 240, 250, 255), (176, 220, 240, 255)]
TITANIUM_PLATE = (190, 200, 214, 255)

# Renderer-only colours. Never paint these into a block base texture (save_block() refuses).
H_WARN = G.H_WARN
H_CRIT = G.H_CRIT
H_WHITE = (255, 236, 190, 255)         # pour peak
HEAT_ONLY = (H_WARN, H_CRIT, H_WHITE)

# Fluid and gas colours (RGB). Gases: alpha 200, mottled, upward drift. Liquids: opaque-ish, flowing.
MEDIA = {
    "hydrogen": (176, 206, 235),
    "oxygen": (150, 226, 214),
    "deuterium": (112, 146, 240),
    "helium_3": (244, 188, 200),
    "methane": (176, 224, 140),
    "nitrogen": (150, 162, 190),
    "carbon_dioxide": (168, 156, 146),
    "cryo_coolant": (70, 170, 240),
    "sulfuric_acid": (188, 220, 60),
    "polymer_resin": (228, 200, 156),
}
GASES = ("hydrogen", "oxygen", "deuterium", "helium_3", "methane", "nitrogen", "carbon_dioxide")
GAS_ALPHA = 200

# Focus lens glass (RGB); the same four values colour the Laser Etcher and Cutting Lance beams.
LENS = {
    "quartz_focus_lens": (232, 240, 244),
    "glacite_focus_lens": (120, 190, 255),
    "ember_focus_lens": (236, 124, 48),
    "void_focus_lens": (58, 44, 110),
}

# Family trim: (top row ramp, bottom row colour). Fusion uses the casing frame + haz_band(), and
# utility is plain alloy + teal, so neither has a trim band.
FAMILIES = {
    "metallurgy": (CINDER[:2], CINDER[2]),
    "foundry": (BRICK[:2], BRICK[2]),
    "chemistry": (ENAMEL, mix(ENAMEL[1], A_DARK, 0.35)),
    "components": (GOLD[:1] + GOLD[2:], GOLD[1]),
    "heat": (COPPER[:1] + COPPER[2:], COPPER[1]),
    "extraction": (IRON[:1] + IRON[2:], IRON[1]),
    "fusion": None,
    "utility": None,
}

# PULSE hue window (same numbers as gen_animations.TEAL) and the row helper.
TEAL = (0.42, 0.62)


def pulse(name, amp=0.28, frames=8, frametime=3):
    """One gen_animations PULSE row for a texture in textures/block."""
    return (name, frames, frametime, amp, TEAL)


# ---- status lamp socket -------------------------------------------------------------------------
SOCKET_X, SOCKET_Y, SOCKET_PX = 26, 4, 4          # 32x face pixels, top-left origin
SOCKET_FROM = (1.0, 12.0, -0.5)                   # model space, block authored facing north
SOCKET_TO = (3.0, 14.0, 0.0)


def status_socket(px, x=SOCKET_X, y=SOCKET_Y):
    """Paint the status lamp socket on a FRONT texture: a 4x4 px T_DEEP well with a shadow rim.

    Call it after ``family_trim()`` and before any LED work. The renderer draws the lit lamp into
    this well, so never put an emissive pixel here (it would glow while the lamp is dark).
    """
    for yy in range(y - 1, y + SOCKET_PX + 1):
        for xx in range(x - 1, x + SOCKET_PX + 1):
            if 2 <= xx <= S - 3 and 2 <= yy <= S - 3:
                px[xx, yy] = A_DARK
    for yy in range(y, y + SOCKET_PX):
        for xx in range(x, x + SOCKET_PX):
            px[xx, yy] = T_DEEP


def status_socket_element(texture="#front", z_front=0.0):
    """The model element for the socket (a 2x2x0.5 px collar proud of the front face).

    ``z_front`` is the model z of the face the collar sits on (0 for a full-depth front); pass the
    same value to ``StatusLampRenderer.submit`` when it is not 0.
    """
    uv = [13, 2, 15, 4]
    face = {"uv": uv, "texture": texture}
    return {
        "name": "status_socket",
        "from": [SOCKET_FROM[0], SOCKET_FROM[1], z_front - 0.5],
        "to": [SOCKET_TO[0], SOCKET_TO[1], z_front],
        "faces": {"north": face, "east": face, "west": face, "up": face, "down": face},
    }


# ---- family trim --------------------------------------------------------------------------------
def family_trim(px, family, y=2, x0=2, x1=S - 3):
    """The 2 px non-emissive family band directly under the top bevel (rows ``y`` and ``y + 1``).

    Returns False (and paints nothing) for the families without a trim colour. Family fittings
    beyond the band (brick courses, lagging, enamel panels) are the package's own work.
    """
    spec = FAMILIES[family]
    if spec is None:
        return False
    top, low = spec
    for x in range(x0, x1 + 1):
        px[x, y] = top[(x // 2) % len(top)]
        px[x, y + 1] = low
    if family == "heat":                         # lagging bands over the copper
        for x in range(x0 + 3, x1 - 1, 8):
            for i in range(3):
                px[x + i, y] = LAG[1]
                px[x + i, y + 1] = LAG[0]
    elif family == "foundry":                    # mortar joints
        for x in range(x0 + 5, x1 + 1, 6):
            px[x, y] = MORTAR
            px[x, y + 1] = MORTAR
    return True


# ---- glass --------------------------------------------------------------------------------------
def glass_pane(px, x0, y0, x1, y1, frame=None, glints=2):
    """Fill a rectangle with translucent GLASS and diagonal GLASS_HI glints.

    The texture then has partial alpha: the block needs the translucent render layer (integration)
    and a renderer drawing behind it must not rely on these pixels being opaque. ``frame`` paints a
    1 px opaque border in that colour.
    """
    w = x1 - x0 + 1
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            px[x, y] = GLASS
    step = max(5, w // max(1, glints))
    for g in range(glints):
        start = x0 + 2 + g * step
        length = max(3, min(w, y1 - y0 + 1) // 2 - g * 2)
        for i in range(length):
            x, y = start + i, y0 + 1 + length - i + g * 2
            if x0 < x < x1 and y0 < y < y1:
                px[x, y] = GLASS_HI
                if x + 1 < x1 and g == 0:
                    px[x + 1, y] = GLASS_HI
    if frame is not None:
        for x in range(x0, x1 + 1):
            px[x, y0] = frame
            px[x, y1] = frame
        for y in range(y0, y1 + 1):
            px[x0, y] = frame
            px[x1, y] = frame


# ---- composite + gates --------------------------------------------------------------------------
def machine_face(name, family=None, front=False):
    """``machine_base(name)`` + the family band + (fronts only) the status lamp socket."""
    img = machine_base(name)
    px = img.load()
    if family is not None:
        family_trim(px, family)
    if front:
        status_socket(px)
    return img


def heat_pixels(img):
    """Coordinates of any renderer-only heat colour in ``img`` (the package gate: must be empty)."""
    px = img.load()
    w, h = img.size
    return [(x, y) for y in range(h) for x in range(w) if px[x, y] in HEAT_ONLY]


def save_block(img, name):
    """``save(img, "block", name)`` that refuses a base texture carrying a heat colour."""
    bad = heat_pixels(img)
    if bad:
        raise ValueError("%s: renderer-only heat colour at %s (H_* never appears in block base art)"
                         % (name, bad[:4]))
    save(img, "block", name)


def asset_path(rel):
    """Absolute path under assets/nerotech/ in the chosen target (``textures/particle/x.png``)."""
    return os.path.join(G.ROOT, "src/main/resources/assets/nerotech", rel)


def save_asset(img, rel):
    """Additive-only PNG save anywhere under assets/nerotech/ (``--force`` replaces)."""
    path = asset_path(rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    if os.path.exists(path) and not G.FORCE:
        print("skip (exists)", os.path.relpath(path, G.ROOT))
        return
    img.save(path)
    print("wrote", os.path.relpath(path, G.ROOT))


def save_json(obj, rel):
    """Additive-only JSON save under assets/nerotech/ (2-space indent, LF, trailing newline)."""
    path = asset_path(rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    if os.path.exists(path) and not G.FORCE:
        print("skip (exists)", os.path.relpath(path, G.ROOT))
        return
    with open(path, "w", encoding="utf-8", newline="\n") as fh:
        json.dump(obj, fh, indent=2)
        fh.write("\n")
    print("wrote", os.path.relpath(path, G.ROOT))


# ---- shared renderer sprites --------------------------------------------------------------------
WHITE = (255, 255, 255, 255)


def paint_kit():
    """Sprites drawn by MachineRenderHelper / StatusLampRenderer. All white or grey: the renderer
    tints them, so no hue is baked in (and no heat colour ever reaches a texture)."""
    from PIL import Image

    # status_lamp: the lit lamp. status_lamp_half: the static STALL frame (animations off).
    for name, half in (("status_lamp", False), ("status_lamp_half", True)):
        img = Image.new("RGBA", (8, 8), CLEAR)
        px = img.load()
        for y in range(8):
            for x in range(8):
                rim = x in (0, 7) or y in (0, 7)
                col = (206, 206, 206, 255) if rim else WHITE
                if half and x + (7 - y) > 7:      # upper-right half unlit
                    col = (44, 50, 54, 255) if not rim else (30, 34, 38, 255)
                px[x, y] = col
        save(img, "block", name)

    # heat_shimmer: dithered rising waves, tileable both ways (the renderer scrolls V).
    import math
    img = new_img()
    px = img.load()
    for y in range(S):
        for x in range(S):
            wave = math.sin(2 * math.pi * (y / 8.0 + 0.5 * math.sin(2 * math.pi * x / S)))
            if wave > 0.55 and (x + y) % 2 == 0:
                px[x, y] = WHITE
            elif wave > 0.9:
                px[x, y] = (236, 236, 236, 255)
    save(img, "block", "heat_shimmer")

    # fluid_column: opaque pale body with soft vertical streaks, tileable; tinted per medium.
    img = new_img()
    px = img.load()
    rng = rng_for("fluid_column")
    streak = [rng.choice((232, 240, 248, 255)) for _ in range(S)]
    for y in range(S):
        for x in range(S):
            v = streak[x]
            if rng.random() < 0.06:
                v = max(214, v - 22)
            px[x, y] = (v, v, v, 255)
    save(img, "block", "fluid_column")

    # gas_fill_0..2: mottled dither at rising density (cutout has no blending, so density is the
    # dither coverage). Tileable; the renderer drifts V upward.
    for level, cover in enumerate((0.22, 0.45, 0.70)):
        name = "gas_fill_%d" % level
        img = new_img()
        px = img.load()
        rng = rng_for(name)
        for y in range(S):
            for x in range(S):
                blob = 0.5 + 0.5 * math.sin(2 * math.pi * (x / 16.0)) * math.sin(2 * math.pi * (y / 16.0))
                if rng.random() < cover * (0.6 + 0.8 * blob):
                    v = rng.choice((226, 240, 255))
                    px[x, y] = (v, v, v, 255)
        save(img, "block", name)
