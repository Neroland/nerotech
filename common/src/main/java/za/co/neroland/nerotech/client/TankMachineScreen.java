package za.co.neroland.nerotech.client;

import java.util.List;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import za.co.neroland.nerotech.menu.MachineMenu;

/**
 * {@link MachineScreen} plus the Stage C tank gauges: one extra vertical bar per synced fluid/gas
 * level, continuing the energy/heat gauge column to the right (x+32, then every 12px). Drawn with
 * the same {@code fill}-only recipe as the rest of the panel — no GUI texture asset.
 *
 * <p>Gauge order matches the machine's {@code extraData} indices; the colours identify the contents
 * (water blue, hydrogen pale plasma, oxygen teal).
 *
 * @param <T> the machine menu type
 */
public class TankMachineScreen<T extends MachineMenu> extends MachineScreen<T> {

    /** Water. */
    protected static final int WATER = 0xFF3A7FE0;
    /** Hydrogen (the pale plasma end of the NeroTech ramp). */
    protected static final int HYDROGEN = 0xFFCEFFFF;
    /** Oxygen (teal). */
    protected static final int OXYGEN = 0xFF4DD0E1;

    /** First tank gauge x-offset — clear of the energy (x+8) and heat (x+20) gauges. */
    private static final int TANK_X = 32;
    private static final int TANK_SPACING = 12;
    private static final int TANK_W = 10;
    private static final int TANK_H = 46;
    private static final int TANK_Y = 20;

    private final int[] colors;

    protected TankMachineScreen(T menu, Inventory playerInventory, Component title, int... colors) {
        super(menu, playerInventory, title);
        this.colors = colors;
    }

    @Override
    public void extractContents(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        super.extractContents(extractor, mouseX, mouseY, partialTick);
        for (int i = 0; i < this.colors.length; i++) {
            sampleTank(i, tankAmount(i));
            gauge(extractor, this.leftPos + TANK_X + i * TANK_SPACING, this.topPos + TANK_Y,
                    TANK_W, TANK_H, this.menu.extraFraction(i), this.colors[i]);
        }
    }

    /** Whether tank {@code i} holds a gas (everything but the water gauge). */
    protected boolean tankIsGas(int i) {
        return this.colors[i] != WATER;
    }

    /** Whether tank {@code i} is a product the machine makes (an output). Default: all inputs. */
    protected boolean tankIsOutput(int i) {
        return false;
    }

    /** The medium tank {@code i} holds, named from its fixed role colour. */
    protected Component tankName(int i) {
        int color = this.colors[i];
        if (color == WATER) {
            return Component.translatable("block.minecraft.water");
        }
        if (color == OXYGEN) {
            return Component.translatable("gas.nerospace.oxygen");
        }
        return Component.translatable("gas.nerotech.hydrogen");
    }

    /** Tank capacity in mB (the legacy tanks use the configured machine tank size). */
    protected long tankCapacity(int i) {
        return tankIsGas(i) ? this.menu.legacyGasCapacity() : this.menu.legacyFluidCapacity();
    }

    /** Tank amount in mB, from the synced permille gauge (0.1% resolution). */
    protected long tankAmount(int i) {
        return GaugeMath.fromPermille(Math.round(this.menu.extraFraction(i) * 1000F), tankCapacity(i));
    }

    @Override
    protected List<Component> gaugeTooltip(int mx, int my) {
        List<Component> lines = super.gaugeTooltip(mx, my);
        if (!lines.isEmpty()) {
            return lines;
        }
        for (int i = 0; i < this.colors.length; i++) {
            if (over(mx, my, TANK_X + i * TANK_SPACING, TANK_Y, TANK_W, TANK_H)) {
                GaugeTooltips.tank(lines, null, tankName(i), tankAmount(i), tankCapacity(i), tankIsGas(i),
                        tankIsOutput(i), tankWindow(i).perTick());
                return lines;
            }
        }
        return lines;
    }
}
