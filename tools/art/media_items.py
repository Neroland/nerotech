"""A7 Media + items art package: every non-tool item icon of 0.8.0 and the three fluid buckets.

Painted here (``textures/item/<id>.png``, all 32x, all ``item/generated``):

* structural   -- cindersteel ingot / plate / rod, titanium plate
* semiconductor-- crystal seed, crystal boule, wafer
* chemistry    -- sulfur dust, polymer sheet, europium dust, synthetic glacite
* ladder       -- logic and quantum chip; copper / cindersteel / superconductor wire and coil;
                  servo, capacitor, heat sink
* optics       -- the four focus lenses (one ring mount, four cuts: the cut is the colour-blind cue)
* drilling     -- iron (three cones), cindersteel (four stepped blades), glacite (six blades)
* modules      -- heat_sink_module and scrubber_module on the shared ``gen_module`` card
* buckets      -- cryo coolant, sulfuric acid, polymer resin

The still/flow fluid textures are NOT here: ``tools/gen_fluid_textures.py`` owns them (it runs
without Pillow). The press dies belong to the metallurgy package, which paints them to match the die
the Plate Press shows.

Stage 1 left stub icons under these ids, and ``save()`` is additive, so a plain run leaves whatever
is on disk alone. Replace this package's icons, and only this package's, with::

    python tools/gen_textures.py --multiloader --force=media_items

(a full ``--force`` run repaints the stubs first and this package then paints over them).

Shape names the part, colour names the tier or material. Shading is top-left lit; shadows lean to
teal-deep like the rest of the item set. No heat colour is used as a state: ember orange is the
ember lens's own glass.
"""
import math
import sys

from art import common as K

G = K.G
S = K.S
CLEAR = K.CLEAR
FORCE_FLAG = "--force=media_items"

# ---- ramps: (base, light, dark, glint) -------------------------------------------------------------
CIN = (K.CINDER[0], K.CINDER[1], K.CINDER[2], K.CINDER[3])
TI = (K.TITANIUM_PLATE, (222, 230, 240, 255), (146, 158, 178, 255), (244, 248, 252, 255))
COP = (K.COPPER[0], K.COPPER[2], K.COPPER[1], (248, 196, 148, 255))
SUP = (K.T_CYAN, K.FROST[0], K.T_TEAL, (244, 252, 255, 255))      # cyan sheath over a frost core
STEEL = (K.IRON[1], K.IRON[0], (84, 90, 100, 255), K.IRON[2])
SIL = ((92, 104, 124, 255), (150, 166, 190, 255), (60, 70, 88, 255), (208, 220, 238, 255))
CREAM = ((228, 200, 156, 255), (244, 228, 196, 255), (192, 160, 116, 255), (252, 244, 226, 255))
SULFUR = ((226, 220, 78, 255), (246, 242, 150, 255), (178, 168, 44, 255), (252, 250, 204, 255))
EURO = (K.EU_ROD[0], (238, 242, 232, 255), K.EU_ROD[1], (252, 254, 248, 255))
ICE = (K.FROST[1], K.FROST[0], (116, 176, 212, 255), (246, 252, 255, 255))
PACKAGE = ((24, 96, 88, 255), (56, 146, 132, 255), (12, 58, 56, 255), (110, 196, 180, 255))
ALLOY = (K.ALLOY[1], K.ALLOY[3], K.ALLOY[2], K.A_LIGHT)

BUCKET = ((168, 170, 174, 255), (214, 216, 220, 255), (116, 120, 128, 255), (72, 76, 86, 255))


def rgba(c):
    return (c[0], c[1], c[2], 255)


def shadow(c, t=0.4):
    """Teal-leaning shadow of a colour (the item-set shading rule)."""
    return K.mix(c, K.T_DEEP, t)


def lift(c, t=0.35):
    return K.mix(c, (255, 255, 255, 255), t)


# ---- drawing kit -----------------------------------------------------------------------------------
def put(px, x, y, col):
    if 0 <= x < S and 0 <= y < S:
        px[x, y] = col


def rect(px, x0, y0, x1, y1, col):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            put(px, x, y, col)


def poly(px, pts, col):
    """Fill a polygon (pixel centres, even-odd)."""
    ys = [p[1] for p in pts]
    for y in range(int(math.floor(min(ys))), int(math.ceil(max(ys))) + 1):
        cy = y + 0.5
        xs = []
        for i in range(len(pts)):
            (x0, y0), (x1, y1) = pts[i], pts[(i + 1) % len(pts)]
            if (y0 <= cy < y1) or (y1 <= cy < y0):
                xs.append(x0 + (cy - y0) * (x1 - x0) / float(y1 - y0))
        xs.sort()
        for a, b in zip(xs[0::2], xs[1::2]):
            for x in range(int(math.ceil(a - 0.5)), int(math.floor(b - 0.5)) + 1):
                put(px, x, y, col if not callable(col) else col(x, y))


def ellipse(px, cx, cy, rx, ry, col):
    for y in range(int(cy - ry) - 1, int(cy + ry) + 2):
        for x in range(int(cx - rx) - 1, int(cx + rx) + 2):
            if ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2 <= 1.0:
                put(px, x, y, col if not callable(col) else col(x, y))


def seg_dist(x, y, x0, y0, x1, y1):
    """Distance from a point to a segment, and the signed side (negative = upper/left of it)."""
    dx, dy = x1 - x0, y1 - y0
    ln = dx * dx + dy * dy
    t = max(0.0, min(1.0, ((x - x0) * dx + (y - y0) * dy) / ln))
    px_, py_ = x0 + t * dx, y0 + t * dy
    d = math.hypot(x - px_, y - py_)
    side = (x - x0) * dy - (y - y0) * dx
    return d, side, t


def bar(px, x0, y0, x1, y1, half, ramp):
    """A round bar between two points: light on its upper side, dark on its lower side."""
    base, light, dark, glint = ramp
    for y in range(S):
        for x in range(S):
            d, side, _t = seg_dist(x, y, x0, y0, x1, y1)
            if d <= half:
                rel = (d / half) * (1 if side > 0 else -1)   # -1 upper edge .. +1 lower edge
                if rel < -0.72:
                    col = light
                elif rel < -0.25:
                    col = glint if half > 1.6 else light
                elif rel < 0.45:
                    col = base
                else:
                    col = dark
                px[x, y] = col


