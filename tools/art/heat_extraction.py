"""A5 Heat + Extraction art package: Heat Pipe, Heat Exchanger, Deep Core Drill, Drill Derrick and
the Survey Scanner item.

Family trim: lagged copper (Heat) and bare lattice steel (Extraction), both non-emissive. The one
hazard band is the collar round the Deep Core Drill's deck (moving machinery).

* ``paint()`` paints every texture of the package (additive, like every painter).
* ``python tools/art/heat_extraction.py --models`` rewrites the package's block and item models
  (they are generated here so the UV maps and the textures cannot drift apart). The blockstates are
  not touched: their properties belong to the code.

Heat Pipe. One 32x texture per heat level (``heat_pipe_0..3``), laid out as an atlas the models
address with explicit UVs: barrel, sleeve, sleeve cap, heat band, core face. Levels 2 and 3 light the
band (and the core at 3) through ``light_emission``; the colours are bright copper, never one of the
renderer-only heat colours.

Renderer sprites (white, tinted in code): ``heat_exchanger_chevron``, ``deep_core_drill_table``,
``deep_core_drill_string``, ``deep_core_drill_head``, ``deep_core_drill_puff``.
"""
import json
import math
import os
import sys

if __name__ == "__main__":
    sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from art import common as K

S = K.S
COP, COP_D, COP_L = K.COPPER[0], K.COPPER[1], K.COPPER[2]
COP_DD = K.mix(COP_D, K.A_DARK, 0.45)
LAG0, LAG1 = K.LAG
LAG_D = K.mix(LAG0, K.A_DARK, 0.5)
LAG_L = K.mix(LAG1, K.A_LIGHT, 0.35)
IRON, IRON_D, IRON_L = K.IRON[0], K.IRON[1], K.IRON[2]
IRON_DD = K.mix(IRON_D, K.A_DARK, 0.5)
BORE = (10, 13, 16, 255)
PANEL = (16, 20, 24, 255)
WHITE = (255, 255, 255, 255)

# Heat Pipe band / window colours per level: (body, glint). Bright copper at the top, not H_*.
PIPE_HEAT = (
    ((44, 48, 52, 255), (60, 64, 68, 255)),
    ((124, 62, 38, 255), (152, 82, 50, 255)),
    ((226, 128, 62, 255), (244, 168, 100, 255)),
    ((255, 172, 100, 255), (255, 224, 172, 255)),
)
PIPE_WARM = (None, None, ((240, 140, 70, 255), 0.35), ((255, 176, 108, 255), 0.72))
PIPE_EMIT_BAND = (0, 0, 8, 15)
PIPE_EMIT_CORE = (0, 0, 3, 9)


def _hline(px, x0, x1, y, col):
    for x in range(x0, x1 + 1):
        px[x, y] = col


def _vline(px, x, y0, y1, col):
    for y in range(y0, y1 + 1):
        px[x, y] = col


def _rect(px, x0, y0, x1, y1, col):
    for y in range(y0, y1 + 1):
        _hline(px, x0, x1, y, col)


def _frame(px, x0, y0, x1, y1, light, dark):
    _hline(px, x0, x1, y0, light)
    _vline(px, x0, y0, y1, light)
    _hline(px, x0, x1, y1, dark)
    _vline(px, x1, y0, y1, dark)


def _noise(px, rng, x0, y0, x1, y1, ramp):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            px[x, y] = rng.choice(ramp)


def _disc(px, cx, cy, r, col):
    """Filled disc about the point (cx, cy) in texel-centre coordinates."""
    for y in range(int(cy - r - 1), int(cy + r + 2)):
        for x in range(int(cx - r - 1), int(cx + r + 2)):
            if 0 <= x < S and 0 <= y < S and (x + 0.5 - cx) ** 2 + (y + 0.5 - cy) ** 2 <= r * r:
                px[x, y] = col


