package za.co.neroland.nerotech.machine.fusion;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * Fusion milestone hooks. A gate or trophy listener subscribes here; the reactor only reports what
 * happened and who was operating it in the moment (machine contract, Credit). No listener is wired by this
 * family: the milestone gate data arrives later and records through its own idempotent store.
 */
public final class FusionEvents {

    /**
     * A reactor started a gas-fed burn after a stretch of not burning gas (the first charge of a gas
     * episode). {@code operator} is the player who last touched the reactor or one of its injectors in the
     * moment, if any is known; never saved, never logged.
     */
    public record GasIgnition(ServerLevel level, BlockPos reactor, Optional<UUID> operator) {
    }

    private static final List<Consumer<GasIgnition>> GAS_IGNITION = new CopyOnWriteArrayList<>();

    private FusionEvents() {
    }

    /** Subscribe to gas-fed ignitions (server thread). */
    public static void onGasIgnition(Consumer<GasIgnition> listener) {
        GAS_IGNITION.add(listener);
    }

    /** Fire a gas-fed ignition to every listener; one failing listener never stops the others. */
    public static void fireGasIgnition(ServerLevel level, BlockPos reactor, Optional<UUID> operator) {
        if (GAS_IGNITION.isEmpty()) {
            return;
        }
        GasIgnition event = new GasIgnition(level, reactor.immutable(), operator);
        for (Consumer<GasIgnition> listener : GAS_IGNITION) {
            try {
                listener.accept(event);
            } catch (RuntimeException ignored) {
                // A listener's failure is its own; the reactor keeps burning.
            }
        }
    }
}
