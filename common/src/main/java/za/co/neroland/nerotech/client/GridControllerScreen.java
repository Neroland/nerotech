package za.co.neroland.nerotech.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import za.co.neroland.nerotech.menu.GridControllerMenu;

/**
 * The Grid Controller screen: the shared {@link MachineScreen} hull plus a three-line status block
 * in the machine area — machines watched, aggregate grid fill, and whether load shedding is in
 * force. All three come straight from the menu's synced ints, so the panel never guesses.
 */
public class GridControllerScreen extends MachineScreen<GridControllerMenu> {

    /** Nominal (green) / shedding (amber) — the AnalyticsWidget status ramp's two relevant stops. */
    private static final int NOMINAL = 0xFF3CB043;
    private static final int SHEDDING = 0xFFE0B33A;
    private static final int LABEL = 0xFFD6ECFF;

    /** Status block origin, clear of the gauge column (x+8..30) and the upgrade block (x+138). */
    private static final int TEXT_X = 40;
    private static final int TEXT_Y = 22;
    private static final int LINE_STRIDE = 11;

    public GridControllerScreen(GridControllerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    public void extractContents(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        super.extractContents(extractor, mouseX, mouseY, partialTick);

        int x = this.leftPos + TEXT_X;
        int y = this.topPos + TEXT_Y;
        boolean shedding = this.menu.shedding();

        extractor.text(this.font, Component.translatable("gui.nerotech.grid_controller.machines",
                this.menu.machinesSeen()), x, y, LABEL, false);
        extractor.text(this.font, Component.translatable("gui.nerotech.grid_controller.fill",
                        this.menu.gridFillPermille() / 10), x, y + LINE_STRIDE,
                shedding ? SHEDDING : NOMINAL, false);
        extractor.text(this.font, Component.translatable(shedding
                        ? "gui.nerotech.grid_controller.shedding"
                        : "gui.nerotech.grid_controller.nominal"), x, y + 2 * LINE_STRIDE,
                shedding ? SHEDDING : NOMINAL, false);
    }
}