# ---- Heat Pipe ----------------------------------------------------------------------------------
def _pipe(level):
    name = "heat_pipe_%d" % level
    img = K.new_img()
    px = img.load()
    rng = K.rng_for(name)
    if level == 0:                                   # cold: dull, slightly patinated copper
        dull = (92, 86, 82, 255)
        base, dark, light = (K.mix(c, dull, 0.5) for c in (COP, COP_D, COP_L))
    elif PIPE_WARM[level] is None:
        base, dark, light = (K.mix(c, (92, 86, 82, 255), 0.2) for c in (COP, COP_D, COP_L))
    else:
        warm, t = PIPE_WARM[level]
        base, dark, light = (K.mix(c, warm, t) for c in (COP, COP_D, COP_L))
    deep = K.mix(dark, K.A_DARK, 0.45)
    body, glint = PIPE_HEAT[level]

    # A barrel: x 0..9, y 0..7 (length along x)
    ramp = (light, base, base, K.mix(base, light, 0.3), base, base, dark, deep)
    for y in range(8):
        for x in range(10):
            px[x, y] = ramp[y] if rng.random() > 0.12 else K.mix(ramp[y], dark, 0.4)
    _vline(px, 9, 1, 6, dark)                        # seam where the arm meets the node
    # E barrel end: x 12..19, y 0..7
    _rect(px, 12, 0, 19, 7, base)
    _frame(px, 12, 0, 19, 7, light, dark)
    _rect(px, 14, 2, 17, 5, BORE)
    # B sleeve (lagging): x 0..5, y 8..17
    for y in range(8, 18):
        for x in range(6):
            px[x, y] = rng.choice((LAG0, LAG0, LAG1))
    _hline(px, 0, 5, 8, LAG_L)
    _hline(px, 0, 5, 17, LAG_D)
    _vline(px, 0, 8, 17, LAG_D)                      # straps
    _vline(px, 5, 8, 17, LAG_D)
    if level == 3:                                   # scorched lagging beside the glowing band
        _vline(px, 2, 9, 16, K.mix(LAG0, (60, 34, 24, 255), 0.6))
        _vline(px, 3, 9, 16, K.mix(LAG0, (60, 34, 24, 255), 0.6))
    # C sleeve cap: x 8..17, y 8..17
    _rect(px, 8, 8, 17, 17, LAG0)
    _frame(px, 8, 8, 17, 17, LAG_L, LAG_D)
    _rect(px, 10, 10, 15, 15, dark)
    # F heat band: x 20..22, y 8..18
    _rect(px, 20, 8, 22, 18, body)
    _vline(px, 21, 9, 13, glint)
    px[20, 15] = glint
    px[22, 10] = glint
    # D core face: x 0..11, y 18..29
    _noise(px, rng, 0, 18, 11, 29, (base, base, K.mix(base, dark, 0.5)))
    _frame(px, 0, 18, 11, 29, light, deep)
    for cx, cy in ((1, 19), (9, 19), (1, 27), (9, 27)):     # lagging clamps at the corners
        _rect(px, cx, cy, cx + 1, cy + 1, LAG1)
        px[cx + 1, cy + 1] = LAG_D
    _rect(px, 3, 21, 8, 26, K.A_DARK)                # heat window
    _rect(px, 4, 22, 7, 25, body)
    px[4, 22] = glint
    px[5, 22] = glint
    px[4, 23] = glint
    K.save_block(img, name)


# ---- Heat Exchanger -----------------------------------------------------------------------------
def _manifold(px, rng, front):
    """A lagged copper header, model y 2..14 = rows 4..27, with the port flange in the middle."""
    _rect(px, 0, 0, S - 1, 3, COP_D)                 # rows 0..3: only the top rail's end shows them
    _hline(px, 0, S - 1, 0, COP_L)
    _noise(px, rng, 0, 4, S - 1, 27, (LAG0, LAG0, LAG1))
    K.family_trim(px, "heat", y=4, x0=0, x1=S - 1)
    K.family_trim(px, "heat", y=26, x0=0, x1=S - 1)
    for x in (0, S - 1):
        _vline(px, x, 4, 27, LAG_D)
    for y in range(8, 25, 4):                        # stitched lagging seams
        for x in range(1, S - 1):
            if x % 2:
                px[x, y] = LAG_D
    _rect(px, 6, 6, 25, 25, COP)                     # flange plate behind the port
    _noise(px, rng, 7, 7, 24, 24, (COP, COP, COP_D))
    _frame(px, 6, 6, 25, 25, COP_L, COP_DD)
    for bx, by in ((7, 7), (23, 7), (7, 23), (23, 23)):
        px[bx, by] = COP_L
        px[bx + 1, by] = COP_DD
        px[bx, by + 1] = COP_DD
        px[bx + 1, by + 1] = COP_DD
    _disc(px, 16, 16, 6.2, COP_DD)                   # the port shadow
    if front:
        K.status_socket(px)
        K.led(px, 2, 12, K.T_CYAN, K.T_PLASMA)       # two flow tell-tales (PULSE)
        K.led(px, 2, 18, K.T_TEAL)


