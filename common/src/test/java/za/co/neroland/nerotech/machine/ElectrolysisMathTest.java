package za.co.neroland.nerotech.machine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Electrolysis stays an energy sink: Speed modules shorten an operation without making it cheaper, so no
 * module and preset mix makes hydrogen for less NE than NeroTech's Gas Turbine (60 NE x 2.0) gets back
 * from it. Defaults: 40 NE/t, 200 t, 2 hydrogen units per operation. Pure JVM.
 */
class ElectrolysisMathTest {

    private static final int BASE_COST = 40;
    private static final int BASE_TICKS = 200;
    private static final int HYDROGEN_UNITS_PER_OP = 2;
    private static final int GAS_TURBINE_NE_PER_HYDROGEN_UNIT = 120;

    private static int perUnit(double moduleSpeed, double efficiency, double presetSpeed, double presetEnergy) {
        int ticks = ElectrolysisMath.operationTicks(BASE_TICKS, moduleSpeed, presetSpeed);
        int cost = ElectrolysisMath.costPerTick(BASE_COST, efficiency, presetEnergy, moduleSpeed);
        return ticks * cost / HYDROGEN_UNITS_PER_OP;
    }

    @Test
    void noModulesIsUnchanged() {
        assertEquals(BASE_TICKS, ElectrolysisMath.operationTicks(BASE_TICKS, 1.0D, 1.0D));
        assertEquals(BASE_COST, ElectrolysisMath.costPerTick(BASE_COST, 1.0D, 1.0D, 1.0D));
    }

    @Test
    void speedModulesDoNotLowerTheEnergyPerOperation() {
        int slow = perUnit(1.0D, 1.0D, 1.0D, 1.0D);
        for (double speed = 1.0D; speed <= 8.0D; speed += 0.5D) {
            int fast = perUnit(speed, 1.0D, 1.0D, 1.0D);
            assertTrue(Math.abs(fast - slow) <= slow / 50, "speed " + speed + " gave " + fast + " vs " + slow);
        }
    }

    @Test
    void worstCaseStaysAboveTheGasTurbineYield() {
        // 8x module speed, the Efficiency floor (0.25) and Eco (speed 0.75, energy 0.5).
        int worst = perUnit(8.0D, 0.25D, 0.75D, 0.5D);
        assertTrue(worst > GAS_TURBINE_NE_PER_HYDROGEN_UNIT, "hydrogen for " + worst + " NE a unit");
        // Overdrive with full modules too.
        assertTrue(perUnit(8.0D, 0.25D, 1.5D, 2.0D) > GAS_TURBINE_NE_PER_HYDROGEN_UNIT);
    }
}
