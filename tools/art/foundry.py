"""A2 Foundry art package: Blast Foundry (controller), Foundry Brick (+ lit) and Foundry Heat Port.

``paint()`` paints every texture the package owns: the firebrick set (cold and lit), the controller
faces, door, sight glass and fittings, the heat-port face and collar, and the neutral sprites the two
renderers tint (melt, vent glow, pour stream, form wave, ghost outline, collar glow). ``PULSE`` lists
the controller front. ``models()`` holds the block models as data; run this file directly to
(re)write them:

    python tools/art/foundry.py            write the package's model JSON (replaces placeholders)

Silhouettes (16-unit model space, north = front):

* Blast Foundry     -- full brick block; a 1 px front frame (jambs, lintel, sill) round a stepped-arch
                       fire door recess, an iron door half a pixel back with a 4 x 4 px sight glass,
                       and a pour lip standing 1 px proud under the door. Lamp socket on the jamb.
* Foundry Brick     -- four brick courses with the mortar bed recessed half a pixel under each. The
                       ``lit`` model swaps to the warm-mortar textures (the only warm static colour).
* Foundry Heat Port -- brick block with a square copper collar standing 1 px proud of all six faces
                       (the block has no facing). The bore glow is renderer geometry.

The brick bond is mirror-symmetric (2 px joints centred on 0/16 and 8/24) and the top is four square
tiles, so the renderer's form-wave sprites line up with the real mortar on every face and facing.
Renderer sprites are white or neutral; no heat colour is baked into any texture.
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
FIREBOX = (16, 12, 12, 255)
WHITE = (255, 255, 255, 255)
GLASS_DARK = (14, 26, 32, 255)
GLASS_GLINT = (58, 84, 94, 255)
ARRIS = (150, 104, 84, 255)

PULSE = [K.pulse("blast_foundry_front")]


# ---- small pixel helpers ------------------------------------------------------------------------
def rect(px, x0, y0, x1, y1, col):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            px[x, y] = col


def frame(px, x0, y0, x1, y1, light, dark):
    for x in range(x0, x1 + 1):
        px[x, y0] = light
        px[x, y1] = dark
    for y in range(y0, y1 + 1):
        px[x0, y] = light
        px[x1, y] = dark


def cinder(px, x0, y0, x1, y1, rng, framed=True):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            px[x, y] = rng.choice(K.CINDER[:3])
    if framed:
        frame(px, x0, y0, x1, y1, K.CINDER[3], K.mix(K.CINDER[2], K.A_DARK, 0.5))


def bolt(px, x, y):
    px[x, y] = K.CINDER[3]
    px[x + 1, y + 1] = K.mix(K.CINDER[2], K.A_DARK, 0.6)


# ---- the brick bond -----------------------------------------------------------------------------
def is_bed(y):
    """Mortar bed rows: the bottom row of each 8 px course."""
    return y % 8 == 7


def is_joint(x, y):
    """Vertical joints, 2 px wide: even courses on 0/16, odd courses on 8/24 (mirror-symmetric)."""
    if (y // 8) % 2 == 0:
        return x in (0, 15, 16, 31)
    return x in (7, 8, 23, 24)


def is_tile_joint(x, y):
    """Top face: four square tiles (rotation-symmetric)."""
    return x in (0, 15, 16, 31) or y in (0, 15, 16, 31)


def paint_bricks(px, name, lit=False, top=False):
    """Fill the whole tile with the firebrick bond (cold or warm mortar)."""
    rng = K.rng_for(name)
    mortar = K.MORTAR_LIT if lit else K.MORTAR
    mortar2 = K.mix(K.MORTAR_LIT, K.MORTAR, 0.35) if lit else K.mix(K.MORTAR, K.A_DARK, 0.4)
    tone = {}

    def mortar_at(x, y):
        x, y = x % S, y % S
        return is_tile_joint(x, y) if top else (is_joint(x, y) or is_bed(y))

    for y in range(S):
        for x in range(S):
            if top:
                key = (x // 16, y // 16)
            else:
                course = y // 8
                key = (course, ((x + (0 if course % 2 == 0 else 8)) % S) // 16)
            if key not in tone:
                tone[key] = rng.choice((0, 0, 1, 2))
            roll, pick, mroll = rng.random(), rng.choice(K.BRICK), rng.random()
            if mortar_at(x, y):
                px[x, y] = mortar if mroll < 0.8 else mortar2
                continue
            col = K.BRICK[tone[key]] if roll < 0.7 else pick
            if mortar_at(x, y - 1) or mortar_at(x - 1, y):
                col = K.mix(K.mix(col, K.BRICK[1], 0.6), ARRIS, 0.25)     # lit arris
            elif mortar_at(x, y + 1) or mortar_at(x + 1, y):
                col = K.mix(K.mix(col, K.BRICK[2], 0.7), K.MORTAR, 0.25)  # shadow over the joint
                if lit:
                    col = K.mix(col, K.MORTAR_LIT, 0.30)                  # warm spill on the edge
            px[x, y] = col


def paint_foundry_brick():
    for lit in (False, True):
        stem = "foundry_brick_lit" if lit else "foundry_brick"
        img = K.new_img()
        paint_bricks(img.load(), "foundry_brick", lit=lit)      # same seed: only the mortar changes
        K.save_block(img, stem)
        img = K.new_img()
        paint_bricks(img.load(), "foundry_brick_top", lit=lit, top=True)
        K.save_block(img, stem + "_top")
        img = K.new_img()
        px = img.load()
        rng = K.rng_for("foundry_brick_mortar")
        a = K.MORTAR_LIT if lit else K.MORTAR
        b = K.mix(K.MORTAR_LIT, K.MORTAR, 0.35) if lit else K.mix(K.MORTAR, K.A_DARK, 0.4)
        for y in range(S):
            for x in range(S):
                px[x, y] = a if rng.random() < 0.8 else b
        K.save_block(img, "foundry_brick_mortar_lit" if lit else "foundry_brick_mortar")


# ---- Blast Foundry ------------------------------------------------------------------------------
def opening(x, y):
    """The stepped-arch door opening on the 32 px front (model x 3..13, y 3..12)."""
    if 12 <= y <= 25:
        return 6 <= x <= 25
    if 10 <= y <= 11:
        return 8 <= x <= 23
    if 8 <= y <= 9:
        return 10 <= x <= 21
    return False


def paint_blast_foundry():
    # front: brick frame, iron arch trim, iron sill, lamp socket and one teal LED on the jamb
    name = "blast_foundry_front"
    img = K.new_img()
    px = img.load()
    paint_bricks(px, name)
    rng = K.rng_for(name + "/iron")
    cinder(px, 0, 26, S - 1, S - 1, rng)                        # sill (model y 0..3)
    for x in (3, 27):
        bolt(px, x, 28)
    for y in range(S):
        for x in range(S):
            if opening(x, y):
                px[x, y] = FIREBOX
    for y in range(1, 26):                                      # arch trim: 2 px iron round the opening
        for x in range(1, S - 1):
            if opening(x, y):
                continue
            near = min((abs(dx) + abs(dy)) for dx in (-2, -1, 0, 1, 2) for dy in (0, 1, 2)
                       if opening(x + dx, y + dy)) if any(
                opening(x + dx, y + dy) for dx in (-2, -1, 0, 1, 2) for dy in (0, 1, 2)) else 9
            if near == 1:
                px[x, y] = K.CINDER[1]
            elif near == 2:
                px[x, y] = K.CINDER[3] if (y < 12 or x < 16) else K.CINDER[2]
    rect(px, 14, 2, 17, 6, K.CINDER[1])                         # keystone
    frame(px, 14, 2, 17, 6, K.CINDER[3], K.CINDER[2])
    K.status_socket(px)
    K.led(px, 27, 11, K.T_CYAN, K.T_PLASMA)
    for x in range(2, 5):                                       # maker's ticks on the other jamb
        px[x, 5] = K.T_TEAL
    K.save_block(img, name)

    # side / top: the same bond with iron straps, so a loose controller is not a brick
    for name, top in (("blast_foundry", False), ("blast_foundry_top", True)):
        img = K.new_img()
        px = img.load()
        paint_bricks(px, name, top=top)
        rng = K.rng_for(name + "/iron")
        cinder(px, 0, 0, S - 1, 2, rng)
        cinder(px, 0, S - 3, S - 1, S - 1, rng)
        if top:
            cinder(px, 0, 0, 2, S - 1, rng)
            cinder(px, S - 3, 0, S - 1, S - 1, rng)
            for (x, y) in ((1, 1), (S - 3, 1), (1, S - 3), (S - 3, S - 3)):
                bolt(px, x, y)
        else:
            for x in (3, 15, 27):
                bolt(px, x, 0)
                bolt(px, x, S - 3)
        K.save_block(img, name)

    # door: iron plate on the opening (px x 6..25, y 8..25), strap hinges, sight-glass frame, vents
    name = "blast_foundry_door"
    img = K.new_img()
    px = img.load()
    rng = K.rng_for(name)
    cinder(px, 0, 0, S - 1, S - 1, rng, framed=False)
    frame(px, 6, 8, 25, 25, K.CINDER[3], K.mix(K.CINDER[2], K.A_DARK, 0.5))
    for y in (10, 24):                                          # strap hinges
        for x in range(6, 26):
            px[x, y] = K.CINDER[3]
        for x in (8, 15, 22):
            px[x, y] = K.A_LIGHT
    rect(px, 10, 12, 21, 23, K.CINDER[1])                       # glass frame (glass is px 12..19, 14..21)
    frame(px, 10, 12, 21, 23, K.CINDER[3], K.CINDER[2])
    frame(px, 11, 13, 20, 22, K.CINDER[2], K.CINDER[3])
    rect(px, 12, 14, 19, 21, GLASS_DARK)
    for x in (10, 21):                                          # tier ticks: the upper half is the 5-cube tier
        px[x, 17] = K.A_LIGHT
        px[x, 18] = K.A_DARK
    rect(px, 23, 14, 23, 21, K.A_LIGHT)                         # latch bar at the viewer's right
    rect(px, 24, 14, 24, 21, K.A_DARK)
    for x0 in (8, 14, 20):                                      # ash vents (model y 3.5..4.5; renderer glow)
        rect(px, x0, 23, x0 + 3, 24, DARK)
    K.save_block(img, name)

    # glass: dark, with a glint; the renderer draws the melt over it
    name = "blast_foundry_glass"
    img = K.new_img()
    px = img.load()
    rect(px, 0, 0, S - 1, S - 1, GLASS_DARK)
    for (x, y) in ((13, 17), (14, 16), (15, 15), (14, 17), (15, 16), (17, 20), (18, 19)):
        px[x, y] = GLASS_GLINT                                  # inside the 8 px window (12..19, 14..21)
    K.save_block(img, name)

    # parts: lip top with the pour channel (u 0..6, v 0..2), lip face (u 0..6, v 3..4),
    #        cheek (u 8..9.5, v 0..1.5), reveal iron (u 12..16, v 0..16)
    name = "blast_foundry_parts"
    img = K.new_img()
    px = img.load()
    rng = K.rng_for(name)
    cinder(px, 0, 0, S - 1, S - 1, rng, framed=False)
    cinder(px, 0, 0, 11, 3, rng)
    rect(px, 2, 0, 9, 3, DARK)                                  # channel
    cinder(px, 0, 6, 11, 7, rng, framed=False)
    for x in range(12):
        px[x, 6] = K.CINDER[3]
        px[x, 7] = K.mix(K.CINDER[2], K.A_DARK, 0.5)
    for x in range(16, 20):
        px[x, 0] = K.CINDER[3]
    for y in range(S):                                          # reveal: soot-dark iron
        for x in range(24, S):
            px[x, y] = K.mix(rng.choice(K.CINDER[:3]), FIREBOX, 0.45)
    K.save_block(img, name)

    # ---- renderer sprites (neutral; tinted and lit by the renderer) ----
    from PIL import Image

    name = "blast_foundry_melt"                                 # 8 x 8 design at 4x: one texel per glass pixel
    small = Image.new("RGBA", (8, 8), K.CLEAR)
    sp = small.load()
    rng = K.rng_for(name)
    for y in range(8):
        for x in range(8):
            v = 214 + int(20 * math.sin(x * 1.3 + y * 0.9)) + rng.choice((-14, 0, 0, 12))
            v = max(170, min(255, v))
            sp[x, y] = (v, v, v, 255)
    sp[2, 5] = (150, 150, 150, 255)                             # crust flecks
    sp[6, 2] = (158, 158, 158, 255)
    K.save_block(small.resize((S, S), Image.NEAREST), name)

    name = "blast_foundry_glow"                                 # three vent slots across the top two rows
    small = Image.new("RGBA", (16, 16), K.CLEAR)
    sp = small.load()
    for x0 in (0, 6, 12):
        for x in range(x0, x0 + 4):
            sp[x, 0] = WHITE
            sp[x, 1] = (236, 236, 236, 255)
    K.save_block(small.resize((S, S), Image.NEAREST), name)

    name = "blast_foundry_pour"                                 # falling streaks, tileable in V
    img = K.new_img()
    px = img.load()
    rng = K.rng_for(name)
    for x in range(S):
        phase = rng.randrange(S)
        gap = rng.choice((0, 0, 3, 5))
        for y in range(S):
            if gap and (y + phase) % 16 < gap:
                continue
            v = 255 if 8 <= x < 24 else 226
            if (y + phase) % 7 == 0:
                v -= 24
            px[x, y] = (v, v, v, 255)
    K.save_block(img, name)

    for name, top in (("foundry_wave", False), ("foundry_wave_top", True)):   # mortar lines only
        img = K.new_img()
        px = img.load()
        for y in range(S):
            for x in range(S):
                if (is_tile_joint(x, y) if top else (is_joint(x, y) or is_bed(y))):
                    px[x, y] = WHITE
        K.save_block(img, name)

    name = "foundry_ghost"                                      # outline of one missing shell block
    img = K.new_img()
    px = img.load()
    for i in range(S):
        if (i // 2) % 2 == 0 or i < 8 or i >= S - 8:            # dashed edge, solid corners
            for (x, y) in ((i, 0), (i, S - 1), (0, i), (S - 1, i)):
                px[x, y] = WHITE
        if i < 8 or i >= S - 8:
            for (x, y) in ((i, 1), (i, S - 2), (1, i), (S - 2, i)):
                px[x, y] = (214, 214, 214, 255)
    K.save_block(img, name)


# ---- Foundry Heat Port --------------------------------------------------------------------------
def paint_foundry_heat_port():
    name = "foundry_heat_port"                                  # brick face with an iron seat (px 6..25)
    img = K.new_img()
    px = img.load()
    paint_bricks(px, name)
    rng = K.rng_for(name + "/seat")
    cinder(px, 5, 5, 26, 26, rng)
    for (x, y) in ((6, 6), (24, 6), (6, 24), (24, 24)):
        bolt(px, x, y)
    rect(px, 8, 8, 23, 23, DARK)                                # under the collar
    K.save_block(img, name)

    # collar: face (u 0..8, v 0..8), rim (u 8..16, v 0..1)
    name = "foundry_heat_port_collar"
    img = K.new_img()
    px = img.load()
    rng = K.rng_for(name)
    C = K.COPPER
    rect(px, 0, 0, S - 1, S - 1, C[1])
    for y in range(16):
        for x in range(16):
            ring = min(x, y, 15 - x, 15 - y)
            if ring == 0:
                col = C[2] if (y == 0 or x == 0) else C[1]
            elif ring <= 2:
                col = C[0] if rng.random() < 0.8 else C[2]
            elif ring == 3:
                col = K.LAG[0]                                  # lagging groove (the glow ring sits here)
            elif ring == 4:
                col = C[1] if (y == 4 or x == 4) else C[2]
            else:
                col = DARK                                      # bore
            px[x, y] = col
    for (x, y) in ((1, 1), (13, 1), (1, 13), (13, 13)):         # corner bolts
        px[x, y] = C[2]
        px[x + 1, y + 1] = K.mix(C[1], K.A_DARK, 0.5)
    for x in range(16, S):                                      # rim strip
        px[x, 0] = C[2]
        px[x, 1] = C[1] if x % 4 else K.LAG[0]
    K.save_block(img, name)

    from PIL import Image
    name = "foundry_heat_port_glow"                             # bore + groove ring, 16 px design at 2x
    small = Image.new("RGBA", (16, 16), K.CLEAR)
    sp = small.load()
    for y in range(16):
        for x in range(16):
            ring = min(x, y, 15 - x, 15 - y)
            if ring == 3:
                sp[x, y] = (226, 226, 226, 255)
            elif ring >= 5:
                sp[x, y] = WHITE if ring >= 6 or (x + y) % 2 == 0 else (200, 200, 200, 255)
    K.save_block(small.resize((S, S), Image.NEAREST), name)


def paint():
    paint_foundry_brick()
    paint_blast_foundry()
    paint_foundry_heat_port()


# ---- models -------------------------------------------------------------------------------------
def _f(texture, uv=None, cull=None):
    out = {"texture": texture}
    if uv is not None:
        out["uv"] = list(uv)
    if cull is not None:
        out["cullface"] = cull
    return out


def _brick_model(lit):
    stem = "foundry_brick_lit" if lit else "foundry_brick"
    tex = {
        "brick": "nerotech:block/%s" % stem,
        "top": "nerotech:block/%s_top" % stem,
        "mortar": "nerotech:block/foundry_brick_mortar%s" % ("_lit" if lit else ""),
        "particle": "nerotech:block/%s" % stem,
    }
    els = [{
        "name": "mortar_core",
        "from": [0.5, 0, 0.5], "to": [15.5, 15.5, 15.5],
        "faces": {"north": _f("#mortar"), "south": _f("#mortar"), "west": _f("#mortar"),
                  "east": _f("#mortar"), "down": _f("#mortar", cull="down")},
    }]
    for i, y0 in enumerate((0.5, 4.5, 8.5, 12.5)):
        y1 = y0 + 3.5
        uv = [0, 16 - y1, 16, 16 - y0]
        faces = {d: _f("#brick", uv, d) for d in ("north", "south", "west", "east")}
        if i == 3:
            faces["up"] = _f("#top", cull="up")
        els.append({"name": "course_%d" % i, "from": [0, y0, 0], "to": [16, y1, 16], "faces": faces})
    return {"parent": "minecraft:block/block", "textures": tex, "elements": els}


def _controller_model():
    P = "#parts"
    tex = {
        "side": "nerotech:block/blast_foundry",
        "front": "nerotech:block/blast_foundry_front",
        "door": "nerotech:block/blast_foundry_door",
        "glass": "nerotech:block/blast_foundry_glass",
        "top": "nerotech:block/blast_foundry_top",
        "parts": "nerotech:block/blast_foundry_parts",
        "socket": "nerotech:block/blast_foundry_front",
        "particle": "nerotech:block/blast_foundry",
    }
    els = [
        {"name": "body", "from": [0, 0, 1], "to": [16, 16, 16], "faces": {
            "down": _f("#top", cull="down"), "up": _f("#top", cull="up"),
            "south": _f("#side", cull="south"), "west": _f("#side", cull="west"),
            "east": _f("#side", cull="east"), "north": _f("#front")}},
        {"name": "jamb_right", "from": [0, 0, 0], "to": [3, 16, 1], "faces": {
            "north": _f("#front", cull="north"), "west": _f("#side", (15, 0, 16, 16), "west"),
            "east": _f(P, (12, 0, 13, 16)), "up": _f("#top", cull="up"), "down": _f("#top", cull="down")}},
        {"name": "jamb_left", "from": [13, 0, 0], "to": [16, 16, 1], "faces": {
            "north": _f("#front", cull="north"), "east": _f("#side", (0, 0, 1, 16), "east"),
            "west": _f(P, (12, 0, 13, 16)), "up": _f("#top", cull="up"), "down": _f("#top", cull="down")}},
        {"name": "lintel", "from": [3, 12, 0], "to": [13, 16, 1], "faces": {
            "north": _f("#front", cull="north"), "up": _f("#top", cull="up"), "down": _f(P, (12, 0, 16, 1))}},
        {"name": "sill", "from": [3, 0, 0], "to": [13, 3, 1], "faces": {
            "north": _f("#front", cull="north"), "down": _f("#top", cull="down"), "up": _f(P, (12, 0, 16, 1))}},
    ]
    for nm, frm, to in (("arch_a", (3, 11, 0), (5, 12, 1)), ("arch_b", (11, 11, 0), (13, 12, 1)),
                        ("arch_c", (3, 10, 0), (4, 11, 1)), ("arch_d", (12, 10, 0), (13, 11, 1))):
        els.append({"name": nm, "from": list(frm), "to": list(to), "faces": {
            "north": _f("#front"), "down": _f(P, (12, 0, 14, 1)),
            "east": _f(P, (12, 0, 13, 1)), "west": _f(P, (12, 0, 13, 1))}})
    els += [
        {"name": "door", "from": [3, 3, 0.5], "to": [13, 12, 1], "faces": {"north": _f("#door")}},
        {"name": "sight_glass", "from": [6, 5, 0.25], "to": [10, 9, 0.5], "faces": {
            "north": _f("#glass"), "up": _f(P, (12, 0, 16, 0.5)), "down": _f(P, (12, 0, 16, 0.5)),
            "west": _f(P, (12, 0, 12.5, 4)), "east": _f(P, (12, 0, 12.5, 4))}},
        {"name": "pour_lip", "from": [5, 2, -1], "to": [11, 3, 1], "faces": {
            "north": _f(P, (0, 3, 6, 4)), "up": _f(P, (0, 0, 6, 2)), "down": _f(P, (0, 3, 6, 4)),
            "west": _f(P, (8, 0, 10, 1)), "east": _f(P, (8, 0, 10, 1))}},
        {"name": "lip_cheek_a", "from": [5, 3, -1], "to": [6, 3.5, 0.5], "faces": {
            "north": _f(P, (8, 0, 9, 0.5)), "up": _f(P, (8, 0, 9, 1.5)),
            "west": _f(P, (8, 0, 9.5, 0.5)), "east": _f(P, (8, 0, 9.5, 0.5))}},
        {"name": "lip_cheek_b", "from": [10, 3, -1], "to": [11, 3.5, 0.5], "faces": {
            "north": _f(P, (8, 0, 9, 0.5)), "up": _f(P, (8, 0, 9, 1.5)),
            "west": _f(P, (8, 0, 9.5, 0.5)), "east": _f(P, (8, 0, 9.5, 0.5))}},
        K.status_socket_element("#socket", 0.0),
    ]
    return {"parent": "minecraft:block/block", "textures": tex, "elements": els}


def _port_model():
    C = "#collar"
    face, rim = (0, 0, 8, 8), (8, 0, 16, 1)
    tex = {
        "brick": "nerotech:block/foundry_heat_port",
        "collar": "nerotech:block/foundry_heat_port_collar",
        "particle": "nerotech:block/foundry_heat_port",
    }
    els = [{"name": "body", "from": [0, 0, 0], "to": [16, 16, 16],
            "faces": {d: _f("#brick", cull=d) for d in ("down", "up", "north", "south", "west", "east")}}]
    boxes = {
        "north": ((4, 4, -1), (12, 12, 0)), "south": ((4, 4, 16), (12, 12, 17)),
        "west": ((-1, 4, 4), (0, 12, 12)), "east": ((16, 4, 4), (17, 12, 12)),
        "down": ((4, -1, 4), (12, 0, 12)), "up": ((4, 16, 4), (12, 17, 12)),
    }
    opposite = {"north": "south", "south": "north", "west": "east", "east": "west", "up": "down", "down": "up"}
    for d, (frm, to) in boxes.items():
        faces = {}
        for side in ("north", "south", "west", "east", "up", "down"):
            if side == opposite[d]:
                continue
            faces[side] = _f(C, face if side == d else rim)
        els.append({"name": "collar_" + d, "from": list(frm), "to": list(to), "faces": faces})
    return {"parent": "minecraft:block/block", "textures": tex, "elements": els}


def models():
    return {
        "models/block/blast_foundry.json": _controller_model(),
        "models/block/foundry_brick.json": _brick_model(False),
        "models/block/foundry_brick_lit.json": _brick_model(True),
        "models/block/foundry_heat_port.json": _port_model(),
    }


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
