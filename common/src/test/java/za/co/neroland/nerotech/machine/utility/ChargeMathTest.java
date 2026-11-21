package za.co.neroland.nerotech.machine.utility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/** Induction Charger rules: bounded reach, offer() bounds and simulate, the per-tick budget. Pure JVM. */
class ChargeMathTest {

    /** A charger stand-in holding {@code stored} units. */
    private static final class Pool implements ChargeMath.Source {
        long stored;

        Pool(long stored) {
            this.stored = stored;
        }

        @Override
        public long available() {
            return this.stored;
        }

        @Override
        public long take(long amount) {
            this.stored -= amount;
            return amount;
        }
    }

    /**
     * A charger stand-in shaped like the real one: a token budget of {@code budget} units, a buffer of
     * {@code stored} units, and a per-call extraction clamp of {@code maxExtract} units.
     */
    private static final class ClampedCharger implements ChargeMath.Source {
        long budget;
        long stored;
        final long maxExtract;
        final boolean reportOnlyBudget;

        ClampedCharger(long budget, long stored, long maxExtract, boolean reportOnlyBudget) {
            this.budget = budget;
            this.stored = stored;
            this.maxExtract = maxExtract;
            this.reportOnlyBudget = reportOnlyBudget;
        }

        private long extract(long want, boolean simulate) {
            long got = Math.max(0, Math.min(want, Math.min(this.maxExtract, this.stored)));
            if (!simulate) {
                this.stored -= got;
            }
            return got;
        }

        @Override
        public long available() {
            // The fixed charger caps availability with a simulated extraction; the old one did not.
            return this.reportOnlyBudget ? Math.min(this.budget, this.stored) : extract(this.budget, true);
        }

        @Override
        public long take(long amount) {
            long got = extract(amount, false);
            this.budget -= got;
            return got;
        }
    }

    @Test
    void reachIsABoundedBoxOnEveryAxis() {
        assertTrue(ChargeMath.inBox(0, 64, 0, 4, 60, -4, 4));
        assertFalse(ChargeMath.inBox(0, 64, 0, 5, 64, 0, 4));
        assertFalse(ChargeMath.inBox(0, 64, 0, 0, 69, 0, 4));
        assertFalse(ChargeMath.inBox(0, 64, 0, 0, 64, 0, -1));
        assertTrue(ChargeMath.inBox(7, 7, 7, 7, 7, 7, 0));
    }

    @Test
    void offerNeverExceedsTheRequestOrTheSources() {
        Pool a = new Pool(300);
        Pool b = new Pool(500);
        assertEquals(1000, ChargeMath.offer(1000, false, List.of(a, b)) + 200);
        assertEquals(0, a.stored);
        assertEquals(0, b.stored);

        Pool c = new Pool(300);
        Pool d = new Pool(500);
        assertEquals(400, ChargeMath.offer(400, false, List.of(c, d)));
        assertEquals(0, c.stored);
        assertEquals(400, d.stored);
    }

    @Test
    void offerOfNothingGivesNothing() {
        Pool a = new Pool(300);
        assertEquals(0, ChargeMath.offer(0, false, List.of(a)));
        assertEquals(0, ChargeMath.offer(-5, false, List.of(a)));
        assertEquals(0, ChargeMath.offer(100, false, List.of()));
        assertEquals(300, a.stored);
    }

    @Test
    void simulateTakesNothing() {
        Pool a = new Pool(300);
        Pool b = new Pool(500);
        assertEquals(700, ChargeMath.offer(700, true, List.of(a, b)));
        assertEquals(300, a.stored);
        assertEquals(500, b.stored);
        assertEquals(ChargeMath.offer(700, true, List.of(a, b)), ChargeMath.offer(700, false, List.of(a, b)));
        assertEquals(100, b.stored);
    }

    @Test
    void negativeAvailabilityIsIgnored() {
        Pool broken = new Pool(-50);
        Pool ok = new Pool(20);
        assertEquals(20, ChargeMath.offer(100, false, List.of(broken, ok)));
        assertEquals(-50, broken.stored);
    }

