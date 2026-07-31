package za.co.neroland.nerotech.client;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import za.co.neroland.nerotech.menu.ColliderMenu;

/** Concrete screen for the Collider Core. */
public class ColliderScreen extends MachineScreen<ColliderMenu> {

    public ColliderScreen(ColliderMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }
}
