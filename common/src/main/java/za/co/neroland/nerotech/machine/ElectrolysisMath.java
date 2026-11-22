package za.co.neroland.nerotech.machine;

/**
 * The Electrolyzer's duration and per-tick cost. Speed modules shorten an operation but no longer make it
 * cheaper: the per-tick draw rises with the module speed multiplier, so the energy one operation costs is
 * the same at any module speed. Only Efficiency modules and the preset change the energy per operation,
 * which keeps electrolysis a net energy sink against any hydrogen burner (a flat per-tick cost let 8 Speed
 * modules, Eco and the Efficiency floor make hydrogen for about 83 NE a unit). Pure JVM; no game classes.
 */
public final class ElectrolysisMath {

    private ElectrolysisMath() {
    }

    /** Ticks one operation takes: {@code baseTicks} divided by module speed times preset speed, at least 1. */
    public static int operationTicks(int baseTicks, double moduleSpeed, double presetSpeed) {
        return Math.max(1, (int) Math.round(baseTicks / Math.max(0.01D, moduleSpeed * presetSpeed)));
    }

    /**
     * NE drawn per working tick: the base cost times Efficiency and preset energy factors, times the module
     * speed multiplier (never below 1).
     */
    public static int costPerTick(int baseNePerTick, double energyMultiplier, double presetEnergy,
            double moduleSpeed) {
        return (int) Math.max(0, Math.round(baseNePerTick * energyMultiplier * presetEnergy
                * Math.max(1.0D, moduleSpeed)));
    }
}
