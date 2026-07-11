package za.co.neroland.nerotech.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerotech.machine.MachineStatus;
import za.co.neroland.nerotech.menu.AnalyticsTerminalMenu;
import za.co.neroland.nerotech.network.AnalyticsTerminalPayload;
import za.co.neroland.nerotech.network.ClientMenuPos;
import za.co.neroland.nerotech.network.ClientTerminalStats;

/**
 * The Analytics Terminal dashboard — a taller (222px) variant of the procedural
 * {@link MachineScreen} hull panel whose machine area is a scan overview instead of slots and
 * gauges: a header with the roster/active counts and per-status count chips, the hottest-machine
 * line, and up to twelve nearest machines in two columns (coordinates relative to the terminal,
 * status-coloured, with a mini heat bar each). Fed by the container-id-keyed
 * {@link ClientTerminalStats} mailbox; shows a "no data yet" line until the first snapshot lands.
 */
public class AnalyticsTerminalScreen extends AbstractContainerScreen<AnalyticsTerminalMenu> {

    // The MachineScreen hull palette (kept in lockstep; both are procedural fills, no texture).
    private static final int PANEL = 0xFF11161D;
    private static final int PANEL_HI = 0xFF1B232E;
    private static final int EDGE = 0xFF05080D;
    private static final int DIVIDER = 0xFF2A3542;
    private static final int WELL = 0xFF8B8B8B;
    private static final int WELL_EDGE = 0xFF373737;
    private static final int TROUGH = 0xFF0B1119;
    private static final int TITLE = 0xFFD6ECFF;
    private static final int SUBTLE = 0xFF8DA0B4;
    private static final int HEAT = 0xFFE0543A;

    /** Two dashboard row columns of six (payload rows are nearest-first). */
    private static final int ROWS_PER_COLUMN = 6;
    private static final int ROW_STRIDE = 9;

    /** The last dashboard snapshot polled from the mailbox (null until the first push lands). */
    @Nullable
    private AnalyticsTerminalPayload latest;

    public AnalyticsTerminalScreen(AnalyticsTerminalMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 222);
        this.titleLabelX = 8;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    /** The client-side status colours — mirrors {@code AnalyticsWidget#statusColor}. */
    private static int statusColor(MachineStatus status) {
        return switch (status) {
            case RUNNING -> 0xFF3CB043;
            case IDLE -> 0xFF9090A0;
            case STARVED -> 0xFFE0B33A;
            case BLOCKED -> 0xFFE0873A;
            case THROTTLED -> 0xFFE0543A;
            case NO_ENERGY -> 0xFFE8D44D;
            case UNFORMED -> 0xFFB06CE8;
        };
    }

    @Override
    public void extractContents(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        // Drain the mailbox every frame (the AnalyticsWidget poll-and-cache recipe).
        AnalyticsTerminalPayload pending = ClientTerminalStats.poll(this.menu.containerId);
        if (pending != null) {
            this.latest = pending;
        }
        // Resolve the terminal's own position (the MachineScreen recipe) so the hottest-machine
        // line can render terminal-relative coordinates.
        if (this.menu.machinePos() == null) {
            BlockPos resolved = ClientMenuPos.poll(this.menu.containerId);
            if (resolved != null) {
                this.menu.setMachinePos(resolved);
            }
        }

        int x = this.leftPos;
        int y = this.topPos;
        int w = this.imageWidth;
        int h = this.imageHeight;

        // Hull panel with a soft top sheen + a 1px border (the MachineScreen recipe, taller).
        extractor.fill(x - 1, y - 1, x + w + 1, y + h + 1, EDGE);
        extractor.fill(x, y, x + w, y + h, PANEL);
        extractor.fill(x, y, x + w, y + 18, PANEL_HI);
        // Dividers under the title and above the upgrade row / player inventory.
        extractor.fill(x + 7, y + 16, x + w - 7, y + 17, DIVIDER);
        extractor.fill(x + 7, y + 110, x + w - 7, y + 111, DIVIDER);

        // Slot wells (follow the menu's slot positions automatically).
        for (Slot slot : this.menu.slots) {
            int sx = x + slot.x;
            int sy = y + slot.y;
            extractor.fill(sx - 1, sy - 1, sx + 17, sy + 17, WELL_EDGE);
            extractor.fill(sx, sy, sx + 16, sy + 16, WELL);
        }

        dashboard(extractor, x, y);

        super.extractContents(extractor, mouseX, mouseY, partialTick);
    }

