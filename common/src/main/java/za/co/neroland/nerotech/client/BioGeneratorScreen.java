package za.co.neroland.nerotech.client;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import za.co.neroland.nerotech.menu.BioGeneratorMenu;

/** Concrete screen for the Bio Generator. */
public class BioGeneratorScreen extends MachineScreen<BioGeneratorMenu> {

    public BioGeneratorScreen(BioGeneratorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }
}
