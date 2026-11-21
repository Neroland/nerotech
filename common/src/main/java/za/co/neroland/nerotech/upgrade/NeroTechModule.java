package za.co.neroland.nerotech.upgrade;

/**
 * Upgrade modules NeroTech adds on top of Core's upgrade framework. Core's
 * {@link za.co.neroland.nerolandcore.upgrade.UpgradeType} is a closed enum (speed, efficiency, range,
 * capacity), so these two are classified by NeroTech, not by Core: the card is still an
 * {@link UpgradeModuleItem} sitting in a machine's Core {@code UpgradeContainer} slots, Core's
 * classifier reports {@code null} for it (so Core's {@code UpgradeModifiers} never counts it), and
 * NeroTech's machine base reads its effect through {@link ModuleMath}. Core is not changed.
 */
public enum NeroTechModule {

    /** Widens the safe heat band: the throttle threshold moves up toward the heat ceiling. */
    HEAT_SINK,

    /** Less pollution per operation. */
    SCRUBBER
}
