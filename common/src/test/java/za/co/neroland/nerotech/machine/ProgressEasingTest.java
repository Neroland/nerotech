package za.co.neroland.nerotech.machine;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Locks the client-side progress extrapolation behind {@code progressFraction}. Pure JVM. */
class ProgressEasingTest {

    private static final float EPS = 1e-5f;

    @Test
    void extrapolatesBetweenSyncsAndLerpsByPartialTick() {
        ProgressEasing easing = new ProgressEasing();
        easing.sync(0, 100, 1, true);
        assertEquals(0f, easing.fraction(1000L, 0f), EPS);
        assertEquals(0.01f, easing.fraction(1001L, 1f), EPS);
        assertEquals(0.005f, easing.fraction(1001L, 0.5f), EPS);
        assertEquals(0.015f, easing.fraction(1002L, 0.5f), EPS);
    }

    @Test
    void clampsAtOne() {
        ProgressEasing easing = new ProgressEasing();
        easing.sync(95, 100, 2, true);
        easing.fraction(0L, 0f);
        assertEquals(1f, easing.fraction(10L, 1f), EPS);
    }

    @Test
    void stallFreezesMidStrokeAndIdleIsZero() {
        ProgressEasing easing = new ProgressEasing();
        easing.sync(40, 100, 1, false);
        easing.fraction(0L, 0f);
        assertEquals(0.4f, easing.fraction(50L, 0.7f), EPS);
        easing.sync(0, 0, 0, false);
        assertEquals(0f, easing.fraction(60L, 0.5f), EPS);
    }

    @Test
    void aNewOperationNeverLerpsBackwardsAcrossTheWrap() {
        ProgressEasing easing = new ProgressEasing();
        easing.sync(0, 10, 1, true);
        easing.fraction(0L, 0f);
        easing.fraction(9L, 1f);
        easing.sync(0, 10, 1, true);
        assertEquals(0f, easing.fraction(10L, 0f), EPS);
    }
}
