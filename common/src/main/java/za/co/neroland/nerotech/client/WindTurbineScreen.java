package za.co.neroland.nerotech.client;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import za.co.neroland.nerotech.menu.WindTurbineMenu;

/** Concrete screen for the Wind Turbine. */
public class WindTurbineScreen extends MachineScreen<WindTurbineMenu> {

    public WindTurbineScreen(WindTurbineMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }
}
