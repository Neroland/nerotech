package za.co.neroland.nerotech.machine.multiblock;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

/**
 * Static, bounded multiblock validation shared by every NeroTech structure. The scan covers only the
 * shape's bounding box (at most 7³ = 343 positions), tries sizes largest first, and never loads a chunk:
 * an unloaded corner simply reports "unformed" until the area is back.
 *
 * <p>The scan runs over a {@link Probe} so the same code is exercised by plain unit tests with a fake grid;
 * {@link #validate(Level, BlockPos, Direction, StructureShape)} adapts a live level.
 */
public final class MultiblockValidator {

    private MultiblockValidator() {
    }

    /** A formed structure: its size, centre and the shell positions that hold ports (scan order). */
    public record Formed(int size, BlockPos centre, List<BlockPos> ports) {

        public Formed {
            centre = centre.immutable();
            ports = List.copyOf(ports);
        }
    }

    /** What the scan asks about one position. Answers come from block states in a live level. */
    public interface Probe {

        /** Whether every chunk in the box is loaded (validation never loads one). */
        boolean loaded(BlockPos min, BlockPos max);

        boolean shell(BlockPos pos);

        boolean interior(BlockPos pos);

        boolean port(BlockPos pos);
    }

    /** Validate the largest formed structure of {@code shape} around a controller in a live level. */
    public static Optional<Formed> validate(Level level, BlockPos controller, Direction facing, StructureShape shape) {
        return validate(levelProbe(level, shape), controller, facing, shape);
    }

    /** Validate against any probe; the controller's own position is always skipped. */
    public static Optional<Formed> validate(Probe probe, BlockPos controller, Direction facing, StructureShape shape) {
        for (int size : shape.sizes()) {
            Formed formed = check(probe, controller, facing, shape, size);
            if (formed != null) {
                return Optional.of(formed);
            }
        }
        return Optional.empty();
    }

    @Nullable
    private static Formed check(Probe probe, BlockPos controller, Direction facing, StructureShape shape, int size) {
        BlockPos min = shape.min(controller, facing, size);
        BlockPos max = shape.max(controller, facing, size);
        if (!probe.loaded(min, max)) {
            return null; // never force-load chunks for validation
        }
        List<BlockPos> ports = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            if (pos.equals(controller)) {
                continue; // the controller occupies its own slot
            }
            if (shape.isSurface(pos, min, max)) {
                if (probe.port(pos)) {
                    ports.add(pos.immutable());
                } else if (!probe.shell(pos)) {
                    return null;
                }
            } else if (!probe.interior(pos)) {
                return null;
            }
        }
        return new Formed(size, shape.centre(controller, facing, size), ports);
    }

    /** A probe over a live level's block states, answered through the shape's predicates. */
    public static Probe levelProbe(Level level, StructureShape shape) {
        return new Probe() {
            @Override
            public boolean loaded(BlockPos min, BlockPos max) {
                return level.hasChunksAt(min, max);
            }

            @Override
            public boolean shell(BlockPos pos) {
                return shape.isShell(state(pos));
            }

            @Override
            public boolean interior(BlockPos pos) {
                return shape.isInterior(state(pos));
            }

            @Override
            public boolean port(BlockPos pos) {
                return shape.isPort(state(pos));
            }

            private BlockState state(BlockPos pos) {
                return level.getBlockState(pos);
            }
        };
    }
}
