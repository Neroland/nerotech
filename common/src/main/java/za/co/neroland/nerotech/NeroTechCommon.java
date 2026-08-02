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
        // Declare NeroTech's payloads before each loader entry point wires them to its network API.
        za.co.neroland.nerotech.network.NeroTechNetwork.init();
        // NeroLink: register NeroTech's link module (data sections + safe actions + live events) with
        // Core's link registry, so the NeroLink companion bridge auto-serves NeroTech.
        za.co.neroland.nerotech.link.NeroTechLinkModule.register();
        // POPIA/GDPR: register the shared data-erasure hook so a single erase request clears every
        // per-player store NeroTech keeps — pollution attribution (UUIDs only; default attribution is
        // off), the per-player attribution opt-out preference (UUIDs only), and the Tech Guide "seen"
        // bitmasks (UUID-keyed; completion itself lives in vanilla advancements and is never stored).
        za.co.neroland.nerolandcore.data.PlayerDataErasure.register((server, uuid) -> {
            za.co.neroland.nerotech.pollution.PollutionManager.erasePlayer(server, uuid);
            za.co.neroland.nerotech.pollution.PollutionAttributionPrefs.get(server).forgetPlayer(uuid);
            za.co.neroland.nerotech.guide.TechGuideSeenState.get(server).forgetPlayer(uuid);
        });
    }
}