def rim(img, amount=0.45):
    """Darken every pixel whose lower or right neighbour is empty: a soft drawn edge."""
    px = img.load()
    hits = []
    for y in range(S):
        for x in range(S):
            if px[x, y][3] == 0:
                continue
            below = y + 1 >= S or px[x, y + 1][3] == 0
            right = x + 1 >= S or px[x + 1, y][3] == 0
            if below or right:
                hits.append((x, y))
    for x, y in hits:
        px[x, y] = shadow(px[x, y], amount)


def speckle(px, name, x0, y0, x1, y1, cols, chance=0.3):
    """Scatter ``cols`` over the filled pixels of a region (material grain)."""
    rng = K.rng_for(name + ":grain")
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            if px[x, y][3] and rng.random() < chance:
                px[x, y] = rng.choice(cols)


def save_item(img, name):
    """``save()`` with this package's own force switch (see the module docstring)."""
    old = G.FORCE
    G.FORCE = old or FORCE_FLAG in sys.argv
    try:
        K.save(img, "item", name)
    finally:
        G.FORCE = old


# ---- structural ------------------------------------------------------------------------------------
def paint_ingot(name, ramp):
    base, light, dark, glint = ramp
    img = K.new_img()
    px = img.load()
    poly(px, [(9, 10), (24, 10), (28, 17), (5, 17)], light)             # top face
    poly(px, [(5, 17), (28, 17), (26, 24), (7, 24)], base)              # front face
    speckle(px, name, 5, 17, 28, 23, [dark, K.mix(base, dark, 0.5)], 0.22)
    speckle(px, name + "t", 6, 11, 27, 16, [base, glint], 0.14)
    for x in range(9, 24):
        px[x, 10] = glint
    for x in range(6, 28):
        px[x, 17] = K.mix(light, glint, 0.5)                            # front lip catches light
    for x in range(8, 26):
        px[x, 23] = dark
    rect(px, 13, 13, 19, 13, K.mix(base, dark, 0.4))                    # foundry stamp
    rect(px, 13, 14, 19, 14, glint)
    for i, (gx, gy) in enumerate(((9, 15), (10, 14), (11, 13), (12, 12))):   # the one alloy glint
        px[gx, gy] = K.A_LIGHT
    px[10, 15] = K.A_LIGHT
    rim(img, 0.4)
    save_item(img, name)


def paint_plate(name, ramp, chamfer=False, brushed=False):
    base, light, dark, glint = ramp
    img = K.new_img()
    px = img.load()
    rect(px, 6, 7, 24, 23, base)
    rect(px, 7, 24, 25, 25, dark)                                       # thickness
    rect(px, 25, 8, 25, 25, dark)
    if brushed:
        for y in range(9, 23, 2):
            for x in range(8, 23):
                if (x * 7 + y * 3) % 5:
                    px[x, y] = K.mix(base, light, 0.45)
    else:
        speckle(px, name, 7, 8, 23, 22, [light, dark, K.mix(base, dark, 0.5)], 0.3)
    for x in range(6, 25):
        px[x, 7] = light
    for y in range(7, 24):
        px[6, y] = light
    for x in range(8, 24):
        px[x, 23] = K.mix(base, dark, 0.6)
    for y in range(9, 23):
        px[24, y] = K.mix(base, dark, 0.6)
    if chamfer:                                                         # clipped corners
        for (cx, cy, sx, sy) in ((6, 7, 1, 1), (25, 7, -1, 1), (6, 25, 1, -1), (25, 25, -1, -1)):
            for i in range(3):
                for j in range(3 - i):
                    px[cx + sx * i, cy + sy * j] = CLEAR
        for i in range(3):
            px[8 - i, 7 + i] = glint
        for (hx, hy) in ((10, 11), (20, 11), (10, 19), (20, 19)):       # fastener holes
            px[hx, hy] = dark
            px[hx + 1, hy] = dark
            px[hx, hy + 1] = shadow(dark, 0.5)
            px[hx + 1, hy + 1] = glint
    for i in range(5):                                                  # glint streak
        px[10 + i, 14 - i] = K.A_LIGHT if not chamfer else glint
        if i < 3:
            px[13 + i, 16 - i] = K.mix(base, K.A_LIGHT if not chamfer else glint, 0.6)
    rim(img, 0.35)
    save_item(img, name)


def paint_rod(name, ramp):
    base, light, dark, glint = ramp
    img = K.new_img()
    px = img.load()
    bar(px, 10, 27, 28, 9, 2.3, ramp)                                   # rear rod
    bar(px, 4, 23, 22, 5, 2.3, ramp)                                    # front rod
    for (ex, ey) in ((22, 5), (28, 9)):                                 # cut faces
        ellipse(px, ex + 0.5, ey - 0.5, 2.3, 2.3, K.mix(light, glint, 0.5))
        px[ex, ey] = base
        px[ex + 1, ey - 1] = glint
    for (gx, gy) in ((9, 16), (10, 15), (11, 14)):
        px[gx, gy] = K.A_LIGHT
    rim(img, 0.4)
    save_item(img, name)


# ---- wire and coil ---------------------------------------------------------------------------------
def paint_wire(name, ramp):
    """A loose hank: three slack loops tied once, one end hanging free."""
    base, light, dark, glint = ramp
    img = K.new_img()
    px = img.load()
    loops = ((16.5, 16.5, 10.0, 6.4), (14.5, 14.0, 9.6, 6.6), (15.5, 12.0, 9.0, 5.6))
    for (cx, cy, rx, ry) in loops:
        for y in range(S):
            for x in range(S):
                r = math.hypot((x - cx) / rx, (y - cy) / ry)
                d = abs(r - 1.0) * min(rx, ry)
                if d <= 1.25:
                    upper = (y - cy) / ry < -0.15
                    outer = r > 1.0
                    if d < 0.55:
                        col = light if upper else base
                    elif upper == outer:
                        col = glint if upper else dark
                    else:
                        col = base if upper else shadow(dark, 0.25)
                    px[x, y] = col
    bar(px, 24, 18, 27, 26, 1.2, ramp)                                  # the free end
    px[27, 27] = glint
    px[26, 27] = light
    tie = ALLOY
    rect(px, 4, 11, 7, 18, tie[0])                                      # the tie
    rect(px, 4, 11, 7, 11, tie[3])
    rect(px, 4, 14, 7, 14, tie[2])
    rect(px, 4, 18, 7, 18, K.A_DARK)
    px[5, 12] = tie[3]
    rim(img, 0.3)
    save_item(img, name)


