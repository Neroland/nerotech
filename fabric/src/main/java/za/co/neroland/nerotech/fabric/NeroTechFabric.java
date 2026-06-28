package za.co.neroland.nerotech.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import za.co.neroland.nerolandcore.platform.FabricEnergyLookup;

import za.co.neroland.nerotech.NeroTechCommon;
import za.co.neroland.nerotech.pollution.PollutionManager;
import za.co.neroland.nerotech.registry.ModBlockEntities;

/** Fabric entry point for NeroTech. Registration is eager; energy capability is wired here. */
public final class NeroTechFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        NeroTechCommon.LOGGER.info("[NeroTech] Fabric bootstrap");
        NeroTechCommon.init();
        registerCoreEnergy();
        // Periodic regional pollution decay + retention sweep (cheap; gated by interval inside tick).
        ServerTickEvents.END_SERVER_TICK.register(PollutionManager::tick);
    }

    /**
     * Expose every NeroTech machine's energy buffer on Core's shared {@code nerolandcore:energy} lookup,
     * so machines from any Nero mod interoperate on one power network.
     */
    private static void registerCoreEnergy() {
        FabricEnergyLookup.ENERGY.registerForBlockEntity((be, dir) -> be.getEnergy(), ModBlockEntities.NERO_GENERATOR.get());
        FabricEnergyLookup.ENERGY.registerForBlockEntity((be, dir) -> be.getEnergy(), ModBlockEntities.SOLAR_ARRAY.get());
        FabricEnergyLookup.ENERGY.registerForBlockEntity((be, dir) -> be.getEnergy(), ModBlockEntities.ORE_PROCESSOR.get());
        FabricEnergyLookup.ENERGY.registerForBlockEntity((be, dir) -> be.getEnergy(), ModBlockEntities.FABRICATOR.get());
    }
}
