package za.co.neroland.nerotech.machine.utility;

/**
 * Pure arithmetic behind the Induction Charger and {@code api.WirelessCharging}: the bounded reach
 * test and the split of one request across the chargers that can serve it. No Minecraft types, so the
 * rules are unit-tested without a bootstrap.
 */
public final class ChargeMath {

    /** Charger pass cadence in ticks (machine contract, Budget rules: players are scanned every 20 ticks). */
    public static final int PASS_TICKS = 20;

    private ChargeMath() {
    }

    /** One charger's side of a request: what it can give now, and taking it. */
    public interface Source {
        /** Units this source can give right now (never negative). */
        long available();

        /** Take {@code amount} units (never more than {@link #available()} reported). */
        void take(long amount);
    }

    /**
     * Whether block {@code b} is inside the box of {@code radius} blocks around block {@code a} on every
     * axis (the charger's bounded reach). A negative radius reaches nothing.
     */
    public static boolean inBox(long ax, long ay, long az, long bx, long by, long bz, int radius) {
        if (radius < 0) {
            return false;
        }
        return Math.abs(ax - bx) <= radius && Math.abs(ay - by) <= radius && Math.abs(az - bz) <= radius;
    }

    /**
     * Draw up to {@code max} units from {@code sources} in order. Returns the total given, which is never
     * more than {@code max} nor more than the sources hold. With {@code simulate} nothing is taken.
     */
    public static long offer(long max, boolean simulate, Iterable<? extends Source> sources) {
        if (max <= 0) {
            return 0L;
        }
        long remaining = max;
        for (Source source : sources) {
            if (remaining <= 0) {
                break;
            }
            long give = Math.min(remaining, Math.max(0L, source.available()));
            if (give <= 0) {
                continue;
            }
            if (!simulate) {
                source.take(give);
            }
            remaining -= give;
        }
        return max - remaining;
    }

    /** {@code perTick} scaled by a speed factor, floored at 0 and saturated at {@code Long.MAX_VALUE / PASS_TICKS}. */
    public static long scaledPerTick(long perTick, double factor) {
        if (perTick <= 0 || !(factor > 0)) {
            return 0L;
        }
        double scaled = perTick * factor;
        long cap = Long.MAX_VALUE / PASS_TICKS;
        return scaled >= cap ? cap : (long) Math.floor(scaled);
    }
}
