package za.co.neroland.nerotech.menu;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;

import za.co.neroland.nerotech.machine.NeroTechMachineBlockEntity;
import za.co.neroland.nerotech.registry.ModMenuTypes;

/** Item Sorter menu: input + four (filter, buffer) face pairs + upgrade column + player inventory. */
public class ItemSorterMenu extends MachineMenu {

    private static final int MACHINE_SLOTS = 9;

    public ItemSorterMenu(int id, Inventory playerInventory) {
        this(id, playerInventory,
                new SimpleContainer(MACHINE_SLOTS + NeroTechMachineBlockEntity.UPGRADE_SLOTS),
                new SimpleContainerData(6));
    }

    public ItemSorterMenu(int id, Inventory playerInventory, Container container, ContainerData data) {
        super(ModMenuTypes.ITEM_SORTER.get(), id, container, data, MACHINE_SLOTS);
        // Input (left of the filter/buffer columns, clear of the gauges).
        this.addSlot(new PredicateSlot(container, 0, 40, 33));
        // Four filter slots (top row) + their output buffers (row below), one column per horizontal face.
        for (int i = 0; i < 4; i++) {
            this.addSlot(new PredicateSlot(container, 1 + i, 58 + i * 19, 20));
            this.addSlot(new OutputSlot(container, 5 + i, 58 + i * 19, 44));
        }
        addUpgradeAndPlayerSlots(playerInventory);
    }
}
