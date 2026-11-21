package za.co.neroland.nerotech.platform;

import java.util.List;

/**
 * Loader-specific facts the common module may depend on, resolved per loader via {@link Services}
 * ({@link java.util.ServiceLoader}). Kept intentionally small — currently only what telemetry needs
 * (release tag, environment, dist, and the loaded-mod list for crash triage). Grow as needed.
 */
public interface IPlatformHelper {

    /** Human-readable platform name ("Fabric" / "Forge" / "NeoForge"). */
    String getPlatformName();

    /** True when running in a development (IDE/dev) environment. */
    boolean isDevelopmentEnvironment();

    /** True on the physical client. */
    boolean isClient();

    /** NeroTech's version string (for telemetry release tags), or "unknown" if unavailable. */
    String getModVersion();

    /** Ids + versions of every loaded mod ("modid version"), sorted — public manifest strings only. */
    List<String> getLoadedModIds();

    /** Whether the mod with {@code modId} is loaded (gates optional bridges such as compat/neroeconomy). */
    boolean isModLoaded(String modId);

    /**
     * Whether {@code player} may break the block at {@code pos}, asked through the loader's own break
     * event (NeoForge and Forge {@code BlockEvent.BreakEvent}, Fabric {@code PlayerBlockBreakEvents.BEFORE})
     * so claim and protection mods can veto it. Nothing is broken here. Server thread only.
     */
    default boolean mayBreak(net.minecraft.server.level.ServerPlayer player, net.minecraft.server.level.ServerLevel level,
            net.minecraft.core.BlockPos pos) {
        return true;
    }
}
