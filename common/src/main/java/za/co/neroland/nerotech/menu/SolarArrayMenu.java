package za.co.neroland.nerotech.menu;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;

import za.co.neroland.nerotech.machine.NeroTechMachineBlockEntity;
import za.co.neroland.nerotech.registry.ModMenuTypes;

/** Solar Array menu: no machine slots — upgrade column + player inventory; energy/daylight synced. */
public class SolarArrayMenu extends MachineMenu {

    private static final int MACHINE_SLOTS = 0;

    public SolarArrayMenu(int id, Inventory playerInventory) {
        this(id, playerInventory,
                new SimpleContainer(MACHINE_SLOTS + NeroTechMachineBlockEntity.UPGRADE_SLOTS),
                new SimpleContainerData(4));
    }

    public SolarArrayMenu(int id, Inventory playerInventory, Container container, ContainerData data) {
        super(ModMenuTypes.SOLAR_ARRAY.get(), id, container, data, MACHINE_SLOTS);
        addUpgradeAndPlayerSlots(playerInventory);
    }
}
