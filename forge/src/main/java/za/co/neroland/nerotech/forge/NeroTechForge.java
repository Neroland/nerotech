package za.co.neroland.nerotech.forge;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
//? if >=1.21.6 {
import net.minecraftforge.eventbus.api.bus.BusGroup;
//?} else {
/*import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
*///?}
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

import za.co.neroland.nerotech.NeroTechCommon;
import za.co.neroland.nerotech.command.NeroTechCommands;
import za.co.neroland.nerotech.registry.ForgeRegistrationFactory;
import za.co.neroland.nerotech.telemetry.NeroTechTelemetry;

/** MinecraftForge entry point for NeroTech. */
@Mod(NeroTechCommon.MOD_ID)
public final class NeroTechForge {

    public NeroTechForge(FMLJavaModLoadingContext context) {
        NeroTechCommon.LOGGER.info("[NeroTech] Forge bootstrap");
        //? if >=1.21.6 {
        BusGroup modBusGroup = context.getModBusGroup();
        //?} else {
        /*IEventBus modBusGroup = context.getModEventBus();
        *///?}
        // Shared init builds the DeferredRegisters via the RegistrationProvider seam;
        // attach them to NeroTech's mod bus group.
        NeroTechCommon.init();
        // The FluidTypes behind NeroTech's gas fluids — a Forge-only registry, so its register is
        // created here, after common init and before the registers are flushed to the bus group.
        ForgeFluidTypes.init();
        // Anonymous, NeroTech-only crash reporting (opt-out via config/nerotech.properties; off in dev unless DSN set).
        NeroTechTelemetry.init();
        ForgeRegistrationFactory.registerAll(modBusGroup);
        // NeroTech's own payloads (menu → machine position sync); see network.NeroTechNetwork.
        ForgeNetwork.register();
        ForgeCapabilities.register();
        // Periodic regional pollution decay + retention sweep and quest-hook delivery (game bus; gated inside).
        //? if >=1.21.9 {
        TickEvent.ServerTickEvent.Post.BUS.addListener(event -> NeroTechCommon.serverTick(event.server()));
        //?}
        //? if >=1.21.6 && <1.21.9 {
        /*TickEvent.ServerTickEvent.Post.BUS.addListener(event -> NeroTechCommon.serverTick(event.getServer()));
        *///?}
        //? if <1.21.6 {
        /*MinecraftForge.EVENT_BUS.addListener((TickEvent.ServerTickEvent.Post event) ->
                NeroTechCommon.serverTick(event.getServer()));
        *///?}
        // Creative-only debug commands (/nerotech gallery); shared brigadier tree in common.
        //? if >=1.21.6 {
        RegisterCommandsEvent.BUS.addListener(event ->
                NeroTechCommands.register(event.getDispatcher()));
        //?} else {
        /*MinecraftForge.EVENT_BUS.addListener((RegisterCommandsEvent event) ->
                NeroTechCommands.register(event.getDispatcher()));
        *///?}
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ForgeClientSetup.init(modBusGroup);
        }
    }
}
