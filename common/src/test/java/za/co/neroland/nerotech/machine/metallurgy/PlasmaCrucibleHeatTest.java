package za.co.neroland.nerotech.machine.metallurgy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Plasma Crucible rule: it holds its own temperature, so a cold start costs more energy than a warm
 * one (the arc must first raise the heat to the recipe's {@code min_heat}), and a hot ambient both keeps
 * an idle crucible warmer and makes its cold start cheaper. Defaults: shared config (loss 20 permille,
 * 4 heat per working tick, throttle 800, capacity 1000) and the family's firing settings.
 */
class PlasmaCrucibleHeatTest {

    private static final int LOSS = 20;
    private static final int WORK_HEAT = 4;
    private static final int THROTTLE = 800;
    private static final int CAPACITY = 1_000;
    private static final int MIN_HEAT = 200;
    private static final int ENERGY = 40;
    private static final int TIME = 200;

    private static long job(int startHeat, int ambient) {
        return CrucibleHeat.jobEnergy(startHeat, MIN_HEAT, ambient, LOSS,
                MetallurgySettings.plasmaCrucibleFiringHeat(), MetallurgySettings.plasmaCrucibleFiringEnergy(),
                WORK_HEAT, ENERGY, TIME, THROTTLE, CAPACITY);
    }

    @Test
    void coldStartCostsMoreThanWarmStart() {
        long warm = job(MIN_HEAT + 50, 0);
        long cold = job(0, 0);
        assertEquals((long) ENERGY * TIME, warm, "a warm crucible pays only the recipe's own energy");
        assertTrue(cold > warm, "a cold start pays for the arc first: cold " + cold + " vs warm " + warm);
        long firingTicks = (cold - warm) / MetallurgySettings.plasmaCrucibleFiringEnergy();
        assertTrue(firingTicks >= MIN_HEAT / MetallurgySettings.plasmaCrucibleFiringHeat(),
                "the premium is at least the ticks needed to fire up to min_heat");
    }

    @Test
    void hotAmbientMakesTheColdStartCheaper() {
        long temperate = job(0, 0);
        long nether = job(150, 150); // an idle crucible in a 150-heat ambient sits at 150
        assertTrue(nether < temperate, "hot ambient: " + nether + " vs temperate " + temperate);
    }

    @Test
    void idleCrucibleHoldsItsHeat() {
        int hold = MetallurgySettings.plasmaCrucibleHoldIntervalTicks();
        int held = CrucibleHeat.idle(400, 0, LOSS, 600, hold);
        int uninsulated = CrucibleHeat.idle(400, 0, LOSS, 600, 1);
        assertTrue(held > uninsulated, "insulated " + held + " vs bare " + uninsulated);
        assertTrue(held >= MIN_HEAT / 2, "30 s idle keeps a good part of the melt heat: " + held);
        int heldHot = CrucibleHeat.idle(400, 150, LOSS, 600, hold);
        assertTrue(heldHot > held, "a hot ambient retains heat better: " + heldHot + " vs " + held);
        assertTrue(job(held, 0) < job(0, 0), "a crucible that held its heat restarts cheaper");
    }

    @Test
    void firingNeverOverheatsAndHoldingNeverSlowsWarmingUp() {
        assertEquals(THROTTLE - 1, CrucibleHeat.fire(THROTTLE - 5, 12, THROTTLE, CAPACITY));
        assertEquals(THROTTLE - 1, CrucibleHeat.fire(THROTTLE - 1, 12, THROTTLE, CAPACITY));
        assertEquals(-1L, CrucibleHeat.jobEnergy(0, THROTTLE, 0, LOSS, 12, 60, WORK_HEAT, ENERGY, TIME,
                THROTTLE, CAPACITY), "a min_heat at the throttle can never run");
        assertEquals(0, CrucibleHeat.holdCorrection(50, 150, LOSS, 7L, 20), "below a hot ambient it warms freely");
        assertEquals(0, CrucibleHeat.holdCorrection(400, 0, LOSS, 40L, 20), "the cadence tick exchanges normally");
        assertTrue(CrucibleHeat.holdCorrection(400, 0, LOSS, 41L, 20) > 0, "off-cadence ticks keep the heat");
        assertEquals(1, CrucibleHeat.firingCost(0, 1.0D));
        assertEquals(30, CrucibleHeat.firingCost(60, 0.5D));
    }
}
