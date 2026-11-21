package za.co.neroland.nerotech.machine.utility;

import za.co.neroland.nerotech.config.NeroTechConfig;

/**
 * Tunables of the utility family (Induction Charger, Annunciator Panel). Defaults inline for now; the
 * integrator moves them into {@code NeroTechConfig} under the same names (see the family's config list)
 * and points these methods at them.
 */
public final class UtilitySettings {

    private UtilitySettings() {
    }

    /**
     * Whether the Induction Charger charges energy items held or worn by players standing in range.
     * Proximity only; nothing about a player is stored. Off leaves the entity charging API working.
     */
    public static boolean inductionChargePlayers() {
        return NeroTechConfig.inductionChargePlayers();
    }

    /** Reach of an Induction Charger, in blocks, on every axis (a bounded box around the charger). */
    public static int inductionChargerRadius() {
        return NeroTechConfig.inductionChargerRadius();
    }

    /**
     * NE one charger may hand out per tick, before upgrades and the overclock preset. Player charging
     * and the entity charging API share it; unused allowance banks for at most one pass (20 ticks).
     */
    public static int inductionChargerTransferPerTick() {
        return NeroTechConfig.inductionChargerTransferPerTick();
    }

    /** Whether an Annunciator Panel plays its alarm sound once when a new alarm lights. */
    public static boolean annunciatorAlarmSound() {
        return NeroTechConfig.annunciatorAlarmSound();
    }

    /** How often (ticks) an Annunciator Panel re-reads the alarms of the machines touching it. */
    public static int annunciatorRefreshTicks() {
        return NeroTechConfig.annunciatorRefreshTicks();
    }
}
