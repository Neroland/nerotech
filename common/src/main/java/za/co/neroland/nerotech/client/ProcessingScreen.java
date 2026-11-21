package za.co.neroland.nerotech.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerotech.gas.NeroTechGases;
import za.co.neroland.nerotech.machine.MachineStatus;
import za.co.neroland.nerotech.machine.ProcessingMachineBlockEntity;
import za.co.neroland.nerotech.machine.ProcessingSpec;
import za.co.neroland.nerotech.machine.Readout;
import za.co.neroland.nerotech.menu.ProcessingLayout;
import za.co.neroland.nerotech.menu.ProcessingMenu;

/**
 * The generic screen for every {@link ProcessingMenu}, drawn from its {@link ProcessingLayout} on the
 * {@link MachineScreen} hull (art direction §9): status line at y 16 naming the state and, when
 * stalled, the cause and the fix; energy trough at x 8 and heat trough at x 160; tank gauges; a
 * progress arrow; a ghost icon in the empty catalyst slot; and a tooltip on every gauge, the arrow and
 * every empty role slot. All text is English source keys. Subclass for a special screen and override
 * {@link #extractMachine} to draw more.
 *
 * <p>Register with {@code sink.register(MENU.get(), ProcessingScreen::create)}.
 *
 * @param <T> the processing menu type
 */
public class ProcessingScreen<T extends ProcessingMenu> extends MachineScreen<T> {

    private static final int EDGE = 0xFF05080D;
    private static final int TROUGH = 0xFF0B1119;
    private static final int ENERGY = 0xFFE0B33A;
    private static final int WORK = 0xFF55C2F0;
    private static final int HEAT = 0xFFE0543A;
    private static final int SUBTLE = 0xFF8DA0B4;
    private static final int GHOST_VEIL = 0xAA8B8B8B;
    /** Status line colours by readout (REST, WORK, STALL, HOT); the words carry the meaning. */
    private static final int[] READOUT_COLORS = {0xFF4DD0E1, 0xFF55C2F0, 0xFFE0B33A, 0xFFE0543A};
    /** Gauge colours for the seven NeroTech gases, by index in {@code NeroTechGases.ALL}. */
    private static final int[] GAS_COLORS = {
            0xFFCEFFFF, 0xFF4DD0E1, 0xFFB8D8FF, 0xFFF0E6A0, 0xFF9CCC65, 0xFF90A4FF, 0xFFB0B0B0};
    private static final int WATER = 0xFF3A7FE0;
    private static final int LAVA = 0xFFE0782A;
    private static final int FLUID_DEFAULT = 0xFF4DB6AC;

    protected final ProcessingLayout layout;

