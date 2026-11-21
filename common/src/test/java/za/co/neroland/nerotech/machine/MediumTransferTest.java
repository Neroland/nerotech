package za.co.neroland.nerotech.machine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

import za.co.neroland.nerolandcore.sideconfig.SideMode;

/**
 * Locks the adjacent fluid/gas exchange step: a transfer only ever moves medium (the total before equals
 * the total after, even when a store's real answer differs from its simulated one), it respects the
 * per-face budget, and side modes decide which faces push and pull. Pure JVM — no game bootstrap.
 */
class MediumTransferTest {

    /** A tank of one medium with a capacity; {@code lie} makes the real fill take less than simulated. */
    static final class Tank implements MediumTransfer.Store {
        long amount;
        final long capacity;
        long lie;

        Tank(long amount, long capacity) {
            this.amount = amount;
            this.capacity = capacity;
        }

        @Override
        public long drain(long max, boolean simulate) {
            long d = Math.max(0L, Math.min(max, this.amount));
            if (!simulate) {
                this.amount -= d;
            }
            return d;
        }

        @Override
        public long fill(long add, boolean simulate) {
            long f = Math.max(0L, Math.min(add, this.capacity - this.amount));
            if (!simulate) {
                f = Math.max(0L, f - this.lie);
                this.amount += f;
            }
            return f;
        }

        @Override
        public void refund(long back) {
            this.amount += back; // bypasses the capacity: a refund only returns what just left
        }
    }

    @Test
    void moveIsCappedByBudgetSourceAndRoom() {
        Tank from = new Tank(5_000L, 8_000L);
        Tank to = new Tank(0L, 1_500L);
        assertEquals(1_000L, MediumTransfer.move(from, to, 1_000L));
        assertEquals(500L, MediumTransfer.move(from, to, 1_000L), "only the room left");
        assertEquals(0L, MediumTransfer.move(from, to, 1_000L), "a full sink takes nothing");
        assertEquals(3_500L, from.amount);
        assertEquals(1_500L, to.amount);
        Tank empty = new Tank(0L, 1_000L);
        assertEquals(0L, MediumTransfer.move(empty, new Tank(0L, 1_000L), 1_000L));
        assertEquals(0L, MediumTransfer.move(from, to, 0L));
        assertEquals(0L, MediumTransfer.move(from, from, 1_000L), "never into itself");
    }

    @Test
    void aSinkThatTakesLessThanItPromisedLosesNothing() {
        Tank from = new Tank(2_000L, 2_000L);
        Tank to = new Tank(0L, 4_000L);
        to.lie = 300L;
        long moved = MediumTransfer.move(from, to, 1_000L);
        assertEquals(700L, moved);
        assertEquals(2_000L, from.amount + to.amount, "the shortfall went back to the source");
    }

    @Test
    void randomTransfersConserveTheTotal() {
        Random random = new Random(8_008L);
        for (int round = 0; round < 2_000; round++) {
            Tank a = new Tank(random.nextInt(5_000), 5_000L);
            Tank b = new Tank(random.nextInt(5_000), 5_000L);
            b.lie = random.nextInt(3) == 0 ? random.nextInt(400) : 0L;
            long before = a.amount + b.amount;
            long moved = MediumTransfer.move(a, b, random.nextInt(2_000));
            assertTrue(moved >= 0L);
            assertEquals(before, a.amount + b.amount);
            assertTrue(b.amount <= b.capacity);
        }
    }

    @Test
    void sideModesDecideWhichFacesMove() {
        assertFalse(MediumTransfer.machinePushes(SideMode.DISABLED));
        assertFalse(MediumTransfer.machinePushes(SideMode.INPUT));
        assertTrue(MediumTransfer.machinePushes(SideMode.OUTPUT));
        assertTrue(MediumTransfer.machinePushes(SideMode.PUSH));
        assertFalse(MediumTransfer.machinePulls(SideMode.DISABLED));
        assertTrue(MediumTransfer.machinePulls(SideMode.INPUT));
        assertFalse(MediumTransfer.machinePulls(SideMode.OUTPUT));
        assertFalse(MediumTransfer.machinePulls(SideMode.PUSH));
        for (SideMode mode : SideMode.values()) {
            assertEquals(mode == SideMode.PUSH, MediumTransfer.tankPushes(mode), "the tank is passive: " + mode);
            // I/O both pushes and pulls; nothing else does both.
            if (MediumTransfer.machinePushes(mode) && MediumTransfer.machinePulls(mode)) {
                assertTrue(mode.canInsert() && mode.canExtract(), "only an I/O face both pushes and pulls: " + mode);
            }
        }
    }

    @Test
    void exchangeSkipsFacesTheModesClose() {
        Tank own = new Tank(6_000L, 8_000L);
        SideMode[] modes = {SideMode.DISABLED, SideMode.INPUT, SideMode.OUTPUT, SideMode.PUSH,
                SideMode.OUTPUT, SideMode.DISABLED};
        Tank[] sinks = new Tank[6];
        for (int i = 0; i < 6; i++) {
            sinks[i] = new Tank(0L, 10_000L);
        }
        sinks[4] = null; // nothing on that face
        long moved = MediumTransfer.exchange(own, modes, sinks, 1_000L, MediumTransfer::machinePushes, true);
        assertEquals(2_000L, moved, "the OUTPUT and PUSH faces, 1,000 mB each");
        assertEquals(0L, sinks[0].amount);
        assertEquals(0L, sinks[1].amount);
        assertEquals(1_000L, sinks[2].amount);
        assertEquals(1_000L, sinks[3].amount);
        assertEquals(0L, sinks[5].amount);
        assertEquals(4_000L, own.amount);

        Tank input = new Tank(0L, 8_000L);
        Tank[] sources = {new Tank(3_000L, 8_000L), new Tank(3_000L, 8_000L), null, null, null, null};
        SideMode[] pullModes = {SideMode.INPUT, SideMode.OUTPUT, SideMode.INPUT, SideMode.INPUT,
                SideMode.INPUT, SideMode.INPUT};
        long pulled = MediumTransfer.exchange(input, pullModes, sources, 1_000L, MediumTransfer::machinePulls, false);
        assertEquals(1_000L, pulled, "only the INPUT face draws");
        assertEquals(2_000L, sources[0].amount);
        assertEquals(3_000L, sources[1].amount);
    }

    @Test
    void theExchangeIsBudgetedNotPerTick() {
        int due = 0;
        for (long t = 0; t < 200; t++) {
            if (MediumTransfer.due(t, 3)) {
                due++;
            }
        }
        assertEquals(200 / MediumTransfer.INTERVAL_TICKS, due);
        assertTrue(MediumTransfer.INTERVAL_TICKS > 1);
    }
}
