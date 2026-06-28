package za.co.neroland.nerotech.platform;

import java.util.ServiceLoader;

import za.co.neroland.nerotech.NeroTechCommon;

/**
 * Loads loader-specific service implementations via {@link ServiceLoader} — the
 * lightweight, dependency-free alternative to Architectury's {@code @ExpectPlatform}.
 *
 * <p>Common code resolves a per-loader implementation from the {@code META-INF/services}
 * entry shipped by each loader module (Fabric / Forge / NeoForge). NeroTech keeps its
 * own seam (mirroring Nerospace) rather than reusing Core's: Core's NeoForge/Forge
 * registration factory flushes its {@code DeferredRegister}s to Core's mod bus during
 * Core construction, which runs before any downstream {@code init()} — so NeroTech must
 * own its registration seam to attach to NeroTech's mod bus at the right time.
 */
public final class Services {

    /** Loader-specific facts (version, environment, dist, loaded mods) — used by telemetry. */
    public static final IPlatformHelper PLATFORM = load(IPlatformHelper.class);

    private Services() {
    }

    public static <T> T load(Class<T> clazz) {
        final T loaded = ServiceLoader.load(clazz)
                .findFirst()
                .orElseThrow(() -> new NullPointerException(
                        "No implementation found for service " + clazz.getName()));
        NeroTechCommon.LOGGER.debug("Loaded service {} -> {}",
                clazz.getSimpleName(), loaded.getClass().getName());
        return loaded;
    }
}
