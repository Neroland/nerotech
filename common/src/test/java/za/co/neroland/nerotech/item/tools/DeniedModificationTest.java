package za.co.neroland.nerotech.item.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * Protected ground stays protected: the Power Drill (area and vein) and the Cutting Lance (one target)
 * offer each block to a permission predicate first, and a denied block never reaches the break path.
 * In game the predicate is {@code PowerDrillItem.mayBreak} and the break path is
 * {@code ServerPlayerGameMode.destroyBlock}, whose loader event can still veto. Pure JVM.
 */
class DeniedModificationTest {

    private static final BreakPlanner.Cell O = new BreakPlanner.Cell(10, 40, -3);

    @Test
    void drillBreaksNothingWhenEveryBlockIsDenied() {
        List<BreakPlanner.Cell> broken = new ArrayList<>();
        List<BreakPlanner.Cell> area = BreakPlanner.area(O, BreakPlanner.Axis.Y);
        assertEquals(0, BreakPlanner.execute(area, c -> false, c -> broken.add(c), 64));
        List<BreakPlanner.Cell> vein = BreakPlanner.vein(O, c -> true, 64);
        assertEquals(0, BreakPlanner.execute(vein, c -> false, c -> broken.add(c), 64));
        assertTrue(broken.isEmpty(), "a denied block must never reach the break path");
    }

    @Test
    void drillSkipsOnlyTheDeniedBlocks() {
        // A claim boundary at x = 10: everything with x > 10 belongs to someone else.
        Set<BreakPlanner.Cell> broken = new HashSet<>();
        List<BreakPlanner.Cell> area = BreakPlanner.area(O, BreakPlanner.Axis.Y);
        int count = BreakPlanner.execute(area, c -> c.x() <= 10, broken::add, 64);
        assertEquals(5, count);
        assertTrue(broken.stream().allMatch(c -> c.x() <= 10));
    }

    @Test
    void aBreakPathVetoIsNotCounted() {
        // Permission granted, but the loader event cancels every break (a protection mod).
        int count = BreakPlanner.execute(BreakPlanner.area(O, BreakPlanner.Axis.X), c -> true, c -> false, 64);
        assertEquals(0, count);
    }

    @Test
    void lanceCutsNothingOnDeniedGround() {
        List<BreakPlanner.Cell> broken = new ArrayList<>();
        assertEquals(0, BreakPlanner.execute(List.of(O), c -> false, c -> broken.add(c), 1));
        assertTrue(broken.isEmpty());
        assertEquals(1, BreakPlanner.execute(List.of(O), c -> true, c -> broken.add(c), 1));
        assertEquals(List.of(O), broken);
    }

    @Test
    void budgetStopsTheDrillEvenWhenPermitted() {
        List<BreakPlanner.Cell> broken = new ArrayList<>();
        assertEquals(3, BreakPlanner.execute(BreakPlanner.vein(O, c -> true, 64), c -> true, broken::add, 3));
        assertEquals(3, broken.size());
    }
}
