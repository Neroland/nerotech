package za.co.neroland.nerotech.machine.heat;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import za.co.neroland.nerotech.client.ProcessingScreen;
import za.co.neroland.nerotech.menu.ProcessingMenu;

/**
 * Heat Exchanger screen (client only): the shared processing screen with the exchanger's own causes on
 * the status line (no tank, no machine, empty tank, nothing moved) and no energy trough, because the
 * exchanger draws no power.
 */
public class HeatExchangerScreen extends ProcessingScreen<ProcessingMenu> {

    public HeatExchangerScreen(ProcessingMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    /** Typed factory for screen registration. */
    public static HeatExchangerScreen create(ProcessingMenu menu, Inventory playerInventory, Component title) {
        return new HeatExchangerScreen(menu, playerInventory, title);
    }

    @Override
    protected boolean drawsEnergyTrough() {
        return false;
    }

    @Override
    protected Component statusLine() {
        int code = this.menu.detail() - HeatExchangerBlockEntity.CODE_NO_TANK;
        if (code >= 0 && code < HeatExchangerBlockEntity.CODE_KEYS.length) {
            return Component.translatable("nerotech.machine.status_line",
                    Component.translatable("nerotech.machine.readout." + readout().name().toLowerCase(java.util.Locale.ROOT)),
                    Component.translatable(HeatExchangerBlockEntity.CODE_KEYS[code]));
        }
        return super.statusLine();
    }
}
