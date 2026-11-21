package za.co.neroland.nerotech.machine.fusion.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import za.co.neroland.nerotech.client.GaugeTooltips;
import za.co.neroland.nerotech.client.MachineScreen;
import za.co.neroland.nerotech.machine.ProcessingMachineBlockEntity;
import za.co.neroland.nerotech.machine.fusion.FuelInjectorMenu;

/**
 * Fuel Injector screen ({@code screens/fuel_injector.md}): a labelled deuterium gauge ("D", blue) and a
 * helium-3 gauge ("He", rose) with the 2 : 1 ratio between them, amount tooltips, the heat trough, and
 * a status line that names the cause. No energy trough: the injector draws no power. The letters carry
 * the meaning, so the two couplings never rest on hue alone.
 */
public class FuelInjectorScreen extends MachineScreen<FuelInjectorMenu> {

    /** Deuterium: the blue coupling. */
    private static final int DEUTERIUM = 0xFF5C8FE6;
    /** Helium-3: the rose coupling (art direction gas colour 244, 188, 200). */
    private static final int HELIUM_3 = 0xFFF4BCC8;
    private static final int HEAT = 0xFFE0543A;
    private static final int LABEL = 0xFFD6ECFF;
    private static final int OK = 0xFF7FD67F;
    private static final int STALL = 0xFFE0B33A;

    private static final int GAUGE_Y = 22;
    private static final int GAUGE_W = 10;
    private static final int GAUGE_H = 28;
    private static final int HEAT_X = 8;
    private static final int D_X = 44;
    private static final int HE_X = 84;
    private static final int STATUS_Y = 62;

    public FuelInjectorScreen(FuelInjectorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected boolean drawsDefaultGauges() {
        return false;
    }

    @Override
    protected boolean drawsDefaultWorkBar() {
        return false;
    }

    private long amount(int hiIndex) {
        return ProcessingMachineBlockEntity.join(this.menu.extraValue(hiIndex), this.menu.extraValue(hiIndex + 1));
    }

    private boolean charged() {
        return this.menu.extraValue(6) != 0;
    }

    @Override
    public void extractContents(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        super.extractContents(extractor, mouseX, mouseY, partialTick);
        int x = this.leftPos;
        int y = this.topPos;
        sampleTank(0, amount(2));
        sampleTank(1, amount(4));
        gauge(extractor, x + HEAT_X, y + GAUGE_Y, GAUGE_W, GAUGE_H, this.menu.heatFraction(), HEAT);
        gauge(extractor, x + D_X, y + GAUGE_Y, GAUGE_W, GAUGE_H, this.menu.extraFraction(0), DEUTERIUM);
        gauge(extractor, x + HE_X, y + GAUGE_Y, GAUGE_W, GAUGE_H, this.menu.extraFraction(1), HELIUM_3);
        centred(extractor, Component.translatable("gui.nerotech.fuel_injector.gauge.deuterium"),
                x + D_X + GAUGE_W / 2, y + GAUGE_Y + GAUGE_H + 2, DEUTERIUM);
        centred(extractor, Component.translatable("gui.nerotech.fuel_injector.gauge.helium_3"),
                x + HE_X + GAUGE_W / 2, y + GAUGE_Y + GAUGE_H + 2, HELIUM_3);
        centred(extractor, Component.translatable("gui.nerotech.fuel_injector.ratio"),
                x + (D_X + HE_X + GAUGE_W) / 2, y + GAUGE_Y + GAUGE_H / 2 - 4, LABEL);
        Component status = Component.translatable(charged()
                ? "gui.nerotech.fuel_injector.status.ready" : "gui.nerotech.fuel_injector.status.needs_both");
        extractor.text(this.font, status, x + 8, y + STATUS_Y, charged() ? OK : STALL, false);
        // The tooltip is emitted by MachineScreen from gaugeTooltip (once, after every part).
    }

    @Override
    protected List<Component> gaugeTooltip(int mx, int my) {
        return tooltip(mx, my);
    }

    private void centred(GuiGraphicsExtractor extractor, Component text, int cx, int ty, int color) {
        extractor.text(this.font, text, cx - this.font.width(text) / 2, ty, color, false);
    }

    private static boolean inside(int mx, int my, int gx) {
        return mx >= gx - 1 && mx <= gx + GAUGE_W && my >= GAUGE_Y - 1 && my <= GAUGE_Y + GAUGE_H;
    }

    private List<Component> tooltip(int mx, int my) {
        List<Component> lines = new ArrayList<>();
        long capacity = amount(7);
        if (inside(mx, my, D_X)) {
            GaugeTooltips.tank(lines, null, Component.translatable("gas.nerotech.deuterium"), amount(2), capacity,
                    true, false, tankWindow(0).perTick());
        } else if (inside(mx, my, HE_X)) {
            GaugeTooltips.tank(lines, null, Component.translatable("gas.nerotech.helium_3"), amount(4), capacity,
                    true, false, tankWindow(1).perTick());
        } else if (inside(mx, my, HEAT_X)) {
            heatTooltip(lines);
        } else if (mx >= D_X + GAUGE_W && mx < HE_X && my >= GAUGE_Y && my < GAUGE_Y + GAUGE_H) {
            lines.add(Component.translatable("gui.nerotech.fuel_injector.ratio.tooltip"));
        }
        return lines;
    }
}
