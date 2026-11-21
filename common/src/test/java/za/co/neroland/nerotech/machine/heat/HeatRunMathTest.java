package za.co.neroland.nerotech.machine.heat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Heat Pipe run rules (decision 012): heat is conserved along a run (out never exceeds in), it flows from
 * the hot endpoint to the cold one, long runs lose more, re-seeding never creates heat, and the contact
 * damage threshold. Pure JVM.
 */
class HeatRunMathTest {

    /** A machine-like node with a capacity; optional refusal of incoming heat. */
    static final class Box implements HeatRunMath.Node {
        int heat;
        final int capacity;
        final boolean refuses;

        Box(int heat, int capacity, boolean refuses) {
            this.heat = heat;
            this.capacity = capacity;
            this.refuses = refuses;
        }

        @Override
        public int heat() {
            return this.heat;
        }

        @Override
        public int take(int amount) {
            int taken = Math.min(amount, this.heat);
            this.heat -= taken;
            return taken;
        }

        @Override
        public int give(int amount) {
            if (this.refuses) {
                return 0;
            }
            int accepted = Math.min(amount, this.capacity - this.heat);
            this.heat += accepted;
            return accepted;
        }
    }

    @Test
    void conservationAlongARun() {
        Box hot = new Box(1000, 1000, false);
        Box cold = new Box(0, 1000, false);
        Box refusing = new Box(100, 1000, true);
        List<Box> ends = List.of(hot, cold, refusing);
        long run = 0;
        long lostTotal = 0;
        long before = 1000 + 0 + 100;
        for (int i = 0; i < 500; i++) {
            HeatRunMath.Result r = HeatRunMath.exchange(run, 12, ends, 250, 1000, 0, 1);
            run = r.runHeat();
            lostTotal += r.lost();
            long now = hot.heat + cold.heat + refusing.heat + run;
            assertEquals(before, now + lostTotal, "every unit is accounted for: held or lost");
            assertTrue(now <= before, "heat is never created");
        }
        assertTrue(cold.heat > 0, "heat reached the cold endpoint through the run");
    }

    @Test
    void heatFlowsHotToColdInOneExchange() {
        Box hot = new Box(800, 1000, false);
        Box cold = new Box(0, 1000, false);
        HeatRunMath.Result r = HeatRunMath.exchange(400, 1, List.of(cold, hot), 500, 1000, 0, 0);
        assertTrue(hot.heat < 800);
        assertTrue(cold.heat > 0);
        assertEquals(1200, hot.heat + cold.heat + r.runHeat());
    }

    @Test
    void longRunsLoseMore() {
        long shortLost = lossAfter(4);
        long longLost = lossAfter(64);
        assertTrue(longLost > shortLost, "a 64-pipe run sheds more than a 4-pipe run: " + longLost + " vs " + shortLost);
    }

    private static long lossAfter(int pipes) {
        Box hot = new Box(1000, 1000, false);
        Box cold = new Box(0, 1000, false);
        long run = 0;
        long lost = 0;
        for (int i = 0; i < 200; i++) {
            hot.heat = 1000; // a reactor held at full heat
            HeatRunMath.Result r = HeatRunMath.exchange(run, pipes, new ArrayList<>(List.of(hot, cold)), 250, 1000, 0, 1);
            run = r.runHeat();
            lost += r.lost();
        }
        return lost;
    }

    @Test
    void neverLosesBelowAmbientOrGainsFromIt() {
        HeatRunMath.Result r = HeatRunMath.exchange(100, 10, List.of(), 250, 1000, 50, 5);
        assertEquals(100, r.runHeat(), "a run colder than ambient neither loses nor gains");
    }

    @Test
    void reseedNeverCreatesHeat() {
        int capacity = 1000;
        for (int t = 0; t <= capacity; t++) {
            int level = HeatRunMath.level(t, capacity);
            int seed = HeatRunMath.seedForLevel(level, capacity);
            assertTrue(seed <= t, "seed " + seed + " exceeds temperature " + t);
            assertEquals(level, HeatRunMath.level(seed, capacity), "seed shows the same level");
        }
    }

    @Test
    void contactDamageThreshold() {
        assertFalse(HeatRunMath.hurtsToTouch(0, true));
        assertFalse(HeatRunMath.hurtsToTouch(1, true));
        assertTrue(HeatRunMath.hurtsToTouch(2, true));
        assertTrue(HeatRunMath.hurtsToTouch(3, true));
        assertFalse(HeatRunMath.hurtsToTouch(3, false), "config switch turns it off");
        assertEquals(2, HeatRunMath.level(500, 1000), "half capacity is level 2, the first that hurts");
        assertEquals(1, HeatRunMath.level(499, 1000));
    }
}
