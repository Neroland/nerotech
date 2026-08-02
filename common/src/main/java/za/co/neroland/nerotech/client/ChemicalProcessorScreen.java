package za.co.neroland.nerotech.client;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import za.co.neroland.nerotech.menu.ChemicalProcessorMenu;

/** Concrete screen for the Chemical Processor — one oxygen gauge beside energy/heat. */
public class ChemicalProcessorScreen extends TankMachineScreen<ChemicalProcessorMenu> {

    public ChemicalProcessorScreen(ChemicalProcessorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, OXYGEN);
    }
}
