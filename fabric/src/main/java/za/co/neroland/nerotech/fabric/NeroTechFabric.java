package za.co.neroland.nerotech.fabric;

import java.util.function.Supplier;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.recipe.v1.sync.RecipeSynchronization;
import net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;

import net.minecraft.world.level.block.entity.BlockEntityType;

import za.co.neroland.nerolandcore.platform.FabricEnergyLookup;
import za.co.neroland.nerolandcore.platform.FabricFluidLookup;
import za.co.neroland.nerolandcore.platform.FabricGasLookup;

import za.co.neroland.nerotech.NeroTechCommon;
import za.co.neroland.nerotech.command.NeroTechCommands;
import za.co.neroland.nerotech.machine.NeroTechMachineBlockEntity;
import za.co.neroland.nerotech.pollution.PollutionManager;
import za.co.neroland.nerotech.registry.ModBlockEntities;
import za.co.neroland.nerotech.registry.ModRecipeTypes;
import za.co.neroland.nerotech.telemetry.NeroTechTelemetry;

/** Fabric entry point for NeroTech. Registration is eager; energy capability is wired here. */
public final class NeroTechFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        NeroTechCommon.LOGGER.info("[NeroTech] Fabric bootstrap");
        NeroTechCommon.init();
        // Anonymous, NeroTech-only crash reporting (opt-out via config/nerotech.properties; off in dev unless DSN set).
        NeroTechTelemetry.init();
        // NeroTech's own payloads (menu → machine position sync); see network.NeroTechNetwork.
        FabricNetwork.registerCommon();
        registerCoreEnergy();
        registerItemHandlers();
        registerCoreFluidAndGas();
        registerRecipeSync();
        // Periodic regional pollution decay + retention sweep (cheap; gated by interval inside tick).
        ServerTickEvents.END_SERVER_TICK.register(PollutionManager::tick);
        // Creative-only debug commands (/nerotech gallery); shared brigadier tree in common.
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                NeroTechCommands.register(dispatcher));
    }

    /**
     * Expose every NeroTech machine's energy buffer on Core's shared {@code nerolandcore:energy} lookup,
     * so machines from any Nero mod interoperate on one power network.
     */
    private static void registerCoreEnergy() {
        for (Supplier<BlockEntityType<? extends NeroTechMachineBlockEntity>> type
                : ModBlockEntities.energyMachineTypes()) {
            energyHandler(machineType(type.get()));
        }
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
        for (Supplier<BlockEntityType<? extends NeroTechMachineBlockEntity>> type
                : ModBlockEntities.itemMachineTypes()) {
            itemHandler(machineType(type.get()));
        }
    }

    private static <T extends NeroTechMachineBlockEntity> void itemHandler(BlockEntityType<T> type) {
        ItemStorage.SIDED.registerForBlockEntity((be, dir) -> ContainerStorage.of(be, dir), type);
    }

    /**
     * Stage C: expose the fluid/gas machines' tanks on Core's shared {@code nerolandcore:fluid} and
     * {@code nerolandcore:gas} lookups, so NeroTech's gas chain interoperates with Core's Fluid/Gas
     * Tanks — and any other mod on those surfaces — with no cross-mod dependency.
     */
    private static void registerCoreFluidAndGas() {
        for (Supplier<BlockEntityType<? extends NeroTechMachineBlockEntity>> type
                : ModBlockEntities.gasMachineTypes()) {
            gasHandler(machineType(type.get()));
        }
        for (Supplier<BlockEntityType<? extends NeroTechMachineBlockEntity>> type
                : ModBlockEntities.fluidMachineTypes()) {
            fluidHandler(machineType(type.get()));
        }
    }

    private static <T extends NeroTechMachineBlockEntity> void gasHandler(BlockEntityType<T> type) {
        FabricGasLookup.GAS.registerForBlockEntity((be, dir) -> be.gasStorage(dir), type);
    }

    private static <T extends NeroTechMachineBlockEntity> void fluidHandler(BlockEntityType<T> type) {
        FabricFluidLookup.FLUID.registerForBlockEntity((be, dir) -> be.fluidStorage(dir), type);
    }

    /**
     * Recipe sync is opt-in on Fabric (26.x clients hold no full recipe list). Without this the client
     * never receives NeroTech's machine recipes and recipe viewers — see {@code compat.jei} — would
     * show empty pages. Recipe definitions only; no player data crosses the wire.
     */
    private static void registerRecipeSync() {
        RecipeSynchronization.synchronizeRecipeSerializer(ModRecipeTypes.ORE_PROCESSING_SERIALIZER.get());
        RecipeSynchronization.synchronizeRecipeSerializer(ModRecipeTypes.FABRICATING_SERIALIZER.get());
        RecipeSynchronization.synchronizeRecipeSerializer(ModRecipeTypes.ADVANCED_FABRICATING_SERIALIZER.get());
        RecipeSynchronization.synchronizeRecipeSerializer(ModRecipeTypes.COLLIDER_SERIALIZER.get());
        RecipeSynchronization.synchronizeRecipeSerializer(ModRecipeTypes.CHEMICAL_PROCESSING_SERIALIZER.get());
    }

    /**
     * Re-brands a wildcard machine type as the exact type the registration helpers want. Safe by
     * construction: the lists in {@code ModBlockEntities} only ever hold block-entity types whose
     * value class extends {@link NeroTechMachineBlockEntity}, and the handlers below only ever read
     * from the block entity through that base type.
     */
    @SuppressWarnings("unchecked")
    private static BlockEntityType<NeroTechMachineBlockEntity> machineType(
            BlockEntityType<? extends NeroTechMachineBlockEntity> type) {
        return (BlockEntityType<NeroTechMachineBlockEntity>) type;
    }
}
