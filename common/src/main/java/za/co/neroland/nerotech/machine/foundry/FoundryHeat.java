package za.co.neroland.nerotech.machine.foundry;

import za.co.neroland.nerotech.heat.ThermalMath;

/**
 * The Blast Foundry's heat rules, pure. The furnace heat a recipe's {@code min_heat} is checked
 * against has exactly one source: heat drained out of the structure's Foundry Heat Ports. Energy
 * never heats it, and the environment never warms it either; a foundry in a hot dimension simply
 * loses heat more slowly, because the loss is proportional to its lead over ambient.
 */
public final class FoundryHeat {

    private FoundryHeat() {
    }

    /** How much of {@code amount} a port buffer holding {@code stored} of {@code capacity} takes. */
    public static int portAccept(int stored, int capacity, int amount) {
        if (amount <= 0) {
            return 0;
        }
        return Math.max(0, Math.min(amount, capacity - Math.max(0, stored)));
    }

    /** How much of the ports' {@code offered} heat the furnace takes (room only; never negative). */
    public static int intake(int furnaceHeat, int capacity, int offered) {
        if (offered <= 0) {
            return 0;
        }
        return Math.max(0, Math.min(offered, capacity - Math.max(0, furnaceHeat)));
    }

    /**
     * One tick of furnace heat with no intake: a running batch draws {@code runningDraw}, then the
     * furnace sheds {@code lossPermille} of its lead over ambient. It never warms toward ambient and
     * never goes below zero, so with no port heat it can only fall.
     */
    public static int cool(int furnaceHeat, int ambient, int lossPermille, int runningDraw, boolean running) {
        int heat = Math.max(0, furnaceHeat);
        if (running) {
            heat -= Math.max(0, runningDraw);
        }
        if (heat > ambient) {
            heat += ThermalMath.ambientStep(heat, ambient, lossPermille); // negative: toward ambient
        }
        return Math.max(0, heat);
    }

    /** A port buffer as a glow bucket {@code 0 .. buckets - 1} (0 is the collar's "too cold" tell). */
    public static int bucket(int stored, int capacity, int buckets) {
        if (capacity <= 0 || stored <= 0 || buckets <= 0) {
            return 0;
        }
        return Math.min(buckets - 1, (int) ((long) stored * buckets / capacity));
    }
}
