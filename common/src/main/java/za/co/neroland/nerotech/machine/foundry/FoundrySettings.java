package za.co.neroland.nerotech.machine.foundry;

import za.co.neroland.nerotech.config.NeroTechConfig;

/**
 * The foundry family's tunables, read in one place. The defaults are inline until the integrator moves
 * the keys into {@code NeroTechConfig} (names, ranges and comments are listed for it); then each method
 * becomes a one-line delegate and no caller changes.
 */
public final class FoundrySettings {

    private FoundrySettings() {
    }

    /** {@code foundryBatchUnits}: recipe units a 3³ foundry runs per cycle. */
    public static int batchUnits() {
        return NeroTechConfig.foundryBatchUnits();
    }

    /** {@code foundryLargeBatchMultiplier}: how many times the 3³ batch a 5³ foundry runs. */
    public static int largeBatchMultiplier() {
        return NeroTechConfig.foundryLargeBatchMultiplier();
    }

    /** {@code foundryHeatPortCapacity}: heat a Foundry Heat Port buffers. */
    public static int heatPortCapacity() {
        return NeroTechConfig.foundryHeatPortCapacity();
    }

    /** The furnace's own heat ceiling: the shared machine heat scale. */
    public static int furnaceCapacity() {
        return NeroTechConfig.heatCapacity();
    }

    /** {@code foundryHeatLossPermille}: permille of the furnace's lead over ambient lost per tick. */
    public static int heatLossPermille() {
        return NeroTechConfig.foundryHeatLossPermille();
    }

    /** {@code foundryHeatDrawPerTick}: furnace heat a running batch uses per tick. */
    public static int heatDrawPerTick() {
        return NeroTechConfig.foundryHeatDrawPerTick();
    }

    /** {@code foundryHeatIntakeIntervalTicks}: how often the controller drains its ports. */
    public static int heatIntakeIntervalTicks() {
        return NeroTechConfig.foundryHeatIntakeIntervalTicks();
    }

    /** {@code foundryLitHoldTicks}: ticks the shell stays lit after the last working tick. */
    public static int litHoldTicks() {
        return NeroTechConfig.foundryLitHoldTicks();
    }
}
