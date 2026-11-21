package za.co.neroland.nerotech.machine.components;

/**
 * The Crystal Grower's growth rate, pure so it is testable without Minecraft. Growth is counted in
 * permille of one tick of recipe progress: 1000 is the nominal powered rate (one progress tick per game
 * tick, before Speed modules and presets, which shorten the recipe instead).
 *
 * <ul>
 *   <li><b>Unpowered</b> the grower still grows, at {@code unpoweredPermille} of the powered rate.</li>
 *   <li><b>Cold</b> ambient adds {@code perHeatPermille} for every point of heat below the threshold,
 *       capped at {@code capPermille}. The bonus multiplies both the powered and the unpowered rate.</li>
 * </ul>
 *
 * <p>Every input is clamped, so a bad config value can slow the grower or stop the bonus but can never
 * make it negative, unbounded, or faster unpowered than powered.
 */
public final class CrystalGrowthMath {

    /** The powered rate with no cold bonus: one progress tick per game tick. */
    public static final int NOMINAL = 1_000;
    /** Hard ceiling on the cold bonus whatever the config says (permille). */
    public static final int MAX_COLD_BONUS = 4_000;

    private CrystalGrowthMath() {
    }

    /** The cold bonus (permille, 0..cap) at {@code ambient}. */
    public static int coldBonus(int ambient, int threshold, int perHeatPermille, int capPermille) {
        int cap = clamp(capPermille, 0, MAX_COLD_BONUS);
        int perHeat = clamp(perHeatPermille, 0, 1_000);
        long below = (long) threshold - (long) ambient;
        if (below <= 0 || perHeat == 0 || cap == 0) {
            return 0;
        }
        return (int) Math.min(cap, below * perHeat);
    }

    /**
     * Growth this tick in permille of a progress tick.
     *
     * @param powered whether the grower paid this tick's energy
     */
    public static int ratePermille(boolean powered, int ambient, int unpoweredPermille, int threshold,
            int perHeatPermille, int capPermille) {
        int base = powered ? NOMINAL : clamp(unpoweredPermille, 0, NOMINAL);
        int bonus = coldBonus(ambient, threshold, perHeatPermille, capPermille);
        return (int) ((long) base * (NOMINAL + bonus) / NOMINAL);
    }

    /** {@link #ratePermille} with the family settings. */
    public static int ratePermille(boolean powered, int ambient) {
        return ratePermille(powered, ambient, ComponentsSettings.crystalGrowerUnpoweredPermille(),
                ComponentsSettings.crystalGrowerColdThreshold(),
                ComponentsSettings.crystalGrowerColdBonusPerHeatPermille(),
                ComponentsSettings.crystalGrowerColdBonusCapPermille());
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
