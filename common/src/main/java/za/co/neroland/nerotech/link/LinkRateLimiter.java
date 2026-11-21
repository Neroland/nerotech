package za.co.neroland.nerotech.link;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.LongSupplier;

/**
 * NeroTech's own action budget (link schema 2, decision 007): {@link #MAX_ACTIONS} actions per player
 * per sliding {@link #WINDOW_MS}. In memory only, keyed by UUID. A window is dropped only once its
 * newest call is older than the window ({@link #pruneExpired}), not when the player logs out, so a
 * relog or an offline action does not refill the budget (review L1); an erasure request drops it at
 * once. The bridge has a limiter of its own; this one holds even when a bridge is configured loosely.
 */
final class LinkRateLimiter {

    static final int MAX_ACTIONS = 10;
    /** The separate, generous budget for privacy opt-outs: never refused in normal use, still bounded. */
    static final int MAX_OPT_OUTS = 30;
    static final long WINDOW_MS = 60_000L;

    private final Map<UUID, Deque<Long>> calls = new HashMap<>();
    private final LongSupplier clock;
    private final int max;

    LinkRateLimiter(LongSupplier clock) {
        this(clock, MAX_ACTIONS);
    }

    LinkRateLimiter(LongSupplier clock, int max) {
        this.clock = clock;
        this.max = max;
    }

    /** The most calls this limiter allows per window. */
    int max() {
        return this.max;
    }

    /** Spend one action for {@code player}; false (and nothing spent) when the budget is used up. */
    synchronized boolean tryAcquire(UUID player) {
        long now = this.clock.getAsLong();
        Deque<Long> times = this.calls.computeIfAbsent(player, k -> new ArrayDeque<>());
        while (!times.isEmpty() && now - times.peekFirst() >= WINDOW_MS) {
            times.pollFirst();
        }
        if (times.size() >= this.max) {
            return false;
        }
        times.addLast(now);
        return true;
    }

    synchronized void forget(UUID player) {
        this.calls.remove(player);
    }

    synchronized void retainOnly(Set<UUID> online) {
        this.calls.keySet().retainAll(online);
    }

    /** Drop every window whose newest call is a full window old: nothing in it can refuse a call any more. */
    synchronized void pruneExpired() {
        long now = this.clock.getAsLong();
        this.calls.values().removeIf(times -> times.isEmpty() || now - times.peekLast() >= WINDOW_MS);
    }

    /** Whether a window is held for {@code player} (erasure probe). */
    synchronized boolean holds(UUID player) {
        return this.calls.containsKey(player);
    }
}
