package za.co.neroland.nerotech.client;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import za.co.neroland.nerotech.menu.ElectrolyzerMenu;

/** Concrete screen for the Electrolyzer — water, hydrogen and oxygen gauges beside energy/heat. */
public class ElectrolyzerScreen extends TankMachineScreen<ElectrolyzerMenu> {

    public ElectrolyzerScreen(ElectrolyzerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, WATER, HYDROGEN, OXYGEN);
    }
}
