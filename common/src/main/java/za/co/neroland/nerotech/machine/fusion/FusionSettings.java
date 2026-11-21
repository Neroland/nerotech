package za.co.neroland.nerotech.machine.fusion;

import za.co.neroland.nerotech.config.NeroTechConfig;

/**
 * Fusion balance values, read in one place. The defaults are inline until the integrator moves them
 * into {@code NeroTechConfig} (keys and comments listed in the family's config note); every caller reads
 * them through these methods, so the move touches only this class.
 */
public final class FusionSettings {

    private FusionSettings() {
    }

    /** {@code fusionGasMinShellSize}: smallest shell edge that can contain the gas-fed burn (7 = the maximal shell). */
    public static int gasMinShellSize() {
        return NeroTechConfig.fusionGasMinShellSize();
    }

    /** {@code fusionGasChargeTicks}: ticks of burn bought per gas draw (gas is drawn per charge, never per tick). */
    public static int gasChargeTicks() {
        return NeroTechConfig.fusionGasChargeTicks();
    }

    /** {@code fusionGasDeuteriumPerTick}: mB of deuterium burned per tick of gas-fed burn. */
    public static int deuteriumPerTick() {
        return NeroTechConfig.fusionGasDeuteriumPerTick();
    }

    /** {@code fusionGasHelium3PerTick}: mB of helium-3 burned per tick of gas-fed burn. */
    public static int helium3PerTick() {
        return NeroTechConfig.fusionGasHelium3PerTick();
    }

    /** {@code fusionGasOutputPermille}: output of a gas-fed burn against a fuel-cell burn of the same shell. */
    public static int gasOutputPermille() {
        return NeroTechConfig.fusionGasOutputPermille();
    }

    /** {@code fuelInjectorGasCapacity}: mB each of the injector's two tanks holds. */
    public static long injectorGasCapacity() {
        return NeroTechConfig.fuelInjectorGasCapacity();
    }

    /** {@code fusionLatticeHeatReductionPermille}: heat gain removed per counted Europium Flux Lattice. */
    public static int latticeHeatReductionPermille() {
        return NeroTechConfig.fusionLatticeHeatReductionPermille();
    }

    /** {@code fusionLatticeMaxCount}: lattices beyond this many in one shell add nothing. */
    public static int latticeMaxCount() {
        return NeroTechConfig.fusionLatticeMaxCount();
    }
}
