package za.co.neroland.nerotech.client;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import za.co.neroland.nerotech.menu.GasTurbineMenu;

/** Concrete screen for the Gas Turbine — one fuel-gas gauge beside energy/heat. */
public class GasTurbineScreen extends TankMachineScreen<GasTurbineMenu> {

    public GasTurbineScreen(GasTurbineMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, HYDROGEN);
    }

    /** The turbine burns any listed fuel gas, so the gauge names the role rather than one gas. */
    @Override
    protected Component tankName(int i) {
        return Component.translatable("nerotech.gui.gauge.fuel_gas");
    }
}
