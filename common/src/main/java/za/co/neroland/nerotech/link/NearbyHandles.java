package za.co.neroland.nerotech.link;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * The opaque per-request handles a {@code nearby} response hands out (link schema 2, decision 007).
 *
 * <p>Each {@code nearby} answer replaces the requester's previous table: a fresh random nonce plus the
 * targets in response order. A handle is {@code <nonce>-<index>}; it resolves only for the requester
 * whose response minted it, only while that response is the requester's latest, and only for
 * {@link #TTL_MS}. The targets (dimension + position) never leave the server: the client sees the
 * handle and nothing it could turn back into a place. Held in memory only, dropped on logout.
 *
 * @param <T> the server-side target (a dimension + block position in production, anything in tests)
 */
final class NearbyHandles<T> {

    /** How long a handle stays valid after its response was built. */
    static final long TTL_MS = 30_000L;

    private record Issue<T>(String nonce, long issuedAt, List<T> targets) {
    }

    private final Map<UUID, Issue<T>> latest = new ConcurrentHashMap<>();
    private final LongSupplier clock;
    private final SecureRandom random = new SecureRandom();

    NearbyHandles(LongSupplier clock) {
        this.clock = clock;
    }

    /**
     * Mint handles for one response, replacing the requester's previous table.
     *
     * @return one handle per target, in the same order
     */
    List<String> issue(UUID requester, List<T> targets) {
        String nonce = Long.toHexString(this.random.nextLong() & 0xFFFFFFFFFFFFL);
        List<T> copy = List.copyOf(targets);
        this.latest.put(requester, new Issue<>(nonce, this.clock.getAsLong(), copy));
        List<String> handles = new ArrayList<>(copy.size());
        for (int i = 0; i < copy.size(); i++) {
            handles.add(nonce + "-" + Integer.toString(i, 36));
        }
        return handles;
    }

    /**
     * The target behind {@code handle}, or empty when the handle is malformed, expired, superseded or
     * was minted for another requester.
     */
    Optional<T> resolve(UUID requester, String handle) {
        if (requester == null || handle == null) {
            return Optional.empty();
        }
        Issue<T> issue = this.latest.get(requester);
        if (issue == null || this.clock.getAsLong() - issue.issuedAt() > TTL_MS) {
            return Optional.empty();
        }
        int dash = handle.lastIndexOf('-');
        if (dash <= 0 || !handle.substring(0, dash).equals(issue.nonce())) {
            return Optional.empty();
        }
        int index;
        try {
            index = Integer.parseInt(handle.substring(dash + 1), 36);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
        if (index < 0 || index >= issue.targets().size()
                || !Integer.toString(index, 36).equals(handle.substring(dash + 1))) {
            return Optional.empty();
        }
        return Optional.of(issue.targets().get(index));
    }

    /** Drop one player's table (logout, erasure). */
    void forget(UUID player) {
        this.latest.remove(player);
    }

    /** Drop every table whose player is not in {@code online}. */
    void retainOnly(Set<UUID> online) {
        this.latest.keySet().retainAll(online);
    }

    /** Whether a table is held for {@code player} (erasure probe). */
    boolean holds(UUID player) {
        return this.latest.containsKey(player);
    }
}
