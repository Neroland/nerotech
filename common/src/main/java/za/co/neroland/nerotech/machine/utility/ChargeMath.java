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

        /**
         * Take up to {@code amount} units (never more than {@link #available()} reported).
         *
         * @return the units really taken, which may be fewer than asked
         */
        long take(long amount);
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
                // Count only what the source really gave up: a source may clamp a single call.
                give = Math.max(0L, Math.min(give, source.take(give)));
            }
            remaining -= give;
        }
        return max - remaining;
    }

    /**
     * FE that {@code ne} NE pays for at {@code ratio} FE per NE, rounded down so a delivery never
     * outruns its payment. 0 for a non-positive amount or ratio.
     */
    public static long neToFeFloor(long ne, double ratio) {
        if (ne <= 0 || !(ratio > 0)) {
            return 0L;
        }
        double fe = Math.floor(ne * ratio + 1e-9);
        return fe >= Long.MAX_VALUE ? Long.MAX_VALUE : (long) fe;
    }

    /**
     * NE that delivering {@code fe} FE costs at {@code ratio} FE per NE, rounded up (at least 1 for any
     * non-zero delivery), so nothing is free at any ratio. 0 for a non-positive amount or ratio.
     */
    public static long feToNeCeil(long fe, double ratio) {
        if (fe <= 0 || !(ratio > 0)) {
            return 0L;
        }
        double ne = Math.ceil(fe / ratio - 1e-9);
        return Math.max(1L, ne >= Long.MAX_VALUE ? Long.MAX_VALUE : (long) ne);
    }

    /**
     * NE an NE-denominated item may accept from an offer of {@code maxFe} FE: rounded down, so the FE it
     * then reports ({@link #acceptedFe}) never exceeds the offer.
     */
    public static long feOfferToNe(long maxFe, double ratio) {
        if (maxFe <= 0 || !(ratio > 0)) {
            return 0L;
        }
        double ne = Math.floor(maxFe / ratio + 1e-9);
        return ne >= Long.MAX_VALUE ? Long.MAX_VALUE : (long) ne;
    }

    /**
     * FE to report for {@code ne} NE accepted by an NE-denominated item: rounded up and capped at
     * {@code maxFe}, so the charger is paid at least what the item gained.
     */
    public static long acceptedFe(long ne, double ratio, long maxFe) {
        if (ne <= 0 || !(ratio > 0) || maxFe <= 0) {
            return 0L;
        }
        double fe = Math.ceil(ne * ratio - 1e-9);
        return Math.min(maxFe, fe >= Long.MAX_VALUE ? Long.MAX_VALUE : (long) fe);
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
