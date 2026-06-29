package za.co.neroland.nerotech.menu;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;

import za.co.neroland.nerotech.machine.NeroTechMachineBlockEntity;
import za.co.neroland.nerotech.registry.ModMenuTypes;

/** Ore Processor menu: input + output + upgrade column + player inventory; energy/progress synced. */
public class OreProcessorMenu extends MachineMenu {

    private static final int MACHINE_SLOTS = 2;

    public OreProcessorMenu(int id, Inventory playerInventory) {
        this(id, playerInventory,
                new SimpleContainer(MACHINE_SLOTS + NeroTechMachineBlockEntity.UPGRADE_SLOTS),
                new SimpleContainerData(6));
    }

    public OreProcessorMenu(int id, Inventory playerInventory, Container container, ContainerData data) {
        super(ModMenuTypes.ORE_PROCESSOR.get(), id, container, data, MACHINE_SLOTS);
        this.addSlot(new PredicateSlot(container, 0, 56, 33));
        this.addSlot(new OutputSlot(container, 1, 104, 33));
        addUpgradeAndPlayerSlots(playerInventory);
    }
}
