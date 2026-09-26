package za.co.neroland.nerotech.machine;

/**
 * Pure arithmetic for the Fusion Reactor meltdown blast, kept Minecraft-free so it is unit-testable
 * without a loader (the block entity's static init needs FML/Fabric).
 */
public final class MeltdownMath {

    private MeltdownMath() {
    }

    /**
     * Blast radius for a shell of the given size under the configured cap: {@code shellSize + 1}
     * (4 / 6 / 8 for the 3³ / 5³ / 7³ shells), clamped to {@code cap} and never below 1.
     */
    public static int meltdownRadius(int shellSize, int cap) {
        return Math.max(1, Math.min(shellSize + 1, cap));
    }
}
