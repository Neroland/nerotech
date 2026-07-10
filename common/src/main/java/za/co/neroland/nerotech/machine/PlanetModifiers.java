package za.co.neroland.nerotech.machine;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalDouble;

import net.minecraft.world.level.Level;

import za.co.neroland.nerotech.compat.NerospacePlanetCompat;
import za.co.neroland.nerotech.config.NeroTechConfig;

/**
 * Per-planet generation modifiers (Stage H precedence — the Stage 4 "deferred" note is resolved):
 *
 * <ol>
 *   <li><b>Nerospace api</b> — when Nerospace is installed and the dimension is a known planet,
 *       {@link NerospacePlanetCompat#solarMultiplier} derives the multiplier from the planet's
 *       published traits (airless/hazard; mapping documented there). Runtime-guarded: NeroTech
 *       runs fully standalone without Nerospace.</li>
 *   <li><b>Config fallback</b> — otherwise the {@code solarDimensionMultipliers} comma-list
 *       ({@code dimensionId=multiplier}), keyed by dimension id.</li>
 *   <li><b>Default</b> — 1.0 (Earth tier plays untouched).</li>
 * </ol>
 */
public final class PlanetModifiers {

    private static volatile String parsedFrom;
    private static volatile Map<String, Double> cache = Map.of();

    private PlanetModifiers() {
    }

    /** Solar output multiplier for {@code level}'s dimension (api → config → 1.0). */
    public static double solarMultiplier(Level level) {
        OptionalDouble api = NerospacePlanetCompat.solarMultiplier(level);
        if (api.isPresent()) {
            return api.getAsDouble();
        }
        String dim = level.dimension().identifier().toString();
        return table().getOrDefault(dim, 1.0D);
    }

    private static Map<String, Double> table() {
        String raw = NeroTechConfig.solarDimensionMultipliers();
        if (raw.equals(parsedFrom)) {
            return cache;
        }
        Map<String, Double> parsed = new HashMap<>();
        for (String pair : raw.split(",")) {
            String entry = pair.trim();
            int eq = entry.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            try {
                parsed.put(entry.substring(0, eq).trim(), Double.parseDouble(entry.substring(eq + 1).trim()));
            } catch (NumberFormatException ignored) {
                // skip malformed entries
            }
        }
        cache = Map.copyOf(parsed);
        parsedFrom = raw;
        return cache;
    }
}
