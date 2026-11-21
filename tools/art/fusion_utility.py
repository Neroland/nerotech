"""A6 Fusion + Utility art package: Fuel Injector, Europium Flux Lattice, Induction Charger and
Annunciator Panel.

``paint()`` paints every texture the package owns (base art, one ``_parts`` sheet for the injector's
fittings and the sprites the three renderers draw). ``PULSE`` lists the injector front, the lit
lattice beads and the charger top. ``models()`` holds the block models as data; run this file
directly to (re)write them:

    python tools/art/fusion_utility.py        write the package's model JSON (replaces placeholders)

Silhouettes (16-unit model space, north = front):

* Fuel Injector         -- full fusion-casing body with one hazard collar band; on the front a manifold
                           plate carrying two gas couplings (viewer's left ringed blue = deuterium,
                           right ringed rose = helium-3) and the lamp socket; on the back a stepped
                           nozzle cone pointing into the reactor. Ring glow and nozzle glow are
                           renderer geometry.
* Europium Flux Lattice -- casing frame around a dark core set 2 px back; on every face a 3x3 lattice
                           of pale rods with node beads. The block occludes like a casing, so the
                           core closes it (no see-through). ``stable`` swaps the bead texture.
* Induction Charger     -- floor plate, three concentric coil rings rising toward a short centre post
                           that carries the lamp socket. Lit rings and the field column are renderer
                           geometry.
* Annunciator Panel     -- thin wall slab with a bezel and a hood over five lamp lenses in a row; each
                           lens is tied by a leader line to its engraved glyph (flame, empty hopper,
                           full crate, cloud, cracked gear) in two staggered rows so every glyph gets
                           6-7 px. Lit lamps and lit glyphs are renderer geometry.

Renderer sprites are white so the renderer can tint them; no heat colour is baked in.
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
VOID = (6, 9, 12, 255)
WHITE = (255, 255, 255, 255)
STEEL = [(150, 160, 168, 255), (122, 132, 140, 255), (186, 196, 204, 255), (92, 102, 110, 255)]
DEUTERIUM = K.MEDIA["deuterium"] + (255,)
HELIUM_3 = K.MEDIA["helium_3"] + (255,)

PULSE = [
    K.pulse("fuel_injector_front"),
    K.pulse("europium_flux_lattice_beads", amp=0.34),
    K.pulse("induction_charger_top", amp=0.26),
]

# Annunciator layout, 32x face pixels (the face shows px 2..29). One row per alarm bit, in
# MachineAlarms order. The renderer (AnnunciatorPanelRenderer) carries the same numbers.
LAMP_Y0, LAMP_Y1 = 6, 9                       # lens rows (inclusive)
LAMP_X = [4 + 5 * k for k in range(5)]       # lens left edge; 4 px wide
GLYPHS = [
    ("overheat", 4, 12, ("...#..",
                         "..##..",
                         ".###.#",
                         ".#####",
                         "##.###",
                         "##..##",
                         ".####.")),
    ("starved", 8, 20, ("##...##",
                        "#.....#",
                        "#.....#",
                        ".#...#.",
                        ".#...#.",
                        "..#.#..",
                        "..#.#..")),
    ("full", 13, 12, ("..##..",
                      ".####.",
                      "######",
                      "##..##",
                      "#.##.#",
                      "##..##",
                      "######")),
    ("polluting", 17, 20, ("..##...",
                           ".####..",
                           "######.",
                           "#######",
                           ".......",
                           ".#.#.#.",
                           "#.#.#..")),
    ("failing", 22, 12, (".#..#.",
                         "####.#",
                         "##..##",
                         "#....#",
                         "##..##",
                         "#.####",
                         ".#..#.")),
]

# Charger coil rings: (inset in model units, top y). Inner first; the renderer lights them outward.
RINGS = [(5, 4.5), (3, 4.0), (1, 3.5)]


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
    for x in range(x0, x1 + 1):
        px[x, y0] = light
        px[x, y1] = dark
    for y in range(y0, y1 + 1):
        px[x0, y] = light
        px[x1, y] = dark


def conduit_ring(px):
    """The fusion casing's dashed conduit ring, so the fusion parts read as one shell."""
    for i in range(3, 29):
        c = K.T_DEEP if i % 4 < 2 else K.mix(K.T_DEEP, K.T_TEAL, 0.6)
        px[i, 2] = c
        px[i, 29] = c
        px[2, i] = c
        px[29, i] = c
    for i in (8, 16, 24):
        px[i, 2] = K.T_TEAL
        px[i, 29] = K.T_TEAL
        px[2, i] = K.T_TEAL
        px[29, i] = K.T_TEAL


