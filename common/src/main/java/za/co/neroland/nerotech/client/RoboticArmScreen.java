package za.co.neroland.nerotech.client;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import za.co.neroland.nerotech.menu.RoboticArmMenu;

/** Concrete screen for the Robotic Arm. */
public class RoboticArmScreen extends MachineScreen<RoboticArmMenu> {

    public RoboticArmScreen(RoboticArmMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }
}
