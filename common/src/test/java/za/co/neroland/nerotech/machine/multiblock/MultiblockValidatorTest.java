package za.co.neroland.nerotech.machine.multiblock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import org.junit.jupiter.api.Test;

import za.co.neroland.nerotech.machine.FusionStructure;

/**
 * Regression tests for the shared multiblock base on a pure fake grid (no Minecraft bootstrap): the fusion
 * shell forms at 3, 5 and 7 exactly as before the move, and a broken shell, a wrong interior or an
 * unloaded box all read as unformed.
 */
class MultiblockValidatorTest {

    private static final char SHELL = 'S';
    private static final char AIR = 'A';
    private static final char PORT = 'P';
    private static final char JUNK = 'J';

    /** A sparse grid: missing positions read as air. */
    private static final class Grid implements MultiblockValidator.Probe {
        final Map<BlockPos, Character> cells = new HashMap<>();
        boolean loaded = true;

        void put(BlockPos pos, char c) {
            this.cells.put(pos.immutable(), c);
        }

        char at(BlockPos pos) {
            return this.cells.getOrDefault(pos, AIR);
        }

        @Override
        public boolean loaded(BlockPos min, BlockPos max) {
            return this.loaded;
        }

        @Override
        public boolean shell(BlockPos pos) {
            return at(pos) == SHELL;
        }

        @Override
        public boolean interior(BlockPos pos) {
            return at(pos) == AIR;
        }

        @Override
        public boolean port(BlockPos pos) {
            return at(pos) == PORT;
        }
    }

    private static final BlockPos CONTROLLER = new BlockPos(10, 64, -20);
    private static final Direction FACING = Direction.SOUTH;

    /** Build a hollow cube of shell around the controller's wall-centre slot, the old way. */
    private static Grid cube(int size) {
        Grid grid = new Grid();
        int half = (size - 1) / 2;
        BlockPos centre = CONTROLLER.relative(FACING.getOpposite(), half);
        BlockPos min = centre.offset(-half, -half, -half);
        BlockPos max = centre.offset(half, half, half);
        for (BlockPos p : BlockPos.betweenClosed(min, max)) {
            boolean surface = p.getX() == min.getX() || p.getX() == max.getX() || p.getY() == min.getY()
                    || p.getY() == max.getY() || p.getZ() == min.getZ() || p.getZ() == max.getZ();
            if (surface && !p.equals(CONTROLLER)) {
                grid.put(p, SHELL);
            }
        }
        return grid;
    }

    private static Optional<MultiblockValidator.Formed> fusion(Grid grid) {
        return MultiblockValidator.validate(grid, CONTROLLER, FACING, FusionShape.INSTANCE);
    }

    @Test
    void fusionShellFormsAtEverySize() {
        for (int size : new int[] {3, 5, 7}) {
            Optional<MultiblockValidator.Formed> formed = fusion(cube(size));
            assertTrue(formed.isPresent(), "a " + size + " shell must form");
            assertEquals(size, formed.get().size());
            int half = (size - 1) / 2;
            assertEquals(CONTROLLER.relative(FACING.getOpposite(), half), formed.get().centre());
            assertTrue(formed.get().ports().isEmpty());
            FusionStructure.Shell shell = FusionStructure.toShell(formed);
            assertEquals((size - 1) / 2, shell == null ? -1 : shell.tier(), "tier is (size-1)/2 as before");
        }
    }

    @Test
    void brokenShellIsUnformed() {
        Grid grid = cube(5);
        BlockPos corner = CONTROLLER.relative(FACING.getOpposite(), 4).offset(2, 2, 0);
        assertEquals(SHELL, grid.at(corner));
        grid.put(corner, AIR);
        assertFalse(fusion(grid).isPresent());
    }

    @Test
    void wrongInteriorIsUnformed() {
        Grid grid = cube(3);
        grid.put(CONTROLLER.relative(FACING.getOpposite(), 1), JUNK); // the single interior block
        assertFalse(fusion(grid).isPresent());
    }

    @Test
    void probeReportedPortIsCollected() {
        Grid grid = cube(3);
        grid.put(CONTROLLER.relative(FACING.getOpposite(), 2), PORT); // back wall centre
        // The probe answers per position; in a live level FusionShape.isPort is always false.
        Optional<MultiblockValidator.Formed> formed = fusion(grid);
        assertTrue(formed.isPresent(), "the shared scan accepts a probe-reported port in the shell");
        assertEquals(1, formed.get().ports().size());
    }

    @Test
    void unloadedBoxIsUnformed() {
        Grid grid = cube(7);
        grid.loaded = false;
        assertFalse(fusion(grid).isPresent());
    }

    @Test
    void largestShellWinsAndSmallerIsTriedAfter() {
        // A 3 shell sitting where a 7 would be: 7 and 5 fail, 3 forms.
        assertEquals(3, fusion(cube(3)).map(MultiblockValidator.Formed::size).orElse(0));
        assertEquals(7, fusion(cube(7)).map(MultiblockValidator.Formed::size).orElse(0));
    }

    @Test
    void drillColumnGeometry() {
        Grid grid = new Grid();
        BlockPos top = new BlockPos(0, 70, 0);
        for (int i = 1; i <= DrillShape.HEIGHT; i++) {
            grid.put(top.below(i), SHELL);
        }
        Optional<MultiblockValidator.Formed> formed =
                MultiblockValidator.validate(grid, top, Direction.NORTH, DrillShape.INSTANCE);
        assertTrue(formed.isPresent());
        assertEquals(top.below(2), formed.get().centre());
        grid.put(top.below(3), AIR);
        assertFalse(MultiblockValidator.validate(grid, top, Direction.NORTH, DrillShape.INSTANCE).isPresent());
    }

    @Test
    void foundryCollectsPortsInScanOrder() {
        Grid grid = cube(5);
        BlockPos back = CONTROLLER.relative(FACING.getOpposite(), 4);
        grid.put(back, PORT);
        grid.put(back.above(), PORT);
        Optional<MultiblockValidator.Formed> formed =
                MultiblockValidator.validate(grid, CONTROLLER, FACING, FoundryShape.INSTANCE);
        assertTrue(formed.isPresent());
        assertEquals(5, formed.get().size());
        assertEquals(2, formed.get().ports().size());
    }

    @Test
    void cadenceMatchesTheReactorsOldRule() {
        int hash = new BlockPos(3, 70, 9).hashCode();
        for (long t = 0; t < 400; t++) {
            assertEquals((t + Math.floorMod(hash, 20)) % 20 == 0, MultiblockController.onCadence(t, hash, false));
            assertEquals((t + Math.floorMod(hash, 100)) % 100 == 0, MultiblockController.onCadence(t, hash, true));
        }
    }
}
