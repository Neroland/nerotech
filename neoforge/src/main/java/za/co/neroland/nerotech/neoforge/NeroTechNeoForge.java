package za.co.neroland.nerotech.neoforge;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

import za.co.neroland.nerotech.NeroTechCommon;

/** NeoForge entry point for NeroTech. */
@Mod(NeroTechCommon.MOD_ID)
public final class NeroTechNeoForge {

    public NeroTechNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        NeroTechCommon.LOGGER.info("[NeroTech] NeoForge bootstrap");
        NeroTechCommon.init();
    }
}
