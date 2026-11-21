package za.co.neroland.nerotech.machine.chemistry;

import java.util.Map;

import net.minecraft.world.level.Level;

import za.co.neroland.nerotech.api.Atmosphere;
import za.co.neroland.nerotech.compat.NerospacePlanetCompat;

/**
 * Live side of {@link AtmosphereMapping}: Nerospace traits through {@link NerospacePlanetCompat}, then
 * the {@code condenserAtmospheres} table (re-parsed only when its text changes), then EARTHLIKE. One
 * reflective trait read per call, so callers cache the result (the condenser refreshes every 200 ticks).
 */
public final class AtmosphereLookup {

    private static volatile String rawTable;
    private static volatile Map<String, Atmosphere> table = Map.of();

    private AtmosphereLookup() {
    }

    /** What a condenser reads in {@code level}'s dimension. */
    public static AtmosphereMapping.Reading readingAt(Level level) {
        String dimension = level.dimension().identifier().toString();
        return AtmosphereMapping.resolve(NerospacePlanetCompat.atmosphere(level), dimension, table());
    }

    private static Map<String, Atmosphere> table() {
        String raw = ChemistrySettings.condenserAtmospheres();
        if (!raw.equals(rawTable)) {
            table = AtmosphereMapping.parseTable(raw);
            rawTable = raw;
        }
        return table;
    }
}
