package za.co.neroland.nerotech.pollution;

import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.data.SavedDataRecovery;
import za.co.neroland.nerolandcore.event.ThresholdEvents;
import za.co.neroland.nerolandcore.event.ThresholdEvents.ThresholdCrossing;

import za.co.neroland.nerotech.config.NeroTechConfig;
import za.co.neroland.nerotech.link.NeroTechLinkModule;

/**
 * Facade for NeroTech's regional pollution. Machines call {@link #record} from their own server tick
 * (batched, every N ticks — never a per-tick block scan); the per-loader server tick calls
 * {@link #tick} to run the periodic decay + retention sweep over the small region/attribution maps.
 *
 * <p>Pollution is regional and aggregate by default. Per-player attribution is opt-in
 * ({@code pollutionPerPlayerAttribution}); when off, no player data is ever stored.
 *
 * <p>Since 0.8.0 every reading belongs to the dimension it was produced in. The {@link ServerLevel}
 * overloads resolve the dimension from the level (existing callers, including add-ons, are unchanged);
 * the {@code (MinecraftServer, String dimensionId, ...)} overloads take it explicitly. Threshold-event
 * scopes are {@code <dimension id>|<region key>}.
 */
public final class PollutionManager {

    /** Coarse 64×64-block regions, packed into a long. */
    private static final int REGION_SHIFT = 6;

    /**
     * Core threshold-event channel for regional pollution crossings caused by machines: rising when a
     * region's emissions pass {@code pollutionEventThreshold}, falling only when a Scrubber or Remediator
     * actively removes enough to bring it back under. Scope = {@code <dimension id>|<packed region key>}
     * (a place; before 0.8.0 the scope was the bare region key).
     */
    public static final Identifier POLLUTION_CHANNEL = Identifier.fromNamespaceAndPath("nerotech", "pollution");

    /**
     * Companion channel for the periodic decay sweep: a region that merely drifted back under the
     * threshold by waiting fires here (falling), never on {@link #POLLUTION_CHANNEL}, so a listener can
     * tell cleanup from time passing. Same scope ({@code <dimension id>|<region key>}); never rises.
     */
    public static final Identifier POLLUTION_DECAY_CHANNEL =
            Identifier.fromNamespaceAndPath("nerotech", "pollution_decay");

    private PollutionManager() {
    }

    public static long regionKey(BlockPos pos) {
        long rx = pos.getX() >> REGION_SHIFT;
        long rz = pos.getZ() >> REGION_SHIFT;
        return (rx & 0xFFFFFFFFL) << 32 | (rz & 0xFFFFFFFFL);
    }

    /** The id string of a level's dimension, e.g. {@code minecraft:overworld}. */
    public static String dimensionId(ServerLevel level) {
        return level.dimension().identifier().toString();
    }

    /** Threshold-event scope for one region: {@code <dimension id>|<region key>} (a place, never a person). */
    public static String scope(String dimensionId, long regionKey) {
        return dimensionId + "|" + regionKey;
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
        record(server, dimensionId(level), pos, amount, owner);
    }

