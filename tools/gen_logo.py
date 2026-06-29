#!/usr/bin/env python3
"""
Generate the NEROTECH mod logo (square), in the shared Neroland family style
(cf. neroland-core/tools/gen_logo.py and nerospace/tools/gen_logo.py): a deep-space
starfield, a glowing faceted central prism, and a beveled glowing wordmark.

NeroTech is the industry/automation/power mod, so the family faceted hexagonal prism
is set inside an industrial **gear** and lit by NeroTech's amber **Nero energy** —
keeping the family palette (teal / steel-blue / cyan) but leading with amber so it
reads as the "power" member of the set. Renders supersampled, then downsamples.

Outputs:
  art/logo/nerotech_logo.png       (1024x1024 master)
  art/logo/nerotech_logo_400.png   (CurseForge/Modrinth-ready)
  common/src/main/resources/nerotech_logo.png  (256x256 in-game mods-list icon)
"""
import math
import os
import random
import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "art/logo")
ICON = os.path.join(ROOT, "common/src/main/resources")
os.makedirs(OUT, exist_ok=True)
os.makedirs(ICON, exist_ok=True)

FINAL = 1024
SS = 2
R = FINAL * SS
rng = random.Random(11)

# Neroland family palette + NeroTech's amber energy accent
NERO_ALLOY = (38, 166, 154)    # teal
STARSTEEL = (140, 178, 208)    # steel-blue
PLASMA = (96, 212, 232)        # cyan
STEEL = (122, 132, 146)        # machine casing
STEEL_DK = (66, 74, 86)
AMBER = (232, 150, 44)         # Nero energy
AMBER_BRIGHT = (255, 206, 120)
BRIGHT = (255, 244, 224)


def _font(size):
    for path in (
        "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
        "/usr/share/fonts/truetype/liberation/LiberationSans-Bold.ttf",
    ):
        if os.path.exists(path):
            return ImageFont.truetype(path, size)
    return ImageFont.load_default()


def background():
    top = np.array([6, 11, 17], float)
    bot = np.array([13, 18, 28], float)
    yy = np.linspace(0, 1, R)[:, None, None]
    img = top[None, None, :] * (1 - yy) + bot[None, None, :] * yy
    img = np.repeat(img, R, axis=1)
    Y, X = np.mgrid[0:R, 0:R].astype(float)

    def glow(cx, cy, rad, color, strength):
        d = np.sqrt((X - cx) ** 2 + (Y - cy) ** 2)
        f = np.clip(1 - d / rad, 0, 1) ** 2 * strength
        for c in range(3):
            img[:, :, c] += color[c] * f

    glow(R * 0.28, R * 0.30, R * 0.55, (20, 80, 84), 0.42)    # teal nebula (family)
    glow(R * 0.76, R * 0.72, R * 0.55, (120, 64, 14), 0.46)   # amber nebula (NeroTech)
    glow(R * 0.5, R * 0.5, R * 0.42, (24, 44, 60), 0.28)

    d = np.sqrt((X - R / 2) ** 2 + (Y - R / 2) ** 2) / (R * 0.72)
    vig = np.clip(1 - (d ** 2) * 0.85, 0.25, 1)
    img *= vig[:, :, None]
    return Image.fromarray(np.clip(img, 0, 255).astype(np.uint8), "RGB").convert("RGBA")


