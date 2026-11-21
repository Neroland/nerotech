package za.co.neroland.nerotech.machine.components;

import za.co.neroland.nerotech.config.NeroTechConfig;

/**
 * The Components family's tunables, read in one place. The values are the defaults inline until the
 * integrator moves them into {@code NeroTechConfig} (keys and comments are listed in the planning
 * scratch file for this family); every caller goes through these methods so that move touches only
 * this class.
 */
public final class ComponentsSettings {

    private ComponentsSettings() {
    }

    /** Capacity (mB) of the Component Forge's fluid input tank. */
    public static final long COMPONENT_FORGE_FLUID_CAPACITY = 8_000L;

    /** Capacity (mB) of the Crystal Grower's fluid input tank. */
    public static final long CRYSTAL_GROWER_FLUID_CAPACITY = 8_000L;

    /**
     * {@code crystalGrowerUnpoweredPermille} (int, 250, 0..1000): growth speed with no power, as permille of
     * the powered speed. 0 makes the grower stall like any other machine when unpowered.
     */
    public static int crystalGrowerUnpoweredPermille() {
        return NeroTechConfig.crystalGrowerUnpoweredPermille();
    }

    /**
     * {@code crystalGrowerColdThreshold} (int, 0): ambient heat at or above which the cold bonus is zero.
     */
    public static int crystalGrowerColdThreshold() {
        return NeroTechConfig.crystalGrowerColdThreshold();
    }

    /**
     * {@code crystalGrowerColdBonusPerHeatPermille} (int, 10, 0..1000): extra growth speed, in permille,
     * per point of ambient heat below the threshold.
     */
    public static int crystalGrowerColdBonusPerHeatPermille() {
        return NeroTechConfig.crystalGrowerColdBonusPerHeatPermille();
    }

    /**
     * {@code crystalGrowerColdBonusCapPermille} (int, 1000, 0..4000): the most the cold bonus can add, in
     * permille (1000 = twice as fast).
     */
    public static int crystalGrowerColdBonusCapPermille() {
        return NeroTechConfig.crystalGrowerColdBonusCapPermille();
    }
}
