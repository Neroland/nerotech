package za.co.neroland.nerotech.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import za.co.neroland.nerotech.menu.MachineMenu;

/**
 * One procedural screen for every NeroTech machine menu — a dark sci-fi hull panel drawn entirely with
 * {@code fill}s (no GUI texture asset). Consistent layout for every machine: energy + heat gauges on the
 * left (each with an always-visible colour cap so it reads even when empty), machine I/O slots centred,
 * upgrade-module slots as a 2×2 block top-right, and a work-progress bar along the bottom of the machine
 * area. 26.x renders container screens via {@code extract*(GuiGraphicsExtractor, ...)}.
 *
 * @param <T> the machine menu type
 */
public class MachineScreen<T extends MachineMenu> extends AbstractContainerScreen<T> {

    private static final int PANEL = 0xFF11161D;
    private static final int PANEL_HI = 0xFF1B232E;   // top sheen
    private static final int EDGE = 0xFF05080D;
    private static final int DIVIDER = 0xFF2A3542;
    private static final int WELL = 0xFF8B8B8B;
    private static final int WELL_EDGE = 0xFF373737;
    private static final int TROUGH = 0xFF0B1119;
    private static final int ENERGY = 0xFFE0B33A;     // amber
    private static final int WORK = 0xFF55C2F0;       // cyan
    private static final int HEAT = 0xFFE0543A;       // red
    private static final int TITLE = 0xFFD6ECFF;
    private static final int SUBTLE = 0xFF8DA0B4;

    public MachineScreen(T menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 166);
        this.titleLabelX = 8;
        this.inventoryLabelX = 8;
    }

    /** Typed factory for screen registration ({@code MachineScreen::create}). */
    public static <M extends MachineMenu> MachineScreen<M> create(M menu, Inventory playerInventory,
            Component title) {
        return new MachineScreen<>(menu, playerInventory, title);
    }

    @Override
    public void extractContents(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        int x = this.leftPos;
        int y = this.topPos;
        int w = this.imageWidth;
        int h = this.imageHeight;

        // Hull panel with a soft top sheen + a 1px border.
        extractor.fill(x - 1, y - 1, x + w + 1, y + h + 1, EDGE);
        extractor.fill(x, y, x + w, y + h, PANEL);
        extractor.fill(x, y, x + w, y + 18, PANEL_HI);
        // Dividers under the title and above the player inventory.
        extractor.fill(x + 7, y + 16, x + w - 7, y + 17, DIVIDER);
        extractor.fill(x + 7, y + 70, x + w - 7, y + 71, DIVIDER);

        // Slot wells (follow the menu's slot positions automatically).
        for (Slot slot : this.menu.slots) {
            int sx = x + slot.x;
            int sy = y + slot.y;
            extractor.fill(sx - 1, sy - 1, sx + 17, sy + 17, WELL_EDGE);
            extractor.fill(sx, sy, sx + 16, sy + 16, WELL);
        }

        // Energy + heat gauges (left), each with an always-on colour cap.
        gauge(extractor, x + 8, y + 20, 10, 46, this.menu.energyFraction(), ENERGY);
        gauge(extractor, x + 20, y + 20, 10, 46, this.menu.heatFraction(), HEAT);

        // Work-progress bar along the bottom of the machine area, lit while working.
        if (this.menu.working()) {
            float f = Math.max(0f, Math.min(1f, this.menu.workFraction()));
            int bx = x + 40;
            int bw = 96;
            extractor.fill(bx - 1, y + 61, bx + bw + 1, y + 68, EDGE);
            extractor.fill(bx, y + 62, bx + bw, y + 67, TROUGH);
            int fw = Math.round(bw * f);
            if (fw > 0) {
                extractor.fill(bx, y + 62, bx + fw, y + 67, WORK);
            }
        }

        super.extractContents(extractor, mouseX, mouseY, partialTick);
    }

    private static void gauge(GuiGraphicsExtractor g, int x, int y, int w, int h, float frac, int fill) {
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, EDGE);
        g.fill(x, y, x + w, y + h, TROUGH);
        float f = Math.max(0f, Math.min(1f, frac));
        int fh = Math.round(h * f);
        if (fh > 0) {
            g.fill(x, y + h - fh, x + w, y + h, fill);   // fills upward
        }
        g.fill(x, y, x + w, y + 2, fill);                // always-on cap so the gauge is identifiable
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor extractor, int mouseX, int mouseY) {
        extractor.text(this.font, this.title, this.titleLabelX, this.titleLabelY, TITLE, false);
        extractor.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY,
                SUBTLE, false);
    }
}
