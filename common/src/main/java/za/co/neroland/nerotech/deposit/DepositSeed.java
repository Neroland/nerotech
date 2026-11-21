package za.co.neroland.nerotech.deposit;

import java.util.List;
import java.util.SplittableRandom;

import org.jetbrains.annotations.Nullable;

/**
 * Pure, deterministic deposit seeding: one virtual deposit per chunk, picked from the dimension's table by
 * a {@link SplittableRandom} keyed on the world seed, dimension id, chunk x/z and {@link #SEED_VERSION}.
 * Nothing is stored for an untapped chunk; the Survey Scanner and the Deep Core Drill both re-derive it.
 *
 * <p>The function is frozen at release: changing it reshuffles every world's deposits, so any change must
 * bump {@link #SEED_VERSION} (the store drops depletion records written under another version).
 */
public final class DepositSeed {

    /** Version of the seeding function; persisted in {@code DepositState}. */
    public static final int SEED_VERSION = 1;

    private DepositSeed() {
    }

    /** One chunk's deposit: which table entry, the ore tag id, its capacity (units) and richness (permille). */
    public record Deposit(int entryIndex, String ore, int capacity, int richness) {
    }

    /** The chunk's deposit, or {@code null} when the table has no usable entry. */
    @Nullable
    public static Deposit of(long worldSeed, String dimensionId, int chunkX, int chunkZ,
            List<DepositTable.Entry> entries) {
        int total = 0;
        for (DepositTable.Entry e : entries) {
            total += Math.max(0, e.weight());
        }
        if (total <= 0) {
            return null;
        }
        SplittableRandom rng = new SplittableRandom(key(worldSeed, dimensionId, chunkX, chunkZ));
        int pick = rng.nextInt(total);
        for (int i = 0; i < entries.size(); i++) {
            DepositTable.Entry e = entries.get(i);
            int w = Math.max(0, e.weight());
            if (pick < w) {
                int capacity = between(rng, e.capacityMin(), e.capacityMax());
                int richness = between(rng, e.richnessMin(), e.richnessMax());
                return new Deposit(i, e.ore(), Math.max(1, capacity), Math.max(0, richness));
            }
            pick -= w;
        }
        return null; // unreachable: pick < total
    }

    /** The 64-bit key for one chunk (exposed for tests). */
    public static long key(long worldSeed, String dimensionId, int chunkX, int chunkZ) {
        long h = mix(worldSeed ^ (0x9E3779B97F4A7C15L * SEED_VERSION));
        h = mix(h ^ dimensionId.hashCode()); // String.hashCode is specified, so stable across JVMs
        h = mix(h ^ (chunkX & 0xFFFFFFFFL));
        return mix(h ^ ((long) chunkZ << 32));
    }

    private static int between(SplittableRandom rng, int min, int max) {
        int lo = Math.min(min, max);
        int hi = Math.max(min, max);
        return lo == hi ? lo : lo + rng.nextInt(hi - lo + 1);
    }

    /** SplitMix64 finaliser. */
    private static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }
}
