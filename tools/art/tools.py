"""A8 Tools + wearables art package: the six hand and head tools of 0.8.0.

Owned here (and nowhere else):

* ``textures/item/<id>.png``            -- one 32x atlas per hand tool (power_drill, cutting_lance,
                                           arc_wrench, multimeter), sampled by its element model
* ``textures/item/<id>.png``            -- the flat 32x icon of each wearable (thermal_goggles,
                                           filter_mask, filter_mask_empty)
* ``textures/item/<id>_worn.png``       -- the atlas of each wearable's head model
* ``models/item/*.json``                -- explicit element models with display transforms
* ``items/*.json``                      -- the client item definitions that pick a model from item data

Run::

    python tools/gen_textures.py --multiloader --force=tools    # repaint this package's textures only
    python tools/art/tools.py --models                          # rewrite its models and item definitions

What each item shows, and from which data:

* Power Drill    -- pistol body, battery under the grip, chuck forward. Two mode LEDs on top (left =
                    area, right = vein; both dark = single). The mode lives inside ``tool_charge``
                    next to the energy, which a client item definition cannot read, so the three
                    models are written but only the plain one is wired (see ``item_definitions``).
* Cutting Lance  -- long two-handed tube; the lens cradle at the tip and a band behind it take the
                    fitted lens colour (``fitted_lens``), empty cradle when unfitted. Each lens also
                    has its own cut on the glass, the same four cuts as the lens items.
* Arc Wrench     -- open jaw, copper electrodes, a teal arc across the gap. Carrying a machine
                    (``carried_machine``) lights the amber load lamp and closes the clamp bar.
* Multimeter     -- handset with a tilted screen, a dial and one probe on a lead.
* Thermal Goggles / Filter Mask -- a flat icon in the inventory, and a head model that vanilla draws
                    on the wearer through the ``head`` display context (the items are equippable
                    with no equipment asset, so there is no armour layer to paint). The mask has a
                    second model and icon with the canister socket empty (no ``fitted_filter``).

Model space: every tool is authored pointing north (-Z) with +Y up, like a block facing north, so the
first-person transform is nearly the identity and the third-person one is a quarter turn about X.
Head models are authored in the head's own frame: the head is the cube 1.6..14.4 on every axis
(one head pixel = 1.6 units), its face is the north side, and the hat layer ends 0.8 further out.

Item material ramps are free of the block rule about heat colours; amber here is the charge-bar
amber, used only for the wrench's load lamp.
"""
import json
import math
import os
import sys

if __name__ == "__main__":
    sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from art import common as K

G = K.G
S = K.S
FORCE_FLAG = "--force=tools"

# ---- ramps ---------------------------------------------------------------------------------------
ALLOY = (K.ALLOY[0], K.ALLOY[1], K.ALLOY[3])
ALLOY_LO = (K.ALLOY[2], K.ALLOY[0])
DARK = ((22, 26, 32, 255), (28, 34, 40, 255), (18, 22, 28, 255))            # rubber / grip
STEEL = (K.IRON[1], K.IRON[0], (136, 140, 148, 255))
STEEL_L = K.IRON[2]
STEEL_D = (84, 90, 100, 255)
COPPER = (K.COPPER[0], K.COPPER[1])
COPPER_L = K.COPPER[2]
AMBER = (224, 179, 58, 255)
AMBER_L = (250, 226, 150, 255)
AMBER_D = (128, 96, 30, 255)
PLEAT = ((226, 232, 230, 255), (188, 196, 192, 255))
SCREEN = (6, 26, 32, 255)
GLASS = ((10, 62, 74, 255), (16, 118, 130, 255), (28, 168, 184, 255), (36, 208, 222, 255))
SKIN = (176, 124, 92, 255)                       # contact-sheet mannequin only; never painted in a texture

LENS = {
    "quartz": K.LENS["quartz_focus_lens"],
    "glacite": K.LENS["glacite_focus_lens"],
    "ember": K.LENS["ember_focus_lens"],
    "void": K.LENS["void_focus_lens"],
}
LENS_ORDER = ("quartz", "glacite", "ember", "void")
DRILL_MODES = ("single", "area", "vein")


def rgba(c):
    return (c[0], c[1], c[2], 255)


def lift(c, t=0.35):
    return K.mix(c, (255, 255, 255, 255), t)


def shade(c, t=0.4):
    return K.mix(c, K.T_DEEP, t)


# ---- atlas sheet ---------------------------------------------------------------------------------
class Sheet(object):
    """One 32x texture cut into named tiles. A model face names a tile and takes its top-left
    ``2 px per model unit`` crop (or the whole tile with a leading ``=``)."""

    def __init__(self, name):
        self.name = name
        self.img = K.new_img()
        self.px = self.img.load()
        self.rng = K.rng_for(name)
        self.tiles = {}

    def tile(self, key, x, y, w, h):
        self.tiles[key] = (x, y, w, h)
        return x, y, x + w - 1, y + h - 1

    def uv(self, key, fw, fh, flip=False):
        fit = key.startswith("=")
        x, y, w, h = self.tiles[key.lstrip("=")]
        pw = w if fit else min(w, max(0.5, 2.0 * fw))
        ph = h if fit else min(h, max(0.5, 2.0 * fh))
        u1, v1, u2, v2 = x / 2.0, y / 2.0, (x + pw) / 2.0, (y + ph) / 2.0
        if flip:
            u1, u2 = u2, u1
        return [round(v, 3) for v in (u1, v1, u2, v2)]

    # -- painting (all coordinates inclusive, in atlas pixels) --
    def put(self, x, y, col):
        if 0 <= x < S and 0 <= y < S:
            self.px[x, y] = col

    def rect(self, x0, y0, x1, y1, col):
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                self.put(x, y, col)

    def hline(self, x0, x1, y, col):
        self.rect(x0, y, x1, y, col)

    def vline(self, x, y0, y1, col):
        self.rect(x, y0, x, y1, col)

    def noise(self, x0, y0, x1, y1, cols):
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                self.put(x, y, self.rng.choice(cols))

    def panel(self, key, x, y, w, h, cols, light=None, dark=None):
        """A tile of grained metal with a lit top/left edge and a shadowed bottom/right edge."""
        x0, y0, x1, y1 = self.tile(key, x, y, w, h)
        self.noise(x0, y0, x1, y1, cols)
        light = light or lift(cols[-1], 0.3)
        dark = dark or shade(cols[0], 0.55)
        if w > 2 and h > 2:
            self.hline(x0, x1, y1, dark)
            self.vline(x1, y0, y1, dark)
            self.hline(x0, x1 - 1, y0, light)
            self.vline(x0, y0, y1 - 1, light)
        return x0, y0, x1, y1

    def flat(self, key, x, y, w, h, col):
        x0, y0, x1, y1 = self.tile(key, x, y, w, h)
        self.rect(x0, y0, x1, y1, col)
        return x0, y0, x1, y1

    def save(self):
        save_item(self.img, self.name)


def save_item(img, name):
    """``save()`` with this package's own force switch (see the module docstring)."""
    old = G.FORCE
    G.FORCE = old or FORCE_FLAG in sys.argv
    try:
        K.save(img, "item", name)
    finally:
        G.FORCE = old


# ---- model kit -----------------------------------------------------------------------------------
FACES = ("north", "south", "east", "west", "up", "down")


