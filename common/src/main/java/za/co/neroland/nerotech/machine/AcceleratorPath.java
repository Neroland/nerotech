package za.co.neroland.nerotech.machine;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerotech.machine.AcceleratorGuideBlock.Bend;
import za.co.neroland.nerotech.machine.AcceleratorMath.Heading;
import za.co.neroland.nerotech.registry.ModBlocks;

/**
 * A traced accelerator beam line: the ordered run of straight segments from an Accelerator Controller
 * out through its Accelerator Guide Coils and — when the builder got it right — back into the
 * controller.
 *
 * <p>Tracing starts at the controller's {@code FACING} heading and ray-marches up to
 * {@code acceleratorMaxGap} blocks looking for the next guide at the same Y. At each guide the
 * heading turns by that guide's {@link Bend} and the march repeats. It ends when the beam re-enters
 * the controller (a {@link #closed()} loop — the only shape that can circulate a particle), when no
 * guide is found within the gap (an OPEN line: the particle would fly off the end), or when the
 * {@code acceleratorMaxGuides} safety cap is hit.
 *
 * <p>Cost is O(guides × gap) block reads, and it runs only on invalidation — the controller caches
 * the result and re-traces on a 100-tick cadence, on its own neighbour changes, and at every launch.
 * Chunks are never force-loaded: an unloaded stretch simply reads as an open line until it is back.
 */
public record AcceleratorPath(List<Segment> segments, boolean closed, int guides, double length) {

    /**
     * One straight run of the beam.
     *
     * @param start   the node the run leaves (the controller, or the previous guide)
     * @param end     the node the run reaches (a guide, the controller, or — on an open line — the
     *                point the beam is lost at)
     * @param heading the direction travelled
     * @param steps   whole blocks marched
     * @param length  {@code steps} in blocks (√2 per step on a diagonal)
     * @param bend    the bend applied at {@code end}; {@code null} when {@code end} is the controller
     *                or the point of loss
     */
    public record Segment(BlockPos start, BlockPos end, Heading heading, int steps, double length,
            @Nullable Bend bend) {

        /**
         * The heading the beam LEAVES {@code end} on — {@link #heading} with {@link #bend} applied (so
         * the same turn {@link #trace} takes). Used by the controller to point the guide's top-face
         * arrow; on a segment with no bend (the controller, or the point of loss) it is just
         * {@link #heading}.
         */
        public Heading outgoing() {
            if (this.bend == null) {
                return this.heading;
            }
            return switch (this.bend) {
                case LEFT -> this.heading.left();
                case RIGHT -> this.heading.right();
                case STRAIGHT -> this.heading;
            };
        }
    }

    /** The heading a controller injects along, from its horizontal {@code FACING}. */
    public static Heading headingOf(Direction facing) {
        return switch (facing) {
            case EAST -> Heading.EAST;
            case SOUTH -> Heading.SOUTH;
            case WEST -> Heading.WEST;
            default -> Heading.NORTH;
        };
    }

    /**
     * Trace the beam line out of {@code controller}. Never returns null: an accelerator with no
     * guides at all is simply an open path of one lost segment.
     */
    public static AcceleratorPath trace(Level level, BlockPos controller, Direction facing,
            int maxGap, int maxGuides) {
        Heading heading = headingOf(facing);
        List<Segment> segments = new ArrayList<>();
        BlockPos node = controller;
        double total = 0.0D;
        int guides = 0;

        while (guides < Math.max(1, maxGuides)) {
            Hit hit = march(level, node, controller, heading, maxGap);
            double length = AcceleratorMath.segmentLength(heading, hit.steps());
            segments.add(new Segment(node, hit.pos(), heading, hit.steps(), length, hit.bend()));
            total += length;
            if (hit.closesLoop()) {
                return new AcceleratorPath(List.copyOf(segments), true, guides, total);
            }
            Bend bend = hit.bend();
            if (bend == null) {
                // Nothing found within the gap (or an unloaded stretch): the beam runs off the end.
                return new AcceleratorPath(List.copyOf(segments), false, guides, total);
            }
            guides++;
            node = hit.pos();
            heading = switch (bend) {
                case LEFT -> heading.left();
                case RIGHT -> heading.right();
                case STRAIGHT -> heading;
            };
        }
        // Safety cap: a pathological build (a spiral of hundreds of guides) is treated as open.
        return new AcceleratorPath(List.copyOf(segments), false, guides, total);
    }

    /** What one ray-march found: a guide (with its bend), the controller again, or nothing. */
    private record Hit(BlockPos pos, int steps, @Nullable Bend bend, boolean closesLoop) {
    }

    /**
     * March up to {@code maxGap} blocks along {@code heading} from {@code from}, stopping at the first
     * guide or at the controller itself. Reads at most {@code maxGap} block states and never loads a
     * chunk — an unloaded position ends the march as "nothing found".
     */
    private static Hit march(Level level, BlockPos from, BlockPos controller, Heading heading, int maxGap) {
        int gap = Math.max(1, maxGap);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int step = 1; step <= gap; step++) {
            cursor.set(from.getX() + heading.dx() * step, from.getY(), from.getZ() + heading.dz() * step);
            if (!level.hasChunkAt(cursor)) {
                break;
            }
            if (cursor.getX() == controller.getX() && cursor.getY() == controller.getY()
                    && cursor.getZ() == controller.getZ()) {
                return new Hit(controller, step, null, true);
            }
            BlockState state = level.getBlockState(cursor);
            if (state.is(ModBlocks.ACCELERATOR_COIL.get())) {
                return new Hit(cursor.immutable(), step, state.getValue(AcceleratorGuideBlock.BEND), false);
            }
        }
        BlockPos lost = new BlockPos(from.getX() + heading.dx() * gap, from.getY(),
                from.getZ() + heading.dz() * gap);
        return new Hit(lost, gap, null, false);
    }

    /**
     * The speed the controller must inject at for this loop: the slowest speed that clears its
     * LONGEST segment (the gap rule inverted). A wide ring therefore demands a fast injection and
     * rewards it with a high bend ceiling; a tight ring starts crawling and stays slow.
     */
    public double injectionSpeed(double allowance, double gapPerSpeed) {
        double required = 0.0D;
        for (Segment segment : this.segments) {
            required = Math.max(required, AcceleratorMath.speedForGap(segment.length(), allowance, gapPerSpeed));
        }
        return required;
    }

    /**
     * The top speed this loop tolerates: {@code bendSpeedBase} × the shortest segment that ends in a
     * real bend. Straight-through guides impose no ceiling, so a loop whose only tight stretches run
     * dead straight is fine. Returns {@link Double#MAX_VALUE} for a loop with no bends at all.
     */
    public double bendCeiling(double bendSpeedBase) {
        double ceiling = Double.MAX_VALUE;
        for (Segment segment : this.segments) {
            Bend bend = segment.bend();
            if (bend != null && bend != Bend.STRAIGHT) {
                ceiling = Math.min(ceiling, AcceleratorMath.maxBendSpeed(segment.length(), bendSpeedBase));
            }
        }
        return ceiling;
    }
}
