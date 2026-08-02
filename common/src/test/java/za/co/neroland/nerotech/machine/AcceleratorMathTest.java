package za.co.neroland.nerotech.machine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import za.co.neroland.nerotech.machine.AcceleratorMath.Heading;

/**
 * Locks the Particle Accelerator's three rules — gap, bend and collision energy — plus the heading
 * algebra the beam trace walks on. Pure JVM: no game bootstrap, no level, no block entity.
 *
 * <p>The default balance values (allowance 4, gapPerSpeed 0.12, bendSpeedBase 20, scale 500) are
 * used throughout, so these tests also pin the shipped ring-size ladder.
 */
class AcceleratorMathTest {

    private static final double ALLOWANCE = 4.0D;
    private static final double GAP_PER_SPEED = 0.12D;
    private static final double BEND_BASE = 20.0D;
    private static final int ENERGY_SCALE = 500;

    @Test
    void headingsTurnFortyFiveDegreesEachWay() {
        assertSame(Heading.NORTH_EAST, Heading.NORTH.right());
        assertSame(Heading.NORTH_WEST, Heading.NORTH.left());
        // Eight 45° turns come all the way back round, either way.
        Heading right = Heading.SOUTH_WEST;
        Heading left = Heading.SOUTH_WEST;
        for (int i = 0; i < 8; i++) {
            right = right.right();
            left = left.left();
        }
        assertSame(Heading.SOUTH_WEST, right);
        assertSame(Heading.SOUTH_WEST, left);
        // A left undoes a right.
        assertSame(Heading.EAST, Heading.EAST.right().left());
    }

    @Test
    void diagonalStepsAreLongerThanAxisSteps() {
        assertFalse(Heading.EAST.diagonal());
        assertTrue(Heading.SOUTH_EAST.diagonal());
        assertEquals(3.0D, AcceleratorMath.segmentLength(Heading.EAST, 3), 1.0e-9D);
        assertEquals(3.0D * Math.sqrt(2.0D), AcceleratorMath.segmentLength(Heading.SOUTH_EAST, 3), 1.0e-9D);
        assertEquals(0.0D, AcceleratorMath.segmentLength(Heading.NORTH, 0), 1.0e-9D);
    }

    @Test
    void gapRuleWidensWithSpeed() {
        // At a standstill only the flat allowance is available.
        assertEquals(4.0D, AcceleratorMath.maxGap(0.0D, ALLOWANCE, GAP_PER_SPEED), 1.0e-9D);
        assertTrue(AcceleratorMath.gapAllowed(4.0D, 0.0D, ALLOWANCE, GAP_PER_SPEED), "exact fit must pass");
        assertFalse(AcceleratorMath.gapAllowed(4.5D, 0.0D, ALLOWANCE, GAP_PER_SPEED));
        // 100 speed buys 12 more blocks of reach.
        assertEquals(16.0D, AcceleratorMath.maxGap(100.0D, ALLOWANCE, GAP_PER_SPEED), 1.0e-9D);
        assertTrue(AcceleratorMath.gapAllowed(16.0D, 100.0D, ALLOWANCE, GAP_PER_SPEED));
        assertFalse(AcceleratorMath.gapAllowed(16.1D, 100.0D, ALLOWANCE, GAP_PER_SPEED));
    }

    @Test
    void injectionSpeedIsTheGapRuleInverted() {
        // Anything inside the flat allowance can be injected at a standstill.
        assertEquals(0.0D, AcceleratorMath.speedForGap(3.0D, ALLOWANCE, GAP_PER_SPEED), 1.0e-9D);
        assertEquals(0.0D, AcceleratorMath.speedForGap(4.0D, ALLOWANCE, GAP_PER_SPEED), 1.0e-9D);
        // A 12-block stretch demands (12-4)/0.12 ≈ 66.7 — and that speed then clears it exactly.
        double required = AcceleratorMath.speedForGap(12.0D, ALLOWANCE, GAP_PER_SPEED);
        assertEquals(66.666666D, required, 1.0e-5D);
        assertTrue(AcceleratorMath.gapAllowed(12.0D, required, ALLOWANCE, GAP_PER_SPEED));
    }

    @Test
    void bendRuleNeedsRunUp() {
        // A 45° turn off a 4-block stretch survives up to speed 80.
        assertEquals(80.0D, AcceleratorMath.maxBendSpeed(4.0D, BEND_BASE), 1.0e-9D);
        assertTrue(AcceleratorMath.bendAllowed(80.0D, 4.0D, BEND_BASE), "exact fit must pass");
        assertFalse(AcceleratorMath.bendAllowed(80.5D, 4.0D, BEND_BASE));
        // Triple the run-up, triple the ceiling — this is why bigger rings reach higher speeds.
        assertTrue(AcceleratorMath.bendAllowed(240.0D, 12.0D, BEND_BASE));
        assertFalse(AcceleratorMath.bendAllowed(240.0D, 11.0D, BEND_BASE));
    }

    @Test
    void collisionEnergyIsQuadraticInSpeed() {
        assertEquals(0, AcceleratorMath.collisionEnergy(0.0D, ENERGY_SCALE));
        assertEquals(0, AcceleratorMath.collisionEnergy(-5.0D, ENERGY_SCALE), "a dead beam carries nothing");
        // 0.5 * 100^2 * 0.5
        assertEquals(2_500, AcceleratorMath.collisionEnergy(100.0D, ENERGY_SCALE));
        // Doubling the speed quadruples the energy.
        assertEquals(10_000, AcceleratorMath.collisionEnergy(200.0D, ENERGY_SCALE));
        // The shipped ladder: the antimatter floor (12,000 J) needs speed ≥ √48000 ≈ 219.1, i.e.
        // speed 220, which the bend rule only allows off stretches of 11 blocks or longer.
        assertTrue(AcceleratorMath.collisionEnergy(220.0D, ENERGY_SCALE) >= 12_000);
        assertTrue(AcceleratorMath.collisionEnergy(219.0D, ENERGY_SCALE) < 12_000);
        assertFalse(AcceleratorMath.bendAllowed(220.0D, 10.0D, BEND_BASE));
        assertTrue(AcceleratorMath.bendAllowed(220.0D, 11.0D, BEND_BASE));
    }

    @Test
    void boostAndDragNeverGoNegative() {
        assertEquals(12.0D, AcceleratorMath.boosted(10.0D, 2.0D), 1.0e-9D);
        assertEquals(9.5D, AcceleratorMath.coasted(10.0D, 0.5D), 1.0e-9D);
        assertEquals(0.0D, AcceleratorMath.coasted(0.2D, 0.5D), 1.0e-9D, "a stopped particle floors at 0");
    }
}
