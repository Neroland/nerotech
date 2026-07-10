#!/usr/bin/env python3
"""In-game animated textures for NeroTech: PULSE breathing on the machine fronts + the fusion
plasma sprite (port of Nerospace's tools/gen_animations.py, PULSE kind only, at 32x).

Output goes into textures/block and is picked up by the atlas via a sibling .png.mcmeta (no
model or Java change). PULSE turns a static 32x32 texture into a vertical frame strip whose
accent pixels breathe in brightness (HSV value, keeps hue -> no white-out). The accent mask is
restricted to the teal hue window, so ONLY the T_* emissives painted by gen_textures.py pulse —
alloy, greys and the fusion hazard stripes stay static.

Frame 0 of every strip is the untouched original, so the pass is IDEMPOTENT: re-running crops
frame 0 back out and rebuilds the strip (resting look unchanged). Frametime 3 per MODELS.md.

Usage: python tools/gen_animations.py --multiloader   (or via `gradlew genAnim`)
Deps: Pillow (exits 0 with a notice without it, mirroring gen_textures.py).
"""
import colorsys
import json
import math
import os
import sys

try:
    from PIL import Image
except ModuleNotFoundError:
    print("gen_animations: Pillow not installed; skipping animation generation (pip install pillow).")
    sys.exit(0)

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from nerotech_target import resolve  # noqa: E402


def BLOCK():
    return resolve("common/src/main/resources/assets/nerotech/textures/block")


# Teal hue window (T_TEAL/T_CYAN/T_PLASMA sit at hue ~0.51): keeps the pulse off anything that
# is not a painted T_* emissive (e.g. the fusion shell's hazard yellow, hue ~0.13).
TEAL = (0.42, 0.62)

# name, frames, frametime(ticks), amplitude, hue-window — frametime 3 across the set (MODELS.md)
PULSE = [
    ("nero_generator_front",          8, 3, 0.30, TEAL),
    ("solar_array_front",             8, 3, 0.26, TEAL),
    ("ore_processor_front",           8, 3, 0.28, TEAL),
    ("advanced_ore_processor_front",  8, 3, 0.30, TEAL),
    ("fabricator_front",              8, 3, 0.28, TEAL),
    ("advanced_fabricator_front",     8, 3, 0.30, TEAL),
    ("fusion_reactor_front",          8, 3, 0.32, TEAL),
    ("auto_crafter_front",            8, 3, 0.28, TEAL),
    ("item_sorter_front",             8, 3, 0.26, TEAL),
    ("scrubber_front",                8, 3, 0.26, TEAL),
    ("remediator_front",              8, 3, 0.28, TEAL),
    ("fusion_reactor_plasma",         8, 3, 0.35, TEAL),  # BER wisp: glow breathing
]


def write_mcmeta(path, frametime, interpolate=False):
    anim = {"frametime": frametime}
    if interpolate:
        anim["interpolate"] = True
    with open(path + ".mcmeta", "w", encoding="utf-8") as fh:
        json.dump({"animation": anim}, fh, indent=2)
        fh.write("\n")


def accent_mask(img, hue):
    """Saturated + bright pixels (vs the texture's median value), optionally hue-windowed."""
    px = img.load()
    w, h = img.size
    vals = [max(px[x, y][:3]) / 255 for y in range(h) for x in range(w) if px[x, y][3]]
    med = sorted(vals)[len(vals) // 2] if vals else .5
    m = []
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if not a:
                continue
            hh, ss, vv = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
            if ss > 0.32 and vv > max(0.42, med * 1.05) and (hue is None or hue[0] <= hh <= hue[1]):
                m.append((x, y))
    return m


def litpx(px, xy, f):
    r, g, b, a = px[xy]
    hh, ss, vv = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
    vv = min(1.0, vv * f)
    ss = min(1.0, ss * (1.0 + 0.12 * (f - 1)))
    r, g, b = (int(c * 255) for c in colorsys.hsv_to_rgb(hh, ss, vv))
    px[xy] = (r, g, b, a)


def pulse_strip(base, frames, amp, hue):
    w = base.width
    mask = accent_mask(base, hue)
    strip = Image.new("RGBA", (w, w * frames))
    for i in range(frames):
        fr = base.copy()
        px = fr.load()
        fac = 1.0 + amp * math.sin(2 * math.pi * i / frames)
        if abs(fac - 1.0) > 1e-9:  # fac==1.0 frames (frame 0 + mid-cycle) stay EXACTLY the base:
            for xy in mask:        # even a no-op litpx would drift pixels via HSV int-truncation,
                litpx(px, xy, fac)  # which would break the crop-frame-0 idempotency guarantee.
        strip.paste(fr, (0, i * w))
    return strip


def main():
    B = BLOCK()
    made = []
    for name, frames, ft, amp, hue in PULSE:
        p = os.path.join(B, name + ".png")
        if not os.path.exists(p):
            print("  miss", name)
            continue
        base = Image.open(p).convert("RGBA")
        base = base.crop((0, 0, base.width, base.width))  # frame 0 of any previous strip
        pulse_strip(base, frames, amp, hue).save(p)
        write_mcmeta(p, ft)
        made.append("%s (pulse %df @%d)" % (name, frames, ft))
    print("animated %d textures:" % len(made))
    for m in made:
        print("  ", m)


if __name__ == "__main__":
    main()
