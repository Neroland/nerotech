package za.co.neroland.nerotech.forge;

import java.util.EnumMap;
import java.util.List;

import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.items.wrapper.SidedInvWrapper;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.energy.NeroEnergyStorage;
import za.co.neroland.nerolandcore.fluid.NeroFluidStorage;
import za.co.neroland.nerolandcore.gas.NeroGases;
import za.co.neroland.nerolandcore.gas.NeroGasStorage;
import za.co.neroland.nerolandcore.platform.ForgeEnergyLookup;
import za.co.neroland.nerolandcore.platform.ForgeFluidHandlers;
import za.co.neroland.nerolandcore.platform.ForgeFluidLookup;
import za.co.neroland.nerolandcore.platform.ForgeGasLookup;

import za.co.neroland.nerotech.NeroTechCommon;
import za.co.neroland.nerotech.machine.NeroTechMachineBlockEntity;

/**
 * Forge capability wiring: attaches every NeroTech machine to (a) Core's shared
 * {@code nerolandcore:energy} capability and (b) the standard {@code ITEM_HANDLER} capability (sided —
 * inputs insertable, outputs extractable), so machines interoperate on one power network and their
 * inventories are the NeroLogistics item-handoff surface. One provider per machine covers every
 * subclass via {@code instanceof}.
 */
public final class ForgeCapabilities {

    private static final Identifier MACHINE_CAPS =
            Identifier.fromNamespaceAndPath(NeroTechCommon.MOD_ID, "machine_caps");

    private ForgeCapabilities() {
    }

    public static void register() {
        AttachCapabilitiesEvent.BlockEntities.BUS.addListener(ForgeCapabilities::onAttachBlockEntity);
    }

    private static void onAttachBlockEntity(AttachCapabilitiesEvent.BlockEntities event) {
        if (event.getObject() instanceof NeroTechMachineBlockEntity machine) {
            MachineProvider provider = new MachineProvider(machine);
            event.addCapability(MACHINE_CAPS, provider);
            event.addListener(provider::invalidate);
        }
    }

    /** A gas view that asks the machine again on every call, so a side-config change is never stale. */
    private record DynamicGas(NeroTechMachineBlockEntity machine, @Nullable Direction side)
            implements NeroGasStorage {

        @Nullable
        private NeroGasStorage current() {
            return this.machine.gasStorage(this.side);
        }

        @Override
        public Identifier getGas() {
            NeroGasStorage view = current();
            return view == null ? NeroGases.EMPTY : view.getGas();
        }

        @Override
        public long getAmount() {
            NeroGasStorage view = current();
            return view == null ? 0L : view.getAmount();
        }

        @Override
        public long getCapacity() {
            NeroGasStorage view = current();
            return view == null ? 0L : view.getCapacity();
        }

        @Override
        public long fill(Identifier gas, long amount, boolean simulate) {
            NeroGasStorage view = current();
            return view == null ? 0L : view.fill(gas, amount, simulate);
        }

        @Override
        public long drain(long amount, boolean simulate) {
            NeroGasStorage view = current();
            return view == null ? 0L : view.drain(amount, simulate);
        }
    }

    /** A fluid view that asks the machine again on every call, for the same reason. */
    private record DynamicFluid(NeroTechMachineBlockEntity machine, @Nullable Direction side)
            implements NeroFluidStorage {

        @Nullable
        private NeroFluidStorage current() {
            return this.machine.fluidStorage(this.side);
        }

        @Override
        public Fluid getFluid() {
            NeroFluidStorage view = current();
            return view == null ? Fluids.EMPTY : view.getFluid();
        }

        @Override
        public long getAmount() {
            NeroFluidStorage view = current();
            return view == null ? 0L : view.getAmount();
        }

        @Override
        public long getCapacity() {
            NeroFluidStorage view = current();
            return view == null ? 0L : view.getCapacity();
        }

        @Override
        public long fill(Fluid fluid, long amount, boolean simulate) {
            NeroFluidStorage view = current();
            return view == null ? 0L : view.fill(fluid, amount, simulate);
        }

        @Override
        public long drain(long amount, boolean simulate) {
            NeroFluidStorage view = current();
            return view == null ? 0L : view.drain(amount, simulate);
        }
    }

    private static final class MachineProvider implements ICapabilityProvider {

        private final NeroTechMachineBlockEntity machine;
        private final LazyOptional<NeroEnergyStorage> energy;
        private final EnumMap<Direction, LazyOptional<NeroEnergyStorage>> sidedEnergy =
                new EnumMap<>(Direction.class);
        private final EnumMap<Direction, LazyOptional<IItemHandler>> sidedItems = new EnumMap<>(Direction.class);
        @Nullable
        private LazyOptional<IItemHandler> unsidedItems;
        // Stage C fluid/gas views (null side = the unsided query).
        private final EnumMap<Direction, LazyOptional<NeroGasStorage>> sidedGas = new EnumMap<>(Direction.class);
        @Nullable
        private LazyOptional<NeroGasStorage> unsidedGas;
        private final EnumMap<Direction, LazyOptional<NeroFluidStorage>> sidedFluid =
                new EnumMap<>(Direction.class);
        @Nullable
        private LazyOptional<NeroFluidStorage> unsidedFluid;
        // The same fluid views on Forge's STANDARD fluid capability, for third-party pipes.
        private final EnumMap<Direction, LazyOptional<IFluidHandler>> sidedStandardFluid =
                new EnumMap<>(Direction.class);
        @Nullable
        private LazyOptional<IFluidHandler> unsidedStandardFluid;