    public ProcessingScreen(T menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, menu.layout().width(), menu.layout().height());
        this.layout = menu.layout();
    }

    /** Typed factory for screen registration ({@code ProcessingScreen::create}). */
    public static <M extends ProcessingMenu> ProcessingScreen<M> create(M menu, Inventory playerInventory,
            Component title) {
        return new ProcessingScreen<>(menu, playerInventory, title);
    }

    @Override
    protected boolean drawsDefaultGauges() {
        return false;
    }

    @Override
    protected boolean drawsDefaultWorkBar() {
        return false;
    }

    @Override
    protected int presetX() {
        return this.layout.presetX();
    }

    @Override
    protected int presetY() {
        return this.layout.presetY();
    }

    @Override
    protected int headerDividerY() {
        return 26;
    }

    @Override
    public void extractContents(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        super.extractContents(extractor, mouseX, mouseY, partialTick);
        extractMachine(extractor, mouseX, mouseY);
        // The tooltip itself is emitted by MachineScreen from gaugeTooltip (once, after every part).
    }

    @Override
    protected List<Component> gaugeTooltip(int mx, int my) {
        return tooltip(mx, my);
    }

    /** The current recipe's draw while a job runs (0 idle), from the synced energy rate. */
    @Override
    protected long energyUsedPerTick() {
        return this.menu.working() ? Math.max(0, this.menu.energyRate()) : 0L;
    }

    @Nullable
    @Override
    protected Integer heatSupplied() {
        return this.menu.heatAvailable();
    }

    /** The status line (state, cause and fix) whenever the machine is not simply running or resting. */
    @Nullable
    @Override
    protected Component stallCause() {
        MachineStatus status = this.menu.status();
        if (status == MachineStatus.RUNNING) {
            return null;
        }
        if (status == MachineStatus.IDLE && this.menu.detail() < 0) {
            return null;
        }
        return statusLine();
    }

    /** Status line, troughs, tanks, arrow and catalyst ghost. Override to add machine-specific parts. */
    protected void extractMachine(GuiGraphicsExtractor extractor, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        extractor.text(this.font, statusLine(), x + 8, y + ProcessingLayout.STATUS_Y,
                READOUT_COLORS[readout().ordinal()], false);

        ProcessingLayout.Gauge e = this.layout.energy();
        if (drawsEnergyTrough()) {
            gauge(extractor, x + e.x(), y + e.y(), e.w(), e.h(), this.menu.energyFraction(), ENERGY);
        }
        ProcessingLayout.Gauge h = this.layout.heat();
        gauge(extractor, x + h.x(), y + h.y(), h.w(), h.h(), this.menu.heatFraction(), HEAT);

        for (int tank = 0; tank < 4; tank++) {
            ProcessingLayout.Gauge g = this.layout.tank(tank);
            long capacity = this.menu.tankCapacity(tank);
            if (g == null || capacity <= 0) {
                continue;
            }
            sampleTank(tank, this.menu.tankAmount(tank));
            float f = (float) this.menu.tankAmount(tank) / capacity;
            gauge(extractor, x + g.x(), y + g.y(), g.w(), g.h(), f, tankColor(tank, this.menu.tankMedium(tank)));
        }

        ProcessingLayout.Gauge a = this.layout.arrow();
        int ax = x + a.x();
        int ay = y + a.y();
        if (drawsWorkArrow()) {
            extractor.fill(ax - 1, ay + a.h() / 2 - 3, ax + a.w() + 1, ay + a.h() / 2 + 4, EDGE);
            extractor.fill(ax, ay + a.h() / 2 - 2, ax + a.w(), ay + a.h() / 2 + 3, TROUGH);
            int fw = Math.round(a.w() * Math.max(0f, Math.min(1f, this.menu.workFraction())));
            if (this.menu.working() && fw > 0) {
                extractor.fill(ax, ay + a.h() / 2 - 2, ax + fw, ay + a.h() / 2 + 3, WORK);
            }
            // Arrow head so the bar reads as a direction even when empty.
            for (int i = 0; i < 4; i++) {
                extractor.fill(ax + a.w() - 4 + i, ay + a.h() / 2 - 4 + i, ax + a.w() - 3 + i, ay + a.h() / 2 + 5 - i,
                        fw >= a.w() - 4 + i ? WORK : SUBTLE);
            }
        }

        ProcessingSpec spec = this.layout.spec();
        if (spec.catalyst() && this.layout.catalyst() != null) {
            Slot slot = this.menu.slots.get(spec.catalystSlot());
            ItemStack icon = this.layout.catalystIcon();
            if (!slot.hasItem() && !icon.isEmpty()) {
                int[] at = this.layout.catalyst();
                extractor.item(icon, x + at[0], y + at[1]);
                extractor.fill(x + at[0], y + at[1], x + at[0] + 16, y + at[1] + 16, GHOST_VEIL);
            }
        }
    }

    /**
     * Whether the energy trough is drawn. Blocks that draw no power (Heat Exchanger, Annunciator Panel)
     * return false: an empty trough would read as "out of power".
     */
    protected boolean drawsEnergyTrough() {
        return true;
    }

    /** Whether the progress arrow is drawn (the Annunciator Panel puts its alarm rows there instead). */
    protected boolean drawsWorkArrow() {
        return true;
    }

    /** The readout the status line shows (same word as the lamp). */
    protected Readout readout() {
        int heatBucket = Math.min(5, (int) (this.menu.heatFraction() * 6f));
        return Readout.of(this.menu.status(), heatBucket);
    }

    /** "Working", or "Stalled — Too cold: needs 800 heat" (state word, then cause and fix). */
    protected Component statusLine() {
        Component state = Component.translatable("nerotech.machine.readout." + readout().name().toLowerCase(Locale.ROOT));
        int detail = this.menu.detail();
        if (detail < 0 || detail >= ProcessingMachineBlockEntity.DETAIL_KEYS.length) {
            return state;
        }
        // While running, only the two causes that explain a running machine are shown (heating up first,
        // growing without power); otherwise "Working" stands alone.
        if (this.menu.status() == MachineStatus.RUNNING && detail != ProcessingMachineBlockEntity.DETAIL_PREHEATING
                && detail != ProcessingMachineBlockEntity.DETAIL_GROWING_UNPOWERED) {
            return state;
        }
        Component cause = Component.translatable(ProcessingMachineBlockEntity.DETAIL_KEYS[detail],
                this.menu.detailParam());
        return Component.translatable("nerotech.machine.status_line", state, cause);
    }

    /** A tank gauge colour by medium (fluids by id, gases by index); override for art direction §6 colours. */
    protected int tankColor(int tank, int medium) {
        if (tank >= ProcessingMachineBlockEntity.TANK_GAS_IN) {
            return medium >= 0 && medium < GAS_COLORS.length ? GAS_COLORS[medium] : SUBTLE;
        }
        Fluid fluid = medium < 0 ? Fluids.EMPTY : BuiltInRegistries.FLUID.byId(medium);
        if (fluid == Fluids.WATER) {
            return WATER;
        }
        if (fluid == Fluids.LAVA) {
            return LAVA;
        }
        return medium < 0 ? SUBTLE : FLUID_DEFAULT;
    }

    /** The medium's display name for a tooltip. */
    protected Component mediumName(int tank, int medium) {
        if (medium < 0) {
            return Component.translatable("nerotech.gui.tank.empty");
        }
        Identifier id;
        String prefix;
        if (tank >= ProcessingMachineBlockEntity.TANK_GAS_IN) {
            if (medium >= NeroTechGases.ALL.size()) {
                return Component.translatable("nerotech.gui.tank.empty");
            }
            id = NeroTechGases.ALL.get(medium);
            prefix = "gas.";
        } else {
            Fluid fluid = BuiltInRegistries.FLUID.byId(medium);
            if (fluid == null) {
                return Component.translatable("nerotech.gui.tank.empty");
            }
            id = BuiltInRegistries.FLUID.getKey(fluid);
            prefix = "block.";
        }
        return Component.translatable(prefix + id.getNamespace() + "." + id.getPath());
    }

    private static boolean inside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    /** Tooltip lines for the panel-relative mouse position, or empty. */
    protected List<Component> tooltip(int mx, int my) {
        List<Component> lines = new ArrayList<>();
        Component status = statusLine();
        if (inside(mx, my, 8, ProcessingLayout.STATUS_Y - 1, Math.max(40, this.font.width(status)), 10)) {
            GaugeTooltips.status(lines, status, this.menu.status());
            return lines;
        }
        ProcessingLayout.Gauge e = this.layout.energy();
        if (drawsEnergyTrough() && over(mx, my, e.x(), e.y(), e.w(), e.h())) {
            energyTooltip(lines);
            return lines;
        }
        ProcessingLayout.Gauge h = this.layout.heat();
        if (over(mx, my, h.x(), h.y(), h.w(), h.h())) {
            heatTooltip(lines);
            return lines;
        }
        for (int tank = 0; tank < 4; tank++) {
            ProcessingLayout.Gauge g = this.layout.tank(tank);
            long capacity = this.menu.tankCapacity(tank);
            if (g != null && capacity > 0 && over(mx, my, g.x(), g.y(), g.w(), g.h())) {
                int medium = this.menu.tankMedium(tank);
                GaugeTooltips.tank(lines, Component.translatable("nerotech.gui.tank." + TANK_ROLES[tank]),
                        medium < 0 ? null : mediumName(tank, medium), this.menu.tankAmount(tank), capacity,
                        tank >= ProcessingMachineBlockEntity.TANK_GAS_IN,
                        tank == ProcessingMachineBlockEntity.TANK_FLUID_OUT || tank == ProcessingMachineBlockEntity.TANK_GAS_OUT,
                        tankWindow(tank).perTick());
                return lines;
            }
        }
        ProcessingLayout.Gauge a = this.layout.arrow();
        if (drawsWorkArrow() && inside(mx, my, a.x(), a.y(), a.w(), a.h())) {
            progressTooltip(lines);
            return lines;
        }
        Slot hovered = this.hoveredSlot;
        ProcessingSpec spec = this.layout.spec();
        if (hovered != null && !hovered.hasItem() && hovered.index < spec.slotCount()) {
            if (hovered.index == spec.catalystSlot()) {
                String key = this.layout.catalystTooltipKey();
                lines.add(Component.translatable(key != null ? key : "nerotech.gui.slot.catalyst"));
            } else if (spec.isOutput(hovered.index)) {
                lines.add(Component.translatable("nerotech.gui.slot.output"));
            } else {
                lines.add(Component.translatable("nerotech.gui.slot.input"));
            }
        }
        return lines;
    }

    private static final String[] TANK_ROLES = {"fluid_in", "fluid_out", "gas_in", "gas_out"};
}
