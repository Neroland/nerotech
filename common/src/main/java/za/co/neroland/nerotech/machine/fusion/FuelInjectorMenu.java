package za.co.neroland.nerotech.machine.fusion;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;

import za.co.neroland.nerotech.machine.NeroTechMachineBlockEntity;
import za.co.neroland.nerotech.menu.MachineMenu;

/**
 * Fuel Injector menu: slotless (its fuel is gas), the upgrade column, the player inventory, and two
 * extra synced gauges — deuterium and helium-3.
 */
public class FuelInjectorMenu extends MachineMenu {

    private static final int MACHINE_SLOTS = 0;
    /** 7 shared indices + the injector's extras (gauges, amounts, charge flag, capacity). */
    private static final int DATA_SLOTS = 7 + FuelInjectorBlockEntity.EXTRA_COUNT;

    public FuelInjectorMenu(int id, Inventory playerInventory) {
        this(id, playerInventory,
                new SimpleContainer(MACHINE_SLOTS + NeroTechMachineBlockEntity.UPGRADE_SLOTS),
                new SimpleContainerData(DATA_SLOTS));
    }

    public FuelInjectorMenu(int id, Inventory playerInventory, Container container, ContainerData data) {
        super(FusionContent.FUEL_INJECTOR_MENU.get(), id, container, data, MACHINE_SLOTS);
        addUpgradeAndPlayerSlots(playerInventory);
    }
}
