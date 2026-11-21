package za.co.neroland.nerotech.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;

import org.junit.jupiter.api.Test;

/** The budget accumulator on its own: per-type totals, means per server tick, ordering. */
class TickBudgetTest {

    @Test
    void accumulatesPerTypeAndSortsByCost() {
        TickBudget.Window w = new TickBudget.Window();
        w.add("a", 1_000_000L);
        w.add("a", 3_000_000L);
        w.add("b", 10_000_000L);
        List<TickBudget.Row> rows = w.rows(String::valueOf, 200);
        assertEquals(2, rows.size());
        assertEquals("b", rows.get(0).type());
        assertEquals(1, rows.get(0).machineTicks());
        assertEquals(0.05D, rows.get(0).meanMsPerTick(), 1e-9);
        assertEquals("a", rows.get(1).type());
        assertEquals(2, rows.get(1).machineTicks());
        assertEquals(4_000_000L, rows.get(1).totalNanos());
        assertEquals(0.02D, rows.get(1).meanMsPerTick(), 1e-9);
    }

    @Test
    void idleByDefault() {
        assertFalse(TickBudget.sampling(), "no window is open until /nerotech budget starts one");
    }
}
