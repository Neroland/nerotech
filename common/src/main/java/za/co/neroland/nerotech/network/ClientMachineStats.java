package za.co.neroland.nerotech.network;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.Nullable;

/**
 * Client-side mailbox for {@link MachineStatsPayload}, keyed by container id — the
 * {@link ClientMenuPos} pattern applied to the Stage G analytics stream: the clientbound
 * handler stays free of client-only classes and just drops the latest snapshot here; the
 * Analytics tab polls (and thereby drains) it and keeps the last snapshot it saw, so a stale
 * payload can never feed a different menu.
 *
 * <p>Bounded by vanilla's container-id counter (wraps at 100 per session) and drained on poll.
 * Machine-scoped stats only — no player data (POPIA/GDPR).
 */
public final class ClientMachineStats {

    private static final Map<Integer, MachineStatsPayload> PENDING = new ConcurrentHashMap<>();

    private ClientMachineStats() {
    }

    /** Called by the clientbound handler. Later snapshots simply replace earlier ones. */
    public static void accept(MachineStatsPayload payload) {
        PENDING.put(payload.containerId(), payload);
    }

    /** Take and clear the snapshot for a container id, or {@code null} if none is pending. */
    @Nullable
    public static MachineStatsPayload poll(int containerId) {
        return PENDING.remove(containerId);
    }

    public static void clear() {
        PENDING.clear();
    }
}
