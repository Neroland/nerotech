package za.co.neroland.nerotech.machine.chemistry;

import java.util.Locale;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import za.co.neroland.nerotech.client.ProcessingScreen;
import za.co.neroland.nerotech.menu.ProcessingMenu;

/**
 * Client only. The generic processing screen plus the chemistry family's own stall cause: the
 * Atmospheric Condenser's "no atmosphere" (detail code
 * {@link AtmosphericCondenserBlockEntity#DETAIL_NO_ATMOSPHERE}).
 */
public class ChemistryScreen extends ProcessingScreen<ProcessingMenu> {

    public ChemistryScreen(ProcessingMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    public static ChemistryScreen create(ProcessingMenu menu, Inventory playerInventory, Component title) {
        return new ChemistryScreen(menu, playerInventory, title);
    }

    @Override
    protected Component statusLine() {
        if (this.menu.detail() == AtmosphericCondenserBlockEntity.DETAIL_NO_ATMOSPHERE) {
            Component state = Component.translatable(
                    "nerotech.machine.readout." + readout().name().toLowerCase(Locale.ROOT));
            return Component.translatable("nerotech.machine.status_line", state,
                    Component.translatable(AtmosphericCondenserBlockEntity.NO_ATMOSPHERE_KEY));
        }
        return super.statusLine();
    }
}