def _exchanger():
    name = "heat_exchanger"
    for suffix, front in (("_front", True), ("_back", False)):
        img = K.machine_base(name + suffix)
        _manifold(img.load(), K.rng_for(name + suffix + "/m"), front)
        K.save_block(img, name + suffix)

    # side: base slab rows 28..31; the two manifold ends are columns 0..2 and 29..31
    img = K.machine_base(name)
    px = img.load()
    rng = K.rng_for(name + "/s")
    _rect(px, 0, 0, S - 1, 27, K.ALLOY[2])
    for x0 in (0, 29):
        _noise(px, rng, x0, 4, x0 + 2, 27, (LAG0, LAG1))
        _hline(px, x0, x0 + 2, 4, COP_L)
        _hline(px, x0, x0 + 2, 5, COP_D)
        _hline(px, x0, x0 + 2, 26, COP)
        _hline(px, x0, x0 + 2, 27, COP_D)
    _hline(px, 0, S - 1, 28, K.A_LIGHT)
    K.save_block(img, name)

    # top: the floor between the fins, the manifold tops along the north and south edges
    img = K.machine_base(name + "_top")
    px = img.load()
    rng = K.rng_for(name + "/t")
    _noise(px, rng, 2, 3, 29, 28, (PANEL, PANEL, K.A_DARK))
    for y0 in (0, 29):
        _noise(px, rng, 0, y0, S - 1, y0 + 2, (LAG0, LAG1))
    _hline(px, 0, S - 1, 0, COP_L)
    _hline(px, 0, S - 1, 31, COP_D)
    K.save_block(img, name + "_top")

    # fin: plate at x 4..27, y 4..27 (uv 2..14); edge strips in column 0..1 and row 0..1
    img = K.new_img()
    px = img.load()
    rng = K.rng_for(name + "_fin")
    plate = (K.mix(K.A_LIGHT, K.ALLOY[3], 0.55), K.mix(K.A_LIGHT, K.ALLOY[3], 0.75), K.ALLOY[3])
    _noise(px, rng, 0, 0, S - 1, S - 1, plate)
    for x in range(4, 28, 4):                        # pressed ribs
        _vline(px, x, 4, 27, K.mix(K.ALLOY[3], K.A_DARK, 0.35))
        _vline(px, x + 1, 4, 27, K.A_LIGHT)
    for cy in (8, 15, 22):                           # copper tubes through the plate
        for cx in (9, 17, 25):
            _rect(px, cx - 1, cy - 1, cx + 2, cy + 2, K.A_DARK)
            _rect(px, cx, cy, cx + 1, cy + 1, COP)
            px[cx, cy] = COP_L
    _rect(px, 0, 0, 1, S - 1, K.A_LIGHT)             # edge strips
    _rect(px, 0, 0, S - 1, 1, K.A_LIGHT)
    for i in range(3, S, 4):
        px[1, i] = K.ALLOY[3]
        px[i, 1] = K.ALLOY[3]
    K.save_block(img, name + "_fin")

    # rail: plain tileable copper (top rail and the two tie rails the chevrons run along)
    img = K.new_img()
    px = img.load()
    rng = K.rng_for(name + "_rail")
    _noise(px, rng, 0, 0, S - 1, S - 1, (COP, COP, COP_D, K.mix(COP, COP_D, 0.5)))
    for y in range(S):
        for x in range(S):
            if (x + y) % 8 == 0 and rng.random() < 0.5:
                px[x, y] = COP_DD
    K.save_block(img, name + "_rail")

    # port: a round copper collar with a dark bore, centred (north-face plan)
    img = K.new_img()
    px = img.load()
    rng = K.rng_for(name + "_port")
    _noise(px, rng, 0, 0, S - 1, S - 1, (COP, COP_D))
    _disc(px, 16, 16, 7.2, COP_L)
    _disc(px, 16, 16, 6.2, COP)
    _disc(px, 16, 16, 4.4, COP_DD)
    _disc(px, 16, 16, 3.4, BORE)
    px[14, 14] = K.T_DEEP
    px[15, 14] = K.T_DEEP
    K.save_block(img, name + "_port")

    # chevron (renderer sprite, white): points to the top of the texture
    img = K.new_img()
    px = img.load()
    for y in range(S):
        for x in range(S):
            d = abs(x - 15.5)
            edge = 6 + d                              # the chevron's leading edge at this column
            if d < 13 and edge <= y < edge + 9:
                px[x, y] = WHITE if y < edge + 7 else (206, 206, 206, 255)
    K.save_block(img, name + "_chevron")


# ---- Deep Core Drill ----------------------------------------------------------------------------
def _hazard(px, y0=22, y1=25):
    K.haz_band(px, y0, y1, 0, S - 1)
    _hline(px, 0, S - 1, y0 - 1, K.A_DARK)
    _hline(px, 0, S - 1, y1 + 1, K.A_DARK)


