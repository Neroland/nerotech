package za.co.neroland.nerotech.machine.heat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

/**
 * Something a Heat Pipe run or a Heat Exchanger can trade heat with that is neither a Foundry Heat Port
 * (read directly through its own {@code acceptHeat}) nor a {@code NeroTechMachineBlockEntity}: the seam for
 * any later heat sink or source, on its block entity or through {@link Provider} on a BE-less block. Every
 * move is exact and unscaled, so heat is never created on the way in.
 */
public interface HeatAcceptor {

    /** Current heat on the machines' scale (0..{@link #heatCapacity()}). */
    int heat();

    /** Heat ceiling. */
    int heatCapacity();

    /**
     * Take up to {@code amount} heat in from {@code side} (the face the heat arrives through).
     *
     * @return heat actually accepted, never more than {@code amount}
     */
    int acceptHeat(int amount, Direction side);

    /**
     * Give up to {@code amount} heat out through {@code side}. Default: an acceptor only takes heat in.
     *
     * @return heat actually given, never more than {@code amount}
     */
    default int releaseHeat(int amount, Direction side) {
        return 0;
    }

    /** Whether a pipe or exchanger on {@code side} connects at all (drives the pipe's connected model). */
    default boolean connectsHeat(Direction side) {
        return true;
    }

    /** For a block with no block entity: the acceptor at a position. */
    interface Provider {

        @Nullable
        HeatAcceptor heatAcceptor(Level level, BlockPos pos, BlockState state);
    }
}