def box(sheet, name, frm, to, tiles, rot=None, shade_=True, skip=()):
    """One element. ``tiles`` is a tile key for every face or ``{face: key, "*": key}``. East faces
    are mirrored so a side tile painted nose-left reads the same on both sides of the tool."""
    dx, dy, dz = (to[i] - frm[i] for i in range(3))
    dims = {"north": (dx, dy), "south": (dx, dy), "east": (dz, dy), "west": (dz, dy),
            "up": (dx, dz), "down": (dx, dz)}
    faces = {}
    for f in FACES:
        if f in skip:
            continue
        key = tiles if isinstance(tiles, str) else tiles.get(f, tiles.get("*"))
        fw, fh = dims[f]
        if key is None or fw <= 0 or fh <= 0:
            continue
        turned = key.endswith("^")                 # tile runs along the face's other axis
        if turned:
            key, fw, fh = key[:-1], fh, fw
        faces[f] = {"texture": "#tex", "uv": sheet.uv(key, fw, fh, flip=(f == "east"))}
        if turned:
            faces[f]["rotation"] = 90
    el = {"name": name, "from": [round(v, 3) for v in frm], "to": [round(v, 3) for v in to]}
    if rot:
        el["rotation"] = {"origin": list(rot[0]), "axis": rot[1], "angle": rot[2]}
    if not shade_:
        el["shade"] = False
    el["faces"] = faces
    return el


def rot_matrix(rx, ry, rz):
    """Display rotation as the game builds it: Z first, then Y, then X (degrees)."""
    a, b, c = (math.radians(v) for v in (rx, ry, rz))
    mx = ((1, 0, 0), (0, math.cos(a), -math.sin(a)), (0, math.sin(a), math.cos(a)))
    my = ((math.cos(b), 0, math.sin(b)), (0, 1, 0), (-math.sin(b), 0, math.cos(b)))
    mz = ((math.cos(c), -math.sin(c), 0), (math.sin(c), math.cos(c), 0), (0, 0, 1))

    def mul(p, q):
        return tuple(tuple(sum(p[i][k] * q[k][j] for k in range(3)) for j in range(3)) for i in range(3))
    return mul(mx, mul(my, mz))


def held(rotation, scale, grip, at=(0.0, 0.0, 0.0)):
    """A display transform that puts the model point ``grip`` at ``at`` (pixels from the context's
    own anchor: the fist in third person, the view-model anchor in first person)."""
    m = rot_matrix(*rotation)
    d = [grip[i] - 8.0 for i in range(3)]
    moved = [scale * sum(m[i][k] * d[k] for k in range(3)) for i in range(3)]
    return {"rotation": list(rotation),
            "translation": [round(at[i] - moved[i], 2) for i in range(3)],
            "scale": [scale, scale, scale]}


def plain(rotation, translation, scale):
    return {"rotation": list(rotation), "translation": list(translation), "scale": [scale, scale, scale]}


def model(sheet_name, elements, display):
    return {"gui_light": "side",
            "textures": {"tex": "nerotech:item/" + sheet_name, "particle": "nerotech:item/" + sheet_name},
            "elements": elements, "display": display}


def flat_model(texture):
    return {"parent": "minecraft:item/generated", "textures": {"layer0": "nerotech:item/" + texture}}


# ==================================================================================================
# Power Drill
# ==================================================================================================
def drill_sheet():
    s = Sheet("power_drill")
    x0, y0, x1, y1 = s.panel("side", 0, 0, 16, 8, ALLOY)                 # body side, nose left
    s.hline(x0 + 1, x1 - 1, y0 + 1, K.ALLOY[3])
    s.hline(x0 + 2, x1 - 5, y0 + 5, K.T_TEAL)                            # accent stripe
    s.put(x0 + 2, y0 + 5, K.T_PLASMA)
    s.put(x0 + 3, y0 + 5, K.T_CYAN)
    for x in (10, 12, 14):                                               # motor vents
        s.vline(x, y0 + 2, y0 + 4, K.A_DARK)
        s.put(x - 1, y0 + 2, K.A_LIGHT)
    s.put(x0 + 1, y0 + 3, K.A_LIGHT)                                     # rivet
    s.put(x0 + 5, y0 + 3, K.A_LIGHT)

    x0, y0, x1, y1 = s.panel("cap", 16, 0, 8, 10, DARK, light=K.ALLOY[3], dark=(10, 12, 16, 255))
    for y in (y0 + 2, y0 + 4, y0 + 6):
        s.hline(x0 + 1, x1 - 1, y, (10, 12, 16, 255))
    s.rect(x0 + 2, y0 + 3, x0 + 5, y0 + 6, K.T_DEEP)                     # fan hub
    s.rect(x0 + 3, y0 + 4, x0 + 4, y0 + 5, K.T_TEAL)
    s.put(x0 + 3, y0 + 4, K.T_CYAN)

    x0, y0, x1, y1 = s.panel("collar", 24, 0, 8, 6, STEEL, light=STEEL_L, dark=STEEL_D)
    for x in (x0 + 2, x0 + 4, x0 + 6):
        s.vline(x, y0 + 1, y1 - 1, STEEL_D)
    s.panel("chuck", 24, 6, 4, 4, (STEEL_D, K.IRON[1]), light=K.IRON[0], dark=(60, 64, 74, 255))
    s.flat("led_off", 28, 6, 2, 2, K.T_DEEP)
    s.put(28, 6, shade(K.T_TEAL, 0.5))
    s.flat("led_area", 30, 6, 2, 2, K.T_CYAN)
    s.put(30, 6, K.T_PLASMA)
    s.flat("led_vein", 30, 8, 2, 2, K.T_PLASMA)
    s.put(30, 8, K.T_GLOW)
    x0, y0, x1, y1 = s.flat("trig", 28, 8, 2, 4, K.T_TEAL)
    s.vline(x0, y0, y1, K.T_CYAN)
    s.put(x1, y1, K.T_DEEP)

    x0, y0, x1, y1 = s.tile("bit", 16, 10, 8, 2)                         # fluted shank
    for x in range(x0, x1 + 1):
        s.put(x, y0, STEEL_L if (x - x0) % 3 else K.IRON[0])
        s.put(x, y1, K.IRON[1] if (x - x0) % 3 != 1 else STEEL_D)
    s.flat("bit_tip", 24, 10, 2, 2, STEEL_L)
    s.put(25, 11, K.IRON[1])

    x0, y0, x1, y1 = s.panel("top", 0, 8, 6, 16, (K.ALLOY[1], K.ALLOY[3]))  # body top, nose up
    s.vline(x0 + 2, y0 + 1, y0 + 9, K.ALLOY[0])
    s.vline(x0 + 3, y0 + 1, y0 + 9, K.A_LIGHT)
    s.hline(x0 + 1, x1 - 1, y0 + 11, K.A_DARK)                           # LED bezel strip
    s.hline(x0 + 1, x1 - 1, y0 + 14, K.A_DARK)

    x0, y0, x1, y1 = s.panel("grip", 6, 8, 5, 12, DARK, light=(48, 56, 66, 255), dark=(10, 12, 16, 255))
    for y in range(y0 + 2, y1, 2):
        s.hline(x0 + 1, x1 - 1, y, (44, 50, 58, 255))

    x0, y0, x1, y1 = s.panel("batt", 11, 12, 10, 5, (K.T_DEEP, shade(K.T_TEAL, 0.6)),
                             light=K.T_TEAL, dark=(4, 22, 28, 255))
    for i, x in enumerate((x0 + 2, x0 + 4, x0 + 6)):                     # charge pips
        s.put(x, y0 + 2, K.T_CYAN if i < 2 else K.T_PLASMA)
    s.put(x1 - 1, y0 + 2, K.A_LIGHT)                                     # latch
    x0, y0, x1, y1 = s.panel("batt_end", 21, 12, 7, 5, ALLOY_LO)
    s.rect(x0 + 2, y0 + 2, x0 + 4, y0 + 2, K.T_TEAL)
    s.panel("dark", 11, 17, 12, 8, ALLOY_LO, light=K.ALLOY[1], dark=K.A_DARK)
    x0, y0, x1, y1 = s.panel("capside", 23, 17, 3, 10, DARK, light=K.ALLOY[3], dark=(10, 12, 16, 255))
    s.put(x0 + 1, y0 + 4, K.T_TEAL)
    s.put(x0 + 1, y0 + 5, K.T_TEAL)
    s.save()
    return s


