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
        // Add-on seam (0.4.0): seed the public MachineTypeRegistry with NeroTech's own machines FIRST,
        // so an add-on constructed after us (NeroPower) appends to a populated registry and every
        // loader's capability wiring — which reads the registry — sees NeroTech's types in front.
        za.co.neroland.nerotech.registry.ModBlockEntities.seed();
        za.co.neroland.nerotech.config.NeroTechConfig.load();
        za.co.neroland.nerotech.registry.ModRegistries.init();
        // Declare NeroTech's payloads before each loader entry point wires them to its network API.
        za.co.neroland.nerotech.network.NeroTechNetwork.init();
        // NeroLink: register NeroTech's link module (data sections + safe actions + live events) with
        // Core's link registry, so the NeroLink companion bridge auto-serves NeroTech.
        za.co.neroland.nerotech.link.NeroTechLinkModule.register();
        // POPIA/GDPR: register the shared data-erasure hook so a single erase request clears every
        // per-player store NeroTech keeps — pollution attribution (UUIDs only; default attribution is
        // off), the per-player attribution opt-out preference (UUIDs only), the Tech Guide "seen"
        // bitmasks (UUID-keyed; completion itself lives in vanilla advancements and is never stored),
        // and machine OWNERSHIP: the placing player's UUID lives in each machine's block entity, which
        // an erase request cannot reach in an unloaded chunk — so the erased UUID is recorded in the
        // prefs store's pending set (30-day expiry) and every machine drops a listed owner on its next
        // tick (loaded now) or its next load (unloaded). Each store's backup is refreshed at once so
        // the erasure reaches Core's last-known-good copy too (SavedDataRecovery contract).
        za.co.neroland.nerolandcore.data.PlayerDataErasure.register((server, uuid) -> {
            za.co.neroland.nerotech.pollution.PollutionManager.erasePlayer(server, uuid);
            za.co.neroland.nerotech.pollution.PollutionAttributionPrefs prefs =
                    za.co.neroland.nerotech.pollution.PollutionAttributionPrefs.get(server);
            prefs.forgetPlayer(uuid);
            prefs.markOwnerErasure(uuid, System.currentTimeMillis() / 86_400_000L);
            za.co.neroland.nerolandcore.data.SavedDataRecovery.backupNow(server.overworld(),
                    za.co.neroland.nerotech.pollution.PollutionAttributionPrefs.TYPE, prefs,
                    za.co.neroland.nerotech.pollution.PollutionAttributionPrefs.ID.toString());
            za.co.neroland.nerotech.guide.TechGuideSeenState seen =
                    za.co.neroland.nerotech.guide.TechGuideSeenState.get(server);
            seen.forgetPlayer(uuid);
            za.co.neroland.nerolandcore.data.SavedDataRecovery.backupNow(server.overworld(),
                    za.co.neroland.nerotech.guide.TechGuideSeenState.TYPE, seen,
                    za.co.neroland.nerotech.guide.TechGuideSeenState.ID.toString());
        });
    }
}
