package za.co.neroland.nerotech.neoforge;

import java.util.function.Supplier;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper;

import net.minecraft.world.level.block.entity.BlockEntityType;

import za.co.neroland.nerolandcore.platform.NeoForgeEnergyLookup;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import za.co.neroland.nerolandcore.fluid.GatedFluidView;
import za.co.neroland.nerolandcore.platform.NeoForgeFluidHandlers;
import za.co.neroland.nerolandcore.platform.NeoForgeFluidLookup;
import za.co.neroland.nerolandcore.platform.NeoForgeGasLookup;

import za.co.neroland.nerotech.NeroTechCommon;
import za.co.neroland.nerotech.api.MachineTypeRegistry;
import za.co.neroland.nerotech.command.NeroTechCommands;
import za.co.neroland.nerotech.machine.NeroTechMachineBlockEntity;
import za.co.neroland.nerotech.pollution.PollutionManager;
import za.co.neroland.nerotech.registry.ModRecipeTypes;
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
        // The FluidTypes behind NeroTech's gas fluids — a NeoForge-only registry, so its register is
        // created here, after common init and before the registers are flushed to the bus.
        NeoForgeFluidTypes.init();
        // Anonymous, NeroTech-only crash reporting (opt-out via config/nerotech.properties; off in dev unless DSN set).
        NeroTechTelemetry.init();
        NeoForgeRegistrationFactory.registerAll(modEventBus);
        // NeroTech's own payloads (menu → machine position sync); see network.NeroTechNetwork.
        NeoForgeNetwork.register(modEventBus);
        modEventBus.addListener(NeroTechNeoForge::onRegisterCapabilities);
        // Periodic regional pollution decay + retention sweep (game bus; gated by interval inside tick).
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> PollutionManager.tick(event.getServer()));
        // Creative-only debug commands (/nerotech gallery); shared brigadier tree in common.
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) ->
                NeroTechCommands.register(event.getDispatcher()));
        // Recipe sync is opt-in on NeoForge (26.x clients hold no full recipe list): ask the server to
        // send NeroTech's machine recipes so recipe viewers — see compat.jei — can list them. Recipe
        // definitions only; no player data crosses the wire.
        NeoForge.EVENT_BUS.addListener((OnDatapackSyncEvent event) -> event.sendRecipes(
                ModRecipeTypes.ORE_PROCESSING.get(),
                ModRecipeTypes.FABRICATING.get(),
                ModRecipeTypes.ADVANCED_FABRICATING.get(),
                ModRecipeTypes.COLLIDER.get(),
                ModRecipeTypes.CHEMICAL_PROCESSING.get()));
        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            NeoForgeClientSetup.init(modEventBus);
        }
    }

    /**
     * Expose every registered machine's surfaces on the loader capabilities, reading the public
     * {@link MachineTypeRegistry} (NeroTech's own machines seeded first in {@code NeroTechCommon.init()},
     * then any add-on's). {@code RegisterCapabilitiesEvent} fires on the mod bus after <b>every</b> mod's
     * constructor has run, so an add-on (NeroPower) that registers its machine types from its own mod
     * constructor — it depends on NeroTech, so ours runs first — is always in the snapshot read here.
     * A registration made later than mod construction (e.g. from a setup event) is too late for
     * NeoForge and must be avoided by add-ons.
     */
    private static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        for (Supplier<BlockEntityType<? extends NeroTechMachineBlockEntity>> type
                : MachineTypeRegistry.energyTypes()) {
            energyCap(event, machineType(type.get()));
        }

        // Item handoff surface (Stage 5): expose every machine's sided inventory on the standard item
        // capability so NeroLogistics / pipes / hoppers move items in and out with no NeroTech dependency.
        for (Supplier<BlockEntityType<? extends NeroTechMachineBlockEntity>> type
                : MachineTypeRegistry.itemTypes()) {
            itemCap(event, machineType(type.get()));
        }

        // Stage C: the fluid/gas machines' tanks on Core's shared fluid/gas capabilities, so the gas
        // chain interoperates with Core's Fluid/Gas Tanks (and any other mod on those surfaces).
        for (Supplier<BlockEntityType<? extends NeroTechMachineBlockEntity>> type
                : MachineTypeRegistry.gasTypes()) {
            gasCap(event, machineType(type.get()));
        }
        for (Supplier<BlockEntityType<? extends NeroTechMachineBlockEntity>> type
                : MachineTypeRegistry.fluidTypes()) {
            fluidCap(event, machineType(type.get()));
        }

        // ... and the same tanks on NEOFORGE'S OWN fluid capability, so third-party fluid pipes see
        // them: Core's lookup is Nero-private, which is why the Electrolyzer took water from a bucket
        // and nothing else (issue #9). Gas tanks join in wearing their transport fluid, so a pipe can
        // carry hydrogen and oxygen as well. One registration per type covers both.
        Set<BlockEntityType<? extends NeroTechMachineBlockEntity>> standardFluid = new LinkedHashSet<>();
        MachineTypeRegistry.fluidTypes().forEach(type -> standardFluid.add(type.get()));
        MachineTypeRegistry.gasTypes().forEach(type -> standardFluid.add(type.get()));
        for (BlockEntityType<? extends NeroTechMachineBlockEntity> type : standardFluid) {
            standardFluidCap(event, machineType(type));
        }
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

    private static <T extends NeroTechMachineBlockEntity> void gasCap(RegisterCapabilitiesEvent event,
            BlockEntityType<T> type) {
        event.registerBlockEntity(NeoForgeGasLookup.GAS, type, (be, side) -> be.gasStorage(side));
    }

    private static <T extends NeroTechMachineBlockEntity> void fluidCap(RegisterCapabilitiesEvent event,
            BlockEntityType<T> type) {
        event.registerBlockEntity(NeoForgeFluidLookup.FLUID, type, (be, side) -> be.fluidStorage(side));
    }

    /**
     * The machine's fluid tank and its gas tanks (as their transport fluids) on NeoForge's standard
     * fluid capability — one handler per face, with a tank per index when a machine offers both.
     */
    private static <T extends NeroTechMachineBlockEntity> void standardFluidCap(RegisterCapabilitiesEvent event,
            BlockEntityType<T> type) {
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, type, (be, side) -> {
            List<GatedFluidView> views = be.standardFluidViews(side);
            return views.isEmpty() ? null : NeoForgeFluidHandlers.asResourceHandler(views);
        });
    }
}
