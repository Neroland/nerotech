package za.co.neroland.nerotech.client;

import java.util.Locale;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import za.co.neroland.nerotech.machine.ColliderStatus;
import za.co.neroland.nerotech.menu.ColliderMenu;

/**
 * Accelerator Controller screen. The shared machine panel plus the four readouts the accelerator
 * actually runs on: beam speed, the collision energy that speed carries, the shape of the traced
 * beam line (guide count + whether it closes into a loop), and the beam status line.
 *
 * <p>All four come from the menu's synced gauges — the client never traces anything itself.
 */
public class ColliderScreen extends MachineScreen<ColliderMenu> {

    private static final int SPEED_COLOR = 0xFF55C2F0;   // cyan, matching the work bar
    private static final int ENERGY_COLOR = 0xFFE0B33A;  // amber, matching the energy gauge
    private static final int LOOP_OK = 0xFF7FD98A;
    private static final int LOOP_BAD = 0xFFE0543A;
    private static final int STATUS_COLOR = 0xFF8DA0B4;

    public ColliderScreen(ColliderMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor extractor, int mouseX, int mouseY) {
        super.extractLabels(extractor, mouseX, mouseY);

        extractor.text(this.font, Component.translatable("gui.nerotech.collider.speed",
                        String.format(Locale.ROOT, "%.0f", this.menu.beamSpeed())),
                40, 20, SPEED_COLOR, false);
        extractor.text(this.font, Component.translatable("gui.nerotech.collider.energy",
                        Integer.toString(this.menu.collisionEnergy())),
                96, 20, ENERGY_COLOR, false);

        boolean closed = this.menu.loopClosed();
        extractor.text(this.font, Component.translatable(
                        closed ? "gui.nerotech.collider.path.closed" : "gui.nerotech.collider.path.open",
                        Integer.toString(this.menu.guideCount())),
                40, 52, closed ? LOOP_OK : LOOP_BAD, false);

        ColliderStatus status = this.menu.beamStatus();
        extractor.text(this.font, Component.translatable(status.translationKey()),
                40, 61, STATUS_COLOR, false);
    }
}
