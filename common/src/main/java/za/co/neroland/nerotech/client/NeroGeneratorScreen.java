package za.co.neroland.nerotech.client;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import za.co.neroland.nerotech.menu.NeroGeneratorMenu;

/** Concrete screen for the Nero Generator (fixes the menu type so screen registration infers cleanly). */
public class NeroGeneratorScreen extends MachineScreen<NeroGeneratorMenu> {

    public NeroGeneratorScreen(NeroGeneratorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }
}
