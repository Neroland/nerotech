package za.co.neroland.nerotech.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.client.SideConfigWidget;
import za.co.neroland.nerolandcore.sideconfig.SideConfigComponent;
import za.co.neroland.nerolandcore.sideconfig.SideConfigured;

import za.co.neroland.nerotech.machine.MachinePreset;
import za.co.neroland.nerotech.machine.MachineStatus;
import za.co.neroland.nerotech.menu.MachineMenu;
import za.co.neroland.nerotech.network.ClientMenuPos;
import za.co.neroland.nerotech.network.MachineStatsPayload;
import za.co.neroland.nerotech.network.MachinePresetPayload;
import za.co.neroland.nerotech.network.NeroTechNetwork;

/**
 * One procedural screen for every NeroTech machine menu — a dark sci-fi hull panel drawn entirely with
 * {@code fill}s (no GUI texture asset). Consistent layout for every machine: energy + heat gauges on the
 * left (each with an always-visible colour cap so it reads even when empty), machine I/O slots centred,
 * upgrade-module slots as a 2×2 block top-right, and a work-progress bar along the bottom of the machine
 * area. Two collapsible side tabs: Core's Side Config net and the Stage G {@link AnalyticsWidget}
 * (only one expands at a time). 26.x renders container screens via {@code extract*(GuiGraphicsExtractor, ...)}.
 *
 * <h2>Add-on GUI contract (stable within 0.x)</h2>
 * Add-ons (NeroPower) extend this screen for their own {@link MachineMenu} subclasses. It draws only
 * the shared {@code ContainerData} indices, which NeroTech keeps fixed within a 0.x minor:
 * <ul>
 *   <li>0 — energy permille of capacity; 1 — constant 1000 (energy gauge);</li>
 *   <li>2 — work progress permille; 3 — 1000 while working, else 0 (progress bar);</li>
 *   <li>4 — heat permille of {@code heatCapacity}; 5 — constant 1000 (heat gauge);</li>
 *   <li>6 — {@link MachinePreset} ordinal (preset button).</li>
 * </ul>
 * Indices {@code 7+} carry the block entity's {@code extraData(int)} values, {@code extraDataCount()}
 * of them, and are the add-on's to draw (see {@link MachineMenu#extraFraction(int)} /
 * {@link MachineMenu#extraValue(int)}); the menu's data must be sized {@code 7 + extraDataCount()}.
 * That layout plus those two block-entity hooks is the stable extension point — see
 * {@link MachineMenu} for the full table.
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

    // Stage H preset cycle button: a small pill under the upgrade block, beside the gauges' row.
    // Free in every machine layout (upgrades end at y+54, the work bar spans x+40..136 at y+61).
    private static final int PRESET_X = 138;
    private static final int PRESET_Y = 56;
    private static final int PRESET_W = 36;
    private static final int PRESET_H = 12;
    /** Eco teal / Balanced white / Overdrive amber (indexed by preset ordinal). */
    private static final int[] PRESET_COLORS = {0xFF4DD0E1, 0xFFE6E6F0, 0xFFE0B33A};

    /** Core's universal Side Config tab (top-right), built once the machine BE is resolved. */
    @Nullable
    private SideConfigWidget sideConfigWidget;
    private boolean sideConfigResolved;

    /** The Stage G Analytics tab, stacked under the Side Config tab (needs only the container id). */
    private final AnalyticsWidget analyticsWidget;

    // Gauge tooltips (0.8.0): rates are measured here from the synced values over the last ~20 client
    // ticks, so no server change or packet is needed for them.
    private final GaugeMath.RateWindow energyWindow = new GaugeMath.RateWindow();
    private final GaugeMath.RateWindow heatWindow = new GaugeMath.RateWindow();
    /** Progress resets at a job boundary (a jump of more than 30%). */
    private final GaugeMath.RateWindow progressWindow = new GaugeMath.RateWindow(300);
    /** Tank windows by tank index (processing tanks 0..3, legacy gauges 0..n, injector couplings). */
    private final GaugeMath.RateWindow[] tankWindows = new GaugeMath.RateWindow[8];

    /** Default gauge and work-bar rectangles (panel-relative). */
    private static final int ENERGY_X = 8;
    private static final int HEAT_X = 20;
    private static final int GAUGE_Y = 20;
    private static final int GAUGE_W = 10;
    private static final int GAUGE_H = 46;
    private static final int BAR_X = 40;
    private static final int BAR_Y = 61;
    private static final int BAR_W = 96;
    private static final int BAR_H = 7;

    public MachineScreen(T menu, Inventory playerInventory, Component title) {
        this(menu, playerInventory, title, 176, 166);
    }

    /**
     * A machine screen with a custom panel size (0.8.0: tall processing screens are 176×186; never
     * wider than 176). The hull dividers, inventory label and side tabs follow the size.
     */
    protected MachineScreen(T menu, Inventory playerInventory, Component title, int imageWidth, int imageHeight) {
        //? if >=26.1 {
        super(menu, playerInventory, title, imageWidth, imageHeight);
        //?} else {
        /*super(menu, playerInventory, title);
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        this.inventoryLabelY = imageHeight - 94;
        *///?}
        this.titleLabelX = 8;
        this.inventoryLabelX = 8;
        // Anchored just below the Side Config tab button; hidden while that panel is expanded
        // (the two open panels would otherwise overlap in the same side column).
        this.analyticsWidget = new AnalyticsWidget(menu.containerId, this.imageWidth + 4, 20,
                menu::presetOrdinal);
    }

    /** Typed factory for screen registration ({@code MachineScreen::create}). */
    public static <M extends MachineMenu> MachineScreen<M> create(M menu, Inventory playerInventory,
            Component title) {
        return new MachineScreen<>(menu, playerInventory, title);
    }

    //? if >=26.1 {
    @Override
    public void extractContents(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        sampleGauges();
        extractPanel(extractor);
        super.extractContents(extractor, mouseX, mouseY, partialTick);
        extractTabs(extractor, mouseX, mouseY);
        // Deferred to the end of the frame on 26.x, so subclasses drawing after super still sit under it.
        emitGaugeTooltip(extractor, mouseX, mouseY);
    }
    //?} else {
    /*// Before 26.1 the hull panel is the container background (renderBg) and render() draws the slots.
    // Everything 26.x draws after the slots goes through extractContents, which subclasses extend.
    @Override
    public void render(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        sampleGauges();
        super.render(extractor, mouseX, mouseY, partialTick);
        extractContents(extractor, mouseX, mouseY, partialTick);
        // After every subclass part, so an immediately drawn (pre-1.21.6) tooltip sits on top.
        emitGaugeTooltip(extractor, mouseX, mouseY);
        this.renderTooltip(extractor, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphicsExtractor extractor, float partialTick, int mouseX, int mouseY) {
        extractPanel(extractor);
    }

    public void extractContents(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        extractTabs(extractor, mouseX, mouseY);
    }
    *///?}

    /** The hull panel, slot wells, gauges, preset button and work bar (drawn under the slots). */
    private void extractPanel(GuiGraphicsExtractor extractor) {
        int x = this.leftPos;
        int y = this.topPos;
        int w = this.imageWidth;
        int h = this.imageHeight;

        // Hull panel with a soft top sheen + a 1px border.
        extractor.fill(x - 1, y - 1, x + w + 1, y + h + 1, EDGE);
        extractor.fill(x, y, x + w, y + h, PANEL);
        extractor.fill(x, y, x + w, y + 18, PANEL_HI);
        // Dividers under the title and above the player inventory.
        extractor.fill(x + 7, y + headerDividerY(), x + w - 7, y + headerDividerY() + 1, DIVIDER);
        extractor.fill(x + 7, y + h - 96, x + w - 7, y + h - 95, DIVIDER);

        // Slot wells (follow the menu's slot positions automatically). Slots parked at negative
        // coordinates are sync-only (ghost templates / previews) and get no well.
        for (Slot slot : this.menu.slots) {
            if (slot.x < 0 || slot.y < 0) {
                continue;
            }
            int sx = x + slot.x;
            int sy = y + slot.y;
            extractor.fill(sx - 1, sy - 1, sx + 17, sy + 17, WELL_EDGE);
            extractor.fill(sx, sy, sx + 16, sy + 16, WELL);
        }

        // Energy + heat gauges (left), each with an always-on colour cap.
        if (drawsDefaultGauges()) {
            gauge(extractor, x + ENERGY_X, y + GAUGE_Y, GAUGE_W, GAUGE_H, this.menu.energyFraction(), ENERGY);
            gauge(extractor, x + HEAT_X, y + GAUGE_Y, GAUGE_W, GAUGE_H, this.menu.heatFraction(), HEAT);
        }

        // Stage H preset cycle button (Eco/Balanced/Overdrive; click sends the serverbound intent).
        presetButton(extractor, x, y);

        // Work-progress bar along the bottom of the machine area, lit while working.
        if (drawsDefaultWorkBar() && this.menu.working()) {
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
    }

    /** The Side Config and Analytics side tabs (drawn over the panel and slots). */
    private void extractTabs(GuiGraphicsExtractor extractor, int mouseX, int mouseY) {
        // Core's Side Config tab, anchored top-right of the GUI (drawn over the panel + slots).
        SideConfigWidget widget = sideConfig();
        if (widget != null) {
            widget.render(extractor, this.leftPos, this.topPos, mouseX, mouseY);
        }
        // The Analytics tab below it — hidden while the Side Config panel is expanded (overlap).
        if (widget == null || !widget.isOpen()) {
            this.analyticsWidget.render(extractor, this.leftPos, this.topPos, mouseX, mouseY);
        }
    }

    /**
     * Lazily build the Side Config widget the first time it can resolve this machine's block-entity and
     * its {@link SideConfigComponent}. The position is taken from the menu when known, then from the
     * server's menu-position payload (mailboxed by container id in {@link ClientMenuPos}), and
     * only as a last resort from the block the player is looking at (the machine they just opened).
     * Returns null for machines without a side config.
     */
    @Nullable
    private SideConfigWidget sideConfig() {
        if (sideConfigResolved) {
            return this.sideConfigWidget;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return null; // try again next frame
        }
        BlockPos pos = this.menu.machinePos();
        if (pos == null) {
            // Authoritative: the position the server sent for exactly this container id.
            pos = ClientMenuPos.poll(this.menu.containerId);
            if (pos != null) {
                this.menu.setMachinePos(pos);
            }
        }
        if (pos == null && mc.hitResult instanceof BlockHitResult hit && mc.hitResult.getType() == HitResult.Type.BLOCK) {
            pos = hit.getBlockPos();
        }
        if (pos == null) {
            return null;
        }
        BlockEntity be = mc.level.getBlockEntity(pos);
        if (!(be instanceof SideConfigured configured)) {
            return null;
        }
        SideConfigComponent comp = configured.sideConfig();
        if (comp == null) {
            sideConfigResolved = true;
            return null;
        }
        this.menu.setMachinePos(pos);
        String typeKey = typeKey(be.getType());
        // Anchor just right of the GUI so the labelled side-config panel sits beside it, not over it.
        this.sideConfigWidget = new SideConfigWidget(pos, comp.config(), typeKey, this.imageWidth + 4, 0);
        sideConfigResolved = true;
        return this.sideConfigWidget;
    }

    /**
     * The Stage H preset cycle button: a colour-coded pill (Eco teal / Balanced white / Overdrive
     * amber) with the preset's short label, tucked under the upgrade block. Clicking cycles
     * Eco → Balanced → Overdrive and sends the serverbound {@code MachinePresetPayload}; the synced
     * ContainerData (index 6) brings the authoritative value back, so the button never guesses.
     */
    private void presetButton(GuiGraphicsExtractor extractor, int x, int y) {
        MachinePreset preset = MachinePreset.byOrdinal(this.menu.presetOrdinal());
        int color = PRESET_COLORS[preset.ordinal()];
        int bx = x + presetX();
        int by = y + presetY();
        extractor.fill(bx - 1, by - 1, bx + PRESET_W + 1, by + PRESET_H + 1, EDGE);
        extractor.fill(bx, by, bx + PRESET_W, by + PRESET_H, TROUGH);
        // Always-on colour cap (the gauges' identification recipe) + the short label in the colour.
        extractor.fill(bx, by, bx + PRESET_W, by + 1, color);
        Component label = Component.translatable(preset.shortTranslationKey());
        extractor.text(this.font, label,
                bx + PRESET_W / 2 - this.font.width(label) / 2, by + 3, color, false);
    }

    /** Mouse-over test for the preset button (screen coordinates). */
    private boolean overPresetButton(double mouseX, double mouseY) {
        int bx = this.leftPos + presetX();
        int by = this.topPos + presetY();
        return mouseX >= bx && mouseX < bx + PRESET_W && mouseY >= by && mouseY < by + PRESET_H;
    }

    // --- gauge tooltips (0.8.0; art direction §9) ---------------------------------------------------

    /** The current game tick (the rate windows' clock), or 0 before a level exists. */
    protected final long gaugeTick() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level == null ? 0L : mc.level.getGameTime();
    }

    /** The rate window for tank {@code index} (created on first use). */
    protected final GaugeMath.RateWindow tankWindow(int index) {
        int i = Math.max(0, Math.min(this.tankWindows.length - 1, index));
        if (this.tankWindows[i] == null) {
            this.tankWindows[i] = new GaugeMath.RateWindow();
        }
        return this.tankWindows[i];
    }

    /** Record one tank amount for its rate (call once per frame from the screen that draws the tank). */
    protected final void sampleTank(int index, long amount) {
        tankWindow(index).sample(gaugeTick(), amount);
    }

    /** Sample the shared gauges once per frame (one sample per game tick is kept). */
    private void sampleGauges() {
        long tick = gaugeTick();
        this.energyWindow.sample(tick, this.menu.energyStored());
        this.heatWindow.sample(tick, this.menu.heatRaw());
        if (this.menu.working()) {
            this.progressWindow.sample(tick, Math.round(this.menu.workFraction() * 1000F));
        } else {
            this.progressWindow.reset();
        }
    }

    /** Net energy change per tick over the window. */
    protected final double gaugeEnergyNetPerTick() {
        return this.energyWindow.perTick();
    }

    /** Heat change per second over the window. */
    protected final double heatPerSecond() {
        return this.heatWindow.perSecond();
    }

    /** Progress change in permille per tick over the window. */
    protected final double progressPerTick() {
        return this.progressWindow.perTick();
    }

    /** The latest Stage G analytics snapshot (1 Hz while the menu is open), or null before the first. */
    @Nullable
    protected final MachineStatsPayload latestStats() {
        return this.analyticsWidget.latest();
    }

    /** The live status from the analytics snapshot, or null before the first one arrives. */
    @Nullable
    protected MachineStatus liveStatus() {
        MachineStatsPayload stats = latestStats();
        return stats == null ? null : MachineStatus.byOrdinal(stats.status());
    }

    /** Operations per minute from the analytics snapshot (0 when unknown or idle). */
    protected int opsPerMinute() {
        MachineStatsPayload stats = latestStats();
        return stats == null ? 0 : Math.max(0, stats.opsRate()) * 60;
    }

    /** NE per tick the machine draws for its current job, or -1 when the screen does not know it. */
    protected long energyUsedPerTick() {
        return -1L;
    }

    /** NE per tick the machine hands to something else (a charger), or -1 when not applicable. */
    protected long energyDeliveredPerTick() {
        return -1L;
    }

    /** Heat a recipe source supplies, or null when the machine has none. */
    @Nullable
    protected Integer heatSupplied() {
        return null;
    }

    /** Why work is not moving, or null when it is (or the screen cannot tell). */
    @Nullable
    protected Component stallCause() {
        return GaugeTooltips.causeOf(liveStatus());
    }

    /** Append the energy gauge tooltip. */
    protected void energyTooltip(List<Component> lines) {
        GaugeTooltips.energy(lines, this.menu.energyStored(), this.menu.energyCapacity(), gaugeEnergyNetPerTick(),
                energyUsedPerTick(), energyDeliveredPerTick());
    }

    /** Append the heat gauge tooltip. */
    protected void heatTooltip(List<Component> lines) {
        MachineStatsPayload stats = latestStats();
        long capacity = this.menu.heatCapacityRaw();
        if (capacity <= 0 && stats != null) {
            capacity = stats.heatCapacity();
        }
        GaugeTooltips.heat(lines, this.menu.heatRaw(), capacity, this.menu.heatSafeLimit(), heatPerSecond(),
                stats == null ? null : stats.ambientHeat(), heatSupplied());
    }

    /** Append the progress tooltip. */
    protected void progressTooltip(List<Component> lines) {
        boolean working = this.menu.working();
        int permille = Math.round(this.menu.workFraction() * 1000F);
        GaugeTooltips.progress(lines, working, permille, progressPerTick(), stallCause(), opsPerMinute());
    }

    /** Whether a panel-relative point is inside a rectangle (one pixel of slack for the gauge frame). */
    protected static boolean over(int mx, int my, int x, int y, int w, int h) {
        return mx >= x - 1 && mx <= x + w && my >= y - 1 && my <= y + h;
    }

    /**
     * Tooltip lines for a panel-relative mouse position, or empty. The base answers for the default
     * energy/heat gauges and the work bar; screens with their own gauges override and extend.
     */
    protected List<Component> gaugeTooltip(int mx, int my) {
        List<Component> lines = new ArrayList<>();
        if (drawsDefaultGauges()) {
            if (over(mx, my, ENERGY_X, GAUGE_Y, GAUGE_W, GAUGE_H)) {
                energyTooltip(lines);
                return lines;
            }
            if (over(mx, my, HEAT_X, GAUGE_Y, GAUGE_W, GAUGE_H)) {
                heatTooltip(lines);
                return lines;
            }
        }
        if (drawsDefaultWorkBar() && over(mx, my, BAR_X, BAR_Y, BAR_W, BAR_H)) {
            progressTooltip(lines);
        }
        return lines;
    }

    /** Emit the gauge tooltip under the mouse, unless an item tooltip owns that spot. */
    private void emitGaugeTooltip(GuiGraphicsExtractor extractor, int mouseX, int mouseY) {
        Slot hovered = this.hoveredSlot;
        if (hovered != null && hovered.hasItem()) {
            return;
        }
        List<Component> lines = gaugeTooltip(mouseX - this.leftPos, mouseY - this.topPos);
        if (lines.isEmpty()) {
            return;
        }
        GaugeTooltips.show(extractor, this.font,
                GaugeTooltips.wrap(this.font, lines, GaugeTooltips.wrapWidth(this.width)), mouseX, mouseY);
    }

    // --- layout hooks (0.8.0, additive): ProcessingScreen moves the shared widgets per its layout ---

    /** Whether the hull draws the default energy/heat gauge pair at x 8 / x 20. */
    protected boolean drawsDefaultGauges() {
        return true;
    }

    /** Whether the hull draws the default work bar along the bottom of the machine area. */
    protected boolean drawsDefaultWorkBar() {
        return true;
    }

    /** Panel-relative x of the preset button. */
    protected int presetX() {
        return PRESET_X;
    }

    /** Panel-relative y of the preset button. */
    protected int presetY() {
        return PRESET_Y;
    }

    /** Panel-relative y of the divider under the title row. */
    protected int headerDividerY() {
        return 16;
    }

    /** Block-entity-type registry id as the copy/paste compatibility key (same-type machines only). */
    private static String typeKey(BlockEntityType<?> type) {
        Identifier id = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(type);
        return id == null ? "nerotech:machine" : id.toString();
    }

    //? if >=1.21.9 {
    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent mouseButtonEvent, boolean doubleClick) {
        double mouseX = mouseButtonEvent.x();
        double mouseY = mouseButtonEvent.y();
        int button = mouseButtonEvent.button();
    //?} else {
    /*@Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
    *///?}
        SideConfigWidget widget = sideConfig();
        if (widget != null && widget.mouseClicked(mouseX, mouseY, button, this.leftPos, this.topPos)) {
            return true;
        }
        // The Analytics tab is hidden (and therefore unclickable) while Side Config is expanded.
        if ((widget == null || !widget.isOpen())
                && this.analyticsWidget.mouseClicked(mouseX, mouseY, button, this.leftPos, this.topPos)) {
            return true;
        }
        // Stage H preset cycle: send the intent; the server validates and syncs the result back.
        if (overPresetButton(mouseX, mouseY)) {
            MachinePreset next = MachinePreset.byOrdinal(this.menu.presetOrdinal()).next();
            NeroTechNetwork.sendToServer(new MachinePresetPayload(this.menu.containerId, next.ordinal()));
            return true;
        }
        //? if >=1.21.9 {
        return super.mouseClicked(mouseButtonEvent, doubleClick);
        //?} else {
        /*return super.mouseClicked(mouseX, mouseY, button);
        *///?}
    }

    protected static void gauge(GuiGraphicsExtractor g, int x, int y, int w, int h, float frac, int fill) {
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