    /** The scan overview between the title divider (y17) and the upgrade row (y110). */
    private void dashboard(GuiGraphicsExtractor g, int x, int y) {
        AnalyticsTerminalPayload data = this.latest;
        if (data == null) {
            g.text(this.font, Component.translatable("nerotech.analytics.no_data"), x + 8, y + 22,
                    SUBTLE, false);
            return;
        }

        // Header: roster + active counts, then one count chip per status that has machines.
        g.text(this.font, Component.literal("Machines " + data.machineCount()
                + "  ·  Active " + data.activeCount()), x + 8, y + 20, TITLE, false);
        int chipX = x + 8;
        int[] counts = data.statusCounts();
        for (int i = 0; i < counts.length && i < MachineStatus.VALUES.length; i++) {
            if (counts[i] <= 0) {
                continue;
            }
            int color = statusColor(MachineStatus.byOrdinal(i));
            g.fill(chipX, y + 31, chipX + 6, y + 37, color);
            String count = Integer.toString(counts[i]);
            g.text(this.font, Component.literal(count), chipX + 8, y + 30, SUBTLE, false);
            chipX += 12 + this.font.width(count);
        }

        // Aggregate regional pollution across the scanned machines' distinct regions (deduped
        // server-side), right-aligned on the chip row with the AnalyticsWidget severity ramp.
        if (data.machineCount() > 0) {
            Component pollution = Component.translatable("nerotech.analytics.terminal_pollution",
                    AnalyticsWidget.levelText(data.regionPollution(), data.pollutionThreshold()));
            g.text(this.font, pollution, x + this.imageWidth - 8 - this.font.width(pollution),
                    y + 30, AnalyticsWidget.pollutionColor(data.regionPollution(),
                            data.pollutionThreshold()), false);
        }

        // Hottest machine (terminal-relative coordinates, heat as a percentage).
        BlockPos hottest = data.hottestPos();
        BlockPos origin = this.menu.machinePos();
        if (hottest != null && origin != null) {
            g.text(this.font, Component.literal("Hottest " + offset(hottest.getX() - origin.getX())
                    + " " + offset(hottest.getY() - origin.getY())
                    + " " + offset(hottest.getZ() - origin.getZ())
                    + "  ·  " + data.hottestPermille() / 10 + "%"), x + 8, y + 41, HEAT, false);
        } else if (hottest != null) {
            // Terminal position not resolved yet — show the heat alone rather than wrong coords.
            g.text(this.font, Component.literal("Hottest  ·  " + data.hottestPermille() / 10 + "%"),
                    x + 8, y + 41, HEAT, false);
        }

        // Rows: nearest machines, two columns of six, coordinates status-coloured + mini heat bar.
        for (int i = 0; i < data.rows().size() && i < 2 * ROWS_PER_COLUMN; i++) {
            AnalyticsTerminalPayload.Row row = data.rows().get(i);
            int colX = x + 8 + (i / ROWS_PER_COLUMN) * 82;
            int rowY = y + 53 + (i % ROWS_PER_COLUMN) * ROW_STRIDE;
            g.text(this.font, Component.literal(offset(row.dx()) + " " + offset(row.dy())
                    + " " + offset(row.dz())), colX, rowY, statusColor(MachineStatus.byOrdinal(row.status())), false);
            // Mini heat bar (12px) after the coordinates.
            int barX = colX + 62;
            g.fill(barX, rowY + 2, barX + 12, rowY + 6, TROUGH);
            int fill = Math.max(0, Math.min(12, row.heatPermille() * 12 / 1000));
            if (fill > 0) {
                g.fill(barX, rowY + 2, barX + fill, rowY + 6, HEAT);
            }
        }
    }

    /** Signed offset formatting: always show the sign so rows read as terminal-relative. */
    private static String offset(int value) {
        return value >= 0 ? "+" + value : Integer.toString(value);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor extractor, int mouseX, int mouseY) {
        extractor.text(this.font, this.title, this.titleLabelX, this.titleLabelY, TITLE, false);
        extractor.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY,
                SUBTLE, false);
    }
}
