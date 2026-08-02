package za.co.neroland.nerotech.gas;

import java.util.HashMap;
import java.util.Map;

/**
 * Pure parser for the {@code turbineGasBurn} config map — a comma-list of
 * {@code gasId=burnMultiplier} pairs (the same shape as {@code thermalAmbientByDimension} and
 * {@code solarDimensionMultipliers}). Deliberately free of any {@code net.minecraft} import so the
 * parse rules are unit-testable on the plain JVM (see
 * {@code common/src/test/java/za/co/neroland/nerotech/gas/GasFuelMapTest.java}); the caching,
 * config-backed lookup lives in {@link TurbineFuels}.
 *
 * <p>Malformed entries are skipped rather than failing the whole list, and non-positive
 * multipliers are dropped (a fuel that yields nothing is not a fuel).
 */
public final class GasFuelMap {

    private GasFuelMap() {
    }

    /** Parse {@code gasId=multiplier,...} into an immutable map; never null, never throws. */
    public static Map<String, Integer> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return Map.of();
        }
        Map<String, Integer> parsed = new HashMap<>();
        for (String pair : raw.split(",")) {
            String entry = pair.trim();
            int eq = entry.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            String key = entry.substring(0, eq).trim();
            if (key.isEmpty()) {
                continue;
            }
            try {
                int multiplier = Integer.parseInt(entry.substring(eq + 1).trim());
                if (multiplier > 0) {
                    parsed.put(key, multiplier);
                }
            } catch (NumberFormatException ignored) {
                // Malformed entries are skipped; the rest of the list still applies.
            }
        }
        return Map.copyOf(parsed);
    }
}
