package za.co.neroland.nerotech.machine.extraction;

import java.util.Locale;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import za.co.neroland.nerotech.client.ProcessingScreen;
import za.co.neroland.nerotech.machine.MachineStatus;
import za.co.neroland.nerotech.menu.ProcessingMenu;

/**
 * The Deep Core Drill screen: the generic processing screen, with the drill's own stall causes
 * (unformed, deposits disabled here, no head, depleted, no deposit) on the status line. Client only.
 */
public class DeepCoreDrillScreen extends ProcessingScreen<ProcessingMenu> {

    public DeepCoreDrillScreen(ProcessingMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected Component statusLine() {
        int detail = this.menu.detail();
        int index = detail - DeepCoreDrillBlockEntity.DETAIL_UNFORMED;
        if (index < 0 || index >= DeepCoreDrillBlockEntity.DRILL_DETAIL_KEYS.length
                || this.menu.status() == MachineStatus.RUNNING) {
            return super.statusLine();
        }
        Component state = Component.translatable("nerotech.machine.readout."
                + readout().name().toLowerCase(Locale.ROOT));
        return Component.translatable("nerotech.machine.status_line", state,
                Component.translatable(DeepCoreDrillBlockEntity.DRILL_DETAIL_KEYS[index]));
    }
}