def add_stars(base):
    layer = Image.new("RGBA", (R, R), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    for _ in range(460):
        x, y = rng.randint(0, R), rng.randint(0, R)
        s = rng.choice([1, 1, 1, 2, 2, 3]) * SS
        b = rng.randint(120, 255)
        tint = rng.choice([(b, b, b), (b, 255, 255), (255, 230, 190), (200, 200, 255)])
        d.ellipse([x, y, x + s, y + s], fill=tint + (rng.randint(120, 255),))
    base.alpha_composite(layer.filter(ImageFilter.GaussianBlur(2 * SS)))
    base.alpha_composite(layer)
    return base


def soft_glow(draw_fn, blur):
    layer = Image.new("RGBA", (R, R), (0, 0, 0, 0))
    draw_fn(ImageDraw.Draw(layer))
    return layer.filter(ImageFilter.GaussianBlur(blur))


def gear_points(cx, cy, ro, ri, teeth):
    pts = []
    for i in range(teeth):
        a = 2 * math.pi * i / teeth
        aw = 2 * math.pi / teeth
        for (frac, rad) in ((0.06, ro), (0.40, ro), (0.50, ri), (0.94, ri)):
            ang = a + aw * frac
            pts.append((cx + math.cos(ang) * rad, cy + math.sin(ang) * rad))
    return pts


def emblem(base, cx, cy, rad):
    # amber + teal aura
    base.alpha_composite(soft_glow(
        lambda dr: dr.ellipse([cx - rad * 1.8, cy - rad * 1.8, cx + rad * 1.8, cy + rad * 1.8],
                              fill=(150, 90, 20, 150)), 34 * SS))
    base.alpha_composite(soft_glow(
        lambda dr: dr.ellipse([cx - rad * 1.3, cy - rad * 1.3, cx + rad * 1.3, cy + rad * 1.3],
                              fill=(40, 150, 150, 120)), 18 * SS))

    # industrial gear behind the prism
    gl = Image.new("RGBA", (R, R), (0, 0, 0, 0))
    gd = ImageDraw.Draw(gl)
    gpts = gear_points(cx, cy, rad * 1.55, rad * 1.18, 12)
    gd.polygon(gpts, fill=STEEL + (255,))
    gd.polygon(gpts, outline=(210, 224, 236, 230), width=SS * 2)
    # gear inner ring + amber rim
    gd.ellipse([cx - rad * 1.06, cy - rad * 1.06, cx + rad * 1.06, cy + rad * 1.06], fill=STEEL_DK + (255,))
    gd.ellipse([cx - rad * 1.06, cy - rad * 1.06, cx + rad * 1.06, cy + rad * 1.06],
               outline=AMBER + (235,), width=SS * 2)
    base.alpha_composite(gl)

    # faceted hexagonal prism (family motif), lit amber
    hexpts = [(cx + math.cos(math.radians(60 * i - 90)) * rad,
               cy + math.sin(math.radians(60 * i - 90)) * rad) for i in range(6)]
    layer = Image.new("RGBA", (R, R), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    facet_cols = [STARSTEEL, NERO_ALLOY, AMBER, STARSTEEL, NERO_ALLOY, AMBER]
    for i in range(6):
        shade = 0.58 + 0.42 * (i / 5.0)
        col = tuple(int(c * shade) for c in facet_cols[i])
        d.polygon([(cx, cy), hexpts[i], hexpts[(i + 1) % 6]], fill=col + (255,))
    # bright amber energy core
    ir = rad * 0.36
    d.ellipse([cx - ir, cy - ir, cx + ir, cy + ir], fill=AMBER_BRIGHT + (255,))
    d.ellipse([cx - ir * 0.5, cy - ir * 0.5, cx + ir * 0.5, cy + ir * 0.5], fill=BRIGHT + (255,))
    for i in range(6):
        d.line([hexpts[i], hexpts[(i + 1) % 6]], fill=(230, 240, 250, 235), width=max(1, SS * 2))
        d.line([(cx, cy), hexpts[i]], fill=(220, 230, 240, 150), width=max(1, SS))
    base.alpha_composite(layer)

    # specular sparkle
    sx, sy = cx - rad * 0.16, cy - rad * 0.46
    base.alpha_composite(soft_glow(
        lambda dr: dr.ellipse([sx - 9 * SS, sy - 9 * SS, sx + 9 * SS, sy + 9 * SS],
                              fill=(255, 255, 255, 255)), 5 * SS))
    dd = ImageDraw.Draw(base)
    L = 18 * SS
    dd.line([sx - L, sy, sx + L, sy], fill=(255, 255, 255, 230), width=SS * 2)
    dd.line([sx, sy - L, sx, sy + L], fill=(255, 255, 255, 230), width=SS * 2)
    return base


def wordmark(base):
    big = _font(int(R * 0.140))
    tagf = _font(int(R * 0.030))

    def centered(text, font, y, fill, glow=None):
        w = ImageDraw.Draw(base).textlength(text, font=font)
        x = (R - w) / 2
        if glow:
            gl = Image.new("RGBA", (R, R), (0, 0, 0, 0))
            ImageDraw.Draw(gl).text((x, y), text, font=font, fill=glow)
            base.alpha_composite(gl.filter(ImageFilter.GaussianBlur(9 * SS)))
            base.alpha_composite(gl.filter(ImageFilter.GaussianBlur(3 * SS)))
        out = Image.new("RGBA", (R, R), (0, 0, 0, 0))
        ImageDraw.Draw(out).text((x, y), text, font=font, fill=(10, 12, 16, 255))
        base.alpha_composite(out.filter(ImageFilter.MaxFilter(2 * SS + 1)))
        ImageDraw.Draw(base).text((x, y), text, font=font, fill=fill)

    centered("NEROTECH", big, int(R * 0.70), (244, 250, 252, 255), glow=(230, 150, 44, 255))

    tag = "I N D U S T R Y   ·   A U T O M A T I O N   ·   P O W E R"
    tw = ImageDraw.Draw(base).textlength(tag, font=tagf)
    ImageDraw.Draw(base).text(((R - tw) / 2, int(R * 0.862)), tag, font=tagf, fill=(232, 196, 150, 255))
    return base


def main():
    img = background()
    img = add_stars(img)
    cx, cy, rad = int(R * 0.5), int(R * 0.355), int(R * 0.125)
    img = emblem(img, cx, cy, rad)
    img = wordmark(img)

    final = img.convert("RGB").resize((FINAL, FINAL), Image.LANCZOS)
    p1 = os.path.join(OUT, "nerotech_logo.png")
    p2 = os.path.join(OUT, "nerotech_logo_400.png")
    p3 = os.path.join(ICON, "nerotech_logo.png")
    final.save(p1)
    final.resize((400, 400), Image.LANCZOS).save(p2)
    final.resize((256, 256), Image.LANCZOS).save(p3)
    for p in (p1, p2, p3):
        print("wrote", os.path.relpath(p, ROOT))


if __name__ == "__main__":
    main()
