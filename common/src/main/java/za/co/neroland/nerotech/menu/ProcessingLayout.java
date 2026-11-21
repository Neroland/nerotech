package za.co.neroland.nerotech.menu;

import java.util.function.Supplier;

import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerotech.machine.ProcessingSpec;

/**
 * Where a {@link ProcessingMenu} puts its slots and where {@code client.ProcessingScreen} draws its
 * gauges, following the GUI conventions (art direction §9): title row y 6, status line y 16, energy
 * trough at x 8, heat trough at x 160, inputs left, progress arrow between, outputs right, tanks as
 * 16-wide gauges, player inventory at the bottom. The panel is 176×166, or 176×186 for tall machines
 * (more than three recipe slots or two outputs pick tall automatically).
 *
 * <p>{@link #builder(ProcessingSpec)} places everything on that grid; a machine moves any piece to
 * match its screen spec with the builder's setters. Coordinates are panel-relative; slot coordinates
 * are the slot's top-left like vanilla. Pure data (the catalyst icon is a lazy supplier), safe on a
 * dedicated server.
 */
public final class ProcessingLayout {

    /** A gauge rectangle, panel-relative. */
    public record Gauge(int x, int y, int w, int h) {
    }

    public static final int WIDTH = 176;
    public static final int HEIGHT = 166;
    public static final int TALL_HEIGHT = 186;
    public static final int STATUS_Y = 16;
    public static final int ENERGY_X = 8;
    public static final int HEAT_X = 160;
    public static final int TROUGH_W = 8;
    public static final int TROUGH_Y = 28;
    public static final int TANK_W = 16;

    private final ProcessingSpec spec;
    private final int height;
    private final int[][] inputs;
    @Nullable
    private final int[] catalyst;
    @Nullable
    private final Supplier<ItemStack> catalystIcon;
    @Nullable
    private final String catalystTooltipKey;
    private final int[][] outputs;
    private final Gauge arrow;
    private final Gauge energy;
    private final Gauge heat;
    @Nullable
    private final Gauge fluidIn;
    @Nullable
    private final Gauge fluidOut;
    @Nullable
    private final Gauge gasIn;
    @Nullable
    private final Gauge gasOut;
    private final int upgradeX;
    private final int upgradeY;
    private final int upgradeColumns;
    private final int presetX;
    private final int presetY;

    private ProcessingLayout(Builder b) {
        this.spec = b.spec;
        this.height = b.height;
        this.inputs = b.inputs;
        this.catalyst = b.catalyst;
        this.catalystIcon = b.catalystIcon;
        this.catalystTooltipKey = b.catalystTooltipKey;
        this.outputs = b.outputs;
        this.arrow = b.arrow;
        int troughH = b.height - HEIGHT + 40;
        this.energy = new Gauge(ENERGY_X, TROUGH_Y, TROUGH_W, troughH);
        this.heat = new Gauge(HEAT_X, TROUGH_Y, TROUGH_W, troughH);
        this.fluidIn = b.fluidIn;
        this.fluidOut = b.fluidOut;
        this.gasIn = b.gasIn;
        this.gasOut = b.gasOut;
        this.upgradeX = b.upgradeX;
        this.upgradeY = b.upgradeY;
        this.upgradeColumns = b.upgradeColumns;
        this.presetX = b.presetX;
        this.presetY = b.presetY;
    }

    /** A layout on the default grid for {@code spec}. */
    public static Builder builder(ProcessingSpec spec) {
        return new Builder(spec);
    }

    public ProcessingSpec spec() {
        return this.spec;
    }

    public int width() {
        return WIDTH;
    }

    public int height() {
        return this.height;
    }

    /** Y of the player inventory's first row. */
    public int playerY() {
        return this.height - 82;
    }

    public int[] input(int i) {
        return this.inputs[i];
    }

    @Nullable
    public int[] catalyst() {
        return this.catalyst;
    }

    /** The ghost icon drawn in the empty catalyst slot (a die, a lens, a seed), or null for none. */
    public ItemStack catalystIcon() {
        return this.catalystIcon == null ? ItemStack.EMPTY : this.catalystIcon.get();
    }

    /** Tooltip naming what the catalyst slot accepts, or null for the generic one. */
    @Nullable
    public String catalystTooltipKey() {
        return this.catalystTooltipKey;
    }

    public int[] output(int i) {
        return this.outputs[i];
    }

    public Gauge arrow() {
        return this.arrow;
    }

    public Gauge energy() {
        return this.energy;
    }

    public Gauge heat() {
        return this.heat;
    }

    /** Tank gauge for {@code ProcessingMachineBlockEntity.TANK_*}, or null when not drawn. */
    @Nullable
    public Gauge tank(int tank) {
        return switch (tank) {
            case 0 -> this.fluidIn;
            case 1 -> this.fluidOut;
            case 2 -> this.gasIn;
            default -> this.gasOut;
        };
    }

