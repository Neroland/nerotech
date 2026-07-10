#!/usr/bin/env python3
"""Generate the 32x32 teal/plasma texture set for NeroTech.

Visual contract: ../neroland-mc-ecosystem/nerotech/MODELS.md (Stage D of BETA-0.1.0-PLAN.md).
Ported from Nerospace's tools/gen_textures.py at 2x scale (32x32 art, 2px bevels). The shared
machine-face recipe is `noise_fill(ALLOY)` + 2px `bevel(A_LIGHT, A_DARK)` + teal emissive
core/LEDs + corner rivets; every emissive/LED pixel uses ONLY the T_* ramp so the PULSE
animator's HSV mask (gen_animations.py) picks it up. H_* heat colours are BER-only and never
appear in this static art; hazard striping is reserved for the Fusion Reactor shell and the
Configurator item (plus a 4px tier tick on the Stellar Cell's top cap).

* Deterministic: every painter seeds its RNG from the texture name, so re-runs are stable.
* ADDITIVE-ONLY: save() skips any PNG that already exists; pass --force to replace the whole
  set (the one-shot Stage D run that retires the interim 16x art).
* Pillow optional: without it the script exits 0 with a notice so `gradlew genAssets` stays
  green on machines without it (`pip install pillow` to enable regeneration).

Outputs into common/src/main/resources/assets/nerotech/textures/{block,item}
(--multiloader / default; see nerotech_target.py).
"""
import hashlib
import math
import os
import random
import sys

try:
    from PIL import Image
except ModuleNotFoundError:
    print("gen_textures: Pillow not installed; skipping texture generation (pip install pillow).")
    sys.exit(0)

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from nerotech_target import src_base, target_label  # noqa: E402

ROOT = src_base()
print("gen_textures: target = %s" % target_label())
BLOCK_DIR = os.path.join(ROOT, "src/main/resources/assets/nerotech/textures/block")
ITEM_DIR = os.path.join(ROOT, "src/main/resources/assets/nerotech/textures/item")
os.makedirs(BLOCK_DIR, exist_ok=True)
os.makedirs(ITEM_DIR, exist_ok=True)

S = 32  # texture size (Nerospace's 16x recipes scaled x2)
FORCE = "--force" in sys.argv

# ---- Palette (RGBA) — single source of truth is MODELS.md's table; keep them in lockstep ----
CLEAR = (0, 0, 0, 0)
A_DARK = (24, 30, 36, 255)                      # alloy shadow
ALLOY = [(40, 50, 58, 255), (48, 60, 68, 255),  # alloy base ramp
         (34, 42, 50, 255), (58, 72, 82, 255)]
A_LIGHT = (132, 156, 172, 255)                  # alloy highlight
T_DEEP = (8, 40, 48, 255)                       # teal deep
T_TEAL = (16, 118, 130, 255)                    # teal base
T_CYAN = (36, 208, 222, 255)                    # cyan accent
T_PLASMA = (130, 248, 255, 255)                 # plasma
T_GLOW = (206, 255, 255, 255)                   # plasma glow peak
H_WARN = (255, 178, 56, 255)                    # heat warning  — BER-only, never painted here
H_CRIT = (255, 84, 56, 255)                     # heat critical — BER-only, never painted here
HAZ_Y = (255, 206, 44, 255)                     # hazard stripe (fusion shell + configurator only)
HAZ_K = (24, 24, 30, 255)
TEAL_RAMP = [T_DEEP, T_TEAL, T_CYAN, T_PLASMA, T_GLOW]

# Desaturated collar greys (item_sorter ports + the BER cap sprite, which the renderer tints
# per side-config mode — keep them near-greyscale so the tint reads true).
GRAY_D = (96, 99, 104, 255)
GRAY = (128, 131, 136, 255)
GRAY_L = (168, 170, 174, 255)

# Metal-appropriate dust hues (teal-tinted shading is mixed in per-painter).
IRON = [(150, 154, 162, 255), (118, 122, 130, 255), (180, 184, 192, 255)]
COPPER = [(196, 116, 60, 255), (160, 88, 44, 255), (228, 152, 92, 255)]
GOLD = [(236, 192, 64, 255), (200, 152, 40, 255), (252, 222, 120, 255)]

# Coverage ledger: every save() records its name so main() can flag any pre-existing PNG that
# no painter owns (a --force run must replace the WHOLE set — no orphans left on the old art).
PAINTED = {"block": set(), "item": set()}


# ---------------- helpers ----------------

def rng_for(name):
    """Deterministic per-name seed — stable across runs and machines."""
    return random.Random(int(hashlib.md5(name.encode()).hexdigest(), 16) & 0xffffffff)


def new_img():
    return Image.new("RGBA", (S, S), CLEAR)


def _mix(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3)) + (255,)


def noise_fill(img, palette, rng):
    px = img.load()
    for y in range(S):
        for x in range(S):
            px[x, y] = rng.choice(palette)
    return img


def bevel(img, light=A_LIGHT, dark=A_DARK):
    """2px bevel (Nerospace's 1px recipe at 2x): highlight top/left, shadow bottom/right."""
    px = img.load()
    light2 = _mix(light, ALLOY[0], 0.45)
    dark2 = _mix(dark, ALLOY[0], 0.35)
    for i in range(S):
        px[i, 0] = light
        px[0, i] = light
        px[i, S - 1] = dark
        px[S - 1, i] = dark
    for i in range(1, S - 1):
        px[i, 1] = light2
        px[1, i] = light2
        px[i, S - 2] = dark2
        px[S - 2, i] = dark2


def rivets(img, pts=((4, 4), (27, 4), (4, 27), (27, 27))):
    """2x2 corner rivets (the 16x single-pixel rivet at 2x)."""
    px = img.load()
    half = _mix(A_LIGHT, A_DARK, 0.45)
    for (rx, ry) in pts:
        px[rx, ry] = A_LIGHT
        px[rx + 1, ry] = half
        px[rx, ry + 1] = half
        px[rx + 1, ry + 1] = A_DARK


def machine_base(name):
    """The shared machine-face recipe: alloy noise + 2px bevel + corner rivets."""
    img = new_img()
    noise_fill(img, ALLOY, rng_for(name))
    bevel(img)
    rivets(img)
    return img


def recess(px, x0, y0, x1, y1, fill=(12, 16, 20, 255)):
    """Sunken dark panel: fill + inner shadow (top/left) + catch-light on the lower lip."""
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            px[x, y] = fill
    for x in range(x0, x1 + 1):
        px[x, y0] = (6, 9, 12, 255)
    for y in range(y0, y1 + 1):
        px[x0, y] = (6, 9, 12, 255)
    for x in range(x0 + 1, x1 + 1):
        px[x, y1] = _mix(A_LIGHT, A_DARK, 0.55)


def led(px, x, y, col=T_CYAN, core=None):
    """4x4 dark socket with a 2x2 emissive core at (x, y) — T_* only, so PULSE catches it."""
    for yy in range(y - 1, y + 3):
        for xx in range(x - 1, x + 3):
            px[xx, yy] = T_DEEP
    for yy in range(y, y + 2):
        for xx in range(x, x + 2):
            px[xx, yy] = col
    if core:
        px[x, y] = core