def _drill():
    name = "deep_core_drill"
    # side / back: the deck is model y 0..5 = rows 22..31; the collar band is rows 22..25
    img = K.machine_base(name)
    px = img.load()
    K.family_trim(px, "extraction")
    _hazard(px)
    for x in range(5, 27, 7):                        # deck bolts under the band
        px[x, 28] = K.A_LIGHT
        px[x + 1, 28] = K.A_DARK
    K.save_block(img, name)

    # front: same deck, plus the instrument post (model x 0.5..3.5 = cols 25..30) with the socket
    img = K.machine_base(name + "_front")
    px = img.load()
    K.family_trim(px, "extraction")
    _hazard(px)
    for y in range(2, 22):                           # post
        px[25, y] = K.A_LIGHT
        px[30, y] = K.A_DARK
        for x in range(26, 30):
            px[x, y] = K.ALLOY[(x + y) % len(K.ALLOY)]
    _hline(px, 25, 30, 2, K.A_LIGHT)
    for y in (11, 15, 19):
        _hline(px, 27, 28, y, K.A_DARK)
    K.status_socket(px)
    K.led(px, 5, 27, K.T_CYAN, K.T_PLASMA)           # deck tell-tales (PULSE)
    K.led(px, 11, 27, K.T_TEAL)
    K.led(px, 17, 27, K.T_TEAL)
    K.save_block(img, name + "_front")

    # top: deck plan. Kelly hole cols/rows 12..19, table seat 8..23
    img = K.machine_base(name + "_top")
    px = img.load()
    for y in range(4, 28, 3):                        # tread plate
        for x in range(4 + (y % 2), 28, 4):
            px[x, y] = K.A_DARK
    _rect(px, 8, 8, 23, 23, IRON_D)
    _frame(px, 8, 8, 23, 23, IRON_L, IRON_DD)
    _rect(px, 10, 10, 21, 21, IRON)
    _frame(px, 10, 10, 21, 21, IRON_DD, IRON_L)
    _rect(px, 12, 12, 19, 19, BORE)
    K.save_block(img, name + "_top")

    # motor: tileable housing skin (louvres + a rivet line)
    img = K.new_img()
    px = img.load()
    rng = K.rng_for(name + "_motor")
    K.noise_fill(img, K.ALLOY, rng)
    for y in range(S):
        if y % 4 == 1:
            _hline(px, 0, S - 1, y, K.A_DARK)
        elif y % 4 == 2:
            _hline(px, 0, S - 1, y, K.mix(K.A_LIGHT, K.A_DARK, 0.55))
    for x in range(0, S, 8):
        _vline(px, x, 0, S - 1, K.ALLOY[3])
        _vline(px, x + 1, 0, S - 1, K.ALLOY[2])
        for y in range(3, S, 8):
            px[x, y] = K.A_LIGHT
    K.save_block(img, name + "_motor")

    # bore: the inside of the kelly hole and of the exhaust stub
    img = K.new_img()
    px = img.load()
    rng = K.rng_for(name + "_bore")
    _noise(px, rng, 0, 0, S - 1, S - 1, (BORE, BORE, K.A_DARK))
    K.save_block(img, name + "_bore")

    # --- renderer sprites (white / grey; tinted and lit in code) ---
    # table: the rotary table seen from above; slots and one index mark so the turning reads
    img = K.new_img()
    px = img.load()
    _rect(px, 0, 0, S - 1, S - 1, (196, 196, 196, 255))
    _frame(px, 0, 0, S - 1, S - 1, WHITE, (120, 120, 120, 255))
    _frame(px, 1, 1, S - 2, S - 2, (232, 232, 232, 255), (140, 140, 140, 255))
    for i in range(4, 28):                           # diagonal drive slots
        for d in (0, 1):
            if not 12 <= i <= 19:
                px[i, i + d - (1 if i > 15 else 0)] = (70, 70, 70, 255)
                px[S - 1 - i, i + d - (1 if i > 15 else 0)] = (70, 70, 70, 255)
    _rect(px, 11, 11, 20, 20, (150, 150, 150, 255))
    _frame(px, 11, 11, 20, 20, (90, 90, 90, 255), (232, 232, 232, 255))
    _rect(px, 13, 13, 18, 18, (40, 40, 40, 255))
    _rect(px, 14, 2, 17, 6, (60, 60, 60, 255))       # index mark
    K.save_block(img, name + "_table")

    # string: drill pipe, tileable vertically; a slanted wear band makes the rotation visible
    img = K.new_img()
    px = img.load()
    rng = K.rng_for(name + "_string")
    for y in range(S):
        for x in range(S):
            shade = (236, 214, 196, 176, 150)[min(4, x * 5 // S)]
            if ((x + y * 2) % S) < 5:
                shade -= 54
            elif rng.random() < 0.08:
                shade -= 16
            px[x, y] = (shade, shade, shade, 255)
    for y in (0, 1, 16, 17):                         # tool joints twice per block
        for x in range(S):
            v = px[x, y][0]
            px[x, y] = (max(40, v - 70),) * 3 + (255,)
    K.save_block(img, name + "_string")

    # head: the bit; cutter teeth round the edge
    img = K.new_img()
    px = img.load()
    _rect(px, 0, 0, S - 1, S - 1, (214, 214, 214, 255))
    for i in range(0, S, 4):
        for a, b in ((i, 0), (i, S - 2), (0, i), (S - 2, i)):
            _rect(px, a, b, min(S - 1, a + 1), min(S - 1, b + 1), WHITE)
        for a, b in ((i + 2, 0), (i + 2, S - 2), (0, i + 2), (S - 2, i + 2)):
            if a < S and b < S:
                _rect(px, a, b, min(S - 1, a + 1), min(S - 1, b + 1), (96, 96, 96, 255))
    for y in range(6, 26):
        for x in range(6, 26):
            if (x + y) % 6 < 2:
                px[x, y] = (128, 128, 128, 255)
    K.save_block(img, name + "_head")

    # puff: exhaust, a dithered blob (cutout)
    img = K.new_img()
    px = img.load()
    rng = K.rng_for(name + "_puff")
    for y in range(S):
        for x in range(S):
            r = math.hypot(x - 15.5, y - 15.5) / 15.5
            lump = 0.12 * math.sin(x * 0.9) * math.cos(y * 0.7)
            if r + lump < 0.55 or (r + lump < 0.92 and (x + y) % 2 == 0 and rng.random() < 0.8):
                v = 255 if r < 0.4 else rng.choice((226, 240))
                px[x, y] = (v, v, v, 255)
    K.save_block(img, name + "_puff")


# ---- Drill Derrick ------------------------------------------------------------------------------
def _derrick():
    img = K.new_img()
    px = img.load()
    rng = K.rng_for("drill_derrick")
    for y in range(S):
        for x in range(S):
            col = rng.choice((IRON, IRON, IRON_D))
            if x % 4 == 0:
                col = IRON_L
            elif x % 4 == 3:
                col = IRON_DD
            px[x, y] = col
    for y in range(2, S, 8):                         # rivets down every member
        for x in range(1, S, 4):
            px[x, y] = IRON_DD
            px[x, y + 1] = IRON_L
    for y0 in (0, 28):                               # girts (rows 0..3) and base plates (rows 28..31)
        _noise(px, rng, 0, y0, S - 1, y0 + 3, (IRON, IRON_D))
        _hline(px, 0, S - 1, y0, IRON_L)
        _hline(px, 0, S - 1, y0 + 3, IRON_DD)
        for x in range(3, S, 6):
            px[x, y0 + 1] = IRON_DD
    K.save_block(img, "drill_derrick")


# ---- Survey Scanner (item; one 32x atlas for the hand-written element model) -----------------------
def _scanner():
    img = K.new_img()
    px = img.load()
    rng = K.rng_for("survey_scanner")
    # pad top: x 0..19, y 0..27 (model 10 x 14)
    _noise(px, rng, 0, 0, 19, 27, K.ALLOY)
    _frame(px, 0, 0, 19, 27, K.A_LIGHT, K.A_DARK)
    _rect(px, 2, 2, 17, 13, PANEL)                   # screen
    _frame(px, 2, 2, 17, 13, K.A_DARK, K.mix(K.A_LIGHT, K.A_DARK, 0.5))
    for r, col in ((5.2, K.T_DEEP), (3.6, K.T_TEAL), (1.9, K.T_DEEP)):   # sonar rings
        for a in range(0, 360, 6):
            x = int(10 + r * math.cos(math.radians(a)))
            y = int(8 + r * 0.85 * math.sin(math.radians(a)))
            if 3 <= x <= 16 and 3 <= y <= 12:
                px[x, y] = col if r != 3.6 else K.T_TEAL
    px[10, 8] = K.T_PLASMA
    px[13, 6] = K.T_CYAN                             # a find
    px[14, 6] = K.T_CYAN
    for i, x in enumerate((3, 7, 11, 15)):           # four keys
        _rect(px, x, 16, x + 2, 18, K.ALLOY[3])
        px[x, 16] = K.A_LIGHT
        px[x + 2, 18] = K.A_DARK
    _rect(px, 3, 21, 16, 22, K.T_DEEP)               # charge bar
    _hline(px, 3, 11, 21, K.T_CYAN)
    _hline(px, 3, 11, 22, K.T_TEAL)
    for x in range(4, 16, 3):                        # grip ribs
        _vline(px, x, 24, 26, K.A_DARK)
    # pad edge / underside: x 20..31, y 0..11
    _noise(px, rng, 20, 0, 31, 11, (K.ALLOY[2], K.ALLOY[0]))
    _hline(px, 20, 31, 0, K.A_LIGHT)
    _hline(px, 20, 31, 11, K.A_DARK)
    # dish face: x 20..31, y 12..23 (model 6 x 6)
    _rect(px, 20, 12, 31, 23, K.ALLOY[2])
    for r, col in ((5.9, K.A_LIGHT), (5.0, IRON), (3.6, IRON_D), (2.2, IRON), (0.9, K.T_CYAN)):
        for y in range(12, 24):
            for x in range(20, 32):
                if (x - 25.5) ** 2 + (y - 17.5) ** 2 <= r * r:
                    px[x, y] = col
    px[25, 17] = K.T_PLASMA
    # dish back + arm: x 20..31, y 24..31
    _noise(px, rng, 20, 24, 31, 31, (IRON_D, IRON))
    _hline(px, 20, 31, 24, IRON_L)
    _hline(px, 20, 31, 31, IRON_DD)
    # spare alloy: x 0..19, y 28..31
    _noise(px, rng, 0, 28, 19, 31, (K.ALLOY[2], K.ALLOY[0]))
    K.save(img, "item", "survey_scanner")


def paint():
    for level in range(4):
        _pipe(level)
    _exchanger()
    _drill()
    _derrick()
    _scanner()


PULSE = [K.pulse("heat_exchanger_front"), K.pulse("deep_core_drill_front")]


# ---- models (python tools/art/heat_extraction.py --models) ---------------------------------------
def _f(tex, uv=None, rot=None, cull=None):
    face = {"texture": "#" + tex}
    if uv is not None:
        face["uv"] = uv
    if rot:
        face["rotation"] = rot
    if cull:
        face["cullface"] = cull
    return face


def _el(name, frm, to, faces, **extra):
    el = {"name": name, "from": frm, "to": to}
    el.update(extra)
    el["faces"] = faces
    return el


def _all(tex, skip=()):
    return {d: _f(tex) for d in ("north", "south", "east", "west", "up", "down") if d not in skip}


def _pipe_models():
    models = {}
    for level in range(4):
        tex = {"pipe": "nerotech:block/heat_pipe_%d" % level, "particle": "nerotech:block/heat_pipe_%d" % level}
        band = {"shade": False}
        if PIPE_EMIT_BAND[level]:
            band["light_emission"] = PIPE_EMIT_BAND[level]
        core = {}
        if PIPE_EMIT_CORE[level]:
            core["light_emission"] = PIPE_EMIT_CORE[level]
        models["block/heat_pipe_core_%d" % level] = {
            "parent": "minecraft:block/block", "textures": tex,
            "elements": [_el("node", [5, 5, 5], [11, 11, 11],
                             {d: _f("pipe", [0, 9, 6, 15]) for d in ("north", "south", "east", "west", "up", "down")},
                             **core)]}
        models["block/heat_pipe_arm_%d" % level] = {
            "parent": "minecraft:block/block", "textures": tex,
            "elements": [
                _el("barrel", [6, 6, 0], [10, 10, 5], {
                    "north": _f("pipe", [6, 0, 10, 4]),
                    "east": _f("pipe", [0, 0, 5, 4]), "west": _f("pipe", [5, 0, 0, 4]),
                    "up": _f("pipe", [0, 0, 5, 4], 90), "down": _f("pipe", [0, 0, 5, 4], 90)}),
                _el("sleeve", [5.5, 5.5, 1], [10.5, 10.5, 4], {
                    "north": _f("pipe", [4, 4, 9, 9]), "south": _f("pipe", [4, 4, 9, 9]),
                    "east": _f("pipe", [0, 4, 3, 9]), "west": _f("pipe", [0, 4, 3, 9]),
                    "up": _f("pipe", [0, 4, 3, 9], 90), "down": _f("pipe", [0, 4, 3, 9], 90)}),
                _el("heat_band", [5.25, 5.25, 1.75], [10.75, 10.75, 3.25], {
                    "north": _f("pipe", [4, 4, 9, 9]), "south": _f("pipe", [4, 4, 9, 9]),
                    "east": _f("pipe", [10, 4, 11.5, 9.5]), "west": _f("pipe", [10, 4, 11.5, 9.5]),
                    "up": _f("pipe", [10, 4, 11.5, 9.5], 90), "down": _f("pipe", [10, 4, 11.5, 9.5], 90)},
                    **band)]}
    # item: a node with two arms along x
    def arm_x(x0, x1, s0, s1, b0, b1, end):
        return [
            _el("barrel", [x0, 6, 6], [x1, 10, 10], {
                end: _f("pipe", [6, 0, 10, 4]),
                "north": _f("pipe", [0, 0, 5, 4]), "south": _f("pipe", [0, 0, 5, 4]),
                "up": _f("pipe", [0, 0, 5, 4]), "down": _f("pipe", [0, 0, 5, 4])}),
            _el("sleeve", [s0, 5.5, 5.5], [s1, 10.5, 10.5], {
                "east": _f("pipe", [4, 4, 9, 9]), "west": _f("pipe", [4, 4, 9, 9]),
                "north": _f("pipe", [0, 4, 3, 9]), "south": _f("pipe", [0, 4, 3, 9]),
                "up": _f("pipe", [0, 4, 3, 9]), "down": _f("pipe", [0, 4, 3, 9])}),
            _el("heat_band", [b0, 5.25, 5.25], [b1, 10.75, 10.75], {
                "east": _f("pipe", [4, 4, 9, 9]), "west": _f("pipe", [4, 4, 9, 9]),
                "north": _f("pipe", [10, 4, 11.5, 9.5]), "south": _f("pipe", [10, 4, 11.5, 9.5]),
                "up": _f("pipe", [10, 4, 11.5, 9.5], 90), "down": _f("pipe", [10, 4, 11.5, 9.5], 90)})]

    models["item/heat_pipe"] = {
        "parent": "minecraft:block/block",
        "textures": {"pipe": "nerotech:block/heat_pipe_0", "particle": "nerotech:block/heat_pipe_0"},
        "elements": [_el("node", [5, 5, 5], [11, 11, 11],
                         {d: _f("pipe", [0, 9, 6, 15]) for d in ("north", "south", "east", "west", "up", "down")})]
        + arm_x(0, 5, 1, 4, 1.75, 3.25, "west") + arm_x(11, 16, 12, 15, 12.75, 14.25, "east"),
        "display": {"gui": {"rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": [0.8, 0.8, 0.8]}}}
    return models


def _exchanger_model():
    els = [
        _el("base", [0, 0, 0], [16, 2, 16], {
            "north": _f("front", cull="north"), "south": _f("back", cull="south"),
            "east": _f("side", cull="east"), "west": _f("side", cull="west"),
            "up": _f("top"), "down": _f("top", cull="down")}),
        _el("manifold_front", [0, 2, 0], [16, 14, 1.5], {
            "north": _f("front", cull="north"), "south": _f("back"),
            "east": _f("side"), "west": _f("side"), "up": _f("top")}),
        _el("manifold_back", [0, 2, 14.5], [16, 14, 16], {
            "south": _f("back", cull="south"), "north": _f("back"),
            "east": _f("side"), "west": _f("side"), "up": _f("top")}),
    ]
    for n in range(7):
        z = 1.5 + 2 * n
        els.append(_el("fin_%d" % n, [2, 2, z], [14, 12, z + 1], {
            "north": _f("fin", [2, 4, 14, 14]), "south": _f("fin", [2, 4, 14, 14]),
            "east": _f("fin", [0, 4, 1, 14]), "west": _f("fin", [0, 4, 1, 14]),
            "up": _f("fin", [2, 0, 14, 1])}))
    els.append(_el("rail_top", [6.5, 14, 0], [9.5, 15, 16], _all("rail", ("down",))))
    els.append(_el("rail_west", [0.25, 7, 1.5], [1.25, 9, 14.5], _all("rail", ("north", "south"))))
    els.append(_el("rail_east", [14.75, 7, 1.5], [15.75, 9, 14.5], _all("rail", ("north", "south"))))
    edge = [0, 0, 1, 6]
    for tag, z0, z1, face in (("front", -1, 0, "north"), ("back", 16, 17, "south")):
        for part, (x0, y0, x1, y1) in (("tall", (5.5, 4.5, 10.5, 11.5)), ("wide", (4.5, 5.5, 11.5, 10.5))):
            els.append(_el("port_%s_%s" % (tag, part), [x0, y0, z0], [x1, y1, z1], {
                face: _f("port"), "east": _f("port", edge), "west": _f("port", edge),
                "up": _f("port", edge, 90), "down": _f("port", edge, 90)}))
    els.append(K.status_socket_element("#front"))
    return {"parent": "minecraft:block/block", "textures": {
        "front": "nerotech:block/heat_exchanger_front", "back": "nerotech:block/heat_exchanger_back",
        "side": "nerotech:block/heat_exchanger", "top": "nerotech:block/heat_exchanger_top",
        "fin": "nerotech:block/heat_exchanger_fin", "rail": "nerotech:block/heat_exchanger_rail",
        "port": "nerotech:block/heat_exchanger_port", "particle": "nerotech:block/heat_exchanger"},
        "elements": els}


def _drill_model():
    bore = [0, 0, 4, 5]
    els = [
        _el("deck_north", [0, 0, 0], [16, 5, 6], {
            "north": _f("front", cull="north"), "east": _f("side", cull="east"), "west": _f("side", cull="west"),
            "up": _f("top"), "down": _f("top", cull="down"), "south": _f("bore", [0, 0, 16, 5])}),
        _el("deck_south", [0, 0, 10], [16, 5, 16], {
            "south": _f("side", cull="south"), "east": _f("side", cull="east"), "west": _f("side", cull="west"),
            "up": _f("top"), "down": _f("top", cull="down"), "north": _f("bore", [0, 0, 16, 5])}),
        _el("deck_west", [0, 0, 6], [6, 5, 10], {
            "west": _f("side", cull="west"), "up": _f("top"), "down": _f("top", cull="down"),
            "east": _f("bore", bore)}),
        _el("deck_east", [10, 0, 6], [16, 5, 10], {
            "east": _f("side", cull="east"), "up": _f("top"), "down": _f("top", cull="down"),
            "west": _f("bore", bore)}),
    ]
    for tag, frm, to, inner in (("north", [4, 5, 4], [12, 6, 6], "south"), ("south", [4, 5, 10], [12, 6, 12], "north"),
                                ("west", [4, 5, 6], [6, 6, 10], "east"), ("east", [10, 5, 6], [12, 6, 10], "west")):
        faces = _all("side", ("down", "up", inner))
        faces["up"] = _f("top")
        faces[inner] = _f("bore", [0, 0, 4, 1])
        els.append(_el("table_seat_" + tag, frm, to, faces))
    els += [
        _el("motor_housing", [3, 5, 11], [13, 12, 16], _all("motor", ("down",))),
        _el("exhaust_stub", [10, 12, 12.5], [12.5, 16, 15], {
            "north": _f("motor"), "south": _f("motor"), "east": _f("motor"), "west": _f("motor"),
            "up": _f("bore", [0, 0, 2.5, 2.5])}),
        _el("cheek_west", [1, 5, 6.5], [3, 14, 9.5], _all("frame", ("down", "up"))),
        _el("cheek_east", [13, 5, 6.5], [15, 14, 9.5], _all("frame", ("down", "up"))),
        _el("crosshead", [1, 14, 6], [15, 16, 10], _all("frame")),
        _el("swivel", [6.5, 12, 6.5], [9.5, 14, 9.5], _all("motor", ("up",))),
        _el("lamp_post", [0.5, 5, 0], [3.5, 15, 1.5], {
            "north": _f("front"), "south": _f("side"), "east": _f("side"), "west": _f("side"), "up": _f("top")}),
        K.status_socket_element("#front"),
    ]
    return {"parent": "minecraft:block/block", "textures": {
        "front": "nerotech:block/deep_core_drill_front", "side": "nerotech:block/deep_core_drill",
        "top": "nerotech:block/deep_core_drill_top", "motor": "nerotech:block/deep_core_drill_motor",
        "bore": "nerotech:block/deep_core_drill_bore", "frame": "nerotech:block/drill_derrick",
        "particle": "nerotech:block/deep_core_drill"}, "elements": els}


def _brace(name, axis, plane0, plane1, c1, c2, half, angle):
    """A 0.75 px diagonal bar in the plane of one side, turned ``angle`` about that side's normal.

    ``c1``/``c2`` are the bar's centre across and up the side; ``half`` is half its length.
    """
    long_uv, end_uv = [0, 2, 0.75, 14], [0, 0, 0.75, 0.75]
    mid = (plane0 + plane1) / 2.0
    if axis == "z":
        frm, to = [c1 - 0.375, c2 - half, plane0], [c1 + 0.375, c2 + half, plane1]
        origin = [c1, c2, mid]
        faces = {"north": _f("leg", long_uv), "south": _f("leg", long_uv),
                 "east": _f("leg", long_uv), "west": _f("leg", long_uv)}
    else:
        frm, to = [plane0, c2 - half, c1 - 0.375], [plane1, c2 + half, c1 + 0.375]
        origin = [mid, c2, c1]
        faces = {"north": _f("leg", long_uv), "south": _f("leg", long_uv),
                 "east": _f("leg", long_uv), "west": _f("leg", long_uv)}
    faces["up"] = _f("leg", end_uv)
    faces["down"] = _f("leg", end_uv)
    return _el(name, frm, to, faces, rotation={"origin": origin, "axis": axis, "angle": angle}, shade=False)


def _derrick_models():
    tex = {"leg": "nerotech:block/drill_derrick", "particle": "nerotech:block/drill_derrick"}
    corners = (("nw", 0, 0), ("ne", 14, 0), ("sw", 0, 14), ("se", 14, 14))
    girts = lambda y0, y1: [  # noqa: E731
        _el("girt_north", [2, y0, 0.25], [14, y1, 1.5], _all("leg")),
        _el("girt_south", [2, y0, 14.5], [14, y1, 15.75], _all("leg")),
        _el("girt_west", [0.25, y0, 2], [1.5, y1, 14], _all("leg")),
        _el("girt_east", [14.5, y0, 2], [15.75, y1, 14], _all("leg"))]
    sides = (("north", "z", 0.3, 0.9), ("south", "z", 15.1, 15.7), ("west", "x", 0.3, 0.9), ("east", "x", 15.1, 15.7))

    upper = [_el("leg_" + n, [x, 0, z], [x + 2, 16, z + 2], _all("leg")) for n, x, z in corners]
    upper += girts(14, 16)
    for side, axis, p0, p1 in sides:
        upper.append(_brace("brace_%s_a" % side, axis, p0, p1 - 0.3, 8, 7, 9.6, 45))
        upper.append(_brace("brace_%s_b" % side, axis, p0 + 0.3, p1, 8, 7, 9.6, -45))

    lower = []
    for n, x, z in corners:
        px0, pz0 = (0 if x == 0 else 12), (0 if z == 0 else 12)
        gx0, gz0 = (0 if x == 0 else 13), (0 if z == 0 else 13)
        lower.append(_el("pad_" + n, [px0, 0, pz0], [px0 + 4, 1, pz0 + 4], _all("leg")))
        lower.append(_el("gusset_" + n, [gx0, 1, gz0], [gx0 + 3, 7, gz0 + 3], _all("leg", ("down",))))
        lower.append(_el("leg_" + n, [x, 7, z], [x + 2, 16, z + 2], _all("leg", ("down",))))
    lower += girts(14, 16)
    lower += [dict(g, name=g["name"] + "_mid") for g in girts(6, 7.5)]
    for side, axis, p0, p1 in sides:
        lower.append(_brace("brace_%s_a" % side, axis, p0, p1 - 0.3, 5, 10.75, 4.4, 45))
        lower.append(_brace("brace_%s_b" % side, axis, p0 + 0.3, p1, 11, 10.75, 4.4, -45))
    for tag, frm, to in (("north", [5, 0, 5], [11, 2, 6.5]), ("south", [5, 0, 9.5], [11, 2, 11]),
                         ("west", [5, 0, 6.5], [6.5, 2, 9.5]), ("east", [9.5, 0, 6.5], [11, 2, 9.5])):
        lower.append(_el("wellhead_" + tag, frm, to, _all("leg")))
    return {"block/drill_derrick_upper": {"parent": "minecraft:block/block", "textures": tex, "elements": upper},
            "block/drill_derrick_lower": {"parent": "minecraft:block/block", "textures": tex, "elements": lower}}


def _scanner_model():
    t = "tex"
    edge = [10, 0, 16, 0.75]
    els = [
        _el("pad", [3, 0, 1], [13, 1.5, 15], {
            "up": _f(t, [0, 0, 10, 14]), "down": _f(t, [10, 0.5, 16, 5.5]),
            "north": _f(t, edge), "south": _f(t, edge), "east": _f(t, edge), "west": _f(t, edge)}),
        _el("dish_arm", [11.5, 1.5, 1.5], [12.5, 5, 2.5], {
            d: _f(t, [10, 12.5, 11, 15.5]) for d in ("north", "south", "east", "west")}),
        _el("dish", [9, 4, 1.25], [15, 10, 2.25], {
            "south": _f(t, [10, 6, 16, 12]), "north": _f(t, [10, 12, 16, 16]),
            "east": _f(t, [10, 12, 10.5, 16]), "west": _f(t, [10, 12, 10.5, 16]),
            "up": _f(t, [10, 12, 16, 12.5]), "down": _f(t, [10, 15.5, 16, 16])},
            rotation={"origin": [12, 4, 1.75], "axis": "x", "angle": -22.5}),
    ]
    return {"item/survey_scanner": {
        "gui_light": "side",
        "textures": {"tex": "nerotech:item/survey_scanner", "particle": "nerotech:item/survey_scanner"},
        "elements": els,
        "display": {
            "gui": {"rotation": [48, 200, 0], "translation": [0, 1.5, 0], "scale": [0.95, 0.95, 0.95]},
            "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
            "fixed": {"rotation": [-90, 180, 0], "translation": [0, 0, -7], "scale": [0.9, 0.9, 0.9]},
            "thirdperson_righthand": {"rotation": [55, 0, 0], "translation": [0, 3.5, 1], "scale": [0.55, 0.55, 0.55]},
            "thirdperson_lefthand": {"rotation": [55, 0, 0], "translation": [0, 3.5, 1], "scale": [0.55, 0.55, 0.55]},
            "firstperson_righthand": {"rotation": [18, -8, 0], "translation": [1, 4, 0], "scale": [0.62, 0.62, 0.62]},
            "firstperson_lefthand": {"rotation": [18, 8, 0], "translation": [1, 4, 0], "scale": [0.62, 0.62, 0.62]}}}}


def models():
    """Every model of the package, keyed by its path under ``models/`` without the extension."""
    out = {}
    out.update(_pipe_models())
    out["block/heat_exchanger"] = _exchanger_model()
    out["block/deep_core_drill"] = _drill_model()
    out.update(_derrick_models())
    out.update(_scanner_model())
    out["item/heat_exchanger"] = {"parent": "nerotech:block/heat_exchanger"}
    out["item/deep_core_drill"] = {"parent": "nerotech:block/deep_core_drill"}
    out["item/drill_derrick"] = {"parent": "nerotech:block/drill_derrick_upper"}
    return out


def write_models():
    """Rewrite the package's models in place (these files belong to this package alone)."""
    for rel, model in sorted(models().items()):
        path = K.asset_path("models/%s.json" % rel)
        text = json.dumps(model, indent=2) + "\n"
        old = None
        if os.path.exists(path):
            with open(path, "r", encoding="utf-8", newline="") as fh:
                old = fh.read()
        if old == text:
            continue
        with open(path, "w", encoding="utf-8", newline="\n") as fh:
            fh.write(text)
        print("wrote", os.path.relpath(path, K.G.ROOT))


if __name__ == "__main__":
    if "--models" in sys.argv:
        write_models()
    if "--paint" in sys.argv:                        # this package only (add --force to repaint)
        paint()
