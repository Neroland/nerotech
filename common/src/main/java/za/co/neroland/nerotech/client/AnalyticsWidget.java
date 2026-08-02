package za.co.neroland.nerotech.client;

import java.util.function.IntSupplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerotech.machine.MachinePreset;
import za.co.neroland.nerotech.machine.MachineStatus;
import za.co.neroland.nerotech.network.ClientMachineStats;
import za.co.neroland.nerotech.network.MachineStatsPayload;

/**
 * The Stage G Analytics tab: a collapsible panel beside every machine GUI (Core's
 * {@code SideConfigWidget} composition — the screen forwards {@code render} and
 * {@code mouseClicked}) showing ONE status line naming the machine's current limiting cause,
 * the current heat/energy percentages and ops rate, three 60-second sparklines (heat, stored
 * energy, regional pollution) drawn as 1px column fills, plus two labelled info clusters:
 * <b>Efficiency</b> (effective speed/energy multipliers from upgrades × preset, and the
 * ambient-vs-heat thermal context with a headroom-coloured heat line) and <b>Pollution</b>
 * (the region's level against the event threshold on a teal→amber→red severity ramp, and this
 * machine's signed nominal rate per minute). All values arrive server-computed in the
 * container-id-keyed {@link ClientMachineStats} mailbox (the server streams while the menu is
 * open); degrades to a "no data yet" line until the first snapshot lands. Pure client visuals —
 * no player data.
 */
public final class AnalyticsWidget {

    private static final int CELL = 16;
    private static final int HEADER_H = 14;
    private static final int PANEL_W = 142;
    private static final int PANEL_H = 174;

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
    /** Pollution severity ramp: teal (fine) → amber ({@link #ENERGY}) → crit red ({@link #HEAT}). */
    private static final int POLLUTION_OK = 0xFF3AC2A5;
    /** Faint threshold tick drawn across the pollution sparkline (premultiplied dim red). */
    private static final int THRESHOLD_TICK = 0x66E0543A;

    // Section layout (y offsets below the panel header; the sparkline column stays right-aligned).
    private static final int STATUS_Y = 4;
    private static final int NOW_Y = 15;
    private static final int HEAT_PLOT_Y = 28;
    private static final int ENERGY_PLOT_Y = HEAT_PLOT_Y + PLOT_H + 12;
    private static final int EFFICIENCY_HEADER_Y = ENERGY_PLOT_Y + PLOT_H + 6;
    private static final int MULTIPLIERS_Y = EFFICIENCY_HEADER_Y + 11;
    private static final int THERMAL_Y = MULTIPLIERS_Y + 11;
    private static final int POLLUTION_HEADER_Y = THERMAL_Y + 13;
    private static final int REGION_Y = POLLUTION_HEADER_Y + 11;
    private static final int POLLUTION_PLOT_Y = REGION_Y + 13;

    private final int containerId;
    private final int anchorX;
    private final int anchorY;

    /**
     * The menu's synced Stage H preset ordinal (ContainerData index 6) — shown in the header line
     * so a glance at the analytics panel names the active trade-off. Live supplier, not a snapshot.
     */
    private final IntSupplier presetOrdinal;

    private boolean open;

    /** The last snapshot polled from the mailbox (kept between pushes; null until the first). */
    @Nullable
    private MachineStatsPayload latest;

    public AnalyticsWidget(int containerId, int anchorX, int anchorY, IntSupplier presetOrdinal) {
        this.containerId = containerId;
        this.anchorX = anchorX;
        this.anchorY = anchorY;
        this.presetOrdinal = presetOrdinal;
    }

    public boolean isOpen() {
        return this.open;
    }

