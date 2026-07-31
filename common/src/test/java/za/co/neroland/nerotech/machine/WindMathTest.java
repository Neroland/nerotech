package za.co.neroland.nerotech.machine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Locks the Stage D Wind Turbine altitude curve: clamped at both ends, linear and monotone in
 * between, and multiplied down to nothing by an airless dimension's 0 multiplier. Pure JVM — no
 * game bootstrap.
 */
class WindMathTest {

    @Test
    void curveIsClampedAtBothEnds() {
        assertEquals(WindMath.MIN_FACTOR, WindMath.heightFactor(WindMath.MIN_Y));
        assertEquals(WindMath.MIN_FACTOR, WindMath.heightFactor(0));
        assertEquals(WindMath.MIN_FACTOR, WindMath.heightFactor(-64), "below the world floor still clamps");
        assertEquals(WindMath.MAX_FACTOR, WindMath.heightFactor(WindMath.MAX_Y));
        assertEquals(WindMath.MAX_FACTOR, WindMath.heightFactor(320));
    }

    @Test
    void curveIsLinearBetweenTheAnchors() {
        // Midpoint of 80..200 is y=140 → halfway between 0.5x and 2.0x.
        assertEquals(1.25D, WindMath.heightFactor(140), 1.0e-9D);
        // A quarter of the way up (y=110) → 0.5 + 0.25 * 1.5.
        assertEquals(0.875D, WindMath.heightFactor(110), 1.0e-9D);
    }

    @Test
    void curveIsMonotoneAcrossTheWholeWorldColumn() {
        double previous = WindMath.heightFactor(-64);
        for (int y = -63; y <= 320; y++) {
            double current = WindMath.heightFactor(y);
            assertTrue(current >= previous, "wind must never fall as altitude rises (y=" + y + ")");
            previous = current;
        }
    }

    @Test
    void rateScalesWithTheCurveAndFloorsAtZero() {
        assertEquals(13, WindMath.ratePerTick(25, WindMath.MIN_Y, 1.0D)); // 25 * 0.5 = 12.5 → 13
        assertEquals(50, WindMath.ratePerTick(25, WindMath.MAX_Y, 1.0D)); // 25 * 2.0
        // Nerospace airless hook: multiplier 0 means a turbine there produces literally nothing.
        assertEquals(0, WindMath.ratePerTick(25, 200, 0.0D));
        assertEquals(0, WindMath.ratePerTick(0, 200, 1.0D));
    }
}
