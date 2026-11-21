package za.co.neroland.nerotech.upgrade;

/**
 * The effects of NeroTech's own upgrade modules ({@link NeroTechModule}) without Minecraft types
 * (unit-tested). Both are bounded: at most {@link #MAX_COUNTED} modules of a kind count, the heat band
 * never widens past {@link #MAX_HEADROOM_PERMILLE} of the headroom between the throttle threshold and
 * the heat ceiling (so a module never brings a machine to its ceiling or a reactor to meltdown), and
 * pollution never falls below the configured floor.
 */
public final class ModuleMath {

    /** Modules of one kind beyond this many add nothing. */
    public static final int MAX_COUNTED = 4;

    /** The most of the throttle-to-ceiling headroom Heat Sink Modules may open (permille). */
    public static final int MAX_HEADROOM_PERMILLE = 500;

    private ModuleMath() {
    }

    /**
     * The throttle threshold with {@code heatSinks} Heat Sink Modules fitted: {@code base} plus a share
     * of the headroom up to {@code capacity}, {@code perModulePermille} per module, capped at
     * {@link #MAX_HEADROOM_PERMILLE}. Never below {@code base}, always below {@code capacity} when
     * {@code base} is.
     */
    public static int throttleThreshold(int base, int capacity, int heatSinks, int perModulePermille) {
        int counted = Math.max(0, Math.min(MAX_COUNTED, heatSinks));
        long headroom = (long) capacity - base;
        if (counted == 0 || headroom <= 0) {
            return base;
        }
        long share = Math.min(MAX_HEADROOM_PERMILLE, (long) counted * Math.max(0, perModulePermille));
        return (int) (base + headroom * share / 1000L);
    }

    /**
     * The pollution factor (permille of normal) with {@code scrubbers} Scrubber Modules fitted:
     * {@code 1000 - n × perModulePermille}, never below {@code floorPermille} nor above 1000.
     */
    public static int pollutionPermille(int scrubbers, int perModulePermille, int floorPermille) {
        int counted = Math.max(0, Math.min(MAX_COUNTED, scrubbers));
        int floor = Math.max(0, Math.min(1000, floorPermille));
        int value = 1000 - counted * Math.max(0, perModulePermille);
        return Math.max(floor, Math.min(1000, value));
    }
}