def drill_model(s, mode):
    rake = ((8, 8, 9.5), "x", -22.5)
    e = [
        box(s, "body", (6.5, 8, 4), (9.5, 12, 12),
            {"east": "side", "west": "side", "up": "top", "*": "dark"}),
        box(s, "motor_cap", (6, 7.5, 12), (10, 12.5, 13.5), {"south": "cap", "north": "dark", "*": "capside"}),
        box(s, "collar", (6.75, 8.5, 2.5), (9.25, 11.5, 4), "collar"),
        box(s, "chuck", (7.25, 9.25, 1), (8.75, 10.75, 2.5), "chuck"),
        box(s, "bit", (7.6, 9.6, -3), (8.4, 10.4, 1),
            {"north": "bit_tip", "east": "bit", "west": "bit", "up": "bit^", "down": "bit^"}, skip=("south",)),
        box(s, "grip", (7, 2.5, 8.25), (9, 8, 10.75), "grip", rot=rake, skip=("up",)),
        box(s, "battery", (6.25, 0.25, 7.25), (9.75, 2.5, 12.25),
            {"east": "batt", "west": "batt", "up": "dark", "*": "batt_end"}, rot=rake),
        box(s, "trigger", (7.5, 6.4, 7.2), (8.5, 8, 8.1), "trig", skip=("up",)),
        box(s, "guard_front", (7.6, 5.2, 5.6), (8.4, 8, 6.1), "grip", skip=("up",)),
        box(s, "guard_bottom", (7.6, 5.2, 6.1), (8.4, 5.7, 9.2), "grip"),
        box(s, "led_area", (6.8, 12, 10), (7.6, 12.25, 10.8), "led_area" if mode == "area" else "led_off",
            shade_=mode != "area", skip=("down",)),
        box(s, "led_vein", (8.4, 12, 10), (9.2, 12.25, 10.8), "led_vein" if mode == "vein" else "led_off",
            shade_=mode != "vein", skip=("down",)),
    ]
    grip = (8, 5.4, 10.3)
    display = {
        "gui": plain((90, -52, 90), (0.25, 0.75, 0), 0.84),
        "ground": plain((0, 0, 0), (0, 2, 0), 0.5),
        "fixed": plain((0, 90, 0), (0, 0.5, 0), 0.9),
        "thirdperson_righthand": held((82, 0, 0), 0.85, grip, at=(0, 0.5, 0.5)),
        "firstperson_righthand": held((4, 8, 0), 0.72, grip, at=(-1.5, 3.5, -1.0)),
    }
    return model("power_drill", e, display)


# ==================================================================================================
# Cutting Lance
# ==================================================================================================
def _lens_cut(s, x0, y0, kind):
    """The 4x4 glass of a fitted lens: colour plus the lens item's own cut."""
    c = rgba(LENS[kind])
    hi, lo = lift(c, 0.45), shade(c, 0.35)
    s.rect(x0, y0, x0 + 3, y0 + 3, c)
    if kind == "quartz":                                   # round, plain: one soft glint
        s.put(x0 + 1, y0 + 1, (255, 255, 255, 255))
        s.put(x0 + 3, y0 + 3, lo)
    elif kind == "glacite":                                # hexagonal facets: a lit and a dark diagonal
        for i in range(4):
            s.put(x0 + i, y0 + i, hi)
        s.put(x0 + 3, y0, lo)
        s.put(x0, y0 + 3, lo)
    elif kind == "ember":                                  # triangular starburst: a bright cross
        for i in range(4):
            s.put(x0 + 1, y0 + i, hi)
            s.put(x0 + i, y0 + 2, hi)
        s.put(x0 + 1, y0 + 2, (255, 236, 190, 255))
    else:                                                  # void: annular, dark centre, plasma rim glint
        s.rect(x0 + 1, y0 + 1, x0 + 2, y0 + 2, (20, 14, 44, 255))
        s.put(x0, y0, K.T_PLASMA)
        s.put(x0 + 3, y0 + 3, lift(c, 0.2))


def lance_sheet():
    s = Sheet("cutting_lance")
    x0, y0, x1, y1 = s.panel("tube", 0, 0, 32, 4, ALLOY)                 # nose left
    s.hline(x0 + 1, x1 - 1, y0 + 1, K.ALLOY[3])
    for x in (4, 5, 15, 16, 26, 27):                                     # clamp bands
        s.vline(x, y0, y1, STEEL[0] if x % 2 == 0 else STEEL_D)
    s.hline(7, 13, y0 + 2, K.T_TEAL)                                     # power conduit
    s.hline(18, 24, y0 + 2, K.T_TEAL)
    s.put(7, y0 + 2, K.T_CYAN)
    s.put(18, y0 + 2, K.T_CYAN)

    x0, y0, x1, y1 = s.panel("pack", 0, 4, 10, 6, DARK, light=K.ALLOY[3], dark=(10, 12, 16, 255))
    for x in (x0 + 5, x0 + 7):                                           # cooling slots
        s.vline(x, y0 + 1, y0 + 3, (8, 10, 14, 255))
    s.rect(x0 + 1, y0 + 2, x0 + 3, y0 + 3, K.T_DEEP)                     # cell window
    s.put(x0 + 1, y0 + 2, K.T_CYAN)
    s.put(x0 + 2, y0 + 2, K.T_TEAL)
    x0, y0, x1, y1 = s.panel("pack_end", 10, 4, 6, 6, ALLOY_LO)
    s.rect(x0 + 2, y0 + 2, x0 + 3, y0 + 3, K.T_TEAL)
    s.put(x0 + 2, y0 + 2, K.T_CYAN)

    x0, y0, x1, y1 = s.panel("emit", 16, 4, 4, 6, STEEL, light=STEEL_L, dark=STEEL_D)
    s.vline(x0 + 2, y0 + 1, y1 - 1, STEEL_D)
    x0, y0, x1, y1 = s.panel("emit_f", 20, 4, 6, 6, STEEL, light=STEEL_L, dark=STEEL_D)
    s.rect(x0 + 1, y0 + 1, x0 + 4, y0 + 4, (10, 12, 16, 255))            # aperture
    s.rect(x0 + 2, y0 + 2, x0 + 3, y0 + 3, K.T_DEEP)
    x0, y0, x1, y1 = s.panel("grip", 26, 4, 3, 8, DARK, light=(48, 56, 66, 255), dark=(10, 12, 16, 255))
    for y in range(y0 + 2, y1, 2):
        s.put(x0 + 1, y, (44, 50, 58, 255))
    s.panel("ring", 29, 4, 3, 6, (K.IRON[1], K.IRON[0]), light=STEEL_L, dark=STEEL_D)

    x0, y0, x1, y1 = s.tile("line", 0, 10, 26, 1)                        # coolant line
    for x in range(x0, x1 + 1):
        s.put(x, y0, COPPER_L if x % 5 == 0 else COPPER[0])
    x0, y0, x1, y1 = s.tile("fin", 0, 11, 6, 1)
    s.hline(x0, x1, y0, K.IRON[0])
    s.put(x0, y0, STEEL_L)

    for i, kind in enumerate(LENS_ORDER):                                # glass and band per lens
        s.tile("lens_" + kind, i * 4, 12, 4, 4)
        _lens_cut(s, i * 4, 12, kind)
        c = rgba(LENS[kind])
        bx = 16 + i * 2
        s.flat("band_" + kind, bx, 12, 2, 3, c)
        s.hline(bx, bx + 1, 12, lift(c, 0.4))
        s.hline(bx, bx + 1, 14, shade(c, 0.3))
    s.flat("band_none", 24, 12, 2, 3, (10, 12, 16, 255))
    s.put(24, 12, K.A_DARK)
    s.panel("dark", 0, 16, 12, 8, ALLOY_LO, light=K.ALLOY[1], dark=K.A_DARK)
    s.save()
    return s


