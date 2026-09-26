package za.co.neroland.nerotech.link;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import za.co.neroland.nerolandcore.data.PlayerDataErasure;

import za.co.neroland.nerotech.pollution.PollutionAttributionPrefs;

/**
 * Unit tests for the per-player pollution attribution opt-out store behind the NeroLink
 * {@code set_pollution_attribution} action. Plain JVM (no server) — {@link PollutionAttributionPrefs}
 * is constructed directly, mirroring {@code PollutionErasureRoundTripTest}. Random UUIDs, never
 * logged.
 */
class PollutionAttributionPrefsTest {

    @Test
    void togglingEnabledFlipsTheOptOutState() {
        PollutionAttributionPrefs prefs = new PollutionAttributionPrefs();
        UUID player = UUID.randomUUID();

        assertFalse(prefs.isOptedOut(player), "default is follow-global (not opted out)");

        boolean optedOut = prefs.setEnabled(player, false); // enabled=false ⇒ opt OUT
        assertTrue(optedOut);
        assertTrue(prefs.isOptedOut(player));

        optedOut = prefs.setEnabled(player, true); // enabled=true ⇒ opt back IN
        assertFalse(optedOut);
        assertFalse(prefs.isOptedOut(player));
    }

    @Test
    void erasureHookRoundTripClearsThePreference() {
        PollutionAttributionPrefs prefs = new PollutionAttributionPrefs();
        UUID player = UUID.randomUUID();

        prefs.setEnabled(player, false);
        assertTrue(prefs.isOptedOut(player));

        // Same shape as NeroTechCommon.init(): the prefs eraser is part of the shared hook.
        PlayerDataErasure.register((server, uuid) -> prefs.forgetPlayer(uuid));
        PlayerDataErasure.erase(null, player);

        assertFalse(prefs.isOptedOut(player), "erasure returns the player to the global default");
    }

    @Test
    void erasureTargetsOnlyTheRequestedPlayer() {
        PollutionAttributionPrefs prefs = new PollutionAttributionPrefs();
        UUID erased = UUID.randomUUID();
        UUID retained = UUID.randomUUID();

        prefs.setEnabled(erased, false);
        prefs.setEnabled(retained, false);

        prefs.forgetPlayer(erased);

        assertFalse(prefs.isOptedOut(erased));
        assertTrue(prefs.isOptedOut(retained), "another player's opt-out must survive a targeted erasure");
    }

    @Test
    void pendingOwnerErasureIsListedUntilItExpires() {
        PollutionAttributionPrefs prefs = new PollutionAttributionPrefs();
        UUID erased = UUID.randomUUID();
        long day = 20_000L;

        assertFalse(prefs.isPendingErasure(erased), "nothing pending by default");
        int epochBefore = PollutionAttributionPrefs.erasureEpoch();

        prefs.markOwnerErasure(erased, day);

        assertTrue(prefs.isPendingErasure(erased), "an erased owner is pending for loaded/unloaded machines");
        assertTrue(PollutionAttributionPrefs.erasureEpoch() != epochBefore,
                "the epoch moves so loaded machines re-check on their next tick");

        prefs.purgeExpiredErasures(day + PollutionAttributionPrefs.ERASURE_RETENTION_DAYS);
        assertTrue(prefs.isPendingErasure(erased), "still pending on the last retained day");

        prefs.purgeExpiredErasures(day + PollutionAttributionPrefs.ERASURE_RETENTION_DAYS + 1);
        assertFalse(prefs.isPendingErasure(erased), "expired rows are dropped");
    }

    @Test
    void newErasureRequestPurgesOlderExpiredRows() {
        PollutionAttributionPrefs prefs = new PollutionAttributionPrefs();
        UUID old = UUID.randomUUID();
        UUID fresh = UUID.randomUUID();

        prefs.markOwnerErasure(old, 1_000L);
        prefs.markOwnerErasure(fresh, 1_000L + PollutionAttributionPrefs.ERASURE_RETENTION_DAYS + 5);

        assertFalse(prefs.isPendingErasure(old), "a request purges rows that have already expired");
        assertTrue(prefs.isPendingErasure(fresh));
    }
}
