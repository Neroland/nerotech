package za.co.neroland.nerotech.item.tools;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Which extra blocks a Power Drill or Cutting Lance break takes, and the bounded order it breaks them in,
 * without Minecraft types (unit-tested). The planner only proposes cells: {@link #execute} asks a
 * <b>permission predicate per block</b> (the player's real break path in game, so claim and protection
 * mods can veto each one) and skips every denied cell; nothing is broken without that permission.
 *
 * <ul>
 * <li><b>Area</b>: the 3×3 plane through the first block, perpendicular to the face it was mined from.</li>
 * <li><b>Vein</b>: a breadth-first walk over the 26 neighbours of blocks that match the first one, capped at
 * {@link #VEIN_HARD_CAP} blocks including the first, and at most {@link #VEIN_RADIUS} blocks from it on
 * every axis, so a walk never reaches far into unloaded ground.</li>
 * </ul>
 */
public final class BreakPlanner {

    /** The most blocks one vein-mode break can ever take, whatever the config says. */
    public static final int VEIN_HARD_CAP = 64;
    /** The furthest a vein walk goes from the first block on any axis. */
    public static final int VEIN_RADIUS = 8;
    /** Blocks an area break proposes besides the first (a 3×3 plane). */
    public static final int AREA_EXTRA = 8;

    private BreakPlanner() {
    }

    /** A block position without Minecraft types. */
    public record Cell(int x, int y, int z) {

        Cell offset(int dx, int dy, int dz) {
            return new Cell(this.x + dx, this.y + dy, this.z + dz);
        }
    }

    /** The axis a block was mined along (the face normal's axis). */
    public enum Axis {
        X, Y, Z
    }

    /** The eight other cells of the 3×3 plane through {@code origin} perpendicular to {@code axis}. */
    public static List<Cell> area(Cell origin, Axis axis) {
        List<Cell> out = new ArrayList<>(AREA_EXTRA);
        for (int a = -1; a <= 1; a++) {
            for (int b = -1; b <= 1; b++) {
                if (a == 0 && b == 0) {
                    continue;
                }
                out.add(switch (axis) {
                    case X -> origin.offset(0, a, b);
                    case Y -> origin.offset(a, 0, b);
                    case Z -> origin.offset(a, b, 0);
                });
            }
        }
        return out;
    }

    /**
     * The vein around {@code origin} (excluded from the result): cells matching {@code sameVein}, found
     * breadth-first, at most {@code cap - 1} of them ({@code cap} is clamped to {@link #VEIN_HARD_CAP}).
     */
    public static List<Cell> vein(Cell origin, Predicate<Cell> sameVein, int cap) {
        int limit = Math.max(0, Math.min(VEIN_HARD_CAP, cap) - 1);
        List<Cell> out = new ArrayList<>();
        if (limit == 0) {
            return out;
        }
        Set<Cell> seen = new HashSet<>();
        seen.add(origin);
        Deque<Cell> queue = new ArrayDeque<>();
        queue.add(origin);
        while (!queue.isEmpty() && out.size() < limit) {
            Cell at = queue.poll();
            for (int dx = -1; dx <= 1 && out.size() < limit; dx++) {
                for (int dy = -1; dy <= 1 && out.size() < limit; dy++) {
                    for (int dz = -1; dz <= 1 && out.size() < limit; dz++) {
                        Cell next = at.offset(dx, dy, dz);
                        if (Math.abs(next.x - origin.x) > VEIN_RADIUS || Math.abs(next.y - origin.y) > VEIN_RADIUS
                                || Math.abs(next.z - origin.z) > VEIN_RADIUS || !seen.add(next)
                                || !sameVein.test(next)) {
                            continue;
                        }
                        out.add(next);
                        queue.add(next);
                    }
                }
            }
        }
        return out;
    }

    /** Breaks one permitted cell; returns whether it actually broke (the break path may still refuse). */
    @FunctionalInterface
    public interface Breaker {
        boolean breakCell(Cell cell);
    }

    /**
     * Break the planned {@code cells} in order, at most {@code budget} of them: each cell is first offered to
     * {@code permitted}; a denied cell is skipped and never reaches {@code breaker}. Returns how many broke.
     */
    public static int execute(List<Cell> cells, Predicate<Cell> permitted, Breaker breaker, int budget) {
        int broken = 0;
        for (Cell cell : cells) {
            if (broken >= budget) {
                break;
            }
            if (!permitted.test(cell)) {
                continue;
            }
            if (breaker.breakCell(cell)) {
                broken++;
            }
        }
        return broken;
    }
}
