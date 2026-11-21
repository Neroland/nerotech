package za.co.neroland.nerotech.machine.heat;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The cached runs of one level (decision 012). Positions are packed longs, and the world is reached only
 * through {@link Topology}, so the cache rules are tested on the plain JVM.
 *
 * <ul>
 *   <li>A run is resolved once, by a bounded flood fill (at most {@code cap} pipes) from the pipe that asks,
 *       and that pipe becomes the run's <b>anchor</b>: the only pipe that ticks for the run.</li>
 *   <li>A neighbour change at any pipe <b>dissolves</b> its run. Each pipe keeps an equal share of the run's
 *       heat as a pending share (the remainder of the division is lost, never created); the next run built
 *       over that pipe takes the share back. A split run therefore divides its heat and a merged run adds it.</li>
 *   <li>A pipe with no pending share (after a reload) is seeded with {@link HeatRunMath#seedForLevel}, the least
 *       heat its blockstate level can mean.</li>
 *   <li>Pending shares expire after {@value #PENDING_TTL} ticks (the pipe was broken), and a run is
 *       re-resolved after {@value #MAX_AGE} ticks so a change no neighbour update reported heals itself.</li>
 * </ul>
 */
public final class HeatRunIndex {

    /** Ticks a dissolved pipe keeps its share waiting for the next run. */
    public static final long PENDING_TTL = 400L;
    /** Ticks after which a run is dissolved and resolved again on its anchor's next tick. */
    public static final long MAX_AGE = 1200L;

    /** What the index needs to know about the world. */
    public interface Topology {

        /** Whether {@code pos} holds a heat pipe (and is loaded). */
        boolean isPipe(long pos);

        /** The pipe's {@code heat} blockstate level (0..3). */
        int level(long pos);

        /** The position one step from {@code pos} in direction {@code dir} (0..5). */
        long offset(long pos, int dir);
    }

    /** One resolved run. */
    public static final class Run {
        private final long anchor;
        private final long[] pipes;
        private final long builtAt;
        long heat;
        long nextExchange;
        int shownLevel;
        long[] endpoints = new long[0];

        Run(long anchor, long[] pipes, long heat, long builtAt, int shownLevel) {
            this.anchor = anchor;
            this.pipes = pipes;
            this.heat = heat;
            this.builtAt = builtAt;
            this.shownLevel = shownLevel;
        }

        public long anchor() {
            return this.anchor;
        }

        public long[] pipes() {
            return this.pipes;
        }

        public int size() {
            return this.pipes.length;
        }

        public long heat() {
            return this.heat;
        }

        public void heat(long value) {
            this.heat = Math.max(0L, value);
        }

        public long builtAt() {
            return this.builtAt;
        }

        public long nextExchange() {
            return this.nextExchange;
        }

        public void nextExchange(long tick) {
            this.nextExchange = tick;
        }

        /** The level last written to the run's blockstates. */
        public int shownLevel() {
            return this.shownLevel;
        }

        public void shownLevel(int level) {
            this.shownLevel = level;
        }

        /** Endpoint positions found when the run was resolved. */
        public long[] endpoints() {
            return this.endpoints;
        }

        public void endpoints(long[] positions) {
            this.endpoints = positions == null ? new long[0] : positions;
        }
    }

    private final Map<Long, Run> byPipe = new HashMap<>();
    private final Map<Long, long[]> pending = new HashMap<>(); // pos -> {share, since}

    /** The live run containing {@code pos}, or null. */
    public Run runAt(long pos) {
        return this.byPipe.get(pos);
    }

    /** Number of pipes owned by live runs (tests and the budget readout). */
    public int trackedPipes() {
        return this.byPipe.size();
    }

    /** Pending shares waiting for a run (tests). */
    public int pendingCount() {
        return this.pending.size();
    }

    /** Neighbour change at {@code pos}: dissolve the run holding it, if any. */
    public void invalidate(long pos, long now) {
        Run run = this.byPipe.get(pos);
        if (run != null) {
            dissolve(run, now);
        }
    }

    /** The pipe at {@code pos} was removed: dissolve its run and drop the removed pipe's own share. */
    public void remove(long pos, long now) {
        invalidate(pos, now);
        this.pending.remove(pos);
    }

    /** Dissolve {@code run}: every pipe keeps an equal share of its heat as a pending share. */
    public void dissolve(Run run, long now) {
        long share = run.size() == 0 ? 0L : run.heat / run.size();
        for (long p : run.pipes) {
            if (this.byPipe.get(p) == run) {
                this.byPipe.remove(p);
            }
            long[] old = this.pending.get(p);
            long carried = old == null ? 0L : old[0];
            this.pending.put(p, new long[] {carried + share, now});
        }
        run.heat = 0L;
    }

    /**
     * Resolve the run through {@code start} (a pipe that no live run owns), with {@code start} as its anchor.
     * Pipes owned by another live run are a boundary, so two runs never share a pipe.
     *
     * @return the new run, or null when {@code start} is not a pipe
     */
    public Run resolve(long start, Topology topology, int cap, int capacity, long now) {
        Run existing = this.byPipe.get(start);
        if (existing != null) {
            return existing;
        }
        if (!topology.isPipe(start)) {
            return null;
        }
        prune(now);
        List<Long> found = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        ArrayDeque<Long> queue = new ArrayDeque<>();
        queue.add(start);
        seen.add(start);
        while (!queue.isEmpty() && found.size() < Math.max(1, cap)) {
            long p = queue.poll();
            found.add(p);
            for (int dir = 0; dir < 6; dir++) {
                long q = topology.offset(p, dir);
                if (seen.add(q) && !this.byPipe.containsKey(q) && topology.isPipe(q)) {
                    queue.add(q);
                }
            }
        }
        long[] pipes = new long[found.size()];
        long heat = 0L;
        int startLevel = topology.level(start);
        for (int i = 0; i < pipes.length; i++) {
            long p = found.get(i);
            pipes[i] = p;
            long[] share = this.pending.remove(p);
            heat += share != null ? share[0] : HeatRunMath.seedForLevel(topology.level(p), capacity);
        }
        Run run = new Run(start, pipes, heat, now, startLevel);
        for (long p : pipes) {
            this.byPipe.put(p, run);
        }
        return run;
    }

    /** Whether {@code run} is due to be resolved again. */
    public static boolean stale(Run run, long now) {
        return now - run.builtAt >= MAX_AGE;
    }

    /** Drop pending shares older than {@value #PENDING_TTL} ticks (their pipe is gone). */
    private void prune(long now) {
        if (this.pending.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<Long, long[]>> it = this.pending.entrySet().iterator();
        while (it.hasNext()) {
            if (now - it.next().getValue()[1] > PENDING_TTL) {
                it.remove();
            }
        }
    }

    /** Total heat held by live runs and pending shares (conservation tests). */
    public long heldHeat() {
        long total = 0L;
        Set<Run> runs = new HashSet<>(this.byPipe.values());
        for (Run run : runs) {
            total += run.heat;
        }
        for (long[] share : this.pending.values()) {
            total += share[0];
        }
        return total;
    }
}