def lance_model(s, lens):
    rake = ((8, 8, 16), "x", -22.5)
    band = "band_" + lens
    e = [
        box(s, "tube", (7, 8, -4), (9, 10, 18),
            {"east": "=tube", "west": "=tube", "up": "=tube^", "down": "=tube^"}),
        box(s, "power_pack", (6.5, 7.5, 18), (9.5, 10.5, 23),
            {"east": "pack", "west": "pack", "south": "pack_end", "*": "dark"}),
        box(s, "butt_pad", (6.75, 7.25, 23), (9.25, 10.75, 24), "grip", skip=("north",)),
        box(s, "rear_grip", (7.25, 4.25, 15), (8.75, 8, 17), "grip", rot=rake, skip=("up",)),
        box(s, "fore_grip", (7.25, 5.25, 5), (8.75, 8, 6.5), "grip", skip=("up",)),
        box(s, "emitter", (6.25, 7.25, -6), (9.75, 10.75, -3.5), {"north": "emit_f", "south": "dark", "*": "emit"}),
        box(s, "lens_band", (6.1, 8.25, -5.6), (9.9, 9.75, -4.1), band, shade_=lens == "none",
            skip=("north", "south")),
        box(s, "cradle_top", (6, 10.5, -8.5), (10, 11, -6), "ring", skip=("south",)),
        box(s, "cradle_bottom", (6, 7, -8.5), (10, 7.5, -6), "ring", skip=("south",)),
        box(s, "cradle_west", (6, 7.5, -8.5), (6.5, 10.5, -6), "ring", skip=("south", "up", "down")),
        box(s, "cradle_east", (9.5, 7.5, -8.5), (10, 10.5, -6), "ring", skip=("south", "up", "down")),
        box(s, "coolant_line", (9, 9.4, 0), (9.5, 9.9, 14), {"east": "=line", "up": "=line^", "down": "=line^"}),
    ]
    for i, z in enumerate((-3.0, -1.6, -0.2)):
        e.append(box(s, "fin_%d" % i, (7.25, 10, z), (8.75, 10.7, z + 0.6), "fin", skip=("down",)))
    if lens != "none":
        e.append(box(s, "lens", (6.5, 7.5, -7.9), (9.5, 10.5, -6.7),
                     {"north": "=lens_" + lens, "south": "=lens_" + lens, "*": band}, shade_=False))
    grip = (8, 6.0, 16.4)
    display = {
        "gui": plain((84, -45, 90), (0, 0, 0), 0.58),
        "ground": plain((0, 0, 0), (0, 2, 0), 0.5),
        "fixed": plain((0, 90, 45), (0, 0, 0), 0.58),
        "thirdperson_righthand": held((86, 0, 0), 0.8, grip, at=(0, 0.5, 0.5)),
        "firstperson_righthand": held((3, 10, 0), 0.7, grip, at=(0.5, 4.0, 4.5)),
    }
    return model("cutting_lance", e, display)


# ==================================================================================================
# Arc Wrench
# ==================================================================================================
def wrench_sheet():
    s = Sheet("arc_wrench")
    x0, y0, x1, y1 = s.panel("handle", 0, 0, 20, 4, DARK, light=(48, 56, 66, 255), dark=(10, 12, 16, 255))
    for x in range(x0 + 6, x1 - 1, 2):                                   # grip ribs
        s.vline(x, y0 + 1, y1 - 1, (44, 50, 58, 255))
    s.vline(x0 + 2, y0, y1, K.T_TEAL)                                    # insulation band at the neck
    s.vline(x0 + 3, y0, y1, K.T_DEEP)
    s.put(x0 + 2, y0, K.T_CYAN)
    x0, y0, x1, y1 = s.panel("cell", 20, 0, 5, 6, (K.T_DEEP, shade(K.T_TEAL, 0.6)),
                             light=K.T_TEAL, dark=(4, 22, 28, 255))
    s.put(x0 + 2, y0 + 2, K.T_CYAN)
    s.put(x0 + 2, y0 + 3, K.T_PLASMA)
    x0, y0, x1, y1 = s.panel("cell_end", 25, 0, 4, 6, ALLOY_LO)
    s.put(x0 + 1, y0 + 2, K.T_TEAL)
    s.put(x0 + 2, y0 + 3, K.T_TEAL)

    x0, y0, x1, y1 = s.panel("neck", 0, 4, 6, 6, ALLOY)
    s.put(x0 + 2, y0 + 2, K.A_LIGHT)                                     # pivot bolt
    s.put(x0 + 3, y0 + 3, K.A_DARK)
    x0, y0, x1, y1 = s.panel("jaw", 6, 4, 12, 4, STEEL, light=STEEL_L, dark=STEEL_D)   # nose left
    for x in range(x0 + 1, x0 + 6, 2):                                   # teeth along the inner edge
        s.put(x, y1, K.IRON[0])
    x0, y0, x1, y1 = s.panel("yoke", 18, 6, 4, 14, STEEL, light=STEEL_L, dark=STEEL_D)
    s.put(x0 + 1, y0 + 6, STEEL_D)
    s.put(x0 + 2, y0 + 7, STEEL_L)
    s.flat("tip", 22, 6, 2, 2, COPPER[0])
    s.put(22, 6, COPPER_L)
    s.put(23, 7, COPPER[1])
    s.flat("lamp_off", 24, 6, 2, 2, AMBER_D)
    s.put(25, 7, (70, 52, 18, 255))
    s.flat("lamp_on", 26, 6, 2, 2, AMBER)
    s.put(26, 6, AMBER_L)
    x0, y0, x1, y1 = s.panel("clamp", 22, 8, 2, 6, (K.IRON[1], K.IRON[0]), light=STEEL_L, dark=STEEL_D)
    s.panel("dark", 6, 8, 12, 8, ALLOY_LO, light=K.ALLOY[1], dark=K.A_DARK)

    x0, y0, x1, y1 = s.tile("arc", 0, 10, 5, 7)                          # the arc: nose left, cut out
    for (x, y, col) in ((2, 0, K.T_CYAN), (2, 1, K.T_PLASMA), (3, 2, K.T_GLOW), (2, 3, K.T_GLOW),
                        (1, 3, K.T_PLASMA), (1, 4, K.T_GLOW), (2, 5, K.T_PLASMA), (2, 6, K.T_CYAN),
                        (4, 2, K.T_CYAN), (0, 4, K.T_CYAN), (3, 1, K.T_TEAL), (1, 5, K.T_TEAL)):
        s.put(x0 + x, y0 + y, col)
    x0, y0, x1, y1 = s.tile("arc_front", 0, 17, 3, 7)                    # the same arc seen end on
    for (x, y, col) in ((1, 0, K.T_CYAN), (1, 1, K.T_PLASMA), (2, 2, K.T_GLOW), (1, 3, K.T_GLOW),
                        (0, 4, K.T_PLASMA), (1, 5, K.T_PLASMA), (1, 6, K.T_CYAN), (0, 3, K.T_TEAL)):
        s.put(x0 + x, y0 + y, col)
    s.save()
    return s


