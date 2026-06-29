package za.co.neroland.nerotech.client;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import za.co.neroland.nerotech.menu.OreProcessorMenu;

/** Concrete screen for the Ore Processor. */
public class OreProcessorScreen extends MachineScreen<OreProcessorMenu> {

    public OreProcessorScreen(OreProcessorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }
}
