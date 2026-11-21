package za.co.neroland.nerotech.machine.heat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

/** Run-cache rules: bounded resolve, invalidation, split/merge conserve heat, TTL, reseed. */
class HeatRunIndexTest {

    /** A line of pipes along x, packed as plain longs (x only). */
    static final class Line implements HeatRunIndex.Topology {
        final Map<Long, Integer> pipes = new HashMap<>();

        Line add(long from, long to, int level) {
            for (long x = from; x <= to; x++) {
                this.pipes.put(x, level);
            }
            return this;
        }

        @Override
        public boolean isPipe(long pos) {
            return this.pipes.containsKey(pos);
        }

        @Override
        public int level(long pos) {
            return this.pipes.getOrDefault(pos, 0);
        }

        @Override
        public long offset(long pos, int dir) {
            return switch (dir) {
                case 0 -> pos - 1;
                case 1 -> pos + 1;
                default -> Long.MIN_VALUE + dir; // other axes: never a pipe
            };
        }
    }

    @Test
    void resolveIsCachedAndBounded() {
        Line line = new Line().add(0, 99, 0);
        HeatRunIndex index = new HeatRunIndex();
        HeatRunIndex.Run run = index.resolve(0, line, 64, 1000, 0);
        assertNotNull(run);
        assertEquals(64, run.size(), "flood fill stops at the cap");
        assertSame(run, index.resolve(5, line, 64, 1000, 1), "a cached pipe returns its run, no rescan");
        HeatRunIndex.Run rest = index.resolve(80, line, 64, 1000, 1);
        assertEquals(36, rest.size(), "the remainder is its own run; runs never overlap");
        assertEquals(100, index.trackedPipes());
    }

    @Test
    void invalidationDropsTheRunAndKeepsHeat() {
        Line line = new Line().add(0, 9, 0);
        HeatRunIndex index = new HeatRunIndex();
        HeatRunIndex.Run run = index.resolve(0, line, 64, 1000, 0);
        run.heat(1000);
        index.invalidate(4, 10);
        assertNull(index.runAt(0), "every pipe of the run is released");
        assertEquals(1000, index.heldHeat(), "the heat waits as pending shares");
        HeatRunIndex.Run again = index.resolve(3, line, 64, 1000, 11);
        assertNotSame(run, again);
        assertEquals(1000, again.heat(), "the new run takes the shares back");
        assertEquals(3, again.anchor(), "the pipe that resolved is the anchor");
    }

    @Test
    void splitAndMergeNeverCreateHeat() {
        Line line = new Line().add(0, 9, 3);
        HeatRunIndex index = new HeatRunIndex();
        HeatRunIndex.Run run = index.resolve(0, line, 64, 1000, 0);
        run.heat(7003);
        // Break pipe 5: the run splits in two.
        line.pipes.remove(5L);
        index.remove(5, 1);
        HeatRunIndex.Run left = index.resolve(0, line, 64, 1000, 2);
        HeatRunIndex.Run right = index.resolve(9, line, 64, 1000, 2);
        assertEquals(5, left.size());
        assertEquals(4, right.size());
        assertTrue(left.heat() + right.heat() <= 7003, "a split divides heat, never adds");
        long split = left.heat() + right.heat();
        // Re-place it: the halves merge.
        line.pipes.put(5L, 0);
        index.invalidate(4, 3);
        index.invalidate(6, 3);
        HeatRunIndex.Run merged = index.resolve(5, line, 64, 1000, 4);
        assertEquals(10, merged.size());
        assertTrue(merged.heat() <= split, "a merge adds the halves' heat and nothing else");
    }

    @Test
    void pendingSharesExpire() {
        Line line = new Line().add(0, 3, 0);
        HeatRunIndex index = new HeatRunIndex();
        index.resolve(0, line, 64, 1000, 0).heat(400);
        index.invalidate(0, 0);
        line.pipes.clear();
        line.add(10, 10, 0);
        index.resolve(10, line, 64, 1000, HeatRunIndex.PENDING_TTL + 1);
        assertEquals(0, index.pendingCount(), "shares of broken pipes are dropped");
    }

    @Test
    void reloadSeedsFromBlockstateWithoutCreatingHeat() {
        Line line = new Line().add(0, 9, 2);
        HeatRunIndex index = new HeatRunIndex();
        HeatRunIndex.Run run = index.resolve(0, line, 64, 1000, 0);
        assertEquals(10L * HeatRunMath.seedForLevel(2, 1000), run.heat());
        assertTrue(run.heat() <= 10L * 500, "never more than the least heat level 2 means");
    }

    @Test
    void runsAgeOut() {
        Line line = new Line().add(0, 2, 0);
        HeatRunIndex index = new HeatRunIndex();
        HeatRunIndex.Run run = index.resolve(0, line, 64, 1000, 0);
        assertTrue(!HeatRunIndex.stale(run, HeatRunIndex.MAX_AGE - 1));
        assertTrue(HeatRunIndex.stale(run, HeatRunIndex.MAX_AGE));
    }
}
