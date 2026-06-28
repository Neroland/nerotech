package za.co.neroland.nerotech.client;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import za.co.neroland.nerotech.menu.AutoCrafterMenu;

/** Concrete screen for the Auto Crafter. */
public class AutoCrafterScreen extends MachineScreen<AutoCrafterMenu> {

    public AutoCrafterScreen(AutoCrafterMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }
}
