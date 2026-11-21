package za.co.neroland.nerotech.api;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import za.co.neroland.nerotech.heat.ThermalEnvironment;
import za.co.neroland.nerotech.machine.PlanetModifiers;
import za.co.neroland.nerotech.machine.chemistry.AtmosphereLookup;
import za.co.neroland.nerotech.machine.chemistry.AtmosphereMapping;

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

    /**
     * The air an Atmospheric Condenser draws at {@code pos} in {@code level} (since 0.8.0): Nerospace
     * planet archetype / temperature band when Nerospace is installed (Europa is {@link Atmosphere#VACUUM}
     * by dimension id) → the {@code condenserAtmospheres} config table → {@link Atmosphere#EARTHLIKE}.
     * Dimension-wide today; the position is part of the signature so a later release can vary it.
     */
    public static Atmosphere atmosphereAt(Level level, BlockPos pos) {
        return AtmosphereLookup.readingAt(level).atmosphere();
    }

    /**
     * How fast the condenser draws {@link #atmosphereAt} here, as a multiplier of its recipe rate
     * (1.0 normally, 0.5 on a twilight world, 0 in vacuum). Since 0.8.0.
     */
    public static double atmosphereRateAt(Level level, BlockPos pos) {
        AtmosphereMapping.Reading reading = AtmosphereLookup.readingAt(level);
        return reading.atmosphere().condensable() ? reading.rate() : 0.0D;
    }
}
