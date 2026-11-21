"""NeroTech art packages (0.8.0 "Offworld Industry").

One painter module per work package lives in this folder. Each module exposes

    paint()        -- paint and save every texture the package owns (use ``art.common``)
    PULSE = [...]  -- rows for gen_animations.py: (name, frames, frametime, amplitude, hue window)

``gen_textures.py`` calls every module's ``paint()`` and ``gen_animations.py`` appends every module's
``PULSE``; neither generator needs editing when a package lands. A package that has not been written
yet is skipped with a notice. An error inside a module that does exist is NOT swallowed: it is a bug in
that package and must fail the run.

``common`` is the shared kit (palette, helpers, shared renderer sprites) and is not a package.
"""
import importlib
import os

# Painting order. Later modules may overwrite nothing (save() is additive-only), so order only
# matters for the log. Any other ``*.py`` dropped into this folder is picked up after these.
PACKAGES = (
    "particles",
    "metallurgy",
    "foundry",
    "chemistry",
    "components",
    "heat_extraction",
    "fusion_utility",
    "media_items",
    "tools",
)

_NOT_PACKAGES = ("__init__", "common")


def package_names():
    """The known packages in order, then any extra module found in this folder (sorted)."""
    here = os.path.dirname(os.path.abspath(__file__))
    found = sorted(f[:-3] for f in os.listdir(here)
                   if f.endswith(".py") and f[:-3] not in _NOT_PACKAGES)
    names = [n for n in PACKAGES if n in found]
    names += [n for n in found if n not in PACKAGES]
    return names, [n for n in PACKAGES if n not in found]


def load_packages(quiet=False):
    """Import every package module that exists. Returns ``[(name, module), ...]``."""
    names, missing = package_names()
    if missing and not quiet:
        print("art: no module yet for: %s" % ", ".join(missing))
    return [(n, importlib.import_module("art." + n)) for n in names]


def paint_all():
    """Shared kit sprites first, then every package's ``paint()``."""
    from art import common
    common.paint_kit()
    for name, module in load_packages():
        paint = getattr(module, "paint", None)
        if paint is None:
            print("art: %s has no paint(); skipped" % name)
            continue
        print("art: painting %s" % name)
        paint()


def all_pulse():
    """Every package's ``PULSE`` rows, concatenated in package order."""
    rows = []
    for name, module in load_packages(quiet=True):
        rows.extend(getattr(module, "PULSE", ()))
    return rows
