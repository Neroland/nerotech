package za.co.neroland.nerotech.machine.heat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import za.co.neroland.nerotech.heat.ThermalMath;

/**
 * Pure integer maths for a Heat Pipe <b>run</b>: a run of connected pipes is one thermal
 * node holding {@code runHeat} spread over its pipes, so its temperature is {@code runHeat / pipes} on the
 * machines' heat scale. On each exchange every endpoint conducts with the run through
 * {@link ThermalMath#conductionStep}, hottest endpoint first, so heat can pass from a hot machine through
 * the run into a cold one in the same exchange. Each pipe then sheds a small share of its heat above
 * ambient, so a long run carries less of what enters it.
 *
 * <p><b>Heat is never created.</b> Every move is exact: what an endpoint gives up is what the run
 * receives and vice versa, and the only other change is the loss, which only removes heat. No
 * {@code net.minecraft} import, so the conservation rule is tested on the plain JVM.
 */
public final class HeatRunMath {

    /** Highest pipe {@code heat} blockstate level. */
    public static final int MAX_LEVEL = 3;
    /** Level at or above which a pipe hurts to touch. */
    public static final int HOT_LEVEL = 2;

    private HeatRunMath() {
    }

    /** One endpoint of a run, as the maths sees it. Both moves are exact and never exceed {@code amount}. */
    public interface Node {

        /** Current heat on the machines' scale. */
        int heat();

        /** Remove up to {@code amount} heat; returns what was removed. */
        int take(int amount);

        /** Add up to {@code amount} heat; returns what was accepted. */
        int give(int amount);
    }

    /** Outcome of one exchange: the run's new heat and the heat its pipes lost to the surroundings. */
    public record Result(long runHeat, long lost) {
    }

    /** Per-pipe temperature of a run. */
    public static int temperature(long runHeat, int pipes) {
        if (pipes <= 0 || runHeat <= 0) {
            return 0;
        }
        return (int) Math.min(Integer.MAX_VALUE, runHeat / pipes);
    }

    /** The {@code heat} blockstate level (0..3) for a per-pipe temperature. */
    public static int level(int temperature, int capacity) {
        if (capacity <= 0 || temperature <= 0) {
            return 0;
        }
        return (int) Math.max(0, Math.min(MAX_LEVEL, (long) temperature * (MAX_LEVEL + 1) / capacity));
    }

    /**
     * The least per-pipe heat that shows as {@code level}: used to re-seed a run from its blockstates after
     * a reload. Because it is the minimum for that level, re-seeding can only lose heat, never create it.
     */
    public static int seedForLevel(int level, int capacity) {
        int clamped = Math.max(0, Math.min(MAX_LEVEL, level));
        if (clamped == 0 || capacity <= 0) {
            return 0;
        }
        long numerator = (long) clamped * capacity;
        return (int) ((numerator + MAX_LEVEL) / (MAX_LEVEL + 1));
    }

    /** Whether a pipe at {@code level} hurts to touch (config switch {@code enabled}). */
    public static boolean hurtsToTouch(int level, boolean enabled) {
        return enabled && level >= HOT_LEVEL;
    }

    /**
     * One exchange between a run and its endpoints.
     *
     * @param runHeat              heat held by the run (all pipes together)
     * @param pipes                pipes in the run (its thermal mass)
     * @param endpoints            machines and heat acceptors touching the run
     * @param conductivityPermille share of each endpoint-vs-run temperature difference moved
     * @param capacity             heat ceiling per pipe (the machines' {@code heatCapacity})
     * @param ambient              the surroundings' heat; pipes only lose toward it, never gain from it
     * @param lossPermillePerPipe  share of each pipe's heat above ambient lost per exchange
     */
    public static Result exchange(long runHeat, int pipes, List<? extends Node> endpoints, int conductivityPermille,
            int capacity, int ambient, int lossPermillePerPipe) {
        if (pipes <= 0) {
            return new Result(Math.max(0L, runHeat), 0L);
        }
        long heat = Math.max(0L, runHeat);
        long ceiling = (long) pipes * Math.max(0, capacity);
        if (conductivityPermille > 0 && !endpoints.isEmpty()) {
            List<Node> order = new ArrayList<>(endpoints);
            order.sort(Comparator.comparingInt(Node::heat).reversed());
            for (Node node : order) {
                int t = temperature(heat, pipes);
                int step = ThermalMath.conductionStep(node.heat(), t, conductivityPermille);
                if (step > 0) {
                    int room = (int) Math.max(0L, Math.min(Integer.MAX_VALUE, ceiling - heat));
                    int taken = node.take(Math.min(step, room));
                    heat += Math.max(0, taken);
                } else if (step < 0) {
                    int want = (int) Math.min(-(long) step, heat);
                    int given = node.give(want);
                    heat -= Math.max(0, Math.min(given, want));
                }
            }
        }
        long lost = 0L;
        int t = temperature(heat, pipes);
        if (lossPermillePerPipe > 0 && t > ambient && ambient >= 0) {
            long excess = heat - (long) ambient * pipes;
            lost = excess * Math.min(1000, lossPermillePerPipe) / 1000L;
            if (lost <= 0) {
                lost = 1;
            }
            lost = Math.min(lost, excess);
        } else if (lossPermillePerPipe > 0 && ambient < 0 && heat > 0) {
            // A sub-zero ambient still only drains what the run holds.
            lost = Math.max(1L, heat * Math.min(1000, lossPermillePerPipe) / 1000L);
            lost = Math.min(lost, heat);
        }
        heat -= lost;
        return new Result(heat, lost);
    }
}
