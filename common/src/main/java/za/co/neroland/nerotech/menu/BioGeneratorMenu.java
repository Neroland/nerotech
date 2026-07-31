package za.co.neroland.nerotech.menu;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;

import za.co.neroland.nerotech.machine.NeroTechMachineBlockEntity;
import za.co.neroland.nerotech.registry.ModMenuTypes;

/** Bio Generator menu: one feedstock slot + upgrade column + player inventory; energy/burn synced. */
public class BioGeneratorMenu extends MachineMenu {

    private static final int MACHINE_SLOTS = 1;

    public BioGeneratorMenu(int id, Inventory playerInventory) {
        this(id, playerInventory,
                new SimpleContainer(MACHINE_SLOTS + NeroTechMachineBlockEntity.UPGRADE_SLOTS),
                new SimpleContainerData(7));
    }

    public BioGeneratorMenu(int id, Inventory playerInventory, Container container, ContainerData data) {
        super(ModMenuTypes.BIO_GENERATOR.get(), id, container, data, MACHINE_SLOTS);
        this.addSlot(new PredicateSlot(container, 0, 80, 33));
        addUpgradeAndPlayerSlots(playerInventory);
    }
}