    public int upgradeX() {
        return this.upgradeX;
    }

    public int upgradeY() {
        return this.upgradeY;
    }

    public int upgradeColumns() {
        return this.upgradeColumns;
    }

    public int presetX() {
        return this.presetX;
    }

    public int presetY() {
        return this.presetY;
    }

    /** Builder on the default grid; every setter overrides one piece. */
    public static final class Builder {
        private final ProcessingSpec spec;
        private int height;
        private int[][] inputs;
        @Nullable
        private int[] catalyst;
        @Nullable
        private Supplier<ItemStack> catalystIcon;
        @Nullable
        private String catalystTooltipKey;
        private int[][] outputs;
        private Gauge arrow = new Gauge(84, 34, 22, 16);
        @Nullable
        private Gauge fluidIn;
        @Nullable
        private Gauge fluidOut;
        @Nullable
        private Gauge gasIn;
        @Nullable
        private Gauge gasOut;
        private int upgradeX = 62;
        private int upgradeY;
        private int upgradeColumns = 4;
        private int presetX = 136;
        private int presetY = 4;

        private Builder(ProcessingSpec spec) {
            this.spec = spec;
            int recipeSlots = spec.firstOutput();
            this.height = recipeSlots > 3 || spec.outputs() > 2 ? TALL_HEIGHT : HEIGHT;
            // Recipe slots flow on a 3-wide grid from (26, 30); the catalyst takes the next cell.
            this.inputs = new int[spec.inputs()][];
            for (int i = 0; i < spec.inputs(); i++) {
                this.inputs[i] = new int[] {26 + (i % 3) * 18, 30 + (i / 3) * 18};
            }
            if (spec.catalyst()) {
                int i = spec.inputs();
                this.catalyst = new int[] {26 + (i % 3) * 18, 30 + (i / 3) * 18};
            }
            this.outputs = new int[spec.outputs()][];
            for (int i = 0; i < spec.outputs(); i++) {
                this.outputs[i] = new int[] {110 + (i % 2) * 18, 30 + (i / 2) * 18};
            }
            this.upgradeY = this.height - HEIGHT + 54;
            // Tanks get a default spot only where the grid leaves room; screen specs place them.
            int tankH = this.height - HEIGHT + 40;
            if (spec.fluidIn() > 0 && spec.inputs() <= 1) {
                this.fluidIn = new Gauge(62, TROUGH_Y, TANK_W, tankH);
            }
            if (spec.fluidOut() > 0 && spec.outputs() <= 1) {
                this.fluidOut = new Gauge(134, TROUGH_Y, TANK_W, tankH);
            }
        }

        /** Panel height: {@link #HEIGHT} or {@link #TALL_HEIGHT}. */
        public Builder height(int height) {
            this.height = height == TALL_HEIGHT ? TALL_HEIGHT : HEIGHT;
            this.upgradeY = this.height - HEIGHT + 54;
            return this;
        }

        public Builder input(int index, int x, int y) {
            this.inputs[index] = new int[] {x, y};
            return this;
        }

        public Builder catalyst(int x, int y) {
            this.catalyst = new int[] {x, y};
            return this;
        }

        /** Ghost icon for the empty catalyst slot and the tooltip key naming what it accepts. */
        public Builder catalystHint(@Nullable Supplier<ItemStack> icon, @Nullable String tooltipKey) {
            this.catalystIcon = icon;
            this.catalystTooltipKey = tooltipKey;
            return this;
        }

        public Builder output(int index, int x, int y) {
            this.outputs[index] = new int[] {x, y};
            return this;
        }

        public Builder arrow(int x, int y, int w, int h) {
            this.arrow = new Gauge(x, y, w, h);
            return this;
        }

        public Builder fluidIn(int x, int y, int h) {
            this.fluidIn = new Gauge(x, y, TANK_W, h);
            return this;
        }

        public Builder fluidOut(int x, int y, int h) {
            this.fluidOut = new Gauge(x, y, TANK_W, h);
            return this;
        }

        public Builder gasIn(int x, int y, int h) {
            this.gasIn = new Gauge(x, y, TANK_W, h);
            return this;
        }

        public Builder gasOut(int x, int y, int h) {
            this.gasOut = new Gauge(x, y, TANK_W, h);
            return this;
        }

        /** Upgrade-module grid origin and width in columns (4 slots). */
        public Builder upgrades(int x, int y, int columns) {
            this.upgradeX = x;
            this.upgradeY = y;
            this.upgradeColumns = columns;
            return this;
        }

        public Builder preset(int x, int y) {
            this.presetX = x;
            this.presetY = y;
            return this;
        }

        public ProcessingLayout build() {
            return new ProcessingLayout(this);
        }
    }
}
