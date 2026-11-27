package za.co.neroland.nerotech.neoforge;

import java.util.function.Supplier;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
//? if >=1.20.3 {
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
//?}
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
//? if >=1.20.5 {
import net.neoforged.neoforge.event.tick.ServerTickEvent;
//?}
//? if >=1.21.9 {
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper;
//?} else {
/*import net.neoforged.neoforge.items.wrapper.InvWrapper;
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper;
*///?}

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
        //? if >=1.20.3 {
        modEventBus.addListener(NeroTechNeoForge::onRegisterCapabilities);
        //?} else {
        /*// NeoForge 20.2 predates RegisterCapabilitiesEvent: attach the same surfaces per block entity instead.
        NeoForge.EVENT_BUS.addGenericListener(net.minecraft.world.level.block.entity.BlockEntity.class,
                NeroTechNeoForge::onAttachBlockEntity);
        *///?}
        // Periodic regional pollution decay + retention sweep and quest-hook delivery (game bus; gated inside).
        //? if >=1.20.5 {
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> NeroTechCommon.serverTick(event.getServer()));
        //?} else {
        /*NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.TickEvent.ServerTickEvent event) -> {
            if (event.phase == net.neoforged.neoforge.event.TickEvent.Phase.END) {
                NeroTechCommon.serverTick(event.getServer());
            }
        });
        *///?}
        // Creative-only debug commands (/nerotech gallery); shared brigadier tree in common.
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) ->
                NeroTechCommands.register(event.getDispatcher()));
        // Recipe sync is opt-in on NeoForge (26.x clients hold no full recipe list): ask the server to
        // send NeroTech's machine recipes so recipe viewers — see compat.jei — can list them. Recipe
        // definitions only; no player data crosses the wire.
        // Before 1.21.2 vanilla still syncs every recipe; NeoForge 21.3 has no sendRecipes (and no recipe viewer).
        //? if >=1.21.4 {
        // All sixteen machine recipe types (the five originals plus 0.8.0's eleven process types).
        NeoForge.EVENT_BUS.addListener((OnDatapackSyncEvent event) -> event.sendRecipes(
                ModRecipeTypes.syncedTypes().toArray(new net.minecraft.world.item.crafting.RecipeType<?>[0])));
        //?}
        //? if >=1.21.9 {
        if (FMLEnvironment.getDist() == Dist.CLIENT) {
        //?} else {
        /*if (FMLEnvironment.dist == Dist.CLIENT) {
        *///?}
            NeoForgeClientSetup.init(modEventBus);
        }
    }

    //? if >=1.20.3 {
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
        //? if >=1.21.9 {
        event.registerBlockEntity(Capabilities.Item.BLOCK, type,
                (be, side) -> side != null ? new WorldlyContainerWrapper(be, side) : VanillaContainerWrapper.of(be));
        //?} else {
        /*// Before 1.21.9 NeoForge has no transfer API: the item surface is IItemHandler.
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, type,
                (be, side) -> side != null ? new SidedInvWrapper(be, side) : new InvWrapper(be));
        *///?}
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
        //? if >=1.21.9 {
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, type, (be, side) -> {
        //?} else {
        /*event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, type, (be, side) -> {
        *///?}
            List<GatedFluidView> views = be.standardFluidViews(side);
            return views.isEmpty() ? null : NeoForgeFluidHandlers.asResourceHandler(views);
        });
    }
    //?} else {
    /*private static final net.minecraft.resources.ResourceLocation MACHINE_CAPS =
            new net.minecraft.resources.ResourceLocation(NeroTechCommon.MOD_ID, "machine_caps");

    /^*
     * NeoForge 20.2: the Forge-style capability system. Every machine whose type is in the public
     * {@link MachineTypeRegistry} gets the same surfaces the newer lines register through
     * {@code RegisterCapabilitiesEvent} (Nero energy, items, Nero gas/fluid, standard fluid). The registry
     * is read when each block entity is created, so add-on machine types registered at mod construction
     * are always seen.
     *^/
    private static void onAttachBlockEntity(
            net.neoforged.neoforge.event.AttachCapabilitiesEvent<net.minecraft.world.level.block.entity.BlockEntity> event) {
        if (!(event.getObject() instanceof NeroTechMachineBlockEntity be)) {
            return;
        }
        BlockEntityType<?> type = be.getType();
        boolean energy = contains(MachineTypeRegistry.energyTypes(), type);
        boolean item = contains(MachineTypeRegistry.itemTypes(), type);
        boolean gas = contains(MachineTypeRegistry.gasTypes(), type);
        boolean fluid = contains(MachineTypeRegistry.fluidTypes(), type);
        if (!energy && !item && !gas && !fluid) {
            return;
        }
        MachineCaps caps = new MachineCaps(be, energy, item, gas, fluid);
        event.addCapability(MACHINE_CAPS, caps);
        event.addListener(caps::invalidate);
    }

    private static boolean contains(List<Supplier<BlockEntityType<? extends NeroTechMachineBlockEntity>>> types,
            BlockEntityType<?> type) {
        for (Supplier<BlockEntityType<? extends NeroTechMachineBlockEntity>> candidate : types) {
            if (candidate.get() == type) {
                return true;
            }
        }
        return false;
    }

    private static final class MachineCaps implements net.neoforged.neoforge.common.capabilities.ICapabilityProvider {
        private final NeroTechMachineBlockEntity be;
        private final boolean energy;
        private final boolean item;
        private final boolean gas;
        private final boolean fluid;
        private final List<net.neoforged.neoforge.common.util.LazyOptional<?>> handedOut = new java.util.ArrayList<>();

        MachineCaps(NeroTechMachineBlockEntity be, boolean energy, boolean item, boolean gas, boolean fluid) {
            this.be = be;
            this.energy = energy;
            this.item = item;
            this.gas = gas;
            this.fluid = fluid;
        }

        @Override
        public <T> net.neoforged.neoforge.common.util.LazyOptional<T> getCapability(
                net.neoforged.neoforge.common.capabilities.Capability<T> cap, @org.jetbrains.annotations.Nullable net.minecraft.core.Direction side) {
            Object value = null;
            if (energy && cap == NeoForgeEnergyLookup.ENERGY) {
                value = be.sideConfig() != null ? be.sideConfig().energyView(side) : be.getEnergy();
            } else if (item && cap == net.neoforged.neoforge.common.capabilities.Capabilities.ITEM_HANDLER) {
                value = side != null ? new SidedInvWrapper(be, side) : new InvWrapper(be);
            } else if (gas && cap == NeoForgeGasLookup.GAS) {
                value = be.gasStorage(side);
            } else if (fluid && cap == NeoForgeFluidLookup.FLUID) {
                value = be.fluidStorage(side);
            } else if ((fluid || gas) && cap == net.neoforged.neoforge.common.capabilities.Capabilities.FLUID_HANDLER) {
                List<GatedFluidView> views = be.standardFluidViews(side);
                value = views.isEmpty() ? null : NeoForgeFluidHandlers.asResourceHandler(views);
            }
            if (value == null) {
                return net.neoforged.neoforge.common.util.LazyOptional.empty();
            }
            Object resolved = value;
            net.neoforged.neoforge.common.util.LazyOptional<Object> optional =
                    net.neoforged.neoforge.common.util.LazyOptional.of(() -> resolved);
            handedOut.add(optional);
            return optional.cast();
        }

        void invalidate() {
            handedOut.forEach(net.neoforged.neoforge.common.util.LazyOptional::invalidate);
            handedOut.clear();
        }
    }
    *///?}
}
