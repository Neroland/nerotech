package za.co.neroland.nerotech.machine;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.world.level.Level;

import za.co.neroland.nerotech.config.NeroTechConfig;

/**
 * Per-planet generation modifiers — the <b>deferred fallback</b> for Stage 4. A published
 * {@code nerospace.api} (planet-trait query) is the intended source; until it exists, NeroTech reads
 * per-dimension multipliers from Core config ({@code solarDimensionMultipliers}), keyed by dimension id.
 * No Nerospace import, so Earth tier (overworld → default 1.0) plays fully standalone.
 */
public final class PlanetModifiers {

    private static volatile String parsedFrom;
    private static volatile Map<String, Double> cache = Map.of();

    private PlanetModifiers() {
    }

    /** Solar output multiplier for {@code level}'s dimension (1.0 if unset). */
    public static double solarMultiplier(Level level) {
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
