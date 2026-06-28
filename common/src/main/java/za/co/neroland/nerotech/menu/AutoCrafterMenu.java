package za.co.neroland.nerotech.menu;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;

import za.co.neroland.nerotech.machine.NeroTechMachineBlockEntity;
import za.co.neroland.nerotech.registry.ModMenuTypes;

/** Auto Crafter menu: 3×3 grid + output + upgrade column + player inventory. */
public class AutoCrafterMenu extends MachineMenu {

    private static final int MACHINE_SLOTS = 10;

    public AutoCrafterMenu(int id, Inventory playerInventory) {
        this(id, playerInventory,
                new SimpleContainer(MACHINE_SLOTS + NeroTechMachineBlockEntity.UPGRADE_SLOTS),
                new SimpleContainerData(6));
    }

    public AutoCrafterMenu(int id, Inventory playerInventory, Container container, ContainerData data) {
        super(ModMenuTypes.AUTO_CRAFTER.get(), id, container, data, MACHINE_SLOTS);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                this.addSlot(new PredicateSlot(container, row * 3 + col, 40 + col * 18, 17 + row * 18));
            }
        }
        this.addSlot(new OutputSlot(container, 9, 112, 35));
        addUpgradeAndPlayerSlots(playerInventory);
    }
}
