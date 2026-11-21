package za.co.neroland.nerotech.link;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.LongSupplier;

/**
 * At most one {@code alarm} event per machine type per player per {@link #WINDOW_MS}.
 * Keyed by recipient UUID and block id only; in memory, cleared on logout.
 */
final class AlarmThrottle {

    static final long WINDOW_MS = 30_000L;

    private final Map<UUID, Map<String, Long>> last = new HashMap<>();
    private final LongSupplier clock;

    AlarmThrottle(LongSupplier clock) {
        this.clock = clock;
    }

    /** Whether {@code player} may be told about an alarm on a {@code type} machine now; records it if so. */
    synchronized boolean tryFire(UUID player, String type) {
        long now = this.clock.getAsLong();
        Map<String, Long> perType = this.last.computeIfAbsent(player, k -> new HashMap<>());
        Long previous = perType.get(type);
        if (previous != null && now - previous < WINDOW_MS) {
            return false;
        }
        perType.put(type, now);
        return true;
    }

    synchronized void forget(UUID player) {
        this.last.remove(player);
    }

    synchronized void retainOnly(Set<UUID> online) {
        this.last.keySet().retainAll(online);
    }

    /** Whether any throttle entry is held for {@code player} (erasure probe). */
    synchronized boolean holds(UUID player) {
        return this.last.containsKey(player);
    }
}
