package za.co.neroland.nerotech.deposit;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ChunkPos;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerotech.config.NeroTechConfig;
import za.co.neroland.nerotech.pollution.PollutionManager;

/**
 * The deposit API the Deep Core Drill and the Survey Scanner use. A lookup costs one seed derivation and
 * one map read, and callers make it per drill operation, never per tick. World data only.
 */
public final class DepositService {

    /** Records the daily sweep may remove in one pass. */
    public static final int PRUNE_LIMIT = 256;

    private DepositService() {
    }

    /** A chunk's deposit as a survey reads it: ore tag id, output item, capacity, units left, richness ‰. */
    public record Survey(String dimension, String ore, Item output, int capacity, int remaining, int richness) {
    }

    /** One extraction: units taken from the deposit and the item count they yield. */
    public record Extraction(String ore, Item output, int taken, int yield) {
    }

    /** Read-only survey; {@code null} when the dimension has no deposits. */
    @Nullable
    public static Survey survey(ServerLevel level, ChunkPos chunk) {
        return survey(level, chunk.getMinBlockX() >> 4, chunk.getMinBlockZ() >> 4);
    }

    /** Read-only survey by chunk coordinates. */
    @Nullable
    public static Survey survey(ServerLevel level, int chunkX, int chunkZ) {
        Resolved r = resolve(level, chunkX, chunkZ);
        if (r == null) {
            return null;
        }
        int remaining = NeroTechConfig.depositDepletionEnabled()
                ? DepositState.get(level.getServer()).peek(r.key, r.deposit.capacity(), now(level),
                        NeroTechConfig.depositRecoveryPerDay())
                : r.deposit.capacity();
        return new Survey(r.dimension, r.deposit.ore(), r.output, r.deposit.capacity(), remaining,
                r.deposit.richness());
    }

    /** Take up to {@code units} from the chunk's deposit; {@code null} when there is none or it is empty. */
    @Nullable
    public static Extraction extract(ServerLevel level, ChunkPos chunk, int units) {
        return extract(level, chunk.getMinBlockX() >> 4, chunk.getMinBlockZ() >> 4, units);
    }

    /** Extraction by chunk coordinates. */
    @Nullable
    public static Extraction extract(ServerLevel level, int chunkX, int chunkZ, int units) {
        if (units <= 0) {
            return null;
        }
        Resolved r = resolve(level, chunkX, chunkZ);
        if (r == null) {
            return null;
        }
        int taken = NeroTechConfig.depositDepletionEnabled()
                ? DepositState.get(level.getServer()).extract(r.key, r.deposit.capacity(), units, now(level),
                        NeroTechConfig.depositRecoveryPerDay())
                : units;
        if (taken <= 0) {
            return null;
        }
        return new Extraction(r.deposit.ore(), r.output, taken,
                yieldFor(taken, r.deposit.richness(), NeroTechConfig.depositYieldPermille()));
    }

    /** Item count for {@code taken} units at a richness and the config yield scale (both permille). Pure. */
    public static int yieldFor(int taken, int richnessPermille, int yieldPermille) {
        long v = (long) taken * richnessPermille / 1000L * yieldPermille / 1000L;
        return (int) Math.max(0L, Math.min(Integer.MAX_VALUE, v));
    }

    /** Whether {@code dimensionId} is in the comma-list {@code csv}. Pure. */
    public static boolean disabled(String dimensionId, String csv) {
        if (csv == null || csv.isBlank()) {
            return false;
        }
        for (String part : csv.split(",")) {
            if (part.trim().equals(dimensionId)) {
                return true;
            }
        }
        return false;
    }

    /** Once per in-game day: drop up to {@link #PRUNE_LIMIT} fully recovered records. */
    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % DepositState.DAY_TICKS != 0L) {
            return;
        }
        ServerLevel overworld = server.overworld();
        DepositState.get(server).prune(overworld.getGameTime(), NeroTechConfig.depositRecoveryPerDay(), PRUNE_LIMIT);
    }

    private record Resolved(String dimension, String key, DepositSeed.Deposit deposit, Item output) {
    }

    @Nullable
    private static Resolved resolve(ServerLevel level, int chunkX, int chunkZ) {
        String dim = PollutionManager.dimensionId(level);
        if (disabled(dim, NeroTechConfig.depositDisabledDimensions())) {
            return null;
        }
        MinecraftServer server = level.getServer();
        DepositTable table = DepositTable.forServer(server);
        java.util.List<DepositTable.Entry> entries = table.entries(dim);
        DepositSeed.Deposit deposit = DepositSeed.of(server.overworld().getSeed(), dim, chunkX, chunkZ, entries);
        if (deposit == null) {
            return null;
        }
        Item output = table.output(entries.get(deposit.entryIndex()));
        if (output == null) {
            return null;
        }
        return new Resolved(dim, DepositState.key(dim, chunkX, chunkZ), deposit, output);
    }

    /** One clock for every dimension: the overworld's game time (depletion is stored on the overworld). */
    private static long now(ServerLevel level) {
        return level.getServer().overworld().getGameTime();
    }
}
