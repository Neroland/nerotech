package za.co.neroland.nerotech.machine;

/**
 * The Wind Turbine's altitude curve (Stage D) — pure integer/float maths with no Minecraft types, so
 * the balance can be unit-tested off-thread and off-world (see {@code WindMathTest}).
 *
 * <p>Wind strength rises linearly with altitude between {@value #MIN_Y} and {@value #MAX_Y} and is
 * clamped outside that band: a turbine at or below sea-level-ish y=80 runs at {@value #MIN_FACTOR}×,
 * one at or above y=200 runs at {@value #MAX_FACTOR}×. Height is the only reason to build tall, so
 * the curve is deliberately smooth and monotone — no cliffs for a player to discover by accident.
 */
public final class WindMath {

    /** Bottom of the curve: at or below this y the turbine sits at {@value #MIN_FACTOR}×. */
    public static final int MIN_Y = 80;
    /** Top of the curve: at or above this y the turbine sits at {@value #MAX_FACTOR}×. */
    public static final int MAX_Y = 200;
    /** Multiplier at (and below) {@link #MIN_Y}. */
    public static final double MIN_FACTOR = 0.5D;
    /** Multiplier at (and above) {@link #MAX_Y}. */
    public static final double MAX_FACTOR = 2.0D;

    private WindMath() {
    }

    /**
     * The altitude multiplier at world height {@code y}: {@value #MIN_FACTOR} at or below
     * {@link #MIN_Y}, {@value #MAX_FACTOR} at or above {@link #MAX_Y}, linear in between.
     */
    public static double heightFactor(int y) {
        if (y <= MIN_Y) {
            return MIN_FACTOR;
        }
        if (y >= MAX_Y) {
            return MAX_FACTOR;
        }
        return MIN_FACTOR + (MAX_FACTOR - MIN_FACTOR) * (y - MIN_Y) / (MAX_Y - MIN_Y);
    }

    /**
     * NE/tick for one turbine: the config base scaled by the altitude curve and the combined
     * machine/dimension multiplier, rounded and floored at 0 (an airless dimension multiplies to 0).
     *
     * @param baseNePerTick the {@code windTurbineNePerTick} config value
     * @param y             the turbine's world height
     * @param multiplier    upgrades × preset × dimension, already combined by the caller
     */
    public static int ratePerTick(int baseNePerTick, int y, double multiplier) {
        if (baseNePerTick <= 0 || multiplier <= 0.0D) {
            return 0;
        }
        return Math.max(0, (int) Math.round(baseNePerTick * heightFactor(y) * multiplier));
    }
}
