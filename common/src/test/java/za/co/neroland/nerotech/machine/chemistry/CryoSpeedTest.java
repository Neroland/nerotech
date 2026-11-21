package za.co.neroland.nerotech.machine.chemistry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Locks the Cryo Distiller's ambient rule: cold speeds it up, heat slows it down, the bounds hold. Pure JVM. */
class CryoSpeedTest {

    private static final int PER = 5;
    private static final int MIN = 500;
    private static final int MAX = 2000;

    @Test
    void coldSpeedsItUp() {
        assertEquals(1.0D, CryoSpeed.factor(0, PER, MIN, MAX), 1e-9);
        assertEquals(1.4D, CryoSpeed.factor(-80, PER, MIN, MAX), 1e-9); // Glacira
        assertTrue(CryoSpeed.factor(-100, PER, MIN, MAX) > CryoSpeed.factor(-10, PER, MIN, MAX));
    }

    @Test
    void heatSlowsItDown() {
        assertTrue(CryoSpeed.factor(50, PER, MIN, MAX) < 1.0D);
        assertTrue(CryoSpeed.factor(80, PER, MIN, MAX) < CryoSpeed.factor(20, PER, MIN, MAX));
    }

    @Test
    void boundsHold() {
        assertEquals(2.0D, CryoSpeed.factor(-200, PER, MIN, MAX), 1e-9);
        assertEquals(2.0D, CryoSpeed.factor(-100_000, PER, MIN, MAX), 1e-9);
        assertEquals(0.5D, CryoSpeed.factor(150, PER, MIN, MAX), 1e-9); // the Nether
        assertEquals(0.5D, CryoSpeed.factor(Integer.MAX_VALUE, PER, MIN, MAX), 1e-9);
        assertEquals(2.0D, CryoSpeed.factor(Integer.MIN_VALUE, PER, MIN, MAX), 1e-9);
        // A zero bonus disables the effect; a floor above the cap never inverts the range.
        assertEquals(1.0D, CryoSpeed.factor(-80, 0, MIN, MAX), 1e-9);
        assertEquals(0.9D, CryoSpeed.factor(0, PER, 900, 100), 1e-9);
    }
}