def wrench_model(s, state):
    """``state``: ``charged`` (arc across the gap), ``idle`` (no arc) or ``carrying`` (arc, the clamp
    bar closed across the jaw and the load lamp lit)."""
    carrying = state == "carrying"
    e = [
        box(s, "handle", (7.25, 7, 6), (8.75, 9, 16),
            {"east": "handle", "west": "handle", "up": "handle^", "down": "handle^"}),
        box(s, "cell", (7, 6.5, 16), (9, 9.5, 18.5), {"east": "cell", "west": "cell", "*": "cell_end"}),
        box(s, "neck", (7, 6.5, 3.5), (9, 9.5, 6), "neck"),
        box(s, "yoke", (7.25, 4.5, 3), (8.75, 11.5, 5), {"east": "yoke", "west": "yoke", "*": "jaw"}),
        box(s, "jaw_upper", (7.25, 9.5, -2), (8.75, 11.5, 3), {"*": "jaw", "north": "tip"}, skip=("south",)),
        box(s, "jaw_lower", (7.25, 4.5, -2), (8.75, 6.5, 3), {"*": "jaw", "north": "tip"}, skip=("south",)),
        box(s, "electrode_upper", (7.6, 8.5, -2), (8.4, 9.5, -1), "tip", skip=("up",)),
        box(s, "electrode_lower", (7.6, 6.5, -2), (8.4, 7.5, -1), "tip", skip=("down",)),
        box(s, "load_lamp", (7.5, 9, 6.6), (8.5, 9.3, 7.6), "lamp_on" if carrying else "lamp_off",
            shade_=not carrying, skip=("down",)),
    ]
    if state != "idle":
        e.append(box(s, "arc", (8, 6.3, -2.75), (8, 9.7, -0.25), {"east": "=arc", "west": "=arc"}, shade_=False))
        e.append(box(s, "arc_cross", (7.25, 6.3, -1.5), (8.75, 9.7, -1.5),
                     {"north": "=arc_front", "south": "=arc_front"}, shade_=False))
    if carrying:
        e.append(box(s, "clamp_bar", (7.5, 6.5, 1.2), (8.5, 9.5, 1.8), "clamp", skip=("up", "down")))
    grip = (8, 8, 12)
    display = {
        "gui": plain((84, -45, 90), (0, 0, 0), 0.74),
        "ground": plain((0, 0, 0), (0, 2, 0), 0.5),
        "fixed": plain((0, 90, 45), (0, 0, 0), 0.74),
        "thirdperson_righthand": held((84, 0, 0), 0.8, grip, at=(0, 0.5, 0.5)),
        "firstperson_righthand": held((6, 10, 0), 0.75, grip, at=(-0.5, 4.5, 0.5)),
    }
    return model("arc_wrench", e, display)


# ==================================================================================================
# Multimeter
# ==================================================================================================
def _digit(s, x, y, rows, col):
    for dy, row in enumerate(rows):
        for dx, ch in enumerate(row):
            if ch == "#":
                s.put(x + dx, y + dy, col)


def meter_sheet():
    s = Sheet("multimeter")
    bump = (10, 12, 16, 255)
    # lower body, front: dial and two jacks
    x0, y0, x1, y1 = s.panel("front", 0, 0, 12, 15, ALLOY)
    s.rect(x0, y0, x1, y0, DARK[0])
    s.vline(x0, y0, y1, DARK[1])                                          # rubber bumper edges
    s.vline(x1, y0, y1, bump)
    s.hline(x0, x1, y1, bump)
    s.rect(x0 + 3, y0 + 4, x0 + 8, y0 + 9, K.A_DARK)                      # dial well
    for (dx, dy) in ((2, 3), (9, 3), (2, 10), (9, 10), (5, 2), (6, 2)):   # range ticks
        s.put(x0 + dx, y0 + dy, K.A_LIGHT)
    s.put(x0 + 4, y0 + 12, K.T_CYAN)                                      # jacks: live and common
    s.put(x0 + 7, y0 + 12, bump)
    s.put(x0 + 4, y0 + 13, K.T_DEEP)
    s.put(x0 + 7, y0 + 13, K.A_DARK)
    s.hline(x0 + 2, x0 + 9, y0 + 1, K.T_TEAL)                             # trim under the head

    # head, front: the screen
    x0, y0, x1, y1 = s.panel("screen", 0, 15, 13, 10, DARK, light=K.ALLOY[3], dark=bump)
    s.rect(x0 + 1, y0 + 1, x1 - 1, y1 - 1, SCREEN)
    three = ("##", " #", "##", " #", "##")
    seven = ("##", " #", " #", " #", " #")
    two = ("##", " #", "##", "# ", "##")
    _digit(s, x0 + 2, y0 + 2, three, K.T_PLASMA)
    _digit(s, x0 + 5, y0 + 2, seven, K.T_PLASMA)
    s.put(x0 + 7, y0 + 6, K.T_CYAN)                                       # decimal point
    _digit(s, x0 + 8, y0 + 2, two, K.T_PLASMA)
    for i in range(9):                                                    # bar graph under the digits
        s.put(x0 + 2 + i, y0 + 8, K.T_CYAN if i < 6 else K.T_DEEP)
    s.put(x1 - 2, y0 + 2, K.T_TEAL)                                       # unit mark

    x0, y0, x1, y1 = s.panel("back", 13, 0, 12, 15, ALLOY_LO, light=K.ALLOY[1], dark=bump)
    s.rect(x0 + 3, y0 + 3, x0 + 8, y0 + 8, K.ALLOY[0])                    # cell door
    s.hline(x0 + 3, x0 + 8, y0 + 3, K.A_LIGHT)
    s.put(x0 + 8, y0 + 5, K.T_TEAL)
    for y in (y0 + 10, y0 + 12):
        s.hline(x0 + 3, x0 + 8, y, K.A_DARK)
    x0, y0, x1, y1 = s.panel("head_back", 13, 15, 13, 10, DARK, light=K.ALLOY[3], dark=bump)
    s.hline(x0 + 3, x1 - 3, y0 + 4, K.A_DARK)                             # stand hinge
    x0, y0, x1, y1 = s.panel("side", 25, 0, 5, 15, DARK, light=(48, 56, 66, 255), dark=bump)
    for y in range(y0 + 3, y1 - 1, 3):
        s.hline(x0 + 1, x1 - 1, y, (44, 50, 58, 255))
    x0, y0, x1, y1 = s.tile("probe", 30, 0, 2, 16)                        # probe handle: teal, finger guard
    s.rect(x0, y0, x1, y1, K.T_TEAL)
    s.vline(x0, y0, y1, K.T_CYAN)
    s.hline(x0, x1, y0 + 2, K.T_DEEP)
    s.hline(x0, x1, y0 + 3, K.A_LIGHT)
    s.hline(x0, x1, y1, K.T_DEEP)
    x0, y0, x1, y1 = s.tile("probe_tip", 30, 16, 1, 5)
    s.vline(x0, y0, y1, STEEL_L)
    s.put(x0, y1, K.IRON[1])
    x0, y0, x1, y1 = s.tile("lead", 13, 25, 8, 1)
    for x in range(x0, x1 + 1):
        s.put(x, y0, K.T_TEAL if x % 3 else K.T_DEEP)
    x0, y0, x1, y1 = s.panel("knob", 13, 27, 4, 4, (K.ALLOY[3], K.A_LIGHT), light=lift(K.A_LIGHT, 0.3),
                             dark=K.ALLOY[0])
    s.put(x0 + 2, y0 + 1, K.T_CYAN)                                       # pointer
    s.put(x0 + 1, y0 + 2, K.T_CYAN)
    s.panel("dark", 18, 26, 12, 6, DARK, light=(48, 56, 66, 255), dark=bump)
    s.panel("top", 0, 25, 13, 5, DARK, light=K.ALLOY[3], dark=bump)
    s.save()
    return s


