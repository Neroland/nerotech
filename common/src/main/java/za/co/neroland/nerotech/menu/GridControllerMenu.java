package za.co.neroland.nerotech.menu;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;

import za.co.neroland.nerotech.machine.NeroTechMachineBlockEntity;
import za.co.neroland.nerotech.registry.ModMenuTypes;

/**
 * Grid Controller menu: no machine slots — upgrade column + player inventory, plus three
 * machine-specific synced ints after the seven shared ones (machines seen, aggregate grid fill in
 * permille, shedding flag), read by {@code client.GridControllerScreen}.
 */
public class GridControllerMenu extends MachineMenu {

    private static final int MACHINE_SLOTS = 0;

    /** Seven shared ContainerData indices + the controller's three status ints. */
    private static final int DATA_SIZE = 7 + 3;

    public GridControllerMenu(int id, Inventory playerInventory) {
        this(id, playerInventory,
                new SimpleContainer(MACHINE_SLOTS + NeroTechMachineBlockEntity.UPGRADE_SLOTS),
                new SimpleContainerData(DATA_SIZE));
    }

    public GridControllerMenu(int id, Inventory playerInventory, Container container, ContainerData data) {
        super(ModMenuTypes.GRID_CONTROLLER.get(), id, container, data, MACHINE_SLOTS);
        addUpgradeAndPlayerSlots(playerInventory);
    }

    /** Machines the last scan found in range. */
    public int machinesSeen() {
        return extraValue(0);
    }

    /** Aggregate stored NE across those machines, permille of their combined capacity. */
    public int gridFillPermille() {
        return extraValue(1);
    }

    /** Whether the controller is currently shedding load. */
    public boolean shedding() {
        return extraValue(2) > 0;
    }
}
