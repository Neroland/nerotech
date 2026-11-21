package za.co.neroland.nerotech.machine.heat;

import za.co.neroland.nerotech.config.NeroTechConfig;

/**
 * Heat config, defaults inline until the integrator moves these keys into {@code NeroTechConfig}
 * (same names; see the family's config sheet). Every value is read per use, so a hot reload applies.
 */
public final class HeatSettings {

    private HeatSettings() {
    }

    /** Most pipes one run resolves (the flood-fill cap). */
    public static int heatPipeMaxRun() {
        return NeroTechConfig.heatPipeMaxRun();
    }

    /** Permille of each endpoint-vs-run temperature difference a Heat Pipe run moves per exchange. */
    public static int heatPipeConductivityPermille() {
        return NeroTechConfig.heatPipeConductivityPermille();
    }

    /** Permille of each pipe's heat above ambient lost per exchange (long runs carry less). */
    public static int heatPipeLossPermille() {
        return NeroTechConfig.heatPipeLossPermille();
    }

    /** Ticks between a run's exchanges. */
    public static int heatPipeExchangeIntervalTicks() {
        return NeroTechConfig.heatPipeExchangeIntervalTicks();
    }

    /** Whether a pipe at heat level 2 or more hurts entities standing on it. */
    public static boolean heatPipeContactDamage() {
        return NeroTechConfig.heatPipeContactDamage();
    }

    /** Damage per contact hit (half a heart per point). */
    public static float heatPipeContactDamageAmount() {
        return (float) NeroTechConfig.heatPipeContactDamageAmount();
    }

    /** Most heat one Heat Exchanger exchange moves (before Speed modules and the overclock preset). */
    public static int heatExchangerMaxPerOp() {
        return NeroTechConfig.heatExchangerMaxPerOp();
    }

    /** Permille of the machine-vs-tank difference a Heat Exchanger moves per exchange for a storing medium. */
    public static int heatExchangerConductivityPermille() {
        return NeroTechConfig.heatExchangerConductivityPermille();
    }

    /** Coolant media: {@code id=heatPerMb}. Absorb machine heat and are used up. */
    public static String heatExchangerCoolants() {
        return NeroTechConfig.heatExchangerCoolants();
    }

    /** Hot media: {@code id=heatPerMb}. Give heat to the machine and are used up. */
    public static String heatExchangerHotMedia() {
        return NeroTechConfig.heatExchangerHotMedia();
    }

    /** Temperature a hot medium heats a machine up to. */
    public static int heatExchangerHotTemperature() {
        return NeroTechConfig.heatExchangerHotTemperature();
    }
}
