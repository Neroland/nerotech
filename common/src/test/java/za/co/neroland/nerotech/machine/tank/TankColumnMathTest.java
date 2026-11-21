package za.co.neroland.nerotech.machine.tank;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

/**
 * Locks the Process Tank column model: fills settle bottom-first, drains take from the top, columns
 * partition by medium and the 16-tank cap, and merging, splitting and breaking a tank only ever move
 * medium — the total before always equals the total after plus what leaves on the dropped item.
 * Pure JVM — no game bootstrap.
 */
class TankColumnMathTest {

    private static final long CAP = 1_000L;
    private static final TankMedium WATER = TankMedium.fluid("minecraft:water");
    private static final TankMedium LAVA = TankMedium.fluid("minecraft:lava");
    private static final TankMedium HYDROGEN = TankMedium.gas("nerotech:hydrogen");

    @Test
    void fillGoesBottomFirstAndStopsAtCapacity() {
        long[] shares = new long[3];
        assertEquals(1_500L, TankColumnMath.fill(shares, CAP, 1_500L, false));
        assertArrayEquals(new long[] {1_000L, 500L, 0L}, shares);
        assertEquals(1_500L, TankColumnMath.fill(shares, CAP, 2_000L, false), "only the room left is accepted");
        assertArrayEquals(new long[] {1_000L, 1_000L, 1_000L}, shares);
        assertEquals(0L, TankColumnMath.fill(shares, CAP, 1L, false));
    }

    @Test
    void simulateChangesNothing() {
        long[] shares = {200L, 0L};
        assertEquals(1_800L, TankColumnMath.fill(shares, CAP, 5_000L, true));
        assertEquals(200L, TankColumnMath.drain(shares, 5_000L, true));
        assertArrayEquals(new long[] {200L, 0L}, shares);
    }

    @Test
    void drainTakesFromTheTop() {
        long[] shares = {1_000L, 1_000L, 300L};
        assertEquals(800L, TankColumnMath.drain(shares, 800L, false));
        assertArrayEquals(new long[] {1_000L, 500L, 0L}, shares);
        assertEquals(1_500L, TankColumnMath.drain(shares, 9_999L, false));
        assertEquals(0L, TankColumnMath.total(shares));
    }

    @Test
    void settlePoursDownAndConserves() {
        long[] merged = {0L, 700L, 1_000L, 200L};
        long[] settled = TankColumnMath.settle(merged, CAP);
        assertArrayEquals(new long[] {1_000L, 900L, 0L, 0L}, settled);
        assertEquals(TankColumnMath.total(merged), TankColumnMath.total(settled));
    }

    @Test
    void settleNeverDropsAnOverfill() {
        long[] over = {1_000L, 1_500L};
        assertEquals(2_500L, TankColumnMath.total(TankColumnMath.settle(over, CAP)));
    }

