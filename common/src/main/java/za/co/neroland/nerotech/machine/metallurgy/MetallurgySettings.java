package za.co.neroland.nerotech.machine.metallurgy;

import za.co.neroland.nerotech.config.NeroTechConfig;

/**
 * Tunables of the metallurgy family. Defaults inline for now; the integrator moves them into
 * {@code NeroTechConfig} under the same names (see the family's config list).
 */
public final class MetallurgySettings {

    private MetallurgySettings() {
    }

    /** NE per tick the Plasma Crucible spends while firing its arc to reach a recipe's minimum heat. */
    public static int plasmaCrucibleFiringEnergy() {
        return NeroTechConfig.plasmaCrucibleFiringEnergy();
    }

    /** Heat one firing tick adds. */
    public static int plasmaCrucibleFiringHeat() {
        return NeroTechConfig.plasmaCrucibleFiringHeat();
    }

    /** While idle the crucible exchanges heat with its surroundings once every this many ticks (1 = never insulated). */
    public static int plasmaCrucibleHoldIntervalTicks() {
        return NeroTechConfig.plasmaCrucibleHoldIntervalTicks();
    }
}
