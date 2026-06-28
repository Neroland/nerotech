package za.co.neroland.nerotech.neoforge;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import za.co.neroland.nerolandcore.platform.NeoForgeEnergyLookup;

import za.co.neroland.nerotech.NeroTechCommon;
import za.co.neroland.nerotech.pollution.PollutionManager;
import za.co.neroland.nerotech.registry.ModBlockEntities;
import za.co.neroland.nerotech.registry.NeoForgeRegistrationFactory;

/** NeoForge entry point for NeroTech. */
@Mod(NeroTechCommon.MOD_ID)
public final class NeroTechNeoForge {

    public NeroTechNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        NeroTechCommon.LOGGER.info("[NeroTech] NeoForge bootstrap");
        // Shared init builds the DeferredRegisters via the RegistrationProvider seam;
        // attach them to NeroTech's mod event bus.
        NeroTechCommon.init();
        NeoForgeRegistrationFactory.registerAll(modEventBus);
        modEventBus.addListener(NeroTechNeoForge::onRegisterCapabilities);
        // Periodic regional pollution decay + retention sweep (game bus; gated by interval inside tick).
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> PollutionManager.tick(event.getServer()));
        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            NeoForgeClientSetup.init(modEventBus);
        }
    }

    /** Expose each machine's energy buffer on Core's shared {@code nerolandcore:energy} capability. */
    private static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(NeoForgeEnergyLookup.ENERGY, ModBlockEntities.NERO_GENERATOR.get(),
                (be, side) -> be.getEnergy());
        event.registerBlockEntity(NeoForgeEnergyLookup.ENERGY, ModBlockEntities.SOLAR_ARRAY.get(),
                (be, side) -> be.getEnergy());
        event.registerBlockEntity(NeoForgeEnergyLookup.ENERGY, ModBlockEntities.ORE_PROCESSOR.get(),
                (be, side) -> be.getEnergy());
        event.registerBlockEntity(NeoForgeEnergyLookup.ENERGY, ModBlockEntities.FABRICATOR.get(),
                (be, side) -> be.getEnergy());
    }
}
