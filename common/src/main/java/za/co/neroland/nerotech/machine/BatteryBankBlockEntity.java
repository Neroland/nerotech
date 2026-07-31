package za.co.neroland.nerotech.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import za.co.neroland.nerolandcore.sideconfig.Channel;
import za.co.neroland.nerolandcore.sideconfig.SideConfig;
import za.co.neroland.nerolandcore.sideconfig.SidePreset;

import za.co.neroland.nerotech.config.NeroTechConfig;
import za.co.neroland.nerotech.menu.BatteryBankMenu;
import za.co.neroland.nerotech.registry.ModBlockEntities;

/**
 * Battery Bank (Stage D) — the grid buffer. A single block (deliberately <b>not</b> a multiblock:
 * stack or spread them freely, they simply chain) with a {@code batteryBankCapacity} NE buffer, an
 * order of magnitude above a normal machine's. It generates nothing and consumes nothing; it stores
 * the surplus a Solar Array or Wind Turbine makes at noon and hands it back after dark.
 *
 * <p><b>Side posture:</b> Core's {@link SidePreset#STORAGE} — every face I/O, so it both accepts and
 * pushes without configuration, with auto-eject on so it feeds neighbouring machines on its own. A
 * player who wants a strict in-top / out-sides layout reconfigures any face with the Configurator.
 * FE interop is automatic: the bank sits on Core's {@code EnergyLookup} seam like every other
 * machine, so Energized Power (or any FE mod on that surface) charges from it and into it.
 */
public class BatteryBankBlockEntity extends NeroTechMachineBlockEntity {

    public BatteryBankBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BATTERY_BANK.get(), pos, state, 0,
                NeroTechConfig.batteryBankCapacity(), NeroTechConfig.machineMaxTransfer());
        setupSideConfig(SideConfig.builder()
                .channel(Channel.ENERGY)
                .defaultPreset(SidePreset.STORAGE)
                .autoEject(Channel.ENERGY, true)
                .build());
    }

    @Override
    protected void tickMachine(Level level, BlockPos pos, BlockState state) {
        // A buffer does no work: no heat, no pollution, no progress bar. "Active" simply means the
        // bank is holding a charge (the BER lights its cell columns).
        boolean charged = getEnergy().getAmount() > 0;
        setActive(charged);
        if (!charged) {
            reportStatus(MachineStatus.NO_ENERGY);
        }
        MachineEnergy.pushToNeighbours(level, pos, energyBuffer(), NeroTechConfig.machineMaxTransfer(), sideConfig());
    }

    /** Shedding a buffer would achieve nothing — it has no work rate to throttle. */
    @Override
    public boolean shedable() {
        return false;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.nerotech.battery_bank");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new BatteryBankMenu(containerId, playerInventory, this, this.data);
    }
}
