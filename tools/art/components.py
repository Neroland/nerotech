"""A4 Components art package: Component Forge, Laser Etcher, Crystal Grower.

Family trim is contact gold (non-emissive). Every base texture is painted as a plan of the model it
wraps (the block models use automatic UVs, so a face shows the pixels at its own model position):

* north face:  texture x = 32 - 2 * model x,  texture y = 32 - 2 * model y
* up face:     texture x = 2 * model x,       texture y = 2 * model z

The status lamp socket therefore lands at face pixels x 26..29, y 4..7 (``K.status_socket``), which
is model x 1..3, y 12..14: each block carries an instrument post there.

Renderer sprites (white or near-white, tinted in code; no heat colour anywhere):
``component_forge_arm``, ``component_forge_holo``, ``laser_etcher_lens``, ``laser_etcher_beam``,
``crystal_grower_glass``, ``crystal_grower_fog``, ``crystal_grower_crystal``.
"""
import math

from art import common as K

GOLD_HI, GOLD_LO = K.GOLD[2], K.GOLD[1]
PANEL = (16, 20, 24, 255)
WELL = (6, 9, 12, 255)


def _hline(px, x0, x1, y, col):
    for x in range(x0, x1 + 1):
        px[x, y] = col


def _vline(px, x, y0, y1, col):
    for y in range(y0, y1 + 1):
        px[x, y] = col


def _rect(px, x0, y0, x1, y1, col):
    for y in range(y0, y1 + 1):
        _hline(px, x0, x1, y, col)


def _frame(px, x0, y0, x1, y1, light=K.A_LIGHT, dark=K.A_DARK):
    """1 px raised frame: light top/left, dark bottom/right."""
    _hline(px, x0, x1, y0, light)
    _vline(px, x0, y0, y1, light)
    _hline(px, x0, x1, y1, dark)
    _vline(px, x1, y0, y1, dark)


def _gold_band(px, y, x0=2, x1=K.S - 3):
    K.family_trim(px, "components", y=y, x0=x0, x1=x1)


def _contacts(px, x0, x1, y, step=3):
    """A row of gold contact fingers (the family fitting)."""
    for x in range(x0, x1 + 1, step):
        px[x, y] = GOLD_HI
        px[x, y + 1] = GOLD_LO


def _vents(px, x0, x1, y0, y1):
    for y in range(y0, y1 + 1, 2):
        _hline(px, x0, x1, y, K.A_DARK)
        _hline(px, x0, x1, y + 1, K.mix(K.A_LIGHT, K.A_DARK, 0.6))


def _post(px, x0=25, x1=30, y0=3, y1=23):
    """The instrument post that carries the lamp socket."""
    for y in range(y0, y1 + 1):
        px[x0, y] = K.A_LIGHT
        px[x1, y] = K.A_DARK
        for x in range(x0 + 1, x1):
            px[x, y] = K.ALLOY[(x + y) % len(K.ALLOY)]
    _hline(px, x0, x1, y0, K.A_LIGHT)
    for y in range(y0 + 8, y1 - 1, 4):                 # cable clips down the post
        _hline(px, x0 + 2, x1 - 2, y, K.A_DARK)