def meter_model(s):
    tilt = ((8, 8.5, 8.25), "x", -22.5)
    e = [
        box(s, "body", (5, 1, 7), (11, 8.5, 9.5),
            {"south": "front", "north": "back", "east": "side", "west": "side", "*": "dark"}),
        box(s, "head", (4.75, 8.2, 7), (11.25, 13.2, 9.5),
            {"south": "=screen", "north": "=head_back", "east": "side", "west": "side", "*": "top"}, rot=tilt),
        box(s, "dial", (6.75, 3.75, 9.5), (9.25, 6.25, 10.1), {"south": "=knob", "*": "dark"}, skip=("north",)),
        box(s, "probe", (11, 3, 7.75), (12, 11, 8.75), "probe"),
        box(s, "probe_tip", (11.3, 11, 8.05), (11.7, 13.5, 8.45), "probe_tip", skip=("down",)),
        box(s, "lead_up", (11.25, 0.4, 8), (11.75, 3, 8.5), "lead^", skip=("up",)),
        box(s, "lead_across", (9, 0.4, 8), (11.25, 0.9, 8.5), "lead"),
        box(s, "lead_plug", (9, 0.4, 8), (9.5, 1, 8.5), "lead", skip=("up",)),
    ]
    grip = (8, 4.2, 8.25)
    display = {
        "gui": plain((12, 22, 0), (-0.5, 1, 0), 1.12),
        "ground": plain((0, 0, 0), (0, 2, 0), 0.5),
        "fixed": plain((0, 0, 0), (-0.5, 1, 0), 1.0),
        "thirdperson_righthand": held((62, 0, 0), 0.7, grip, at=(0, 1.0, 0.5)),
        "firstperson_righthand": held((-10, -14, 0), 0.72, grip, at=(-2.0, 4.5, -1.0)),
    }
    return model("multimeter", e, display)


# ==================================================================================================
# Thermal Goggles
# ==================================================================================================
HEAD_LO, HEAD_HI = 1.6, 14.4          # the wearer's head in head-model space
HAT = 0.8                             # how far the hat layer stands off the head