def haz_band(px, y0, y1, x0=2, x1=S - 3):
    """Diagonal hazard striping — Fusion Reactor shell / Configurator grip ONLY."""
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            px[x, y] = HAZ_Y if ((x + y) // 3) % 2 == 0 else HAZ_K


def _angdiff(a, b):
    d = (a - b) % (2 * math.pi)
    return d - 2 * math.pi if d > math.pi else d


def save(img, folder, name):
    # ADDITIVE-ONLY: never clobber an existing asset (pass --force to override).
    d = BLOCK_DIR if folder == "block" else ITEM_DIR
    PAINTED[folder].add(name + ".png")
    path = os.path.join(d, name + ".png")
    if os.path.exists(path) and not FORCE:
        print("skip (exists)", os.path.relpath(path, ROOT))
        return
    img.save(path)
    print("wrote", os.path.relpath(path, ROOT))


# ---------------- machine triads (side / _front / _top) ----------------

def gen_nero_generator():
    # side: intake louvres low, exhaust pipe run high
    img = machine_base("nero_generator")
    px = img.load()
    for x in range(4, 28):
        px[x, 6] = ALLOY[3]
        px[x, 7] = A_DARK
    for x in (8, 16, 24):                       # pipe clamps
        px[x, 5] = A_LIGHT
        px[x, 8] = A_DARK
    for y in (18, 20, 22, 24):                  # intake louvres
        for x in range(6, 26):
            px[x, y] = A_DARK
            if x % 6 == 0:
                px[x, y - 1] = A_LIGHT
    save(img, "block", "nero_generator")

    # front: intake grille + twin status LEDs (PULSE target)
    img = machine_base("nero_generator_front")
    px = img.load()
    recess(px, 5, 9, 26, 25)
    for y in (11, 14, 17, 20, 23):              # grille slats
        for x in range(7, 25):
            px[x, y] = ALLOY[1] if x % 2 == 0 else ALLOY[2]
            px[x, y + 1] = A_DARK
    for y in (13, 16, 19, 22):                  # teal furnace-glow between the slats
        for x in range(12, 20):
            px[x, y] = T_TEAL if x in (15, 16) else T_DEEP
    led(px, 8, 5)
    led(px, 22, 5, T_CYAN, T_PLASMA)
    save(img, "block", "nero_generator_front")

    # top: exhaust stack mouth with a grate
    img = machine_base("nero_generator_top")
    px = img.load()
    for y in range(S):
        for x in range(S):
            d = math.hypot(x - 15.5, y - 15.5)
            if d <= 4.0:
                px[x, y] = (10, 13, 16, 255)
            elif d <= 6.5:
                px[x, y] = A_DARK
            elif d <= 8.0:
                px[x, y] = ALLOY[3] if (x + y) % 2 == 0 else ALLOY[1]
    for x in range(10, 22):                     # grate bars over the stack mouth
        if x % 3 == 0:
            for y in range(12, 20):
                if math.hypot(x - 15.5, y - 15.5) <= 4.0:
                    px[x, y] = ALLOY[2]
    led(px, 5, 24, T_TEAL)
    save(img, "block", "nero_generator_top")


def gen_solar_array():
    # side: low pedestal — deck overhang shadow + cable conduit
    img = machine_base("solar_array")
    px = img.load()
    for x in range(4, 28):
        px[x, 4] = A_DARK
        px[x, 5] = ALLOY[2]
    for y in range(8, 26):
        px[15, y] = A_DARK
        px[16, y] = ALLOY[2]
    for y in (10, 16, 22):                      # conduit clamps
        px[14, y] = A_LIGHT
        px[17, y] = A_DARK
    save(img, "block", "solar_array")

    # front: thin charge bar (PULSE target)
    img = machine_base("solar_array_front")
    px = img.load()
    recess(px, 6, 13, 25, 18)
    seg_cols = (T_TEAL, T_TEAL, T_CYAN, T_CYAN, T_PLASMA, T_DEEP)
    for i, col in enumerate(seg_cols):
        sx = 7 + i * 3
        for y in range(14, 18):
            px[sx, y] = col
            px[sx + 1, y] = col
            if sx + 2 <= 24:
                px[sx + 2, y] = (6, 9, 12, 255)
    save(img, "block", "solar_array_front")

    # top: the solar cell grid
    img = machine_base("solar_array_top")
    px = img.load()
    rng = rng_for("solar_array_top_cells")
    for y in range(3, 29):
        for x in range(3, 29):
            if (x - 3) % 6 == 5 or (y - 3) % 6 == 5:
                px[x, y] = A_DARK
            else:
                col = T_DEEP
                if (x + y) % 7 == 0:
                    col = _mix(T_DEEP, T_TEAL, 0.55)
                if rng.random() < 0.04:
                    col = T_CYAN
                px[x, y] = col
    save(img, "block", "solar_array_top")


def gen_ore_processor(adv):
    name = "advanced_ore_processor" if adv else "ore_processor"

    # side: bolted drum-housing seam (+ plasma conduit ridges on the advanced tier)
    img = machine_base(name)
    px = img.load()
    for y in range(4, 28):
        px[15, y] = A_DARK
        px[16, y] = ALLOY[2]
    for y in (6, 13, 20, 26):                   # seam bolts
        px[14, y] = A_LIGHT
        px[17, y] = A_DARK
    if adv:
        for cx in (4, 27):                      # plasma conduit ridges
            for y in range(5, 27):
                px[cx, y] = T_DEEP if y % 4 < 2 else _mix(T_DEEP, T_TEAL, 0.6)
            for y in (8, 16, 24):
                px[cx, y] = T_TEAL
    save(img, "block", name)

    # front: crusher maw glow between interlocking teeth (PULSE target)
    img = machine_base(name + "_front")
    px = img.load()
    recess(px, 5, 7, 26, 25, (10, 13, 16, 255))
    core = T_PLASMA if adv else T_CYAN
    mid = T_CYAN if adv else T_TEAL
    for y in range(13, 19):                     # the maw glow
        for x in range(7, 25):
            dy = abs(y - 15.5)
            px[x, y] = T_DEEP if dy > 2 else (mid if dy > 1 else core)
    for x in (10, 16, 22):                      # sparks
        px[x, 15] = T_GLOW if adv else T_PLASMA
    for x in range(6, 26):                      # interlocking teeth, top and bottom rows
        deep = 12 if ((x - 6) // 4) % 2 == 0 else 10
        for y in range(8, deep):
            px[x, y] = A_LIGHT if x % 4 == 0 else ALLOY[1]
        deep2 = 20 if ((x - 4) // 4) % 2 == 0 else 22
        for y in range(deep2, 25):
            px[x, y] = A_LIGHT if x % 4 == 0 else ALLOY[2]
    if adv:
        for cx in (3, 28):                      # conduit ridges frame the maw
            for y in range(7, 26):
                px[cx, y] = T_DEEP if y % 4 < 2 else _mix(T_DEEP, T_TEAL, 0.6)
    save(img, "block", name + "_front")

    # top: hopper throat stepping down to the drums
    img = machine_base(name + "_top")
    px = img.load()
    for ring, col in ((0, ALLOY[2]), (1, A_DARK), (2, (14, 18, 22, 255))):
        r0 = 5 + ring * 3
        r1 = 26 - ring * 3
        for y in range(r0, r1 + 1):
            for x in range(r0, r1 + 1):
                px[x, y] = col
    for y in range(12, 20):                     # the throat
        for x in range(12, 20):
            px[x, y] = (8, 11, 14, 255)
    for x in range(12, 20):                     # drum slats visible below
        px[x, 15] = ALLOY[3] if x % 2 == 0 else A_DARK
        px[x, 16] = A_DARK if x % 2 == 0 else ALLOY[3]
    if adv:
        px[15, 15] = T_PLASMA
        px[16, 16] = T_TEAL
    save(img, "block", name + "_top")


def gen_fabricator(adv):
    name = "advanced_fabricator" if adv else "fabricator"

    # side: tool cabinet + vent slats (+ teal conduit / crystal sight-glass on advanced)
    img = machine_base(name)
    px = img.load()
    for y in range(15, 27):
        for x in range(6, 26):
            px[x, y] = A_DARK if (x + y) % 5 == 0 else ALLOY[2]
    for x in range(6, 26):
        px[x, 15] = A_DARK
    for x in range(14, 18):                     # drawer handle
        px[x, 20] = A_LIGHT
    for x in range(7, 25, 3):                   # vent slats
        px[x, 5] = A_DARK
        px[x + 1, 5] = A_DARK
    if adv:
        for y in range(6, 27):
            px[27, y] = T_DEEP if y % 4 < 2 else _mix(T_DEEP, T_TEAL, 0.6)
        for (dx, dy, c) in ((0, 0, T_CYAN), (1, 0, T_TEAL), (0, 1, T_TEAL),
                            (-1, 0, T_DEEP), (0, -1, T_DEEP)):
            px[9 + dx, 9 + dy] = c              # crystal sight-glass
    save(img, "block", name)

    # front: workbed slit (twin slits + crystal on advanced) + progress LEDs (PULSE target)
    img = machine_base(name + "_front")
    px = img.load()
    if not adv:
        recess(px, 5, 10, 26, 22)
        for x in range(7, 25):
            px[x, 14] = T_DEEP
            px[x, 15] = T_CYAN if x % 5 == 0 else T_TEAL
            px[x, 16] = T_CYAN if 12 <= x <= 19 else T_TEAL
            px[x, 17] = T_DEEP
        px[15, 16] = T_PLASMA
        px[16, 16] = T_PLASMA
        lit = 3
    else:
        recess(px, 5, 8, 26, 24)
        for sy in (11, 19):
            for x in range(7, 25):
                px[x, sy] = T_CYAN if x % 5 == 0 else T_TEAL
                px[x, sy + 1] = T_CYAN if 11 <= x <= 20 else T_TEAL
        px[15, 12] = T_PLASMA
        px[16, 20] = T_PLASMA
        for (dx, dy, c) in ((0, 0, T_GLOW), (1, 0, T_PLASMA), (-1, 0, T_PLASMA),
                            (0, 1, T_PLASMA), (0, -1, T_PLASMA)):
            px[15 + dx, 15 + dy] = c            # the suspended void-crystal indicator
        lit = 6
    for i in range(6):                          # progress LED strip
        col = T_CYAN if i < lit else T_DEEP
        px[7 + i * 3, 27] = col
        px[8 + i * 3, 27] = col
    save(img, "block", name + "_front")

    # top: workbed + gantry rails (+ clamp field ring / crystal on advanced)
    img = machine_base(name + "_top")
    px = img.load()
    recess(px, 4, 6, 27, 25, (16, 20, 24, 255))
    for y in range(9, 23):                      # bed alignment dots
        for x in range(6, 26):
            if x % 4 == 2 and y % 4 == 0:
                px[x, y] = A_DARK
    for ry in ((8, 22) if not adv else (7, 12, 19, 24)):
        for x in range(5, 27):
            px[x, ry] = A_LIGHT
            px[x, ry + 1] = A_DARK
    if not adv:
        for y in range(14, 18):                 # workpiece on the bed
            for x in range(14, 18):
                px[x, y] = T_TEAL
        px[15, 15] = T_CYAN
        px[16, 16] = T_CYAN
    else:
        for y in range(10, 22):                 # plasma clamp field
            for x in range(10, 22):
                d = math.hypot(x - 15.5, y - 15.5)
                if 4.0 <= d <= 5.5 and (x + y) % 3 == 0:
                    px[x, y] = T_DEEP
        for (dx, dy, c) in ((0, 0, T_GLOW), (1, 0, T_PLASMA), (-1, 0, T_PLASMA),
                            (0, 1, T_PLASMA), (0, -1, T_PLASMA)):
            px[15 + dx, 15 + dy] = c            # the void-crystal in its clamp
    save(img, "block", name + "_top")


def gen_fusion_reactor():
    # side: hazard-striped shell bands + heavy plate seams (the ONE hazard-striped block)
    img = machine_base("fusion_reactor")
    px = img.load()
    haz_band(px, 3, 6)
    haz_band(px, 25, 28)
    for x in range(2, 30):
        px[x, 2] = A_DARK
        px[x, 7] = A_DARK
        px[x, 24] = A_DARK
        px[x, 29] = A_DARK
    for y in (12, 19):                          # heavy plate seams
        for x in range(3, 29):
            px[x, y] = A_DARK
    for x in (6, 15, 25):                       # plate bolts
        px[x, 15] = A_LIGHT
        px[x, 16] = A_DARK
    save(img, "block", "fusion_reactor")

    # front: round viewport ring over a plasma core (PULSE target — teal only, no hazard here
    # so the animator's mask never strobes the stripes)
    img = machine_base("fusion_reactor_front")
    px = img.load()
    for y in range(S):
        for x in range(S):
            d = math.hypot(x - 15.5, y - 15.5)
            if d <= 3.0:
                px[x, y] = T_GLOW if d <= 1.6 else T_PLASMA
            elif d <= 5.5:
                px[x, y] = T_PLASMA if (x + y) % 4 == 0 else T_CYAN
            elif d <= 7.5:
                px[x, y] = T_CYAN if (x * 2 + y) % 5 == 0 else T_TEAL
            elif d <= 9.0:
                px[x, y] = T_DEEP
            elif d <= 12.0:
                if d >= 11.2 or d <= 9.8:
                    px[x, y] = A_DARK
                else:
                    px[x, y] = ALLOY[3] if (x + y) % 2 == 0 else ALLOY[1]
    for k in range(8):                          # viewport ring bolts
        ang = k * math.pi / 4
        px[int(round(15.5 + 10.5 * math.cos(ang))),
           int(round(15.5 + 10.5 * math.sin(ang)))] = A_LIGHT
    save(img, "block", "fusion_reactor_front")

    # top: octagonal plate (chamfered corners w/ hazard edge) + torus ring
    img = machine_base("fusion_reactor_top")
    px = img.load()
    for y in range(2, 30):
        for x in range(2, 30):
            m = min(x, 31 - x) + min(y, 31 - y)
            if m < 6:
                px[x, y] = A_DARK
            elif m < 8:
                px[x, y] = HAZ_Y if ((x + y) // 2) % 2 == 0 else HAZ_K
    for y in range(S):
        for x in range(S):
            d = math.hypot(x - 15.5, y - 15.5)
            if 8.0 <= d <= 11.5:
                ang = math.atan2(y - 15.5, x - 15.5)
                seg = int((ang + math.pi) / (math.pi / 6)) % 2
                px[x, y] = _mix(T_DEEP, T_TEAL, 0.7) if seg else T_DEEP
            elif d < 4.0:
                px[x, y] = A_DARK if (x + y) % 2 == 0 else ALLOY[2]
    for k in range(4):                          # torus pips
        ang = k * math.pi / 2 + math.pi / 4
        px[int(round(15.5 + 9.7 * math.cos(ang))),
           int(round(15.5 + 9.7 * math.sin(ang)))] = T_CYAN
    save(img, "block", "fusion_reactor_top")


def gen_fusion_casing():
    # multiblock shell plate: dark alloy + dashed teal conduit ring + plate seams. NO hazard
    # striping here — that stays on the controller shell so the casing reads as plain material.
    img = machine_base("fusion_casing")
    px = img.load()
    for i in range(3, 29):                      # dashed conduit ring just inside the bevel
        c = T_DEEP if i % 4 < 2 else _mix(T_DEEP, T_TEAL, 0.6)
        px[i, 2] = c
        px[i, 29] = c
        px[2, i] = c
        px[29, i] = c
    for i in (8, 16, 24):                       # conduit junction pips
        px[i, 2] = T_TEAL
        px[i, 29] = T_TEAL
        px[2, i] = T_TEAL
        px[29, i] = T_TEAL
    for x in range(6, 26):                      # heavy plate seam cross
        px[x, 15] = A_DARK
        px[x, 16] = ALLOY[2]
    for y in range(6, 26):
        px[15, y] = A_DARK
        px[16, y] = ALLOY[2]
    rivets(img, ((7, 7), (24, 7), (7, 24), (24, 24)))   # subtle inner rivet square
    px[15, 15] = A_LIGHT                        # hub bolt where the seams cross
    px[16, 16] = A_DARK
    save(img, "block", "fusion_casing")


def gen_fusion_containment_glass():
    # mostly-transparent teal pane: low-alpha fill, brighter teal frame, faint plasma glints.
    # The ONLY texture in the set with real partial alpha (model renders translucent).
    img = new_img()
    px = img.load()
    rng = rng_for("fusion_containment_glass")
    for y in range(S):                          # low-alpha teal fill with a faint weave
        for x in range(S):
            a = 46 if (x + y) % 2 == 0 else 38
            col = T_TEAL if (x * 3 + y) % 13 else T_DEEP
            px[x, y] = col[:3] + (a,)
    for k in range(9):                          # faint plasma glints drifting in the field
        gx = rng.randrange(4, 28)
        gy = rng.randrange(4, 28)
        px[gx, gy] = T_PLASMA[:3] + (110,)
        if rng.random() < 0.4:
            px[min(gx + 1, 27), gy] = T_CYAN[:3] + (80,)
    for i in range(S):                          # bright teal frame border
        for (fx, fy) in ((i, 0), (0, i), (i, S - 1), (S - 1, i)):
            px[fx, fy] = T_CYAN[:3] + (210,)
    for i in range(1, S - 1):
        for (fx, fy) in ((i, 1), (1, i), (i, S - 2), (S - 2, i)):
            px[fx, fy] = T_TEAL[:3] + (150,)
    for (cx, cy) in ((2, 2), (29, 2), (2, 29), (29, 29)):   # frame corner studs
        px[cx, cy] = T_GLOW[:3] + (220,)
    save(img, "block", "fusion_containment_glass")


def gen_auto_crafter():
    # side: press housing — hydraulic columns + cross beam
    img = machine_base("auto_crafter")
    px = img.load()
    for cx in (7, 24):
        for y in range(6, 26):
            px[cx, y] = ALLOY[3]
            px[cx + 1, y] = A_DARK
        px[cx, 6] = A_LIGHT
    for x in range(5, 27):
        px[x, 10] = A_DARK
        px[x, 11] = ALLOY[2]
    save(img, "block", "auto_crafter")

    # front: hologram emitter lens (PULSE target)
    img = machine_base("auto_crafter_front")
    px = img.load()
    recess(px, 5, 5, 26, 26, (10, 13, 16, 255))
    for y in range(S):
        for x in range(S):
            d = math.hypot(x - 15.5, y - 14.5)
            if d <= 2.0:
                px[x, y] = T_GLOW if d <= 1.0 else T_PLASMA
            elif d <= 6.8:
                px[x, y] = _mix(T_DEEP, T_TEAL, 0.75) if int(d) % 2 == 0 else T_DEEP
            elif d <= 8.2:
                px[x, y] = A_DARK
    px[11, 10] = A_LIGHT                        # lens specular
    for x in range(12, 20):                     # emitter slit under the lens
        px[x, 24] = T_CYAN if x % 3 == 0 else T_TEAL
    save(img, "block", "auto_crafter_front")

    # top: press plate with a cross ridge + guide rods
    img = machine_base("auto_crafter_top")
    px = img.load()
    recess(px, 5, 5, 26, 26, (16, 20, 24, 255))
    for y in range(10, 22):
        for x in range(10, 22):
            px[x, y] = ALLOY[3] if (x + y) % 2 == 0 else ALLOY[1]
    for i in range(10, 22):
        px[i, 15] = A_LIGHT
        px[i, 16] = A_DARK
        px[15, i] = A_LIGHT
        px[16, i] = A_DARK
    for (gx, gy) in ((7, 7), (24, 7), (7, 24), (24, 24)):
        px[gx, gy] = A_LIGHT
        px[gx + (1 if gx < 15 else -1), gy] = A_DARK
    px[15, 15] = T_CYAN                         # lens core peeking through
    px[16, 16] = T_CYAN
    save(img, "block", "auto_crafter_top")


def gen_item_sorter():
    # side: a port collar (the junction has ports on every face)
    img = machine_base("item_sorter")
    px = img.load()
    for y in range(S):
        for x in range(S):
            d = math.hypot(x - 15.5, y - 15.5)
            if d <= 4.5:
                px[x, y] = (10, 13, 16, 255) if (x + y) % 3 == 0 else A_DARK
            elif d <= 7.0:
                px[x, y] = GRAY
            elif d <= 9.0:
                px[x, y] = GRAY_L if (x + y) % 2 == 0 else GRAY
            elif d <= 10.0:
                px[x, y] = GRAY_D
    for k in range(4):                          # collar clamp bolts
        ang = k * math.pi / 2 + math.pi / 4
        px[int(round(15.5 + 8 * math.cos(ang))),
           int(round(15.5 + 8 * math.sin(ang)))] = A_LIGHT
    save(img, "block", "item_sorter")

    # front: six-dot routing matrix (PULSE target)
    img = machine_base("item_sorter_front")
    px = img.load()
    recess(px, 6, 6, 25, 25, (10, 13, 16, 255))
    dots = ((11, 10, T_CYAN, None), (19, 10, T_CYAN, None),
            (11, 15, T_PLASMA, T_GLOW), (19, 15, T_TEAL, None),
            (11, 20, T_CYAN, None), (19, 20, T_CYAN, None))
    for (dx2, dy2, c, core) in dots:
        led(px, dx2, dy2, c, core)
    for y in (10, 15, 20):                      # routing traces between the pairs
        for x in range(14, 18):
            px[x, y + 1] = T_DEEP
    save(img, "block", "item_sorter_front")

    # top: routing cross channels + the top port collar
    img = machine_base("item_sorter_top")
    px = img.load()
    for i in range(4, 28):
        px[i, 15] = (10, 13, 16, 255)
        px[i, 16] = (10, 13, 16, 255)
        px[15, i] = (10, 13, 16, 255)
        px[16, i] = (10, 13, 16, 255)
    for i in range(5, 27, 3):                   # teal route pips in the channels
        px[i, 15] = T_DEEP
        px[16, i] = T_DEEP
    for y in range(S):
        for x in range(S):
            d = math.hypot(x - 15.5, y - 15.5)
            if d <= 3.0:
                px[x, y] = (10, 13, 16, 255)
            elif d <= 5.0:
                px[x, y] = GRAY
            elif d <= 6.0:
                px[x, y] = GRAY_D
    save(img, "block", "item_sorter_top")


def gen_scrubber():
    # side: intake louvres low + a duct seam feeding the stack (the filter bay's frame elements
    # reuse this texture, so it stays generic plate)
    img = machine_base("scrubber")
    px = img.load()
    for y in range(5, 27):                      # duct seam up to the stack
        px[11, y] = A_DARK
        px[12, y] = ALLOY[2]
    for y in (8, 15, 22):                       # seam clamps
        px[10, y] = A_LIGHT
        px[13, y] = A_DARK
    for y in (18, 20, 22, 24):                  # intake louvres
        for x in range(16, 28):
            px[x, y] = A_DARK
            if x % 6 == 4:
                px[x, y - 1] = A_LIGHT
    save(img, "block", "scrubber")

    # front: circular cowl grille around the fan window + twin teal status LEDs (PULSE target)
    img = machine_base("scrubber_front")
    px = img.load()
    recess(px, 6, 8, 26, 28, (10, 13, 16, 255))
    for y in range(S):                          # concentric cowl rings + radial spokes
        for x in range(S):
            d = math.hypot(x - 16.0, y - 18.0)
            if d > 10.0 or y < 9 or y > 27 or x < 7 or x > 25:
                continue
            ang = math.atan2(y - 18.0, x - 16.0)
            spoke = int((ang + math.pi) / (math.pi / 4)) % 2 == 0 and 3.0 < d < 9.0
            if 9.0 <= d <= 10.0:
                px[x, y] = A_DARK
            elif 7.5 <= d < 9.0:
                px[x, y] = ALLOY[3] if (x + y) % 2 == 0 else ALLOY[1]
            elif spoke and (x + y) % 2 == 0:
                px[x, y] = ALLOY[2]             # grille spokes over the dark intake
            elif d <= 2.0:
                px[x, y] = T_DEEP if d > 1.0 else T_TEAL   # hub glow behind the fan
    led(px, 8, 4)
    led(px, 22, 4, T_CYAN, T_PLASMA)
    save(img, "block", "scrubber_front")

    # top: exhaust stack mouth (offset over the model's stack) + vent grille + tell-tale LED
    img = machine_base("scrubber_top")
    px = img.load()
    for y in range(S):
        for x in range(S):
            d = math.hypot(x - 12.0, y - 22.0)
            if d <= 3.5:
                px[x, y] = (10, 13, 16, 255)
            elif d <= 5.5:
                px[x, y] = A_DARK
            elif d <= 7.0:
                px[x, y] = ALLOY[3] if (x + y) % 2 == 0 else ALLOY[1]
    for x in range(8, 17):                      # grate bars over the stack mouth
        if x % 3 == 2:
            for y in range(19, 26):
                if math.hypot(x - 12.0, y - 22.0) <= 3.5:
                    px[x, y] = ALLOY[2]
    for y in (6, 8, 10):                        # vent grille over the vent box
        for x in range(20, 27):
            px[x, y] = A_DARK
    led(px, 24, 22, T_TEAL)
    save(img, "block", "scrubber_top")


def gen_remediator():
    # side: low process tank band with a teal fill line (the body is the block's lower half;
    # the boom mounts reuse this texture as plain plate)
    img = machine_base("remediator")
    px = img.load()
    for x in range(3, 29):                      # tank band seams (lower half = the body)
        px[x, 16] = A_DARK
        px[x, 17] = ALLOY[3]
        px[x, 28] = A_DARK
    for x in range(4, 28):                      # teal fill line in the sight channel
        px[x, 22] = T_DEEP if x % 4 < 2 else T_TEAL
    for x in (6, 15, 24):                       # band bolts
        px[x, 18] = A_LIGHT
        px[x, 27] = A_DARK
    save(img, "block", "remediator")

    # front: tank sight window + spray-nozzle row above it (PULSE target)
    img = machine_base("remediator_front")
    px = img.load()
    recess(px, 7, 17, 24, 28, (10, 13, 16, 255))
    for y in range(19, 28):                     # the tank window: teal fluid, brighter at the surface
        for x in range(9, 23):
            if y == 19:
                px[x, y] = T_CYAN if x % 5 == 0 else T_TEAL   # the fill surface line
            elif y < 23:
                px[x, y] = T_TEAL if (x + y) % 6 == 0 else _mix(T_DEEP, T_TEAL, 0.5)
            else:
                px[x, y] = T_DEEP
    px[15, 19] = T_PLASMA                       # surface glint
    for nx in (8, 13, 18, 23):                  # spray-nozzle motif above the window
        px[nx, 12] = A_DARK
        px[nx + 1, 12] = A_DARK
        px[nx, 13] = A_DARK
        px[nx + 1, 13] = A_DARK
        px[nx, 14] = T_CYAN                     # nozzle tips (emissive)
        px[nx + 1, 14] = T_CYAN
    led(px, 25, 4, T_CYAN, T_PLASMA)
    save(img, "block", "remediator_front")

    # top: deck plate with the two boom-mount pads + a teal feed conduit between them
    img = machine_base("remediator_top")
    px = img.load()
    for (mx0, mx1) in ((4, 10), (22, 28)):      # mount pads
        for y in range(12, 20):
            for x in range(mx0, mx1 + 1):
                px[x, y] = A_DARK if (x in (mx0, mx1) or y in (12, 19)) else (14, 18, 22, 255)
        px[mx0 + 3, 15] = A_LIGHT               # pivot pin
        px[mx0 + 3, 16] = A_LIGHT
    for x in range(11, 22):                     # feed conduit between the pads
        px[x, 15] = T_DEEP if x % 4 < 2 else _mix(T_DEEP, T_TEAL, 0.6)
        px[x, 16] = A_DARK
    for i in (12, 16, 20):                      # conduit pips
        px[i, 15] = T_TEAL
    save(img, "block", "remediator_top")


def gen_analytics_terminal():
    # side: console housing — a cable trunk feeding the screen head + a data-port cluster low
    # (the pedestal/column/bezel elements all reuse this as generic plate)
    img = machine_base("analytics_terminal")
    px = img.load()
    for y in range(4, 28):                      # cable trunk up the housing
        px[15, y] = A_DARK
        px[16, y] = ALLOY[2]
        px[17, y] = A_DARK
    for y in (7, 14, 21):                       # trunk clamps
        px[14, y] = A_LIGHT
        px[18, y] = A_DARK
    for (dy, on) in ((22, True), (25, False)):  # data-port pair low on the housing
        for x in range(5, 11):
            px[x, dy] = A_DARK
            px[x, dy + 1] = (12, 16, 20, 255)
        if on:
            px[6, dy + 1] = T_TEAL              # one live link pip (kept dim — sides stay quiet)
    save(img, "block", "analytics_terminal")

    # front: the dark dashboard screen — teal readout rows of ragged lengths + a sparkline trace
    # + a status-LED strip under the bezel (PULSE target)
    img = machine_base("analytics_terminal_front")
    px = img.load()
    recess(px, 4, 5, 27, 24, (8, 11, 14, 255))
    rng = rng_for("analytics_terminal_front_rows")
    for i, ry in enumerate((8, 12, 16)):        # readout rows: label tick + ragged data bar
        px[6, ry] = T_CYAN
        px[7, ry] = T_CYAN
        for x in range(9, 9 + rng.randrange(10, 16)):
            px[x, ry] = T_TEAL if (x + i) % 5 else T_DEEP
    trace = (22, 21, 21, 20, 21, 22, 21, 20, 19, 20, 21, 21, 20, 19, 19, 20, 21, 22, 21, 20)
    for i, ty in enumerate(trace):              # sparkline trace along the screen bottom
        px[6 + i, ty] = T_CYAN if i % 4 else T_PLASMA
    for i, lx in enumerate((8, 14, 20)):        # status LEDs under the bezel
        led(px, lx, 27, T_CYAN if i != 1 else T_TEAL, T_PLASMA if i == 0 else None)
    save(img, "block", "analytics_terminal_front")

    # top: deck plate with the hologram projector lens (teal ring) behind a vent grille
    img = machine_base("analytics_terminal_top")
    px = img.load()
    for y in range(S):
        for x in range(S):
            d = math.hypot(x - 16.0, y - 17.0)
            if d <= 2.0:
                px[x, y] = T_CYAN if d > 1.0 else T_PLASMA   # projector lens
            elif d <= 3.5:
                px[x, y] = T_DEEP
            elif d <= 5.0:
                px[x, y] = A_DARK
            elif d <= 6.5:
                px[x, y] = ALLOY[3] if (x + y) % 2 == 0 else ALLOY[1]
    for y in (5, 7, 9):                         # vent grille toward the back edge
        for x in range(8, 25):
            px[x, y] = A_DARK
            if x % 6 == 2:
                px[x, y - 1] = A_LIGHT
    save(img, "block", "analytics_terminal_top")


# ---------------- BER sprites (dynamic geometry textures) ----------------

def gen_ber_sprites():
    # nero_generator_rotor — radial turbine blades on transparent (spun by the BER).
    img = new_img()
    px = img.load()
    for y in range(S):
        for x in range(S):
            d = math.hypot(x - 15.5, y - 15.5)
            if d > 15.2:
                continue
            ang = math.atan2(y - 15.5, x - 15.5)
            if d <= 3.5:
                px[x, y] = A_DARK if d <= 1.5 else (ALLOY[3] if (x + y) % 2 == 0 else ALLOY[1])
            elif d <= 14.5:
                a = ((ang + d * 0.10) / (2 * math.pi) * 8.0) % 1.0
                w = 0.34 - d * 0.008            # blades taper toward the rim
                if a < w:
                    if d >= 13.0:
                        px[x, y] = T_CYAN       # emissive blade tips
                    elif a < 0.07:
                        px[x, y] = A_LIGHT      # leading edge
                    else:
                        px[x, y] = ALLOY[1] if a < w * 0.6 else ALLOY[2]
    px[15, 15] = A_LIGHT
    save(img, "block", "nero_generator_rotor")

    # solar_array_deck — the pitching panel: framed solar cell grid.
    img = new_img()
    px = img.load()
    rng = rng_for("solar_array_deck")
    for y in range(S):
        for x in range(S):
            if x < 2 or y < 2 or x > 29 or y > 29:
                px[x, y] = ALLOY[1] if (x + y) % 2 == 0 else ALLOY[2]
            elif (x - 2) % 7 == 6 or (y - 2) % 7 == 6:
                px[x, y] = A_DARK
            else:
                col = T_DEEP
                if (x + y) % 9 == 0:
                    col = _mix(T_DEEP, T_TEAL, 0.6)
                if rng.random() < 0.035:
                    col = T_CYAN
                px[x, y] = col
    for i in range(S):                          # frame bevel
        px[i, 0] = A_LIGHT
        px[0, i] = A_LIGHT
        px[i, S - 1] = A_DARK
        px[S - 1, i] = A_DARK
    save(img, "block", "solar_array_deck")

    # ore_processor_drum — toothed crusher cylinder, TILEABLE along its (x) axis: the pattern is
    # a pure function of x % 8, so the BER can wrap it around a spinning drum with no seam.
    img = new_img()
    px = img.load()
    for y in range(S):
        v = math.sin(math.pi * (y + 0.5) / S)   # cylinder shading, bright at the equator
        base = _mix(A_DARK, ALLOY[3], 0.25 + 0.75 * v)
        for x in range(S):
            k = x % 8
            if k < 4:                           # tooth
                if k == 0:
                    col = _mix(base, A_LIGHT, 0.35)
                elif k == 3:
                    col = _mix(base, A_DARK, 0.35)
                else:
                    col = _mix(base, A_LIGHT, 0.15)
            else:                               # gap
                col = _mix(base, A_DARK, 0.55)
                if k == 5 and 10 <= y <= 21:
                    col = _mix(col, T_DEEP, 0.5)
            px[x, y] = col
    save(img, "block", "ore_processor_drum")

    # fabricator_arm — alloy gantry beam with a teal actuator carriage, transparent elsewhere.
    img = new_img()
    px = img.load()
    for y in range(12, 20):                     # the beam
        for x in range(S):
            if y == 12:
                col = A_LIGHT
            elif y == 19:
                col = A_DARK
            elif y in (15, 16):
                col = A_DARK if x % 6 < 3 else (16, 20, 24, 255)   # rail slot
            else:
                col = ALLOY[1] if (x + y) % 3 else ALLOY[2]
            px[x, y] = col
    for y in range(8, 24):                      # actuator carriage
        for x in range(12, 20):
            px[x, y] = ALLOY[2] if (x + y) % 2 == 0 else ALLOY[0]
    for x in range(12, 20):
        px[x, 8] = A_LIGHT
        px[x, 23] = A_DARK
    for y in range(8, 24):
        px[12, y] = A_LIGHT
        px[19, y] = A_DARK
    for y in range(13, 19):                     # teal actuator core
        for x in range(14, 18):
            d = max(abs(x - 15.5), abs(y - 15.5))
            px[x, y] = T_CYAN if d < 1.2 else T_TEAL
    px[15, 15] = T_PLASMA
    px[16, 15] = T_PLASMA
    save(img, "block", "fabricator_arm")

    # fusion_reactor_plasma — soft radial teal-white wisp on transparent (alpha falls off with
    # radius; three swirl arms). PULSE gives it the glow breathing in gen_animations.py.
    img = new_img()
    px = img.load()
    for y in range(S):
        for x in range(S):
            dx = x - 15.5
            dy = y - 15.5
            r = math.hypot(dx, dy)
            if r > 15.5:
                continue
            ang = math.atan2(dy, dx)
            g = max(0.0, 1.0 - r / 15.0)
            swirl = 0.5 + 0.5 * math.sin(3.0 * ang + r * 0.85)
            a = (g ** 1.5) * (0.45 + 0.55 * swirl)
            if a < 0.06:
                continue
            if a > 0.85:
                col = T_GLOW
            elif a > 0.62:
                col = T_PLASMA
            elif a > 0.38:
                col = T_CYAN
            elif a > 0.18:
                col = T_TEAL
            else:
                col = T_DEEP
            px[x, y] = col[:3] + (min(255, int(90 + 165 * a)),)
    save(img, "block", "fusion_reactor_plasma")

    # auto_crafter_press — the alloy stamp face that slams on each craft.
    img = new_img()
    noise_fill(img, ALLOY, rng_for("auto_crafter_press"))
    bevel(img)
    rivets(img)
    px = img.load()
    for y in range(9, 23):                      # recessed stamp face
        for x in range(9, 23):
            px[x, y] = A_DARK if (x + y) % 2 == 0 else ALLOY[2]
    for i in range(9, 23):
        px[i, 15] = A_LIGHT
        px[i, 16] = A_DARK
        px[15, i] = A_LIGHT
        px[16, i] = A_DARK
    for (bx, by) in ((6, 6), (25, 6), (6, 25), (25, 25)):
        px[bx, by] = A_LIGHT
        px[bx, by + 1] = A_DARK
    save(img, "block", "auto_crafter_press")

    # item_sorter_cap — neutral light-grey collar cap. DELIBERATELY desaturated: the BER tints
    # it per side-config mode, so any hue baked in here would pollute the mode colours.
    img = new_img()
    px = img.load()
    for y in range(S):
        for x in range(S):
            d = math.hypot(x - 15.5, y - 15.5)
            if d > 15.0:
                continue
            if d <= 4.5:
                col = (40, 42, 46, 255)
            elif d <= 8.5:
                col = GRAY_D
            elif d <= 12.0:
                col = GRAY
            else:
                col = GRAY_L
            ang = math.atan2(y - 15.5, x - 15.5)
            if 12.0 < d <= 15.0 and -2.6 < ang < -1.2:
                col = (196, 198, 202, 255)      # top sheen (still grey)
            px[x, y] = col
    for k in range(4):
        ang = k * math.pi / 2 + math.pi / 4
        px[int(round(15.5 + 13.3 * math.cos(ang))),
           int(round(15.5 + 13.3 * math.sin(ang)))] = (70, 72, 76, 255)
    save(img, "block", "item_sorter_cap")

    # scrubber_fan — radial FOUR-blade intake fan on transparent (the rotor recipe at 4 blades;
    # the BER stacks two of these 45° apart for an eight-blade cross).
    img = new_img()
    px = img.load()
    for y in range(S):
        for x in range(S):
            d = math.hypot(x - 15.5, y - 15.5)
            if d > 15.2:
                continue
            ang = math.atan2(y - 15.5, x - 15.5)
            if d <= 3.0:
                px[x, y] = A_DARK if d <= 1.2 else (ALLOY[3] if (x + y) % 2 == 0 else ALLOY[1])
            elif d <= 14.5:
                a = ((ang + d * 0.12) / (2 * math.pi) * 4.0) % 1.0
                w = 0.30 - d * 0.006            # blades taper toward the rim
                if a < w:
                    if d >= 13.0:
                        px[x, y] = T_CYAN       # emissive blade tips
                    elif a < 0.05:
                        px[x, y] = A_LIGHT      # leading edge
                    else:
                        px[x, y] = ALLOY[1] if a < w * 0.6 else ALLOY[2]
    px[15, 15] = A_LIGHT
    save(img, "block", "scrubber_fan")

    # remediator_boom — thin alloy spray-boom arm on transparent, nozzle tips teal. Drawn
    # base-down: the BER maps v=0 to the boom tip, so the tip nozzles sit at the TOP rows.
    img = new_img()
    px = img.load()
    for y in range(2, 30):                      # the arm: full height, narrow
        for x in range(12, 20):
            if x == 12:
                col = A_LIGHT
            elif x == 19:
                col = A_DARK
            else:
                col = ALLOY[1] if (x + y) % 3 else ALLOY[2]
            px[x, y] = col
    for y in range(6, 28, 4):                   # nozzle pips down the spray edge
        px[10, y] = A_DARK
        px[11, y] = T_TEAL
    for y in range(2, 6):                       # emissive tip nozzle cluster
        for x in range(13, 19):
            px[x, y] = T_CYAN if (x + y) % 2 == 0 else T_TEAL
    px[15, 2] = T_PLASMA
    px[16, 3] = T_PLASMA
    for x in range(12, 20):                     # root collar at the pivot end
        px[x, 28] = A_LIGHT
        px[x, 29] = A_DARK
    save(img, "block", "remediator_boom")


# ---------------- items ----------------

def gen_dust(name, ramp):
    """Dust pile: metal-appropriate hue, teal-tinted shading (MODELS.md item rule)."""
    rng = rng_for(name)
    img = new_img()
    px = img.load()
    shadow = _mix(ramp[1], T_DEEP, 0.45)
    for y in range(12, 28):
        half = min(12, int((y - 10) * 0.75))
        for x in range(16 - half, 16 + half):
            if rng.random() < 0.9:
                r = rng.random()
                col = ramp[2] if (y < 16 and r < 0.35) else (ramp[0] if r < 0.75 else ramp[1])
                if y >= 24 or x < 16 - half + 2 or x > 16 + half - 3:
                    if rng.random() < 0.6:
                        col = shadow
                px[x, y] = col
    for (sx, sy) in ((10, 9), (20, 8), (15, 6), (23, 12), (8, 13)):
        px[sx, sy] = ramp[2 if (sx + sy) % 2 == 0 else 0]
    save(img, "item", name)


def gen_circuit_board():
    rng = rng_for("circuit_board")
    img = new_img()
    px = img.load()
    substrate = [(10, 44, 50, 255), (8, 38, 44, 255), (12, 50, 56, 255)]
    x0, y0, x1, y1 = 5, 7, 26, 24
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            px[x, y] = rng.choice(substrate)
    for x in range(x0, x1 + 1):                 # board edges
        px[x, y0] = _mix(substrate[0], A_LIGHT, 0.3)
        px[x, y1] = _mix(substrate[1], A_DARK, 0.5)
    for y in range(y0, y1 + 1):
        px[x0, y] = _mix(substrate[0], A_LIGHT, 0.3)
        px[x1, y] = _mix(substrate[1], A_DARK, 0.5)
    for ty in (10, 14, 18, 21):                 # traces with node dots
        for x in range(x0 + 2, x1 - 1):
            px[x, ty] = T_TEAL
        px[x0 + 2, ty] = T_CYAN
        px[x1 - 2, ty] = T_CYAN
    for y in range(12, 20):                     # the chip
        for x in range(13, 19):
            px[x, y] = A_DARK
    for y in range(13, 19):
        px[12, y] = A_LIGHT                     # chip pins
        px[19, y] = A_LIGHT
    px[14, 13] = T_PLASMA                       # chip status dot
    for (hx, hy) in ((x0 + 1, y0 + 1), (x1 - 1, y0 + 1), (x0 + 1, y1 - 1), (x1 - 1, y1 - 1)):
        px[hx, hy] = A_LIGHT                    # mount holes
    save(img, "item", "circuit_board")


def gen_machine_frame():
    rng = rng_for("machine_frame")
    img = new_img()
    px = img.load()
    for y in range(4, 28):                      # hollow frame beams (centre stays transparent)
        for x in range(4, 28):
            if 8 <= x <= 23 and 8 <= y <= 23:
                continue
            px[x, y] = rng.choice(ALLOY)
    for i in range(4, 28):                      # outer edges
        px[i, 4] = A_LIGHT
        px[4, i] = A_LIGHT
        px[i, 27] = A_DARK
        px[27, i] = A_DARK
    for i in range(8, 24):                      # inner rim (opposite shading — a hole)
        px[i, 8] = A_DARK
        px[8, i] = A_DARK
        px[i, 23] = _mix(A_LIGHT, A_DARK, 0.4)
        px[23, i] = _mix(A_LIGHT, A_DARK, 0.4)
    rivets(img, ((6, 6), (25, 6), (6, 25), (25, 25)))
    for (nx, ny) in ((15, 6), (15, 25), (6, 15), (25, 15)):
        px[nx, ny] = T_TEAL                     # teal locator nodes
        px[nx + 1, ny] = T_TEAL
    save(img, "item", "machine_frame")


def gen_nero_coil():
    img = new_img()
    px = img.load()
    rng = rng_for("nero_coil")
    for (cy0, cy1) in ((5, 8), (23, 26)):       # bobbin caps
        for y in range(cy0, cy1 + 1):
            for x in range(10, 22):
                px[x, y] = rng.choice(ALLOY)
        for x in range(10, 22):
            px[x, cy0] = A_LIGHT
            px[x, cy1] = A_DARK
    for y in range(9, 23):                      # windings
        for x in range(11, 21):
            col = T_TEAL if y % 2 == 0 else T_DEEP
            if x == 13:
                col = T_CYAN if y % 2 == 0 else T_TEAL
            elif x >= 19:
                col = _mix(col, A_DARK, 0.45)
            px[x, y] = col
    for x in range(12, 20):                     # energised centre band
        px[x, 15] = T_CYAN
    px[15, 15] = T_PLASMA
    px[16, 15] = T_PLASMA
    for tx in (15, 16):                         # terminals
        px[tx, 4] = A_LIGHT
        px[tx, 27] = A_LIGHT
    save(img, "item", "nero_coil")


def gen_fusion_cell():
    rng = rng_for("fusion_cell")
    img = new_img()
    px = img.load()
    for y in range(5, 27):                      # canister shell
        for x in range(11, 21):
            px[x, y] = rng.choice(ALLOY)
    for (cy0, cy1) in ((5, 7), (24, 26)):       # end caps
        for y in range(cy0, cy1 + 1):
            for x in range(11, 21):
                px[x, y] = ALLOY[2]
        for x in range(11, 21):
            px[x, cy0] = A_LIGHT if cy0 == 5 else ALLOY[3]
            px[x, cy1] = A_DARK
    for y in range(5, 27):                      # shell shading
        px[11, y] = _mix(ALLOY[3], A_LIGHT, 0.4)
        px[20, y] = A_DARK
    for y in range(9, 23):                      # plasma window
        for x in range(13, 19):
            d = math.hypot(x - 15.5, y - 15.5)
            if d <= 1.5:
                px[x, y] = T_GLOW
            elif d <= 3.0:
                px[x, y] = T_PLASMA
            elif d <= 5.0:
                px[x, y] = T_CYAN
            elif d <= 7.0:
                px[x, y] = T_TEAL
            else:
                px[x, y] = T_DEEP
    for y in range(9, 23):                      # window frame
        px[12, y] = A_DARK
        px[19, y] = A_DARK
    for x in range(12, 20):
        px[x, 8] = A_DARK
        px[x, 23] = A_DARK
    save(img, "item", "fusion_cell")


def gen_plasma_cell():
    """Tier-2 fuel: the Fusion Cell canister recipe, one step brighter up the plasma ramp."""
    rng = rng_for("plasma_cell")
    img = new_img()
    px = img.load()
    for y in range(5, 27):                      # canister shell
        for x in range(11, 21):
            px[x, y] = rng.choice(ALLOY)
    for (cy0, cy1) in ((5, 7), (24, 26)):       # alloy end caps
        for y in range(cy0, cy1 + 1):
            for x in range(11, 21):
                px[x, y] = ALLOY[2]
        for x in range(11, 21):
            px[x, cy0] = A_LIGHT if cy0 == 5 else ALLOY[3]
            px[x, cy1] = A_DARK
    for y in range(5, 27):                      # shell shading
        px[11, y] = _mix(ALLOY[3], A_LIGHT, 0.4)
        px[20, y] = A_DARK
    for y in range(9, 23):                      # plasma window — brighter core than fusion_cell
        for x in range(13, 19):
            d = math.hypot(x - 15.5, y - 15.5)
            if d <= 2.5:
                px[x, y] = T_GLOW
            elif d <= 4.0:
                px[x, y] = T_PLASMA
            elif d <= 5.5:
                px[x, y] = T_CYAN
            elif d <= 7.0:
                px[x, y] = T_CYAN if (x + y) % 2 == 0 else T_TEAL
            else:
                px[x, y] = T_TEAL
    for y in range(9, 23):                      # window frame
        px[12, y] = A_DARK
        px[19, y] = A_DARK
    for x in range(12, 20):
        px[x, 8] = A_DARK
        px[x, 23] = A_DARK
    px[15, 6] = T_CYAN                          # tier studs on the top cap
    px[16, 6] = T_CYAN
    save(img, "item", "plasma_cell")


def gen_stellar_cell():
    """Tier-3 fuel: near-white T_GLOW core behind DOUBLE containment rings + a small hazard
    tick on the top cap (the only hazard pixels outside the reactor shell / configurator)."""
    rng = rng_for("stellar_cell")
    img = new_img()
    px = img.load()
    for y in range(5, 27):                      # canister shell
        for x in range(11, 21):
            px[x, y] = rng.choice(ALLOY)
    for (cy0, cy1) in ((5, 7), (24, 26)):       # alloy end caps
        for y in range(cy0, cy1 + 1):
            for x in range(11, 21):
                px[x, y] = ALLOY[2]
        for x in range(11, 21):
            px[x, cy0] = A_LIGHT if cy0 == 5 else ALLOY[3]
            px[x, cy1] = A_DARK
    for y in range(5, 27):                      # shell shading
        px[11, y] = _mix(ALLOY[3], A_LIGHT, 0.4)
        px[20, y] = A_DARK
    for y in range(9, 23):                      # window — near-white stellar core
        for x in range(13, 19):
            d = math.hypot(x - 15.5, y - 15.5)
            if d <= 3.0:
                px[x, y] = T_GLOW
            elif d <= 4.5:
                px[x, y] = T_GLOW if (x + y) % 2 == 0 else T_PLASMA
            elif d <= 6.0:
                px[x, y] = T_PLASMA
            else:
                px[x, y] = T_CYAN
    for y in range(9, 23):                      # window frame
        px[12, y] = A_DARK
        px[19, y] = A_DARK
    for x in range(12, 20):
        px[x, 8] = A_DARK
        px[x, 23] = A_DARK
    for ry in (12, 19):                         # double containment rings over the window
        for x in range(12, 20):
            px[x, ry] = A_LIGHT if x % 2 == 0 else ALLOY[3]
    for i, tx in enumerate(range(13, 17)):      # small hazard tick on the top cap
        px[tx, 6] = HAZ_Y if i % 2 == 0 else HAZ_K
    save(img, "item", "stellar_cell")


def _filter_body(name, pleat_ramp):
    """Shared filter-item silhouette: pleated medium in an alloy frame. The clean cartridge gets
    a small teal service tick; the dirty one is fouled grey-brown with NO teal (MODELS.md: teal
    marks live emissives, and a spent filter is inert)."""
    rng = rng_for(name)
    img = new_img()
    px = img.load()
    x0, y0, x1, y1 = 7, 5, 24, 26
    for y in range(y0, y1 + 1):                 # alloy frame
        for x in range(x0, x1 + 1):
            px[x, y] = rng.choice(ALLOY)
    for x in range(x0, x1 + 1):
        px[x, y0] = A_LIGHT
        px[x, y1] = A_DARK
    for y in range(y0, y1 + 1):
        px[x0, y] = A_LIGHT
        px[x1, y] = A_DARK
    rivets(img, ((x0 + 1, y0 + 1), (x1 - 2, y0 + 1), (x0 + 1, y1 - 2), (x1 - 2, y1 - 2)))
    for y in range(y0 + 3, y1 - 2):             # pleated medium: vertical accordion folds
        for x in range(x0 + 3, x1 - 2):
            k = (x - x0 - 3) % 4
            if k == 0:
                col = pleat_ramp[2]             # fold crest
            elif k == 3:
                col = pleat_ramp[1]             # fold valley
            else:
                col = pleat_ramp[0]
            if y >= y1 - 5 and rng.random() < 0.3:
                col = pleat_ramp[1]             # settle shadow at the base
            px[x, y] = col
    return img, px


def gen_filter_cartridge():
    # white/light pleats + the small teal service tick on the frame
    pleats = ((214, 220, 226, 255), (188, 196, 205, 255), (236, 240, 244, 255))
    img, px = _filter_body("filter_cartridge", pleats)
    px[9, 6] = T_CYAN                           # teal service tick (top-left frame)
    px[10, 6] = T_CYAN
    px[9, 7] = T_TEAL
    save(img, "item", "filter_cartridge")


def gen_dirty_filter():
    # same silhouette, fouled grey-brown pleats, grime blotches, no teal anywhere
    pleats = ((110, 100, 88, 255), (84, 76, 64, 255), (134, 122, 106, 255))
    img, px = _filter_body("dirty_filter", pleats)
    rng = rng_for("dirty_filter_grime")
    for _ in range(26):                         # grime blotches over the pleats
        gx = rng.randrange(10, 22)
        gy = rng.randrange(9, 23)
        px[gx, gy] = (58, 52, 44, 255)
        if rng.random() < 0.5:
            px[min(gx + 1, 21), gy] = (70, 62, 52, 255)
    save(img, "item", "dirty_filter")


# Upgrade modules: shared alloy card + dark glyph panel; the glyph is the identity.

def _glyph_speed(px):
    for (ax, c) in ((11, T_CYAN), (15, T_PLASMA)):   # double chevron >>
        for i in range(4):
            px[ax + i, 11 + i] = c
            px[ax + i, 18 - i] = c
        px[ax + 3, 14] = c
        px[ax + 3, 15] = c


def _glyph_bolt(px):
    bolt = [(17, 9), (16, 10), (15, 11), (14, 12), (15, 12), (16, 12), (17, 12),
            (16, 13), (15, 14), (14, 15), (13, 16)]
    for (bx, by) in bolt:
        px[bx, by] = T_CYAN
    px[17, 9] = T_PLASMA
    px[13, 16] = T_PLASMA


def _glyph_bars(px):
    for (by, c) in ((11, T_CYAN), (14, T_TEAL), (17, T_DEEP)):
        for x in range(12, 20):
            px[x, by] = c
            px[x, by + 1] = c


def _glyph_rings(px):
    for y in range(9, 21):
        for x in range(10, 22):
            d = math.hypot(x - 15.5, y - 14.5)
            if d < 1.0:
                px[x, y] = T_PLASMA
            elif abs(d - 2.4) < 0.6:
                px[x, y] = T_CYAN
            elif abs(d - 4.8) < 0.6:
                px[x, y] = T_TEAL


def gen_module(name, glyph):
    rng = rng_for(name)
    img = new_img()
    px = img.load()
    x0, y0, x1, y1 = 7, 5, 24, 26
    for y in range(y0, y1 + 1):                 # card body
        for x in range(x0, x1 + 1):
            px[x, y] = rng.choice(ALLOY)
    for x in range(x0, x1 + 1):
        px[x, y0] = A_LIGHT
        px[x, y1] = A_DARK
    for y in range(y0, y1 + 1):
        px[x0, y] = A_LIGHT
        px[x1, y] = A_DARK
    for x in range(x0 + 2, x1 - 1, 3):          # connector pins
        px[x, y1 + 1] = A_LIGHT
        px[x + 1, y1 + 1] = _mix(A_LIGHT, A_DARK, 0.5)
    for y in range(y0 + 2, y1 - 1, 4):          # edge traces
        px[x0 + 2, y] = T_DEEP
        px[x1 - 2, y] = T_DEEP
    for y in range(9, 21):                      # glyph panel
        for x in range(10, 22):
            px[x, y] = (12, 18, 22, 255)
    glyph(px)
    px[x1, y0] = CLEAR                          # keying notch
    px[x1 - 1, y0] = A_DARK
    save(img, "item", name)


def gen_configurator():
    """The Configurator wrench: alloy body, teal plasma tip at the jaws, thin hazard band on
    the grip (the only hazard-striped item — MODELS.md)."""
    img = new_img()
    px = img.load()

    def put(x, y, c):
        if 0 <= x < S and 0 <= y < S:
            px[x, y] = c

    n = 26
    for i in range(n + 1):                      # handle outline (dark)
        t = i / n
        xi = int(round(7 + 11 * t))
        yi = int(round(26 - 14 * t))
        for dy in range(-2, 3):
            for dx in range(-2, 3):
                if abs(dx) + abs(dy) <= 2:
                    put(xi + dx, yi + dy, A_DARK)
    for i in range(n + 1):                      # handle core + highlight
        t = i / n
        xi = int(round(7 + 11 * t))
        yi = int(round(26 - 14 * t))
        for dy in (-1, 0, 1):
            for dx in (-1, 0, 1):
                if abs(dx) + abs(dy) <= 1:
                    put(xi + dx, yi + dy, ALLOY[3])
        put(xi - 1, yi - 1, A_LIGHT)
    for i in range(n + 1):                      # thin hazard band on the grip
        t = i / n
        if 0.12 <= t <= 0.34:
            xi = int(round(7 + 11 * t))
            yi = int(round(26 - 14 * t))
            band = HAZ_Y if (i // 2) % 2 == 0 else HAZ_K
            for dy in (-1, 0, 1):
                for dx in (-1, 0, 1):
                    if abs(dx) + abs(dy) <= 1:
                        put(xi + dx, yi + dy, band)
    hx0, hy0 = 21.5, 8.5                        # open-end head, jaws opening up-right
    for y in range(2, 17):
        for x in range(14, 30):
            d = math.hypot(x - hx0, y - hy0)
            if not (2.4 <= d <= 5.6):
                continue
            ang = math.atan2(y - hy0, x - hx0)
            gap = abs(_angdiff(ang, -math.pi / 4))
            if gap < 0.55:
                continue                        # the jaw opening
            if gap < 0.95:
                put(x, y, T_PLASMA if d < 4.6 else T_CYAN)   # energised jaw tips
            elif d < 3.2:
                put(x, y, A_DARK)
            elif d > 5.0:
                put(x, y, A_LIGHT if (x + y) < 30 else A_DARK)
            else:
                put(x, y, ALLOY[3] if (x + y) % 2 == 0 else ALLOY[1])
    put(25, 5, T_GLOW)                          # plasma arc in the gap
    put(26, 4, T_PLASMA)
    put(24, 6, T_PLASMA)
    save(img, "item", "configurator")


# ---------------- main ----------------

def check_coverage():
    """Flag any pre-existing PNG no painter owns — after a --force run NOTHING may be left on
    the old 16x art, and every model-referenced name must resolve."""
    clean = True
    for folder, d in (("block", BLOCK_DIR), ("item", ITEM_DIR)):
        existing = {f for f in os.listdir(d) if f.endswith(".png")}
        orphans = sorted(existing - PAINTED[folder])
        if orphans:
            clean = False
            print("NOTICE: existing %s textures WITHOUT a painter (left untouched): %s"
                  % (folder, ", ".join(orphans)))
    if clean:
        print("coverage: every existing block/item PNG has a painter.")


def main():
    # machine triads: <name>.png (sides) + <name>_front.png + <name>_top.png
    gen_nero_generator()
    gen_solar_array()
    gen_ore_processor(False)
    gen_ore_processor(True)
    gen_fabricator(False)
    gen_fabricator(True)
    gen_fusion_reactor()
    gen_fusion_casing()
    gen_fusion_containment_glass()
    gen_auto_crafter()
    gen_item_sorter()
    gen_scrubber()
    gen_remediator()
    gen_analytics_terminal()
    # BER sprites
    gen_ber_sprites()
    # items
    gen_dust("iron_dust", IRON)
    gen_dust("copper_dust", COPPER)
    gen_dust("gold_dust", GOLD)
    gen_circuit_board()
    gen_machine_frame()
    gen_nero_coil()
    gen_fusion_cell()
    gen_plasma_cell()
    gen_stellar_cell()
    gen_filter_cartridge()
    gen_dirty_filter()
    gen_module("speed_module", _glyph_speed)
    gen_module("efficiency_module", _glyph_bolt)
    gen_module("capacity_module", _glyph_bars)
    gen_module("range_module", _glyph_rings)
    gen_configurator()
    check_coverage()


if __name__ == "__main__":
    main()
