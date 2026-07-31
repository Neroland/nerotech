package za.co.neroland.nerotech.client;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import za.co.neroland.nerotech.menu.WirelessNodeMenu;

/** Concrete screen for the Wireless Power Node. */
public class WirelessNodeScreen extends MachineScreen<WirelessNodeMenu> {

    public WirelessNodeScreen(WirelessNodeMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }
}
