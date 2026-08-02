package za.co.neroland.nerotech.network;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.Nullable;

/**
 * Client-side mailbox for {@link AnalyticsTerminalPayload}, keyed by container id — the
 * {@link ClientMenuPos} pattern applied to the terminal dashboard stream: the clientbound
 * handler drops the latest snapshot here; the Analytics Terminal screen polls (drains) it and
 * keeps the last snapshot it saw, so a stale payload can never feed a different menu.
 *
 * <p>Bounded by vanilla's container-id counter (wraps at 100 per session) and drained on poll.
 * Machine positions/statuses/heat only — no player data (POPIA/GDPR).
 */
public final class ClientTerminalStats {

    private static final Map<Integer, AnalyticsTerminalPayload> PENDING = new ConcurrentHashMap<>();

    private ClientTerminalStats() {
    }

    /** Called by the clientbound handler. Later snapshots simply replace earlier ones. */
    public static void accept(AnalyticsTerminalPayload payload) {
        PENDING.put(payload.containerId(), payload);
    }

    /** Take and clear the snapshot for a container id, or {@code null} if none is pending. */
    @Nullable
    public static AnalyticsTerminalPayload poll(int containerId) {
        return PENDING.remove(containerId);
    }

    public static void clear() {
        PENDING.clear();
    }
}
