package za.co.neroland.nerotech.api;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import za.co.neroland.nerotech.heat.ThermalEnvironment;
import za.co.neroland.nerotech.machine.PlanetModifiers;

/**
 * The per-planet environment NeroTech's own generators and thermal model run on, re-exported for
 * add-ons so a NeroPower solar tower or wind farm scales <i>exactly</i> like NeroTech's Solar Array
 * and Wind Turbine — same Nerospace planet traits when Nerospace is installed, same
 * {@code solarDimensionMultipliers} / {@code windDimensionMultipliers} /
 * {@code thermalAmbientByDimension} config fallbacks when it is not, same 1.0 / default on Earth.
 *
 * <p>Precedence for every method: Nerospace api (runtime-guarded, optional) → NeroTech config table
 * keyed by dimension id → default. Cheap enough to call per tick for the multipliers (a parsed
 * table lookup); {@link #ambientAt} does one biome holder lookup, so cache it per block entity and
 * refresh on an interval the way {@code NeroTechMachineBlockEntity} does.
 */
public final class PlanetApi {

    private PlanetApi() {
    }

    /** Solar output multiplier for {@code level}'s dimension (api → config → 1.0). */
    public static double solarMultiplier(Level level) {
        return PlanetModifiers.solarMultiplier(level);
    }

    /**
     * Wind output multiplier for {@code level}'s dimension (api → config → 1.0). Zero on an airless
     * Nerospace planet regardless of the config table.
     */
    public static double windMultiplier(Level level) {
        return PlanetModifiers.windMultiplier(level);
    }

    /**
     * Ambient heat at {@code pos} in {@code level}: the dimension's base level (api → config →
     * {@code thermalAmbientDefault}) plus the biome flavour ({@code (baseTemperature - 0.8) *
     * thermalBiomeScale}). This is the level a machine's heat relaxes toward.
     */
    public static int ambientAt(Level level, BlockPos pos) {
        return ThermalEnvironment.ambientAt(level, pos);
    }
}
