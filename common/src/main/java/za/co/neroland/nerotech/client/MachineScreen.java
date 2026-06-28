package za.co.neroland.nerotech.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import za.co.neroland.nerotech.menu.MachineMenu;

/**
 * One procedural screen for every NeroTech machine menu — draws a dark sci-fi hull panel, slot wells,
 * an energy gauge and a work-progress bar entirely with {@code fill}s (no GUI texture asset needed).
 * 26.x renders container screens via {@code extract*(GuiGraphicsExtractor, ...)}.
 *
 * @param <T> the machine menu type
 */
public class MachineScreen<T extends MachineMenu> extends AbstractContainerScreen<T> {

    private static final int PANEL = 0xFF11161D;
    private static final int PANEL_EDGE = 0xFF05080D;
    private static final int WELL = 0xFF8B8B8B;
    private static final int WELL_EDGE = 0xFF373737;
    private static final int TROUGH = 0xFF0B1119;
    private static final int ENERGY = 0xFFE0B33A;   // amber
    private static final int WORK = 0xFF55C2F0;      // cyan
    private static final int HEAT = 0xFFE0543A;      // red
    private static final int TITLE = 0xFFD6ECFF;
    private static final int SUBTLE = 0xFF8DA0B4;

    public MachineScreen(T menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 166);
        this.titleLabelX = 8;
        this.inventoryLabelX = 8;
    }

    /** Typed factory for screen registration ({@code MachineScreen::create}) — fixes generic inference. */
    public static <M extends MachineMenu> MachineScreen<M> create(M menu, Inventory playerInventory,
            Component title) {
        return new MachineScreen<>(menu, playerInventory, title);
    }

    @Override
    public void extractContents(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        int x = this.leftPos;
        int y = this.topPos;
        // Hull panel.
        extractor.fill(x - 1, y - 1, x + this.imageWidth + 1, y + this.imageHeight + 1, PANEL_EDGE);
        extractor.fill(x, y, x + this.imageWidth, y + this.imageHeight, PANEL);
        // Slot wells.
        for (Slot slot : this.menu.slots) {
            int sx = x + slot.x;
            int sy = y + slot.y;
            extractor.fill(sx - 1, sy - 1, sx + 17, sy + 17, WELL_EDGE);
            extractor.fill(sx, sy, sx + 16, sy + 16, WELL);
        }
        // Energy gauge (vertical, left).
        gauge(extractor, x + 8, y + 18, 8, 48, this.menu.energyFraction(), ENERGY, true);
        // Heat gauge (vertical, right) — the Stage-3 consequence axis.
        gauge(extractor, x + this.imageWidth - 16, y + 18, 8, 48, this.menu.heatFraction(), HEAT, true);
        // Work-progress bar (horizontal, centre) — lit while the machine is working.
        if (this.menu.working()) {
            gauge(extractor, x + 78, y + 34, 22, 8, this.menu.workFraction(), WORK, false);
        }
        super.extractContents(extractor, mouseX, mouseY, partialTick);
    }

    private static void gauge(GuiGraphicsExtractor g, int x, int y, int w, int h, float frac, int fill,
            boolean vertical) {
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, PANEL_EDGE);
        g.fill(x, y, x + w, y + h, TROUGH);
        float f = Math.max(0f, Math.min(1f, frac));
        if (vertical) {
            int fh = Math.round(h * f);
            if (fh > 0) {
                g.fill(x, y + h - fh, x + w, y + h, fill);
            }
        } else {
            int fw = Math.round(w * f);
            if (fw > 0) {
                g.fill(x, y, x + fw, y + h, fill);
            }
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor extractor, int mouseX, int mouseY) {
        extractor.text(this.font, this.title, this.titleLabelX, this.titleLabelY, TITLE, false);
        extractor.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY,
                SUBTLE, false);
    }
}
