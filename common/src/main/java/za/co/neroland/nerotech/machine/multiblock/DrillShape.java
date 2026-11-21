package za.co.neroland.nerotech.machine.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Deep Core Drill: the controller sits on top of a 1×1×3 column of {@code #nerotech:drill_derrick}
 * blocks. Not a cube, but the same API: the box is the column, every position in it is "shell", and the
 * centre is the column's middle block. Facing does not matter.
 */
public final class DrillShape implements StructureShape {

    public static final DrillShape INSTANCE = new DrillShape();

    /** Column height below the controller. */
    public static final int HEIGHT = 3;

    private static final int[] SIZES = {HEIGHT};

    private DrillShape() {
    }

    @Override
    public int[] sizes() {
        return SIZES.clone();
    }

    @Override
    public boolean isShell(BlockState state) {
        return state.is(MultiblockTags.DRILL_DERRICK);
    }

    @Override
    public boolean isInterior(BlockState state) {
        return false; // the column has no interior
    }

    @Override
    public BlockPos centre(BlockPos controller, Direction facing, int size) {
        return controller.below((size + 1) / 2);
    }

    @Override
    public BlockPos min(BlockPos controller, Direction facing, int size) {
        return controller.below(size);
    }

    @Override
    public BlockPos max(BlockPos controller, Direction facing, int size) {
        return controller.below();
    }

    @Override
    public boolean isSurface(BlockPos pos, BlockPos min, BlockPos max) {
        return true;
    }
}
