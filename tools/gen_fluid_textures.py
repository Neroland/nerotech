#!/usr/bin/env python3
"""Generate the still/flow textures for NeroTech's gas transport fluids.

NeroTech's gases (`nerotech:hydrogen`, and the shared `nerospace:oxygen` as fluid `nerotech:oxygen`) are registered as fluids so that any
mod's fluid pipe can carry them (see wiki/Fluids-and-Gases.md). They are never placed in the
world, so these textures only ever appear in *other* mods' tank and pipe GUIs — a flat, softly
mottled tint per gas is enough, and keeps them readable next to water.

* Deterministic: the mottling seeds from the texture name, so re-runs are byte-stable.
* ADDITIVE-ONLY: skips any PNG that already exists; pass --force to replace them.
* No Pillow needed — writes the PNGs directly (zlib + struct), so `gradlew genAssets` stays
  green everywhere.

Outputs into common/src/main/resources/assets/nerotech/textures/block — the vanilla block atlas
already stitches every namespace's `textures/block/`, so no atlas source file is needed (an
`assets/nerotech/atlases/blocks.json` is never read: atlas definitions live under the atlas's own
`minecraft` namespace).
"""
import os
import random
import struct
import sys
import zlib

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from nerotech_target import src_base, target_label  # noqa: E402

SIZE = 16
FRAMES = 4  # a 16x64 strip; the .mcmeta animates it slowly so a piped gas reads as "moving"

# Pale blue-white hydrogen, pale teal oxygen — both light enough to read as a gas, not a liquid.
GASES = {
    "hydrogen": (176, 206, 235),
    "oxygen": (150, 226, 214),
}
ALPHA = 200


def _mottle(base, seed):
    """One frame of softly mottled pixels around `base`."""
    rng = random.Random(seed)
    rows = []
    for _ in range(SIZE):
        row = bytearray()
        for _ in range(SIZE):
            jitter = rng.randint(-12, 12)
            row += bytes(max(0, min(255, channel + jitter)) for channel in base)
            row.append(ALPHA)
        rows.append(bytes(row))
    return rows


def _png(path, rows):
    raw = b"".join(b"\x00" + row for row in rows)
    height = len(rows)

    def chunk(tag, payload):
        body = tag + payload
        return struct.pack(">I", len(payload)) + body + struct.pack(">I", zlib.crc32(body))

    data = (b"\x89PNG\r\n\x1a\n"
            + chunk(b"IHDR", struct.pack(">IIBBBBB", SIZE, height, 8, 6, 0, 0, 0))
            + chunk(b"IDAT", zlib.compress(raw, 9))
            + chunk(b"IEND", b""))
    with open(path, "wb") as handle:
        handle.write(data)


def main():
    force = "--force" in sys.argv
    out_dir = os.path.join(src_base(), "src/main/resources/assets/nerotech/textures/block")
    os.makedirs(out_dir, exist_ok=True)
    print("gen_fluid_textures: target = %s" % target_label())

    for gas, colour in GASES.items():
        for kind in ("still", "flow"):
            name = "%s_%s" % (gas, kind)
            png = os.path.join(out_dir, name + ".png")
            meta = png + ".mcmeta"
            if os.path.exists(png) and not force:
                print("  skip %s (exists)" % name)
                continue
            rows = []
            for frame in range(FRAMES):
                rows.extend(_mottle(colour, "%s:%d" % (name, frame)))
            _png(png, rows)
            with open(meta, "w", encoding="utf-8", newline="\n") as handle:
                handle.write('{\n  "animation": {\n    "frametime": %d\n  }\n}\n'
                             % (6 if kind == "flow" else 10))
            print("  wrote %s (%dx%d, %d frames)" % (name, SIZE, SIZE * FRAMES, FRAMES))


if __name__ == "__main__":
    main()
