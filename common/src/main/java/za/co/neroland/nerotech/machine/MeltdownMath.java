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

    /**
     * Resolves the {@code fusionMeltdownTerrainDamage} setting for a runtime: {@code "on"} → true,
     * {@code "off"} → false, {@code "auto"} → {@code !dedicatedServer} (off on a dedicated server,
     * on in singleplayer / LAN). Matching trims and ignores case; {@code null} or any unrecognised
     * value is treated as {@code "auto"}.
     *
     * <p>No separate "explosion breaks blocks" helper exists on purpose: the reactor picks
     * {@code ExplosionInteraction.BLOCK} vs {@code NONE} with a direct ternary on this boolean.
     *
     * @param mode            the raw config value
     * @param dedicatedServer {@code MinecraftServer.isDedicatedServer()} for the running server
     */
    public static boolean terrainDamage(String mode, boolean dedicatedServer) {
        String normalised = mode == null ? "auto" : mode.trim().toLowerCase(java.util.Locale.ROOT);
        return switch (normalised) {
            case "on" -> true;
            case "off" -> false;
            default -> !dedicatedServer;
        };
    }
}
