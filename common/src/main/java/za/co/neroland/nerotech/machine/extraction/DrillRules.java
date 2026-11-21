package za.co.neroland.nerotech.machine.extraction;

/**
 * The Deep Core Drill's rules without Minecraft types (unit-tested): operation length, the operation
 * cadence (the deposit is read once per completed operation, never per tick), head wear, yield and
 * the stall precedence. Machine-scoped numbers only; no player data.
 */
public final class DrillRules {

    private DrillRules() {
    }

    /** Why a drill is not working, in the order they are checked. */
    public enum Stall {
        NONE,
        UNFORMED,
        DIMENSION_DISABLED,
        NO_HEAD,
        DEPLETED,
        OUTPUT_FULL
    }

    /** The first reason a drill cannot run, or {@link Stall#NONE}. */
    public static Stall stall(boolean formed, boolean dimensionDisabled, boolean hasHead, boolean depleted,
            boolean outputRoom) {
        if (!formed) {
            return Stall.UNFORMED;
        }
        if (dimensionDisabled) {
            return Stall.DIMENSION_DISABLED;
        }
        if (!hasHead) {
            return Stall.NO_HEAD;
        }
        if (depleted) {
            return Stall.DEPLETED;
        }
        return outputRoom ? Stall.NONE : Stall.OUTPUT_FULL;
    }

    /** Ticks one operation takes after the speed multiplier and preset factor (never below 1). */
    public static int operationTicks(int baseTicks, double speedMultiplier, double presetSpeedFactor) {
        double factor = Math.max(0.01D, speedMultiplier * presetSpeedFactor);
        return Math.max(1, (int) Math.round(Math.max(1, baseTicks) / factor));
    }

    /**
     * One powered tick of progress: returns the new progress, where 0 after a non-zero input means an
     * operation completed this tick (the only moment the caller reads the deposit).
     */
    public static int advance(int progress, int operationTicks) {
        int next = progress + 1;
        return next >= Math.max(1, operationTicks) ? 0 : next;
    }

    /** Whether {@link #advance} just completed an operation. */
    public static boolean completed(int before, int after) {
        return after == 0 && before >= 0;
    }

    /** Operations left on a head of {@code tier} that has used {@code used} (never negative). */
    public static int operationsLeft(int used, DrillHeadTier tier) {
        return Math.max(0, tier.operations() - Math.max(0, used));
    }

    /** The item bar (0-13) for a head of {@code tier} that has used {@code used}: full when fresh. */
    public static int wearBarWidth(int used, DrillHeadTier tier) {
        int ops = Math.max(1, tier.operations());
        return Math.round(13.0F * operationsLeft(used, tier) / ops);
    }

    /** Head wear after one operation: the new used count; equal to 0 together with a consumed head. */
    public static int wear(int used, DrillHeadTier tier) {
        int next = Math.max(0, used) + 1;
        return next >= tier.operations() ? 0 : next;
    }

    /** Whether the operation that took the head from {@code used} wore the head out. */
    public static boolean wornOut(int used, DrillHeadTier tier) {
        return Math.max(0, used) + 1 >= tier.operations();
    }

    /**
     * Convert wear between tiers as a fraction of life, rounded up, so no conversion can refund wear.
     * Used to move a pre-0.8.0 machine-held wear figure onto the fitted head when an old save loads.
     */
    public static int carryWear(int used, DrillHeadTier from, DrillHeadTier to) {
        int u = Math.max(0, used);
        if (from == null || to == null || from == to || u == 0) {
            return u;
        }
        long scaled = ((long) u * to.operations() + from.operations() - 1) / from.operations();
        return (int) Math.min(to.operations() - 1L, scaled);
    }

    /** Items for one operation: the deposit's yield times the head's multiplier, at least 1 when any. */
    public static int headYield(int depositYield, DrillHeadTier tier) {
        if (depositYield <= 0) {
            return 0;
        }
        long v = (long) depositYield * tier.yieldPermille() / 1000L;
        return (int) Math.max(1L, Math.min(Integer.MAX_VALUE, v));
    }
}