def dull(col, t=0.45):
    return K.mix(col, K.A_DARK, t)


# ---- Fuel Injector ------------------------------------------------------------------------------
def paint_fuel_injector():
    # side: casing plate with the one hazard collar band
    img = K.machine_base("fuel_injector")
    px = img.load()
    conduit_ring(px)
    K.haz_band(px, 13, 18)
    for x in range(2, 30):
        px[x, 12] = K.A_DARK
        px[x, 19] = K.A_DARK
        px[x, 20] = K.mix(K.A_LIGHT, K.A_DARK, 0.55)
    K.rivets(img, ((7, 6), (24, 6), (7, 24), (24, 24)))
    K.save_block(img, "fuel_injector")

    # top / bottom: casing plate with a feed seam running front to back
    img = K.machine_base("fuel_injector_top")
    px = img.load()
    conduit_ring(px)
    for y in range(5, 27):
        px[15, y] = K.A_DARK
        px[16, y] = K.ALLOY[2]
    for y in range(6, 26, 5):
        rect(px, 13, y, 18, y + 1, K.ALLOY[3])
        px[13, y] = K.A_LIGHT
        px[18, y + 1] = K.A_DARK
    K.save_block(img, "fuel_injector_top")

    # front: flow LEDs over the manifold seat, hazard strip under it, lamp socket top right
    img = K.machine_base("fuel_injector_front")
    px = img.load()
    conduit_ring(px)
    K.status_socket(px)
    K.recess(px, 4, 8, 22, 13)
    for x in (6, 11, 16):
        K.led(px, x, 10, K.T_CYAN, K.T_PLASMA)
    px[21, 10] = K.T_TEAL                                   # chevron tip: flow runs toward the socket
    px[20, 9] = K.T_TEAL
    px[20, 11] = K.T_TEAL
    rect(px, 4, 15, 27, 24, DARK)                           # manifold seat (hidden by the plate)
    K.haz_band(px, 26, 27, 4, 27)
    K.save_block(img, "fuel_injector_front")

    # back (faces the plasma): the nozzle cone seen end-on, concentric steps down to the bore
    img = K.machine_base("fuel_injector_back")
    px = img.load()
    rng = K.rng_for("fuel_injector_back")
    conduit_ring(px)
    K.recess(px, 5, 5, 26, 26)
    for i in range(6, 26):                                  # seat ring: dim teal ticks
        c = K.T_DEEP if i % 3 else K.mix(K.T_DEEP, K.T_TEAL, 0.7)
        px[i, 6] = c
        px[i, 25] = c
        px[6, i] = c
        px[25, i] = c
    noise_rect(px, 8, 8, 23, 23, STEEL[1:2] + STEEL[3:], rng)   # step 1
    frame(px, 8, 8, 23, 23, STEEL[0], K.A_DARK)
    noise_rect(px, 11, 11, 20, 20, STEEL[:2], rng)          # step 2
    frame(px, 11, 11, 20, 20, STEEL[2], STEEL[3])
    rect(px, 14, 14, 17, 17, STEEL[2])                      # tip
    rect(px, 15, 15, 16, 16, K.T_DEEP)                      # bore
    K.save_block(img, "fuel_injector_back")

    # parts sheet
    img = K.new_img()
    px = img.load()
    rng = K.rng_for("fuel_injector_parts")
    noise_rect(px, 0, 0, S - 1, S - 1, STEEL[:2], rng)
    for i, gas in enumerate((DEUTERIUM, HELIUM_3)):         # coupling faces: (0,0) blue, (8,0) rose
        ox = i * 8
        noise_rect(px, ox, 0, ox + 7, 7, STEEL[:2], rng)
        frame(px, ox, 0, ox + 7, 7, STEEL[2], STEEL[3])
        for y in range(8):
            for x in range(8):
                r = math.hypot(x - 3.5, y - 3.5)
                if 2.2 <= r <= 3.7:
                    px[ox + x, y] = dull(gas, 0.35)
                elif r < 2.2:
                    px[ox + x, y] = DARK
        px[ox + 2, 1] = dull(gas, 0.1)                      # one catch-light on the ring
        # collar side strips: (16,0) blue, (16,2) rose
        for x in range(16, 24):
            px[x, i * 2] = STEEL[2] if x % 4 else STEEL[0]
            px[x, i * 2 + 1] = dull(gas, 0.3)
    rect(px, 24, 0, 27, 1, STEEL[3])                        # pipe stub side
    px[24, 0] = STEEL[0]
    px[25, 0] = STEEL[0]
    rect(px, 24, 4, 27, 7, STEEL[0])                        # pipe stub end: rim + bore
    rect(px, 25, 5, 26, 6, VOID)
    noise_rect(px, 0, 8, 23, 17, STEEL[1:2] + STEEL[3:], rng)   # manifold plate (24 x 10)
    frame(px, 0, 8, 23, 17, STEEL[0], K.A_DARK)
    for x in range(1, 23):
        px[x, 12] = K.A_DARK                                # gallery seam between the couplings
        px[x, 13] = STEEL[0]
    for bx in (1, 11, 12, 22):
        px[bx, 9] = K.A_LIGHT
        px[bx, 16] = K.A_LIGHT
    for x in range(16):                                     # nozzle step sides: (0,18) 16 x 2
        px[x, 18] = STEEL[2] if x % 5 else STEEL[0]
        px[x, 19] = STEEL[3]
    K.save_block(img, "fuel_injector_parts")

    # renderer sprites: coupling ring (8x8) and nozzle glow (16x16), white
    img = K.new_img().resize((8, 8))
    px = img.load()
    for y in range(8):
        for x in range(8):
            px[x, y] = WHITE if 2.2 <= math.hypot(x - 3.5, y - 3.5) <= 3.7 else K.CLEAR
    K.save_block(img, "fuel_injector_ring")
    img = K.new_img().resize((16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            r = math.hypot(x - 7.5, y - 7.5)
            on = r < 3.2 or (r < 5.6 and (x + y) % 2 == 0) or (r < 7.6 and x % 2 == 0 and y % 2 == 0)
            px[x, y] = WHITE if on else K.CLEAR
    K.save_block(img, "fuel_injector_glow")


# ---- Europium Flux Lattice ----------------------------------------------------------------------
def paint_europium_flux_lattice():
    # frame: casing language (the bars show the outer 4 px of each face)
    img = K.machine_base("europium_flux_lattice")
    px = img.load()
    conduit_ring(px)
    rect(px, 4, 4, 27, 27, DARK)
    K.save_block(img, "europium_flux_lattice")

    # core: the dark the lattice floats over, a faint deep-teal bloom toward the centre
    img = K.new_img()
    px = img.load()
    rng = K.rng_for("europium_flux_lattice_core")
    for y in range(S):
        for x in range(S):
            d = math.hypot(x - 15.5, y - 15.5) / 16.0
            base = K.mix(K.T_DEEP, VOID, min(1.0, 0.25 + d * 0.9))
            px[x, y] = K.mix(base, VOID, 0.5) if rng.random() < 0.3 else base
    K.save_block(img, "europium_flux_lattice_core")

    # rod: pale europium, lit from the top left
    img = K.new_img()
    px = img.load()
    rng = K.rng_for("europium_flux_lattice_rod")
    for y in range(S):
        for x in range(S):
            px[x, y] = K.EU_ROD[0] if (x + y) % 2 == 0 or rng.random() < 0.35 else K.EU_ROD[1]
    for i in range(0, S, 8):
        for j in range(S):
            px[i, j] = K.mix(K.EU_ROD[1], K.A_DARK, 0.25)
    K.save_block(img, "europium_flux_lattice_rod")

    # beads: 4x4 tiles (models address tile 0,0). Lit = plasma; dark = dull glass.
    lit = [[K.T_CYAN, K.T_PLASMA, K.T_PLASMA, K.T_CYAN],
           [K.T_PLASMA, K.T_GLOW, K.T_PLASMA, K.T_CYAN],
           [K.T_PLASMA, K.T_PLASMA, K.T_CYAN, K.T_TEAL],
           [K.T_CYAN, K.T_CYAN, K.T_TEAL, K.T_TEAL]]
    g0 = K.mix(K.EU_ROD[1], K.A_DARK, 0.55)
    g1 = K.mix(K.EU_ROD[1], K.A_DARK, 0.75)
    off = [[g0, g0, g0, g1],
           [g0, K.EU_ROD[1], g0, g1],
           [g0, g0, K.T_DEEP, g1],
           [g1, g1, g1, K.A_DARK]]
    for name, tile in (("europium_flux_lattice_beads", lit), ("europium_flux_lattice_beads_dark", off)):
        img = K.new_img()
        px = img.load()
        for y in range(S):
            for x in range(S):
                px[x, y] = tile[y % 4][x % 4]
        K.save_block(img, name)


# ---- Induction Charger --------------------------------------------------------------------------
def _charger_side(name, socket):
    img = K.machine_base(name)
    px = img.load()
    rng = K.rng_for(name + "/post")
    for x in range(0, S):                                             # coil edge strip (uv y 11-12)
        px[x, 22] = K.T_DEEP if x % 4 < 2 else K.A_DARK
        px[x, 23] = K.A_DARK
    noise_rect(px, 12, 14, 19, 25, STEEL[1:2] + STEEL[3:], rng)        # post face (uv 6,7 - 10,13)
    for y in range(14, 26):
        px[12, y] = STEEL[0]
        px[19, y] = K.A_DARK
    rect(px, 12, 14, 19, 14, STEEL[2])
    rect(px, 12, 22, 19, 22, K.A_DARK)                                # collar line near the foot
    rect(px, 12, 23, 19, 23, K.T_DEEP)
    if socket:                                                        # lamp socket on the post
        rect(px, 13, 15, 18, 20, K.A_DARK)
        rect(px, 14, 16, 17, 19, K.T_DEEP)
    rect(px, 0, 26, S - 1, 26, K.A_LIGHT)                             # plate edge (uv y 13-16)
    noise_rect(px, 0, 27, S - 1, 30, K.ALLOY, rng)
    rect(px, 0, 28, S - 1, 28, K.T_DEEP)
    for x in range(3, S, 8):
        px[x, 28] = K.mix(K.T_DEEP, K.T_TEAL, 0.6)
    rect(px, 0, 31, S - 1, 31, K.A_DARK)
    K.save_block(img, name)


def _ring_band(inset):
    """The 2 px band of a ring in 32x top pixels: yields (x, y)."""
    a, b = int(inset * 2), S - 1 - int(inset * 2)
    for i in range(a, b + 1):
        for w in (0, 1):
            yield i, a + w
            yield i, b - w
            yield a + w, i
            yield b - w, i


def paint_induction_charger():
    _charger_side("induction_charger", False)
    _charger_side("induction_charger_front", True)

    img = K.machine_base("induction_charger_top")
    px = img.load()
    for inset, _top in RINGS:                                         # unlit coil windings
        a, b = int(inset * 2), S - 1 - int(inset * 2)
        for x, y in _ring_band(inset):
            edge = x in (a, b) or y in (a, b)
            px[x, y] = (STEEL[1] if (x + y) % 3 else STEEL[3]) if edge else K.T_DEEP
    for x, y in ((15, 8), (15, 22), (8, 15), (22, 15)):               # compass pips (PULSE)
        rect(px, x, y, x + 1, y + 1, K.T_CYAN)
    rect(px, 12, 12, 19, 19, STEEL[1])                                # post cap
    frame(px, 12, 12, 19, 19, STEEL[2], STEEL[3])
    K.led(px, 15, 15, K.T_CYAN, K.T_PLASMA)
    K.save_block(img, "induction_charger_top")

    for k, (inset, _top) in enumerate(RINGS):                         # renderer: one lit ring each
        img = K.new_img()
        px = img.load()
        for x, y in _ring_band(inset):
            px[x, y] = WHITE
        K.save_block(img, "induction_charger_ring_%d" % k)

    img = K.new_img()                                                 # renderer: field column
    px = img.load()
    rng = K.rng_for("induction_charger_field")
    for x in range(1, S, 3):
        y = rng.randrange(S)
        for _ in range(3):
            run = rng.randrange(3, 8)
            for i in range(run):
                if (i + x) % 4 != 3:
                    px[x, (y + i) % S] = WHITE
            y += run + rng.randrange(4, 9)
    K.save_block(img, "induction_charger_field")


# ---- Annunciator Panel --------------------------------------------------------------------------
def paint_annunciator_panel():
    for name in ("annunciator_panel", "annunciator_panel_top"):
        img = K.machine_base(name)
        px = img.load()
        if name.endswith("_top"):
            for x in range(3, 29):
                px[x, 15] = K.A_DARK
        K.save_block(img, name)

    img = K.machine_base("annunciator_panel_front")
    px = img.load()
    rng = K.rng_for("annunciator_panel_front")
    frame(px, 2, 2, 29, 29, K.A_LIGHT, K.A_DARK)                      # bezel (2 px)
    frame(px, 3, 3, 28, 28, K.mix(K.A_LIGHT, K.ALLOY[0], 0.45), K.mix(K.A_DARK, K.ALLOY[0], 0.35))
    field = [K.mix(c, K.A_DARK, 0.12) for c in K.ALLOY]
    noise_rect(px, 4, 4, 27, 27, field, rng)
    rect(px, 4, 4, 27, 4, K.A_DARK)                                   # hood shadow
    rect(px, 4, 5, 27, 5, VOID)
    lens = (58, 66, 74, 255)
    for x0 in LAMP_X:                                                 # unlit lenses: smoked grey
        rect(px, x0, LAMP_Y0, x0 + 3, LAMP_Y1, lens)
        rect(px, x0, LAMP_Y1, x0 + 3, LAMP_Y1, (36, 42, 48, 255))
        px[x0, LAMP_Y0] = (120, 134, 144, 255)
        px[x0 + 1, LAMP_Y0] = (88, 98, 108, 255)
        px[x0 + 3, LAMP_Y1] = VOID
    line = K.mix(K.A_LIGHT, K.A_DARK, 0.2)
    for k, (_n, gx, gy, rows) in enumerate(GLYPHS):                   # leader lines, then glyphs
        lx = LAMP_X[k] + 2
        if k in (1, 3):
            lx = 11 if k == 1 else 20
        for y in range(LAMP_Y1 + 1, gy - 1):
            px[lx, y] = line
        for j, row in enumerate(rows):
            for i, ch in enumerate(row):
                if ch == "#":
                    px[gx + i, gy + j] = VOID
        for j, row in enumerate(rows):                                # catch-light under the cut
            for i, ch in enumerate(row):
                below = rows[j + 1][i] if j + 1 < len(rows) else "."
                if ch == "#" and below != "#" and gy + j + 1 <= 27:
                    px[gx + i, gy + j + 1] = K.mix(K.A_LIGHT, K.A_DARK, 0.3)
    K.save_block(img, "annunciator_panel_front")

    img = K.new_img()                                                 # renderer: the lit glyphs
    px = img.load()
    for _n, gx, gy, rows in GLYPHS:
        for j, row in enumerate(rows):
            for i, ch in enumerate(row):
                if ch == "#":
                    px[gx + i, gy + j] = WHITE
    K.save_block(img, "annunciator_panel_glyphs")


def paint():
    paint_fuel_injector()
    paint_europium_flux_lattice()
    paint_induction_charger()
    paint_annunciator_panel()


# ---- models -------------------------------------------------------------------------------------
_DIRS = ("north", "south", "west", "east", "up", "down")
_OPPOSITE = {"north": "south", "south": "north", "west": "east", "east": "west", "up": "down", "down": "up"}


def _el(name, frm, to, default="#side", **faces):
    """One element. ``faces`` maps a direction to a texture key, ``(key, uv)`` or None (omit);
    unnamed faces use ``default``."""
    out = {}
    for d in _DIRS:
        spec = faces[d] if d in faces else default
        if spec is None:
            continue
        if isinstance(spec, tuple):
            out[d] = {"uv": list(spec[1]), "texture": spec[0]}
        else:
            out[d] = {"texture": spec}
    return {"name": name, "from": list(frm), "to": list(to), "faces": out}


def _cull(el, *dirs):
    for d in dirs:
        if d in el["faces"]:
            el["faces"][d]["cullface"] = d
    return el


def _fuel_injector():
    P = "#parts"
    body = _cull(_el("body", (0, 0, 0), (16, 16, 16), north="#front", south="#back", up="#top", down="#top"),
                 *_DIRS)
    els = [body,
           _el("manifold", (2, 3.5, -0.5), (14, 8.5, 0),
               north=(P, (0, 4, 12, 9)), south=None, west=(P, (0, 4, 0.5, 9)), east=(P, (11.5, 4, 12, 9)),
               up=(P, (0, 4, 12, 4.5)), down=(P, (0, 8.5, 12, 9)))]
    # viewer's left (model high x) = deuterium, blue; viewer's right = helium-3, rose
    for name, x0, face_u, strip_v in (("deuterium", 9.5, 0, 0), ("helium_3", 2.5, 4, 1)):
        strip = (P, (8, strip_v, 12, strip_v + 1))
        els.append(_el("coupling_" + name, (x0, 4, -1.5), (x0 + 4, 8, -0.5),
                       north=(P, (face_u, 0, face_u + 4, 4)), south=None,
                       west=strip, east=strip, up=strip, down=strip))
        stub = (P, (12, 0, 14, 1))
        els.append(_el("stub_" + name, (x0 + 1, 5, -2.5), (x0 + 3, 7, -1.5),
                       north=(P, (12, 2, 14, 4)), south=None, west=stub, east=stub, up=stub, down=stub))
    for i, (a, z) in enumerate(((4, 16), (5.5, 17), (7, 18))):
        side = (P, (0, 9, 16 - 2 * a, 10))
        els.append(_el("nozzle_%d" % i, (a, a, z), (16 - a, 16 - a, z + 1),
                       north=None, south=("#back", (a, a, 16 - a, 16 - a)),
                       west=side, east=side, up=side, down=side))
    els.append(K.status_socket_element("#front", 0.0))
    return {
        "parent": "minecraft:block/block",
        "textures": {
            "front": "nerotech:block/fuel_injector_front",
            "back": "nerotech:block/fuel_injector_back",
            "side": "nerotech:block/fuel_injector",
            "top": "nerotech:block/fuel_injector_top",
            "parts": "nerotech:block/fuel_injector_parts",
            "casing": "nerotech:block/fuel_injector",
            "lamp": "nerotech:block/status_lamp",
            "particle": "nerotech:block/fuel_injector",
        },
        "elements": els,
    }


def _face_box(face, u0, u1, v0, v1, d0, d1):
    """A box given in a face's own axes (u, v across the face, d = depth in from it)."""
    if face == "north":
        return (u0, v0, d0), (u1, v1, d1)
    if face == "south":
        return (u0, v0, 16 - d1), (u1, v1, 16 - d0)
    if face == "west":
        return (d0, v0, u0), (d1, v1, u1)
    if face == "east":
        return (16 - d1, v0, u0), (16 - d0, v1, u1)
    if face == "down":
        return (u0, d0, v0), (u1, d1, v1)
    return (u0, 16 - d1, v0), (u1, 16 - d0, v1)


def _lattice(stable):
    C = "#casing"
    els = []
    for a in (0, 14):                                   # 12 frame bars
        for b in (0, 14):
            els.append(_cull(_el("bar_x", (0, a, b), (16, a + 2, b + 2), default=C), *_DIRS))
            els.append(_cull(_el("bar_y", (a, 2, b), (a + 2, 14, b + 2), default=C, up=None, down=None), *_DIRS))
            els.append(_cull(_el("bar_z", (a, b, 2), (a + 2, b + 2, 14), default=C, north=None, south=None),
                             *_DIRS))
    els.append(_el("core", (2, 2, 2), (14, 14, 14), default="#core"))
    centres = (4.5, 8.0, 11.5)
    for face in _DIRS:
        back = _OPPOSITE[face]
        for c in centres:                               # rods, 1 px thick, half a pixel proud of the core
            for along_v in (True, False):
                if along_v:
                    frm, to = _face_box(face, c - 0.5, c + 0.5, 2, 14, 0.5, 1.5)
                else:
                    frm, to = _face_box(face, 2, 14, c - 0.5, c + 0.5, 0.5, 1.5)
                spec = {d: ("#rod", (0, 0, 1, 12) if along_v else (0, 0, 12, 1)) for d in _DIRS}
                spec[back] = None
                els.append(_el("rod", frm, to, **spec))
        for cu in centres:                              # node beads, flush with the face
            for cv in centres:
                frm, to = _face_box(face, cu - 1, cu + 1, cv - 1, cv + 1, 0, 2)
                spec = {d: ("#bead", (0, 0, 2, 2)) for d in _DIRS}
                spec[back] = None
                bead = _cull(_el("bead", frm, to, **spec), face)
                if stable:
                    bead["shade"] = False
                    bead["light_emission"] = 15
                els.append(bead)
    return {
        "parent": "minecraft:block/block",
        "textures": {
            "casing": "nerotech:block/europium_flux_lattice",
            "core": "nerotech:block/europium_flux_lattice_core",
            "rod": "nerotech:block/europium_flux_lattice_rod",
            "bead": "nerotech:block/europium_flux_lattice_beads" + ("" if stable else "_dark"),
            "particle": "nerotech:block/europium_flux_lattice",
        },
        "elements": els,
    }


def _induction_charger():
    plate = _el("floor_plate", (0, 0, 0), (16, 3, 16), up=("#top", (0, 0, 16, 16)), down="#side",
                north=("#side", (0, 13, 16, 16)), south=("#side", (0, 13, 16, 16)),
                west=("#side", (0, 13, 16, 16)), east=("#side", (0, 13, 16, 16)))
    els = [_cull(plate, "north", "south", "west", "east", "down")]
    for k, (a, top) in enumerate(RINGS):
        b = 16 - a
        for name, frm, to in (("n", (a, 3, a), (b, top, a + 1)), ("s", (a, 3, b - 1), (b, top, b)),
                              ("w", (a, 3, a + 1), (a + 1, top, b - 1)), ("e", (b - 1, 3, a + 1), (b, top, b - 1))):
            edge = ("#side", (0, 11, 8, 11.5))
            els.append(_el("coil_%d_%s" % (k, name), frm, to,
                           up=("#top", (frm[0], frm[2], to[0], to[2])), down=None,
                           north=edge, south=edge, west=edge, east=edge))
    post_uv = (6, 7, 10, 13)
    els.append(_el("centre_post", (6, 3, 6), (10, 9, 10), up=("#top", (6, 6, 10, 10)), down=None,
                   north=("#front", post_uv), south=("#side", post_uv), west=("#side", post_uv),
                   east=("#side", post_uv)))
    sock = ("#front", (7, 8, 9, 10))
    els.append(_el("status_lamp_socket", (7, 6, 5.5), (9, 8, 6), north=sock, south=None, west=sock, east=sock,
                   up=sock, down=sock))
    return {
        "parent": "minecraft:block/block",
        "textures": {
            "top": "nerotech:block/induction_charger_top",
            "side": "nerotech:block/induction_charger",
            "front": "nerotech:block/induction_charger_front",
            "lamp": "nerotech:block/status_lamp",
            "particle": "nerotech:block/induction_charger",
        },
        "elements": els,
    }


def _annunciator_panel():
    F = "#front"
    els = [_el("panel_body", (1, 1, 14), (15, 15, 16), north=(F, (1, 1, 15, 15)), south=("#side", (1, 1, 15, 15)),
               west=("#side", (14, 1, 16, 15)), east=("#side", (0, 1, 2, 15)),
               up=("#top", (1, 14, 15, 16)), down=("#top", (1, 0, 15, 2)))]
    long_edge = ("#side", (1, 1, 15, 2))
    short_edge = ("#side", (1, 2, 2, 14))
    dot = ("#side", (1, 1, 2, 2))
    for name, frm, to in (("bezel_top", (1, 14, 13), (15, 15, 14)), ("bezel_bottom", (1, 1, 13), (15, 2, 14)),
                          ("bezel_left", (14, 2, 13), (15, 14, 14)), ("bezel_right", (1, 2, 13), (2, 14, 14))):
        u = (F, (16 - to[0], 16 - to[1], 16 - frm[0], 16 - frm[1]))
        wide = to[0] - frm[0] > 2
        els.append(_el(name, frm, to, north=u, south=None,
                       west=dot if wide else short_edge, east=dot if wide else short_edge,
                       up=long_edge if wide else dot, down=long_edge if wide else dot))
    hood = ("#side", (2, 2, 14, 2.5))
    els.append(_el("hood", (2, 13.25, 13), (14, 13.75, 14), north=hood, south=None, west=dot, east=dot,
                   up=("#side", (2, 3, 14, 4)), down=("#side", (2, 4, 14, 5))))
    for k, x0 in enumerate(LAMP_X):                     # lenses stand half a pixel proud
        u0, v0 = x0 / 2.0, LAMP_Y0 / 2.0
        u1, v1 = u0 + 2, (LAMP_Y1 + 1) / 2.0
        uv = (F, (u0, v0, u1, v1))
        els.append(_el("lamp_window_%d" % k, (16 - u1, 16 - v1, 13.5), (16 - u0, 16 - v0, 14),
                       north=uv, south=None, west=uv, east=uv, up=uv, down=uv))
    return {
        "parent": "minecraft:block/block",
        "textures": {
            "top": "nerotech:block/annunciator_panel_top",
            "side": "nerotech:block/annunciator_panel",
            "front": "nerotech:block/annunciator_panel_front",
            "particle": "nerotech:block/annunciator_panel",
        },
        "elements": els,
    }


def models():
    return {
        "models/block/fuel_injector.json": _fuel_injector(),
        "models/block/europium_flux_lattice.json": _lattice(False),
        "models/block/europium_flux_lattice_stable.json": _lattice(True),
        "models/block/induction_charger.json": _induction_charger(),
        "models/block/annunciator_panel.json": _annunciator_panel(),
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
