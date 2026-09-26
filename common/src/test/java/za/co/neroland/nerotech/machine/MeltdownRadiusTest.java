package za.co.neroland.nerotech.machine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Locks the Fusion Reactor meltdown blast radius ({@link MeltdownMath#meltdownRadius}):
 * shell edge + 1 for the three shells, capped by {@code fusionMeltdownRadiusCap}, never below 1.
 * Pure JVM — the helper is static and touches no level, block state or config.
 */
class MeltdownRadiusTest {

    @Test
    void shellsGiveFourSixEightUnderTheDefaultCap() {
        assertEquals(4, MeltdownMath.meltdownRadius(3, 8));
        assertEquals(6, MeltdownMath.meltdownRadius(5, 8));
        assertEquals(8, MeltdownMath.meltdownRadius(7, 8));
    }

    @Test
    void capClampsTheLargestShells() {
        assertEquals(4, MeltdownMath.meltdownRadius(7, 4), "a low cap flattens every shell to it");
        assertEquals(4, MeltdownMath.meltdownRadius(5, 4));
        assertEquals(4, MeltdownMath.meltdownRadius(3, 4), "the 3-shell is exactly at a cap of 4");
        assertEquals(1, MeltdownMath.meltdownRadius(7, 1), "the config minimum is a 1-block blast");
    }

    @Test
    void neverBelowOneEvenForDegenerateInput() {
        assertEquals(1, MeltdownMath.meltdownRadius(0, 8), "an unformed (size 0) reactor still blasts 1");
        assertEquals(1, MeltdownMath.meltdownRadius(-5, 8));
        assertEquals(1, MeltdownMath.meltdownRadius(3, 0), "an out-of-range cap can't zero the blast");
    }

    @Test
    void monotoneInShellSizeUpToTheCap() {
        int cap = 8;
        int previous = MeltdownMath.meltdownRadius(1, cap);
        for (int size = 2; size <= 16; size++) {
            int current = MeltdownMath.meltdownRadius(size, cap);
            assertTrue(current >= previous, "radius must never shrink as the shell grows (size=" + size + ")");
            assertTrue(current <= cap, "radius must never exceed the cap (size=" + size + ")");
            previous = current;
        }
    }
}
