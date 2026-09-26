package za.co.neroland.nerotech.item;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * The Wireless Power Node pairing authorisation rule, kept Minecraft-free so it is unit-testable
 * without a loader. {@link ConfiguratorItem} gathers one {@link Endpoint} per node an action would
 * touch — the clicked node, the pending (first) node when pairing, and the clicked node's
 * <i>current</i> partner (or the pending node's old partner) whenever the action would break that
 * link — and asks {@link #allowed} for the verdict.
 *
 * <p>For <b>every</b> endpoint:
 * <ol>
 *   <li>its chunk must be loaded (an unloaded endpoint cannot be verified, so it is refused), and the
 *       player must be able to interact at it (vanilla {@code Player.mayInteract});</li>
 *   <li>when it has an owner, the player must be that owner or hold gamemaster permission.</li>
 * </ol>
 *
 * <p>Owners are UUIDs compared here and nowhere stored, logged or shown (POPIA/GDPR).
 */
public final class LinkAuth {

    private LinkAuth() {
    }

    /**
     * One node an action would touch.
     *
     * @param loaded      whether the node's chunk is loaded (so it could be checked at all)
     * @param mayInteract whether the player may interact at the node's position (ignored when unloaded)
     * @param owner       the node's owner, when it has one
     */
    public record Endpoint(boolean loaded, boolean mayInteract, Optional<UUID> owner) {

        public Endpoint {
            owner = owner == null ? Optional.empty() : owner;
        }

        /** An endpoint whose chunk is not loaded — always refused. */
        public static Endpoint unloaded() {
            return new Endpoint(false, false, Optional.empty());
        }
    }

    /**
     * Whether {@code player} may perform an action touching all of {@code endpoints}. A {@code null}
     * player (no actor) or an empty endpoint list is refused — there is nothing to authorise against.
     *
     * @param player     the acting player's UUID (the item holder, never a nearest-player lookup)
     * @param gamemaster whether the player holds gamemaster (level 2) permission — overrides ownership
     *                   only, never the loaded / {@code mayInteract} checks
     * @param endpoints  every node the action touches
     */
    public static boolean allowed(UUID player, boolean gamemaster, List<Endpoint> endpoints) {
        if (player == null || endpoints == null || endpoints.isEmpty()) {
            return false;
        }
        for (Endpoint endpoint : endpoints) {
            if (endpoint == null || !endpoint.loaded() || !endpoint.mayInteract()) {
                return false;
            }
            Optional<UUID> owner = endpoint.owner();
            if (owner.isPresent() && !gamemaster && !Objects.equals(owner.get(), player)) {
                return false;
            }
        }
        return true;
    }
}