def _banded_glass(s, x0, y0, x1, y1):
    """Teal glass in horizontal bands (deep at the top to cyan), with one plasma glint."""
    h = y1 - y0 + 1
    for y in range(y0, y1 + 1):
        band = GLASS[min(len(GLASS) - 1, (y - y0) * len(GLASS) // h)]
        s.hline(x0, x1, y, band)
    s.put(x0 + 1, y0 + 1, K.T_PLASMA)
    s.put(x0 + 2, y0 + 1, K.T_CYAN)
    s.put(x1 - 1, y1 - 1, K.T_DEEP)


def goggles_sheet():
    s = Sheet("thermal_goggles_worn")
    x0, y0, x1, y1 = s.panel("lens", 0, 0, 10, 8, STEEL, light=STEEL_L, dark=STEEL_D)
    s.rect(x0 + 1, y0 + 1, x1 - 1, y1 - 1, K.A_DARK)
    _banded_glass(s, x0 + 2, y0 + 1, x1 - 2, y1 - 1)
    x0, y0, x1, y1 = s.panel("barrel", 10, 0, 4, 8, STEEL, light=STEEL_L, dark=STEEL_D)
    s.vline(x0 + 1, y0, y1, K.T_TEAL)                                    # focus ring
    s.vline(x0 + 2, y0, y1, STEEL_D)
    x0, y0, x1, y1 = s.panel("plate", 0, 8, 28, 8, ALLOY)
    s.hline(x0 + 1, x1 - 1, y0 + 6, K.A_DARK)
    s.panel("strap", 0, 16, 29, 5, DARK, light=(48, 56, 66, 255), dark=(10, 12, 16, 255))
    for x in range(3, 28, 4):
        s.put(x, 18, (44, 50, 58, 255))
    x0, y0, x1, y1 = s.panel("buckle", 0, 21, 6, 6, STEEL, light=STEEL_L, dark=STEEL_D)
    s.rect(x0 + 2, y0 + 2, x0 + 3, y0 + 3, DARK[0])
    x0, y0, x1, y1 = s.panel("cell", 6, 21, 8, 7, (K.T_DEEP, shade(K.T_TEAL, 0.6)),
                             light=K.T_TEAL, dark=(4, 22, 28, 255))
    s.put(x0 + 2, y0 + 3, K.T_CYAN)
    s.put(x0 + 4, y0 + 3, K.T_CYAN)
    s.put(x0 + 6, y0 + 3, K.T_PLASMA)
    s.panel("nub", 14, 21, 4, 4, ALLOY)
    s.flat("led", 18, 21, 2, 2, K.T_CYAN)
    s.put(18, 21, K.T_PLASMA)
    s.panel("dark", 20, 21, 8, 8, ALLOY_LO, light=K.ALLOY[1], dark=K.A_DARK)
    s.save()
    return s


def goggles_model(s):
    lo, hi = HEAD_LO - HAT, HEAD_HI + HAT            # the hat layer's faces: 0.8 and 15.2
    e = [
        box(s, "strap_west", (lo - 0.45, 6.0, lo), (lo - 0.05, 8.4, hi), "strap", skip=("north", "south")),
        box(s, "strap_east", (hi + 0.05, 6.0, lo), (hi + 0.45, 8.4, hi), "strap", skip=("north", "south")),
        box(s, "strap_back", (lo - 0.45, 6.0, hi + 0.05), (hi + 0.45, 8.4, hi + 0.45), "strap"),
        box(s, "buckle", (6.8, 5.8, hi + 0.45), (9.2, 8.6, hi + 0.75), "=buckle", skip=("north",)),
        box(s, "face_plate", (lo - 0.45, 5.4, lo - 0.5), (hi + 0.45, 9.0, lo - 0.05),
            {"north": "plate", "*": "dark"}),
        box(s, "lens_west", (2.4, 5.2, lo - 2.0), (7.2, 9.2, lo - 0.5),
            {"north": "=lens", "south": "dark", "*": "barrel"}),
        box(s, "lens_east", (8.8, 5.2, lo - 2.0), (13.6, 9.2, lo - 0.5),
            {"north": "=lens", "south": "dark", "*": "barrel"}),
        box(s, "sensor", (7.2, 8.4, lo - 1.4), (8.8, 9.8, lo - 0.5), "nub"),
        box(s, "sensor_led", (7.6, 8.8, lo - 1.6), (8.4, 9.4, lo - 1.4), "led", shade_=False, skip=("south",)),
        box(s, "power_cell", (hi + 0.45, 5.6, 5), (hi + 1.3, 8.8, 9), {"east": "=cell", "*": "dark"},
            skip=("west",)),
    ]
    display = {
        "gui": plain((14, 205, 0), (0, 1, 0), 0.95),
        "ground": plain((0, 0, 0), (0, 2, 0), 0.5),
        "fixed": plain((0, 180, 0), (0, 1, 0), 0.95),
        "thirdperson_righthand": plain((80, 180, 0), (0, 2, -1), 0.5),
        "firstperson_righthand": plain((8, 165, 0), (2, 3, 0), 0.6),
    }
    return model("thermal_goggles_worn", e, display)


def paint_goggles_icon():
    s = Sheet("thermal_goggles")
    strap = DARK[1]
    for x in range(1, 31):                                               # strap, sagging behind the head
        y = 10 if 4 <= x <= 27 else 11
        s.rect(x, y, x, y + 2, strap)
        s.put(x, y, (52, 60, 70, 255))
        s.put(x, y + 2, (10, 12, 16, 255))
    for x in range(5, 27, 4):
        s.put(x, 11, (64, 72, 84, 255))
    s.rect(3, 12, 28, 21, K.ALLOY[0])                                    # face plate
    s.noise(4, 13, 27, 20, ALLOY)
    s.hline(3, 28, 12, K.A_LIGHT)
    s.hline(3, 28, 21, K.A_DARK)
    s.vline(3, 12, 21, K.ALLOY[3])
    s.vline(28, 12, 21, K.A_DARK)
    for (cx0, cx1) in ((4, 14), (17, 27)):                               # twin lenses
        s.rect(cx0, 11, cx1, 22, K.IRON[1])
        s.hline(cx0 + 1, cx1 - 1, 10, K.IRON[0])
        s.hline(cx0 + 1, cx1 - 1, 23, STEEL_D)
        s.hline(cx0, cx1, 11, STEEL_L)
        s.vline(cx0, 11, 22, K.IRON[0])
        s.vline(cx1, 12, 22, STEEL_D)
        s.hline(cx0, cx1, 22, STEEL_D)
        s.rect(cx0 + 1, 12, cx1 - 1, 21, K.A_DARK)
        _banded_glass(s, cx0 + 2, 13, cx1 - 2, 20)
        s.put(cx0 + 2, 13, K.A_DARK)                                     # rounded glass corners
        s.put(cx1 - 2, 13, K.A_DARK)
        s.put(cx0 + 2, 20, K.A_DARK)
        s.put(cx1 - 2, 20, K.A_DARK)
        s.put(cx0 + 3, 14, K.T_GLOW)
    s.rect(15, 9, 16, 12, K.ALLOY[3])                                    # sensor between the lenses
    s.put(15, 9, K.A_LIGHT)
    s.put(15, 10, K.T_PLASMA)
    s.put(16, 10, K.T_CYAN)
    s.rect(15, 14, 16, 19, K.ALLOY[0])                                   # bridge
    s.put(15, 14, K.A_LIGHT)
    s.hline(15, 16, 19, K.A_DARK)
    s.save()


# ==================================================================================================
# Filter Mask
# ==================================================================================================
def mask_sheet():
    s = Sheet("filter_mask_worn")
    x0, y0, x1, y1 = s.panel("cup", 0, 0, 15, 10, ALLOY)
    s.hline(x0 + 1, x1 - 1, y0 + 1, K.T_TEAL)                            # face seal
    s.put(x0 + 1, y0 + 1, K.T_CYAN)
    for y in (y0 + 5, y0 + 7):                                           # chin vents
        s.hline(x0 + 2, x0 + 4, y, K.A_DARK)
        s.hline(x1 - 4, x1 - 2, y, K.A_DARK)
    x0, y0, x1, y1 = s.panel("cup_side", 15, 0, 4, 10, ALLOY_LO, light=K.ALLOY[1], dark=K.A_DARK)
    s.vline(x1 - 1, y0 + 1, y1 - 1, K.T_DEEP)
    x0, y0, x1, y1 = s.panel("valve", 19, 0, 4, 4, DARK, light=K.ALLOY[3], dark=(10, 12, 16, 255))
    s.put(x0 + 1, y0 + 1, K.T_TEAL)
    s.put(x0 + 2, y0 + 2, K.T_DEEP)
    s.flat("led", 19, 4, 2, 2, K.T_CYAN)
    s.put(19, 4, K.T_PLASMA)
    x0, y0, x1, y1 = s.panel("can_front", 23, 0, 7, 7, ALLOY_LO, light=K.A_LIGHT, dark=K.A_DARK)
    for x in range(x0 + 1, x1):                                          # pleated filter face
        s.vline(x, y0 + 1, y1 - 1, PLEAT[(x - x0) % 2])
    s.hline(x0 + 1, x1 - 1, y0 + 3, K.T_TEAL)                            # retaining bar
    x0, y0, x1, y1 = s.panel("can_side", 0, 10, 6, 7, ALLOY_LO, light=K.A_LIGHT, dark=K.A_DARK)
    s.vline(x0 + 1, y0 + 1, y1 - 1, PLEAT[0])                            # pleats show at the open end
    s.vline(x0 + 4, y0 + 1, y1 - 1, K.T_TEAL)                            # lock ring
    x0, y0, x1, y1 = s.panel("socket", 6, 10, 6, 6, ALLOY, light=K.A_LIGHT, dark=K.A_DARK)
    s.rect(x0 + 1, y0 + 1, x1 - 1, y1 - 1, (8, 10, 14, 255))             # the empty hole
    s.put(x0 + 1, y0 + 1, K.T_DEEP)
    s.put(x1 - 1, y1 - 1, K.T_DEEP)
    s.panel("socket_side", 12, 13, 3, 6, ALLOY, light=K.A_LIGHT, dark=K.A_DARK)
    x0, y0, x1, y1 = s.panel("nose", 12, 10, 7, 3, ALLOY)
    s.panel("strap", 0, 19, 29, 3, DARK, light=(48, 56, 66, 255), dark=(10, 12, 16, 255))
    s.panel("dark", 20, 10, 8, 6, ALLOY_LO, light=K.ALLOY[1], dark=K.A_DARK)
    s.save()
    return s


def mask_model(s, loaded):
    lo, hi = HEAD_LO - HAT, HEAD_HI + HAT
    e = [
        box(s, "strap_west", (lo - 0.45, 3.2, lo), (lo - 0.05, 4.6, hi), "strap", skip=("north", "south")),
        box(s, "strap_east", (hi + 0.05, 3.2, lo), (hi + 0.45, 4.6, hi), "strap", skip=("north", "south")),
        box(s, "strap_back", (lo - 0.45, 3.2, hi + 0.05), (hi + 0.45, 4.6, hi + 0.45), "strap"),
        box(s, "strap_front_west", (lo - 0.45, 3.2, lo - 0.45), (4.4, 4.6, lo - 0.05), "strap"),
        box(s, "strap_front_east", (11.6, 3.2, lo - 0.45), (hi + 0.45, 4.6, lo - 0.05), "strap"),
        box(s, "cup", (4.4, 1.6, lo - 2.0), (11.6, 5.8, lo - 0.05),
            {"north": "=cup", "south": "dark", "east": "cup_side", "west": "cup_side", "*": "dark"}),
        box(s, "nose_bridge", (6.4, 5.8, lo - 1.4), (9.6, 6.4, lo - 0.05), "nose", skip=("down",)),
        box(s, "valve", (7, 2.6, lo - 2.6), (9, 4.6, lo - 2.0), {"north": "=valve", "*": "dark"}, skip=("south",)),
        box(s, "status_led", (5.0, 4.6, lo - 2.15), (5.6, 5.2, lo - 2.0), "led", shade_=False, skip=("south",)),
    ]
    if loaded:
        e.append(box(s, "canister", (11.6, 1.4, lo - 3.6), (14.8, 4.6, lo - 0.6),
                     {"north": "=can_front", "south": "dark", "*": "can_side"}))
    else:
        e.append(box(s, "socket", (11.6, 1.6, lo - 1.5), (14.4, 4.4, lo - 0.6),
                     {"north": "=socket", "south": "dark", "*": "socket_side"}))
    display = {
        "gui": plain((14, 205, 0), (-1, 5, 0), 1.0),
        "ground": plain((0, 0, 0), (0, 4, 0), 0.5),
        "fixed": plain((0, 180, 0), (0, 4, 0), 0.95),
        "thirdperson_righthand": plain((80, 180, 0), (0, 2, -3), 0.5),
        "firstperson_righthand": plain((8, 165, 0), (2, 5, 0), 0.6),
    }
    return model("filter_mask_worn", e, display)


def paint_mask_icon(loaded):
    s = Sheet("filter_mask" if loaded else "filter_mask_empty")
    strap = DARK[1]
    for x in range(1, 31):                                               # strap
        s.rect(x, 9, x, 10, strap)
        s.put(x, 9, (52, 60, 70, 255))
    for (x, y) in ((1, 11), (2, 12), (30, 11), (29, 12)):
        s.put(x, y, strap)
    # cup: a wide top narrowing to the chin
    for y in range(8, 27):
        t = (y - 8) / 18.0
        half = int(round(9.5 - 4.5 * t * t))
        for x in range(13 - half, 13 + half + 1):
            edge_l = x == 13 - half
            edge_r = x == 13 + half
            col = s.rng.choice(ALLOY)
            if edge_l or y == 8:
                col = K.A_LIGHT
            elif edge_r or y == 26:
                col = K.A_DARK
            elif x > 13 + half - 3:
                col = K.ALLOY[2]
            s.put(x, y, col)
    s.hline(6, 20, 10, K.T_TEAL)                                         # face seal
    s.put(6, 10, K.T_CYAN)
    s.put(7, 10, K.T_CYAN)
    s.rect(10, 5, 16, 8, K.ALLOY[1])                                     # nose bridge
    s.hline(10, 16, 5, K.A_LIGHT)
    s.vline(16, 6, 8, K.A_DARK)
    s.rect(10, 15, 16, 21, DARK[0])                                      # exhale valve
    s.hline(10, 16, 15, K.ALLOY[3])
    s.vline(10, 15, 21, K.ALLOY[3])
    for y in (17, 19):
        s.hline(12, 15, y, (10, 12, 16, 255))
    s.put(12, 17, K.T_TEAL)
    s.put(7, 13, K.T_PLASMA)                                             # status LED
    s.put(7, 14, K.T_CYAN)
    for y in (22, 24):                                                   # chin vents
        s.hline(10, 12, y, K.A_DARK)
    if loaded:                                                           # side canister, pleats showing
        s.rect(21, 14, 30, 25, K.ALLOY[2])
        s.hline(22, 29, 13, K.A_LIGHT)
        s.hline(22, 29, 26, K.A_DARK)
        s.vline(21, 14, 25, K.A_LIGHT)
        s.vline(30, 14, 25, K.A_DARK)
        for x in range(23, 29):
            s.vline(x, 15, 24, PLEAT[(x - 23) % 2])
        s.hline(22, 29, 19, K.T_TEAL)
        s.put(22, 19, K.T_CYAN)
        s.hline(22, 29, 20, K.T_DEEP)
    else:                                                                # the socket, empty
        s.rect(21, 15, 28, 24, K.ALLOY[1])
        s.hline(21, 28, 15, K.A_LIGHT)
        s.vline(21, 15, 24, K.A_LIGHT)
        s.hline(21, 28, 24, K.A_DARK)
        s.vline(28, 15, 24, K.A_DARK)
        s.rect(23, 17, 26, 22, (8, 10, 14, 255))
        s.put(23, 17, K.T_DEEP)
        s.put(26, 22, K.T_DEEP)
        s.put(22, 19, K.T_TEAL)                                          # bayonet lugs
        s.put(27, 20, K.T_TEAL)
    s.save()


# ==================================================================================================
# package entry
# ==================================================================================================
def sheets():
    """Paint (additively) and return every atlas, keyed by tool."""
    return {"drill": drill_sheet(), "lance": lance_sheet(), "wrench": wrench_sheet(),
            "meter": meter_sheet(), "goggles": goggles_sheet(), "mask": mask_sheet()}


def paint():
    sheets()
    paint_goggles_icon()
    paint_mask_icon(True)
    paint_mask_icon(False)


PULSE = []          # tools do not animate


def models():
    """Every model of the package, keyed by its path under ``models/`` without the extension."""
    quiet, G_force = sys.stdout, G.FORCE
    sys.stdout = open(os.devnull, "w")              # the sheets are only needed for their tile maps here
    G.FORCE = False
    try:
        sh = {"drill": drill_sheet(), "lance": lance_sheet(), "wrench": wrench_sheet(),
              "meter": meter_sheet(), "goggles": goggles_sheet(), "mask": mask_sheet()}
    finally:
        sys.stdout.close()
        sys.stdout = quiet
        G.FORCE = G_force
    out = {}
    out["item/power_drill"] = drill_model(sh["drill"], "single")
    out["item/power_drill_area"] = drill_model(sh["drill"], "area")
    out["item/power_drill_vein"] = drill_model(sh["drill"], "vein")
    out["item/cutting_lance"] = lance_model(sh["lance"], "none")
    for kind in LENS_ORDER:
        out["item/cutting_lance_" + kind] = lance_model(sh["lance"], kind)
    out["item/arc_wrench"] = wrench_model(sh["wrench"], "charged")
    out["item/arc_wrench_idle"] = wrench_model(sh["wrench"], "idle")
    out["item/arc_wrench_carrying"] = wrench_model(sh["wrench"], "carrying")
    out["item/multimeter"] = meter_model(sh["meter"])
    out["item/thermal_goggles"] = goggles_model(sh["goggles"])
    out["item/thermal_goggles_icon"] = flat_model("thermal_goggles")
    out["item/filter_mask"] = mask_model(sh["mask"], True)
    out["item/filter_mask_empty"] = mask_model(sh["mask"], False)
    out["item/filter_mask_icon"] = flat_model("filter_mask")
    out["item/filter_mask_empty_icon"] = flat_model("filter_mask_empty")
    return out


def _ref(name):
    return {"type": "minecraft:model", "model": "nerotech:item/" + name}


def _worn(head, icon):
    """The head model on the wearer, the flat icon everywhere else."""
    return {"type": "minecraft:select", "property": "minecraft:display_context",
            "cases": [{"when": "head", "model": _ref(head)}], "fallback": _ref(icon)}


def _has(component, on_true, on_false):
    return {"type": "minecraft:condition", "property": "minecraft:has_component",
            "component": "nerotech:" + component, "on_true": on_true, "on_false": on_false}


def item_definitions():
    """Client item definitions, as ``{resource root: {item id: definition}}``.

    ``resources`` is read from 1.21.4 up (1.21.1 has no item definitions and uses
    ``models/item/<id>.json`` alone). ``resources-1.21.5`` overlays it from 1.21.5, where a select
    may read a component's value: that is what the lance needs to tell the four lenses apart. On
    1.21.4 a fitted lance shows the clear quartz glass whatever the lens.

    Not wired, because nothing a definition can read carries it: the drill's mode and whether a tool
    holds any charge both live inside ``tool_charge`` beside the energy number.
    """
    base = {
        "power_drill": {"model": _ref("power_drill")},
        "cutting_lance": {"model": _has("fitted_lens", _ref("cutting_lance_quartz"), _ref("cutting_lance"))},
        "arc_wrench": {"model": _has("carried_machine", _ref("arc_wrench_carrying"), _ref("arc_wrench"))},
        "multimeter": {"model": _ref("multimeter")},
        "thermal_goggles": {"model": _worn("thermal_goggles", "thermal_goggles_icon")},
        "filter_mask": {"model": _has("fitted_filter",
                                      _worn("filter_mask", "filter_mask_icon"),
                                      _worn("filter_mask_empty", "filter_mask_empty_icon"))},
    }
    lensed = {"cutting_lance": {"model": {
        "type": "minecraft:select", "property": "minecraft:component", "component": "nerotech:fitted_lens",
        "cases": [{"when": kind, "model": _ref("cutting_lance_" + kind)} for kind in LENS_ORDER],
        "fallback": _ref("cutting_lance")}}}
    return {"resources": base, "resources-1.21.5": lensed}


def _write(path, obj):
    text = json.dumps(obj, indent=2) + "\n"
    old = None
    if os.path.exists(path):
        with open(path, "r", encoding="utf-8", newline="") as fh:
            old = fh.read()
    if old == text:
        return
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as fh:
        fh.write(text)
    print("wrote", os.path.relpath(path, G.ROOT))


def write_models():
    """Rewrite the package's models and item definitions in place (they belong to it alone)."""
    for rel, mdl in sorted(models().items()):
        _write(K.asset_path("models/%s.json" % rel), mdl)
    for root, defs in sorted(item_definitions().items()):
        for name, definition in sorted(defs.items()):
            _write(os.path.join(G.ROOT, "src/main", root, "assets/nerotech/items", name + ".json"), definition)


if __name__ == "__main__":
    if "--models" in sys.argv:
        write_models()
    if "--paint" in sys.argv:                        # this package only (add --force=tools to repaint)
        paint()
