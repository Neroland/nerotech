package za.co.neroland.nerotech.fabric;

import net.fabricmc.api.ModInitializer;

import za.co.neroland.nerotech.NeroTechCommon;

/** Fabric entry point for NeroTech. */
public final class NeroTechFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        NeroTechCommon.LOGGER.info("[NeroTech] Fabric bootstrap");
        NeroTechCommon.init();
    }
}
