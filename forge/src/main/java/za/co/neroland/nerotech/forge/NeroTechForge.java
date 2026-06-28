package za.co.neroland.nerotech.forge;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

import za.co.neroland.nerotech.NeroTechCommon;
import za.co.neroland.nerotech.pollution.PollutionManager;
import za.co.neroland.nerotech.registry.ForgeRegistrationFactory;
import za.co.neroland.nerotech.telemetry.NeroTechTelemetry;

/** MinecraftForge entry point for NeroTech. */
@Mod(NeroTechCommon.MOD_ID)
public final class NeroTechForge {

    public NeroTechForge(FMLJavaModLoadingContext context) {
        NeroTechCommon.LOGGER.info("[NeroTech] Forge bootstrap");
        BusGroup modBusGroup = context.getModBusGroup();
        // Shared init builds the DeferredRegisters via the RegistrationProvider seam;
        // attach them to NeroTech's mod bus group.
        NeroTechCommon.init();
        // Anonymous, NeroTech-only crash reporting (opt-out via config/nerotech.properties; off in dev unless DSN set).
        NeroTechTelemetry.init();
        ForgeRegistrationFactory.registerAll(modBusGroup);
        ForgeCapabilities.register();
        // Periodic regional pollution decay + retention sweep (game bus; gated by interval inside tick).
        TickEvent.ServerTickEvent.Post.BUS.addListener(event -> PollutionManager.tick(event.server()));
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ForgeClientSetup.init(modBusGroup);
        }
    }
}