        MachineProvider(NeroTechMachineBlockEntity machine) {
            this.machine = machine;
            this.energy = LazyOptional.of(machine::getEnergy);
        }

        @Override
        public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
            if (cap == ForgeEnergyLookup.ENERGY) {
                return energy(side).cast();
            }
            if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER) {
                return items(side).cast();
            }
            if (cap == ForgeGasLookup.GAS) {
                return gas(side).cast();
            }
            if (cap == ForgeFluidLookup.FLUID) {
                return fluid(side).cast();
            }
            if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER) {
                // Core's lookup alone is Nero-only, which is why the Electrolyzer used to take water
                // from a bucket and nothing else (issue #9).
                return standardFluid(side).cast();
            }
            return LazyOptional.empty();
        }

        /**
         * The machine's per-face gas view, or empty for machines that handle no gas. Resolved on every
         * call rather than cached: a face's mode can change at any time and Forge has no capability
         * invalidation hook here, so a snapshot taken when the face was closed would stay closed until
         * the chunk reloaded.
         */
        private LazyOptional<NeroGasStorage> gas(@Nullable Direction side) {
            if (this.machine.gasStorage(side) == null && this.machine.gasStorage(null) == null) {
                return LazyOptional.empty(); // this machine has no gas at all
            }
            if (side == null) {
                if (this.unsidedGas == null) {
                    this.unsidedGas = LazyOptional.of(() -> new DynamicGas(this.machine, null));
                }
                return this.unsidedGas;
            }
            return this.sidedGas.computeIfAbsent(side, d -> LazyOptional.of(() -> new DynamicGas(this.machine, d)));
        }

        /**
         * The machine's per-face fluid view, or empty for machines that handle no fluid. Resolved on
         * every call, for the same reason as {@link #gas}.
         */
        private LazyOptional<NeroFluidStorage> fluid(@Nullable Direction side) {
            if (this.machine.fluidStorage(side) == null && this.machine.fluidStorage(null) == null) {
                return LazyOptional.empty(); // this machine has no fluid at all
            }
            if (side == null) {
                if (this.unsidedFluid == null) {
                    this.unsidedFluid = LazyOptional.of(() -> new DynamicFluid(this.machine, null));
                }
                return this.unsidedFluid;
            }
            return this.sidedFluid.computeIfAbsent(side, d -> LazyOptional.of(() -> new DynamicFluid(this.machine, d)));
        }

        /**
         * The machine's fluid tank and its gas tanks (as their transport fluids) on Forge's standard
         * {@code IFluidHandler} — one handler per face, with a tank per view when a machine has both.
         * Caching the handler is safe: each view carries live permission suppliers, so a side-config
         * change is honoured without the capability being re-resolved.
         */
        private LazyOptional<IFluidHandler> standardFluid(@Nullable Direction side) {
            if (side == null) {
                if (this.unsidedStandardFluid == null) {
                    this.unsidedStandardFluid = standardFluidFor(null);
                }
                return this.unsidedStandardFluid;
            }
            return this.sidedStandardFluid.computeIfAbsent(side, this::standardFluidFor);
        }

        private LazyOptional<IFluidHandler> standardFluidFor(@Nullable Direction side) {
            if (this.machine.standardFluidViews(side).isEmpty()) {
                return LazyOptional.empty();
            }
            return LazyOptional.of(() -> ForgeFluidHandlers.asFluidHandler(this.machine.standardFluidViews(side)));
        }

        /**
         * Side-config-gated energy view: a face exposes the buffer only when its ENERGY mode permits it;
         * a DISABLED face yields an empty capability. Machines without ENERGY side config fall back to the
         * ungated buffer.
         */
        private LazyOptional<NeroEnergyStorage> energy(@Nullable Direction side) {
            if (this.machine.sideConfig() == null || side == null) {
                return this.energy;
            }
            return this.sidedEnergy.computeIfAbsent(side, d -> {
                NeroEnergyStorage view = this.machine.sideConfig().energyView(d);
                return view == null ? LazyOptional.empty() : LazyOptional.of(() -> view);
            });
        }

        private LazyOptional<IItemHandler> items(@Nullable Direction side) {
            if (side == null) {
                if (this.unsidedItems == null) {
                    this.unsidedItems = LazyOptional.of(() -> new InvWrapper(this.machine));
                }
                return this.unsidedItems;
            }
            return this.sidedItems.computeIfAbsent(side, d -> LazyOptional.of(() -> new SidedInvWrapper(this.machine, d)));
        }

        void invalidate() {
            this.energy.invalidate();
            this.sidedEnergy.values().forEach(LazyOptional::invalidate);
            this.sidedEnergy.clear();
            if (this.unsidedItems != null) {
                this.unsidedItems.invalidate();
            }
            this.sidedItems.values().forEach(LazyOptional::invalidate);
            if (this.unsidedGas != null) {
                this.unsidedGas.invalidate();
                this.unsidedGas = null;
            }
            this.sidedGas.values().forEach(LazyOptional::invalidate);
            this.sidedGas.clear();
            if (this.unsidedFluid != null) {
                this.unsidedFluid.invalidate();
                this.unsidedFluid = null;
            }
            this.sidedFluid.values().forEach(LazyOptional::invalidate);
            this.sidedFluid.clear();
            if (this.unsidedStandardFluid != null) {
                this.unsidedStandardFluid.invalidate();
                this.unsidedStandardFluid = null;
            }
            this.sidedStandardFluid.values().forEach(LazyOptional::invalidate);
            this.sidedStandardFluid.clear();
        }
    }
}
