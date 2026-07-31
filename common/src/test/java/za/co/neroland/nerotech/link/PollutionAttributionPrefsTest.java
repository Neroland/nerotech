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
}
