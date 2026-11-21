package za.co.neroland.nerotech.machine.chemistry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import za.co.neroland.nerotech.api.Atmosphere;

/**
 * Locks the condenser's atmosphere table (decision 006 §5): archetype → atmosphere, the Europa override,
 * the twilight half rate, the band fallback for unknown archetypes, and the config table when Nerospace
 * is absent (defaulting to EARTHLIKE). Pure JVM.
 */
class AtmosphereMappingTest {

    private static Atmosphere of(String dimension, String archetype, String band) {
        return AtmosphereMapping.fromTraits(dimension, archetype, band).orElseThrow().atmosphere();
    }

    @Test
    void archetypesMapPerDecision() {
        assertEquals(Atmosphere.VACUUM, of("nerospace:station", "ORBITAL", "TEMPERATE"));
        assertEquals(Atmosphere.VACUUM, of("nerospace:luna", "BARREN_MOON", "COLD"));
        assertEquals(Atmosphere.THIN_CO2, of("nerospace:mars", "DESERT", "COLD"));
        assertEquals(Atmosphere.SULFUROUS, of("nerospace:cindara", "VOLCANIC", "INFERNAL"));
        assertEquals(Atmosphere.NITROGEN_METHANE, of("nerospace:glacira", "FROZEN", "FRIGID"));
        assertEquals(Atmosphere.NITROGEN_OXYGEN, of("nerospace:greenxertz", "JUNGLE", "HOT"));
        assertEquals(Atmosphere.NITROGEN_OXYGEN, of("nerospace:nyxara", "TWILIGHT", "COLD"));
    }

    @Test
    void europaIsAirlessDespiteBeingFrozen() {
        assertEquals(Atmosphere.VACUUM, of(AtmosphereMapping.EUROPA, "FROZEN", "FRIGID"));
        assertEquals(Atmosphere.VACUUM, of(AtmosphereMapping.EUROPA, "", ""));
    }

    @Test
    void twilightDrawsAtHalfRate() {
        assertEquals(0.5D, AtmosphereMapping.fromTraits("nerospace:nyxara", "TWILIGHT", "COLD").orElseThrow().rate(), 1e-9);
        assertEquals(1.0D, AtmosphereMapping.fromTraits("nerospace:greenxertz", "JUNGLE", "HOT").orElseThrow().rate(), 1e-9);
    }

    @Test
    void unknownArchetypeFallsBackOnTheBandOrDefers() {
        assertEquals(Atmosphere.SULFUROUS, of("nerospace:new", "OCEAN", "INFERNAL"));
        assertEquals(Atmosphere.NITROGEN_METHANE, of("nerospace:new", "OCEAN", "FRIGID"));
        assertTrue(AtmosphereMapping.fromTraits("nerospace:new", "OCEAN", "TEMPERATE").isEmpty());
        assertTrue(AtmosphereMapping.fromTraits("nerospace:new", "", "").isEmpty());
    }

    @Test
    void absentNerospaceUsesTheConfigTableThenEarthlike() {
        Map<String, Atmosphere> table = AtmosphereMapping.parseTable(ChemistrySettings.CONDENSER_ATMOSPHERES_DEFAULT);
        assertEquals(Atmosphere.SULFUROUS,
                AtmosphereMapping.resolve(Optional.empty(), "minecraft:the_nether", table).atmosphere());
        assertEquals(Atmosphere.VACUUM, AtmosphereMapping.resolve(Optional.empty(), "minecraft:the_end", table).atmosphere());
        assertEquals(Atmosphere.EARTHLIKE,
                AtmosphereMapping.resolve(Optional.empty(), "minecraft:overworld", table).atmosphere());
        assertEquals(Atmosphere.EARTHLIKE, AtmosphereMapping.resolve(Optional.empty(), "nerospace:mars", Map.of()).atmosphere());
    }

    @Test
    void planetTraitsBeatTheConfigTable() {
        Map<String, Atmosphere> table = AtmosphereMapping.parseTable("nerospace:mars=EARTHLIKE");
        assertEquals(Atmosphere.THIN_CO2, AtmosphereMapping.resolve(
                AtmosphereMapping.fromTraits("nerospace:mars", "DESERT", "COLD"), "nerospace:mars", table).atmosphere());
        // Traits with no opinion (an unknown archetype in a temperate band) hand over to the table.
        assertEquals(Atmosphere.EARTHLIKE, AtmosphereMapping.resolve(
                AtmosphereMapping.fromTraits("nerospace:mars", "OCEAN", "TEMPERATE"), "nerospace:mars", table).atmosphere());
    }

    @Test
    void tableParsingSkipsJunk() {
        Map<String, Atmosphere> table = AtmosphereMapping.parseTable(
                " a:b = thin_co2 ,bad, =VACUUM,c:d=NOT_AN_ATMOSPHERE,e:f=vacuum");
        assertEquals(Map.of("a:b", Atmosphere.THIN_CO2, "e:f", Atmosphere.VACUUM), table);
        assertTrue(AtmosphereMapping.parseTable("").isEmpty());
        assertTrue(AtmosphereMapping.parseTable(null).isEmpty());
    }
}
