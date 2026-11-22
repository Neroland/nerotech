package za.co.neroland.nerotech.api;

import java.util.UUID;

import net.minecraft.server.MinecraftServer;

import za.co.neroland.nerotech.pollution.PollutionAttributionPrefs;

/**
 * The player's own-data control that NeroTech exposes through NeroLink
 * ({@code set_pollution_attribution}). An add-on that would record anything about a player as a side
 * effect of NeroTech's machines (an attribution, a milestone gate) should skip it when this returns
 * true, the same way NeroTech does.
 *
 * <p>Privacy (POPIA/GDPR): answers one yes/no question about one player the caller already knows;
 * it exposes no list of players and stores nothing.
 */
public final class PrivacyPrefs {

    private PrivacyPrefs() {
    }

    /** Whether {@code player} has opted out of NeroTech recording data about them. */
    public static boolean ownDataOptedOut(MinecraftServer server, UUID player) {
        if (server == null || player == null) {
            return false;
        }
        return PollutionAttributionPrefs.get(server).isOptedOut(player);
    }
}
