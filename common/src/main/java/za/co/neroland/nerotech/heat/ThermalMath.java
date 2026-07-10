package za.co.neroland.nerotech.heat;

/**
 * Pure integer math for the full thermal model (Stage C decision, 2026-07-10): machines are no
 * longer thermal islands — heat conducts between adjacent machines and every machine relaxes
 * toward its environment's ambient heat level. This class is deliberately free of any
 * {@code net.minecraft} import so the maths is unit-testable on the plain JVM
 * (see {@code common/src/test/java/za/co/neroland/nerotech/heat/ThermalMathTest.java}).
 *
 * <p>Units: "heat" is the same abstract unit the machines have always used (gauge scale is
 * {@code heatCapacity}, default 1000). Rates are expressed in permille (‰) so config stays
 * integer-only and hot-reload-friendly.
 *
 * <p>Performance contract: both steps are a handful of integer ops. Conduction runs on an
 * interval with cached neighbour links (never a per-tick neighbour scan); the ambient step is
 * per-tick but constant-cost.
 */
public final class ThermalMath {

    private ThermalMath() {
    }

    /**
     * Heat to move from {@code hotter} side A to side B in one conduction exchange
     * (negative when B is hotter — the same value can be applied symmetrically:
     * {@code a -= step; b += step}). Moves {@code conductivityPermille}‰ of the temperature
     * delta, clamped to half the delta so a single exchange can never overshoot equilibrium,
     * with a minimum magnitude of 1 while a delta of at least 2 exists (guarantees convergence
     * despite integer division).
     *
     * <p>Each machine of a linked pair runs this on its own (phase-offset) schedule, so a pair
     * exchanges roughly twice per interval; the default conductivity accounts for that.
     */
    public static int conductionStep(int heatA, int heatB, int conductivityPermille) {
        if (conductivityPermille <= 0) {
            return 0;
        }
        int delta = heatA - heatB;
        if (delta == 0) {
            return 0;
        }
        int magnitude = Math.abs(delta);
        int move = (int) ((long) magnitude * conductivityPermille / 1000L);
        // Never overshoot the midpoint; but keep at least 1 flowing while a real gradient exists.
        move = Math.min(move, magnitude / 2);
        if (move == 0 && magnitude >= 2) {
            move = 1;
        }
        return delta > 0 ? move : -move;
    }

    /**
     * Signed heat change for one tick of environmental exchange: the machine relaxes toward
     * {@code ambient} by {@code lossPermille}‰ of the difference, minimum magnitude 1 while any
     * difference exists (so machines always converge to ambient instead of stalling on integer
     * division). A hot machine cools; a machine colder than a hot environment warms up.
     */
    public static int ambientStep(int heat, int ambient, int lossPermille) {
        if (lossPermille <= 0 || heat == ambient) {
            return 0;
        }
        int delta = ambient - heat;
        int magnitude = Math.abs(delta);
        int step = (int) ((long) magnitude * lossPermille / 1000L);
        if (step == 0) {
            step = 1;
        }
        step = Math.min(step, magnitude);
        return delta > 0 ? step : -step;
    }

    /** Clamp a heat value into {@code [0, capacity]}. */
    public static int clampHeat(int heat, int capacity) {
        return Math.max(0, Math.min(capacity, heat));
    }
}
