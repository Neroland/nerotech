package za.co.neroland.nerotech.pollution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import net.minecraft.core.BlockPos;

import org.junit.jupiter.api.Test;

import za.co.neroland.nerolandcore.data.PlayerDataErasure;

/**
 * POPIA/GDPR round-trip for NeroTech's opt-in pollution attribution (the mod's only
 * player-attributable store): insert a fake player's attribution row, dispatch an erasure
 * through Core's shared {@link PlayerDataErasure} hook, and assert the row is gone —
 * plus the retention-prune sweep in isolation.
 *
 * <p>Plain JVM, no game bootstrap: {@link PollutionState} is constructed directly instead of
 * through {@code PollutionState.get(server)}, because that lookup needs a live
 * {@link net.minecraft.server.MinecraftServer}'s SavedData storage. The eraser registered here
 * mirrors NeroTechCommon's production registration ({@code PollutionManager::erasePlayer}
 * &rarr; {@code PollutionState.forgetPlayer}) but binds to the local state for the same reason;
 * the {@code MinecraftServer} parameter is pass-through and unused by the eraser itself.
 *
 * <p>Privacy note: these tests generate random UUIDs and never log them (Core's hook logs only
 * anonymous counts).
 */
class PollutionErasureRoundTripTest {

    @Test
    void erasureHookRoundTripRemovesAttribution() {
        PollutionState state = new PollutionState();
        UUID player = UUID.randomUUID();

        state.attribute(player, 25, 100L);
        assertEquals(25, state.attributed(player), "attribution row must exist before erasure");

        // Same shape as NeroTechCommon.init(): register the pollution eraser with Core's hook.
        PlayerDataErasure.register((server, uuid) -> state.forgetPlayer(uuid));

        // One shared erase request purges the player; the server handle is pass-through here.
        PlayerDataErasure.erase(null, player);

        assertEquals(0, state.attributed(player), "erasure request must purge the attribution row");
    }

    @Test
    void erasureTargetsOnlyTheRequestedPlayer() {
        PollutionState state = new PollutionState();
        UUID erased = UUID.randomUUID();
        UUID retained = UUID.randomUUID();

        state.attribute(erased, 10, 200L);
        state.attribute(retained, 40, 200L);

        state.forgetPlayer(erased);

        assertEquals(0, state.attributed(erased));
        assertEquals(40, state.attributed(retained), "other players' rows must survive a targeted erasure");
    }

    @Test
    void retentionPruneDropsOnlyStaleRows() {
        PollutionState state = new PollutionState();
        UUID stale = UUID.randomUUID();
        UUID fresh = UUID.randomUUID();

        state.attribute(stale, 15, 0L);    // last touched on day 0
        state.attribute(fresh, 20, 90L);   // last touched on day 90

        state.pruneStale(30, 95L);         // 30-day retention window, "today" = day 95

        assertEquals(0, state.attributed(stale), "rows older than the retention window must be pruned");
        assertEquals(20, state.attributed(fresh), "rows inside the retention window must be kept");
    }

    @Test
    void retentionPruneDisabledWhenWindowIsZero() {
        PollutionState state = new PollutionState();
        UUID player = UUID.randomUUID();

        state.attribute(player, 5, 0L);
        state.pruneStale(0, 10_000L); // 0 disables pruning entirely

        assertEquals(5, state.attributed(player));
    }

    @Test
    void erasureLeavesAggregateRegionalPollutionIntact() {
        PollutionState state = new PollutionState();
        UUID player = UUID.randomUUID();
        long region = PollutionManager.regionKey(new BlockPos(130, 64, -70));

        state.addRegion(region, 12);
        state.attribute(player, 12, 50L);

        state.forgetPlayer(player);

        assertEquals(0, state.attributed(player), "personal attribution must be gone");
        assertEquals(12, state.region(region), "aggregate (non-personal) regional pollution must remain");
        assertTrue(state.region(region) > 0);
    }
}
