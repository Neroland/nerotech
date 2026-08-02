package za.co.neroland.nerotech.client;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import za.co.neroland.nerotech.menu.BatteryBankMenu;

/** Concrete screen for the Battery Bank — the shared energy gauge is the whole readout. */
public class BatteryBankScreen extends MachineScreen<BatteryBankMenu> {

    public BatteryBankScreen(BatteryBankMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }
}
