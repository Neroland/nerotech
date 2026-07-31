package za.co.neroland.nerotech.menu;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;

import za.co.neroland.nerotech.machine.NeroTechMachineBlockEntity;
import za.co.neroland.nerotech.registry.ModMenuTypes;

/**
 * Gas Turbine menu: slotless (its fuel is a gas) — the upgrade column, the player inventory, and one
 * extra synced gauge for the fuel tank.
 */
public class GasTurbineMenu extends MachineMenu {

    private static final int MACHINE_SLOTS = 0;
    /** 7 shared indices + the fuel-gas level. */
    private static final int DATA_SLOTS = 8;

    public GasTurbineMenu(int id, Inventory playerInventory) {
        this(id, playerInventory,
                new SimpleContainer(MACHINE_SLOTS + NeroTechMachineBlockEntity.UPGRADE_SLOTS),
                new SimpleContainerData(DATA_SLOTS));
    }

    public GasTurbineMenu(int id, Inventory playerInventory, Container container, ContainerData data) {
        super(ModMenuTypes.GAS_TURBINE.get(), id, container, data, MACHINE_SLOTS);
        addUpgradeAndPlayerSlots(playerInventory);
    }
}
