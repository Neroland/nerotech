package za.co.neroland.nerotech.forge;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import za.co.neroland.nerotech.NeroTechCommon;

/** MinecraftForge entry point for NeroTech. */
@Mod(NeroTechCommon.MOD_ID)
public final class NeroTechForge {

    public NeroTechForge(FMLJavaModLoadingContext context) {
        NeroTechCommon.LOGGER.info("[NeroTech] Forge bootstrap");
        NeroTechCommon.init();
    }
}
