package za.co.neroland.nerotech.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import za.co.neroland.nerotech.menu.AutoCrafterMenu;

/**
 * Auto Crafter screen. On top of the shared machine panel it renders: the server-synced recipe
 * <b>preview</b> as a ghost in the output well while the output is empty, the <b>lock template</b>
 * ghosts in any locked-but-empty grid slot, a teal tint on locked grid wells, and the Lock/Unlock
 * toggle (routed through {@code handleInventoryButtonClick} — no custom packet).
 */
public class AutoCrafterScreen extends MachineScreen<AutoCrafterMenu> {

    private static final int GHOST_TINT = 0x998B8B8B;   // translucent well grey over ghost icons
    private static final int LOCK_TINT = 0x332A8B8B;    // teal wash over locked grid wells
    private static final int BUTTON_BG = 0xFF1B232E;
    private static final int BUTTON_EDGE = 0xFF2A3542;
    private static final int BUTTON_TEXT = 0xFFD6ECFF;

    private static final int GRID_X = 40;
    private static final int GRID_Y = 17;
    private static final int OUTPUT_X = 112;
    private static final int OUTPUT_Y = 35;
    private static final int BUTTON_X = 104;
    private static final int BUTTON_Y = 3;
    private static final int BUTTON_W = 64;
    private static final int BUTTON_H = 12;

    public AutoCrafterScreen(AutoCrafterMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    public void extractContents(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        super.extractContents(extractor, mouseX, mouseY, partialTick);
        int x = this.leftPos;
        int y = this.topPos;
        boolean locked = this.menu.locked();

        // Teal wash over the grid wells while locked, and template ghosts in the empty ones.
        for (int i = 0; i < 9; i++) {
            int sx = x + GRID_X + (i % 3) * 18;
            int sy = y + GRID_Y + (i / 3) * 18;
            if (locked) {
                extractor.fill(sx, sy, sx + 16, sy + 16, LOCK_TINT);
            }
            ItemStack template = this.menu.template(i);
            if (!template.isEmpty() && gridSlot(i).getItem().isEmpty()) {
                ghost(extractor, template, sx, sy);
            }
        }

        // Recipe preview ghost in the output well while it's empty.
        ItemStack preview = this.menu.preview();
        if (!preview.isEmpty() && this.menu.slots.get(9).getItem().isEmpty()) {
            ghost(extractor, preview, x + OUTPUT_X, y + OUTPUT_Y);
        }

        // Lock/Unlock toggle in the title band.
        int bx = x + BUTTON_X;
        int by = y + BUTTON_Y;
        extractor.fill(bx - 1, by - 1, bx + BUTTON_W + 1, by + BUTTON_H + 1, BUTTON_EDGE);
        extractor.fill(bx, by, bx + BUTTON_W, by + BUTTON_H, BUTTON_BG);
        Component label = Component.translatable(locked
                ? "gui.nerotech.auto_crafter.unlock"
                : "gui.nerotech.auto_crafter.lock");
        int tw = this.font.width(label);
        extractor.text(this.font, label, bx + (BUTTON_W - tw) / 2, by + 2, BUTTON_TEXT, false);
    }

    /** Menu slot for grid index 0..8 (grid slots are added first, so menu index == grid index). */
    private Slot gridSlot(int index) {
        return this.menu.slots.get(index);
    }

    /** Draw a ghost: the item icon washed out by a translucent well-coloured overlay. */
    private static void ghost(GuiGraphicsExtractor extractor, ItemStack stack, int x, int y) {
        extractor.item(stack, x, y);
        extractor.fill(x, y, x + 16, y + 16, GHOST_TINT);
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent mouseButtonEvent, boolean doubleClick) {
        double mx = mouseButtonEvent.x();
        double my = mouseButtonEvent.y();
        int bx = this.leftPos + BUTTON_X;
        int by = this.topPos + BUTTON_Y;
        if (mx >= bx && mx < bx + BUTTON_W && my >= by && my < by + BUTTON_H) {
            if (this.minecraft != null && this.minecraft.gameMode != null) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId,
                        AutoCrafterMenu.BUTTON_TOGGLE_LOCK);
            }
            return true;
        }
        return super.mouseClicked(mouseButtonEvent, doubleClick);
    }
}
