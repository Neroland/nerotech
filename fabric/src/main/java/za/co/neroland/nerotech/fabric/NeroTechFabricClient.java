package za.co.neroland.nerotech.fabric;

import net.fabricmc.api.ClientModInitializer;

import za.co.neroland.nerotech.NeroTechCommon;

/** Fabric client entry point for NeroTech. */
public final class NeroTechFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        NeroTechCommon.LOGGER.info("[NeroTech] Fabric client bootstrap");
    }
}
