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
GUI_DIR = os.path.join(ROOT, "src/main/resources/assets/nerotech/textures/gui")
os.makedirs(BLOCK_DIR, exist_ok=True)
os.makedirs(ITEM_DIR, exist_ok=True)
os.makedirs(GUI_DIR, exist_ok=True)

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
PAINTED = {"block": set(), "item": set(), "gui": set()}


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
    d = {"block": BLOCK_DIR, "item": ITEM_DIR, "gui": GUI_DIR}[folder]
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


def _accelerator_coil_face():
    # Particle Collider ring segment: alloy plate wrapped in magnet windings, with the teal beam
    # channel running straight through the middle so a placed ring reads as one continuous loop.
    # No hazard striping — that stays on the Fusion Reactor shell.
    img = machine_base("accelerator_coil")
    px = img.load()
    for y in range(6, 26):                      # winding bands down the plate
        band = ((y - 6) // 2) % 2 == 0
        for x in range(3, 29):
            if y < 13 or y > 18:
                px[x, y] = ALLOY[3] if band else A_DARK
    for x in range(3, 29):                      # coil clamps top and bottom of the winding stack
        px[x, 5] = A_LIGHT
        px[x, 26] = A_DARK
    for y in range(13, 19):                     # the beam channel (PULSE target — T_* only)
        for x in range(S):
            d = abs(y - 15.5)
            px[x, y] = T_DEEP if d > 2.0 else (T_TEAL if d > 1.0 else T_CYAN)
    for x in range(2, 30, 6):                   # beam pips along the channel
        px[x, 15] = T_PLASMA
        px[x, 16] = T_PLASMA
    rivets(img, ((7, 8), (24, 8), (7, 23), (24, 23)))
    return img


def gen_accelerator_coil():
    save(_accelerator_coil_face(), "block", "accelerator_coil")


# ---- guide-coil direction indicators (24 top faces: 3 bends x 8 headings) ----
#
# The Accelerator Controller writes each traced guide's OUTGOING heading into its `heading`
# blockstate, and these are the top faces that blockstate picks. Each is the plain coil face with a
# beam-cyan arrow composited on: the arrow ENTERS along the incoming heading, kinks 45 degrees at the
# coil's centre (where the magnet actually bends the beam) and LEAVES pointing at the named heading.
# Only the north-facing variant of each bend is painted; the other seven are that overlay rotated in
# 45-degree steps (NEAREST, so the pixel art stays hard-edged and the alpha stays binary).

# Clockwise from north — the same order as AcceleratorMath.Heading, so index == rotation step.
ARROW_HEADINGS = ("north", "north_east", "east", "south_east", "south",
                  "south_west", "west", "north_west")

# Overlay canvas: 2x the tile, tile inset at [16, 48), so a stroke that runs off the tile edge is
# still on-canvas after a 45-degree rotation and crops back to a clean edge-to-edge line.
_OV = 2 * S
_OV_OFF = S // 2
_OV_MID = _OV_OFF + (S - 1) / 2.0          # the tile's centre in canvas coordinates


def _arrow_stroke(px, x0, y0, x1, y1, width, col):
    """Stamp a `width`-thick line of `col` on the (canvas-sized) overlay — chunky pixel art, no AA."""
    span = max(abs(x1 - x0), abs(y1 - y0))
    steps = int(span * 3) + 1
    lo = -(width // 2)
    hi = width + lo
    for i in range(steps + 1):
        t = i / steps
        cx = int(round(x0 + (x1 - x0) * t))
        cy = int(round(y0 + (y1 - y0) * t))
        for dy in range(lo, hi):
            for dx in range(lo, hi):
                x, y = cx + dx, cy + dy
                if 0 <= x < _OV and 0 <= y < _OV:
                    px[x, y] = col


def _arrow_overlay(bend):
    """The north-pointing arrow for one bend, drawn on the oversized overlay canvas.

    The incoming leg matches AcceleratorMath.Heading exactly: a LEFT guide leaving north was entered
    on NORTH_EAST (up-and-right, so it enters at the lower LEFT), a RIGHT guide on NORTH_WEST.
    """
    ov = Image.new("RGBA", (_OV, _OV), CLEAR)
    px = ov.load()
    o = _OV_OFF
    mid = _OV_MID
    # entry leg -> the coil centre, then the exit leg north to the head.
    entry = {"straight": (o + 15.5, o + 38.0),
             "left": (o - 6.0, o + 37.0),
             "right": (o + 37.0, o + 37.0)}[bend]
    legs = [(entry[0], entry[1], mid, mid), (mid, mid, o + 15.5, o + 6.0)]
    head = [(o + 15.5, o + 4.0, o + 8.5, o + 12.0), (o + 15.5, o + 4.0, o + 22.5, o + 12.0)]
    for (width, col) in ((6, T_DEEP), (4, T_CYAN), (2, T_PLASMA)):
        for (x0, y0, x1, y1) in legs + head:
            _arrow_stroke(px, x0, y0, x1, y1, width, col)
    return ov


def gen_accelerator_coil_indicators():
    base = _accelerator_coil_face()
    for bend in ("straight", "left", "right"):
        overlay = _arrow_overlay(bend)
        for (step, heading) in enumerate(ARROW_HEADINGS):
            # PIL rotates anticlockwise; the headings run clockwise, hence the negative angle.
            spun = overlay.rotate(-45 * step, resample=Image.NEAREST, center=(_OV_MID, _OV_MID))
            img = base.copy()
            img.alpha_composite(spun.crop((_OV_OFF, _OV_OFF, _OV_OFF + S, _OV_OFF + S)))
            save(img, "block", "accelerator_coil_top_%s_%s" % (bend, heading))


def gen_collider_core():
    # side: heavy injector housing with the beam channel continuing the coils' teal line
    img = machine_base("collider_core")
    px = img.load()
    for y in (7, 24):                           # housing plate seams
        for x in range(3, 29):
            px[x, y] = A_DARK
            px[x, y + 1] = ALLOY[2]
    for y in range(13, 19):                     # beam channel through-line (matches the coil)
        for x in range(S):
            d = abs(y - 15.5)
            px[x, y] = T_DEEP if d > 2.0 else (T_TEAL if d > 1.0 else T_CYAN)
    for x in (5, 15, 26):                       # injector taps straddling the channel
        for y in (10, 21):
            led(px, x, y, T_TEAL)
    save(img, "block", "collider_core")

    # front: the injection aperture — a bright beam eye ringed by focusing magnets (PULSE target)
    img = machine_base("collider_core_front")
    px = img.load()
    recess(px, 4, 4, 27, 27, (10, 13, 16, 255))
    for y in range(S):
        for x in range(S):
            d = math.hypot(x - 15.5, y - 15.5)
            if d <= 2.2:
                px[x, y] = T_GLOW
            elif d <= 4.0:
                px[x, y] = T_PLASMA
            elif d <= 6.0:
                px[x, y] = T_CYAN if (x + y) % 3 else T_PLASMA
            elif d <= 7.6:
                px[x, y] = T_TEAL
            elif d <= 9.0:
                px[x, y] = T_DEEP
    for k in range(8):                          # focusing magnets around the aperture
        ang = k * math.pi / 4
        mx = int(round(15.5 + 10.6 * math.cos(ang)))
        my = int(round(15.5 + 10.6 * math.sin(ang)))
        for yy in range(my - 1, my + 2):
            for xx in range(mx - 1, mx + 2):
                px[xx, yy] = ALLOY[3]
        px[mx, my] = A_LIGHT
    save(img, "block", "collider_core_front")

    # top: quadrant access plates around a teal beam crossing (the ring seen from above)
    img = machine_base("collider_core_top")
    px = img.load()
    for x in range(4, 28):                      # crossing beam lines
        px[x, 15] = T_DEEP if x % 4 < 2 else T_TEAL
        px[x, 16] = T_TEAL if x % 4 < 2 else T_DEEP
    for y in range(4, 28):
        px[15, y] = T_DEEP if y % 4 < 2 else T_TEAL
        px[16, y] = T_TEAL if y % 4 < 2 else T_DEEP
    for (qx, qy) in ((6, 6), (19, 6), (6, 19), (19, 19)):   # access plates in the quadrants
        for y in range(qy, qy + 7):
            for x in range(qx, qx + 7):
                px[x, y] = ALLOY[2] if (x + y) % 2 == 0 else ALLOY[0]
        px[qx, qy] = A_LIGHT
        px[qx + 6, qy + 6] = A_DARK
    for (cx, cy) in ((15, 15), (16, 16)):       # the crossing hub
        px[cx, cy] = T_GLOW
    save(img, "block", "collider_core_top")


def gen_electrolyzer():
    # side: paired electrode stacks flanking a tall bright cell column
    img = machine_base("electrolyzer")
    px = img.load()
    for cx in (6, 23):                          # electrode busbars
        for y in range(6, 26):
            px[cx, y] = ALLOY[3]
            px[cx + 1, y] = A_DARK
        px[cx, 6] = A_LIGHT
    recess(px, 12, 5, 18, 26, (10, 14, 18, 255))
    for y in range(6, 26):                      # the cell column (PULSE target — T_* only)
        band = (y // 3) % 2 == 0
        for x in range(13, 18):
            px[x, y] = T_TEAL if band else T_DEEP
    for y in range(8, 25, 4):                   # bubbles rising through the cell
        px[15, y] = T_PLASMA
        px[14, y + 1] = T_CYAN
    save(img, "block", "electrolyzer")

    # front: the split cell — hydrogen side pale, oxygen side teal, split by the membrane
    img = machine_base("electrolyzer_front")
    px = img.load()
    recess(px, 4, 4, 27, 27, (10, 14, 18, 255))
    for y in range(6, 26):
        for x in range(6, 15):                  # H2 half (pale plasma)
            px[x, y] = T_PLASMA if (x + y) % 5 == 0 else T_CYAN
        for x in range(18, 26):                 # O2 half (teal)
            px[x, y] = T_CYAN if (x + y) % 5 == 0 else T_TEAL
    for y in range(5, 27):                      # the membrane between them
        px[15, y] = A_LIGHT
        px[16, y] = A_DARK
    for x in (9, 22):                           # collection taps at the top of each half
        led(px, x, 7, T_GLOW)
    save(img, "block", "electrolyzer_front")

    # top: two gas take-off ports either side of the water inlet
    img = machine_base("electrolyzer_top")
    px = img.load()
    recess(px, 12, 12, 19, 19, (10, 14, 18, 255))
    for y in range(13, 19):                     # water inlet
        for x in range(13, 19):
            px[x, y] = T_DEEP if (x + y) % 2 else T_TEAL
    for (ox, oy) in ((6, 6), (23, 6), (6, 23), (23, 23)):
        led(px, ox, oy, T_CYAN)
    for x in range(4, 28):                      # manifold run across the deck
        px[x, 15] = ALLOY[3]
        px[x, 16] = A_DARK
    save(img, "block", "electrolyzer_top")


def gen_gas_turbine():
    # side: gas feed ducting running into a stubby combustion drum
    img = machine_base("gas_turbine")
    px = img.load()
    for y in range(9, 23):                      # drum body
        for x in range(4, 28):
            px[x, y] = ALLOY[1] if (y // 2) % 2 == 0 else ALLOY[2]
    for x in range(4, 28):                      # drum lips
        px[x, 9] = A_LIGHT
        px[x, 22] = A_DARK
    for y in range(14, 18):                     # the gas feed line (PULSE target)
        for x in range(S):
            px[x, y] = T_TEAL if y in (15, 16) else T_DEEP
    for x in range(5, 28, 7):                   # feed pips
        px[x, 15] = T_PLASMA
    rivets(img, ((6, 11), (25, 11), (6, 20), (25, 20)))
    save(img, "block", "gas_turbine")

    # front: the rotor — swept blades around a bright hub (PULSE target)
    img = machine_base("gas_turbine_front")
    px = img.load()
    recess(px, 4, 4, 27, 27, (10, 13, 16, 255))
    for y in range(S):
        for x in range(S):
            dx = x - 15.5
            dy = y - 15.5
            d = math.hypot(dx, dy)
            if d > 11.5 or d < 1.5:
                continue
            ang = math.atan2(dy, dx) + d * 0.35   # sweep the blades with radius
            blade = int((ang / (2 * math.pi) * 6) % 2) == 0
            px[x, y] = T_TEAL if blade else T_DEEP
    for y in range(14, 18):                     # the hub
        for x in range(14, 18):
            px[x, y] = T_CYAN
    px[15, 15] = T_GLOW
    px[16, 16] = T_GLOW
    save(img, "block", "gas_turbine_front")

    # top: exhaust stack ring + intake grille
    img = machine_base("gas_turbine_top")
    px = img.load()
    for y in range(S):
        for x in range(S):
            d = math.hypot(x - 15.5, y - 15.5)
            if 6.0 <= d <= 9.5:
                px[x, y] = ALLOY[3] if (x + y) % 2 == 0 else A_DARK
            elif d < 5.0:
                px[x, y] = T_DEEP if d > 3.0 else T_TEAL
    px[15, 15] = T_CYAN
    px[16, 16] = T_CYAN
    for (gx, gy) in ((5, 5), (24, 5), (5, 24), (24, 24)):
        led(px, gx, gy, T_TEAL)
    save(img, "block", "gas_turbine_top")


def gen_chemical_processor():
    # side: reagent pipework running down into a wash vessel
    img = machine_base("chemical_processor")
    px = img.load()
    for cx in (8, 22):                          # standpipes
        for y in range(4, 20):
            px[cx, y] = ALLOY[3]
            px[cx + 1, y] = A_DARK
    for x in range(6, 26):                      # vessel shoulder + belly
        px[x, 20] = A_LIGHT
        px[x, 21] = ALLOY[3]
    for y in range(22, 28):
        for x in range(6, 26):
            px[x, y] = ALLOY[2] if (x // 2) % 2 == 0 else ALLOY[0]
    for y in range(10, 19, 4):                  # reagent flow pips down the pipes
        px[8, y] = T_CYAN
        px[22, y] = T_CYAN
    save(img, "block", "chemical_processor")

    # front: the wash drum window — swirling oxygen wash over a dark charge (PULSE target)
    img = machine_base("chemical_processor_front")
    px = img.load()
    recess(px, 5, 5, 26, 26, (10, 14, 18, 255))
    for y in range(6, 26):
        for x in range(6, 26):
            dx = x - 15.5
            dy = y - 15.5
            d = math.hypot(dx, dy)
            if d > 9.6:
                continue
            swirl = math.atan2(dy, dx) * 2.0 + d * 0.9
            band = int(swirl / 1.2) % 2 == 0
            if d < 3.4:
                px[x, y] = T_GLOW if band else T_PLASMA
            else:
                px[x, y] = T_CYAN if band else T_TEAL
    for k in range(4):                          # spray heads around the drum
        ang = math.pi / 4 + k * math.pi / 2
        sx = int(round(15.5 + 11.0 * math.cos(ang)))
        sy = int(round(15.5 + 11.0 * math.sin(ang)))
        px[sx, sy] = A_LIGHT
    save(img, "block", "chemical_processor_front")

    # top: reagent inlet cluster over quadrant plates
    img = machine_base("chemical_processor_top")
    px = img.load()
    for (qx, qy) in ((4, 4), (18, 4), (4, 18), (18, 18)):
        for y in range(qy, qy + 10):
            for x in range(qx, qx + 10):
                px[x, y] = ALLOY[2] if (x + y) % 3 else ALLOY[0]
    recess(px, 13, 13, 18, 18, (10, 14, 18, 255))
    for y in range(14, 18):
        for x in range(14, 18):
            px[x, y] = T_TEAL
    px[15, 15] = T_PLASMA
    for (ix, iy) in ((9, 15), (22, 15), (15, 9), (15, 22)):
        led(px, ix, iy, T_CYAN)
    save(img, "block", "chemical_processor_top")


def gen_coolant_pump():
    # side: pump volute with the coolant return line running low
    img = machine_base("coolant_pump")
    px = img.load()
    for y in range(S):
        for x in range(S):
            d = math.hypot(x - 15.5, y - 13.5)
            if d <= 8.5:
                px[x, y] = ALLOY[3] if d > 6.5 else ALLOY[1]
    for y in range(24, 28):                     # the return line (PULSE target — T_* only)
        for x in range(2, 30):
            px[x, y] = T_TEAL if y in (25, 26) else T_DEEP
    for x in range(4, 29, 6):
        px[x, 25] = T_CYAN
    for k in range(6):                          # volute bolts
        ang = k * math.pi / 3
        bx = int(round(15.5 + 7.4 * math.cos(ang)))
        by = int(round(13.5 + 7.4 * math.sin(ang)))
        px[bx, by] = A_LIGHT
    save(img, "block", "coolant_pump")

    # front: the impeller eye (PULSE target)
    img = machine_base("coolant_pump_front")
    px = img.load()
    recess(px, 5, 5, 26, 26, (10, 14, 18, 255))
    for y in range(S):
        for x in range(S):
            dx = x - 15.5
            dy = y - 15.5
            d = math.hypot(dx, dy)
            if d > 10.2:
                continue
            vane = int(((math.atan2(dy, dx) + d * 0.28) / (2 * math.pi) * 8) % 2) == 0
            if d < 2.6:
                px[x, y] = T_GLOW
            else:
                px[x, y] = T_CYAN if vane else T_DEEP
    save(img, "block", "coolant_pump_front")

    # top: inlet/outlet manifold with flow arrows in teal
    img = machine_base("coolant_pump_top")
    px = img.load()
    for y in range(6, 26):                      # the two manifold runs
        px[9, y] = T_DEEP
        px[10, y] = T_TEAL
        px[21, y] = T_TEAL
        px[22, y] = T_DEEP
    for y in range(7, 25, 5):                   # flow pips, opposite directions
        px[10, y] = T_CYAN
        px[21, 31 - y] = T_CYAN
    for x in range(12, 20):                     # cross-over plate
        for y in range(13, 19):
            px[x, y] = ALLOY[2] if (x + y) % 2 else ALLOY[0]
    led(px, 15, 15, T_PLASMA)
    save(img, "block", "coolant_pump_top")


def gen_radiator():
    # A stack of cooling fins between two header rails — the same face on all six sides, so a
    # radiator run reads as one continuous block. Cold end of the ramp only (never H_*).
    img = machine_base("radiator")
    px = img.load()
    for y in range(4, 28):
        fin = (y // 2) % 2 == 0
        for x in range(3, 29):
            px[x, y] = A_LIGHT if fin else ALLOY[2]
    for x in range(2, 30):                      # header rails top and bottom
        px[x, 3] = A_DARK
        px[x, 28] = A_DARK
    for cy in (7, 15, 23):                      # coolant channels showing between the fins
        for x in range(3, 29):
            px[x, cy] = T_TEAL if (x // 3) % 2 == 0 else T_DEEP
    for x in range(5, 29, 8):
        px[x, 15] = T_CYAN
    rivets(img, ((4, 4), (27, 4), (4, 27), (27, 27)))
    save(img, "block", "radiator")


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


# ---------------- Stage D: power tech ----------------

def gen_wind_turbine():
    # side: the tower — a tall alloy mast column with guy-wire ticks either side
    img = machine_base("wind_turbine")
    px = img.load()
    for y in range(3, 29):                      # the mast
        for x in range(12, 20):
            px[x, y] = ALLOY[3] if (x + y) % 3 else ALLOY[1]
        px[12, y] = A_LIGHT
        px[19, y] = A_DARK
    for y in range(6, 28, 5):                   # ladder rungs up the mast
        for x in range(13, 19):
            px[x, y] = A_DARK
    for y in range(7, 27, 4):                   # guy-wire anchor ticks
        px[7, y] = A_LIGHT
        px[8, y] = A_DARK
        px[23, y] = A_LIGHT
        px[24, y] = A_DARK
    led(px, 15, 25, T_TEAL)
    save(img, "block", "wind_turbine")

    # front: the three-blade rotor sweeping around a bright hub (PULSE target — T_* only)
    img = machine_base("wind_turbine_front")
    px = img.load()
    recess(px, 4, 4, 27, 27, (10, 13, 16, 255))
    for y in range(5, 27):
        for x in range(5, 27):
            dx = x - 15.5
            dy = y - 15.5
            d = math.hypot(dx, dy)
            if d > 11.0 or d < 2.2:
                continue
            a = ((math.atan2(dy, dx) + math.pi) / (2 * math.pi) * 3.0) % 1.0
            if a < 0.17:                        # three blades, cyan on the leading edge
                px[x, y] = T_CYAN if a < 0.04 else T_TEAL
    for y in range(14, 18):                     # the rotor hub
        for x in range(14, 18):
            px[x, y] = T_PLASMA
    px[15, 15] = T_GLOW
    px[16, 16] = T_GLOW
    save(img, "block", "wind_turbine_front")

    # top: the nacelle deck — spinner boss up front, airflow chevrons trailing aft
    img = machine_base("wind_turbine_top")
    px = img.load()
    for y in range(11, 21):                     # the nacelle running fore-aft
        for x in range(3, 29):
            px[x, y] = ALLOY[1] if (y // 2) % 2 == 0 else ALLOY[2]
    for x in range(3, 29):
        px[x, 11] = A_LIGHT
        px[x, 20] = A_DARK
    for y in range(10, 22):                     # the spinner boss
        for x in range(4, 13):
            d = math.hypot(x - 8.0, y - 15.5)
            if d <= 2.0:
                px[x, y] = T_GLOW if d <= 1.0 else T_CYAN
            elif d <= 3.6:
                px[x, y] = T_DEEP
    for cx in (16, 20, 24):                     # airflow chevrons in the wake
        for i in range(4):
            px[cx + i, 13 + i] = T_TEAL
            px[cx + i, 18 - i] = T_TEAL
    save(img, "block", "wind_turbine_top")


def gen_geothermal_generator():
    # side: a recessed bore-pipe of stacked heat bands rising from the bedrock below
    img = machine_base("geothermal_generator")
    px = img.load()
    recess(px, 12, 6, 19, 28, (10, 14, 18, 255))
    for y in range(7, 29):                      # the bore column (PULSE target — T_* only)
        band = ((28 - y) // 3) % 2 == 0
        for x in range(13, 19):
            px[x, y] = T_TEAL if band else T_DEEP
    for y in range(10, 28, 5):                  # heat pulses climbing the bore
        px[15, y] = T_CYAN
        px[16, y - 1] = T_PLASMA
    for cy in (8, 15, 22):                      # pipe clamps
        px[11, cy] = A_LIGHT
        px[20, cy] = A_DARK
    for x in range(4, 12):                      # wellhead plate beside the bore
        px[x, 12] = A_DARK
        px[x, 13] = ALLOY[2]
    save(img, "block", "geothermal_generator")

    # front: a grate over rising heat plumes — teal at the floor, plasma at the crown
    img = machine_base("geothermal_generator_front")
    px = img.load()
    recess(px, 5, 9, 26, 27, (10, 13, 16, 255))
    for y in range(10, 27):
        t = (26 - y) / 16.0
        col = T_TEAL if t < 0.34 else (T_CYAN if t < 0.7 else T_PLASMA)
        for x in range(7, 25):
            px[x, y] = col if (x + y) % 3 == 0 else T_DEEP
    for y in (12, 16, 20, 24):                  # grate bars across the plumes
        for x in range(7, 25):
            px[x, y] = A_DARK if x % 2 == 0 else ALLOY[2]
    led(px, 8, 5)
    led(px, 22, 5, T_CYAN, T_PLASMA)
    save(img, "block", "geothermal_generator_front")

    # top: the capped wellhead ring with pipe runs radiating to the block edges
    img = machine_base("geothermal_generator_top")
    px = img.load()
    for i in range(6, 13):                      # radiating pipe runs, N/S/E/W
        px[15 + i, 15] = ALLOY[3]
        px[15 + i, 16] = A_DARK
        px[15 - i, 15] = ALLOY[3]
        px[15 - i, 16] = A_DARK
        px[15, 15 + i] = ALLOY[3]
        px[16, 15 + i] = A_DARK
        px[15, 15 - i] = ALLOY[3]
        px[16, 15 - i] = A_DARK
    for y in range(9, 23):
        for x in range(9, 23):
            d = math.hypot(x - 15.5, y - 15.5)
            if d <= 3.0:
                px[x, y] = ALLOY[2] if (x + y) % 2 == 0 else ALLOY[0]   # the cap
            elif d <= 4.6:
                px[x, y] = T_DEEP if (x + y) % 3 else T_TEAL            # wellhead ring
            elif d <= 6.0:
                px[x, y] = A_DARK
    for k in range(4):                          # wellhead bolts on the cap
        ang = k * math.pi / 2 + math.pi / 4
        px[int(round(15.5 + 5.4 * math.cos(ang))),
           int(round(15.5 + 5.4 * math.sin(ang)))] = A_LIGHT
    px[15, 15] = T_PLASMA
    save(img, "block", "geothermal_generator_top")


def gen_bio_generator():
    # side: a hopper wedge funnelling feedstock down into the combustion slot
    img = machine_base("bio_generator")
    px = img.load()
    for i in range(9):                          # the wedge narrows as it descends
        y = 6 + i
        x0 = 5 + i
        x1 = 26 - i
        for x in range(x0, x1 + 1):
            px[x, y] = ALLOY[2] if (x + y) % 3 else ALLOY[0]
        px[x0, y] = A_LIGHT
        px[x1, y] = A_DARK
    for y in range(15, 22):                     # the throat down to the burner
        for x in range(13, 19):
            px[x, y] = A_DARK if (x + y) % 2 else (12, 16, 20, 255)
    recess(px, 11, 22, 20, 26, (10, 14, 18, 255))
    for y in range(23, 26):                     # the combustion slot (PULSE target — T_* only)
        for x in range(12, 20):
            px[x, y] = T_TEAL if y == 24 else T_DEEP
    px[15, 24] = T_PLASMA
    px[17, 24] = T_CYAN
    save(img, "block", "bio_generator")

    # front: hopper mouth over the firebox window — banded burn bed with drifting embers
    img = machine_base("bio_generator_front")
    px = img.load()
    for i in range(6):                          # the hopper mouth
        y = 4 + i
        for x in range(5 + i, 27 - i):
            px[x, y] = A_DARK if (x + y) % 3 else (14, 18, 22, 255)
    recess(px, 5, 12, 26, 27, (10, 13, 16, 255))
    for y in range(13, 27):                     # the burn bed, banded
        for x in range(7, 25):
            px[x, y] = T_TEAL if ((y - 13) // 2) % 2 == 0 else T_DEEP
    rng = rng_for("bio_generator_embers")
    for k in range(7):                          # embers in the burn
        ex = rng.randrange(8, 24)
        ey = rng.randrange(14, 26)
        px[ex, ey] = T_PLASMA
        px[ex + 1, ey] = T_CYAN
    for x in range(6, 26):                      # firebox lip
        px[x, 12] = A_LIGHT
    save(img, "block", "bio_generator_front")

    # top: the open intake throat with the feed auger spiralling in it
    img = machine_base("bio_generator_top")
    px = img.load()
    recess(px, 6, 6, 25, 25, (10, 13, 16, 255))
    for y in range(7, 25):
        for x in range(7, 25):
            dx = x - 15.5
            dy = y - 15.5
            d = math.hypot(dx, dy)
            if d > 8.8 or d < 1.2:
                continue
            spiral = (math.atan2(dy, dx) / (2 * math.pi) + d * 0.16) % 1.0
            if spiral < 0.34:                   # the auger flight
                px[x, y] = A_LIGHT if spiral < 0.06 else ALLOY[3]
            elif d > 7.4:
                px[x, y] = A_DARK
    for y in range(14, 18):                     # the shaft glowing down the throat
        for x in range(14, 18):
            px[x, y] = T_TEAL
    px[15, 15] = T_CYAN
    px[16, 16] = T_CYAN
    save(img, "block", "bio_generator_top")


def gen_battery_bank():
    # side: three racked cell rows, each part-charged, capped by a terminal at the right
    img = machine_base("battery_bank")
    px = img.load()
    for i, ry in enumerate((6, 14, 22)):
        recess(px, 5, ry, 26, ry + 5, (10, 14, 18, 255))
        fill = (12, 16, 20)[i]                  # the lower rows hold the most charge
        for y in range(ry + 1, ry + 5):
            for x in range(6, 25):
                px[x, y] = T_TEAL if x <= fill else T_DEEP
            px[25, y] = A_LIGHT                 # the terminal cap
            px[26, y] = A_DARK
        px[fill, ry + 2] = T_CYAN
    save(img, "block", "battery_bank")

    # front: a 2x3 grid of cells, each with its own charge column (PULSE target — T_* only)
    img = machine_base("battery_bank_front")
    px = img.load()
    for row, cy in enumerate((8, 15, 22)):
        for cx in (5, 17):
            recess(px, cx, cy, cx + 9, cy + 5, (10, 13, 16, 255))
            lit = 2 + row * 2                   # the bottom cells are the fullest
            for k in range(5):
                c = (T_CYAN if k == lit - 1 else T_TEAL) if k < lit else T_DEEP
                for x in range(cx + 1, cx + 9):
                    px[x, cy + 5 - k] = c
    led(px, 15, 4, T_PLASMA, T_GLOW)            # the master charge LED
    save(img, "block", "battery_bank_front")

    # top: busbar runs strung between the two terminal posts
    img = machine_base("battery_bank_top")
    px = img.load()
    for tx in (7, 24):                          # the terminal posts
        for y in range(11, 20):
            for x in range(tx - 3, tx + 4):
                px[x, y] = ALLOY[2] if (x + y) % 2 else ALLOY[0]
        for x in range(tx - 3, tx + 4):
            px[x, 11] = A_LIGHT
            px[x, 19] = A_DARK
        px[tx, 15] = A_LIGHT
    for by in (11, 15, 19):                     # the busbars
        for x in range(11, 21):
            px[x, by] = T_DEEP if (x // 2) % 2 == 0 else T_TEAL
            px[x, by + 1] = A_DARK
    for x in (12, 16, 20):
        px[x, 15] = T_CYAN
    save(img, "block", "battery_bank_top")


def gen_grid_controller():
    # side: a thin conduit run across the housing with a pair of link LEDs
    img = machine_base("grid_controller")
    px = img.load()
    for x in range(3, 29):                      # the conduit run
        px[x, 14] = A_DARK
        px[x, 15] = T_DEEP if x % 4 < 2 else _mix(T_DEEP, T_TEAL, 0.6)
        px[x, 16] = A_DARK
    for x in (8, 16, 24):                       # conduit junction pips
        px[x, 15] = T_TEAL
    for x in (6, 15, 24):                       # conduit clamps
        px[x, 13] = A_LIGHT
        px[x, 17] = A_DARK
    for y in (22, 24, 26):                      # vent slats low on the housing
        for x in range(8, 24):
            px[x, y] = A_DARK
    led(px, 8, 6, T_TEAL)
    led(px, 22, 6, T_CYAN)
    save(img, "block", "grid_controller")

    # front: the dispatch screen — a stepped bar-graph under a plasma threshold rule
    img = machine_base("grid_controller_front")
    px = img.load()
    recess(px, 4, 5, 27, 26, (8, 11, 14, 255))
    for i, h in enumerate((5, 9, 14, 11, 17, 13, 8)):
        bx = 6 + i * 3
        for y in range(25 - h, 25):
            c = T_CYAN if y < 14 else T_TEAL
            px[bx, y] = c
            px[bx + 1, y] = c
    for x in range(5, 27):                      # the threshold rule across the graph
        px[x, 14] = T_PLASMA if x % 3 else T_DEEP
    for x in range(6, 27):                      # graph baseline
        px[x, 25] = A_DARK
    save(img, "block", "grid_controller_front")

    # top: dispatch lines fanning out from the hub LED
    img = machine_base("grid_controller_top")
    px = img.load()
    for k in range(8):
        ang = k * math.pi / 4
        for r in range(5, 13):
            px[int(round(15.5 + r * math.cos(ang))),
               int(round(15.5 + r * math.sin(ang)))] = T_TEAL if r % 3 else T_DEEP
        px[int(round(15.5 + 12.0 * math.cos(ang))),
           int(round(15.5 + 12.0 * math.sin(ang)))] = T_CYAN
    for y in range(11, 21):                     # the hub plate
        for x in range(11, 21):
            if math.hypot(x - 15.5, y - 15.5) <= 4.0:
                px[x, y] = A_DARK if (x + y) % 2 else ALLOY[2]
    led(px, 15, 15, T_PLASMA, T_GLOW)
    save(img, "block", "grid_controller_top")


def gen_wireless_node():
    # side: a slim pylon carrying two stacked emitter rings
    img = machine_base("wireless_node")
    px = img.load()
    for y in range(4, 28):                      # the pylon
        for x in range(13, 19):
            px[x, y] = ALLOY[3] if (x + y) % 3 else ALLOY[1]
        px[13, y] = A_LIGHT
        px[18, y] = A_DARK
    for ry in (10, 20):                         # emitter rings (PULSE target — T_* only)
        for x in range(6, 26):
            px[x, ry] = T_DEEP
            px[x, ry + 1] = T_TEAL if (x // 2) % 2 == 0 else T_DEEP
            px[x, ry + 2] = T_DEEP
        px[6, ry + 1] = A_LIGHT
        px[25, ry + 1] = A_DARK
        px[15, ry + 1] = T_CYAN
        px[16, ry + 1] = T_CYAN
    px[15, 5] = T_PLASMA                        # mast tip pip
    save(img, "block", "wireless_node")

    # front: concentric transmission rings around the beacon core (PULSE target)
    img = machine_base("wireless_node_front")
    px = img.load()
    recess(px, 4, 4, 27, 27, (10, 13, 16, 255))
    for y in range(5, 27):
        for x in range(5, 27):
            d = math.hypot(x - 15.5, y - 15.5)
            if d > 10.8:
                continue
            if d <= 1.6:
                px[x, y] = T_GLOW
            else:
                px[x, y] = (T_CYAN, T_TEAL, T_DEEP)[int(d) % 3]
    for k in range(4):                          # antenna spurs breaking the rings
        ang = k * math.pi / 2 + math.pi / 4
        for r in (8, 9, 10):
            px[int(round(15.5 + r * math.cos(ang))),
               int(round(15.5 + r * math.sin(ang)))] = A_DARK
    save(img, "block", "wireless_node_front")

    # top: the emitter dish aperture with four alignment ticks
    img = machine_base("wireless_node_top")
    px = img.load()
    for y in range(6, 26):
        for x in range(6, 26):
            d = math.hypot(x - 15.5, y - 15.5)
            if d <= 2.2:
                px[x, y] = T_GLOW if d <= 1.0 else T_PLASMA   # the aperture
            elif d <= 5.0:
                px[x, y] = T_TEAL if (x + y) % 3 else T_DEEP
            elif d <= 7.0:
                px[x, y] = A_DARK
            elif d <= 9.4:
                px[x, y] = ALLOY[3] if (x + y) % 2 == 0 else ALLOY[1]
    for (tx, ty) in ((15, 4), (15, 27), (4, 15), (27, 15)):   # alignment ticks
        px[tx, ty] = A_LIGHT
        px[tx + 1, ty] = T_CYAN
    save(img, "block", "wireless_node_top")


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


# ---------------- tech guide (pedestal triad + datapad + GUI panel) ----------------

def gen_tech_guide():
    """Pedestal triad (Nerospace's Star Guide cube in NeroTech's teal): side = alloy column with an
    emissive power seam feeding the projector, front = the column's datapad glyph face, top = the
    projector plate with concentric emitter rings (the hologram BER floats above this face)."""
    # side: power seam up the column + paired status LEDs at the plinth
    img = machine_base("tech_guide")
    px = img.load()
    for x in range(8, 24):                      # top-plate and plinth shadow lines
        px[x, 6] = A_LIGHT
        px[x, 7] = A_DARK
        px[x, 25] = A_DARK
        px[x, 26] = A_LIGHT
    for y in range(8, 25):                      # emissive seam (T_* only, so PULSE catches it)
        px[15, y] = T_TEAL if y % 4 < 2 else T_DEEP
        px[16, y] = T_CYAN if y % 4 < 2 else T_TEAL
    led(px, 5, 22, T_CYAN, T_PLASMA)
    led(px, 25, 22, T_CYAN)
    save(img, "block", "tech_guide")

    # front: recessed panel with a docked-datapad outline + the projected "you are here" spark
    img = machine_base("tech_guide_front")
    px = img.load()
    recess(px, 6, 6, 25, 25)
    for y in range(9, 23):                      # datapad slab
        for x in range(10, 22):
            px[x, y] = (10, 15, 19, 255)
    for x in range(10, 22):                     # teal slab frame
        px[x, 9] = T_TEAL
        px[x, 22] = T_DEEP
    for y in range(9, 23):
        px[10, y] = T_TEAL
        px[21, y] = T_DEEP
    for i, ry in enumerate(range(12, 21, 4)):   # chapter rows: lit tick + dim text line
        px[12, ry] = T_CYAN if i < 2 else T_DEEP
        for x in range(14, 20):
            px[x, ry] = _mix(T_DEEP, T_TEAL, 0.55) if x % 3 else T_DEEP
    for (dx, dy, c) in ((0, 0, T_GLOW), (1, 0, T_PLASMA), (-1, 0, T_PLASMA),
                        (0, 1, T_PLASMA), (0, -1, T_PLASMA)):
        px[15 + dx, 15 + dy] = c                # the projector spark
    save(img, "block", "tech_guide_front")

    # top: projector plate — plasma lens with concentric emitter rings
    img = machine_base("tech_guide_top")
    px = img.load()
    for y in range(3, 29):
        for x in range(3, 29):
            d = math.hypot(x - 15.5, y - 15.5)
            if d <= 2.2:
                px[x, y] = T_GLOW if d <= 1.2 else T_PLASMA   # the lens
            elif 5.0 <= d <= 6.2:
                px[x, y] = T_CYAN if (x + y) % 3 else T_TEAL
            elif 9.0 <= d <= 10.2:
                px[x, y] = T_TEAL if (x + y) % 2 else T_DEEP
    save(img, "block", "tech_guide_top")


def gen_tech_guide_datapad():
    """Item: a teal-framed datapad slate — lit chapter ticks down the left, a plasma status dot
    top-right (the Star Guide Book's role in NeroTech's hardware)."""
    img = new_img()
    px = img.load()
    for y in range(3, 29):                      # slate body + dark casing edge
        for x in range(6, 26):
            px[x, y] = A_DARK if (x in (6, 25) or y in (3, 28)) else (12, 18, 22, 255)
    for x in range(7, 25):                      # screen frame (lit top/left, dark bottom/right)
        px[x, 4] = T_TEAL
        px[x, 27] = T_DEEP
    for y in range(4, 28):
        px[7, y] = T_TEAL
        px[24, y] = T_DEEP
    for i, ry in enumerate(range(9, 25, 4)):    # chapter rows: tick + text line
        px[10, ry] = T_CYAN if i < 2 else T_DEEP
        for x in range(12, 22):
            px[x, ry] = _mix(T_DEEP, T_TEAL, 0.55) if x % 3 else T_DEEP
    px[22, 6] = T_PLASMA                        # status dot
    px[21, 6] = T_CYAN
    save(img, "item", "tech_guide_datapad")


def gen_gui_tech_guide():
    """The Tech Guide screen panel: 240x200 sci-fi hull in a 256x256 sheet (see TechGuideScreen).
    Layout zones: title strip, chapter rail (x 6..78), step canvas (x 80..234, y 20..96) and the
    guide-text panel (y 96..194). Nerospace's gen_gui_star_guide recipe with the T_CYAN accent."""
    W, H = 240, 200
    img = Image.new("RGBA", (256, 256), CLEAR)
    px = img.load()
    rng = rng_for("gui_tech_guide")
    INK = (5, 8, 13, 255)
    HULL = [(13, 17, 25, 255), (15, 20, 29, 255), (11, 15, 22, 255)]
    PANEL = (8, 11, 17, 255)
    ACCENT = (36, 208, 222, 255)                # T_CYAN — TechGuideScreen.ACCENT
    ACCENT_D = (18, 104, 111, 255)
    for y in range(H):                          # hull body with light noise
        for x in range(W):
            px[x, y] = rng.choice(HULL)
    for i in range(W):                          # outer frame
        px[i, 0] = ACCENT
        px[i, H - 1] = ACCENT_D
    for i in range(H):
        px[0, i] = ACCENT
        px[W - 1, i] = ACCENT_D
    for y in range(1, H - 1):                   # inset shadow line
        px[1, y] = INK
        px[W - 2, y] = INK
    for x in range(1, W - 1):
        px[x, 1] = INK
        px[x, H - 2] = INK

    # Recessed zones: chapter rail, step canvas, text panel.
    def recess_zone(x0, y0, x1, y1):
        for y in range(y0, y1):
            for x in range(x0, x1):
                px[x, y] = PANEL
        for x in range(x0, x1):
            px[x, y0] = INK
            px[x, y1 - 1] = (30, 40, 56, 255)
        for y in range(y0, y1):
            px[x0, y] = INK
            px[x1 - 1, y] = (30, 40, 56, 255)

    recess_zone(6, 19, 78, 194)                 # chapter rail
    recess_zone(80, 19, 234, 95)                # step canvas
    recess_zone(80, 96, 234, 194)               # guide-text panel
    for x in range(6, 234, 2):                  # title underline dots
        px[x, 16] = ACCENT_D
    save(img, "gui", "tech_guide")


# ---------------- Stage E automation / Stage F exotic endgame ----------------

def gen_conveyor_belt():
    """Belt frame + running surface. The top face carries the direction chevrons; the blockstate
    rotates the whole model, so the art only ever points NORTH (v=0 is north on a top face)."""
    # side: low chassis, belt edge running through it, roller pips
    img = machine_base("conveyor_belt")
    px = img.load()
    recess(px, 3, 12, 28, 21, (10, 13, 16, 255))
    for x in range(4, 28):                      # the belt edge seen side-on
        px[x, 15] = GRAY_D
        px[x, 16] = GRAY
        px[x, 17] = GRAY_D
    for x in range(5, 28, 6):                   # roller pips (teal = the driven rollers)
        px[x, 16] = T_DEEP
        px[x, 15] = T_TEAL
    for ry in (12, 21):                         # frame rails
        for x in range(3, 29):
            px[x, ry] = A_LIGHT if x % 2 == 0 else ALLOY[3]
    save(img, "block", "conveyor_belt")

    # top: woven belt surface + three cyan chevrons pointing north
    img = machine_base("conveyor_belt_top")
    px = img.load()
    for y in range(3, 29):
        for x in range(3, 29):
            px[x, y] = GRAY_D if (x + y) % 4 == 0 else A_DARK
    for y in range(3, 29):                      # belt edges
        px[3, y] = GRAY
        px[28, y] = GRAY_D
    for cy in (6, 14, 22):                      # chevrons: apex north, legs trailing south
        for k in range(7):
            yy = cy + k
            if yy > 28:
                break
            for cx in (15 - k, 16 + k):
                if 4 <= cx <= 27:
                    px[cx, yy] = T_CYAN
                    if yy + 1 <= 28:
                        px[cx, yy + 1] = T_DEEP
    save(img, "block", "conveyor_belt_top")


def gen_robotic_arm():
    # side: servo pedestal with a rotation collar and a cable run
    img = machine_base("robotic_arm")
    px = img.load()
    recess(px, 4, 18, 27, 27, (10, 14, 18, 255))
    for x in range(5, 27):                      # cable run into the base
        px[x, 22] = T_DEEP if (x // 2) % 2 == 0 else A_DARK
    for y in range(S):
        for x in range(S):
            d = math.hypot(x - 15.5, y - 11.5)
            if d <= 3.0:
                px[x, y] = (10, 13, 16, 255)
            elif d <= 5.0:
                px[x, y] = GRAY
            elif d <= 6.0:
                px[x, y] = GRAY_D
    led(px, 15, 10, T_CYAN, T_GLOW)             # the servo indicator
    save(img, "block", "robotic_arm")

    # front: the gripper claw over a two-lamp status strip (PULSE target — T_* only)
    img = machine_base("robotic_arm_front")
    px = img.load()
    recess(px, 6, 5, 25, 20, (10, 13, 16, 255))
    for k in range(7):                          # the two claw fingers, closing inward
        for x in (9 + k, 22 - k):
            px[x, 7 + k] = A_LIGHT if k % 2 == 0 else ALLOY[3]
            px[x, 8 + k] = A_DARK
    for y in range(9, 18):                      # the grip gap glow between them
        px[15, y] = T_TEAL
        px[16, y] = T_TEAL
    px[15, 13] = T_PLASMA
    px[16, 13] = T_PLASMA
    led(px, 9, 24, T_CYAN, None)
    led(px, 21, 24, T_TEAL, None)
    save(img, "block", "robotic_arm_front")

    # top: the shoulder turntable with its index marks
    img = machine_base("robotic_arm_top")
    px = img.load()
    for y in range(S):
        for x in range(S):
            d = math.hypot(x - 15.5, y - 15.5)
            if d <= 3.5:
                px[x, y] = (10, 13, 16, 255)
            elif d <= 9.0:
                px[x, y] = GRAY if (x + y) % 2 == 0 else GRAY_D
            elif d <= 10.5:
                px[x, y] = A_DARK
    for k in range(8):                          # turntable index marks
        ang = k * math.pi / 4
        px[int(round(15.5 + 9.5 * math.cos(ang))),
           int(round(15.5 + 9.5 * math.sin(ang)))] = T_DEEP
    for y in range(12, 20):                     # the arm's parked reach, pointing north
        px[15, y - 8] = T_TEAL
        px[16, y - 8] = T_TEAL
    px[15, 15] = T_CYAN
    px[16, 16] = T_CYAN
    save(img, "block", "robotic_arm_top")


def gen_singularity_vault():
    # side: heavy containment ribs bracketing a dark inspection window
    img = machine_base("singularity_vault")
    px = img.load()
    for rx in (5, 14, 23):                      # vertical containment ribs
        for y in range(3, 29):
            px[rx, y] = A_LIGHT
            px[rx + 1, y] = ALLOY[3]
            px[rx + 2, y] = A_DARK
    recess(px, 8, 11, 12, 20, (8, 10, 13, 255))
    recess(px, 17, 11, 21, 20, (8, 10, 13, 255))
    for y in range(12, 20):                     # the stored-mass sliver behind each window
        px[10, y] = T_DEEP if y % 2 else T_TEAL
        px[19, y] = T_DEEP if y % 2 else T_TEAL
    save(img, "block", "singularity_vault")

    # front: the access port — an event-horizon disc ringed by a fill scale
    img = machine_base("singularity_vault_front")
    px = img.load()
    for y in range(S):
        for x in range(S):
            d = math.hypot(x - 15.5, y - 15.5)
            if d <= 4.0:
                px[x, y] = (4, 6, 9, 255)       # the singularity: darker than any alloy
            elif d <= 5.5:
                px[x, y] = T_DEEP
            elif d <= 6.5:
                px[x, y] = T_TEAL if (x + y) % 2 == 0 else T_DEEP
            elif d <= 7.5:
                px[x, y] = T_CYAN if (x + y) % 3 == 0 else T_TEAL
            elif d <= 9.0:
                px[x, y] = A_DARK
    for k in range(12):                         # the fill scale around the port
        ang = -math.pi / 2 + k * math.pi / 6
        sx = int(round(15.5 + 10.5 * math.cos(ang)))
        sy = int(round(15.5 + 10.5 * math.sin(ang)))
        px[sx, sy] = T_CYAN if k < 8 else T_DEEP
    led(px, 15, 27, T_PLASMA, T_GLOW)           # the "holding something" lamp
    save(img, "block", "singularity_vault_front")

    # top: the compression lattice folding inward
    img = machine_base("singularity_vault_top")
    px = img.load()
    for y in range(S):
        for x in range(S):
            d = max(abs(x - 15.5), abs(y - 15.5))
            if d <= 2.5:
                px[x, y] = (4, 6, 9, 255)
            elif d <= 4.5:
                px[x, y] = T_DEEP
            elif d <= 10.5 and (int(d) % 2 == 0):
                px[x, y] = ALLOY[2]
            elif d <= 10.5:
                px[x, y] = ALLOY[0]
    for k in range(4):                          # lattice spokes into the centre
        ang = k * math.pi / 2 + math.pi / 4
        for r in range(4, 11):
            px[int(round(15.5 + r * math.cos(ang))),
               int(round(15.5 + r * math.sin(ang)))] = T_DEEP if r % 2 else T_TEAL
    save(img, "block", "singularity_vault_top")


def gen_antimatter_cell():
    """Tier-4 fuel: the cell canister with an INVERTED core — a void darker than the alloy,
    ringed by plasma — under TRIPLE containment rings, plus a hazard tick on BOTH caps (the
    fourth and last place hazard striping is allowed: reactor shell, configurator, stellar
    cell tick, and this)."""
    rng = rng_for("antimatter_cell")
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
    for y in range(9, 23):                      # window — void core inside a plasma halo
        for x in range(13, 19):
            d = math.hypot(x - 15.5, y - 15.5)
            if d <= 2.5:
                px[x, y] = (4, 6, 9, 255)
            elif d <= 3.5:
                px[x, y] = T_GLOW if (x + y) % 2 == 0 else T_PLASMA
            elif d <= 5.0:
                px[x, y] = T_PLASMA
            elif d <= 6.5:
                px[x, y] = T_CYAN
            else:
                px[x, y] = T_TEAL
    for y in range(9, 23):                      # window frame
        px[12, y] = A_DARK
        px[19, y] = A_DARK
    for x in range(12, 20):
        px[x, 8] = A_DARK
        px[x, 23] = A_DARK
    for ry in (11, 15, 20):                     # TRIPLE containment rings
        for x in range(12, 20):
            px[x, ry] = A_LIGHT if x % 2 == 0 else ALLOY[3]
    for cap_y in (6, 25):                       # hazard tick on both caps
        for i, tx in enumerate(range(13, 19)):
            px[tx, cap_y] = HAZ_Y if i % 2 == 0 else HAZ_K
    save(img, "item", "antimatter_cell")


# ---------------- main ----------------

def check_coverage():
    """Flag any pre-existing PNG no painter owns — after a --force run NOTHING may be left on
    the old 16x art, and every model-referenced name must resolve."""
    clean = True
    for folder, d in (("block", BLOCK_DIR), ("item", ITEM_DIR), ("gui", GUI_DIR)):
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
    gen_accelerator_coil()
    gen_accelerator_coil_indicators()
    gen_collider_core()
    gen_electrolyzer()
    gen_gas_turbine()
    gen_chemical_processor()
    gen_coolant_pump()
    gen_radiator()
    gen_auto_crafter()
    gen_item_sorter()
    gen_scrubber()
    gen_remediator()
    gen_analytics_terminal()
    gen_tech_guide()
    # Stage D: power tech
    gen_battery_bank()
    gen_bio_generator()
    gen_geothermal_generator()
    gen_grid_controller()
    gen_wind_turbine()
    gen_wireless_node()
    # Stage E automation + Stage F exotic endgame
    gen_conveyor_belt()
    gen_robotic_arm()
    gen_singularity_vault()
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
    gen_antimatter_cell()
    gen_filter_cartridge()
    gen_dirty_filter()
    gen_module("speed_module", _glyph_speed)
    gen_module("efficiency_module", _glyph_bolt)
    gen_module("capacity_module", _glyph_bars)
    gen_module("range_module", _glyph_rings)
    gen_configurator()
    gen_tech_guide_datapad()
    # GUI sheets (the Tech Guide is the one textured screen — Star Guide recipe)
    gen_gui_tech_guide()
    check_coverage()


if __name__ == "__main__":
    main()
