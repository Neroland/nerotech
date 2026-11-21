package za.co.neroland.nerotech.machine.extraction;

/**
 * The Survey Scanner's rules without Minecraft types (unit-tested): energy cost and cooldown of a
 * scan, charging from a Core energy source, and the coarse bands a survey reports. A survey reports a
 * 20 % band and a recovery state, never an exact count. No player data.
 */
public final class ScannerRules {

    private ScannerRules() {
    }

    /** What a scan attempt does. */
    public enum Outcome {
        SCANNED,
        COOLDOWN,
        NO_ENERGY
    }

    /** A deposit's recovery state as a survey shows it. */
    public enum Recovery {
        UNTOUCHED,
        RECOVERING,
        EXHAUSTED
    }

    /** Whether a scan at {@code now} may run, and why not. Cooldown wins over energy. */
    public static Outcome attempt(int stored, int cost, long now, long readyAt) {
        if (now < readyAt) {
            return Outcome.COOLDOWN;
        }
        return stored >= Math.max(0, cost) ? Outcome.SCANNED : Outcome.NO_ENERGY;
    }

    /** The scanner state after a scan at {@code now}: cost taken, next scan at {@code now + cooldown}. */
    public static ScannerState afterScan(ScannerState state, int cost, long now, int cooldownTicks) {
        return new ScannerState(Math.max(0, state.energy() - Math.max(0, cost)), now + Math.max(0, cooldownTicks));
    }

    /** NE a scanner holding {@code stored} of {@code capacity} can take from a source offering {@code available}. */
    public static int chargeAmount(int stored, int capacity, long available) {
        long room = Math.max(0, capacity - Math.max(0, stored));
        return (int) Math.max(0L, Math.min(room, available));
    }

    /**
     * The band (0-5) for {@code remaining} of {@code capacity}: 0 = empty, 1 = up to 20 %,
     * 2 = up to 40 %, ... 5 = above 80 %.
     */
    public static int band(int remaining, int capacity) {
        if (capacity <= 0 || remaining <= 0) {
            return 0;
        }
        long fifths = (Math.min(remaining, capacity) * 5L + capacity - 1L) / capacity;
        return (int) Math.max(1L, Math.min(5L, fifths));
    }

    /** The recovery state of a deposit with {@code remaining} of {@code capacity} units. */
    public static Recovery recovery(int remaining, int capacity) {
        if (remaining <= 0) {
            return Recovery.EXHAUSTED;
        }
        return remaining >= capacity ? Recovery.UNTOUCHED : Recovery.RECOVERING;
    }
}
