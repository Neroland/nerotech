package za.co.neroland.nerotech.machine.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The shape of one NeroTech multiblock: which sizes it comes in, where its bounding box sits relative to
 * the controller, and which block states may fill each role. {@link MultiblockValidator} does the bounded
 * scan; a shape only answers questions.
 *
 * <p>The geometry methods take no block state, so a shape's layout can be checked in a plain unit test
 * against a fake grid. The default geometry is a hollow cube with the controller at the centre of one
 * vertical wall, facing outward (the fusion reactor's layout).
 */
public interface StructureShape {

    /** Candidate edge sizes, largest first so a big structure is never mis-detected as a smaller one. */
    int[] sizes();

    /** A state that may form the outer shell (wall, floor, roof). */
    boolean isShell(BlockState state);

    /** A state that may fill the interior. */
    boolean isInterior(BlockState state);

    /** A state allowed in the shell as a port (collected into {@code Formed.ports}). */
    default boolean isPort(BlockState state) {
        return false;
    }

    /** The structure's centre for a controller at {@code controller} facing {@code facing}. */
    default BlockPos centre(BlockPos controller, Direction facing, int size) {
        return controller.relative(facing.getOpposite(), (size - 1) / 2);
    }

    /** Lowest corner of the bounding box (inclusive). */
    default BlockPos min(BlockPos controller, Direction facing, int size) {
        int half = (size - 1) / 2;
        return centre(controller, facing, size).offset(-half, -half, -half);
    }

    /** Highest corner of the bounding box (inclusive). */
    default BlockPos max(BlockPos controller, Direction facing, int size) {
        int half = (size - 1) / 2;
        return centre(controller, facing, size).offset(half, half, half);
    }

    /** Whether {@code pos} (inside the box) is a shell position; everything else is interior. */
    default boolean isSurface(BlockPos pos, BlockPos min, BlockPos max) {
        return pos.getX() == min.getX() || pos.getX() == max.getX()
                || pos.getY() == min.getY() || pos.getY() == max.getY()
                || pos.getZ() == min.getZ() || pos.getZ() == max.getZ();
    }
}