    @Test
    void aClampedChargerDeliversExactlyWhatItLoses() {
        // H1: budget 5,120, buffer 100,000, per-call clamp 2,000. Three offers used to deliver 9,360.
        ClampedCharger charger = new ClampedCharger(5120, 100_000, 2000, false);
        long delivered = 0;
        for (int i = 0; i < 3; i++) {
            delivered += ChargeMath.offer(5120, false, List.of(charger));
        }
        assertEquals(100_000 - charger.stored, delivered);
        assertTrue(delivered <= 5120);
    }

    @Test
    void offerCountsOnlyWhatTheSourceReallyGave() {
        // Even a source that over-reports availability cannot make offer() count energy it never gave.
        ClampedCharger charger = new ClampedCharger(5120, 100_000, 2000, true);
        long delivered = ChargeMath.offer(5120, false, List.of(charger));
        assertEquals(2000, delivered);
        assertEquals(98_000, charger.stored);
    }

    @Test
    void conversionsNeverMakeEnergyAtAnyRatio() {
        for (double ratio : new double[] {0.25, 0.5, 1.0, 2.5, 4.0, 10.0}) {
            for (long ne = 0; ne <= 50; ne++) {
                long fe = ChargeMath.neToFeFloor(ne, ratio);
                // Paying for the FE an NE amount buys never costs more than that NE...
                assertTrue(ChargeMath.feToNeCeil(fe, ratio) <= Math.max(ne, fe > 0 ? 1 : 0),
                        "ratio " + ratio + " ne " + ne);
                // ...and the FE bought is never worth more than the NE paid.
                assertTrue(fe <= ne * ratio + 1e-9);
            }
            for (long fe = 1; fe <= 50; fe++) {
                // Every non-zero delivery costs at least its worth, and at least 1 NE.
                long cost = ChargeMath.feToNeCeil(fe, ratio);
                assertTrue(cost >= 1);
                assertTrue(cost * ratio + 1e-9 >= fe, "ratio " + ratio + " fe " + fe);
                // An NE item offered fe FE takes no more NE than the offer is worth, and reports no
                // more FE than offered, but at least the worth of what it took.
                long ne = ChargeMath.feOfferToNe(fe, ratio);
                assertTrue(ne * ratio <= fe + 1e-9);
                long reported = ChargeMath.acceptedFe(ne, ratio, fe);
                assertTrue(reported <= fe);
                assertTrue(ne == 0 || ChargeMath.feToNeCeil(reported, ratio) >= ne);
            }
        }
        // Ratio 10: 14 FE costs 2 NE (it used to cost 1); ratio 4: a tool offered 2,000 FE takes 500 NE.
        assertEquals(2, ChargeMath.feToNeCeil(14, 10.0));
        assertEquals(500, ChargeMath.feOfferToNe(2000, 4.0));
        assertEquals(2000, ChargeMath.acceptedFe(500, 4.0, 2000));
        assertEquals(0, ChargeMath.feToNeCeil(0, 4.0));
        assertEquals(0, ChargeMath.neToFeFloor(10, 0.0));
    }

    @Test
    void budgetCapsOutputPerTickAndBanksOnePass() {
        ChargeBudget budget = new ChargeBudget();
        long perTick = 10;
        long burst = perTick * ChargeMath.PASS_TICKS;
        assertEquals(burst, budget.available(100, perTick, burst));
        budget.spend(burst);
        assertEquals(0, budget.available(100, perTick, burst));
        assertEquals(30, budget.available(103, perTick, burst));
        budget.spend(1000);
        assertEquals(0, budget.available(103, perTick, burst));
        assertEquals(burst, budget.available(1_000_000, perTick, burst));
        // A clock set back starts a fresh bucket rather than going negative.
        budget.spend(burst);
        assertEquals(burst, budget.available(5, perTick, burst));
    }

    @Test
    void speedFactorScalesThePerTickCap() {
        assertEquals(256, ChargeMath.scaledPerTick(256, 1.0));
        assertEquals(512, ChargeMath.scaledPerTick(256, 2.0));
        assertEquals(0, ChargeMath.scaledPerTick(256, 0.0));
        assertEquals(0, ChargeMath.scaledPerTick(-1, 3.0));
        assertEquals(Long.MAX_VALUE / ChargeMath.PASS_TICKS, ChargeMath.scaledPerTick(Long.MAX_VALUE, 10.0));
    }
}
