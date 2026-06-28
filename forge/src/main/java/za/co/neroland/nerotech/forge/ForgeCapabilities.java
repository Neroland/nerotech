package za.co.neroland.nerotech.forge;

import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.energy.NeroEnergyStorage;
import za.co.neroland.nerolandcore.platform.ForgeEnergyLookup;

import za.co.neroland.nerotech.NeroTechCommon;
import za.co.neroland.nerotech.machine.NeroTechMachineBlockEntity;

/**
 * Forge capability wiring: attaches every NeroTech machine's energy buffer to Core's shared
 * {@code nerolandcore:energy} capability ({@link ForgeEnergyLookup#ENERGY}), so machines from any Nero
 * mod interoperate on one power network.
 */
public final class ForgeCapabilities {

    private static final Identifier ENERGY_CAP =
            Identifier.fromNamespaceAndPath(NeroTechCommon.MOD_ID, "energy_cap");

    private ForgeCapabilities() {
    }

    public static void register() {
        AttachCapabilitiesEvent.BlockEntities.BUS.addListener(ForgeCapabilities::onAttachBlockEntity);
    }

    private static void onAttachBlockEntity(AttachCapabilitiesEvent.BlockEntities event) {
        if (event.getObject() instanceof NeroTechMachineBlockEntity machine) {
            EnergyProvider provider = new EnergyProvider(machine);
            event.addCapability(ENERGY_CAP, provider);
            event.addListener(provider::invalidate);
        }
    }

    private static final class EnergyProvider implements ICapabilityProvider {

        private final LazyOptional<NeroEnergyStorage> energy;

        EnergyProvider(NeroTechMachineBlockEntity machine) {
            this.energy = LazyOptional.of(machine::getEnergy);
        }

        @Override
        public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
            return cap == ForgeEnergyLookup.ENERGY ? this.energy.cast() : LazyOptional.empty();
        }

        void invalidate() {
            this.energy.invalidate();
        }
    }
}
