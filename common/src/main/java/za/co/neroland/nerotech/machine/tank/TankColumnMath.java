package za.co.neroland.nerotech.machine.tank;

import java.util.ArrayList;
import java.util.List;

/**
 * The Process Tank column model, free of Minecraft types so it is unit-testable.
 *
 * <p>Every tank physically owns a <b>share</b> of the column's contents (at most its own capacity).
 * The column shows one shared level — the sum of the shares over the sum of the capacities — and
 * settles bottom-first, like a liquid would: filling tops up the lowest tank with room, draining
 * empties the highest tank first. Because each share lives in exactly one tank, merging, splitting
 * and breaking a column can only <i>move</i> amounts between tanks; every operation here returns
 * enough to prove the total is conserved (see {@link Removal}).
 *
 * <p>Shares are indexed bottom-up. All amounts are millibuckets.
 */
public final class TankColumnMath {

    /** The most tanks one column joins; a taller stack splits into several columns. */
    public static final int MAX_COLUMN = 16;

    private TankColumnMath() {
    }

    /** Sum of the shares. */
    public static long total(long[] shares) {
        long sum = 0L;
        for (long share : shares) {
            sum += share;
        }
        return sum;
    }

    /**
     * Redistribute the total bottom-first: every tank from the bottom is filled to {@code capacity}
     * until the total is placed. The total is unchanged (anything above the column's capacity — which
     * cannot happen through {@link #fill} — stays on the top tank rather than vanishing).
     */
    public static long[] settle(long[] shares, long capacity) {
        long remaining = total(shares);
        long[] out = new long[shares.length];
        for (int i = 0; i < out.length; i++) {
            long put = Math.min(capacity, remaining);
            out[i] = put;
            remaining -= put;
        }
        if (remaining > 0 && out.length > 0) {
            out[out.length - 1] += remaining;
        }
        return out;
    }

    /** Insert up to {@code amount} bottom-first into {@code shares} (mutated unless simulating). */
    public static long fill(long[] shares, long capacity, long amount, boolean simulate) {
        if (amount <= 0) {
            return 0L;
        }
        long left = amount;
        for (int i = 0; i < shares.length && left > 0; i++) {
            long room = Math.max(0L, capacity - shares[i]);
            long put = Math.min(room, left);
            if (!simulate) {
                shares[i] += put;
            }
            left -= put;
        }
        return amount - left;
    }

    /** Remove up to {@code amount} top-first from {@code shares} (mutated unless simulating). */
    public static long drain(long[] shares, long amount, boolean simulate) {
        if (amount <= 0) {
            return 0L;
        }
        long left = amount;
        for (int i = shares.length - 1; i >= 0 && left > 0; i--) {
            long take = Math.min(Math.max(0L, shares[i]), left);
            if (!simulate) {
                shares[i] -= take;
            }
            left -= take;
        }
        return amount - left;
    }

    /**
     * Split a contiguous bottom-up stack of tanks into columns: a column grows upward while the next
     * tank's medium is compatible with the column's medium so far and the column has fewer than
     * {@code max} tanks. Returns {@code [start, end)} index pairs, bottom-up.
     */
    public static List<int[]> partition(List<TankMedium> stack, int max) {
        List<int[]> columns = new ArrayList<>();
        int start = 0;
        TankMedium medium = TankMedium.EMPTY;
        for (int i = 0; i < stack.size(); i++) {
            TankMedium next = stack.get(i) == null ? TankMedium.EMPTY : stack.get(i);
            boolean fits = i - start < max && medium.compatible(next);
            if (i > start && !fits) {
                columns.add(new int[] {start, i});
                start = i;
                medium = TankMedium.EMPTY;
            }
            medium = medium.or(next);
        }
        if (!stack.isEmpty()) {
            columns.add(new int[] {start, stack.size()});
        }
        return columns;
    }

    /** The column containing {@code index}, from {@link #partition}'s result. */
    public static int[] columnOf(List<int[]> columns, int index) {
        for (int[] column : columns) {
            if (index >= column[0] && index < column[1]) {
                return column;
            }
        }
        return new int[] {index, index + 1};
    }

    /**
     * Where a removed tank's share goes: first into the remaining tanks of its column below it (bottom
     * up), then above it, each up to {@code capacity}; whatever still does not fit is
     * {@link Removal#carried} away on the dropped tank item. {@code below} and {@code above} are
     * mutated with what they received.
     */
    public static Removal remove(long share, long[] below, long[] above, long capacity) {
        long movedBelow = fill(below, capacity, share, false);
        long movedAbove = fill(above, capacity, share - movedBelow, false);
        return new Removal(movedBelow, movedAbove, share - movedBelow - movedAbove);
    }

    /** The outcome of {@link #remove}: {@code movedBelow + movedAbove + carried == share}. */
    public record Removal(long movedBelow, long movedAbove, long carried) {
    }

    /** Comparator strength 0–15 for a fill level; at least 1 while anything is stored. */
    public static int comparatorSignal(long amount, long capacity) {
        if (amount <= 0 || capacity <= 0) {
            return 0;
        }
        return Math.max(1, (int) Math.min(15L, amount * 15L / capacity));
    }
}
