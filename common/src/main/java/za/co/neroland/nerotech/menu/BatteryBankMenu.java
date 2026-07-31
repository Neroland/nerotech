package za.co.neroland.nerotech.menu;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;

import za.co.neroland.nerotech.machine.NeroTechMachineBlockEntity;
import za.co.neroland.nerotech.registry.ModMenuTypes;

/**
 * Battery Bank menu: no machine slots — the energy gauge <i>is</i> the readout (stored NE as a
 * fraction of {@code batteryBankCapacity}), plus the upgrade column and player inventory.
 */
public class BatteryBankMenu extends MachineMenu {

    private static final int MACHINE_SLOTS = 0;

    public BatteryBankMenu(int id, Inventory playerInventory) {
        this(id, playerInventory,
                new SimpleContainer(MACHINE_SLOTS + NeroTechMachineBlockEntity.UPGRADE_SLOTS),
                new SimpleContainerData(7));
    }

    public BatteryBankMenu(int id, Inventory playerInventory, Container container, ContainerData data) {
        super(ModMenuTypes.BATTERY_BANK.get(), id, container, data, MACHINE_SLOTS);
        addUpgradeAndPlayerSlots(playerInventory);
    }
}
