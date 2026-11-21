package za.co.neroland.nerotech.machine.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Crystal Grower rule: it grows with no power at all, power and cold ambient each make it faster, and
 * every bound holds whatever the config says.
 */
class CrystalGrowerRuleTest {

    private static final int TEMPERATE = 0;
    private static final int SNOWY = -40;
    private static final int FROZEN_WORLD = -100_000;

    @Test
    void unpoweredStillGrows() {
        assertTrue(CrystalGrowthMath.ratePermille(false, TEMPERATE) > 0);
        assertTrue(CrystalGrowthMath.ratePermille(false, 500) > 0, "even in a hot biome");
    }

    @Test
    void powerAndColdAreFaster() {
        int unpowered = CrystalGrowthMath.ratePermille(false, TEMPERATE);
        int powered = CrystalGrowthMath.ratePermille(true, TEMPERATE);
        int poweredCold = CrystalGrowthMath.ratePermille(true, SNOWY);
        assertTrue(powered > unpowered);
        assertTrue(poweredCold > powered);
        assertTrue(CrystalGrowthMath.ratePermille(false, SNOWY) > unpowered, "cold helps unpowered too");
        assertEquals(CrystalGrowthMath.NOMINAL, powered, "temperate and powered is the nominal rate");
    }

    @Test
    void capsHold() {
        int cap = ComponentsSettings.crystalGrowerColdBonusCapPermille();
        assertEquals(CrystalGrowthMath.NOMINAL + cap, CrystalGrowthMath.ratePermille(true, FROZEN_WORLD));
        assertEquals(cap, CrystalGrowthMath.coldBonus(Integer.MIN_VALUE, 0, 1_000, cap), "no overflow");
        // A silly config is clamped: the bonus never passes MAX_COLD_BONUS, unpowered never beats powered.
        assertEquals(CrystalGrowthMath.NOMINAL + CrystalGrowthMath.MAX_COLD_BONUS,
                CrystalGrowthMath.ratePermille(true, FROZEN_WORLD, 250, 0, 999_999, 999_999));
        for (int ambient : new int[] {FROZEN_WORLD, SNOWY, TEMPERATE, 1_000}) {
            assertTrue(CrystalGrowthMath.ratePermille(false, ambient, 99_999, 0, 10, 1_000)
                    <= CrystalGrowthMath.ratePermille(true, ambient, 99_999, 0, 10, 1_000));
        }
        assertEquals(0, CrystalGrowthMath.ratePermille(false, TEMPERATE, -5, 0, 10, 1_000),
                "a negative unpowered rate is zero, never backwards");
        assertEquals(0, CrystalGrowthMath.coldBonus(TEMPERATE, 0, 10, 1_000), "no bonus at the threshold");
        assertEquals(0, CrystalGrowthMath.coldBonus(SNOWY, 0, -10, 1_000), "a negative per-heat bonus is zero");
    }

    @Test
    void unpoweredBouleFinishesInBoundedTime() {
        int time = 1_200; // crystal_growing_crystal_boule
        int rate = CrystalGrowthMath.ratePermille(false, TEMPERATE);
        int carry = 0;
        int progress = 0;
        int ticks = 0;
        while (progress < time && ticks < 1_000_000) {
            carry += rate;
            int steps = carry / CrystalGrowthMath.NOMINAL;
            carry -= steps * CrystalGrowthMath.NOMINAL;
            progress += steps;
            ticks++;
        }
        assertEquals(time * CrystalGrowthMath.NOMINAL / rate, ticks, "unpowered takes 1000/rate times as long");
    }
}
