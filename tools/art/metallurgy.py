"""A1 Metallurgy art package: Plasma Crucible, Plate Press, Filament Spooler and the three press dies.

``paint()`` paints every texture the package owns (base triads, one ``_parts`` sheet per block for the
fittings, the sprites the three renderers draw, the die item icons). ``PULSE`` lists the three fronts.
``models()`` holds the block and item models as data; run this file directly to (re)write them:

    python tools/art/metallurgy.py            write the package's model JSON (replaces placeholders)

Silhouettes (16-unit model space, north = front):

* Plasma Crucible  -- base slab, open pot with 2 px walls, three electrode masts stepping in to y 16,
                      front tap spout. The lamp socket sits on the front mast at the viewer's right.
* Plate Press      -- bed, two columns, crosshead beam, bolster with a die seat. The ram, the fitted
                      die and the impact flash are renderer geometry.
* Filament Spooler -- low bed, feed die tower at the viewer's left, open cradle at the right. The
                      spool, its winding and the wire are renderer geometry.

Renderer sprites are white or neutral so the renderer can tint them; no heat colour is baked in.
"""
import json
import math
import os
import sys

_TOOLS = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
if _TOOLS not in sys.path:
    sys.path.insert(0, _TOOLS)

from art import common as K  # noqa: E402

S = K.S
DARK = (12, 16, 20, 255)
STEEL = [(176, 184, 190, 255), (140, 150, 158, 255), (214, 222, 228, 255), (104, 114, 122, 255)]
WHITE = (255, 255, 255, 255)

PULSE = [
    K.pulse("plasma_crucible_front", amp=0.30),
    K.pulse("plate_press_front"),
    K.pulse("filament_spooler_front"),
]


# ---- small pixel helpers ------------------------------------------------------------------------
def rect(px, x0, y0, x1, y1, col):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            px[x, y] = col


def noise_rect(px, x0, y0, x1, y1, palette, rng):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            px[x, y] = rng.choice(palette)


def frame(px, x0, y0, x1, y1, light=K.A_LIGHT, dark=K.A_DARK):
    """1 px raised edge: light top/left, dark bottom/right."""
    for x in range(x0, x1 + 1):
        px[x, y0] = light
        px[x, y1] = dark
    for y in range(y0, y1 + 1):
        px[x0, y] = light
        px[x1, y] = dark


