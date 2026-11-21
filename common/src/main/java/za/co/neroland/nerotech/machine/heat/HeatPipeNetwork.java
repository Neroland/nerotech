package za.co.neroland.nerotech.machine.heat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import za.co.neroland.nerotech.config.NeroTechConfig;
import za.co.neroland.nerotech.heat.ThermalEnvironment;

/**
 * Drives Heat Pipe runs (decision 012): one {@link HeatRunIndex} per level, held weakly so an unloaded level
 * drops its cache. Pipes have no block entity. Each run's <b>anchor</b> pipe carries a scheduled block tick
 * on the exchange interval; every other pipe of the run stays still. Endpoints are found when the run is
 * resolved (on a neighbour change, or when the run ages out), never per tick. Server thread only.
 */
public final class HeatPipeNetwork {

    /** Most endpoints one run trades with. */
    public static final int MAX_ENDPOINTS = 32;

    private static final Map<Level, HeatRunIndex> INDEX = new WeakHashMap<>();

    private HeatPipeNetwork() {
    }

    static HeatRunIndex index(Level level) {
        synchronized (INDEX) {
            return INDEX.computeIfAbsent(level, k -> new HeatRunIndex());
        }
    }

    /** A neighbour of the pipe at {@code pos} changed: drop its run and wake the pipe to rebuild it. */
    public static void invalidate(ServerLevel level, BlockPos pos, Block block) {
        index(level).invalidate(pos.asLong(), level.getGameTime());
        schedule(level, pos, block, 1);
    }

    /** Schedule a tick at {@code pos} unless one is already pending. */
    static void schedule(ServerLevel level, BlockPos pos, Block block, int delay) {
        if (!level.getBlockTicks().hasScheduledTick(pos, block)) {
            level.scheduleTick(pos, block, Math.max(1, delay));
        }
    }

    /** Random-tick backstop: a pipe whose run has no ticking anchor wakes it. */
    public static void heal(ServerLevel level, BlockPos pos, Block block) {
        HeatRunIndex.Run run = index(level).runAt(pos.asLong());
        BlockPos target = run == null ? pos : BlockPos.of(run.anchor());
        if (level.isLoaded(target) && level.getBlockState(target).is(block)) {
            schedule(level, target, block, HeatSettings.heatPipeExchangeIntervalTicks());
        } else if (run != null) {
            index(level).dissolve(run, level.getGameTime());
            schedule(level, pos, block, 1);
        }
    }

    /** The scheduled tick of the pipe at {@code pos}. */
    public static void tick(ServerLevel level, BlockPos pos, Block block) {
        long now = level.getGameTime();
        long key = pos.asLong();
        HeatRunIndex index = index(level);
        int capacity = NeroTechConfig.heatCapacity();
        HeatRunIndex.Run run = index.runAt(key);
        if (run != null && run.anchor() == key && HeatRunIndex.stale(run, now)) {
            index.dissolve(run, now);
            run = null;
        }
        if (run == null) {
            run = index.resolve(key, topology(level, block), HeatSettings.heatPipeMaxRun(), capacity, now);
            if (run == null) {
                return;
            }
            run.endpoints(findEndpoints(level, run, block));
            run.shownLevel(-1);
            run.nextExchange(now);
        }
        if (run.anchor() != key) {
            return;
        }
        int interval = Math.max(1, HeatSettings.heatPipeExchangeIntervalTicks());
        if (now >= run.nextExchange()) {
            run.nextExchange(now + interval);
            if (!exchange(level, run, capacity)) {
                // An endpoint vanished without a neighbour update reaching us: rebuild next tick.
                index.dissolve(run, now);
                schedule(level, pos, block, 1);
                return;
            }
            showLevel(level, run, block, capacity);
        }
        schedule(level, pos, block, interval);
    }

    private static boolean exchange(ServerLevel level, HeatRunIndex.Run run, int capacity) {
        long[] endpoints = run.endpoints();
        List<HeatRunMath.Node> nodes = new ArrayList<>(endpoints.length / 2);
        for (int i = 0; i + 1 < endpoints.length; i += 2) {
            BlockPos at = BlockPos.of(endpoints[i]);
            if (!level.isLoaded(at)) {
                return false;
            }
            HeatRunMath.Node node = HeatNodes.node(level, at, Direction.values()[(int) endpoints[i + 1]]);
            if (node == null) {
                return false;
            }
            nodes.add(node);
        }
        int ambient = ThermalEnvironment.ambientAt(level, BlockPos.of(run.anchor()));
        HeatRunMath.Result result = HeatRunMath.exchange(run.heat(), run.size(), nodes,
                HeatSettings.heatPipeConductivityPermille(), capacity, ambient, HeatSettings.heatPipeLossPermille());
        run.heat(result.runHeat());
        return true;
    }

    /** Write the run's heat level to its pipes' blockstates, only when the level changed. */
    private static void showLevel(ServerLevel level, HeatRunIndex.Run run, Block block, int capacity) {
        int shown = HeatRunMath.level(HeatRunMath.temperature(run.heat(), run.size()), capacity);
        if (shown == run.shownLevel()) {
            return;
        }
        run.shownLevel(shown);
        for (long p : run.pipes()) {
            BlockPos at = BlockPos.of(p);
            if (!level.isLoaded(at)) {
                continue;
            }
            BlockState state = level.getBlockState(at);
            if (state.is(block) && state.getValue(HeatPipeBlock.HEAT) != shown) {
                // Clients only, no neighbour or shape updates: a heat change never cascades.
                level.setBlock(at, state.setValue(HeatPipeBlock.HEAT, shown),
                        Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            }
        }
    }

    private static long[] findEndpoints(ServerLevel level, HeatRunIndex.Run run, Block block) {
        List<Long> found = new ArrayList<>();
        java.util.Set<Long> seen = new java.util.HashSet<>();
        outer:
        for (long p : run.pipes()) {
            BlockPos pipe = BlockPos.of(p);
            for (Direction dir : Direction.values()) {
                BlockPos at = pipe.relative(dir);
                if (!level.isLoaded(at) || level.getBlockState(at).is(block) || !seen.add(at.asLong())) {
                    continue;
                }
                if (HeatNodes.isEndpoint(level, at, dir.getOpposite())) {
                    found.add(at.asLong());
                    found.add((long) dir.getOpposite().ordinal());
                    if (found.size() >= MAX_ENDPOINTS * 2) {
                        break outer;
                    }
                }
            }
        }
        long[] out = new long[found.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = found.get(i);
        }
        return out;
    }

    private static HeatRunIndex.Topology topology(ServerLevel level, Block block) {
        return new HeatRunIndex.Topology() {
            @Override
            public boolean isPipe(long pos) {
                BlockPos at = BlockPos.of(pos);
                return level.isLoaded(at) && level.getBlockState(at).is(block);
            }

            @Override
            public int level(long pos) {
                BlockState state = level.getBlockState(BlockPos.of(pos));
                return state.is(block) ? state.getValue(HeatPipeBlock.HEAT) : 0;
            }

            @Override
            public long offset(long pos, int dir) {
                return BlockPos.offset(pos, Direction.values()[dir]);
            }
        };
    }
}
