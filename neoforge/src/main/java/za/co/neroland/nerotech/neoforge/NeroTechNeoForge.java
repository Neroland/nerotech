package za.co.neroland.nerotech.neoforge;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper;

import net.minecraft.world.level.block.entity.BlockEntityType;

import za.co.neroland.nerolandcore.platform.NeoForgeEnergyLookup;

import za.co.neroland.nerotech.NeroTechCommon;
import za.co.neroland.nerotech.machine.NeroTechMachineBlockEntity;
import za.co.neroland.nerotech.pollution.PollutionManager;
import za.co.neroland.nerotech.registry.ModBlockEntities;
import za.co.neroland.nerotech.registry.NeoForgeRegistrationFactory;
import za.co.neroland.nerotech.telemetry.NeroTechTelemetry;

/** NeoForge entry point for NeroTech. */
@Mod(NeroTechCommon.MOD_ID)
public final class NeroTechNeoForge {

    public NeroTechNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        NeroTechCommon.LOGGER.info("[NeroTech] NeoForge bootstrap");
        // Shared init builds the DeferredRegisters via the RegistrationProvider seam;
        // attach them to NeroTech's mod event bus.
        NeroTechCommon.init();
        // Anonymous, NeroTech-only crash reporting (opt-out via config/nerotech.properties; off in dev unless DSN set).
        NeroTechTelemetry.init();
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
        energyCap(event, ModBlockEntities.NERO_GENERATOR.get());
        energyCap(event, ModBlockEntities.SOLAR_ARRAY.get());
        energyCap(event, ModBlockEntities.ORE_PROCESSOR.get());
        energyCap(event, ModBlockEntities.FABRICATOR.get());
        energyCap(event, ModBlockEntities.FUSION_REACTOR.get());
        energyCap(event, ModBlockEntities.ADVANCED_ORE_PROCESSOR.get());
        energyCap(event, ModBlockEntities.ADVANCED_FABRICATOR.get());
        energyCap(event, ModBlockEntities.AUTO_CRAFTER.get());
        energyCap(event, ModBlockEntities.ITEM_SORTER.get());

        // Item handoff surface (Stage 5): expose every machine's sided inventory on the standard item
        // capability so NeroLogistics / pipes / hoppers move items in and out with no NeroTech dependency.
        itemCap(event, ModBlockEntities.NERO_GENERATOR.get());
        itemCap(event, ModBlockEntities.SOLAR_ARRAY.get());
        itemCap(event, ModBlockEntities.ORE_PROCESSOR.get());
        itemCap(event, ModBlockEntities.FABRICATOR.get());
        itemCap(event, ModBlockEntities.FUSION_REACTOR.get());
        itemCap(event, ModBlockEntities.ADVANCED_ORE_PROCESSOR.get());
        itemCap(event, ModBlockEntities.ADVANCED_FABRICATOR.get());
        itemCap(event, ModBlockEntities.AUTO_CRAFTER.get());
        itemCap(event, ModBlockEntities.ITEM_SORTER.get());
    }

    /**
     * Side-config-gated energy view: a face exposes the buffer only when its ENERGY mode permits it
     * (insert-only / extract-only / both); a DISABLED face returns null. Machines with no ENERGY side
     * config fall back to the ungated buffer.
     */
    private static <T extends NeroTechMachineBlockEntity> void energyCap(RegisterCapabilitiesEvent event,
            BlockEntityType<T> type) {
        event.registerBlockEntity(NeoForgeEnergyLookup.ENERGY, type,
                (be, side) -> be.sideConfig() != null ? be.sideConfig().energyView(side) : be.getEnergy());
    }

    private static <T extends NeroTechMachineBlockEntity> void itemCap(RegisterCapabilitiesEvent event,
            BlockEntityType<T> type) {
        event.registerBlockEntity(Capabilities.Item.BLOCK, type,
                (be, side) -> side != null ? new WorldlyContainerWrapper(be, side) : VanillaContainerWrapper.of(be));
    }
}
