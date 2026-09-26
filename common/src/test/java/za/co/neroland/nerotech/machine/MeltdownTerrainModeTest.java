package za.co.neroland.nerotech.machine;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Locks the {@code fusionMeltdownTerrainDamage} resolution ({@link MeltdownMath#terrainDamage}):
 * all three modes on a dedicated server and in singleplayer, plus unrecognised input falling back to
 * {@code auto}. Pure JVM — no config, level or server is touched. The reactor maps the result
 * straight onto {@code ExplosionInteraction.BLOCK} / {@code NONE}, so this is the whole decision.
 */
class MeltdownTerrainModeTest {

    @Test
    void autoIsOffOnDedicatedAndOnInSingleplayer() {
        assertFalse(MeltdownMath.terrainDamage("auto", true), "auto on a dedicated server spares terrain");
        assertTrue(MeltdownMath.terrainDamage("auto", false), "auto in singleplayer/LAN breaks terrain");
    }

    @Test
    void onAlwaysBreaksTerrain() {
        assertTrue(MeltdownMath.terrainDamage("on", true));
        assertTrue(MeltdownMath.terrainDamage("on", false));
    }

    @Test
    void offNeverBreaksTerrain() {
        assertFalse(MeltdownMath.terrainDamage("off", true));
        assertFalse(MeltdownMath.terrainDamage("off", false));
    }

    @Test
    void unrecognisedOrMissingValueIsTreatedAsAuto() {
        assertFalse(MeltdownMath.terrainDamage("maybe", true));
        assertTrue(MeltdownMath.terrainDamage("maybe", false));
        assertFalse(MeltdownMath.terrainDamage("", true));
        assertFalse(MeltdownMath.terrainDamage(null, true));
        assertTrue(MeltdownMath.terrainDamage(null, false));
    }

    @Test
    void matchingTrimsAndIgnoresCase() {
        assertTrue(MeltdownMath.terrainDamage("  ON ", true));
        assertFalse(MeltdownMath.terrainDamage("Off", false));
    }
}