# ---- Component Forge --------------------------------------------------------------------------
def _forge():
    name = "component_forge"

    def body(img, front):
        px = img.load()
        # towers / cups / pedestal zone (model y 8..16): plain ribbed alloy
        for x in (7, 8, 23, 24):
            _vline(px, x, 4, 15, K.A_DARK if x in (8, 24) else K.A_LIGHT)
        _rect(px, 12, 10, 19, 15, K.ALLOY[2])           # pedestal
        _hline(px, 12, 19, 10, GOLD_HI)
        _hline(px, 12, 19, 11, GOLD_LO)
        for cx in ((3,) if front else (3, 27)):         # cup lips seen edge-on
            _hline(px, cx - 1, cx + 2, 12, GOLD_LO)
        # workbed edge (model y 6..8): the gold band is the bed lip
        _gold_band(px, 16)
        _hline(px, 2, 29, 18, K.A_LIGHT)
        _hline(px, 2, 29, 19, K.A_DARK)
        # body (model y 0..6)
        K.recess(px, 5, 21, 26, 28, PANEL)
        return px

    img = K.machine_face(name)
    px = body(img, False)
    _vents(px, 7, 13, 23, 26)
    _contacts(px, 17, 24, 23)
    _contacts(px, 17, 24, 26)
    K.save_block(img, name)

    img = K.machine_face(name + "_front")
    px = body(img, True)
    _post(px, 25, 30, 3, 15)
    K.status_socket(px)
    # fluid line under the bed: a teal conduit the renderer brightens while working
    _hline(px, 7, 24, 22, K.T_DEEP)
    for x in range(7, 25):
        px[x, 23] = K.T_TEAL if x % 4 else K.T_CYAN
    _hline(px, 7, 24, 24, K.T_DEEP)
    for i in range(6):                                  # six input tell-tales, one per cup
        K.led(px, 7 + i * 3, 26, K.T_TEAL if i % 2 else K.T_CYAN)
    K.save_block(img, name + "_front")

    img = K.machine_face(name + "_top")
    px = img.load()
    K.recess(px, 3, 3, 28, 28, PANEL)
    for y in range(6, 27, 4):                           # bed alignment dots
        for x in range(8, 25, 4):
            px[x, y] = K.A_DARK
    for cx in (2, 26):                                  # six cups (model x 1..3 / 13..15)
        for cz in (6, 14, 22):
            _rect(px, cx, cz, cx + 3, cz + 3, GOLD_LO)
            _hline(px, cx, cx + 3, cz, GOLD_HI)
            _rect(px, cx + 1, cz + 1, cx + 2, cz + 2, WELL)
    _rect(px, 12, 12, 19, 19, K.ALLOY[2])               # pedestal top: contact pad
    _frame(px, 12, 12, 19, 19, GOLD_HI, GOLD_LO)
    _rect(px, 14, 14, 17, 17, K.T_DEEP)
    px[15, 15] = K.T_TEAL
    px[16, 16] = K.T_TEAL
    for tx in (4, 24):                                  # stalk shoulders (back corners)
        _rect(px, tx, 26, tx + 3, 29, K.ALLOY[1])
        _frame(px, tx, 26, tx + 3, 29)
        px[tx + 1, 27] = K.T_TEAL
    _rect(px, 1, 0, 6, 3, K.ALLOY[1])                   # instrument post top
    _frame(px, 1, 0, 6, 3)
    K.save_block(img, name + "_top")

    # renderer: manipulator stalk (bare steel with gold wrist bands and a teal tool tip)
    img = K.new_img()
    px = img.load()
    rng = K.rng_for(name + "_arm")
    for y in range(K.S):
        for x in range(K.S):
            px[x, y] = rng.choice(K.IRON[:2])
    _frame(px, 0, 0, 31, 31, K.IRON[2], K.A_DARK)
    for y in (9, 10, 21, 22):
        _hline(px, 1, 30, y, GOLD_LO if y % 2 else GOLD_HI)
    _rect(px, 13, 13, 18, 18, K.T_DEEP)
    _rect(px, 14, 14, 17, 17, K.T_CYAN)
    K.save_block(img, name + "_arm")

    # renderer: result hologram, a wireframe component drawn white (tinted teal -> plasma in code)
    img = K.new_img()
    px = img.load()
    w = K.WHITE
    _frame(px, 8, 8, 23, 23, w, w)
    _frame(px, 12, 12, 19, 19, w, w)
    for i in range(10, 22, 3):                          # pins
        _vline(px, i, 4, 7, w)
        _vline(px, i, 24, 27, w)
        _hline(px, 4, 7, i, w)
        _hline(px, 24, 27, i, w)
    for y in range(9, 23, 2):                           # scan lines inside
        for x in range(9, 23, 2):
            if not (12 <= x <= 19 and 12 <= y <= 19):
                px[x, y] = (214, 214, 214, 255)
    px[15, 15] = w
    px[16, 16] = w
    K.save_block(img, name + "_holo")