def paint_coil(name, ramp):
    """A wound bobbin seen from above its top flange: round flange, hub hole, barrel of windings.
    (Not the flat pancake of the Nero Coil and not the accelerator coil block.)"""
    base, light, dark, glint = ramp
    img = K.new_img()
    px = img.load()
    fl = (STEEL[0], STEEL[1], STEEL[2], STEEL[3])
    ellipse(px, 15.5, 24.0, 10.5, 4.2, fl[2])                           # bottom flange
    ellipse(px, 15.5, 23.2, 10.5, 4.2, fl[0])
    for y in range(9, 24):                                              # winding barrel
        for x in range(8, 24):
            bow = 2.6 * math.sqrt(max(0.0, 1.0 - ((x - 15.5) / 8.2) ** 2))
            if y > 21 + bow:
                continue
            turn = int(math.floor(y - bow)) % 3
            edge = (x - 15.5) / 8.0
            if turn == 0:
                col = dark
            elif edge < -0.55:
                col = glint if turn == 1 else light
            elif edge < 0.35:
                col = light if turn == 1 else base
            else:
                col = base if turn == 1 else K.mix(base, dark, 0.6)
            px[x, y] = col
    ellipse(px, 15.5, 9.6, 10.5, 4.2, fl[2])                            # top flange edge
    ellipse(px, 15.5, 8.6, 10.5, 4.2, fl[1])                            # top flange face
    for x in range(7, 16):                                              # lit rim, upper left
        for y in range(4, 9):
            v = ((x - 15.5) / 10.5) ** 2 + ((y - 8.6) / 4.2) ** 2
            if 0.78 < v <= 1.0:
                px[x, y] = fl[3]
    ellipse(px, 15.5, 8.4, 3.2, 1.4, K.A_DARK)                          # hub hole
    rect(px, 13, 10, 18, 10, fl[3])                                     # its lit lower lip
    put(px, 25, 12, light)                                              # wire tail off the flange
    put(px, 26, 13, base)
    put(px, 26, 14, base)
    put(px, 27, 15, dark)
    rim(img, 0.35)
    save_item(img, name)


# ---- semiconductor ---------------------------------------------------------------------------------
def paint_crystal_seed():
    base, light, dark, glint = SIL
    img = K.new_img()
    px = img.load()
    rect(px, 15, 15, 16, 25, K.GRAY_L)                                  # the pin
    rect(px, 16, 15, 16, 25, K.GRAY_D)
    rect(px, 11, 26, 20, 27, ALLOY[1])                                  # foot
    rect(px, 11, 28, 20, 28, ALLOY[2])
    rect(px, 11, 26, 20, 26, K.A_LIGHT)
    rect(px, 13, 14, 18, 15, K.GOLD[0])                                 # clamp
    rect(px, 13, 15, 18, 15, K.GOLD[1])
    px[13, 14] = K.GOLD[2]
    poly(px, [(16, 3), (20, 8), (18, 14), (14, 14), (12, 9)], base)     # the shard
    poly(px, [(16, 3), (16, 14), (14, 14), (12, 9)], light)
    poly(px, [(18.2, 8), (20, 8), (18, 14), (17, 14)], dark)
    for (gx, gy) in ((15, 5), (14, 7), (14, 8), (13, 9)):
        px[gx, gy] = glint
    px[15, 11] = K.T_CYAN                                               # the growing lattice
    rim(img, 0.3)
    save_item(img, "crystal_seed")


def paint_crystal_boule():
    base, light, dark, glint = SIL
    img = K.new_img()
    px = img.load()

    def half_width(y):
        if y < 4:
            return 1.5
        if y < 10:                                                      # shoulder
            return 1.5 + 4.8 * math.sin((y - 4) / 6.0 * math.pi / 2)
        if y <= 24:
            return 6.3
        return max(0.0, 6.3 * math.sqrt(max(0.0, 1.0 - ((y - 24) / 4.0) ** 2)))   # rounded tail

    for y in range(2, 29):
        hw = half_width(y)
        for x in range(S):
            off = (x - 15.5) / max(hw, 0.01)
            if abs(x - 15.5) <= hw:
                if off < -0.78:
                    col = base
                elif off < -0.45:
                    col = light
                elif off < -0.22:
                    col = glint
                elif off < 0.3:
                    col = base
                elif off < 0.7:
                    col = K.mix(base, dark, 0.6)
                else:
                    col = dark
                px[x, y] = col
    for y in (12, 17, 22):                                              # growth rings
        for x in range(10, 22):
            if px[x, y][3]:
                px[x, y] = shadow(px[x, y], 0.22)
    px[13, 26] = K.T_TEAL
    rim(img, 0.3)
    save_item(img, "crystal_boule")


def paint_wafer():
    """Thin mirror disc with one flat edge and the faint grid of dies."""
    base, light, dark, glint = SIL
    face = (146, 160, 186, 255)
    grid = (126, 140, 168, 255)
    img = K.new_img()
    px = img.load()
    cx, cy, rx, ry = 15.5, 14.0, 12.0, 11.0
    flat = 22                                                           # the flat edge (a chord)
    for dy, col in ((2, dark), (1, shadow(base, 0.2))):                 # disc thickness
        for y in range(S):
            for x in range(S):
                if ((x - cx) / rx) ** 2 + ((y - dy - cy) / ry) ** 2 <= 1.0 and y - dy <= flat:
                    px[x, y] = col
    for y in range(S):
        for x in range(S):
            v = ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2
            if v <= 1.0 and y <= flat:
                col = face
                if v <= 0.66 and (x % 4 == 3 or y % 4 == 2):
                    col = grid                                          # the die grid
                if v > 0.84:
                    col = light if (x + y) < 30 else base               # bevelled edge
                if 27 <= x + y <= 28 and v <= 0.84:
                    col = K.mix(col, glint, 0.75)                       # mirror sweep
                elif x + y == 31 and v <= 0.84:
                    col = K.mix(col, glint, 0.4)
                px[x, y] = col
    for x in range(S):
        if px[x, flat][3] and px[x, flat] not in (dark, shadow(base, 0.2)):
            px[x, flat] = light
    rim(img, 0.25)
    save_item(img, "wafer")


