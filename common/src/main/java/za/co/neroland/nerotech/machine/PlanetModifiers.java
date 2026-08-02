package za.co.neroland.nerotech.machine;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.function.Supplier;

import net.minecraft.world.level.Level;

import za.co.neroland.nerotech.compat.NerospacePlanetCompat;
import za.co.neroland.nerotech.config.NeroTechConfig;

/**
 * Per-planet generation modifiers (Stage H precedence — the Stage 4 "deferred" note is resolved):
 *
 * <ol>
 *   <li><b>Nerospace api</b> — when Nerospace is installed and the dimension is a known planet,
 *       {@link NerospacePlanetCompat} derives the multiplier from the planet's published traits
 *       (airless/hazard; mapping documented there). Runtime-guarded: NeroTech runs fully standalone
 *       without Nerospace.</li>
 *   <li><b>Config fallback</b> — otherwise the matching comma-list config
 *       ({@code dimensionId=multiplier}), keyed by dimension id.</li>
 *   <li><b>Default</b> — 1.0 (Earth tier plays untouched).</li>
 * </ol>
 */
public final class PlanetModifiers {

    /** Solar output table ({@code solarDimensionMultipliers}). */
    private static final Table SOLAR = new Table(NeroTechConfig::solarDimensionMultipliers);

    /** Wind output table ({@code windDimensionMultipliers}, Stage D). */
    private static final Table WIND = new Table(NeroTechConfig::windDimensionMultipliers);

    private PlanetModifiers() {
    }

    /** Solar output multiplier for {@code level}'s dimension (api → config → 1.0). */
    public static double solarMultiplier(Level level) {
        return resolve(NerospacePlanetCompat.solarMultiplier(level), SOLAR, level);
    }

    /**
     * Wind output multiplier for {@code level}'s dimension (api → config → 1.0). The Nerospace hook
     * returns 0 for an airless planet, so a turbine on the Moon-likes is inert no matter what the
     * config table says — the table only ever governs Earth and non-Nerospace dimensions.
     */
    public static double windMultiplier(Level level) {
        return resolve(NerospacePlanetCompat.windMultiplier(level), WIND, level);
    }

    private static double resolve(OptionalDouble api, Table table, Level level) {
        if (api.isPresent()) {
            return api.getAsDouble();
        }
        return table.get(level.dimension().identifier().toString());
    }

    /** A lazily parsed {@code dimensionId=multiplier} comma-list, re-parsed only when the raw text changes. */
    private static final class Table {

        private final Supplier<String> source;
        private volatile String parsedFrom;
        private volatile Map<String, Double> cache = Map.of();

        Table(Supplier<String> source) {
            this.source = source;
        }

        double get(String dimension) {
            return parsed().getOrDefault(dimension, 1.0D);
        }

        private Map<String, Double> parsed() {
            String raw = this.source.get();
            if (raw.equals(this.parsedFrom)) {
                return this.cache;
            }
            Map<String, Double> parsed = new HashMap<>();
            for (String pair : raw.split(",")) {
                String entry = pair.trim();
                int eq = entry.indexOf('=');
                if (eq <= 0) {
                    continue;
                }
                try {
                    parsed.put(entry.substring(0, eq).trim(),
                            Double.parseDouble(entry.substring(eq + 1).trim()));
                } catch (NumberFormatException ignored) {
                    // skip malformed entries
                }
            }
            this.cache = Map.copyOf(parsed);
            this.parsedFrom = raw;
            return this.cache;
        }
    }
}
