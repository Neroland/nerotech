package za.co.neroland.nerotech.machine.heat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerotech.machine.NeroTechMachineBlockEntity;
import za.co.neroland.nerotech.machine.foundry.FoundryHeatPortBlockEntity;

/**
 * Adapters from the world to {@link HeatRunMath.Node}: a Foundry Heat Port (one way in, through its public
 * {@code acceptHeat}), a {@link HeatAcceptor} (block entity first, then a block {@link HeatAcceptor.Provider})
 * or any {@link NeroTechMachineBlockEntity}. Machine deposits are made
 * exact: the base {@code addHeat} scales by the overclock preset, so {@link #deposit} trims any overshoot
 * and tops up any shortfall, and heat is neither created nor lost on the way in.
 */
public final class HeatNodes {

    private HeatNodes() {
    }

    /** Whether something at {@code pos} can trade heat with a pipe on {@code side} of it. */
    public static boolean isEndpoint(Level level, BlockPos pos, Direction side) {
        return node(level, pos, side) != null;
    }

    /**
     * The node at {@code pos}, seen from {@code side} (the face of {@code pos} that touches the pipe or
     * exchanger), or null.
     */
    @Nullable
    public static HeatRunMath.Node node(Level level, BlockPos pos, Direction side) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof FoundryHeatPortBlockEntity port && !port.isRemoved()) {
            return portNode(port);
        }
        if (be instanceof HeatAcceptor acceptor) {
            return acceptor.connectsHeat(side) ? acceptorNode(acceptor, side) : null;
        }
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof HeatAcceptor.Provider provider) {
            HeatAcceptor acceptor = provider.heatAcceptor(level, pos, state);
            if (acceptor != null && acceptor.connectsHeat(side)) {
                return acceptorNode(acceptor, side);
            }
            return null;
        }
        if (be instanceof NeroTechMachineBlockEntity machine && !machine.isRemoved()) {
            return machineNode(machine);
        }
        return null;
    }

    /** A machine as a node. */
    public static HeatRunMath.Node machineNode(NeroTechMachineBlockEntity machine) {
        return new HeatRunMath.Node() {
            @Override
            public int heat() {
                return machine.heat();
            }

            @Override
            public int take(int amount) {
                return amount <= 0 ? 0 : machine.extractHeat(amount, 0);
            }

            @Override
            public int give(int amount) {
                return deposit(machine, amount);
            }
        };
    }

    /**
     * A Foundry Heat Port: its buffer level is its temperature, it only ever takes heat in (no extract), and
     * the source pays exactly what {@code acceptHeat} returned.
     */
    private static HeatRunMath.Node portNode(FoundryHeatPortBlockEntity port) {
        return new HeatRunMath.Node() {
            @Override
            public int heat() {
                return port.heatStored();
            }

            @Override
            public int take(int amount) {
                return 0;
            }

            @Override
            public int give(int amount) {
                return amount <= 0 ? 0 : clamp(port.acceptHeat(amount), amount);
            }
        };
    }

    private static HeatRunMath.Node acceptorNode(HeatAcceptor acceptor, Direction side) {
        return new HeatRunMath.Node() {
            @Override
            public int heat() {
                return acceptor.heat();
            }

            @Override
            public int take(int amount) {
                return amount <= 0 ? 0 : clamp(acceptor.releaseHeat(amount, side), amount);
            }

            @Override
            public int give(int amount) {
                return amount <= 0 ? 0 : clamp(acceptor.acceptHeat(amount, side), amount);
            }
        };
    }

    private static int clamp(int value, int max) {
        return Math.max(0, Math.min(value, max));
    }

    /**
     * Add exactly up to {@code amount} heat to {@code machine}, whatever its preset: repeated
     * {@code addHeat} calls fill any shortfall (Eco halves each), and an overshoot (Overdrive doubles) is
     * taken straight back out. Stops early at the heat ceiling.
     *
     * @return heat actually added, never more than {@code amount}
     */
    public static int deposit(NeroTechMachineBlockEntity machine, int amount) {
        int delivered = 0;
        for (int i = 0; i < 16 && delivered < amount; i++) {
            int before = machine.heat();
            machine.addHeat(amount - delivered);
            int added = machine.heat() - before;
            if (added <= 0) {
                break;
            }
            delivered += added;
        }
        if (delivered > amount) {
            delivered -= machine.extractHeat(delivered - amount, 0);
        }
        return Math.max(0, Math.min(delivered, amount));
    }
}