# ---- dusts, sheet, glacite -------------------------------------------------------------------------
def paint_dust(name, ramp, spread=0.75, crystals=(), spark=None):
    base, light, dark, glint = ramp
    rng = K.rng_for(name)
    img = K.new_img()
    px = img.load()
    shade = shadow(dark, 0.4)
    top = 13 if spread > 0.8 else 11
    for y in range(top, 28):
        half = min(12, int((y - top + 2) * spread))
        for x in range(16 - half, 16 + half):
            if rng.random() < 0.92:
                r = rng.random()
                col = light if (y < top + 5 and r < 0.4) else (base if r < 0.72 else dark)
                if y >= 25 or x < 16 - half + 2 or x > 16 + half - 3:
                    if rng.random() < 0.6:
                        col = shade
                elif x < 15 and r < 0.12:
                    col = glint
                px[x, y] = col
    for (sx, sy) in ((9, 10), (21, 9), (14, 7), (24, 13), (7, 14)):     # loose grains
        px[sx, sy] = light if (sx + sy) % 2 == 0 else base
    for (cx, cy) in crystals:                                           # small crystal lumps
        poly(px, [(cx, cy - 3), (cx + 3, cy), (cx, cy + 3), (cx - 3, cy)], base)
        poly(px, [(cx, cy - 3), (cx, cy + 3), (cx - 3, cy)], light)
        px[cx - 1, cy - 1] = glint
        px[cx + 1, cy + 1] = dark
    if spark is not None:
        for (sx, sy) in ((13, 17), (19, 21), (11, 23)):
            px[sx, sy] = spark
    save_item(img, name)


def paint_polymer_sheet():
    """Cream sheet, one corner curled up and over."""
    base, light, dark, glint = CREAM
    img = K.new_img()
    px = img.load()
    poly(px, [(4, 8), (26, 6), (28, 24), (6, 26)], base)
    poly(px, [(4, 8), (26, 6), (26.2, 7.4), (4.2, 9.4)], light)         # lit edge
    poly(px, [(6, 24.6), (28, 22.6), (28, 24), (6, 26)], dark)          # thickness
    for i in range(8):                                                  # soft gloss streak
        put(px, 8 + i, 19 - i, glint)
        put(px, 9 + i, 19 - i, light)
    for i in range(4):
        put(px, 13 + i, 20 - i, light)
    for y in range(S):                                                  # lift the corner off
        for x in range(S):
            if x + y >= 45:
                px[x, y] = CLEAR
    under = K.mix(dark, (150, 112, 70, 255), 0.25)
    poly(px, [(27.5, 16.5), (20, 25.5), (19, 17.5)], under)             # the flap, underside up
    poly(px, [(27.5, 16.5), (19, 17.5), (20.5, 19), (25, 18.5)], dark)
    for i in range(8):                                                  # fold line
        put(px, 27 - i, 17 + i, glint)
    for i in range(7):                                                  # shadow the flap casts
        put(px, 18, 18 + i, K.mix(base, dark, 0.7))
    for x in range(19, 27):
        put(px, x, 16, K.mix(base, dark, 0.7))
    rim(img, 0.22)
    save_item(img, "polymer_sheet")


def paint_synthetic_glacite():
    """A grown crystal: one true hexagonal prism on a machined, stepped flat base."""
    base, light, dark, glint = ICE
    img = K.new_img()
    px = img.load()
    for y in range(3, 24):
        hw = 6.0 if y >= 10 else 0.6 + (y - 3) * 0.78                   # point, then straight sides
        for x in range(S):
            off = x - 15.5
            if abs(off) <= hw:
                if off < -2.2:
                    col = light
                elif off < 2.2:
                    col = base if y >= 10 else K.mix(base, light, 0.5)
                else:
                    col = dark
                px[x, y] = col
    for y in range(4, 23):                                              # facet edges
        if px[13, y][3]:
            px[13, y] = glint
        if px[18, y][3] and y >= 10:
            px[18, y] = K.mix(base, dark, 0.55)
    for x in range(10, 22):                                             # shoulder line
        if px[x, 10][3]:
            px[x, 10] = K.mix(px[x, 10], glint, 0.5)
    rect(px, 8, 24, 23, 25, light)                                      # machined step
    rect(px, 8, 24, 23, 24, glint)
    rect(px, 19, 25, 23, 25, base)
    rect(px, 7, 26, 24, 27, base)                                       # machined foot
    rect(px, 7, 26, 24, 26, K.mix(base, glint, 0.5))
    rect(px, 7, 28, 24, 28, dark)
    for x in range(9, 23, 3):                                           # tool marks
        px[x, 27] = dark
    px[15, 14] = K.T_CYAN
    px[15, 15] = K.T_CYAN
    px[16, 17] = K.T_PLASMA
    rim(img, 0.3)
    save_item(img, "synthetic_glacite")


# ---- processors ------------------------------------------------------------------------------------
def _pins(px, positions, y0, y1, vertical):
    for p in positions:
        for q in range(y0, y1 + 1):
            col = K.GOLD[0] if q != (y1 if y1 > 15 else y0) else K.GOLD[1]
            if vertical:
                put(px, p, q, col)
                put(px, p + 1, q, K.GOLD[1] if col == K.GOLD[0] else shadow(K.GOLD[1], 0.3))
            else:
                put(px, q, p, col)
                put(px, q, p + 1, K.GOLD[1] if col == K.GOLD[0] else shadow(K.GOLD[1], 0.3))


