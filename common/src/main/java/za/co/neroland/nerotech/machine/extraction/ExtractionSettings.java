package za.co.neroland.nerotech.machine.extraction;

import za.co.neroland.nerotech.config.NeroTechConfig;

/**
 * Tunables of the extraction family. Defaults inline for now; the integrator moves them into
 * {@code NeroTechConfig} under the same names (see the family's config list). The deposit keys
 * ({@code depositYieldPermille}, {@code depositDepletionEnabled}, {@code depositRecoveryPerDay},
 * {@code depositDisabledDimensions}) already live in {@code NeroTechConfig} and are read from there.
 */
public final class ExtractionSettings {

    private ExtractionSettings() {
    }

    /** Ticks one Deep Core Drill operation takes at 1x speed; one deposit read per operation. */
    public static int drillOperationTicks() {
        return NeroTechConfig.drillOperationTicks();
    }

    /** NE per tick the drill draws while it works (before modules and preset). */
    public static int drillEnergyPerTick() {
        return NeroTechConfig.drillEnergyPerTick();
    }

    /** Ticks a drill waits before it tries a depleted or missing deposit again. */
    public static int drillDepletedRetryTicks() {
        return NeroTechConfig.drillDepletedRetryTicks();
    }

    /** NE the Survey Scanner holds. */
    public static int surveyScannerCapacity() {
        return NeroTechConfig.surveyScannerCapacity();
    }

    /** NE one survey costs. */
    public static int surveyScannerScanCost() {
        return NeroTechConfig.surveyScannerScanCost();
    }

    /** Ticks between two surveys with the same scanner. */
    public static int surveyScannerCooldownTicks() {
        return NeroTechConfig.surveyScannerCooldownTicks();
    }
}
