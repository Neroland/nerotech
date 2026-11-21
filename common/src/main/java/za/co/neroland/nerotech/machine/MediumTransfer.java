package za.co.neroland.nerotech.machine;

import za.co.neroland.nerolandcore.sideconfig.SideMode;

/**
 * The pure step behind a machine's adjacent fluid and gas exchange (no pipes): how much moves between
 * two stores, and which faces a side mode lets it move through. Pure JVM, no game classes beyond Core's
 * {@link SideMode} enum, so the rules are locked by {@code MediumTransferTest}.
 *
 * <p><b>Conservation.</b> {@link #move} simulates the drain and the fill first, then drains exactly what
 * the sink said it would take and fills that; if a store's real answer differs from its simulated one,
 * the shortfall goes back to the source ({@link Store#refund}), so a transfer never creates or destroys
 * medium.
 *
 * <p><b>Side modes.</b> A machine hands its output out of any face that lets it out (OUTPUT, I/O, PUSH)
 * and draws input in through any face that takes input automatically (INPUT, I/O). The Process Tank is
 * passive storage: machines push into it and pull from it, and it pushes out on its own only through a
 * face the player set to PUSH. A DISABLED face moves nothing either way.
 */
public final class MediumTransfer {

    /** Ticks between exchanges (spread by a per-machine phase): the step is budgeted, not per tick. */
    public static final int INTERVAL_TICKS = 10;

    /** mB per face per exchange (fluid and gas alike): 100 mB a tick on average. */
    public static final long PER_FACE_BUDGET_MB = 1_000L;

    private MediumTransfer() {
    }

    /** One side of a transfer, already bound to the medium being moved. */
    public interface Store {
        /** Remove up to {@code max}; @return the amount removed (or that would be, when simulating). */
        long drain(long max, boolean simulate);

        /** Add up to {@code amount}; @return the amount accepted (or that would be, when simulating). */
        long fill(long amount, boolean simulate);

        /** Put back {@code amount} that left this store but did not land (bypasses input filters). */
        default void refund(long amount) {
            fill(amount, false);
        }
    }

    /** Whether this exchange cycle is due for a machine with {@code phase}. */
    public static boolean due(long gameTime, int phase) {
        return Math.floorMod(gameTime + phase, (long) INTERVAL_TICKS) == 0L;
    }

    /** A machine pushes its output out of any face that lets medium out. */
    public static boolean machinePushes(SideMode mode) {
        return mode != null && mode.canExtract();
    }

    /** A machine pulls its input in through any face that takes input automatically. */
    public static boolean machinePulls(SideMode mode) {
        return mode != null && mode.autoInputs();
    }

    /** The passive Process Tank pushes only through a face set to PUSH, and never pulls. */
    public static boolean tankPushes(SideMode mode) {
        return mode == SideMode.PUSH;
    }

    /**
     * Move up to {@code budget} from {@code from} to {@code to}.
     *
     * @return the amount that left {@code from} and landed in {@code to}
     */
    public static long move(Store from, Store to, long budget) {
        if (from == null || to == null || from == to || budget <= 0L) {
            return 0L;
        }
        long offer = Math.min(budget, from.drain(budget, true));
        if (offer <= 0L) {
            return 0L;
        }
        long room = Math.min(offer, to.fill(offer, true));
        if (room <= 0L) {
            return 0L;
        }
        long taken = Math.min(room, from.drain(room, false));
        if (taken <= 0L) {
            return 0L;
        }
        long put = Math.max(0L, Math.min(taken, to.fill(taken, false)));
        if (put < taken) {
            from.refund(taken - put);
        }
        return put;
    }

    /**
     * Offer {@code own} to each face's neighbour whose mode passes {@code rule}, up to
     * {@code perFaceBudget} per face, pushing ({@code pushing}) or pulling. {@code modes} and
     * {@code neighbours} are indexed by face; a null neighbour is no store there.
     *
     * @return total moved
     */
    public static long exchange(Store own, SideMode[] modes, Store[] neighbours, long perFaceBudget,
            java.util.function.Predicate<SideMode> rule, boolean pushing) {
        long total = 0L;
        int faces = Math.min(modes.length, neighbours.length);
        for (int i = 0; i < faces; i++) {
            Store other = neighbours[i];
            if (other == null || !rule.test(modes[i])) {
                continue;
            }
            total += pushing ? move(own, other, perFaceBudget) : move(other, own, perFaceBudget);
        }
        return total;
    }
}
