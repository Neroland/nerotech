package za.co.neroland.nerotech.pollution;

import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerotech.config.NeroTechConfig;

/**
 * Facade for NeroTech's regional pollution. Machines call {@link #record} from their own server tick
 * (batched, every N ticks — never a per-tick block scan); the per-loader server tick calls
 * {@link #tick} to run the periodic decay + retention sweep over the small region/attribution maps.
 *
 * <p>Pollution is regional and aggregate by default. Per-player attribution is opt-in
 * ({@code pollutionPerPlayerAttribution}); when off, no player data is ever stored.
 */
public final class PollutionManager {

    /** Coarse 64×64-block regions, packed into a long. */
    private static final int REGION_SHIFT = 6;

    private PollutionManager() {
    }

    public static long regionKey(BlockPos pos) {
        long rx = pos.getX() >> REGION_SHIFT;
        long rz = pos.getZ() >> REGION_SHIFT;
        return (rx & 0xFFFFFFFFL) << 32 | (rz & 0xFFFFFFFFL);
    }

    /** Emit {@code amount} pollution into {@code pos}'s region; attribute to {@code owner} only if opted in. */
    public static void record(ServerLevel level, BlockPos pos, int amount, @Nullable UUID owner) {
        if (amount <= 0) {
            return;
        }
        MinecraftServer server = level.getServer();
        if (server == null) {
            return;
        }
        PollutionState state = PollutionState.get(server);
        state.addRegion(regionKey(pos), amount);
        if (owner != null && NeroTechConfig.pollutionPerPlayerAttribution()) {
            state.attribute(owner, amount, today());
        }
    }

    /** Current regional pollution at a position. */
    public static int regionPollution(ServerLevel level, BlockPos pos) {
        MinecraftServer server = level.getServer();
        return server == null ? 0 : PollutionState.get(server).region(regionKey(pos));
    }

    /** Periodic decay + retention prune. Cheap: runs only every configured interval, over small maps. */
    public static void tick(MinecraftServer server) {
        int interval = NeroTechConfig.pollutionDecayIntervalTicks();
        if (server.getTickCount() % interval != 0) {
            return;
        }
        PollutionState state = PollutionState.get(server);
        state.decay(NeroTechConfig.pollutionDecayAmount());
        state.pruneStale(NeroTechConfig.pollutionAttributionRetentionDays(), today());
    }

    /** Shared data-erasure hook target (POPIA/GDPR). */
    public static void erasePlayer(MinecraftServer server, UUID player) {
        PollutionState.get(server).forgetPlayer(player);
    }

    private static long today() {
        return System.currentTimeMillis() / 86_400_000L;
    }
}
