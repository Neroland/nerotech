package za.co.neroland.nerotech.client;

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
import za.co.neroland.nerotech.menu.MachineMenu;
import za.co.neroland.nerotech.network.ClientMenuPos;
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

    public MachineScreen(T menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 166);
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

    @Override
    public void extractContents(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        int x = this.leftPos;
        int y = this.topPos;
        int w = this.imageWidth;
        int h = this.imageHeight;

        // Hull panel with a soft top sheen + a 1px border.
        extractor.fill(x - 1, y - 1, x + w + 1, y + h + 1, EDGE);
        extractor.fill(x, y, x + w, y + h, PANEL);
        extractor.fill(x, y, x + w, y + 18, PANEL_HI);
        // Dividers under the title and above the player inventory.
        extractor.fill(x + 7, y + 16, x + w - 7, y + 17, DIVIDER);
        extractor.fill(x + 7, y + 70, x + w - 7, y + 71, DIVIDER);

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
        gauge(extractor, x + 8, y + 20, 10, 46, this.menu.energyFraction(), ENERGY);
        gauge(extractor, x + 20, y + 20, 10, 46, this.menu.heatFraction(), HEAT);

        // Stage H preset cycle button (Eco/Balanced/Overdrive; click sends the serverbound intent).
        presetButton(extractor, x, y);

        // Work-progress bar along the bottom of the machine area, lit while working.
        if (this.menu.working()) {
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

        super.extractContents(extractor, mouseX, mouseY, partialTick);

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
        int bx = x + PRESET_X;
        int by = y + PRESET_Y;
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
        int bx = this.leftPos + PRESET_X;
        int by = this.topPos + PRESET_Y;
        return mouseX >= bx && mouseX < bx + PRESET_W && mouseY >= by && mouseY < by + PRESET_H;
    }

    /** Block-entity-type registry id as the copy/paste compatibility key (same-type machines only). */
    private static String typeKey(BlockEntityType<?> type) {
        Identifier id = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(type);
        return id == null ? "nerotech:machine" : id.toString();
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent mouseButtonEvent, boolean doubleClick) {
        SideConfigWidget widget = sideConfig();
        if (widget != null && widget.mouseClicked(mouseButtonEvent.x(), mouseButtonEvent.y(),
                mouseButtonEvent.button(), this.leftPos, this.topPos)) {
            return true;
        }
        // The Analytics tab is hidden (and therefore unclickable) while Side Config is expanded.
        if ((widget == null || !widget.isOpen())
                && this.analyticsWidget.mouseClicked(mouseButtonEvent.x(), mouseButtonEvent.y(),
                mouseButtonEvent.button(), this.leftPos, this.topPos)) {
            return true;
        }
        // Stage H preset cycle: send the intent; the server validates and syncs the result back.
        if (overPresetButton(mouseButtonEvent.x(), mouseButtonEvent.y())) {
            MachinePreset next = MachinePreset.byOrdinal(this.menu.presetOrdinal()).next();
            NeroTechNetwork.sendToServer(new MachinePresetPayload(this.menu.containerId, next.ordinal()));
            return true;
        }
        return super.mouseClicked(mouseButtonEvent, doubleClick);
    }

    private static void gauge(GuiGraphicsExtractor g, int x, int y, int w, int h, float frac, int fill) {
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