    /** {@link #record(ServerLevel, BlockPos, int, UUID)} with the dimension given explicitly. */
    public static void record(MinecraftServer server, String dimensionId, BlockPos pos, int amount,
            @Nullable UUID owner) {
        if (amount <= 0) {
            return;
        }
        PollutionState state = PollutionState.get(server);
        long key = regionKey(pos);
        int before = state.region(dimensionId, key);
        state.addRegion(dimensionId, key, amount);
        publishCrossing(dimensionId, key, before, before + amount);
        // Per-player attribution: opt-in globally AND not individually opted out (the NeroLink
        // set_pollution_attribution privacy control). Only own-data (the owner's own UUID) is ever
        // touched, and only when both gates pass (POPIA/GDPR).
        if (owner != null && NeroTechConfig.pollutionPerPlayerAttribution()
                && !PollutionAttributionPrefs.get(server).isOptedOut(owner)) {
            int attributedBefore = state.attributed(owner);
            state.attribute(owner, amount, today());
            NeroTechLinkModule.onAttributedPollution(server, owner, attributedBefore, attributedBefore + amount);
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
        return scrub(server, dimensionId(level), pos, amount, adjacentPermille);
    }

    /** {@link #scrub(ServerLevel, BlockPos, int, int)} with the dimension given explicitly. */
    public static int scrub(MinecraftServer server, String dimensionId, BlockPos pos, int amount, int adjacentPermille) {
        if (amount <= 0) {
            return 0;
        }
        PollutionState state = PollutionState.get(server);
        long rx = pos.getX() >> REGION_SHIFT;
        long rz = pos.getZ() >> REGION_SHIFT;
        int removed = takeAndPublish(state, dimensionId, key(rx, rz), amount);
        int adjacentAmount = (int) ((long) amount * adjacentPermille / 1000L);
        if (adjacentAmount > 0) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx != 0 || dz != 0) {
                        removed += takeAndPublish(state, dimensionId, key(rx + dx, rz + dz), adjacentAmount);
                    }
                }
            }
        }
        return removed;
    }

    private static int takeAndPublish(PollutionState state, String dimensionId, long key, int amount) {
        int before = state.region(dimensionId, key);
        int removed = state.takeRegion(dimensionId, key, amount);
        if (removed > 0) {
            publishCrossing(dimensionId, key, before, before - removed);
        }
        return removed;
    }

    /**
     * Publish a Core threshold event when a region's pollution crosses {@code pollutionEventThreshold}
     * in either direction (rising = worsening, falling = recovered). Dormant until a listener (the
     * intended consumer is NeroEvents) registers; the scope is {@code <dimension>|<region key>} — a place,
     * never a person.
     */
    private static void publishCrossing(String dimensionId, long key, int before, int after) {
        int threshold = NeroTechConfig.pollutionEventThreshold();
        if (threshold <= 0) {
            return;
        }
        boolean wasAbove = before >= threshold;
        boolean isAbove = after >= threshold;
        if (wasAbove != isAbove) {
            ThresholdEvents.fire("nerotech", new ThresholdCrossing(
                    POLLUTION_CHANNEL, scope(dimensionId, key), after, threshold, isAbove));
        }
    }

    private static long key(long rx, long rz) {
        return (rx & 0xFFFFFFFFL) << 32 | (rz & 0xFFFFFFFFL);
    }

    /** Current regional pollution at a position. */
    public static int regionPollution(ServerLevel level, BlockPos pos) {
        MinecraftServer server = level.getServer();
        return server == null ? 0 : regionPollution(server, dimensionId(level), pos);
    }

    /** Current regional pollution at a position in the given dimension. */
    public static int regionPollution(MinecraftServer server, String dimensionId, BlockPos pos) {
        return PollutionState.get(server).region(dimensionId, regionKey(pos));
    }

    /**
     * A coarse band for a reading against {@code pollutionEventThreshold}: {@code none}, {@code low}
     * (under the threshold), {@code high} (under twice it) or {@code severe}. Surfaces that show pollution
     * to players use this, never the raw value.
     */
    public static String band(int value) {
        if (value <= 0) {
            return "none";
        }
        int threshold = NeroTechConfig.pollutionEventThreshold();
        if (threshold <= 0 || value < threshold) {
            return "low";
        }
        return value < 2L * threshold ? "high" : "severe";
    }

    /** Periodic decay + retention prune. Cheap: runs only every configured interval, over small maps. */
    public static void tick(MinecraftServer server) {
        // This per-loader server tick (wired in every loader entry point) is also NeroTech's single
        // reliable handle on the running server, so the NeroLink module can resolve players/SavedData
        // when the companion bridge queries it. Cheap volatile write; no player data involved.
        NeroTechLinkModule.rememberServer(server);
        // The same per-tick hook drives two other cheap server-side clocks: the /nerotech budget window
        // (one volatile read when idle) and the once-per-in-game-day deposit prune.
        za.co.neroland.nerotech.command.TickBudget.serverTick(server);
        za.co.neroland.nerotech.deposit.DepositService.tick(server);
        // Worn tools and the pollution exposure pulse the Filter Mask blocks (one region read per player per pulse).
        za.co.neroland.nerotech.item.tools.WornTools.tick(server);
        int interval = NeroTechConfig.pollutionDecayIntervalTicks();
        if (server.getTickCount() % interval != 0) {
            return;
        }
        PollutionState state = PollutionState.get(server);
        int threshold = NeroTechConfig.pollutionEventThreshold();
        // Decay recoveries go on their own channel: nerotech:pollution falling is reserved for active removal.
        state.decayAll(NeroTechConfig.pollutionDecayAmount(), threshold, (dim, key) ->
                ThresholdEvents.fire("nerotech", new ThresholdCrossing(
                        POLLUTION_DECAY_CHANNEL, scope(dim, key), threshold - 1L, threshold, false)));
        state.pruneStale(NeroTechConfig.pollutionAttributionRetentionDays(), today());
    }

    /**
     * Shared data-erasure hook target (POPIA/GDPR). Refreshes Core's last-known-good backup at once
     * so the erasure reaches that second copy too (the {@link SavedDataRecovery} contract).
     */
    public static void erasePlayer(MinecraftServer server, UUID player) {
        PollutionState state = PollutionState.get(server);
        state.forgetPlayer(player);
        SavedDataRecovery.backupNow(server.overworld(), PollutionState.TYPE, state, PollutionState.ID.toString());
    }

    private static long today() {
        return System.currentTimeMillis() / 86_400_000L;
    }
}