def paint_logic_chip():
    """Square green-teal package, gold pins on four sides, one die window."""
    base, light, dark, glint = PACKAGE
    img = K.new_img()
    px = img.load()
    spots = (9, 13, 17, 21)
    _pins(px, spots, 5, 7, True)
    _pins(px, spots, 24, 26, True)
    _pins(px, spots, 5, 7, False)
    _pins(px, spots, 24, 26, False)
    rect(px, 8, 8, 23, 23, base)
    speckle(px, "logic_chip", 9, 9, 22, 22, [K.mix(base, light, 0.3), K.mix(base, dark, 0.4)], 0.3)
    rect(px, 8, 8, 23, 8, light)
    rect(px, 8, 8, 8, 23, light)
    rect(px, 8, 23, 23, 23, dark)
    rect(px, 23, 8, 23, 23, dark)
    px[8, 8] = glint
    rect(px, 12, 12, 19, 19, dark)                                      # die window frame
    rect(px, 13, 13, 18, 18, K.T_DEEP)
    rect(px, 14, 14, 17, 17, K.T_TEAL)
    rect(px, 15, 15, 16, 16, K.T_CYAN)
    px[14, 14] = K.T_CYAN
    px[15, 15] = K.T_PLASMA
    rect(px, 12, 19, 19, 19, K.mix(base, light, 0.6))                   # window lip
    px[10, 10] = glint                                                  # pin-1 dot
    px[11, 10] = light
    for x in (10, 12, 14):                                              # part marking
        px[x, 21] = K.mix(base, light, 0.7)
    save_item(img, "logic_chip")


def paint_quantum_chip():
    """Larger and octagonal: a plasma core under a glass lid, pins on all eight sides."""
    img = K.new_img()
    px = img.load()
    body = (K.ALLOY[2], K.ALLOY[3], K.A_DARK, K.A_LIGHT)

    def octagon(inset):
        lo, hi, cut = 3 + inset, 28 - inset, 7 - inset // 2
        return [(lo + cut, lo), (hi - cut + 1, lo), (hi + 1, lo + cut), (hi + 1, hi - cut + 1),
                (hi - cut + 1, hi + 1), (lo + cut, hi + 1), (lo, hi - cut + 1), (lo, lo + cut)]

    poly(px, octagon(0), K.GOLD[1])                                     # pin skirt
    for y in range(S):
        for x in range(S):
            if px[x, y][3] and (x + y) % 2 == 0:
                px[x, y] = K.GOLD[0]
            if px[x, y][3] and (x + y) % 4 == 0:
                px[x, y] = K.GOLD[2]
    poly(px, octagon(2), body[0])
    speckle(px, "quantum_chip", 5, 5, 26, 26, [K.ALLOY[0], K.ALLOY[1]], 0.5)
    poly(px, octagon(2), lambda x, y: body[3] if (y == 5 or x == 5 or (x + y) <= 17)
         else (body[2] if (y == 26 or x == 26 or (x + y) >= 46) else px[x, y]))
    for y in range(S):                                                  # teal guard ring
        for x in range(S):
            d = math.hypot(x - 15.5, y - 15.5)
            if 8.2 < d <= 9.3:
                px[x, y] = K.T_TEAL if (x + y) % 3 else K.T_CYAN
            elif d <= 8.2:
                if d > 7.0:
                    px[x, y] = K.A_LIGHT if (x + y) < 31 else K.GRAY_D   # lid bezel
                elif d > 5.2:
                    px[x, y] = K.mix(K.T_DEEP, K.GLASS, 0.35)            # glass over the well
                elif d > 3.6:
                    px[x, y] = K.T_TEAL
                elif d > 2.2:
                    px[x, y] = K.T_CYAN
                elif d > 1.0:
                    px[x, y] = K.T_PLASMA
                else:
                    px[x, y] = K.T_GLOW
    for (gx, gy) in ((11, 13), (12, 12), (13, 11), (12, 13), (13, 12)):  # lid glint
        px[gx, gy] = rgba(K.GLASS_HI)
    px[19, 19] = K.mix(K.T_DEEP, K.GLASS, 0.7)
    for (dx, dy) in ((8, 8), (23, 8), (8, 23), (23, 23)):               # corner lands
        px[dx, dy] = K.GOLD[2]
    save_item(img, "quantum_chip")


# ---- parts -----------------------------------------------------------------------------------------
def paint_servo():
    """Motor can with mounting ears and a straight output horn on top."""
    img = K.new_img()
    px = img.load()
    for y in range(13, 28):                                             # can
        for x in range(8, 23):
            px[x, y] = K.ALLOY[3] if x < 11 else (K.ALLOY[1] if x < 18 else K.ALLOY[2])
    rect(px, 8, 13, 22, 13, K.A_LIGHT)
    rect(px, 8, 27, 22, 27, K.A_DARK)
    rect(px, 4, 16, 26, 18, K.ALLOY[3])                                 # mounting ears
    rect(px, 4, 16, 26, 16, K.A_LIGHT)
    rect(px, 4, 18, 26, 18, K.A_DARK)
    for ex in (5, 24):
        px[ex, 17] = K.A_DARK
        px[ex + 1, 17] = K.A_DARK
    rect(px, 10, 21, 20, 24, K.T_DEEP)                                  # label
    rect(px, 11, 22, 16, 22, K.T_TEAL)
    rect(px, 11, 23, 13, 23, K.T_CYAN)
    px[19, 22] = K.T_PLASMA
    rect(px, 13, 9, 17, 12, K.GRAY)                                     # output boss
    rect(px, 13, 9, 13, 12, K.GRAY_L)
    rect(px, 17, 9, 17, 12, K.GRAY_D)
    horn = (224, 226, 230, 255)
    rect(px, 5, 5, 25, 8, horn)                                         # horn
    rect(px, 5, 5, 25, 5, (246, 248, 250, 255))
    rect(px, 5, 8, 25, 8, K.GRAY)
    for cx in (4, 26):
        rect(px, cx, 6, cx, 7, K.GRAY_L)
    for hx in (7, 10, 20, 23):                                          # horn holes
        px[hx, 6] = K.GRAY_D
        px[hx, 7] = K.A_DARK
    rect(px, 14, 5, 16, 7, K.GOLD[0])                                   # horn screw
    px[14, 5] = K.GOLD[2]
    px[16, 7] = K.GOLD[1]
    rect(px, 23, 24, 24, 24, K.T_CYAN)                                  # lead
    rect(px, 25, 25, 26, 25, K.T_TEAL)
    rect(px, 27, 26, 27, 27, K.A_DARK)
    rim(img, 0.3)
    save_item(img, "servo")


