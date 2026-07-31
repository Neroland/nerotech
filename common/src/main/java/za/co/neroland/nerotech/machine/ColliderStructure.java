package za.co.neroland.nerotech.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerotech.registry.ModBlocks;

/**
 * Particle Collider multiblock validation (Stage B). The collider is a <b>horizontal hollow square
 * ring</b> of {@code accelerator_coil} at the Collider Core's own Y level, in one of two outer
 * sizes — 5×5 or 7×7 — with the core occupying exactly one ring position (edge or corner), every
 * other ring position a coil (corners included) and every interior position air.
 *
 * <p>Ring size is the throughput axis: the 5×5 loop runs at 1× and the 7×7 at 2×, i.e. the larger
 * loop halves the operation time (see {@link #speedPermille(int)}).
 *
 * <p>Validation is a bounded scan run by the core on a cadence (and once immediately after a
 * neighbour change) — never a per-tick scan. For a candidate size {@code N} only the origins that
 * put the core on the perimeter are tried (≤ 4N of them), and each trial reads only the ring
 * perimeter plus the interior, so the whole pass stays in the low hundreds of block reads even at
 * 7×7. Chunks are never loaded by validation: an unloaded corner simply reports "unformed" until
 * the area is back.
 */
public final class ColliderStructure {

    /** Candidate ring sizes, largest first so a 7×7 loop is never mis-detected as a 5×5. */
    private static final int[] SIZES = {7, 5};

    private ColliderStructure() {
    }

    /** A formed ring: its outer edge size and the minimum (north-west) corner at the core's Y. */
    public record Ring(int size, BlockPos min) {

        /** Throughput multiplier in permille (5×5 → 1000, 7×7 → 2000). */
        public int speedPermille() {
            return ColliderStructure.speedPermille(this.size);
        }
    }

    /**
     * Throughput multiplier (permille) for a ring size: the 7×7 loop gives the beam twice the
     * path, so it completes an operation in half the time. Sizes the validator never produces
     * fall back to 1×.
     */
    public static int speedPermille(int size) {
        return size == 7 ? 2_000 : 1_000;
    }

    /** Validate the largest formed ring around a core, or {@code null} when unformed. */
    @Nullable
    public static Ring validate(Level level, BlockPos core) {
        for (int size : SIZES) {
            Ring ring = check(level, core, size);
            if (ring != null) {
                return ring;
            }
        }
        return null;
    }

    /**
     * Try every ring origin of edge {@code size} that puts the core on the perimeter. The core sits
     * on the west or east edge (x fixed) or on the north or south edge (z fixed), which leaves at
     * most {@code 4 * size} origins — enumerated here rather than sweeping the whole neighbourhood.
     */
    @Nullable
    private static Ring check(Level level, BlockPos core, int size) {
        int y = core.getY();
        for (int offset = 0; offset < size; offset++) {
            // Core on the west edge / on the east edge (x pinned, z slides along the edge).
            Ring west = ring(level, core, size, core.getX(), core.getZ() - offset, y);
            if (west != null) {
                return west;
            }
            Ring east = ring(level, core, size, core.getX() - size + 1, core.getZ() - offset, y);
            if (east != null) {
                return east;
            }
            // Core on the north edge / on the south edge (z pinned, x slides along the edge).
            Ring north = ring(level, core, size, core.getX() - offset, core.getZ(), y);
            if (north != null) {
                return north;
            }
            Ring south = ring(level, core, size, core.getX() - offset, core.getZ() - size + 1, y);
            if (south != null) {
                return south;
            }
        }
        return null;
    }

    /** Validate one concrete ring footprint: perimeter all coils (bar the core) + air inside. */
    @Nullable
    private static Ring ring(Level level, BlockPos core, int size, int minX, int minZ, int y) {
        int maxX = minX + size - 1;
        int maxZ = minZ + size - 1;
        BlockPos min = new BlockPos(minX, y, minZ);
        BlockPos max = new BlockPos(maxX, y, maxZ);
        if (!level.hasChunksAt(min, max)) {
            return null; // never force-load chunks for validation
        }
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                boolean perimeter = x == minX || x == maxX || z == minZ || z == maxZ;
                cursor.set(x, y, z);
                if (cursor.equals(core)) {
                    // The core must sit ON the ring, never floating in the beam cavity.
                    if (!perimeter) {
                        return null;
                    }
                    continue;
                }
                BlockState state = level.getBlockState(cursor);
                if (perimeter) {
                    // Corners are part of the perimeter, so this covers the "corners required" rule.
                    if (!state.is(ModBlocks.ACCELERATOR_COIL.get())) {
                        return null;
                    }
                } else if (!state.isAir()) {
                    return null;
                }
            }
        }
        return new Ring(size, min.immutable());
    }
}
