package za.co.neroland.nerotech.machine.chemistry;

import za.co.neroland.nerotech.config.NeroTechConfig;

/**
 * The chemistry family's config keys, read in one place. Defaults are inline until the integrator
 * adds the keys to {@code NeroTechConfig} (names, types and comments are listed in the family's config
 * scratch note); then each method reads the config value instead.
 */
public final class ChemistrySettings {

    /** {@code condenserAtmospheres}: {@code dimensionId=ATMOSPHERE} entries used when Nerospace has no say. */
    public static final String CONDENSER_ATMOSPHERES_DEFAULT = "minecraft:the_nether=SULFUROUS,minecraft:the_end=VACUUM";

    private ChemistrySettings() {
    }

    /** Per-dimension condenser atmospheres; unlisted dimensions are EARTHLIKE. */
    public static String condenserAtmospheres() {
        return NeroTechConfig.condenserAtmospheres();
    }

    /** Cryo Distiller speed gained per point of ambient cold, in permille (5 = +0.5 % per heat below 0). */
    public static int cryoColdBonusPerHeatPermille() {
        return NeroTechConfig.cryoColdBonusPerHeatPermille();
    }

    /** Cryo Distiller speed floor in hot ambient, permille of the recipe rate. */
    public static int cryoSpeedMinPermille() {
        return NeroTechConfig.cryoSpeedMinPermille();
    }

    /** Cryo Distiller speed cap in cold ambient, permille of the recipe rate. */
    public static int cryoSpeedMaxPermille() {
        return NeroTechConfig.cryoSpeedMaxPermille();
    }

    /** Gas (mB) a Centrifuge pushes into the next one in its cascade per tick. */
    public static int centrifugeCascadePushPerTick() {
        return NeroTechConfig.centrifugeCascadePushPerTick();
    }
}