def paint_capacitor():
    """Two-lead can in polymer cream with a polarity stripe."""
    base, light, dark, glint = CREAM
    img = K.new_img()
    px = img.load()
    for y in range(6, 22):
        for x in range(10, 22):
            off = (x - 15.5) / 6.0
            px[x, y] = (base if off < -0.75 else glint if off < -0.45 else light if off < -0.1
                        else base if off < 0.5 else dark)
    ellipse(px, 15.5, 6.0, 6.0, 2.4, light)                             # top cap
    ellipse(px, 15.5, 5.6, 4.6, 1.6, K.GRAY_L)
    rect(px, 13, 5, 18, 5, K.GRAY_D)                                    # vent score
    px[15, 4] = K.GRAY_D
    px[16, 6] = K.GRAY_D
    stripe = (112, 92, 70, 255)
    rect(px, 18, 8, 20, 21, stripe)                                     # polarity stripe
    for y in (10, 14, 18):
        rect(px, 18, y, 20, y, glint)
    rect(px, 10, 21, 21, 22, shadow(dark, 0.3))                         # crimped foot
    rect(px, 11, 22, 20, 22, K.A_DARK)
    rect(px, 12, 23, 13, 29, K.GRAY_L)                                  # leads (one long)
    rect(px, 13, 23, 13, 29, K.GRAY_D)
    rect(px, 18, 23, 19, 27, K.GRAY_L)
    rect(px, 19, 23, 19, 27, K.GRAY_D)
    rim(img, 0.25)
    save_item(img, "capacitor")


def paint_heat_sink():
    """The part: a bare finned block (the upgrade module of the same name is a card with a glyph)."""
    base, light, dark, glint = STEEL
    img = K.new_img()
    px = img.load()
    poly(px, [(3, 22), (25, 22), (29, 19), (7, 19)], light)             # base top
    rect(px, 3, 22, 25, 26, base)                                       # base front
    poly(px, [(26, 22), (29, 19), (29, 23), (26, 26)], dark)            # base side
    rect(px, 3, 22, 25, 22, glint)
    rect(px, 3, 26, 25, 26, dark)
    for i, fx in enumerate(range(3, 25, 4)):                            # six fins
        rect(px, fx + 3, 5, fx + 5, 18, dark)                           # fin side (depth)
        rect(px, fx + 4, 4, fx + 5, 4, K.mix(base, dark, 0.5))
        rect(px, fx, 7, fx + 2, 21, base)                               # fin face
        rect(px, fx, 7, fx, 21, glint)
        rect(px, fx + 1, 7, fx + 1, 21, light)
        rect(px, fx, 6, fx + 2, 6, glint)
        put(px, fx + 3, 5, light)
        put(px, fx + 1, 5, light)
        put(px, fx + 2, 5, light)
    rim(img, 0.3)
    save_item(img, "heat_sink")


# ---- focus lenses ----------------------------------------------------------------------------------
def _lens_mount(px):
    """The ring mount shared by all four lenses: alloy ring, keyed tab, two gold contacts."""
    for y in range(S):
        for x in range(S):
            d = math.hypot(x - 15.5, y - 14.5)
            if 9.6 < d <= 12.6:
                lit = (x - 15.5) + (y - 14.5)
                if d > 11.8:
                    col = K.A_LIGHT if lit < -6 else (K.A_DARK if lit > 6 else K.ALLOY[3])
                elif d > 10.4:
                    col = K.ALLOY[3] if lit < 0 else K.ALLOY[1]
                else:
                    col = K.A_DARK if lit < 0 else K.ALLOY[3]
                px[x, y] = col
    rect(px, 13, 27, 18, 29, K.ALLOY[1])                                # keyed tab
    rect(px, 13, 27, 18, 27, K.ALLOY[3])
    rect(px, 13, 29, 18, 29, K.A_DARK)
    rect(px, 15, 28, 16, 28, K.GOLD[0])
    px[5, 14] = K.GOLD[0]                                               # side contacts
    px[26, 14] = K.GOLD[1]


def paint_lens(name, cut):
    glass = rgba(K.LENS[name])
    hi = lift(glass, 0.5)
    lo = shadow(glass, 0.35)
    img = K.new_img()
    px = img.load()
    _lens_mount(px)
    cx, cy, R = 15.5, 14.5, 9.6
    for y in range(S):
        for x in range(S):
            dx, dy = x - cx, y - cy
            d = math.hypot(dx, dy)
            if d > R:
                continue
            ang = math.atan2(dy, dx)
            if cut == "round":                                          # plain polished disc
                col = glass
                if d > 8.2 and (dx + dy) > 4:
                    col = K.mix(glass, (150, 172, 190, 255), 0.6)
                elif d > 6.0 and (dx + dy) > 7:
                    col = K.mix(glass, (150, 172, 190, 255), 0.3)
                if 5.6 < d < 7.4 and -2.7 < ang < -1.7:
                    col = (255, 255, 255, 255)                           # one arc glint
            elif cut == "hex":                                          # six facets, hexagonal table
                sector = int(math.floor((ang + math.pi) / (math.pi / 3))) % 6
                local = (ang + math.pi) % (math.pi / 3) - math.pi / 6
                hex_r = d * math.cos(local)                              # distance to the hex edge
                tone = (hi, glass, lo, K.mix(glass, lo, 0.5), glass, lift(glass, 0.25))[sector]
                if hex_r > 8.2:
                    col = (22, 52, 96, 255)                              # outside the hexagon
                elif hex_r > 7.4:
                    col = hi
                elif hex_r < 3.6:
                    col = lift(glass, 0.35) if hex_r < 2.8 else hi       # the table
                else:
                    col = tone
                    if abs(local) > math.pi / 6 - 0.11:
                        col = hi                                         # facet ridges
            elif cut == "star":                                         # triangular starburst
                n = 8
                seg = (ang + math.pi + math.pi / n) % (2 * math.pi / n) - math.pi / n
                frac = abs(seg) / (math.pi / n)                          # 0 on a ray axis, 1 between rays
                reach = 2.2 + (R - 2.2) * (1.0 - frac)
                if d <= 1.6:
                    col = (255, 226, 170, 255)
                elif d <= reach:
                    col = hi if seg < 0 else glass
                    if d > reach - 1.0:
                        col = glass if seg < 0 else lo
                else:
                    col = (92, 34, 22, 255)                              # dark between the rays
            else:                                                       # annular: dark centre
                if d < 3.6:
                    col = (16, 12, 34, 255)
                elif d < 4.6:
                    col = lo
                elif d > 8.6:
                    col = K.T_PLASMA if (-2.9 < ang < -1.2) else (K.T_TEAL if -3.3 < ang < -0.8 or ang > 2.9
                                                                else lo)
                else:
                    col = glass
                    if 5.4 < d < 6.6 and (ang < -1.4 or ang > 2.6):
                        col = lift(glass, 0.3)
                    if d > 7.4 and (dx + dy) > 5:
                        col = lo
            px[x, y] = col
    if cut == "annular":
        px[12, 8] = K.T_GLOW
        px[11, 9] = K.T_GLOW
    save_item(img, name)


