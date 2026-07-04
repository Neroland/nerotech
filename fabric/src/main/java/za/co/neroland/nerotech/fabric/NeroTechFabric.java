package za.co.neroland.nerotech.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;

import net.minecraft.world.level.block.entity.BlockEntityType;

import za.co.neroland.nerolandcore.platform.FabricEnergyLookup;

import za.co.neroland.nerotech.NeroTechCommon;
import za.co.neroland.nerotech.machine.NeroTechMachineBlockEntity;
import za.co.neroland.nerotech.pollution.PollutionManager;
import za.co.neroland.nerotech.registry.ModBlockEntities;
import za.co.neroland.nerotech.telemetry.NeroTechTelemetry;

/** Fabric entry point for NeroTech. Registration is eager; energy capability is wired here. */
public final class NeroTechFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        NeroTechCommon.LOGGER.info("[NeroTech] Fabric bootstrap");
        NeroTechCommon.init();
        // Anonymous, NeroTech-only crash reporting (opt-out via config/nerotech.properties; off in dev unless DSN set).
        NeroTechTelemetry.init();
        registerCoreEnergy();
        registerItemHandlers();
        // Periodic regional pollution decay + retention sweep (cheap; gated by interval inside tick).
        ServerTickEvents.END_SERVER_TICK.register(PollutionManager::tick);
    }

    /**
     * Expose every NeroTech machine's energy buffer on Core's shared {@code nerolandcore:energy} lookup,
     * so machines from any Nero mod interoperate on one power network.
     */
    private static void registerCoreEnergy() {
        energyHandler(ModBlockEntities.NERO_GENERATOR.get());
        energyHandler(ModBlockEntities.SOLAR_ARRAY.get());
        energyHandler(ModBlockEntities.ORE_PROCESSOR.get());
        energyHandler(ModBlockEntities.FABRICATOR.get());
        energyHandler(ModBlockEntities.FUSION_REACTOR.get());
        energyHandler(ModBlockEntities.ADVANCED_ORE_PROCESSOR.get());
        energyHandler(ModBlockEntities.ADVANCED_FABRICATOR.get());
        energyHandler(ModBlockEntities.AUTO_CRAFTER.get());
        energyHandler(ModBlockEntities.ITEM_SORTER.get());
    }

    /**
     * Side-config-gated energy view: a face exposes the buffer only when its ENERGY mode permits it; a
     * DISABLED face returns null (no API on that side). Machines without ENERGY side config fall back to
     * the ungated buffer.
     */
    private static <T extends NeroTechMachineBlockEntity> void energyHandler(BlockEntityType<T> type) {
        FabricEnergyLookup.ENERGY.registerForBlockEntity(
                (be, dir) -> be.sideConfig() != null ? be.sideConfig().energyView(dir) : be.getEnergy(), type);
    }

    /**
     * Item handoff surface (Stage 5): expose every machine's sided inventory on the Fabric Transfer API
     * item storage, so NeroLogistics / pipes / hoppers move items in and out with no NeroTech dependency.
     */
    private static void registerItemHandlers() {
        itemHandler(ModBlockEntities.NERO_GENERATOR.get());
        itemHandler(ModBlockEntities.SOLAR_ARRAY.get());
        itemHandler(ModBlockEntities.ORE_PROCESSOR.get());
        itemHandler(ModBlockEntities.FABRICATOR.get());
        itemHandler(ModBlockEntities.FUSION_REACTOR.get());
        itemHandler(ModBlockEntities.ADVANCED_ORE_PROCESSOR.get());
        itemHandler(ModBlockEntities.ADVANCED_FABRICATOR.get());
        itemHandler(ModBlockEntities.AUTO_CRAFTER.get());
        itemHandler(ModBlockEntities.ITEM_SORTER.get());
    }

    private static <T extends NeroTechMachineBlockEntity> void itemHandler(BlockEntityType<T> type) {
        ItemStorage.SIDED.registerForBlockEntity((be, dir) -> ContainerStorage.of(be, dir), type);
    }
}
