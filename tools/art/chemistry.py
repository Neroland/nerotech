"""A3 Chemistry art package: Centrifuge, Cryo Distiller, Atmospheric Condenser, Reaction Vat and the
Process Tank (four column models).

Each machine is declared ONCE as a list of boxes (``MACHINES``). From that list this module builds

* the block model (explicit elements, automatic UVs, so a face shows the part of its texture that
  lies under it), and
* five projection textures per machine: ``<id>_front`` (north view), ``<id>`` (east view),
  ``<id>_west``, ``<id>_back`` and ``<id>_top``. A projection is the machine as seen square-on from
  that side, so texture and geometry cannot drift apart.

The renderer sprites (drum band, frost, vapour, louvres, paddle, bubbles) are painted here too. They
are white or neutral where the renderer tints them; no heat colour reaches a texture.

``paint()`` is additive like every other painter. ``write_models()`` (called by ``paint()``) is
additive too; ``--force`` replaces both.
"""
import math

from art import common as K

MOD = "nerotech"

# ---- materials (ramps) ----------------------------------------------------------------------------
_M = {
    "alloy": list(K.ALLOY),
    "dark": [K.ALLOY[2], K.mix(K.ALLOY[2], K.A_DARK, 0.45)],
    "light": [K.ALLOY[3], K.ALLOY[1], K.mix(K.ALLOY[3], K.A_LIGHT, 0.25)],
    "enamel": list(K.ENAMEL),
    "pipe": [K.mix(K.ALLOY[3], K.A_LIGHT, 0.45), K.mix(K.ALLOY[3], K.A_LIGHT, 0.2)],
}


def B(name, x0, y0, z0, x1, y1, z1, mat="alloy", **extra):
    box = {"name": name, "from": (x0, y0, z0), "to": (x1, y1, z1), "mat": mat}
    box.update(extra)
    return box


# socket_z: model z of the face the status lamp sits on. The renderers pass the same value to
# StatusLampRenderer.submit: keep the two in step.
MACHINES = {
    # Squat base, eight-sided drum, domed cap, a low outlet east and a high outlet west, and the
    # instrument post that carries the lamp.
    "centrifuge": {
        "socket_z": 0.0,
        "boxes": [
            B("base", 0, 0, 0, 16, 4, 16),
            B("drum_wide", 3, 4, 5, 13, 13, 11, "light"),
            B("drum_deep", 5, 4, 3, 11, 13, 13, "light"),
            B("drum_core", 4, 4, 4, 12, 13, 12, "light"),
            B("cap", 4, 13, 4, 12, 15, 12, "dark"),
            B("cap_dome", 6, 15, 6, 10, 16, 10, "enamel"),
            B("outlet_low", 13, 6, 6, 16, 9, 10, "pipe"),
            B("outlet_high", 0, 9, 6, 3, 12, 10, "pipe"),
            B("lamp_post", 0, 4, 0, 4, 15, 2, "dark"),
        ],
    },
    # Narrow column on a wide foot, three tray collars, a condenser loop on the east side and a
    # gauge mast for the lamp.
    "cryo_distiller": {
        "socket_z": 1.0,
        "boxes": [
            B("foot", 1, 0, 1, 15, 3, 15),
            B("column", 5, 3, 5, 11, 16, 11, "light"),
            B("tray_low", 4, 5, 4, 12, 6, 12, "enamel"),
            B("tray_mid", 4, 9, 4, 12, 10, 12, "enamel"),
            B("tray_high", 4, 13, 4, 12, 14, 12, "enamel"),
            B("coil_low", 11, 4, 7, 14, 5, 9, "pipe"),
            B("coil_rise", 13, 5, 7, 14, 11, 9, "pipe"),
            B("coil_high", 11, 11, 7, 14, 12, 9, "pipe"),
            B("coil_mid", 11, 7, 7, 13, 8, 9, "pipe"),
            B("gauge_mast", 1, 3, 1, 3, 15, 3, "dark"),
        ],
    },
    # Receiver bulb with a sight window, narrow neck, hollow louvred intake bell on top.
    "atmospheric_condenser": {
        "socket_z": 1.5,
        "boxes": [
            B("foot", 2, 0, 2, 14, 2, 14, "dark"),
            B("bulb", 3, 2, 3, 13, 7, 13),
            B("bulb_shoulder", 4, 7, 4, 12, 8, 12, "light"),
            B("neck", 6, 8, 6, 10, 10, 10, "dark"),
            B("bell_floor", 3, 10, 3, 13, 11, 13, "dark"),
            B("bell_north", 2, 10, 2, 14, 16, 3, "light"),
            B("bell_south", 2, 10, 13, 14, 16, 14, "light"),
            B("bell_west", 2, 10, 3, 3, 16, 13, "light"),
            B("bell_east", 13, 10, 3, 14, 16, 13, "light"),
            B("lamp_plate", 0.5, 11.5, 1.5, 3.5, 14.5, 2, "dark"),
        ],
    },
    # Glass-fronted vessel on four legs, agitator motor box on top, an inlet stub on either side.
    "reaction_vat": {
        "socket_z": 3.0,
        "translucent": True,
        "boxes": [
            B("leg_nw", 1, 0, 3, 3, 2, 5, "dark"),
            B("leg_ne", 13, 0, 3, 15, 2, 5, "dark"),
            B("leg_sw", 1, 0, 11, 3, 2, 13, "dark"),
            B("leg_se", 13, 0, 11, 15, 2, 13, "dark"),
            B("floor", 1, 2, 3, 15, 3, 13),
            B("back", 1, 3, 12, 15, 12, 13),
            B("wall_west", 1, 3, 3, 2, 12, 12),
            B("wall_east", 14, 3, 3, 15, 12, 12),
            B("sill", 2, 3, 3, 14, 4, 4, "light"),
            B("lintel", 2, 11, 3, 14, 12, 4, "light"),
            B("glass", 2, 4, 3.25, 14, 11, 3.75, "glass", faces=("north", "south"), tex="glass"),
            B("shoulder", 2, 12, 4, 14, 13, 12, "enamel"),
            B("motor", 5, 13, 5, 11, 16, 11, "dark"),
            B("stub_west", 0, 7, 7, 1, 9, 9, "pipe"),
            B("stub_east", 15, 7, 7, 16, 9, 9, "pipe"),
            B("lamp_house", 1, 12, 3, 4, 15, 5, "dark"),
        ],
    },
}

