package za.co.neroland.nerotech.menu;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;

import za.co.neroland.nerotech.machine.NeroTechMachineBlockEntity;
import za.co.neroland.nerotech.machine.RoboticArmBlockEntity;
import za.co.neroland.nerotech.registry.ModMenuTypes;

/**
 * Robotic Arm menu: the single filter slot + upgrade column + player inventory. The filter is a
 * template, never stock — the arm never consumes it and never exposes it to automation.
 */
public class RoboticArmMenu extends MachineMenu {

    private static final int MACHINE_SLOTS = 1;

    public RoboticArmMenu(int id, Inventory playerInventory) {
        this(id, playerInventory,
                new SimpleContainer(MACHINE_SLOTS + NeroTechMachineBlockEntity.UPGRADE_SLOTS),
                new SimpleContainerData(7));
    }

    public RoboticArmMenu(int id, Inventory playerInventory, Container container, ContainerData data) {
        super(ModMenuTypes.ROBOTIC_ARM.get(), id, container, data, MACHINE_SLOTS);
        this.addSlot(new PredicateSlot(container, RoboticArmBlockEntity.FILTER_SLOT, 80, 33));
        addUpgradeAndPlayerSlots(playerInventory);
    }
}