    /** Preset header colours: Eco teal / Balanced white / Overdrive amber (the MachineScreen set). */
    private static int presetColor(MachinePreset preset) {
        return switch (preset) {
            case ECO -> 0xFF4DD0E1;
            case BALANCED -> 0xFFE6E6F0;
            case OVERDRIVE -> 0xFFE0B33A;
        };
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

    /** Severity ramp for a pollution level against the event threshold (threshold off = teal). */
    static int pollutionColor(int level, int threshold) {
        if (threshold <= 0 || level < threshold / 2) {
            return POLLUTION_OK;
        }
        return level >= threshold ? HEAT : ENERGY;
    }

    /** Heat headroom colour: amber within 15% of the throttle threshold, crit red past it. */
    private static int heatHeadroomColor(int heatRaw, int throttle) {
        if (throttle <= 0 || heatRaw < throttle * 85 / 100) {
            return SUBTLE;
        }
        return heatRaw >= throttle ? HEAT : ENERGY;
    }

    /** "412 / 1000" while the event threshold is on, the plain value when it is 0/off. */
    static String levelText(int level, int threshold) {
        return threshold > 0 ? level + " / " + threshold : Integer.toString(level);
    }

    /** Signed per-minute rate text: "+3", "-9" or "0" (the sign IS the information). */
    private static String rateText(int perMinute) {
        return perMinute > 0 ? "+" + perMinute : Integer.toString(perMinute);
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

        // Panel + header (with the active Stage H preset name right-aligned, in its colour).
        g.fill(px, py, px + PANEL_W, py + PANEL_H, PANEL);
        outline(g, px, py, PANEL_W, PANEL_H, OUTLINE);
        g.fill(px, py, px + PANEL_W, py + HEADER_H, HEADER);
        g.text(font, Component.literal("Analytics"), px + 5, py + 3, TEXT, false);
        MachinePreset preset = MachinePreset.byOrdinal(this.presetOrdinal.getAsInt());
        Component presetName = Component.translatable(preset.translationKey());
        g.text(font, presetName, px + PANEL_W - 5 - font.width(presetName), py + 3,
                presetColor(preset), false);

        MachineStatsPayload stats = this.latest;
        if (stats == null) {
            g.text(font, Component.translatable("nerotech.analytics.no_data"), px + 5,
                    py + HEADER_H + 6, SUBTLE, false);
            return;
        }

        int top = py + HEADER_H;

        // The status line: one word naming the current limiting cause, colour-coded.
        MachineStatus status = MachineStatus.byOrdinal(stats.status());
        g.text(font, Component.translatable(status.translationKey()), px + 5, top + STATUS_Y,
                statusColor(status), false);

        // Current values: heat / energy percentages + the net ops rate.
        String now = "Heat " + stats.heatPermille() / 10 + "%  ·  NE " + stats.energyPermille() / 10
                + "%  ·  " + stats.opsRate() + " op/s";
        g.text(font, Component.literal(now), px + 5, top + NOW_Y, SUBTLE, false);

        // The three 60-second sparklines (1px column fills; partial history draws right-aligned
        // so "now" is always the rightmost column).
        int plotX = px + PANEL_W - PLOT_W - 6;
        sparkline(g, font, Component.literal("Heat"), plotX, top + HEAT_PLOT_Y, stats.heatHistory(), HEAT);
        sparkline(g, font, Component.literal("Energy"), plotX, top + ENERGY_PLOT_Y, stats.energyHistory(), ENERGY);

        // EFFICIENCY cluster: upgrades × preset as percentages, then ambient-vs-heat context with
        // the headroom hint (amber within 15% of the throttle threshold, crit red past it).
        section(g, font, px, top + EFFICIENCY_HEADER_Y,
                Component.translatable("nerotech.analytics.efficiency"));
        g.text(font, Component.translatable("nerotech.analytics.multipliers",
                stats.effSpeedPct(), stats.effEnergyPct()), px + 5, top + MULTIPLIERS_Y, SUBTLE, false);
        g.text(font, Component.translatable("nerotech.analytics.thermal", stats.ambientHeat(),
                stats.heatRaw(), stats.heatCapacity()), px + 5, top + THERMAL_Y,
                heatHeadroomColor(stats.heatRaw(), stats.heatThrottle()), false);

        // POLLUTION cluster: the region's level on the severity ramp, this machine's signed
        // nominal rate right-aligned (teal when it removes), and the regional 60s sparkline.
        section(g, font, px, top + POLLUTION_HEADER_Y,
                Component.translatable("nerotech.analytics.pollution"));
        int severity = pollutionColor(stats.regionPollution(), stats.pollutionThreshold());
        g.text(font, Component.translatable("nerotech.analytics.region_level",
                levelText(stats.regionPollution(), stats.pollutionThreshold())),
                px + 5, top + REGION_Y, severity, false);
        Component rate = Component.translatable("nerotech.analytics.rate",
                rateText(stats.pollutionPerMinute()));
        g.text(font, rate, px + PANEL_W - 5 - font.width(rate), top + REGION_Y,
                stats.pollutionPerMinute() < 0 ? POLLUTION_OK : SUBTLE, false);
        pollutionSparkline(g, font, Component.translatable("nerotech.analytics.region"), plotX,
                top + POLLUTION_PLOT_Y, stats.pollutionHistory(), stats.pollutionThreshold(), severity);
    }

    /** A small section header: label + a subtle rule out to the panel edge. */
    private static void section(GuiGraphicsExtractor g, Font font, int px, int y, Component label) {
        g.text(font, label, px + 5, y, TEXT, false);
        int lineX = px + 5 + font.width(label) + 4;
        g.fill(lineX, y + 3, px + PANEL_W - 5, y + 4, HEADER);
    }

    /** One labelled 60x16 sparkline: permille samples as 1px columns filled up from the base. */
    private static void sparkline(GuiGraphicsExtractor g, Font font, Component label, int x, int y,
            short[] samples, int color) {
        plotFrame(g, font, label, x, y);
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

    /**
     * The regional-pollution sparkline: samples are RAW units (not permille), so the plot
     * normalises against the larger of the window max and the event threshold — the shape stays
     * readable whether the region idles at 12 or spikes past 1000 — and marks the threshold as a
     * faint tick line when it falls inside the plot.
     */
    private static void pollutionSparkline(GuiGraphicsExtractor g, Font font, Component label,
            int x, int y, short[] samples, int threshold, int color) {
        plotFrame(g, font, label, x, y);
        int scale = Math.max(1, threshold);
        for (int i = 0; i < samples.length; i++) {
            scale = Math.max(scale, samples[i]);
        }
        int base = y + PLOT_H;
        if (threshold > 0 && threshold < scale) {
            int ty = base - Math.min(PLOT_H - 1, threshold * PLOT_H / scale);
            g.fill(x, ty, x + PLOT_W, ty + 1, THRESHOLD_TICK);
        }
        int start = PLOT_W - samples.length;
        for (int i = 0; i < samples.length && i < PLOT_W; i++) {
            int h = Math.max(0, Math.min(PLOT_H, samples[i] * PLOT_H / scale));
            if (h > 0) {
                int cx = x + start + i;
                g.fill(cx, base - h, cx + 1, base, color);
            }
        }
    }

    /** Shared sparkline chrome: the left label, the 1px border and the trough. */
    private static void plotFrame(GuiGraphicsExtractor g, Font font, Component label, int x, int y) {
        g.text(font, label, x - font.width(label) - 5, y + PLOT_H - 8, SUBTLE, false);
        g.fill(x - 1, y - 1, x + PLOT_W + 1, y + PLOT_H + 1, BORDER);
        g.fill(x, y, x + PLOT_W, y + PLOT_H, TROUGH);
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
