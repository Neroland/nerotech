package za.co.neroland.nerotech.item.tools;

/**
 * The energy and cooldown rules every NeroTech tool shares, without Minecraft types (unit-tested): an
 * action costs NE and starts a cooldown, a tool never holds more than its capacity, charging fills only
 * the room left, and a worn tool's drain never takes it below zero. No player data.
 */
public final class ToolRules {

    private ToolRules() {
    }

    /** What an attempted action does. Cooldown wins over energy. */
    public enum Outcome {
        OK,
        COOLDOWN,
        NO_ENERGY
    }

    public static Outcome attempt(int stored, int cost, long now, long readyAt) {
        if (now < readyAt) {
            return Outcome.COOLDOWN;
        }
        return stored >= Math.max(0, cost) ? Outcome.OK : Outcome.NO_ENERGY;
    }

    /** The charge after an action at {@code now}: cost taken, next action at {@code now + cooldown}. */
    public static ToolCharge afterUse(ToolCharge charge, int cost, long now, int cooldownTicks) {
        return new ToolCharge(Math.max(0, charge.energy() - Math.max(0, cost)), now + Math.max(0, cooldownTicks),
                charge.mode());
    }

    /** NE a tool holding {@code stored} of {@code capacity} accepts from {@code offered}. */
    public static int chargeAmount(int stored, int capacity, long offered) {
        long room = Math.max(0L, (long) capacity - Math.max(0, stored));
        return (int) Math.max(0L, Math.min(room, offered));
    }

    /** NE a worn tool holding {@code stored} loses to a drain of {@code drain}: never more than it holds. */
    public static int drained(int stored, int drain) {
        return Math.max(0, stored - Math.max(0, drain));
    }

    /**
     * How many blocks a {@code perBlock} cost lets a tool holding {@code stored} break, capped at
     * {@code cap}. A free action ({@code perBlock <= 0}) is limited by the cap alone.
     */
    public static int affordable(int stored, int perBlock, int cap) {
        if (cap <= 0) {
            return 0;
        }
        if (perBlock <= 0) {
            return cap;
        }
        return Math.max(0, Math.min(cap, stored / perBlock));
    }

    /** Cutting Lance: whether a cut fires from this hand. The main hand only (review M1: no off-hand cuts). */
    public static boolean lanceFiresFrom(boolean mainHand) {
        return mainHand;
    }

    /**
     * Cutting Lance, Glacite lens: whether the lance must still drop the preserved block itself — only when
     * the break path dropped no item of that block, so one cut never yields it twice.
     */
    public static <T> boolean needsPreservedDrop(java.util.Collection<T> newDropItems, T blockItem) {
        return !newDropItems.contains(blockItem);
    }
}
