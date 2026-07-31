package za.co.neroland.nerotech.forge;

import java.util.EnumMap;

import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.items.wrapper.SidedInvWrapper;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.energy.NeroEnergyStorage;
import za.co.neroland.nerolandcore.fluid.NeroFluidStorage;
import za.co.neroland.nerolandcore.gas.NeroGasStorage;
import za.co.neroland.nerolandcore.platform.ForgeEnergyLookup;
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
            return LazyOptional.empty();
        }

        /** Stage C: the machine's per-face gas view, or empty for machines that handle no gas. */
        private LazyOptional<NeroGasStorage> gas(@Nullable Direction side) {
            if (side == null) {
                if (this.unsidedGas == null) {
                    NeroGasStorage view = this.machine.gasStorage(null);
                    this.unsidedGas = view == null ? LazyOptional.empty() : LazyOptional.of(() -> view);
                }
                return this.unsidedGas;
            }
            return this.sidedGas.computeIfAbsent(side, d -> {
                NeroGasStorage view = this.machine.gasStorage(d);
                return view == null ? LazyOptional.empty() : LazyOptional.of(() -> view);
            });
        }

        /** Stage C: the machine's per-face fluid view, or empty for machines that handle no fluid. */
        private LazyOptional<NeroFluidStorage> fluid(@Nullable Direction side) {
            if (side == null) {
                if (this.unsidedFluid == null) {
                    NeroFluidStorage view = this.machine.fluidStorage(null);
                    this.unsidedFluid = view == null ? LazyOptional.empty() : LazyOptional.of(() -> view);
                }
                return this.unsidedFluid;
            }
            return this.sidedFluid.computeIfAbsent(side, d -> {
                NeroFluidStorage view = this.machine.fluidStorage(d);
                return view == null ? LazyOptional.empty() : LazyOptional.of(() -> view);
            });
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
            }
            this.sidedGas.values().forEach(LazyOptional::invalidate);
            this.sidedGas.clear();
            if (this.unsidedFluid != null) {
                this.unsidedFluid.invalidate();
            }
            this.sidedFluid.values().forEach(LazyOptional::invalidate);
            this.sidedFluid.clear();
        }
    }
}