# ---- Laser Etcher -----------------------------------------------------------------------------
def _etcher():
    name = "laser_etcher"

    def shell(img):
        px = img.load()
        # turret barrel (model y 13..16) rows 0..5, turret collar (y 11..13) rows 6..9,
        # roof slab (y 9..11) rows 10..13, walls rows 14..25, plinth (y 0..3) rows 26..31
        for x in range(10, 22, 3):                      # barrel cooling flutes
            _vline(px, x, 2, 5, K.A_DARK)
        _gold_band(px, 6, 8, 23)
        _hline(px, 8, 23, 9, K.A_DARK)
        _gold_band(px, 10)
        _hline(px, 2, 29, 13, K.A_DARK)
        _hline(px, 2, 29, 26, K.A_LIGHT)                # plinth line (model y 3)
        return px

    img = K.machine_face(name)
    px = shell(img)
    K.recess(px, 5, 15, 26, 24, PANEL)                  # service hatch
    _vents(px, 7, 15, 17, 22)
    _contacts(px, 18, 24, 17)
    _contacts(px, 18, 24, 21)
    _hline(px, 2, 29, 27, K.A_DARK)
    K.save_block(img, name)

    img = K.machine_face(name + "_front")
    px = shell(img)
    _post(px, 25, 30, 3, 9)
    K.status_socket(px)
    # the viewing slot is real geometry; these columns are the jambs around it
    for y in range(14, 26):
        for x in (2, 3, 28, 29):
            px[x, y] = K.ALLOY[(x + y) % 3]
        px[4, y] = K.A_DARK
        px[27, y] = K.A_LIGHT
    _rect(px, 5, 14, 26, 25, WELL)                      # (behind the open slot)
    for x in range(4, 28):                              # sill light bar
        px[x, 27] = K.T_TEAL if x % 3 else K.T_CYAN
    for i in range(4):                                  # four lens-cut tell-tales
        K.led(px, 8 + i * 5, 29, K.T_CYAN if i == 0 else K.T_TEAL)
    _hline(px, 2, 29, 28, K.T_DEEP)
    K.save_block(img, name + "_front")

    img = K.machine_face(name + "_top")
    px = img.load()
    K.recess(px, 4, 4, 27, 27, PANEL)
    for i in range(6, 26, 3):                           # heat-spreader ribs on the roof
        _vline(px, i, 6, 25, K.A_DARK)
    _rect(px, 8, 8, 23, 23, K.ALLOY[0])                 # turret collar (model x/z 4..12)
    _frame(px, 8, 8, 23, 23)
    _rect(px, 10, 10, 21, 21, K.ALLOY[1])               # turret barrel top (model x/z 5..11)
    _frame(px, 10, 10, 21, 21, GOLD_HI, GOLD_LO)
    for y in range(12, 20):                             # the empty lens socket
        for x in range(12, 20):
            d = math.hypot(x - 15.5, y - 15.5)
            if d <= 4.2:
                px[x, y] = WELL if d <= 3.2 else K.A_DARK
    _rect(px, 1, 0, 6, 3, K.ALLOY[1])                   # instrument post top
    _frame(px, 1, 0, 6, 3)
    K.save_block(img, name + "_top")

    # interior: dark chamber, faint teal grid; x 0..11, y 0..11 hold the wafer stage top
    img = K.new_img()
    px = img.load()
    rng = K.rng_for(name + "_inner")
    for y in range(K.S):
        for x in range(K.S):
            px[x, y] = rng.choice(((12, 16, 20, 255), (14, 19, 23, 255), (10, 13, 17, 255)))
    for y in range(16, 32):
        for x in range(K.S):
            if x % 8 == 0 or y % 8 == 0:
                px[x, y] = K.T_DEEP
    _rect(px, 0, 0, 11, 11, K.ALLOY[0])
    _frame(px, 0, 0, 11, 11, GOLD_HI, GOLD_LO)
    for y in range(1, 11):
        for x in range(1, 11):
            d = math.hypot(x - 5.5, y - 5.5)
            if d <= 4.6:                                # the wafer: disc with a faint grid
                px[x, y] = K.IRON[1] if (x % 3 == 0 or y % 3 == 0) else K.IRON[0]
    _hline(px, 3, 8, 10, K.ALLOY[0])                    # the wafer's flat edge
    K.save_block(img, name + "_inner")

    # renderer: lens disc (white glass, grey ring mount; tinted with the fitted lens colour)
    img = K.new_img()
    px = img.load()
    for y in range(K.S):
        for x in range(K.S):
            d = math.hypot(x - 15.5, y - 15.5)
            if d <= 15.5:
                if d > 12.5:
                    px[x, y] = (120, 120, 120, 255)
                elif d > 11.0:
                    px[x, y] = (70, 70, 70, 255)
                else:
                    px[x, y] = (236, 236, 236, 255) if (x + y) % 9 else K.WHITE
    for i in range(6):                                  # glint
        px[9 + i, 14 - i] = K.WHITE
        px[10 + i, 14 - i] = K.WHITE
    K.save_block(img, name + "_lens")

    # renderer: beam (white core, paler dithered flanks; tinted)
    img = K.new_img()
    px = img.load()
    for y in range(K.S):
        for x in range(K.S):
            edge = min(x, 31 - x)
            if edge >= 10:
                px[x, y] = K.WHITE
            elif edge >= 5 or (x + y) % 2:
                px[x, y] = (208, 208, 208, 255)
    K.save_block(img, name + "_beam")


