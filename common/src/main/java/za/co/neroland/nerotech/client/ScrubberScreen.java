package za.co.neroland.nerotech.client;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import za.co.neroland.nerotech.menu.ScrubberMenu;

/** Concrete screen for the Scrubber. */
public class ScrubberScreen extends MachineScreen<ScrubberMenu> {

    public ScrubberScreen(ScrubberMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }
}
