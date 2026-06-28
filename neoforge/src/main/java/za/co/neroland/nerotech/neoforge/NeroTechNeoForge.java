package za.co.neroland.nerotech.neoforge;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

import za.co.neroland.nerotech.NeroTechCommon;
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
    }
}
