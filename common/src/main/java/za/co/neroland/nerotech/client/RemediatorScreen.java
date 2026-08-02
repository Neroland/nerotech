package za.co.neroland.nerotech.client;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import za.co.neroland.nerotech.menu.RemediatorMenu;

/** Concrete screen for the Remediator. */
public class RemediatorScreen extends MachineScreen<RemediatorMenu> {

    public RemediatorScreen(RemediatorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }
}