# ---- drill heads -----------------------------------------------------------------------------------
def _shank(px):
    for y in range(2, 8):                                               # threaded pin
        for x in range(13, 19):
            px[x, y] = (K.A_LIGHT if x < 15 else K.ALLOY[3]) if y % 2 == 0 else (
                K.ALLOY[3] if x < 15 else K.ALLOY[2])
    rect(px, 18, 2, 18, 7, K.A_DARK)
    rect(px, 10, 8, 21, 10, K.ALLOY[3])                                 # collar
    rect(px, 10, 8, 21, 8, K.A_LIGHT)
    rect(px, 10, 10, 21, 10, K.A_DARK)
    rect(px, 19, 9, 21, 9, K.ALLOY[2])


def paint_drill_tricone():
    """Iron: three toothed roller cones under a three-legged body."""
    base, light, dark, glint = STEEL
    img = K.new_img()
    px = img.load()
    _shank(px)
    poly(px, [(9, 11), (23, 11), (25, 19), (7, 19)], base)              # body
    rect(px, 9, 11, 13, 18, light)
    rect(px, 20, 11, 24, 18, dark)
    rect(px, 14, 12, 14, 18, K.A_DARK)                                  # leg slots
    rect(px, 19, 12, 19, 18, K.A_DARK)
    px[16, 14] = K.T_TEAL                                               # flush port
    px[17, 14] = K.T_DEEP
    edge = shadow(dark, 0.6)
    for (cx, cy, r, dim) in ((9.0, 21.5, 5.6, 0.12), (23.0, 21.5, 5.6, 0.3), (16.0, 24.0, 5.6, 0.0)):
        for y in range(S):
            for x in range(S):
                dx, dy = x - cx, y - cy
                d = math.hypot(dx, dy)
                if d > r:
                    continue
                if d > r - 1.0:
                    col = edge                                          # each cone keeps its outline
                elif d < 1.2:
                    col = glint                                         # nose
                elif 2.0 <= d < 3.0:
                    col = dark                                          # groove between tooth rows
                else:
                    teeth = 5 if d < 2.0 else 9
                    a = (math.atan2(dy, dx) + math.pi) / (2 * math.pi) * teeth
                    col = (glint if dx + dy < 0 else light) if (a - math.floor(a)) < 0.55 else base
                px[x, y] = K.mix(col, K.A_DARK, dim) if dim else col
    rim(img, 0.3)
    save_item(img, "iron_drill_head")


def paint_drill_blades(name, blades, ramp, tips=None, stepped=False):
    """Fixed-cutter head: ``blades`` ridges with flutes between, stepped or tapered to the pilot."""
    base, light, dark, glint = ramp
    img = K.new_img()
    px = img.load()
    _shank(px)
    flute = shadow(dark, 0.55)
    for y in range(11, 29):
        if stepped:
            hw = 9.5 if y < 17 else (6.5 if y < 22 else (3.5 if y < 26 else 1.5))
        else:
            hw = 9.5 * math.sqrt(max(0.02, 1.0 - ((y - 11) / 18.5) ** 2)) if y > 13 else 9.5
        for x in range(S):
            off = x - 15.5
            if abs(off) > hw:
                continue
            pos = (off + hw) / (2 * hw) * blades                        # which ridge
            frac = pos - math.floor(pos)
            if frac < 0.24 and hw > 2:
                col = flute
            elif frac < 0.5:
                col = glint
            elif frac < 0.8:
                col = light
            else:
                col = base
            px[x, y] = col
    edges = (16, 21, 25, 28) if stepped else ()
    for ey in edges:                                                    # cutting edge of each step
        for x in range(S):
            if px[x, ey][3] and px[x, ey] != flute:
                px[x, ey] = glint
            if ey + 1 < S and px[x, ey][3] and (px[x, ey + 1][3] == 0):
                px[x, ey] = K.mix(glint, dark, 0.35)
    if tips is not None:                                                # cutter studs on every ridge
        for y in range(12, 28, 3):
            hw = 9.5 * math.sqrt(max(0.02, 1.0 - ((y - 11) / 18.5) ** 2)) if y > 13 else 9.5
            for b in range(blades):
                x = int(round(15.5 - hw + (b + 0.36) * (2 * hw) / blades))
                if 0 <= x < S and px[x, y][3]:
                    px[x, y] = tips[1]
                    if px[x, y + 1][3]:
                        px[x, y + 1] = tips[0]
        px[15, 28] = tips[1]
        px[16, 28] = tips[0]
    rim(img, 0.35)
    save_item(img, name)


# ---- upgrade modules -------------------------------------------------------------------------------
def _glyph_fins(px):
    """Heat sink: five fins on a base bar."""
    for x in range(10, 21):
        px[x, 19] = K.T_TEAL
        px[x, 20] = K.T_DEEP
    for fx in (11, 13, 15, 17, 19):
        for y in range(11, 19):
            px[fx, y] = K.T_CYAN
        px[fx, 10] = K.T_PLASMA


def _glyph_cowl(px):
    """Scrubber: a vent cowl -- hood, brim, two louvres on a stem."""
    hood = ("....####....",
            "..##....##..",
            ".#........#.",
            ".#........#.",
            "############",
            "............",
            "..########..",
            "............",
            "...######...",
            "....#..#....",
            "....#..#....")
    for j, row in enumerate(hood):
        for i, ch in enumerate(row):
            if ch == "#":
                col = K.T_CYAN if j <= 4 else K.T_TEAL
                px[10 + i, 10 + j] = col
    px[14, 10] = K.T_PLASMA
    px[15, 10] = K.T_PLASMA
    px[13, 12] = K.T_TEAL                                               # air under the hood
    px[15, 13] = K.T_TEAL
    px[18, 12] = K.T_TEAL


