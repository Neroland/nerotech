package za.co.neroland.nerotech.heat;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import za.co.neroland.nerotech.compat.NerospacePlanetCompat;
import za.co.neroland.nerotech.config.NeroTechConfig;

/**
 * Ambient heat level for a machine's location — the temperature the thermal model relaxes every
 * machine toward (see {@link ThermalMath#ambientStep}). Two ingredients, both cheap:
 *
 * <ul>
 *   <li><b>Dimension base</b> (Stage H precedence — the old "deferred" note is resolved):
 *       Nerospace's {@code nerospace.api} planet traits via
 *       {@link NerospacePlanetCompat#thermalAmbient} when Nerospace is installed and the dimension
 *       is a known planet (Cindara hot, Glacira sub-zero; runtime-guarded so NeroTech stays fully
 *       standalone) → else the {@code thermalAmbientByDimension} config (comma-list of
 *       {@code dimensionId=heat}, same format and parse-and-cache pattern as
 *       {@code solarDimensionMultipliers} / {@code machine.PlanetModifiers}) → else
 *       {@code thermalAmbientDefault}.</li>
 *   <li><b>Biome flavour</b> — {@code (baseTemperature - 0.8) * thermalBiomeScale}, so vanilla
 *       plains (0.8) is neutral, deserts/nether (2.0) run hot and snowy biomes (≤0) run cold.
 *       Uses only the biome's static base temperature — one holder lookup, no per-tick noise.</li>
 * </ul>
 *
 * <p>Callers cache the result per block-entity and refresh it on an interval (see
 * {@code NeroTechMachineBlockEntity}); this class only caches the parsed dimension table.
 */
public final class ThermalEnvironment {

    /** Vanilla "temperate" biome base temperature — plains; treated as ambient-neutral. */
    private static final float NEUTRAL_BIOME_TEMPERATURE = 0.8F;

    private static volatile String parsedFrom;
    private static volatile Map<String, Integer> cache = Map.of();

    private ThermalEnvironment() {
    }

    /** Ambient heat at {@code pos} in {@code level}: dimension base (api → config) + biome flavour. */
    public static int ambientAt(Level level, BlockPos pos) {
        OptionalInt api = NerospacePlanetCompat.thermalAmbient(level);
        int base;
        if (api.isPresent()) {
            base = api.getAsInt();
        } else {
            String dim = level.dimension().identifier().toString();
            base = table().getOrDefault(dim, NeroTechConfig.thermalAmbientDefault());
        }
        float biomeTemperature = level.getBiome(pos).value().getBaseTemperature();
        int biome = Math.round((biomeTemperature - NEUTRAL_BIOME_TEMPERATURE) * NeroTechConfig.thermalBiomeScale());
        return base + biome;
    }

    private static Map<String, Integer> table() {
        String raw = NeroTechConfig.thermalAmbientByDimension();
        if (raw.equals(parsedFrom)) {
            return cache;
        }
        Map<String, Integer> parsed = new HashMap<>();
        for (String pair : raw.split(",")) {
            String entry = pair.trim();
            int eq = entry.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            try {
                parsed.put(entry.substring(0, eq).trim(), Integer.parseInt(entry.substring(eq + 1).trim()));
            } catch (NumberFormatException ignored) {
                // Malformed entries are skipped; the rest of the list still applies.
            }
        }
        cache = Map.copyOf(parsed);
        parsedFrom = raw;
        return cache;
    }
}
