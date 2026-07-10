package za.co.neroland.nerotech.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerotech.machine.MachineStatus;
import za.co.neroland.nerotech.network.ClientMachineStats;
import za.co.neroland.nerotech.network.MachineStatsPayload;

/**
 * The Stage G Analytics tab: a collapsible panel beside every machine GUI (Core's
 * {@code SideConfigWidget} composition — the screen forwards {@code render} and
 * {@code mouseClicked}) showing ONE status line naming the machine's current limiting cause,
 * the current heat/energy percentages and ops rate, and two 60-second sparklines (heat, stored
 * energy) drawn as 1px column fills. Fed by the container-id-keyed
 * {@link ClientMachineStats} mailbox (the server streams while the menu is open); degrades to a
 * "no data yet" line until the first snapshot lands. Pure client visuals — no player data.
 */
public final class AnalyticsWidget {

    private static final int CELL = 16;
    private static final int HEADER_H = 14;
    private static final int PANEL_W = 142;
    private static final int PANEL_H = 94;

    /** Sparkline plot geometry: one pixel column per one-second sample. */
    private static final int PLOT_W = 60;
    private static final int PLOT_H = 16;

    private static final int PANEL = 0xF018181E;
    private static final int HEADER = 0xFF2C2C36;
    private static final int BORDER = 0xFF000000;
    private static final int OUTLINE = 0xFF54545E;
    private static final int TEXT = 0xFFE6E6F0;
    private static final int SUBTLE = 0xFF9090A0;
    private static final int TAB_CLOSED = 0xFF3AA7C2;
    private static final int TROUGH = 0xFF0B1119;
    private static final int HEAT = 0xFFE0543A;
    private static final int ENERGY = 0xFFE0B33A;

    private final int containerId;
    private final int anchorX;
    private final int anchorY;

    private boolean open;

    /** The last snapshot polled from the mailbox (kept between pushes; null until the first). */
    @Nullable
    private MachineStatsPayload latest;

    public AnalyticsWidget(int containerId, int anchorX, int anchorY) {
        this.containerId = containerId;
        this.anchorX = anchorX;
        this.anchorY = anchorY;
    }

    public boolean isOpen() {
        return this.open;
    }

    /** The client-side colour hint per status (kept out of the common enum by design). */
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

    // --- rendering ----------------------------------------------------------

    public void render(GuiGraphicsExtractor g, int guiLeft, int guiTop, int mouseX, int mouseY) {
        // Drain the mailbox every frame so a snapshot never goes stale across menu reopens.
        MachineStatsPayload pending = ClientMachineStats.poll(this.containerId);
        if (pending != null) {
            this.latest = pending;
        }

        Font font = Minecraft.getInstance().font;
        int px = guiLeft + this.anchorX;
        int py = guiTop + this.anchorY;

        if (!this.open) {
            g.fill(px, py, px + CELL, py + CELL, TAB_CLOSED);
            String tab = "Ana";
            g.text(font, Component.literal(tab), px + CELL / 2 - font.width(tab) / 2, py + 4,
                    0xFF0A0A0A, false);
            return;
        }

        // Panel + header.
        g.fill(px, py, px + PANEL_W, py + PANEL_H, PANEL);
        outline(g, px, py, PANEL_W, PANEL_H, OUTLINE);
        g.fill(px, py, px + PANEL_W, py + HEADER_H, HEADER);
        g.text(font, Component.literal("Analytics"), px + 5, py + 3, TEXT, false);

        MachineStatsPayload stats = this.latest;
        if (stats == null) {
            g.text(font, Component.translatable("nerotech.analytics.no_data"), px + 5,
                    py + HEADER_H + 6, SUBTLE, false);
            return;
        }

        // The status line: one word naming the current limiting cause, colour-coded.
        MachineStatus status = MachineStatus.byOrdinal(stats.status());
        g.text(font, Component.translatable(status.translationKey()), px + 5, py + HEADER_H + 4,
                statusColor(status), false);

        // Current values: heat / energy percentages + the net ops rate.
        String now = "Heat " + stats.heatPermille() / 10 + "%  ·  NE " + stats.energyPermille() / 10
                + "%  ·  " + stats.opsRate() + " op/s";
        g.text(font, Component.literal(now), px + 5, py + HEADER_H + 15, SUBTLE, false);

        // The two 60-second sparklines (1px column fills; partial history draws right-aligned
        // so "now" is always the rightmost column).
        int plotX = px + PANEL_W - PLOT_W - 6;
        sparkline(g, font, "Heat", plotX, py + HEADER_H + 28, stats.heatHistory(), HEAT);
        sparkline(g, font, "Energy", plotX, py + HEADER_H + 28 + PLOT_H + 12, stats.energyHistory(), ENERGY);
    }

    /** One labelled 60x16 sparkline: permille samples as 1px columns filled up from the base. */
    private static void sparkline(GuiGraphicsExtractor g, Font font, String label, int x, int y,
            short[] samples, int color) {
        g.text(font, Component.literal(label), x - font.width(label) - 5, y + PLOT_H - 8, SUBTLE, false);
        g.fill(x - 1, y - 1, x + PLOT_W + 1, y + PLOT_H + 1, BORDER);
        g.fill(x, y, x + PLOT_W, y + PLOT_H, TROUGH);
        int base = y + PLOT_H;
        int start = PLOT_W - samples.length; // right-align a not-yet-full window
        for (int i = 0; i < samples.length && i < PLOT_W; i++) {
            int h = Math.max(0, Math.min(PLOT_H, samples[i] * PLOT_H / 1000));
            if (h > 0) {
                int cx = x + start + i;
                g.fill(cx, base - h, cx + 1, base, color);
            }
        }
    }

    // --- input --------------------------------------------------------------

    public boolean mouseClicked(double mx, double my, int button, int guiLeft, int guiTop) {
        int px = guiLeft + this.anchorX;
        int py = guiTop + this.anchorY;
        if (in(mx, my, px, py, this.open ? PANEL_W : CELL, HEADER_H)) {
            this.open = !this.open;
            return true;
        }
        if (!this.open) {
            return false;
        }
        // Swallow clicks inside the open panel so they don't reach the slots behind it.
        return in(mx, my, px, py, PANEL_W, PANEL_H);
    }

    // --- helpers ------------------------------------------------------------

    private static boolean in(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private static void outline(GuiGraphicsExtractor g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y, x + 1, y + h, color);
        g.fill(x + w - 1, y, x + w, y + h, color);
    }
}
