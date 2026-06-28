package za.co.neroland.nerotech.client;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import za.co.neroland.nerotech.menu.FabricatorMenu;

/** Concrete screen for the Fabricator. */
public class FabricatorScreen extends MachineScreen<FabricatorMenu> {

    public FabricatorScreen(FabricatorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }
}