def paint_module(name, glyph):
    old = G.FORCE
    G.FORCE = old or FORCE_FLAG in sys.argv
    try:
        G.gen_module(name, glyph)                                       # the shared card frame
    finally:
        G.FORCE = old


# ---- buckets ---------------------------------------------------------------------------------------
def paint_bucket(name, medium):
    """The familiar pail: handle, open mouth showing the liquid, tapered body with one hoop."""
    base, light, dark, deep = BUCKET
    fluid = rgba(K.MEDIA[medium])
    f_hi = lift(fluid, 0.4)
    f_lo = shadow(fluid, 0.3)
    img = K.new_img()
    px = img.load()
    for y in range(S):                                                  # handle, behind the mouth
        for x in range(S):
            v = math.hypot((x - 15.5) / 10.2, (y - 10.5) / 8.4)
            if y < 10 and abs(v - 1.0) * 8.4 <= 0.95:
                px[x, y] = light if x < 14 else (base if x < 22 else dark)
    for y in range(10, 29):                                             # body
        t = (y - 10) / 18.0
        hw = 10.4 - 3.4 * t
        if y > 25:
            hw *= math.sqrt(max(0.0, 1.0 - ((y - 25) / 3.6) ** 2))
        for x in range(S):
            off = (x - 15.5) / max(hw, 0.01)
            if abs(x - 15.5) <= hw:
                col = (dark if off < -0.86 else light if off < -0.5 else base if off < 0.35
                       else dark if off < 0.8 else deep)
                px[x, y] = col
    for x in range(S):                                                  # hoop
        if px[x, 19][3]:
            px[x, 19] = shadow(px[x, 19], 0.3)
            px[x, 20] = lift(px[x, 20], 0.25)
    for y in range(S):                                                  # mouth: rim, then liquid
        for x in range(S):
            v = math.hypot((x - 15.5) / 10.6, (y - 10.0) / 4.3)
            if v <= 1.0:
                if v > 0.80:
                    px[x, y] = light if y < 10 else (255, 255, 255, 255) if x < 12 else base
                else:
                    col = fluid
                    if y <= 8:
                        col = f_lo                                       # shaded by the far wall
                    elif y >= 12 or (x < 13 and y >= 10):
                        col = f_hi
                    px[x, y] = col
    if medium == "cryo_coolant":                                        # frost flecks
        for (sx, sy) in ((11, 10), (17, 9), (20, 11), (14, 12)):
            px[sx, sy] = K.FROST[0]
        px[18, 9] = K.FROST[1]
    elif medium == "sulfuric_acid":                                     # bubbles
        for (sx, sy) in ((12, 10), (18, 11)):
            px[sx, sy] = (248, 255, 190, 255)
            px[sx + 1, sy] = f_hi
            px[sx, sy + 1] = f_hi
            px[sx + 1, sy + 1] = f_lo
    else:                                                               # resin swirl
        swirl = K.mix(fluid, (150, 112, 70, 255), 0.55)
        for (sx, sy) in ((10, 10), (11, 11), (12, 11), (13, 11), (14, 10), (15, 10), (16, 10),
                         (17, 11), (18, 11), (19, 10), (13, 9), (20, 10)):
            px[sx, sy] = swirl
    rim(img, 0.3)
    save_item(img, name)


# ---- package entry ---------------------------------------------------------------------------------
ITEMS = (
    "cindersteel_ingot", "cindersteel_plate", "cindersteel_rod", "titanium_plate",
    "synthetic_glacite", "crystal_seed", "crystal_boule", "wafer", "sulfur_dust", "polymer_sheet",
    "europium_dust", "logic_chip", "quantum_chip",
    "copper_wire", "cindersteel_wire", "superconductor_wire",
    "copper_coil", "cindersteel_coil", "superconductor_coil",
    "servo", "capacitor", "heat_sink",
    "quartz_focus_lens", "glacite_focus_lens", "ember_focus_lens", "void_focus_lens",
    "iron_drill_head", "cindersteel_drill_head", "glacite_drill_head",
    "heat_sink_module", "scrubber_module",
    "cryo_coolant_bucket", "sulfuric_acid_bucket", "polymer_resin_bucket",
)


def paint():
    paint_ingot("cindersteel_ingot", CIN)
    paint_plate("cindersteel_plate", CIN)
    paint_rod("cindersteel_rod", CIN)
    paint_plate("titanium_plate", TI, chamfer=True, brushed=True)
    paint_synthetic_glacite()
    paint_crystal_seed()
    paint_crystal_boule()
    paint_wafer()
    paint_dust("sulfur_dust", SULFUR, spread=0.95, crystals=((11, 22), (20, 19)))
    paint_polymer_sheet()
    paint_dust("europium_dust", EURO, spread=0.75, spark=K.T_CYAN)
    paint_logic_chip()
    paint_quantum_chip()
    paint_wire("copper_wire", COP)
    paint_wire("cindersteel_wire", CIN)
    paint_wire("superconductor_wire", SUP)
    paint_coil("copper_coil", COP)
    paint_coil("cindersteel_coil", CIN)
    paint_coil("superconductor_coil", SUP)
    paint_servo()
    paint_capacitor()
    paint_heat_sink()
    paint_lens("quartz_focus_lens", "round")
    paint_lens("glacite_focus_lens", "hex")
    paint_lens("ember_focus_lens", "star")
    paint_lens("void_focus_lens", "annular")
    paint_drill_tricone()
    paint_drill_blades("cindersteel_drill_head", 4, CIN, stepped=True)
    paint_drill_blades("glacite_drill_head", 6, STEEL, tips=K.FROST)
    paint_module("heat_sink_module", _glyph_fins)
    paint_module("scrubber_module", _glyph_cowl)
    paint_bucket("cryo_coolant_bucket", "cryo_coolant")
    paint_bucket("sulfuric_acid_bucket", "sulfuric_acid")
    paint_bucket("polymer_resin_bucket", "polymer_resin")


PULSE = []          # item icons do not animate
