package za.co.neroland.nerotech.upgrade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Heat Sink and Scrubber Module bounds. Pure JVM. */
class ModuleMathTest {

    @Test
    void noModuleLeavesTheConfigValues() {
        assertEquals(800, ModuleMath.throttleThreshold(800, 1000, 0, 125));
        assertEquals(1000, ModuleMath.pollutionPermille(0, 200, 250));
    }

    @Test
    void heatSinksWidenTheBandUpToHalfTheHeadroom() {
        assertEquals(825, ModuleMath.throttleThreshold(800, 1000, 1, 125));
        assertEquals(900, ModuleMath.throttleThreshold(800, 1000, 4, 125));
        // More modules than count, or a huge per-module share, still stop at half the headroom.
        assertEquals(900, ModuleMath.throttleThreshold(800, 1000, 64, 125));
        assertEquals(900, ModuleMath.throttleThreshold(800, 1000, 4, 1000));
        for (int n = 0; n <= 16; n++) {
            int t = ModuleMath.throttleThreshold(800, 1000, n, 250);
            assertTrue(t >= 800 && t < 1000, "threshold " + t + " must stay below the ceiling");
        }
        // A threshold already at or over the ceiling is left alone.
        assertEquals(1000, ModuleMath.throttleThreshold(1000, 1000, 4, 125));
    }

    @Test
    void scrubbersCutPollutionDownToTheFloor() {
        assertEquals(800, ModuleMath.pollutionPermille(1, 200, 250));
        assertEquals(250, ModuleMath.pollutionPermille(4, 200, 250));
        assertEquals(250, ModuleMath.pollutionPermille(99, 1000, 250));
        assertEquals(1000, ModuleMath.pollutionPermille(-3, 200, 250));
        assertEquals(0, ModuleMath.pollutionPermille(4, 1000, 0));
    }
}
