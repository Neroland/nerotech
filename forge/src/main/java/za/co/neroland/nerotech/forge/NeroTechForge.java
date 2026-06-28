package za.co.neroland.nerotech.forge;

import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import za.co.neroland.nerotech.NeroTechCommon;
import za.co.neroland.nerotech.registry.ForgeRegistrationFactory;

/** MinecraftForge entry point for NeroTech. */
@Mod(NeroTechCommon.MOD_ID)
public final class NeroTechForge {

    public NeroTechForge(FMLJavaModLoadingContext context) {
        NeroTechCommon.LOGGER.info("[NeroTech] Forge bootstrap");
        BusGroup modBusGroup = context.getModBusGroup();
        // Shared init builds the DeferredRegisters via the RegistrationProvider seam;
        // attach them to NeroTech's mod bus group.
        NeroTechCommon.init();
        ForgeRegistrationFactory.registerAll(modBusGroup);
    }
}
