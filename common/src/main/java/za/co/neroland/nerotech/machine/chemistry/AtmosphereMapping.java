package za.co.neroland.nerotech.machine.chemistry;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import za.co.neroland.nerotech.api.Atmosphere;

/**
 * Pure mapping from a place to the {@link Atmosphere} an Atmospheric Condenser draws there
 * (machine contract 006 §5). Strings only — no Minecraft or Nerospace class — so the rule test runs on a
 * bare JVM.
 *
 * <p>Precedence, implemented by {@link #resolve}: the Nerospace planet traits (archetype, then the
 * temperature band for an archetype this release does not know), with Europa overridden to
 * {@link Atmosphere#VACUUM} by dimension id because its surface is airless → the
 * {@code condenserAtmospheres} config table ({@code dimensionId=ATMOSPHERE}, comma-separated) →
 * {@link Atmosphere#EARTHLIKE}.
 */
public final class AtmosphereMapping {

    /** Europa's dimension id: a FROZEN world in the traits, but its surface has no air. */
    public static final String EUROPA = "nerospace:europa";

    /** Rate factor on Nyxara-like twilight worlds (nitrogen-oxygen air at half rate). */
    public static final double TWILIGHT_RATE = 0.5D;

    /** What the condenser reads at a place and how fast it draws it (1.0 = recipe rate). */
    public record Reading(Atmosphere atmosphere, double rate) {
        public Reading {
            atmosphere = atmosphere == null ? Atmosphere.EARTHLIKE : atmosphere;
            rate = Double.isFinite(rate) ? Math.max(0.0D, rate) : 1.0D;
        }

        public static Reading of(Atmosphere atmosphere) {
            return new Reading(atmosphere, 1.0D);
        }
    }

    private AtmosphereMapping() {
    }

    /**
     * The reading for a known Nerospace planet from its trait names, or empty when the traits say
     * nothing this release understands (the config table then decides).
     *
     * @param dimensionId     the dimension id ({@code nerospace:europa} overrides to vacuum)
     * @param archetype       {@code PlanetArchetype} constant name ({@code ""} when unreadable)
     * @param temperatureBand {@code TemperatureBand} constant name ({@code ""} when unreadable)
     */
    public static Optional<Reading> fromTraits(String dimensionId, String archetype, String temperatureBand) {
        if (EUROPA.equals(dimensionId)) {
            return Optional.of(Reading.of(Atmosphere.VACUUM));
        }
        String arch = archetype == null ? "" : archetype;
        switch (arch) {
            case "ORBITAL", "BARREN_MOON":
                return Optional.of(Reading.of(Atmosphere.VACUUM));
            case "DESERT":
                return Optional.of(Reading.of(Atmosphere.THIN_CO2));
            case "VOLCANIC":
                return Optional.of(Reading.of(Atmosphere.SULFUROUS));
            case "FROZEN":
                return Optional.of(Reading.of(Atmosphere.NITROGEN_METHANE));
            case "JUNGLE":
                return Optional.of(Reading.of(Atmosphere.NITROGEN_OXYGEN));
            case "TWILIGHT":
                return Optional.of(new Reading(Atmosphere.NITROGEN_OXYGEN, TWILIGHT_RATE));
            default:
                break;
        }
        // An archetype added after this release: fall back on the extreme temperature bands only.
        String band = temperatureBand == null ? "" : temperatureBand;
        return switch (band) {
            case "INFERNAL" -> Optional.of(Reading.of(Atmosphere.SULFUROUS));
            case "FRIGID" -> Optional.of(Reading.of(Atmosphere.NITROGEN_METHANE));
            default -> Optional.empty();
        };
    }

    /**
     * Parse the {@code condenserAtmospheres} table. Entries that do not name a known
     * {@link Atmosphere} (case-insensitive) are skipped, never fatal.
     */
    public static Map<String, Atmosphere> parseTable(String raw) {
        Map<String, Atmosphere> table = new HashMap<>();
        if (raw == null || raw.isBlank()) {
            return table;
        }
        for (String entry : raw.split(",")) {
            int eq = entry.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            String dimension = entry.substring(0, eq).trim();
            String name = entry.substring(eq + 1).trim().toUpperCase(Locale.ROOT);
            if (dimension.isEmpty()) {
                continue;
            }
            for (Atmosphere atmosphere : Atmosphere.values()) {
                if (atmosphere.name().equals(name)) {
                    table.put(dimension, atmosphere);
                    break;
                }
            }
        }
        return table;
    }

    /** Planet traits (when present) → config table → {@link Atmosphere#EARTHLIKE}. */
    public static Reading resolve(Optional<Reading> fromTraits, String dimensionId, Map<String, Atmosphere> table) {
        if (fromTraits != null && fromTraits.isPresent()) {
            return fromTraits.get();
        }
        Atmosphere configured = table == null ? null : table.get(dimensionId);
        return Reading.of(configured == null ? Atmosphere.EARTHLIKE : configured);
    }
}
