package za.co.neroland.nerotech;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loader-agnostic entry point for NeroTech. Each loader entry point
 * (Fabric / Forge / NeoForge) calls {@link #init()} once during mod
 * construction. It builds the cross-loader content registries via the
 * {@link za.co.neroland.nerotech.registry.RegistrationProvider} seam; loader
 * specifics are reached through {@link za.co.neroland.nerotech.platform.Services}.
 */
public final class NeroTechCommon {

    public static final String MOD_ID = "nerotech";
    public static final Logger LOGGER = LoggerFactory.getLogger("NeroTech");

    private NeroTechCommon() {
    }

    /** Called once per loader during mod construction. */
    public static void init() {
        LOGGER.info("[NeroTech] common init");
        za.co.neroland.nerotech.config.NeroTechConfig.load();
        za.co.neroland.nerotech.registry.ModRegistries.init();
        // POPIA/GDPR: register the shared data-erasure hook so a single erase request also clears any
        // per-player pollution attribution NeroTech stored (UUIDs only; default attribution is off).
        za.co.neroland.nerolandcore.data.PlayerDataErasure.register(
                za.co.neroland.nerotech.pollution.PollutionManager::erasePlayer);
    }
}
