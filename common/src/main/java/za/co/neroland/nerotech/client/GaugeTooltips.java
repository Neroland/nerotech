package za.co.neroland.nerotech.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerotech.gas.NeroTechGases;
import za.co.neroland.nerotech.machine.MachineStatus;

/**
 * The shared gauge tooltips for every NeroTech machine screen (art direction §9): each gauge names
 * itself, gives amount / capacity with units, and its rate. Builders append lines to a list; the
 * screen positions and emits them through {@link #show}. Numbers come from {@link GaugeMath}; every
 * word is an English source lang key. Machine-scoped numbers only — no player data.
 */
public final class GaugeTooltips {

    /** Heat change per second treated as steady (sync jitter, ambient drift). */
    public static final double HEAT_DEADBAND_PER_SECOND = 0.5D;
    /** Tank change per second treated as steady. */
    public static final double TANK_DEADBAND_PER_SECOND = 0.5D;

    private GaugeTooltips() {
    }

    private static Component tr(String key, Object... args) {
        return Component.translatable("nerotech.gui.gauge." + key, args);
    }

    // --- energy -----------------------------------------------------------------------------------

    /**
     * Energy: name, stored / capacity in NE, and the rate. With a known draw ({@code usedPerTick >= 0})
     * the net change splits into "in" and "used"; otherwise the net change alone. {@code deliveredPerTick}
     * (a charger's output) adds its own line when non-negative.
     */
    public static void energy(List<Component> out, long stored, long capacity, double netPerTick,
            long usedPerTick, long deliveredPerTick) {
        out.add(tr("energy"));
        if (capacity <= 0) {
            out.add(tr("energy.no_buffer"));
            return;
        }
        out.add(tr("energy.amount", GaugeMath.compact(stored), GaugeMath.compact(capacity),
                GaugeMath.percent(stored, capacity)));
        if (usedPerTick > 0) {
            double in = Math.max(0D, netPerTick + usedPerTick);
            out.add(tr("energy.flow", GaugeMath.signedRate(in), GaugeMath.signedRate(-usedPerTick)));
        } else {
            out.add(tr("energy.net", GaugeMath.signedRate(netPerTick)));
        }
        if (deliveredPerTick >= 0) {
            out.add(tr("energy.delivered", GaugeMath.rate(deliveredPerTick)));
        }
        if (stored >= capacity) {
            out.add(tr("energy.full"));
        }
    }

    // --- heat -------------------------------------------------------------------------------------

    /**
     * Heat: name, value / capacity (and %), the safe band, the trend, and optionally the ambient and the
     * heat a recipe source supplies ({@code null} to omit either).
     */
    public static void heat(List<Component> out, long heat, long capacity, long safeLimit, double perSecond,
            @Nullable Integer ambient, @Nullable Integer supplied) {
        out.add(tr("heat"));
        out.add(tr("heat.amount", GaugeMath.grouped(heat), GaugeMath.grouped(Math.max(0L, capacity)),
                GaugeMath.percent(heat, capacity)));
        if (safeLimit > 0) {
            out.add(heat >= safeLimit ? tr("heat.over", GaugeMath.grouped(safeLimit))
                    : tr("heat.safe", GaugeMath.grouped(safeLimit)));
        }
        int trend = GaugeMath.trend(perSecond, HEAT_DEADBAND_PER_SECOND);
        out.add(trend > 0 ? tr("heat.heating", GaugeMath.rate(perSecond))
                : trend < 0 ? tr("heat.cooling", GaugeMath.rate(perSecond)) : tr("heat.steady"));
        if (ambient != null) {
            out.add(tr("heat.ambient", GaugeMath.grouped(ambient)));
        }
        if (supplied != null && supplied > 0) {
            out.add(tr("heat.available", GaugeMath.grouped(supplied)));
        }
    }

    // --- progress ---------------------------------------------------------------------------------

    /**
     * Progress: name, percent, time left at the current speed (a falling gauge reads as fuel burning
     * down), the stall cause when one is given, and the operation rate when positive.
     */
    public static void progress(List<Component> out, boolean working, int permille, double permillePerTick,
            @Nullable Component stallCause, int opsPerMinute) {
        out.add(tr("progress"));
        if (!working) {
            out.add(stallCause != null ? stallCause : tr("progress.idle"));
            if (opsPerMinute > 0) {
                out.add(tr("progress.ops", GaugeMath.grouped(opsPerMinute)));
            }
            return;
        }
        out.add(tr("progress.percent", Math.max(0, Math.min(100, Math.round(permille / 10F)))));
        if (stallCause != null) {
            out.add(stallCause);
        } else {
            long ticks = GaugeMath.ticksRemaining(permille, permillePerTick);
            if (ticks >= 0) {
                out.add(tr(permillePerTick < 0 ? "progress.burn_left" : "progress.left", GaugeMath.duration(ticks)));
            }
        }
        if (opsPerMinute > 0) {
            out.add(tr("progress.ops", GaugeMath.grouped(opsPerMinute)));
        }
    }

