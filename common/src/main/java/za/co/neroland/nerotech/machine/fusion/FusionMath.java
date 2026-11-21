package za.co.neroland.nerotech.machine.fusion;

/**
 * The fusion reactor's burn and heat arithmetic, pure so the balance and the zero-addition regression are
 * unit-testable without a level: heat per burning tick, the Europium Flux Lattice band widening and its
 * cap, and the gas-fed charge (deuterium and helium-3 in a fixed ratio, drawn across Fuel Injectors).
 */
public final class FusionMath {

    /** The burning-tier value of a gas-fed charge: one above the top fuel cell (antimatter, tier 4). */
    public static final int GAS_TIER = 5;
    /** The top fuel-cell tier; burns at or above it pay the antimatter heat surcharge. */
    public static final int TOP_CELL_TIER = 4;
    /** Extra multiples of the base heat rate while a top-tier charge burns (unchanged from the reactor). */
    public static final int TOP_TIER_HEAT_SURCHARGE = 2;

    private FusionMath() {
    }

    /**
     * Heat added per burning tick before any lattice: {@code base × (3 + shellTier)}, plus
     * {@value #TOP_TIER_HEAT_SURCHARGE} more multiples for a tier-4 or gas-fed charge. This is the reactor's
     * formula as it was before the lattice existed.
     */
    public static int baseHeat(int heatPerOperation, int shellTier, int burningTier) {
        return heatPerOperation * (3 + shellTier + (burningTier >= TOP_CELL_TIER ? TOP_TIER_HEAT_SURCHARGE : 0));
    }

    /** Lattices that count: never negative, never above {@code cap}. */
    public static int countedLattices(int lattices, int cap) {
        return Math.max(0, Math.min(lattices, Math.max(0, cap)));
    }

    /**
     * Heat after the lattice: each counted lattice removes {@code reductionPermille} of the base heat.
     * With no lattice the result is exactly {@code baseHeat}; with any lattice a positive heat never drops
     * below 1 (a burning reactor always warms).
     */
    public static int latticeHeat(int baseHeat, int lattices, int cap, int reductionPermille) {
        int counted = countedLattices(lattices, cap);
        if (counted == 0 || baseHeat <= 0) {
            return baseHeat;
        }
        long permille = Math.min(1000L, (long) counted * Math.max(0, reductionPermille));
        long removed = baseHeat * permille / 1000L;
        return (int) Math.max(1L, baseHeat - removed);
    }

    /** Heat per burning tick with lattices: {@link #latticeHeat} of {@link #baseHeat}. */
    public static int heatPerTick(int heatPerOperation, int shellTier, int burningTier, int lattices, int cap,
            int reductionPermille) {
        return latticeHeat(baseHeat(heatPerOperation, shellTier, burningTier), lattices, cap, reductionPermille);
    }

    /** Deuterium (mB) one gas charge of {@code ticks} burns. */
    public static long deuteriumPerCharge(int ticks, int deuteriumPerTick) {
        return (long) Math.max(0, ticks) * Math.max(0, deuteriumPerTick);
    }

    /** Helium-3 (mB) one gas charge of {@code ticks} burns. */
    public static long helium3PerCharge(int ticks, int helium3PerTick) {
        return (long) Math.max(0, ticks) * Math.max(0, helium3PerTick);
    }

    /**
     * Whether the injectors together hold one full charge of <b>both</b> gases in the configured ratio. A
     * surplus of one gas never covers a shortage of the other, and a zero-length charge never ignites.
     */
    public static boolean canIgniteGas(long deuterium, long helium3, int ticks, int deuteriumPerTick,
            int helium3PerTick) {
        long needD = deuteriumPerCharge(ticks, deuteriumPerTick);
        long needHe = helium3PerCharge(ticks, helium3PerTick);
        return ticks > 0 && needD > 0 && needHe > 0 && deuterium >= needD && helium3 >= needHe;
    }

    /** Whether a shell of edge {@code shellSize} can contain the gas-fed burn. */
    public static boolean shellTakesGas(int shellSize, int minShellSize) {
        return shellSize > 0 && shellSize >= minShellSize;
    }

    /**
     * How much to draw from each tank, in order, to take {@code need} in total (greedy, first tank first).
     * Returns all zeros when the tanks together hold less than {@code need}: a charge is all or nothing.
     */
    public static long[] drawPlan(long[] amounts, long need) {
        long[] plan = new long[amounts.length];
        long total = 0L;
        for (long a : amounts) {
            total += Math.max(0L, a);
        }
        if (need <= 0 || total < need) {
            return plan;
        }
        long left = need;
        for (int i = 0; i < amounts.length && left > 0; i++) {
            long take = Math.min(Math.max(0L, amounts[i]), left);
            plan[i] = take;
            left -= take;
        }
        return plan;
    }

    /** Output for one burning tick; a gas-fed charge scales by {@code gasOutputPermille}, a cell burn is unchanged. */
    public static long gasScaledOutput(long cellOutput, int burningTier, int gasOutputPermille) {
        if (burningTier != GAS_TIER) {
            return cellOutput;
        }
        return cellOutput * Math.max(0, gasOutputPermille) / 1000L;
    }
}
