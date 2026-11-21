package za.co.neroland.nerotech.item.tools;

import java.util.Locale;

/**
 * The focus lens fitted to a Cutting Lance and what it does (pure, unit-tested). The lens is the item
 * itself, held inside the lance as a data component and handed back when swapped, so fitting one never
 * makes or loses a lens.
 *
 * <ul>
 * <li>{@link #NONE}: the base reach and cooldown.</li>
 * <li>{@link #QUARTZ} (precise): half the cooldown.</li>
 * <li>{@link #GLACITE} (cold): blocks in {@code nerotech:cutting_lance/cold_preserved} (ice and plain
 * glass, which drop nothing without silk) come away whole, and cut ice leaves no meltwater.</li>
 * <li>{@link #EMBER} (hot): the drops come out smelted, for 1.5× the energy.</li>
 * <li>{@link #VOID} (long): twice the reach, never past {@link #MAX_RANGE}, for 2× the energy.</li>
 * </ul>
 */
public enum LanceLens {
    NONE,
    QUARTZ,
    GLACITE,
    EMBER,
    VOID;

    /** No lens reaches further than this, whatever the config says. */
    public static final int MAX_RANGE = 32;

    /** The id the data component stores. */
    public String serialized() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static LanceLens parse(String value) {
        if (value != null) {
            for (LanceLens lens : values()) {
                if (lens.serialized().equals(value)) {
                    return lens;
                }
            }
        }
        return NONE;
    }

    /** The lens a focus-lens item id ({@code nerotech:<kind>_focus_lens}) fits, or {@link #NONE}. */
    public static LanceLens ofItemPath(String path) {
        if (path == null || !path.endsWith("_focus_lens")) {
            return NONE;
        }
        return parse(path.substring(0, path.length() - "_focus_lens".length()));
    }

    /** Reach in blocks for a configured base reach. */
    public int range(int base) {
        int clamped = Math.max(1, Math.min(MAX_RANGE, base));
        return this == VOID ? Math.min(MAX_RANGE, clamped * 2) : clamped;
    }

    /** Cooldown in ticks for a configured base cooldown. */
    public int cooldown(int base) {
        int clamped = Math.max(0, base);
        return this == QUARTZ ? clamped / 2 : clamped;
    }

    /** Energy one cut costs for a configured base cost. */
    public int energy(int base) {
        int clamped = Math.max(0, base);
        return switch (this) {
            case EMBER -> clamped * 3 / 2;
            case VOID -> clamped * 2;
            default -> clamped;
        };
    }
}
