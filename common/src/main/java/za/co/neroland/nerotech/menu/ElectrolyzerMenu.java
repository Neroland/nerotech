package za.co.neroland.nerotech.menu;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;

import za.co.neroland.nerotech.machine.NeroTechMachineBlockEntity;
import za.co.neroland.nerotech.registry.ModMenuTypes;

/**
 * Electrolyzer menu: no machine I/O slots at all (water in by bucket or fluid capability, gas out by
 * capability) — just the upgrade column, the player inventory, and three extra synced gauges
 * (water / hydrogen / oxygen) after the seven shared ones.
 */
public class ElectrolyzerMenu extends MachineMenu {

    private static final int MACHINE_SLOTS = 0;
    /** 7 shared indices + water/hydrogen/oxygen. */
    private static final int DATA_SLOTS = 10;

    public ElectrolyzerMenu(int id, Inventory playerInventory) {
        this(id, playerInventory,
                new SimpleContainer(MACHINE_SLOTS + NeroTechMachineBlockEntity.UPGRADE_SLOTS),
                new SimpleContainerData(DATA_SLOTS));
    }

    public ElectrolyzerMenu(int id, Inventory playerInventory, Container container, ContainerData data) {
        super(ModMenuTypes.ELECTROLYZER.get(), id, container, data, MACHINE_SLOTS);
        addUpgradeAndPlayerSlots(playerInventory);
    }
}
