package za.co.neroland.nerotech.client;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import za.co.neroland.nerotech.menu.GeothermalGeneratorMenu;

/** Concrete screen for the Geothermal Generator. */
public class GeothermalGeneratorScreen extends MachineScreen<GeothermalGeneratorMenu> {

    public GeothermalGeneratorScreen(GeothermalGeneratorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }
}