VIEWS = ("front", "side", "west", "back", "top")
_SUFFIX = {"front": "_front", "side": "", "west": "_west", "back": "_back", "top": "_top"}
_FACE_TEX = {"north": "#front", "south": "#back", "east": "#side", "west": "#west", "up": "#top",
             "down": "#top"}


# ---- projection -----------------------------------------------------------------------------------
def _rect(box, view):
    """Pixel rectangle (inclusive) of ``box`` in ``view`` and its painting depth (far first)."""
    (x0, y0, z0), (x1, y1, z1) = box["from"], box["to"]
    if view == "front":
        u0, u1, v0, v1, depth = 16 - x1, 16 - x0, 16 - y1, 16 - y0, -z0
    elif view == "back":
        u0, u1, v0, v1, depth = x0, x1, 16 - y1, 16 - y0, z1
    elif view == "side":
        u0, u1, v0, v1, depth = 16 - z1, 16 - z0, 16 - y1, 16 - y0, x1
    elif view == "west":
        u0, u1, v0, v1, depth = z0, z1, 16 - y1, 16 - y0, -x0
    else:
        u0, u1, v0, v1, depth = x0, x1, z0, z1, y1
    px0, px1 = int(round(u0 * 2)), int(round(u1 * 2)) - 1
    py0, py1 = int(round(v0 * 2)), int(round(v1 * 2)) - 1
    return (max(0, px0), max(0, py0), min(K.S - 1, px1), min(K.S - 1, py1)), depth


def _panel(px, rect, ramp, rng):
    """One box face: noise from the ramp, a light top/left edge and a dark bottom/right edge."""
    x0, y0, x1, y1 = rect
    if x1 < x0 or y1 < y0:
        return
    hi = K.mix(ramp[0], K.A_LIGHT, 0.45)
    lo = K.mix(ramp[0], K.A_DARK, 0.55)
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            px[x, y] = rng.choice(ramp)
    if y1 - y0 >= 1:
        for x in range(x0, x1 + 1):
            px[x, y0] = hi
            px[x, y1] = lo
    if x1 - x0 >= 2:
        for y in range(y0, y1 + 1):
            px[x0, y] = hi if y < y1 else lo
            px[x1, y] = lo