    // --- tanks ------------------------------------------------------------------------------------

    /**
     * A tank: its role (optional), the medium (or "Empty"), amount / capacity in mB (gases also in
     * units), and the fill or drain rate; an output gas tank also gives its production in units/min.
     */
    public static void tank(List<Component> out, @Nullable Component role, @Nullable Component medium,
            long amount, long capacity, boolean gas, boolean output, double mbPerTick) {
        if (role != null) {
            out.add(role);
        }
        out.add(medium == null || amount <= 0 ? tr("tank.empty") : medium);
        out.add(tr("tank.amount", GaugeMath.grouped(Math.max(0L, amount)), GaugeMath.grouped(Math.max(0L, capacity))));
        if (gas) {
            out.add(tr("tank.units", GaugeMath.unitCount(GaugeMath.units(amount, NeroTechGases.UNIT_MB)),
                    GaugeMath.unitCount(GaugeMath.units(capacity, NeroTechGases.UNIT_MB))));
        }
        double perSecond = mbPerTick * GaugeMath.TICKS_PER_SECOND;
        int trend = GaugeMath.trend(perSecond, TANK_DEADBAND_PER_SECOND);
        out.add(trend > 0 ? tr("tank.filling", GaugeMath.rate(perSecond))
                : trend < 0 ? tr("tank.draining", GaugeMath.rate(perSecond)) : tr("tank.steady"));
        if (gas && output && trend > 0) {
            out.add(tr("tank.production", GaugeMath.unitCount(GaugeMath.units(Math.round(perSecond * 60D),
                    NeroTechGases.UNIT_MB))));
        }
    }

    // --- status -----------------------------------------------------------------------------------

    /** The status line hover: the full line (state and cause), then the fix for that status. */
    public static void status(List<Component> out, Component line, MachineStatus status) {
        out.add(tr("status"));
        out.add(line);
        out.add(tr("status.fix", fix(status)));
    }

    /** The one-clause fix for a status ("Connect a generator or a charged Battery Bank"). */
    public static Component fix(MachineStatus status) {
        return tr("fix." + status.name().toLowerCase(Locale.ROOT));
    }

    /** A stall cause from a bare status (machines without a detail code), or null when not stalled. */
    @Nullable
    public static Component causeOf(@Nullable MachineStatus status) {
        if (status == null || status == MachineStatus.RUNNING || status == MachineStatus.IDLE) {
            return null;
        }
        return Component.translatable("nerotech.machine.status_line", Component.translatable(status.translationKey()),
                fix(status));
    }

    // --- emission ---------------------------------------------------------------------------------

    /**
     * Wrap every line to {@code width} pixels so a long cause never runs off a small window (GUI scale 4
     * on 1280×720 leaves 320 px). Wrapped lines lose styling; these tooltips use none.
     */
    public static List<Component> wrap(Font font, List<Component> lines, int width) {
        List<Component> wrapped = new ArrayList<>(lines.size());
        int w = Math.max(60, width);
        for (Component line : lines) {
            if (font.width(line) <= w) {
                wrapped.add(line);
                continue;
            }
            for (FormattedText part : font.getSplitter().splitLines(line, w, Style.EMPTY)) {
                wrapped.add(Component.literal(part.getString()));
            }
        }
        return wrapped;
    }

    /** The wrap width for a screen {@code screenWidth} wide: half the window, 100..220 px. */
    public static int wrapWidth(int screenWidth) {
        return Math.max(100, Math.min(220, screenWidth / 2));
    }

    /** Emit a tooltip at the mouse (deferred on 1.21.6+, drawn immediately before that). */
    public static void show(GuiGraphicsExtractor extractor, Font font, List<Component> lines, int mouseX, int mouseY) {
        if (lines.isEmpty()) {
            return;
        }
        //? if >=1.21.6 {
        extractor.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
        //?} else {
        /*extractor.renderComponentTooltip(font, lines, mouseX, mouseY);
        *///?}
    }
}
