package za.co.neroland.nerotech.client;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import za.co.neroland.nerotech.menu.SolarArrayMenu;

/** Concrete screen for the Solar Array. */
public class SolarArrayScreen extends MachineScreen<SolarArrayMenu> {

    public SolarArrayScreen(SolarArrayMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }
}
