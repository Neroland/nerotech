package za.co.neroland.nerotech.machine.metallurgy;

import za.co.neroland.nerotech.heat.ThermalMath;

/**
 * Pure integer heat rules of the Plasma Crucible, free of Minecraft so the rule test runs on the plain
 * JVM. The crucible holds its own temperature: while idle it exchanges heat with its surroundings only
 * once every {@code holdInterval} ticks (the shared ambient step, on a slower cadence), so a hot crucible
 * stays near the heat its last job left; a hot ambient (a Nether or Cindara build) keeps it hotter still,
 * because the same step relaxes toward that ambient. A job that finds it below the recipe's
 * {@code min_heat} first fires the arc: energy goes into heat, and progress waits until the heat is there.
 * That is the cold-start premium; a warm start pays only the recipe's own energy.
 */
public final class CrucibleHeat {

    /** Upper bound on simulated ticks (a job that never completes reports -1). */
    private static final int MAX_SIM_TICKS = 1_000_000;

    private CrucibleHeat() {
    }

    /**
     * One firing tick: {@code heat} plus {@code firingHeat}, never reaching the throttle threshold (a
     * firing crucible must not overheat itself) and never above {@code capacity}.
     */
    public static int fire(int heat, int firingHeat, int throttle, int capacity) {
        int ceiling = Math.min(capacity, Math.max(0, throttle - 1));
        if (heat >= ceiling) {
            return heat;
        }
        return Math.min(ceiling, heat + Math.max(1, firingHeat));
    }

    /** Energy one firing tick costs after modules and preset ({@code factor}); at least 1. */
    public static int firingCost(int baseEnergy, double factor) {
        return (int) Math.max(1L, Math.round(Math.max(0, baseEnergy) * Math.max(0.0D, factor)));
    }

    /**
     * Heat to add back after the shared thermal step on an idle tick so that the environmental exchange
     * only happens once every {@code holdInterval} ticks. Zero when the crucible is not above ambient
     * (warming toward a hotter ambient is never slowed), on the cadence tick, or when holding is off.
     */
    public static int holdCorrection(int heatBefore, int ambient, int lossPermille, long gameTime, int holdInterval) {
        if (holdInterval <= 1 || heatBefore <= ambient) {
            return 0;
        }
        if (gameTime % holdInterval == 0L) {
            return 0;
        }
        return -ThermalMath.ambientStep(heatBefore, ambient, lossPermille);
    }

    /** Heat after {@code ticks} idle ticks from {@code heat} (ambient step on the hold cadence only). */
    public static int idle(int heat, int ambient, int lossPermille, int ticks, int holdInterval) {
        int h = heat;
        for (int t = 1; t <= ticks; t++) {
            int step = ThermalMath.ambientStep(h, ambient, lossPermille);
            h += step + holdCorrection(h, ambient, lossPermille, t, holdInterval);
        }
        return h;
    }

    /**
     * Total energy one job costs from {@code startHeat}, simulated tick by tick the way the machine runs
     * it: below {@code minHeat} a tick fires the arc ({@code firingEnergy}, {@link #fire}); at or above it
     * a tick spends {@code recipeEnergy}, advances progress and adds {@code workHeat}; every tick ends
     * with the full ambient step (a working crucible is not insulated). Returns -1 when the job can
     * never finish (the minimum heat is at or above the throttle).
     */
    public static long jobEnergy(int startHeat, int minHeat, int ambient, int lossPermille, int firingHeat,
            int firingEnergy, int workHeat, int recipeEnergy, int time, int throttle, int capacity) {
        int heat = startHeat;
        long energy = 0L;
        int progress = 0;
        for (int tick = 0; tick < MAX_SIM_TICKS; tick++) {
            if (heat < minHeat) {
                int next = fire(heat, firingHeat, throttle, capacity);
                if (next == heat) {
                    return -1L;
                }
                heat = next;
                energy += firingEnergy;
            } else {
                energy += recipeEnergy;
                progress++;
                heat = ThermalMath.clampHeat(heat + workHeat, capacity);
                if (progress >= Math.max(1, time)) {
                    return energy;
                }
            }
            heat = ThermalMath.clampHeat(heat + ThermalMath.ambientStep(heat, ambient, lossPermille), capacity);
        }
        return -1L;
    }
}