# ---- Crystal Grower ---------------------------------------------------------------------------
def _grower():
    name = "crystal_grower"

    def shell(img, front):
        px = img.load()
        # cap (model y 15..16) rows 0..1; jar ribs rows 2..21; coolant ring rows 22..23; base 24..31
        for x in (8, 9, 22, 23):
            _vline(px, x, 2, 21, K.A_LIGHT if x in (8, 22) else K.A_DARK)
        _gold_band(px, 2, 8, 23)                        # cap collar
        for x in range(4, 28):                          # coolant ring: frost-cold pipe
            px[x, 22] = K.FROST[0] if x % 5 else K.T_CYAN
            px[x, 23] = K.FROST[1]
        _gold_band(px, 24)
        K.recess(px, 6, 27, 25, 29, PANEL)
        for x in range(8, 24, 2):
            px[x, 28] = K.T_TEAL if front else K.A_DARK
        return px

    img = K.machine_face(name)
    shell(img, False)
    K.save_block(img, name)

    img = K.machine_face(name + "_front")
    px = shell(img, True)
    _post(px, 25, 30, 3, 23)
    K.status_socket(px)
    K.led(px, 10, 28, K.T_CYAN)
    K.led(px, 20, 28, K.T_CYAN)
    K.save_block(img, name + "_front")

    img = K.machine_face(name + "_top")
    px = img.load()
    for y in range(K.S):                                # coolant ring seen from above
        for x in range(K.S):
            d = max(abs(x - 15.5), abs(y - 15.5))
            if 10.0 < d <= 12.0:
                px[x, y] = K.FROST[0] if (x + y) % 5 else K.T_CYAN
            elif d <= 10.0:
                px[x, y] = PANEL
    _rect(px, 8, 8, 23, 23, K.ALLOY[1])                 # cap (model x/z 4..12)
    _frame(px, 8, 8, 23, 23, GOLD_HI, GOLD_LO)
    _rect(px, 13, 13, 18, 18, K.ALLOY[2])               # clamp boss
    _frame(px, 13, 13, 18, 18)
    px[15, 15] = K.T_TEAL
    px[16, 16] = K.T_TEAL
    _rect(px, 1, 0, 6, 5, K.ALLOY[1])                   # instrument post top
    _frame(px, 1, 0, 6, 5)
    K.save_block(img, name + "_top")

    # renderer: jar glass. Binary alpha (cutout): a thin rim, two glints and a sparse dither.
    for sprite, fog in ((name + "_glass", False), (name + "_fog", True)):
        img = K.new_img()
        px = img.load()
        rng = K.rng_for(sprite)
        hi = K.GLASS_HI[:3] + (255,)
        lo = K.GLASS[:3] + (255,)
        for y in range(K.S):
            for x in range(K.S):
                if fog:
                    # fogged: dense dither, thicker toward the top where the vapour gathers
                    if (x + y) % 2 == 0 and rng.random() < 0.95 - 0.02 * y:
                        px[x, y] = hi if rng.random() < 0.5 else (236, 240, 242, 255)
                elif (x * 7 + y * 3) % 23 == 0:
                    px[x, y] = lo
        for g, length in ((5, 9), (13, 5)):
            for i in range(length):
                px[g + i, 4 + length - i] = hi
                px[g + i + 1, 4 + length - i] = hi
        _hline(px, 0, 31, 0, lo)
        _hline(px, 0, 31, 31, lo)
        K.save_block(img, sprite)

    # renderer: the crystal (hangs point-down from the clamp; frost-white, tinted in code)
    img = K.new_img()
    px = img.load()
    frost = (K.FROST[0], K.FROST[1], K.WHITE, (150, 200, 226, 255))
    for y in range(K.S):
        half = 11.0 * (1.0 - (y / 31.0) ** 1.6) + 0.5 if y > 3 else 4.0 + y * 2.2
        for x in range(K.S):
            off = x - 15.5
            if abs(off) <= half:
                facet = 0 if off < -half * 0.35 else 2 if off < half * 0.15 else 1
                px[x, y] = frost[facet]
                if abs(off) > half - 1.2:
                    px[x, y] = frost[3]
    for y in range(6, 26, 5):                           # growth striations
        for x in range(K.S):
            if px[x, y][3] and (x + y) % 3 == 0:
                px[x, y] = frost[3]
    K.save_block(img, name + "_crystal")


def paint():
    _forge()
    _etcher()
    _grower()


PULSE = [
    K.pulse("component_forge_front"),
    K.pulse("laser_etcher_front"),
    K.pulse("crystal_grower_front", amp=0.24),
]
