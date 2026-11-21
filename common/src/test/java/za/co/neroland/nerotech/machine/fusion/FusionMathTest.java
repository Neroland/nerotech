package za.co.neroland.nerotech.machine.fusion;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Locks the fusion reactor's burn and heat arithmetic: the zero-addition regression (no injector, no
 * lattice = the pre-0.8.0 reactor), the Fuel Injector's gas ratio and consumption, and the Europium Flux
 * Lattice band widening and its cap. Pure JVM: no bootstrap, no level.
 */
class FusionMathTest {

    /** The reactor's heat formula exactly as it stood before 0.8.0. */
    private static int legacyHeat(int base, int shellTier, int burningTier) {
        return base * (3 + shellTier + (burningTier >= 4 ? 2 : 0));
    }

    @Test
    void noLatticeHeatIsTheLegacyFormula() {
        for (int base = 0; base <= 12; base++) {
            for (int shellTier = 0; shellTier <= 3; shellTier++) {
                for (int tier = 0; tier <= 4; tier++) {
                    assertEquals(legacyHeat(base, shellTier, tier),
                            FusionMath.heatPerTick(base, shellTier, tier, 0, 8, 60),
                            "base " + base + " shell " + shellTier + " tier " + tier);
                }
            }
        }
    }

    @Test
    void cellBurnOutputIsUnchanged() {
        for (int tier = 0; tier <= 4; tier++) {
            assertEquals(1234L, FusionMath.gasScaledOutput(1234L, tier, 1500));
        }
        assertEquals(1851L, FusionMath.gasScaledOutput(1234L, FusionMath.GAS_TIER, 1500));
    }

    @Test
    void gasBurnPaysTheTopTierSurcharge() {
        assertEquals(legacyHeat(4, 3, 4), FusionMath.baseHeat(4, 3, FusionMath.GAS_TIER));
    }

    @Test
    void eachLatticeWidensTheBand() {
        int base = FusionMath.baseHeat(10, 3, FusionMath.GAS_TIER); // 80
        int previous = FusionMath.latticeHeat(base, 0, 8, 60);
        assertEquals(80, previous);
        for (int n = 1; n <= 8; n++) {
            int now = FusionMath.latticeHeat(base, n, 8, 60);
            assertTrue(now < previous, "lattice " + n + " must lower heat");
            previous = now;
        }
        assertEquals(80 - 80 * 480 / 1000, previous);
    }

    @Test
    void latticesBeyondTheCapAddNothing() {
        int capped = FusionMath.latticeHeat(80, 8, 8, 60);
        assertEquals(capped, FusionMath.latticeHeat(80, 9, 8, 60));
        assertEquals(capped, FusionMath.latticeHeat(80, 26, 8, 60));
        assertEquals(8, FusionMath.countedLattices(98, 8));
        assertEquals(0, FusionMath.countedLattices(-3, 8));
    }

    @Test
    void aBurningReactorAlwaysWarms() {
        assertEquals(1, FusionMath.latticeHeat(3, 8, 8, 200));
        assertEquals(0, FusionMath.latticeHeat(0, 8, 8, 60));
    }

    @Test
    void gasChargeNeedsBothGasesInRatio() {
        // 20 ticks at 2 D + 1 He-3 per tick: 40 mB deuterium and 20 mB helium-3 per charge.
        assertEquals(40L, FusionMath.deuteriumPerCharge(20, 2));
        assertEquals(20L, FusionMath.helium3PerCharge(20, 1));
        assertTrue(FusionMath.canIgniteGas(40, 20, 20, 2, 1));
        assertFalse(FusionMath.canIgniteGas(39, 20, 20, 2, 1));
        assertFalse(FusionMath.canIgniteGas(40, 19, 20, 2, 1));
        // A surplus of one gas never covers a shortage of the other.
        assertFalse(FusionMath.canIgniteGas(100_000, 0, 20, 2, 1));
        assertFalse(FusionMath.canIgniteGas(0, 100_000, 20, 2, 1));
        assertFalse(FusionMath.canIgniteGas(40, 20, 0, 2, 1));
    }

    @Test
    void drawIsAllOrNothingAndSpansInjectors() {
        assertArrayEquals(new long[] {30, 10, 0}, FusionMath.drawPlan(new long[] {30, 25, 5}, 40));
        assertArrayEquals(new long[] {0, 0}, FusionMath.drawPlan(new long[] {10, 5}, 40));
        assertArrayEquals(new long[] {}, FusionMath.drawPlan(new long[] {}, 40));
    }

    @Test
    void onlyAMaximalShellTakesGas() {
        assertTrue(FusionMath.shellTakesGas(7, 7));
        assertFalse(FusionMath.shellTakesGas(5, 7));
        assertFalse(FusionMath.shellTakesGas(0, 0));
    }
}
