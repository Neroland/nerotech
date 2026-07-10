package za.co.neroland.nerotech.pollution;

import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.event.ThresholdEvents;
import za.co.neroland.nerolandcore.event.ThresholdEvents.ThresholdCrossing;

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

    /** Core threshold-event channel for regional pollution crossings (Stage F, dormant until NeroEvents). */
    private static final Identifier POLLUTION_CHANNEL = Identifier.fromNamespaceAndPath("nerotech", "pollution");

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
        long key = regionKey(pos);
        int before = state.region(key);
        state.addRegion(key, amount);
        publishCrossing(key, before, before + amount);
        if (owner != null && NeroTechConfig.pollutionPerPlayerAttribution()) {
            state.attribute(owner, amount, today());
        }
    }

    /**
     * Stage F mitigation: remove up to {@code amount} pollution from {@code pos}'s own region,
     * plus {@code adjacentPermille}‰ of it from each of the 8 neighbouring regions (0 = own
     * region only). Returns the total actually removed. Publishes "recovered" threshold
     * crossings via Core's event bus. Region/aggregate data only — no player data.
     */
    public static int scrub(ServerLevel level, BlockPos pos, int amount, int adjacentPermille) {
        if (amount <= 0) {
            return 0;
        }
        MinecraftServer server = level.getServer();
        if (server == null) {
            return 0;
        }
        PollutionState state = PollutionState.get(server);
        long rx = pos.getX() >> REGION_SHIFT;
        long rz = pos.getZ() >> REGION_SHIFT;
        int removed = takeAndPublish(state, key(rx, rz), amount);
        int adjacentAmount = (int) ((long) amount * adjacentPermille / 1000L);
        if (adjacentAmount > 0) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx != 0 || dz != 0) {
                        removed += takeAndPublish(state, key(rx + dx, rz + dz), adjacentAmount);
                    }
                }
            }
        }
        return removed;
    }

    private static int takeAndPublish(PollutionState state, long key, int amount) {
        int before = state.region(key);
        int removed = state.takeRegion(key, amount);
        if (removed > 0) {
            publishCrossing(key, before, before - removed);
        }
        return removed;
    }

    /**
     * Publish a Core threshold event when a region's pollution crosses {@code pollutionEventThreshold}
     * in either direction (rising = worsening, falling = recovered). Dormant until a listener (the
     * intended consumer is NeroEvents) registers; the scope is a region key — a place, never a person.
     */
    private static void publishCrossing(long key, int before, int after) {
        int threshold = NeroTechConfig.pollutionEventThreshold();
        if (threshold <= 0) {
            return;
        }
        boolean wasAbove = before >= threshold;
        boolean isAbove = after >= threshold;
        if (wasAbove != isAbove) {
            ThresholdEvents.fire(new ThresholdCrossing(
                    POLLUTION_CHANNEL, Long.toString(key), after, threshold, isAbove));
        }
    }

    private static long key(long rx, long rz) {
        return (rx & 0xFFFFFFFFL) << 32 | (rz & 0xFFFFFFFFL);
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
        int threshold = NeroTechConfig.pollutionEventThreshold();
        state.decay(NeroTechConfig.pollutionDecayAmount(), threshold, key ->
                ThresholdEvents.fire(new ThresholdCrossing(
                        POLLUTION_CHANNEL, Long.toString(key), threshold - 1L, threshold, false)));
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
