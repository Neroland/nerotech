package za.co.neroland.nerotech.item;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import za.co.neroland.nerotech.item.LinkAuth.Endpoint;

/**
 * Locks the Wireless Power Node pairing rule ({@link LinkAuth#allowed}): every endpoint an action
 * touches must be loaded and interactable, and every owned endpoint must belong to the actor unless
 * they are a gamemaster. Pure JVM — {@link ConfiguratorItem} only builds the endpoint list.
 */
class WirelessPairingAuthTest {

    private static final UUID ALICE = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID BOB = UUID.fromString("00000000-0000-0000-0000-00000000000b");

    private static Endpoint unowned() {
        return new Endpoint(true, true, Optional.empty());
    }

    private static Endpoint ownedBy(UUID owner) {
        return new Endpoint(true, true, Optional.of(owner));
    }

    @Test
    void unownedBothLoadedIsAllowed() {
        assertTrue(LinkAuth.allowed(ALICE, false, List.of(unowned(), unowned())));
    }

    @Test
    void mayInteractFalseIsRefused() {
        Endpoint protectedSpawn = new Endpoint(true, false, Optional.empty());
        assertFalse(LinkAuth.allowed(ALICE, false, List.of(unowned(), protectedSpawn)));
        assertFalse(LinkAuth.allowed(ALICE, false, List.of(protectedSpawn, unowned())));
        assertFalse(LinkAuth.allowed(ALICE, true, List.of(protectedSpawn)),
                "gamemaster overrides ownership, not mayInteract");
    }

    @Test
    void ownerMismatchOnAnySingleOwnedEndpointIsRefused() {
        // Previously only refused when *both* ends were owned — one owned end is enough now.
        assertFalse(LinkAuth.allowed(ALICE, false, List.of(ownedBy(BOB), unowned())));
        assertFalse(LinkAuth.allowed(ALICE, false, List.of(unowned(), ownedBy(BOB))));
        assertFalse(LinkAuth.allowed(ALICE, false, List.of(ownedBy(ALICE), ownedBy(BOB))));
        assertFalse(LinkAuth.allowed(ALICE, false, List.of(ownedBy(ALICE), unowned(), ownedBy(BOB))),
                "an old partner the pair would unlink counts too");
    }

    @Test
    void ownerMatchIsAllowed() {
        assertTrue(LinkAuth.allowed(ALICE, false, List.of(ownedBy(ALICE), ownedBy(ALICE))));
        assertTrue(LinkAuth.allowed(ALICE, false, List.of(ownedBy(ALICE), unowned())));
        assertTrue(LinkAuth.allowed(ALICE, false, List.of(ownedBy(ALICE))), "storing the pending end");
    }

    @Test
    void gamemasterOverridesOwnership() {
        assertTrue(LinkAuth.allowed(ALICE, true, List.of(ownedBy(BOB), ownedBy(BOB))));
        assertTrue(LinkAuth.allowed(ALICE, true, List.of(ownedBy(BOB), unowned(), ownedBy(ALICE))));
    }

    @Test
    void unloadedEndpointIsRefused() {
        assertFalse(LinkAuth.allowed(ALICE, false, List.of(unowned(), Endpoint.unloaded())));
        assertFalse(LinkAuth.allowed(ALICE, true, List.of(ownedBy(ALICE), Endpoint.unloaded())),
                "can't verify an unloaded end, even as gamemaster");
        assertFalse(LinkAuth.allowed(ALICE, false,
                List.of(new Endpoint(false, true, Optional.empty()))),
                "mayInteract is meaningless for an unloaded end");
    }

    @Test
    void unlinkWithUnloadedPartnerStillChecksClickedNodesOwner() {
        // The clicked node's ownership is enforced on its own, independent of the partner's state:
        // a stranger is refused whether or not the partner is present in the list.
        assertFalse(LinkAuth.allowed(ALICE, false, List.of(ownedBy(BOB))));
        assertFalse(LinkAuth.allowed(ALICE, false, List.of(ownedBy(BOB), Endpoint.unloaded())));
        // And the unloaded partner never makes an owner-mismatched clicked node pass for a stranger,
        // nor an unverifiable partner pass for the clicked node's owner.
        assertFalse(LinkAuth.allowed(ALICE, false, List.of(Endpoint.unloaded(), ownedBy(BOB))));
        assertFalse(LinkAuth.allowed(BOB, false, List.of(ownedBy(BOB), Endpoint.unloaded())));
    }

    @Test
    void noActorOrNoEndpointsIsRefused() {
        assertFalse(LinkAuth.allowed(null, true, List.of(unowned())));
        assertFalse(LinkAuth.allowed(ALICE, true, List.of()));
    }
}
