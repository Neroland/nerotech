package za.co.neroland.nerotech.api;

import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerotech.pollution.PollutionManager;

/**
 * Emit pollution into NeroTech's per-dimension region model from an add-on, exactly as NeroTech's own
 * machines do: same regions, same threshold events, same decay and scrubbing.
 *
 * <p>Privacy (POPIA/GDPR): the region model holds places, never people. The owner overload only
 * attributes the amount to {@code owner} when the server has per-player attribution switched on
 * <b>and</b> that player has not opted out; otherwise it is identical to the place-only overload.
 * Attributed totals are erased through Core's shared erasure hook like NeroTech's own.
 */
public final class Pollution {

    private Pollution() {
    }

    /** Emit {@code amount} pollution into {@code pos}'s region (place only, no attribution). */
    public static void record(ServerLevel level, BlockPos pos, int amount) {
        PollutionManager.record(level, pos, amount, null);
    }

    /**
     * Emit {@code amount} pollution into {@code pos}'s region and, only if the server and the player
     * allow it, attribute it to {@code owner} (the machine's owner, never a nearby player).
     */
    public static void record(ServerLevel level, BlockPos pos, int amount, @Nullable UUID owner) {
        PollutionManager.record(level, pos, amount, owner);
    }

    /** Current pollution in {@code pos}'s region of {@code level}. */
    public static int regionPollution(ServerLevel level, BlockPos pos) {
        return PollutionManager.regionPollution(level, pos);
    }
}
