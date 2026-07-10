package za.co.neroland.nerotech.heat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Locks the full-thermal-model maths (Stage C, 2026-07-10): conduction moves heat hot→cold,
 * conserves it, never overshoots equilibrium, and converges despite integer division; the
 * ambient step relaxes machines toward ambient from both directions and settles exactly there.
 * Pure JVM — no game bootstrap.
 */
class ThermalMathTest {

    @Test
    void conductionMovesHotToColdAndConserves() {
        int a = 900;
        int b = 100;
        int step = ThermalMath.conductionStep(a, b, 100); // 10% of delta 800 = 80
        assertEquals(80, step);
        assertEquals(1000, (a - step) + (b + step), "conduction must conserve total heat");

        // Symmetric: from B's perspective the step is negative (heat flows toward B).
        assertEquals(-80, ThermalMath.conductionStep(b, a, 100));
    }

    @Test
    void conductionNeverOvershootsEquilibrium() {
        // 1000‰ would naively move the full delta; the clamp caps it at half (equilibrium).
        assertEquals(400, ThermalMath.conductionStep(900, 100, 1_000));
        // Equal heat: nothing flows.
        assertEquals(0, ThermalMath.conductionStep(500, 500, 1_000));
    }

    @Test
    void conductionConvergesDespiteIntegerDivision() {
        // Tiny gradient + low conductivity: integer division gives 0, but delta >= 2 still moves 1.
        assertEquals(1, ThermalMath.conductionStep(12, 10, 10));
        // Delta of 1 is thermal contact — stays put (half-clamp forbids overshoot).
        assertEquals(0, ThermalMath.conductionStep(11, 10, 1_000));
        // Repeated exchanges must reach contact, never oscillate.
        int a = 977;
        int b = 3;
        for (int i = 0; i < 10_000 && Math.abs(a - b) > 1; i++) {
            int step = ThermalMath.conductionStep(a, b, 10);
            a -= step;
            b += step;
        }
        assertTrue(Math.abs(a - b) <= 1, "pair must reach equilibrium, got " + a + " / " + b);
        assertEquals(980, a + b, "total heat conserved across the run");
    }

    @Test
    void ambientStepRelaxesBothDirections() {
        // Hot machine in a temperate spot cools...
        assertEquals(-18, ThermalMath.ambientStep(900, 0, 20));
        // ...a cold machine in a hot place (Cindara, nether) warms up...
        assertEquals(3, ThermalMath.ambientStep(0, 150, 20));
        // ...and at ambient nothing changes.
        assertEquals(0, ThermalMath.ambientStep(150, 150, 20));
    }

    @Test
    void ambientStepConvergesExactlyToAmbient() {
        int heat = 1_000;
        int guard = 0;
        while (heat != 40 && guard++ < 10_000) {
            heat += ThermalMath.ambientStep(heat, 40, 20);
        }
        assertEquals(40, heat, "must settle exactly on ambient (min step 1 beats integer division)");

        // From below, too.
        heat = 0;
        guard = 0;
        while (heat != 40 && guard++ < 10_000) {
            heat += ThermalMath.ambientStep(heat, 40, 20);
        }
        assertEquals(40, heat);
    }

    @Test
    void ambientStepNeverOvershoots() {
        // One unit away: the min-1 step lands exactly on ambient, not past it.
        assertEquals(1, ThermalMath.ambientStep(39, 40, 20));
        assertEquals(-1, ThermalMath.ambientStep(41, 40, 20));
        // Disabled exchange does nothing.
        assertEquals(0, ThermalMath.ambientStep(900, 0, 0));
    }

    @Test
    void clampHeatBounds() {
        assertEquals(0, ThermalMath.clampHeat(-5, 1_000));
        assertEquals(1_000, ThermalMath.clampHeat(2_000, 1_000));
        assertEquals(37, ThermalMath.clampHeat(37, 1_000));
    }
}