    @Test
    void partitionSplitsOnMediumAndCap() {
        List<TankMedium> stack = List.of(WATER, TankMedium.EMPTY, WATER, LAVA, TankMedium.EMPTY, HYDROGEN);
        List<int[]> columns = TankColumnMath.partition(stack, TankColumnMath.MAX_COLUMN);
        assertEquals(3, columns.size());
        assertArrayEquals(new int[] {0, 3}, columns.get(0), "water, empty, water share a column");
        assertArrayEquals(new int[] {3, 5}, columns.get(1), "lava starts its own; the empty above joins it");
        assertArrayEquals(new int[] {5, 6}, columns.get(2), "a gas never joins a liquid column");

        List<TankMedium> tall = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            tall.add(TankMedium.EMPTY);
        }
        List<int[]> capped = TankColumnMath.partition(tall, TankColumnMath.MAX_COLUMN);
        assertArrayEquals(new int[] {0, 16}, capped.get(0));
        assertArrayEquals(new int[] {16, 20}, capped.get(1));
        assertArrayEquals(new int[] {16, 20}, TankColumnMath.columnOf(capped, 17));
    }

    @Test
    void mediumCompatibility() {
        assertTrue(TankMedium.EMPTY.compatible(WATER));
        assertTrue(WATER.compatible(TankMedium.fluid("minecraft:water")));
        assertFalse(WATER.compatible(LAVA));
        assertFalse(WATER.compatible(TankMedium.gas("minecraft:water")), "a gas and a fluid never mix");
        assertEquals(TankMedium.EMPTY, new TankMedium(TankMedium.Kind.FLUID, ""), "no id means empty");
        assertEquals(TankMedium.Kind.GAS, TankMedium.Kind.parse("GAS"));
        assertEquals(TankMedium.Kind.NONE, TankMedium.Kind.parse("plasma"));
    }

    @Test
    void breakingAMiddleTankMovesItsShareAndCreatesNothing() {
        // Column of 4, 2.5 tanks full, settled: [1000, 1000, 500, 0]. Break index 1.
        long[] below = {1_000L};
        long[] above = {500L, 0L};
        long before = 1_000L + 1_000L + 500L;
        TankColumnMath.Removal removal = TankColumnMath.remove(1_000L, below, above, CAP);
        assertEquals(0L, removal.movedBelow(), "the tank below was already full");
        assertEquals(1_000L, removal.movedAbove());
        assertEquals(0L, removal.carried(), "there was room, so nothing leaves on the item");
        assertEquals(before, TankColumnMath.total(below) + TankColumnMath.total(above) + removal.carried());
    }

    @Test
    void breakingATankOfAFullColumnCarriesItsShare() {
        long[] below = {1_000L, 1_000L};
        long[] above = {1_000L};
        TankColumnMath.Removal removal = TankColumnMath.remove(1_000L, below, above, CAP);
        assertEquals(1_000L, removal.carried(), "a full column has no room: the share leaves on the item");
        assertEquals(4_000L, TankColumnMath.total(below) + TankColumnMath.total(above) + removal.carried());
    }

    @Test
    void randomMergeSplitAndBreakConserveTotals() {
        Random random = new Random(80L);
        for (int round = 0; round < 500; round++) {
            int size = 1 + random.nextInt(TankColumnMath.MAX_COLUMN);
            long[] shares = new long[size];
            TankColumnMath.fill(shares, CAP, random.nextInt((int) (size * CAP) + 1), false);
            long total = TankColumnMath.total(shares);

            // Merge with a second column and settle: nothing created or lost.
            long[] other = new long[1 + random.nextInt(4)];
            TankColumnMath.fill(other, CAP, random.nextInt((int) (other.length * CAP) + 1), false);
            long[] merged = new long[shares.length + other.length];
            System.arraycopy(shares, 0, merged, 0, shares.length);
            System.arraycopy(other, 0, merged, shares.length, other.length);
            long mergedTotal = total + TankColumnMath.total(other);
            merged = TankColumnMath.settle(merged, CAP);
            assertEquals(mergedTotal, TankColumnMath.total(merged));

            // Break a random tank: what remains plus what is carried equals what was there.
            int index = random.nextInt(merged.length);
            long[] below = java.util.Arrays.copyOfRange(merged, 0, index);
            long[] above = java.util.Arrays.copyOfRange(merged, index + 1, merged.length);
            TankColumnMath.Removal removal = TankColumnMath.remove(merged[index], below, above, CAP);
            assertEquals(merged[index], removal.movedBelow() + removal.movedAbove() + removal.carried());
            assertEquals(mergedTotal, TankColumnMath.total(below) + TankColumnMath.total(above) + removal.carried());
            for (long share : below) {
                assertTrue(share >= 0 && share <= CAP);
            }
            for (long share : above) {
                assertTrue(share >= 0 && share <= CAP);
            }
        }
    }

    @Test
    void comparatorSignal() {
        assertEquals(0, TankColumnMath.comparatorSignal(0L, 16_000L));
        assertEquals(1, TankColumnMath.comparatorSignal(1L, 16_000L), "anything stored reads at least 1");
        assertEquals(15, TankColumnMath.comparatorSignal(16_000L, 16_000L));
        assertEquals(7, TankColumnMath.comparatorSignal(8_000L, 16_000L));
    }
}
