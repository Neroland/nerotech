package za.co.neroland.nerotech.machine.utility;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import za.co.neroland.nerotech.client.ProcessingScreen;
import za.co.neroland.nerotech.machine.MachineAlarms;
import za.co.neroland.nerotech.menu.ProcessingMenu;

/**
 * Client only. The shared processing screen for the two utility blocks, plus what their screen specs add
 * from the utility menu extras ({@link UtilityMenus#X_UTILITY_A}/{@code B}): the Induction Charger's reach
 * and rate lines, and the Annunciator Panel's "all clear / N alarms lit / no machine touching" line.
 * Machine-scoped numbers only; no player data.
 */
public class UtilityScreen extends ProcessingScreen<ProcessingMenu> {

    private static final int LINE_COLOR = 0xFFA9B8C6;
    private static final int LINE_X = 66;
    private static final int REACH_Y = 34;
    private static final int RATE_Y = 44;

    private final boolean charger;

    public UtilityScreen(ProcessingMenu menu, Inventory playerInventory, Component title, boolean charger) {
        super(menu, playerInventory, title);
        this.charger = charger;
    }

    public static UtilityScreen charger(ProcessingMenu menu, Inventory playerInventory, Component title) {
        return new UtilityScreen(menu, playerInventory, title, true);
    }

    public static UtilityScreen panel(ProcessingMenu menu, Inventory playerInventory, Component title) {
        return new UtilityScreen(menu, playerInventory, title, false);
    }

    private int extraA() {
        return this.menu.extraValue(UtilityMenus.X_UTILITY_A);
    }

    private int extraB() {
        return this.menu.extraValue(UtilityMenus.X_UTILITY_B);
    }

    /** Annunciator rows: one glyph and label per alarm, lit or dim, so the lamp is named, not just counted. */
    private static final int ROW_X = 26;
    private static final int ROW_Y = 28;
    private static final int ROW_STEP = 8;
    private static final int LIT = 0xFFE0B33A;
    private static final int DIM = 0xFF3A4654;

    /** The panel draws no power; the charger does. */
    @Override
    protected boolean drawsEnergyTrough() {
        return this.charger;
    }

    /** The panel has no work to show; its alarm rows sit where the arrow would be. */
    @Override
    protected boolean drawsWorkArrow() {
        return this.charger;
    }

    @Override
    protected void extractMachine(GuiGraphicsExtractor extractor, int mouseX, int mouseY) {
        super.extractMachine(extractor, mouseX, mouseY);
        if (!this.charger) {
            extractAlarmRows(extractor);
            return;
        }
        int x = this.leftPos;
        int y = this.topPos;
        extractor.text(this.font, Component.translatable("gui.nerotech.induction_charger.reach", extraA()),
                x + LINE_X, y + REACH_Y, LINE_COLOR, false);
        extractor.text(this.font, Component.translatable("gui.nerotech.induction_charger.rate", extraB()),
                x + LINE_X, y + RATE_Y, LINE_COLOR, false);
    }

    private void extractAlarmRows(GuiGraphicsExtractor extractor) {
        int x = this.leftPos + ROW_X;
        int y = this.topPos + ROW_Y;
        boolean touching = extraA() > 0;
        int mask = extraB();
        for (int i = 0; i < MachineAlarms.COUNT; i++) {
            boolean lit = touching && MachineAlarms.has(mask, 1 << i);
            int ry = y + i * ROW_STEP;
            // Lit: a filled square; dim: an outline only. The shape, not just the colour, says which.
            extractor.fill(x, ry + 1, x + 6, ry + 7, lit ? LIT : DIM);
            if (!lit) {
                extractor.fill(x + 1, ry + 2, x + 5, ry + 6, 0xFF0B1119);
            }
            extractor.text(this.font, Component.translatable("gui.nerotech.annunciator_panel.alarm." + MachineAlarms.IDS[i]),
                    x + 10, ry, lit ? LIT : LINE_COLOR, false);
        }
    }

    @Override
    protected java.util.List<Component> tooltip(int mx, int my) {
        java.util.List<Component> lines = super.tooltip(mx, my);
        if (this.charger || !lines.isEmpty()) {
            return lines;
        }
        if (mx >= ROW_X && mx < ROW_X + 90 && my >= ROW_Y && my < ROW_Y + MachineAlarms.COUNT * ROW_STEP) {
            int i = (my - ROW_Y) / ROW_STEP;
            lines.add(Component.translatable("gui.nerotech.annunciator_panel.alarm." + MachineAlarms.IDS[i] + ".tooltip"));
        }
        return lines;
    }

    @Override
    protected Component statusLine() {
        if (this.charger) {
            int code = this.menu.detail();
            if (this.menu.status() != za.co.neroland.nerotech.machine.MachineStatus.RUNNING
                    && (code == InductionChargerBlockEntity.CODE_PLAYERS_OFF
                            || code == InductionChargerBlockEntity.CODE_NO_ITEM_CHARGERS)) {
                return Component.translatable("nerotech.machine.status_line",
                        Component.translatable("nerotech.machine.readout." + readout().name().toLowerCase(java.util.Locale.ROOT)),
                        Component.translatable(code == InductionChargerBlockEntity.CODE_PLAYERS_OFF
                                ? InductionChargerBlockEntity.DETAIL_PLAYERS_OFF
                                : InductionChargerBlockEntity.DETAIL_NO_ITEM_CHARGERS));
            }
            return super.statusLine();
        }
        if (extraA() <= 0) {
            return Component.translatable("gui.nerotech.annunciator_panel.none");
        }
        int lit = Integer.bitCount(extraB() & 0x1F);
        return lit == 0 ? Component.translatable("gui.nerotech.annunciator_panel.clear")
                : Component.translatable("gui.nerotech.annunciator_panel.count", lit);
    }
}