def _project(name, view):
    spec = MACHINES[name]
    img = K.new_img()
    px = img.load()
    rng = K.rng_for(name + _SUFFIX[view])
    # Anything no box covers is never sampled by the model; fill it dark so the file is opaque.
    for y in range(K.S):
        for x in range(K.S):
            px[x, y] = K.A_DARK
    boxes = [b for b in spec["boxes"] if b["mat"] != "glass"]
    for box in sorted(boxes, key=lambda b: _rect(b, view)[1]):
        _panel(px, _rect(box, view)[0], _M[box["mat"]], rng)
    return img, px


# ---- small paint helpers (pixel space of one view) ------------------------------------------------
def _fill(px, x0, y0, x1, y1, col):
    for y in range(max(0, y0), min(K.S - 1, y1) + 1):
        for x in range(max(0, x0), min(K.S - 1, x1) + 1):
            px[x, y] = col


def _hline(px, x0, x1, y, col):
    _fill(px, x0, y, x1, y, col)


def _enamel_band(px, x0, x1, y):
    """The chemistry family trim: a 2 px enamel band."""
    for x in range(x0, x1 + 1):
        px[x, y] = K.ENAMEL[(x // 2) % 2]
        px[x, y + 1] = K.mix(K.ENAMEL[1], K.A_DARK, 0.35)


# ---- per-machine decals ---------------------------------------------------------------------------
def _decor_centrifuge(view, px):
    if view == "top":
        for r, col in ((7, K.ALLOY[2]), (3, K.T_DEEP)):
            for a in range(0, 360, 6):
                x = int(round(15.5 + r * math.cos(math.radians(a))))
                y = int(round(15.5 + r * math.sin(math.radians(a))))
                px[x, y] = col
        _fill(px, 15, 15, 16, 16, K.T_TEAL)
        return
    # drum: enamel band under the cap, two dark balance rings; base trim
    lo, hi = (10, 21) if view in ("side", "west") else (6, 25)
    _enamel_band(px, lo, hi, 7)
    _hline(px, lo, hi, 13, K.A_DARK)
    _hline(px, lo, hi, 20, K.A_DARK)
    _enamel_band(px, 1, 30, 25)
    if view == "front":
        K.status_socket(px)
        for y in range(10, 22, 2):                  # post: rate ticks down from the lamp
            px[27, y] = K.T_TEAL
        K.led(px, 5, 28)
        K.led(px, 11, 28, K.T_PLASMA)
        _fill(px, 18, 28, 23, 29, K.T_DEEP)         # base vent
        _hline(px, 18, 23, 28, K.T_TEAL)
    else:
        for x in range(6, 26, 4):                   # base vents
            _fill(px, x, 28, x + 1, 29, K.T_DEEP)


def _decor_cryo(view, px):
    if view == "top":
        _fill(px, 13, 13, 18, 18, K.T_DEEP)
        _fill(px, 15, 15, 16, 16, K.FROST[1])
        for x, y in ((4, 4), (27, 4), (4, 27), (27, 27)):
            px[x, y] = K.A_LIGHT
        return
    # rime on the top collar and cap (static; the creeping frost is the renderer's)
    for x in range(8, 24):
        if (x * 7) % 5 < 2:
            px[x, 4] = K.FROST[1]
        if (x * 3) % 4 == 0:
            px[x, 0] = K.FROST[0]
    _enamel_band(px, 3, 28, 26)
    if view == "front":
        K.status_socket(px)
        for y in range(12, 24, 3):                  # column level gauge
            _fill(px, 15, y, 16, y, K.T_TEAL)
        _fill(px, 15, 9, 16, 10, K.T_CYAN)
        K.led(px, 6, 28)
        K.led(px, 12, 28, K.T_PLASMA)
        for y in range(10, 26, 2):                  # mast ticks
            px[27, y] = K.T_TEAL
    else:
        for x in range(6, 26, 5):
            _fill(px, x, 28, x + 1, 29, K.T_DEEP)


def _decor_condenser(view, px):
    if view == "top":
        # looking down the bell: dark throat with the neck opening
        _fill(px, 6, 6, 25, 25, K.mix(K.ALLOY[2], K.A_DARK, 0.6))
        _fill(px, 12, 12, 19, 19, K.T_DEEP)
        _fill(px, 14, 14, 17, 17, K.A_DARK)
        return
    # bell: louvre slots; bulb: enamel band
    for y in (3, 6, 9):
        for x in range(6, 26):
            px[x, y] = K.A_DARK
            px[x, y + 1] = K.mix(K.ALLOY[3], K.A_LIGHT, 0.5)
    _enamel_band(px, 6, 25, 19)
    if view == "front":
        K.status_socket(px)
        # sight window (model x 6..10, y 3..6): dark well, teal rim. The renderer tints it.
        _fill(px, 11, 19, 20, 26, K.A_DARK)
        _fill(px, 12, 20, 19, 25, K.T_DEEP)
        _hline(px, 12, 19, 25, K.T_TEAL)
        K.led(px, 7, 23)
        K.led(px, 23, 23, K.T_PLASMA)


def _decor_vat(view, px):
    if view == "top":
        _fill(px, 13, 13, 18, 18, K.T_DEEP)        # motor cap
        _fill(px, 15, 15, 16, 16, K.T_CYAN)
        for x in range(11, 21, 3):
            px[x, 11] = K.A_LIGHT
        return
    _enamel_band(px, 4, 27, 6)                      # shoulder
    if view == "front":
        K.status_socket(px)
        # vessel interior behind the glass (model x 2..14, y 4..11): dark, with level marks
        _fill(px, 4, 10, 27, 23, K.mix(K.ALLOY[2], K.A_DARK, 0.7))
        for y in range(12, 23, 3):
            _fill(px, 5, y, 6, y, K.A_LIGHT)
        K.led(px, 14, 3)                            # motor LEDs
        K.led(px, 18, 3, K.T_PLASMA)
        _hline(px, 4, 27, 25, K.T_TEAL)             # under-floor line
    else:
        _fill(px, 12, 2, 19, 3, K.T_DEEP)           # motor vent
        for y in range(11, 22, 4):
            _hline(px, 4, 27, y, K.ALLOY[2])


_DECOR = {"centrifuge": _decor_centrifuge, "cryo_distiller": _decor_cryo,
          "atmospheric_condenser": _decor_condenser, "reaction_vat": _decor_vat}


def _paint_machine(name):
    for view in VIEWS:
        img, px = _project(name, view)
        _DECOR[name](view, px)
        K.save_block(img, name + _SUFFIX[view])


# ---- Process Tank ---------------------------------------------------------------------------------
def _paint_tank():
    # side: corner posts (4 px) and top/bottom rings (4 px) around a glass pane
    img = K.new_img()
    px = img.load()
    rng = K.rng_for("process_tank")
    K.glass_pane(px, 4, 4, 27, 27, glints=2)
    for rect in ((0, 0, 31, 3), (0, 28, 31, 31), (0, 4, 3, 27), (28, 4, 31, 27)):
        _panel(px, rect, _M["alloy"], rng)
    _enamel_band(px, 4, 27, 1)
    for y in range(6, 27, 5):                       # fill gauge ticks on both posts
        px[2, y] = K.T_TEAL
        px[29, y] = K.T_TEAL
    for x in (5, 26):                               # rivets on the low ring
        px[x, 29] = K.A_LIGHT
    K.save_block(img, "process_tank")

    # top / bottom plate: ring with a capped port
    img = K.new_img()
    px = img.load()
    rng = K.rng_for("process_tank_top")
    _panel(px, (0, 0, 31, 31), _M["alloy"], rng)
    _panel(px, (4, 4, 27, 27), _M["dark"], rng)
    _panel(px, (11, 11, 20, 20), _M["light"], rng)
    _fill(px, 14, 14, 17, 17, K.T_DEEP)
    for x, y in ((6, 6), (25, 6), (6, 25), (25, 25)):
        px[x, y] = K.A_LIGHT
    K.save_block(img, "process_tank_top")


# ---- renderer sprites -----------------------------------------------------------------------------
WHITE = (255, 255, 255, 255)


def _paint_sprites():
    # centrifuge_band: one side of the spinning drum band. Slanted marks so the turn reads.
    img = K.new_img()
    px = img.load()
    rng = K.rng_for("centrifuge_band")
    for y in range(K.S):
        for x in range(K.S):
            px[x, y] = rng.choice(K.ENAMEL)
            if (x + y // 2) % 16 < 3:
                px[x, y] = K.ALLOY[2]
    for x in range(K.S):
        px[x, 0] = K.A_LIGHT
        px[x, K.S - 1] = K.A_DARK
    _fill(px, 12, 13, 19, 18, K.T_DEEP)
    _fill(px, 13, 14, 18, 17, K.T_CYAN)
    K.save_block(img, "centrifuge_band")

    # cryo_distiller_frost: frost sleeve, dense at the top, thinning to holes at the lower edge.
    img = K.new_img()
    px = img.load()
    rng = K.rng_for("cryo_distiller_frost")
    for y in range(K.S):
        cover = 0.95 - 0.75 * (y / (K.S - 1)) ** 1.6
        for x in range(K.S):
            if rng.random() < cover:
                px[x, y] = WHITE if rng.random() < 0.18 else rng.choice(K.FROST)
    K.save_block(img, "cryo_distiller_frost")

    # cryo_distiller_vapour: sparse cold wisps, tileable both ways (the renderer scrolls it down).
    img = K.new_img()
    px = img.load()
    for y in range(K.S):
        for x in range(K.S):
            wave = math.sin(2 * math.pi * (y / 16.0 + 0.35 * math.sin(2 * math.pi * x / K.S)))
            if wave > 0.6 and (x + 2 * y) % 3 == 0:
                px[x, y] = K.FROST[0]
            elif wave > 0.93 and (x + y) % 2 == 0:
                px[x, y] = WHITE
    K.save_block(img, "cryo_distiller_vapour")

    # atmospheric_condenser_louvre: one rotor slat; _shutter: the closed lid, slatted.
    img = K.new_img()
    px = img.load()
    rng = K.rng_for("atmospheric_condenser_louvre")
    _panel(px, (0, 0, 31, 31), _M["pipe"], rng)
    for x in range(3, 30, 6):
        _fill(px, x, 4, x + 1, 27, K.ALLOY[2])
    _fill(px, 14, 13, 17, 18, K.T_TEAL)
    K.save_block(img, "atmospheric_condenser_louvre")
    img = K.new_img()
    px = img.load()
    rng = K.rng_for("atmospheric_condenser_shutter")
    _panel(px, (0, 0, 31, 31), _M["dark"], rng)
    for y in range(3, 30, 5):
        _hline(px, 2, 29, y, K.A_DARK)
        _hline(px, 2, 29, y + 1, K.ALLOY[3])
    K.save_block(img, "atmospheric_condenser_shutter")

    # reaction_vat_paddle: agitator blade and shaft.
    img = K.new_img()
    px = img.load()
    rng = K.rng_for("reaction_vat_paddle")
    _panel(px, (0, 0, 31, 31), _M["pipe"], rng)
    for x in range(4, 28, 8):
        _fill(px, x, 8, x + 3, 23, K.ALLOY[2])
    K.save_block(img, "reaction_vat_paddle")

    # reaction_vat_bubbles: white rings on clear (surface quad; tinted with the contents colour).
    img = K.new_img()
    px = img.load()
    rng = K.rng_for("reaction_vat_bubbles")
    for _ in range(9):
        cx, cy, r = rng.randrange(4, 28), rng.randrange(4, 28), rng.choice((1.5, 2.2, 3.0))
        for a in range(0, 360, 15):
            x = int(round(cx + r * math.cos(math.radians(a))))
            y = int(round(cy + r * math.sin(math.radians(a))))
            if 0 <= x < K.S and 0 <= y < K.S:
                px[x, y] = WHITE
    K.save_block(img, "reaction_vat_bubbles")

    # reaction_vat_glass: the vessel front.
    img = K.new_img()
    px = img.load()
    K.glass_pane(px, 0, 0, 31, 31, glints=3)
    K.save_block(img, "reaction_vat_glass")


# ---- models ---------------------------------------------------------------------------------------
def _n(v):
    return int(v) if float(v).is_integer() else v


def _cull(face, box):
    (x0, y0, z0), (x1, y1, z1) = box["from"], box["to"]
    return {"north": z0 <= 0, "south": z1 >= 16, "west": x0 <= 0, "east": x1 >= 16,
            "down": y0 <= 0, "up": y1 >= 16}[face]


def machine_model(name):
    spec = MACHINES[name]
    tex = {"front": "%s:block/%s_front" % (MOD, name), "side": "%s:block/%s" % (MOD, name),
           "west": "%s:block/%s_west" % (MOD, name), "back": "%s:block/%s_back" % (MOD, name),
           "top": "%s:block/%s_top" % (MOD, name), "particle": "%s:block/%s" % (MOD, name)}
    elements = []
    for box in spec["boxes"]:
        faces = {}
        for face in box.get("faces", ("north", "south", "west", "east", "up", "down")):
            entry = {"texture": "#" + box["tex"] if "tex" in box else _FACE_TEX[face]}
            if "tex" in box:
                entry["uv"] = [0, 0, 16, 16]
            if _cull(face, box):
                entry["cullface"] = face
            faces[face] = entry
        elements.append({"name": box["name"], "from": [_n(v) for v in box["from"]],
                         "to": [_n(v) for v in box["to"]], "faces": faces})
        if "tex" in box:
            tex[box["tex"]] = "%s:block/%s_%s" % (MOD, name, box["tex"])
    elements.append(K.status_socket_element("#front", spec["socket_z"]))
    model = {"parent": "minecraft:block/block"}
    if spec.get("translucent"):
        model["render_type"] = "minecraft:translucent"
    model["textures"] = tex
    model["elements"] = elements
    return model


TANK_COLUMNS = ("single", "bottom", "middle", "top")


def _side(uv, cull=None):
    face = {"texture": "#side", "uv": uv}
    if cull:
        face["cullface"] = cull
    return face


def tank_model(column):
    """One of the four Process Tank models. Rings are dropped where tanks meet in a column."""
    ring_low = column in ("single", "bottom")
    ring_high = column in ("single", "top")
    y0, y1 = (2 if ring_low else 0), (14 if ring_high else 16)
    sides = ("north", "south", "west", "east")
    elements = []
    if ring_low:
        faces = {f: _side([0, 14, 16, 16], f) for f in sides}
        faces["down"] = {"texture": "#top", "cullface": "down"}
        faces["up"] = {"texture": "#top"}
        elements.append({"name": "ring_low", "from": [0, 0, 0], "to": [16, 2, 16], "faces": faces})
    if ring_high:
        faces = {f: _side([0, 0, 16, 2], f) for f in sides}
        faces["up"] = {"texture": "#top", "cullface": "up"}
        faces["down"] = {"texture": "#top"}
        elements.append({"name": "ring_high", "from": [0, 14, 0], "to": [16, 16, 16], "faces": faces})
    for name, x, z in (("post_nw", 0, 0), ("post_ne", 14, 0), ("post_sw", 0, 14), ("post_se", 14, 14)):
        elements.append({"name": name, "from": [x, y0, z], "to": [x + 2, y1, z + 2],
                         "faces": {f: _side([0, 3, 2, 13]) for f in sides}})
    # Glass on all four sides, set one pixel in so the fluid column never fights it.
    elements.append({"name": "glass", "from": [1, y0, 1], "to": [15, y1, 15],
                     "faces": {f: _side([2, 2, 14, 14]) for f in sides}})
    return {"parent": "minecraft:block/block", "render_type": "minecraft:translucent",
            "textures": {"side": MOD + ":block/process_tank", "top": MOD + ":block/process_tank_top",
                         "particle": MOD + ":block/process_tank_top"},
            "elements": elements}


def tank_model_name(column):
    return "process_tank" if column == "single" else "process_tank_" + column


def write_models():
    for name in MACHINES:
        K.save_json(machine_model(name), "models/block/%s.json" % name)
    for column in TANK_COLUMNS:
        K.save_json(tank_model(column), "models/block/%s.json" % tank_model_name(column))


def paint():
    for name in MACHINES:
        _paint_machine(name)
    _paint_tank()
    _paint_sprites()
    write_models()


PULSE = [K.pulse(name + "_front") for name in MACHINES]
