package za.co.neroland.nerotech.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.recipe.v1.sync.RecipeSynchronization;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;

import net.minecraft.world.level.block.entity.BlockEntityType;

import za.co.neroland.nerolandcore.platform.FabricEnergyLookup;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import za.co.neroland.nerolandcore.fluid.GatedFluidView;
import za.co.neroland.nerolandcore.platform.FabricFluidHandlers;
import za.co.neroland.nerolandcore.platform.FabricFluidLookup;
import za.co.neroland.nerolandcore.platform.FabricGasLookup;

import za.co.neroland.nerotech.NeroTechCommon;
import za.co.neroland.nerotech.api.MachineTypeRegistry;
import za.co.neroland.nerotech.api.MachineTypeRegistry.Surface;
import za.co.neroland.nerotech.command.NeroTechCommands;
import za.co.neroland.nerotech.machine.NeroTechMachineBlockEntity;
import za.co.neroland.nerotech.pollution.PollutionManager;
import za.co.neroland.nerotech.registry.ModRecipeTypes;
import za.co.neroland.nerotech.telemetry.NeroTechTelemetry;

/**
 * Fabric entry point for NeroTech. Registration is eager; energy capability is wired here.
 *
 * <p>Capability wiring subscribes to the public {@link MachineTypeRegistry} with
 * {@link MachineTypeRegistry#onRegistered} rather than reading a one-off snapshot: Fabric has no
 * "after every initializer" event, and an add-on's initializer (NeroPower's) may run <i>after</i>
 * this one even though it depends on NeroTech. A listener replays every type already registered
 * (NeroTech's own, seeded in {@code NeroTechCommon.init()}) and then wires each late registration
 * the moment it lands, so add-on machines are on the lookups by the time any world loads.
 */
public final class NeroTechFabric implements ModInitializer {

    /**
     * Types already given the standard Fabric fluid storage — a machine on both the gas and the fluid
     * surface arrives through two listeners but must register that one handler once.
     */
    private static final Set<BlockEntityType<? extends NeroTechMachineBlockEntity>> STANDARD_FLUID_WIRED =
            new LinkedHashSet<>();

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
     * Expose every registered machine's energy buffer on Core's shared {@code nerolandcore:energy}
     * lookup, so machines from any Nero mod interoperate on one power network. Listener-based, so an
     * add-on registering after this initializer is wired too (see the class note).
     */
    private static void registerCoreEnergy() {
        MachineTypeRegistry.onRegistered(Surface.ENERGY, type -> energyHandler(machineType(type.get())));
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
        MachineTypeRegistry.onRegistered(Surface.ITEM, type -> itemHandler(machineType(type.get())));
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
        MachineTypeRegistry.onRegistered(Surface.GAS, type -> {
            BlockEntityType<NeroTechMachineBlockEntity> machine = machineType(type.get());
            gasHandler(machine);
            standardFluidOnce(machine);
        });
        MachineTypeRegistry.onRegistered(Surface.FLUID, type -> {
            BlockEntityType<NeroTechMachineBlockEntity> machine = machineType(type.get());
            fluidHandler(machine);
            standardFluidOnce(machine);
        });
    }

    /**
     * ... and the same tanks on FABRIC'S OWN fluid storage, so third-party fluid pipes see them:
     * Core's lookup is Nero-private, which is why the Electrolyzer took water from a bucket and
     * nothing else (issue #9). Gas tanks join in wearing their transport fluid, so a pipe can carry
     * hydrogen and oxygen as well. One registration per type covers both — hence the once-guard, as
     * a machine on both surfaces arrives through both listeners above.
     */
    private static void standardFluidOnce(BlockEntityType<NeroTechMachineBlockEntity> type) {
        if (STANDARD_FLUID_WIRED.add(type)) {
            standardFluidHandler(type);
        }
    }

    private static <T extends NeroTechMachineBlockEntity> void gasHandler(BlockEntityType<T> type) {
        FabricGasLookup.GAS.registerForBlockEntity((be, dir) -> be.gasStorage(dir), type);
    }

    private static <T extends NeroTechMachineBlockEntity> void fluidHandler(BlockEntityType<T> type) {
        FabricFluidLookup.FLUID.registerForBlockEntity((be, dir) -> be.fluidStorage(dir), type);
    }

    /**
     * The machine's fluid tank and its gas tanks (as their transport fluids) on Fabric's standard
     * fluid storage — one storage per face, with a slot per tank when a machine offers both.
     */
    private static <T extends NeroTechMachineBlockEntity> void standardFluidHandler(BlockEntityType<T> type) {
        FluidStorage.SIDED.registerForBlockEntity((be, dir) -> {
            List<GatedFluidView> views = be.standardFluidViews(dir);
            return views.isEmpty() ? null : FabricFluidHandlers.asFluidStorage(views);
        }, type);
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
     * construction: the {@link MachineTypeRegistry} only ever holds block-entity types whose value
     * class extends {@link NeroTechMachineBlockEntity}, and the handlers below only ever read from
     * the block entity through that base type.
     */
    @SuppressWarnings("unchecked")
    private static BlockEntityType<NeroTechMachineBlockEntity> machineType(
            BlockEntityType<? extends NeroTechMachineBlockEntity> type) {
        return (BlockEntityType<NeroTechMachineBlockEntity>) type;
    }
}
