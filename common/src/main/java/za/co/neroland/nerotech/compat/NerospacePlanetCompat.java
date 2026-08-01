package za.co.neroland.nerotech.compat;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerotech.NeroTechCommon;

/**
 * Optional bridge to Nerospace's semver-stable {@code za.co.neroland.nerospace.api} planet-trait
 * facade — <b>pure reflection, zero build-time dependency</b>. NeroTech's build never touches a
 * Nerospace artifact: the facade is resolved reflectively at class-init ({@link Api#resolve()}),
 * and when Nerospace is absent (or the api shape ever drifts) every method here returns empty and
 * the per-dimension config tables keep full authority. (The manifests still list Nerospace as an
 * optional, load-before dependency, mirroring the Energized Power entries — load-order metadata
 * only, never a requirement.)
 *
 * <p>Api surface consumed (resolved by name, invoked via cached {@link Method}s):
 * {@code NerospacePlanets.byDimension(ResourceKey)} → {@code Optional<PlanetId>},
 * {@code NerospacePlanets.traits(PlanetId)} → {@code PlanetTraits},
 * {@code PlanetTraits.airless()} → {@code boolean}, {@code PlanetTraits.hazard()} → enum
 * (compared by constant name, so new Hazard constants are simply "no opinion").
 *
 * <p><b>Trait mapping</b> (the api exposes gravity / airless / hazard — no direct insolation or
 * surface-temperature trait, so consumers derive from those; constants javadoc'd per use):
 *
 * <ul>
 *   <li><b>Solar multiplier</b> ({@code machine.PlanetModifiers}): base 1.0 for a known planet;
 *       ×1.25 when airless (no atmospheric attenuation — every current Nerospace body); then
 *       ×1.2 for a HEAT hazard (Cindara's intense insolation → 1.5 total) or ×0.5 for COLD
 *       (Glacira's distant, weak sun → 0.625 total). Greenxertz/Station land on 1.25.</li>
 *   <li><b>Wind multiplier</b> ({@code machine.WindTurbineBlockEntity}): 0 on an airless planet
 *       (no atmosphere, no wind), 1 on any other known planet.</li>
 *   <li><b>Thermal ambient</b> ({@code heat.ThermalEnvironment} dimension base): HEAT → 200,
 *       COLD → −80 (the exact figures the {@code thermalAmbientByDimension} config doc has always
 *       suggested for Cindara/Glacira), NONE → 0 (biome flavour still applies on top).</li>
 * </ul>
 *
 * <p><b>Precedence</b> (implemented at the consumers): api value when available and the dimension
 * is a known planet → existing per-dimension config fallback → default. Earth and any non-Nerospace
 * dimension always return empty here, so modpack config keeps full authority over them.
 */
public final class NerospacePlanetCompat {

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

    @Nullable
    private static final Api API = Api.resolve();

    /** True when the Nerospace api facade resolved reflectively (probed without initialising it). */
    public static final boolean AVAILABLE = API != null;

    private NerospacePlanetCompat() {
    }

    /**
     * Solar output multiplier for {@code level}'s dimension from Nerospace planet traits, or empty
     * when Nerospace is absent or the dimension is not a Nerospace planet (Earth included).
     */
    public static OptionalDouble solarMultiplier(Level level) {
        Api api = API;
        if (api == null) {
            return OptionalDouble.empty();
        }
        Object traits = api.traitsFor(level.dimension());
        if (traits == null) {
            return OptionalDouble.empty();
        }
        double multiplier = 1.0D;
        if (api.airless(traits)) {
            multiplier *= SOLAR_AIRLESS;
        }
        multiplier *= switch (api.hazardName(traits)) {
            case "HEAT" -> SOLAR_HEAT;
            case "COLD" -> SOLAR_COLD;
            default -> 1.0D; // future Hazard constants: no solar opinion
        };
        return OptionalDouble.of(multiplier);
    }

