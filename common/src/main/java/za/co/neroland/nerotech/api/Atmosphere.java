package za.co.neroland.nerotech.api;

/**
 * The air an Atmospheric Condenser can draw at a place, as {@link PlanetApi#atmosphereAt} reports it.
 * A NeroTech enum, so add-ons never need a Nerospace class to read it; the names are the
 * {@code atmosphere} values {@code nerotech:condensing} recipes match on and the values of the
 * {@code condenserAtmospheres} config table. {@link #VACUUM} means there is nothing to condense.
 * Constants may be added in later releases; never renamed.
 */
public enum Atmosphere {
    /** No air: orbital stations, barren moons, Europa's surface, the End. */
    VACUUM,
    /** Thin carbon dioxide (Mars-like deserts). */
    THIN_CO2,
    /** Volcanic air: carbon dioxide with a sulfur trace (Cindara, the Nether). */
    SULFUROUS,
    /** Cold nitrogen with methane (Glacira-like frozen worlds). */
    NITROGEN_METHANE,
    /** Nitrogen and oxygen (Greenxertz-like jungles; Nyxara at half rate). */
    NITROGEN_OXYGEN,
    /** The overworld's air: nitrogen with a carbon dioxide trace. The default everywhere else. */
    EARTHLIKE;

    /** Whether there is any air here for the condenser to draw (true for everything but a vacuum). */
    public boolean condensable() {
        return this != VACUUM;
    }

    /**
     * Same as {@link #condensable()}. The name was misleading (sulfurous and thin CO2 air are not
     * breathable, yet the condenser draws them); kept so nothing that already calls it breaks.
     *
     * @deprecated use {@link #condensable()}
     */
    @Deprecated
    public boolean breathable() {
        return condensable();
    }
}
