package za.co.neroland.nerotech.machine.foundry;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import za.co.neroland.nerotech.client.ProcessingScreen;
import za.co.neroland.nerotech.menu.ProcessingMenu;

/**
 * Blast Foundry screen (client only): the shared processing screen plus the foundry's own "not formed"
 * cause on the status line. The heat gauge and tooltip already show the furnace heat the ports deliver.
 */
public class BlastFoundryScreen extends ProcessingScreen<ProcessingMenu> {

    public BlastFoundryScreen(ProcessingMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    /** Typed factory for screen registration. */
    public static BlastFoundryScreen create(ProcessingMenu menu, Inventory playerInventory, Component title) {
        return new BlastFoundryScreen(menu, playerInventory, title);
    }

    @Override
    protected Component statusLine() {
        if (this.menu.detail() == BlastFoundryBlockEntity.DETAIL_UNFORMED) {
            return Component.translatable("nerotech.machine.status_line",
                    Component.translatable("nerotech.machine.readout.stall"),
                    Component.translatable(BlastFoundryBlockEntity.DETAIL_UNFORMED_KEY));
        }
        if (this.menu.detail() == BlastFoundryBlockEntity.DETAIL_FOUNDRY_COLD) {
            return Component.translatable("nerotech.machine.status_line",
                    Component.translatable("nerotech.machine.readout.stall"),
                    Component.translatable(BlastFoundryBlockEntity.DETAIL_FOUNDRY_COLD_KEY, this.menu.detailParam()));
        }
        if (this.menu.detail() == BlastFoundryBlockEntity.DETAIL_SHORT_BATCH) {
            return Component.translatable("nerotech.machine.status_line",
                    Component.translatable("nerotech.machine.readout.stall"),
                    Component.translatable(BlastFoundryBlockEntity.DETAIL_SHORT_BATCH_KEY));
        }
        return super.statusLine();
    }
}