    /**
     * Wind output multiplier for {@code level}'s dimension (Stage D): {@code 0} on an <b>airless</b>
     * planet — no atmosphere, no wind, and therefore no Wind Turbine output at all — and {@code 1}
     * on any other known Nerospace planet. Empty when Nerospace is absent or the dimension is not a
     * Nerospace planet (Earth included), so the config table keeps full authority there.
     */
    public static OptionalDouble windMultiplier(Level level) {
        Api api = API;
        if (api == null) {
            return OptionalDouble.empty();
        }
        Object traits = api.traitsFor(level.dimension());
        if (traits == null) {
            return OptionalDouble.empty();
        }
        // Airless bodies have no atmosphere to move — a turbine there is inert, not merely weak.
        return OptionalDouble.of(api.airless(traits) ? 0.0D : 1.0D);
    }

    /**
     * Thermal ambient (heat units) for {@code level}'s dimension from Nerospace planet traits, or
     * empty when Nerospace is absent or the dimension is not a Nerospace planet.
     */
    public static OptionalInt thermalAmbient(Level level) {
        Api api = API;
        if (api == null) {
            return OptionalInt.empty();
        }
        Object traits = api.traitsFor(level.dimension());
        if (traits == null) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(switch (api.hazardName(traits)) {
            case "HEAT" -> AMBIENT_HEAT;
            case "COLD" -> AMBIENT_COLD;
            default -> 0; // temperate baseline; biome flavour still applies on top
        });
    }

    /**
     * The cached reflective handles onto the api facade. Resolution never initialises Nerospace
     * classes ({@code Class.forName(..., initialize=false, ...)}); initialisation happens on the
     * first real call, by which point mod loading is complete. Any resolution failure — class
     * absent, method renamed, signature drift — yields {@code null} and the compat degrades to the
     * config tables, logged once at debug level (an absent optional mod is normal, never an error).
     */
    private record Api(Method byDimension, Method traits, Method airless, Method hazard) {

        @Nullable
        static Api resolve() {
            try {
                ClassLoader loader = NerospacePlanetCompat.class.getClassLoader();
                Class<?> planets = Class.forName("za.co.neroland.nerospace.api.NerospacePlanets", false, loader);
                Class<?> planetId = Class.forName("za.co.neroland.nerospace.api.PlanetId", false, loader);
                Class<?> planetTraits = Class.forName("za.co.neroland.nerospace.api.PlanetTraits", false, loader);
                Method byDimension = planets.getMethod("byDimension", ResourceKey.class);
                Method traits = planets.getMethod("traits", planetId);
                Method airless = planetTraits.getMethod("airless");
                Method hazard = planetTraits.getMethod("hazard");
                return new Api(byDimension, traits, airless, hazard);
            } catch (ClassNotFoundException absent) {
                return null; // Nerospace not installed — the normal standalone case, not an error
            } catch (ReflectiveOperationException | RuntimeException drift) {
                // Present but unrecognisable (api drift): degrade to config, note it once for packs.
                NeroTechCommon.LOGGER.debug("[NeroTech] Nerospace present but planet api unresolvable"
                        + " — using per-dimension config tables", drift);
                return null;
            }
        }

        /** {@code PlanetTraits} for the dimension, or {@code null} when it is not a known planet. */
        @Nullable
        Object traitsFor(ResourceKey<Level> dimension) {
            try {
                Optional<?> planet = (Optional<?>) byDimension.invoke(null, dimension);
                if (planet.isEmpty()) {
                    return null;
                }
                return traits.invoke(null, planet.get());
            } catch (ReflectiveOperationException | RuntimeException unexpected) {
                return null; // fail soft: config tables keep authority
            }
        }

        boolean airless(Object planetTraits) {
            try {
                return (Boolean) airless.invoke(planetTraits);
            } catch (ReflectiveOperationException | RuntimeException unexpected) {
                return false;
            }
        }

        /** The hazard enum constant's name, or {@code ""} when unreadable (→ no opinion). */
        String hazardName(Object planetTraits) {
            try {
                Object value = hazard.invoke(planetTraits);
                return value instanceof Enum<?> constant ? constant.name() : "";
            } catch (ReflectiveOperationException | RuntimeException unexpected) {
                return "";
            }
        }
    }
}