def cinder_band(px, x0, x1, y):
    """Two rows of cindersteel trim (the Metallurgy accent) at rows y, y + 1."""
    for x in range(x0, x1 + 1):
        px[x, y] = K.CINDER[(x // 2) % 2]
        px[x, y + 1] = K.CINDER[2]


def cinder_plate(px, x0, y0, x1, y1, rng):
    noise_rect(px, x0, y0, x1, y1, K.CINDER[:3], rng)
    frame(px, x0, y0, x1, y1, K.CINDER[3], K.mix(K.CINDER[2], K.A_DARK, 0.5))


def teal_slit(px, x0, y0, x1, y1):
    """A sunken emissive slit: plasma at the top easing to deep teal (PULSE breathes it)."""
    K.recess(px, x0 - 1, y0 - 1, x1 + 1, y1 + 1)
    ramp = [K.T_PLASMA, K.T_CYAN, K.T_CYAN, K.T_TEAL]
    n = y1 - y0 + 1
    for y in range(y0, y1 + 1):
        col = ramp[min(len(ramp) - 1, (y - y0) * len(ramp) // n)]
        for x in range(x0, x1 + 1):
            px[x, y] = col


def bolt(px, x, y):
    px[x, y] = K.A_LIGHT
    px[x + 1, y + 1] = K.A_DARK


def base_strip(px, top):
    """The lip of a bed/base band on a front or side texture (the band is rows top..31)."""
    for x in range(1, S - 1):
        px[x, top] = K.mix(K.A_LIGHT, K.ALLOY[0], 0.3)
        px[x, top + 1] = K.A_DARK


# ---- Plasma Crucible ----------------------------------------------------------------------------
def _crucible_pot(px, rng, front):
    """The pot wall, texture x 6..25, y 8..25 (model x 3..13, y 3..12)."""
    noise_rect(px, 6, 8, 25, 25, [K.ALLOY[2], K.ALLOY[0], K.ALLOY[2], K.mix(K.ALLOY[2], K.A_DARK, 0.4)], rng)
    cinder_band(px, 6, 25, 8)                        # rim trim
    for y in range(10, 26):                          # wall shading: lit left edge, dark right edge
        px[6, y] = K.ALLOY[3]
        px[25, y] = K.A_DARK
    for x in range(6, 26):                           # foot shadow and a hoop round the belly
        px[x, 25] = K.A_DARK
        px[x, 17] = K.CINDER[2]
        px[x, 16] = K.CINDER[0] if x % 4 else K.CINDER[3]
    for x in (8, 22):
        bolt(px, x, 12)
        bolt(px, x, 21)
    if front:
        teal_slit(px, 15, 11, 16, 14)                # sight slit above the hoop
        K.recess(px, 13, 19, 18, 22)                 # tap seat (the spout stands proud of this)
        rect(px, 14, 20, 17, 21, K.CINDER[2])


def _crucible_masts(px, cols):
    """Mast strips (4 px wide) from the top to the base."""
    for x0 in cols:
        for y in range(0, 26):
            px[x0, y] = K.CINDER[3]
            px[x0 + 1, y] = K.CINDER[1]
            px[x0 + 2, y] = K.CINDER[0]
            px[x0 + 3, y] = K.CINDER[2]
        for y in (10, 11, 18, 19):                   # ceramic insulator collars
            for i in range(4):
                px[x0 + i, y] = K.ENAMEL[i // 2]


def paint_plasma_crucible():
    # front
    name = "plasma_crucible_front"
    img = K.machine_base(name)
    px = img.load()
    rng = K.rng_for(name + "/pot")
    rect(px, 2, 2, 29, 25, K.A_DARK)                 # open air round the pot: never sampled
    _crucible_pot(px, rng, True)
    _crucible_masts(px, (2, 26))
    K.status_socket(px)
    base_strip(px, 26)
    K.led(px, 5, 28, K.T_CYAN, K.T_PLASMA)
    K.led(px, 10, 28, K.T_TEAL)
    for x in range(18, 27, 2):                       # cooling slots
        px[x, 28] = DARK
        px[x, 29] = DARK
    K.save_block(img, name)

    # sides
    name = "plasma_crucible"
    img = K.machine_base(name)
    px = img.load()
    rect(px, 2, 2, 29, 25, K.A_DARK)
    _crucible_pot(px, K.rng_for(name + "/pot"), False)
    base_strip(px, 26)
    for x in range(6, 26, 3):
        px[x, 28] = DARK
        px[x, 29] = DARK
    K.save_block(img, name)

    # top: base deck, pot rim ring (x 3..13) and the dark pot floor (x 5..11)
    name = "plasma_crucible_top"
    img = K.machine_base(name)
    px = img.load()
    rng = K.rng_for(name + "/rim")
    for y in range(6, 26):
        for x in range(6, 26):
            inner = 10 <= x <= 21 and 10 <= y <= 21
            if inner:
                px[x, y] = DARK if (x + y) % 5 else K.T_DEEP
            else:
                px[x, y] = rng.choice(K.CINDER[:2])
    frame(px, 6, 6, 25, 25, K.CINDER[3], K.CINDER[2])
    frame(px, 9, 9, 22, 22, K.CINDER[2], K.CINDER[3])
    for (x, y) in ((2, 2), (26, 2), (14, 26)):       # mast feet
        rect(px, x, y, x + 3, y + 3, K.CINDER[2])
    K.save_block(img, name)

    # parts: mast (u 0..2, v 0..13), arm (u 4..6, v 0..2), tip (u 8..9, v 0..2), spout (u 4..7, v 4..6),
    #        spout channel top (u 8..10, v 4..7)
    name = "plasma_crucible_parts"
    img = K.new_img()
    px = img.load()
    rect(px, 0, 0, S - 1, S - 1, K.CINDER[2])
    _crucible_masts(px, (0,))
    rect(px, 8, 0, 11, 3, K.CINDER[0])               # arm
    frame(px, 8, 0, 11, 3, K.CINDER[3], K.CINDER[2])
    rect(px, 16, 0, 17, 0, K.ENAMEL[1])              # tip: ceramic sleeve, teal point
    rect(px, 16, 1, 17, 1, K.T_TEAL)
    rect(px, 16, 2, 17, 3, K.T_CYAN)
    cinder_plate(px, 8, 8, 13, 11, K.rng_for(name + "/spout"))
    rect(px, 16, 8, 19, 13, K.CINDER[1])             # channel
    rect(px, 17, 8, 18, 13, DARK)
    K.save_block(img, name)

    # renderer sprites
    name = "plasma_crucible_arc"                     # a jagged bolt, left to right, white core
    img = K.new_img()
    px = img.load()
    rng = K.rng_for(name)
    y = 15.5
    for x in range(S):
        y += rng.uniform(-2.6, 2.6)
        y = min(24.0, max(7.0, y + (15.5 - y) * 0.25))
        yi = int(y)
        px[x, yi] = WHITE
        px[x, yi + 1] = WHITE
        px[x, yi - 1] = (190, 250, 255, 255)
        px[x, yi + 2] = (190, 250, 255, 255)
        if x % 7 == 3:                               # side forks
            for k in range(1, 4):
                px[min(S - 1, x + k), max(0, yi - 1 - k * 2)] = (150, 240, 255, 255)
    K.save_block(img, name)

    name = "plasma_crucible_melt"                    # mottled molten surface, tinted by heat
    img = K.new_img()
    px = img.load()
    rng = K.rng_for(name)
    for y in range(S):
        for x in range(S):
            swirl = math.sin(x * 0.7 + math.sin(y * 0.5) * 2.0) + math.cos(y * 0.6 + x * 0.2)
            v = 208 + int(22 * swirl) + rng.choice((-8, 0, 0, 10))
            v = max(150, min(255, v))
            px[x, y] = (v, v, v, 255)
    for (x, y) in ((6, 9), (20, 6), (14, 20), (25, 24), (9, 26)):   # crust flecks
        px[x, y] = (120, 120, 120, 255)
        px[x + 1, y] = (140, 140, 140, 255)
    K.save_block(img, name)


# ---- Plate Press --------------------------------------------------------------------------------
def paint_plate_press():
    # front: crosshead band rows 0..7 (model y 12..16), bed band rows 24..31 (model y 0..4)
    name = "plate_press_front"
    img = K.machine_face(name, "metallurgy", front=True)
    px = img.load()
    for x in range(1, S - 1):                        # crosshead lower lip
        px[x, 7] = K.A_DARK
    K.recess(px, 8, 4, 21, 6)                        # maker's plate: pressure ticks
    for x in range(10, 21, 2):
        px[x, 5] = K.T_TEAL if x < 16 else K.T_CYAN
    rect(px, 2, 8, 29, 23, K.A_DARK)                 # open throat: never sampled
    for x0 in (2, 24):                               # column hints for the flat texture
        noise_rect(px, x0, 8, x0 + 5, 23, K.ALLOY, K.rng_for(name + str(x0)))
    base_strip(px, 24)
    cinder_band(px, 2, 29, 26)
    K.led(px, 5, 29, K.T_CYAN, K.T_PLASMA)
    teal_slit(px, 12, 29, 19, 29)                    # stroke strip
    px[14, 29] = K.T_DEEP
    px[17, 29] = K.T_DEEP
    for x in (24, 26):
        px[x, 29] = DARK
        px[x, 30] = DARK
    K.save_block(img, name)

    name = "plate_press"
    img = K.machine_face(name, "metallurgy")
    px = img.load()
    for x in range(1, S - 1):
        px[x, 7] = K.A_DARK
    for x in (6, 14, 22):                            # crosshead tie-bolts
        bolt(px, x, 5)
    rect(px, 2, 8, 29, 23, K.A_DARK)
    noise_rect(px, 10, 8, 21, 23, K.ALLOY, K.rng_for(name + "/col"))   # column, model z 5..11
    for y in range(8, 24):
        px[10, y] = K.ALLOY[3]
        px[21, y] = K.A_DARK
    base_strip(px, 24)
    cinder_band(px, 2, 29, 26)
    for x in range(8, 24, 3):
        px[x, 29] = DARK
    K.save_block(img, name)

    # top: seen on the crosshead (x 1..15, z 4..12) and on the bed deck
    name = "plate_press_top"
    img = K.machine_base(name)
    px = img.load()
    rng = K.rng_for(name + "/deck")
    cinder_plate(px, 8, 8, 23, 23, rng)              # bolster footprint / crosshead cap
    rect(px, 13, 13, 18, 18, K.A_DARK)               # ram gland
    frame(px, 12, 12, 19, 19, K.A_LIGHT, K.A_DARK)
    rect(px, 14, 14, 17, 17, STEEL[1])
    for (x, y) in ((4, 12), (4, 19), (26, 12), (26, 19)):   # column nuts
        rect(px, x, y, x + 1, y + 1, K.A_LIGHT)
        px[x + 1, y + 1] = K.A_DARK
    K.save_block(img, name)

    # parts: column face (u 0..3, v 0..8), column flank (u 4..10, v 0..8), bolster side (u 0..8, v 10..12),
    #        bolster top with the die seat (u 8..16, v 8..16)
    name = "plate_press_parts"
    img = K.new_img()
    px = img.load()
    rect(px, 0, 0, S - 1, S - 1, K.ALLOY[2])
    rng = K.rng_for(name)
    noise_rect(px, 0, 0, 5, 15, K.ALLOY, rng)        # column face, 3 x 8 units
    for y in range(16):
        px[0, y] = K.A_LIGHT
        px[1, y] = STEEL[1]                          # polished guide way
        px[5, y] = K.A_DARK
    noise_rect(px, 8, 0, 19, 15, K.ALLOY, rng)       # column flank, 6 x 8 units
    for y in range(16):
        px[8, y] = K.ALLOY[3]
        px[19, y] = K.A_DARK
        px[13, y] = STEEL[3]
        px[14, y] = STEEL[1]
    for y in (3, 11):
        bolt(px, 10, y)
        bolt(px, 16, y)
    cinder_plate(px, 0, 20, 15, 23, rng)             # bolster side, 8 x 2 units
    cinder_plate(px, 16, 16, 31, 31, rng)            # bolster top, 8 x 8 units
    K.recess(px, 18, 18, 29, 29)                     # die seat
    K.save_block(img, name)

    # renderer sprites
    name = "plate_press_ram"
    img = K.new_img()
    px = img.load()
    rng = K.rng_for(name)
    cinder_plate(px, 0, 0, S - 1, S - 1, rng)
    frame(px, 1, 1, S - 2, S - 2, K.CINDER[1], K.CINDER[2])
    rect(px, 4, 12, 27, 19, K.CINDER[2])
    for x in range(6, 26, 4):
        rect(px, x, 14, x + 1, 17, K.T_TEAL)         # stroke marks (static teal, not pulsed)
    for (x, y) in ((4, 4), (26, 4), (4, 26), (26, 26)):
        bolt(px, x, y)
    K.save_block(img, name)

    name = "plate_press_rod"
    img = K.new_img()
    px = img.load()
    for y in range(S):
        for x in range(S):
            band = STEEL[2] if x < 8 else STEEL[0] if x < 20 else STEEL[1] if x < 28 else STEEL[3]
            px[x, y] = band if y % 8 else STEEL[3]
    K.save_block(img, name)

    name = "plate_press_flash"                       # impact starburst, tinted plasma by the renderer
    img = K.new_img()
    px = img.load()
    for y in range(S):
        for x in range(S):
            dx, dy = x - 15.5, y - 15.5
            d = math.hypot(dx, dy)
            ang = math.atan2(dy, dx)
            ray = abs(math.sin(ang * 6.0))
            if d < 4.5 or (d < 15.5 and ray > 0.55 + d * 0.022 and (x + y) % 2 == 0) or (d < 9 and ray > 0.8):
                v = 255 if d < 8 else 220
                px[x, y] = (v, v, v, 255)
    K.save_block(img, name)

    for kind in ("plate", "rod", "wafer"):
        img = _die_face("plate_press_die_" + kind, kind)
        K.save_block(img, "plate_press_die_" + kind)


def _die_face(name, kind):
    """Top face of a press die: a steel block with the cavity that names its shape."""
    img = K.new_img()
    px = img.load()
    rng = K.rng_for(name)
    noise_rect(px, 0, 0, S - 1, S - 1, STEEL[:2] + [STEEL[0]], rng)
    frame(px, 0, 0, S - 1, S - 1, STEEL[2], STEEL[3])
    frame(px, 1, 1, S - 2, S - 2, STEEL[2], STEEL[3])
    cav = (38, 44, 50, 255)
    lip = STEEL[2]
    if kind == "plate":                              # one wide rectangular pocket
        rect(px, 6, 9, 25, 22, cav)
        for x in range(6, 26):
            px[x, 23] = lip
            px[x, 9] = DARK
    elif kind == "rod":                              # three parallel grooves
        for y0 in (6, 14, 22):
            rect(px, 5, y0, 26, y0 + 3, cav)
            for x in range(5, 27):
                px[x, y0] = DARK
                px[x, y0 + 4] = lip
    else:                                            # wafer: one round pocket with a flat
        for y in range(S):
            for x in range(S):
                if math.hypot(x - 15.5, y - 15.5) < 10.2 and y < 24:
                    px[x, y] = cav
                    if math.hypot(x - 15.5, y - 16.5) >= 10.2:
                        px[x, y] = DARK
        for x in range(9, 23):
            px[x, 24] = lip
    for (x, y) in ((3, 3), (27, 3), (3, 27), (27, 27)):
        px[x, y] = STEEL[3]
        px[x + 1, y + 1] = STEEL[3]
    return img


# ---- Filament Spooler ---------------------------------------------------------------------------
def paint_filament_spooler():
    # front: lamp post strip x 26..29 (model x 1..3), bed band rows 24..31
    name = "filament_spooler_front"
    img = K.machine_base(name)
    px = img.load()
    rect(px, 2, 2, 29, 23, K.A_DARK)                 # open air above the bed: never sampled
    for y in range(2, 24):                           # the lamp post
        px[26, y] = K.A_LIGHT
        px[27, y] = K.ALLOY[3]
        px[28, y] = K.ALLOY[1]
        px[29, y] = K.A_DARK
    K.status_socket(px)
    base_strip(px, 24)
    cinder_band(px, 2, 29, 26)
    K.led(px, 5, 29, K.T_CYAN, K.T_PLASMA)
    for i, x in enumerate(range(11, 22, 3)):         # wire gauge: four cells
        K.recess(px, x - 1, 28, x + 2, 30)
        px[x, 29] = K.T_TEAL if i < 2 else K.T_CYAN
        px[x + 1, 29] = K.T_TEAL if i < 2 else K.T_CYAN
    K.save_block(img, name)

    name = "filament_spooler"
    img = K.machine_base(name)
    px = img.load()
    rect(px, 2, 2, 29, 23, K.A_DARK)
    base_strip(px, 24)
    cinder_band(px, 2, 29, 26)
    for x in range(7, 26, 4):
        px[x, 29] = DARK
        px[x + 1, 29] = DARK
    K.save_block(img, name)

    # top: the bed deck, with a drip tray under the spool (model x 1..9) and the tower foot (x 11..15)
    name = "filament_spooler_top"
    img = K.machine_base(name)
    px = img.load()
    K.recess(px, 3, 7, 18, 24)                       # tray
    for y in range(9, 24, 3):
        for x in range(5, 18):
            px[x, y] = K.ALLOY[2]
    cinder_plate(px, 21, 9, 30, 22, K.rng_for(name + "/foot"))
    K.save_block(img, name)

    # parts: tower front (u 0..4, v 0..10), tower flank (u 4..10, v 0..10), tower cap (u 10..14, v 0..6),
    #        post (u 14..16, v 0..11), bearing arm (u 0..4, v 12..14), nozzle (u 6..8, v 12..14),
    #        feed slot (u 10..12, v 8..12)
    name = "filament_spooler_parts"
    img = K.new_img()
    px = img.load()
    rect(px, 0, 0, S - 1, S - 1, K.ALLOY[2])
    rng = K.rng_for(name)
    noise_rect(px, 0, 0, 7, 19, K.ALLOY, rng)        # tower front
    frame(px, 0, 0, 7, 19)
    cinder_band(px, 1, 6, 1)
    teal_slit(px, 3, 8, 4, 14)                       # draw-heat slit
    noise_rect(px, 8, 0, 19, 19, K.ALLOY, rng)       # tower flank
    frame(px, 8, 0, 19, 19)
    cinder_band(px, 9, 18, 1)
    K.recess(px, 11, 5, 16, 9)                       # die block seat (the nozzle stands on this)
    for x in range(10, 18, 2):
        px[x, 14] = DARK
        px[x, 16] = DARK
    cinder_plate(px, 20, 0, 27, 11, rng)             # tower cap
    rect(px, 22, 3, 25, 8, DARK)
    for y in range(22):                              # post
        px[28, y] = K.A_LIGHT
        px[29, y] = K.ALLOY[3]
        px[30, y] = K.ALLOY[1]
        px[31, y] = K.A_DARK
    noise_rect(px, 0, 24, 7, 27, K.ALLOY, rng)       # bearing arm
    frame(px, 0, 24, 7, 27)
    rect(px, 3, 25, 4, 26, STEEL[1])                 # axle end
    rect(px, 12, 24, 15, 27, STEEL[0])               # nozzle
    frame(px, 12, 24, 15, 27, STEEL[2], STEEL[3])
    rect(px, 13, 25, 14, 26, DARK)
    rect(px, 20, 16, 23, 23, K.CINDER[0])            # feed slot
    rect(px, 21, 17, 22, 22, DARK)
    K.save_block(img, name)

    # renderer sprites
    name = "filament_spooler_flange"                 # round spoked flange, corners clear (cutout)
    img = K.new_img()
    px = img.load()
    for y in range(S):
        for x in range(S):
            dx, dy = x - 15.5, y - 15.5
            d = math.hypot(dx, dy)
            if d > 15.6:
                continue
            ang = (math.atan2(dy, dx) / (math.pi / 3.0)) % 1.0
            if d > 13.0:
                px[x, y] = K.CINDER[3] if d > 14.4 else K.CINDER[1]
            elif d < 3.2:
                px[x, y] = STEEL[1] if d > 1.5 else K.A_DARK
            elif ang < 0.22 or d < 5.0:
                px[x, y] = K.CINDER[0]               # spokes and hub
            elif d > 11.6:
                px[x, y] = K.CINDER[2]
            # the six windows between the spokes stay clear: the winding shows through them
    px[15, 4] = K.T_CYAN                             # one index mark, so rotation reads
    px[16, 4] = K.T_CYAN
    px[15, 5] = K.T_TEAL
    px[16, 5] = K.T_TEAL
    K.save_block(img, name)

    name = "filament_spooler_wire"                   # wound turns; also the wire run (neutral, tinted)
    img = K.new_img()
    px = img.load()
    rng = K.rng_for(name)
    for y in range(S):
        for x in range(S):
            t = (x + (y // 4) % 2) % 4
            v = (236, 206, 168, 132)[t] + rng.choice((-6, 0, 6))
            px[x, y] = (v, v, v, 255)
    K.save_block(img, name)

    name = "filament_spooler_glow"                   # dithered soft glow, tinted by heat (HOT only)
    img = K.new_img()
    px = img.load()
    for y in range(S):
        for x in range(S):
            d = math.hypot(x - 15.5, y - 15.5) / 15.5
            if d < 0.45 or (d < 0.8 and (x + y) % 2 == 0) or (d < 1.0 and x % 2 == 0 and y % 2 == 0):
                px[x, y] = WHITE
    K.save_block(img, name)


# ---- die items ----------------------------------------------------------------------------------
def paint_die_items():
    """plate_die / rod_die / wafer_die: one steel die block seen three-quarter on; the cavity names
    the shape (wide pocket, three grooves, round pocket with a flat). Shape, not colour, tells them
    apart, and the same cavities are on the die the Plate Press renderer shows."""
    cav = (38, 44, 50, 255)
    out = (30, 36, 42, 255)
    for kind in ("plate", "rod", "wafer"):
        name = kind + "_die"
        img = K.new_img()
        px = img.load()
        rng = K.rng_for(name)
        noise_rect(px, 4, 6, 27, 20, STEEL[:2] + [STEEL[0]], rng)      # top face
        frame(px, 4, 6, 27, 20, STEEL[2], STEEL[1])
        rect(px, 4, 21, 27, 26, STEEL[3])                              # front skirt
        for x in range(4, 28):
            px[x, 21] = STEEL[1]
            px[x, 26] = (60, 68, 76, 255)
        for x in (8, 15, 22):                                          # clamp notches
            rect(px, x, 23, x + 1, 24, (60, 68, 76, 255))
        if kind == "plate":
            rect(px, 8, 9, 23, 17, cav)
            rect(px, 8, 9, 23, 9, DARK)
            rect(px, 8, 18, 23, 18, STEEL[2])
        elif kind == "rod":
            for y0 in (8, 12, 16):
                rect(px, 7, y0, 24, y0 + 1, cav)
                rect(px, 7, y0, 24, y0, DARK)
                rect(px, 7, y0 + 2, 24, y0 + 2, STEEL[2])
        else:
            for y in range(8, 19):
                for x in range(7, 25):
                    if ((x - 15.5) / 7.6) ** 2 + ((y - 13.0) / 5.2) ** 2 <= 1.0 and y <= 17:
                        px[x, y] = cav if y > 8 and ((x - 15.5) / 7.6) ** 2 + ((y - 14.0) / 5.2) ** 2 <= 1.0 else DARK
            rect(px, 11, 18, 20, 18, STEEL[2])
        for x in range(4, 28):                                         # outline
            px[x, 5] = out
            px[x, 27] = out
        for y in range(6, 27):
            px[3, y] = out
            px[28, y] = out
        K.save(img, "item", name)


def paint():
    paint_plasma_crucible()
    paint_plate_press()
    paint_filament_spooler()
    paint_die_items()


# ---- models -------------------------------------------------------------------------------------
def _tex(block):
    return {
        "front": "nerotech:block/%s_front" % block,
        "side": "nerotech:block/%s" % block,
        "top": "nerotech:block/%s_top" % block,
        "parts": "nerotech:block/%s_parts" % block,
        "lamp": "nerotech:block/status_lamp",
        "particle": "nerotech:block/%s" % block,
    }


def _el(name, frm, to, **faces):
    """One element. ``faces`` maps a direction to a texture key, ``(key, uv)`` or None (omit);
    unnamed sides use ``#side``, an unnamed top ``#top``, an unnamed bottom ``#side``."""
    out = {}
    for d in ("north", "south", "west", "east", "up", "down"):
        spec = faces[d] if d in faces else ("#top" if d == "up" else "#side")
        if spec is None:
            continue
        if isinstance(spec, tuple):
            out[d] = {"uv": list(spec[1]), "texture": spec[0]}
        else:
            out[d] = {"texture": spec}
    return {"name": name, "from": list(frm), "to": list(to), "faces": out}


def _all(key, uv_side, uv_cap=None):
    """Every face from one texture: ``uv_side`` on the four sides, ``uv_cap`` on up/down."""
    cap = uv_cap or uv_side
    return dict(north=(key, uv_side), south=(key, uv_side), west=(key, uv_side), east=(key, uv_side),
                up=(key, cap), down=(key, cap))


def _base(height):
    el = _el("base", (0, 0, 0), (16, height, 16), north="#front")
    for d in ("north", "south", "west", "east", "down"):
        el["faces"][d]["cullface"] = d
    return el


def models():
    P = "#parts"
    mast = dict(_all(P, (0, 0, 2, 13), (4, 0, 6, 2)), down=None)
    arm = _all(P, (4, 0, 6, 2))
    tip = _all(P, (8, 0, 9, 2), (8, 1, 9, 2))
    crucible = {
        "parent": "minecraft:block/block",
        "textures": _tex("plasma_crucible"),
        "elements": [
            _base(3),
            _el("pot_floor", (5, 3, 5), (11, 4, 11), north=None, south=None, west=None, east=None, down=None),
            _el("pot_north", (3, 3, 3), (13, 12, 5), north="#front", down=None),
            _el("pot_south", (3, 3, 11), (13, 12, 13), down=None),
            _el("pot_west", (3, 3, 5), (5, 12, 11), north=None, south=None, down=None),
            _el("pot_east", (11, 3, 5), (13, 12, 11), north=None, south=None, down=None),
            _el("mast_front_a", (1, 3, 0), (3, 16, 2), **dict(mast, north="#front")),
            _el("mast_front_b", (13, 3, 0), (15, 16, 2), **mast),
            _el("mast_back", (7, 3, 14), (9, 16, 16), **mast),
            _el("arm_a", (3, 14, 2), (5, 16, 4), **arm),
            _el("arm_b", (11, 14, 2), (13, 16, 4), **arm),
            _el("arm_back", (7, 14, 12), (9, 16, 14), **arm),
            _el("tip_a", (5, 13, 4), (6, 15, 5), **tip),
            _el("tip_b", (10, 13, 4), (11, 15, 5), **tip),
            _el("tip_back", (7.5, 13, 10.5), (8.5, 15, 11.5), **tip),
            _el("spout", (7, 5, 0), (9, 7, 3), **dict(_all(P, (4, 4, 7, 6), (8, 4, 10, 7)), south=None)),
            K.status_socket_element("#front", 0.0),
        ],
    }
    col = dict(north=(P, (0, 0, 3, 8)), south=(P, (0, 0, 3, 8)), west=(P, (4, 0, 10, 8)),
               east=(P, (4, 0, 10, 8)), up=None, down=None)
    press = {
        "parent": "minecraft:block/block",
        "textures": _tex("plate_press"),
        "elements": [
            _base(4),
            _el("bolster", (4, 4, 4), (12, 6, 12), **dict(_all(P, (0, 10, 8, 12), (8, 8, 16, 16)), down=None)),
            _el("column_a", (1, 4, 5), (4, 12, 11), **col),
            _el("column_b", (12, 4, 5), (15, 12, 11), **col),
            _el("crosshead", (1, 12, 4), (15, 16, 12), north="#front", down="#top"),
            K.status_socket_element("#front", 4.0),
        ],
    }
    post = dict(_all(P, (14, 0, 16, 11), (14, 0, 16, 1)), down=None)
    spooler = {
        "parent": "minecraft:block/block",
        "textures": _tex("filament_spooler"),
        "elements": [
            _base(4),
            _el("tower", (11, 4, 5), (15, 14, 11), north=(P, (0, 0, 4, 10)), south=(P, (0, 0, 4, 10)),
                west=(P, (4, 0, 10, 10)), east=(P, (4, 0, 10, 10)), up=(P, (10, 0, 14, 6)), down=None),
            _el("feed_slot", (12, 14, 6), (14, 15, 10), **dict(_all(P, (10, 8, 12, 9), (10, 8, 12, 12)), down=None)),
            _el("nozzle", (10, 11, 7), (11, 13, 9), **dict(_all(P, (6, 12, 8, 14)), east=None)),
            _el("lamp_post", (1, 4, 1), (3, 15, 3), **dict(post, north="#front")),
            _el("bearing_front", (3, 8, 1), (7, 10, 3), **dict(_all(P, (0, 12, 4, 14)), west=None)),
            _el("post_back", (4, 4, 13), (6, 10, 15), **dict(_all(P, (14, 0, 16, 6), (14, 0, 16, 1)), down=None)),
            K.status_socket_element("#front", 1.0),
        ],
    }
    out = {
        "models/block/plasma_crucible.json": crucible,
        "models/block/plate_press.json": press,
        "models/block/filament_spooler.json": spooler,
    }
    for kind in ("plate", "rod", "wafer"):
        out["models/item/%s_die.json" % kind] = {
            "parent": "minecraft:item/generated",
            "textures": {"layer0": "nerotech:item/%s_die" % kind},
        }
    return out


def write_models():
    """Write the package's models (always replaces: the files are this package's own)."""
    for rel, obj in models().items():
        path = K.asset_path(rel)
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "w", encoding="utf-8", newline="\n") as fh:
            json.dump(obj, fh, indent=2)
            fh.write("\n")
        print("wrote", os.path.relpath(path, K.G.ROOT))


if __name__ == "__main__":
    write_models()
