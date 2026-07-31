package za.co.neroland.nerotech.compat;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;

import net.minecraft.world.level.Level;

import za.co.neroland.nerospace.api.Hazard;
import za.co.neroland.nerospace.api.NerospacePlanets;
import za.co.neroland.nerospace.api.PlanetId;
import za.co.neroland.nerospace.api.PlanetTraits;

/**
 * Optional bridge to Nerospace's semver-stable {@code za.co.neroland.nerospace.api} planet-trait
 * facade (Stage H). NeroTech compiles against the API ({@code compileOnly} on the
 * {@code za.co.neroland.nerospace:nerospace-<loader>-<mc>} artifact) but never requires it at
 * runtime: {@link #AVAILABLE} probes for the facade class <b>without initialising it</b>, and every
 * reference to an api type lives inside the nested {@link Holder}, which is only class-loaded once
 * {@code AVAILABLE} is true — so a NeroTech install without Nerospace can never trip a
 * {@code NoClassDefFoundError}. (The manifests list Nerospace as an optional, load-before
 * dependency, mirroring the Energized Power entries.)
 *
 * <p><b>Trait mapping</b> (the api exposes gravity / airless / hazard — no direct insolation or
 * surface-temperature trait, so both consumers derive from those; constants javadoc'd per use):
 *
 * <ul>
 *   <li><b>Solar multiplier</b> ({@code machine.PlanetModifiers}): base 1.0 for a known planet;
 *       ×1.25 when airless (no atmospheric attenuation — every current Nerospace body); then
 *       ×1.2 for a HEAT hazard (Cindara's intense insolation → 1.5 total) or ×0.5 for COLD
 *       (Glacira's distant, weak sun → 0.625 total). Greenxertz/Station land on 1.25.</li>
 *   <li><b>Thermal ambient</b> ({@code heat.ThermalEnvironment} dimension base): HEAT → 200,
 *       COLD → −80 (the exact figures the {@code thermalAmbientByDimension} config doc has always
 *       suggested for Cindara/Glacira), NONE → 0 (Greenxertz/Station sit at the temperate
 *       baseline; biome flavour still applies on top).</li>
 * </ul>
 *
 * <p><b>Precedence</b> (implemented at the consumers): api value when available and the dimension
 * is a known planet → existing per-dimension config fallback → default. Earth and any non-Nerospace
 * dimension always return empty here, so modpack config keeps full authority over them.
 */
public final class NerospacePlanetCompat {

    /** True when the Nerospace api facade is on the classpath (probed without initialising it). */
    public static final boolean AVAILABLE = detect();

    private NerospacePlanetCompat() {
    }

    private static boolean detect() {
        try {
            // initialize=false: probe only — never run Nerospace static init from the probe.
            Class.forName("za.co.neroland.nerospace.api.NerospacePlanets", false,
                    NerospacePlanetCompat.class.getClassLoader());
            return true;
        } catch (Throwable absent) {
            return false;
        }
    }

    /**
     * Solar output multiplier for {@code level}'s dimension from Nerospace planet traits, or empty
     * when Nerospace is absent or the dimension is not a Nerospace planet (Earth included).
     */
    public static OptionalDouble solarMultiplier(Level level) {
        return AVAILABLE ? Holder.solarMultiplier(level) : OptionalDouble.empty();
    }

    /**
     * Thermal ambient (heat units) for {@code level}'s dimension from Nerospace planet traits, or
     * empty when Nerospace is absent or the dimension is not a Nerospace planet.
     */
    public static OptionalInt thermalAmbient(Level level) {
        return AVAILABLE ? Holder.thermalAmbient(level) : OptionalInt.empty();
    }

    /**
     * Wind output multiplier for {@code level}'s dimension (Stage D): {@code 0} on an <b>airless</b>
     * planet — no atmosphere, no wind, and therefore no Wind Turbine output at all — and {@code 1}
     * on any other known Nerospace planet. Empty when Nerospace is absent or the dimension is not a
     * Nerospace planet (Earth included), so the config table keeps full authority there.
     */
    public static OptionalDouble windMultiplier(Level level) {
        return AVAILABLE ? Holder.windMultiplier(level) : OptionalDouble.empty();
    }

    /**
     * The only class that touches {@code nerospace.api} types — loaded lazily, and only behind an
     * {@link #AVAILABLE} check, so the api never needs to resolve when Nerospace is not installed.
     */
    private static final class Holder {

        /** Airless bonus: no atmosphere attenuating sunlight. */
        private static final double SOLAR_AIRLESS = 1.25D;
        /** HEAT-hazard factor (Cindara): close, intense insolation — 1.25 × 1.2 = 1.5 total. */
        private static final double SOLAR_HEAT = 1.2D;
        /** COLD-hazard factor (Glacira): distant, weak sun — 1.25 × 0.5 = 0.625 total. */
        private static final double SOLAR_COLD = 0.5D;

        /** HEAT-hazard ambient (Cindara) — the config doc's long-suggested figure. */
        private static final int AMBIENT_HEAT = 200;
        /** COLD-hazard ambient (Glacira) — the config doc's long-suggested figure. */
        private static final int AMBIENT_COLD = -80;

        private Holder() {
        }

        static OptionalDouble solarMultiplier(Level level) {
            Optional<PlanetId> planet = NerospacePlanets.byDimension(level.dimension());
            if (planet.isEmpty()) {
                return OptionalDouble.empty();
            }
            PlanetTraits traits = NerospacePlanets.traits(planet.get());
            double multiplier = 1.0D;
            if (traits.airless()) {
                multiplier *= SOLAR_AIRLESS;
            }
            multiplier *= switch (traits.hazard()) {
                case HEAT -> SOLAR_HEAT;
                case COLD -> SOLAR_COLD;
                default -> 1.0D; // future Hazard constants: no solar opinion
            };
            return OptionalDouble.of(multiplier);
        }

        static OptionalDouble windMultiplier(Level level) {
            Optional<PlanetId> planet = NerospacePlanets.byDimension(level.dimension());
            if (planet.isEmpty()) {
                return OptionalDouble.empty();
            }
            // Airless bodies have no atmosphere to move — a turbine there is inert, not merely weak.
            return OptionalDouble.of(NerospacePlanets.traits(planet.get()).airless() ? 0.0D : 1.0D);
        }

        static OptionalInt thermalAmbient(Level level) {
            Optional<PlanetId> planet = NerospacePlanets.byDimension(level.dimension());
            if (planet.isEmpty()) {
                return OptionalInt.empty();
            }
            Hazard hazard = NerospacePlanets.traits(planet.get()).hazard();
            return OptionalInt.of(switch (hazard) {
                case HEAT -> AMBIENT_HEAT;
                case COLD -> AMBIENT_COLD;
                default -> 0; // temperate baseline; biome flavour still applies on top
            });
        }
    }
}
