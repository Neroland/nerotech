package za.co.neroland.nerotech.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * A sci-fi styled button for the Tech Guide screen (Nerospace's {@code SpaceButton} recipe in
 * NeroTech's teal): a dark recessed body with a glowing accent border (brighter on hover or when
 * {@linkplain #setSelected selected}) and centred text, drawn entirely in {@link #extractContents}
 * so it doesn't look like a vanilla button.
 */
public class TechButton extends Button {

    private final int accent;
    private boolean selected;

    public TechButton(int x, int y, int width, int height, Component message, int accent, OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        this.accent = accent;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();
        boolean hovered = isHoveredOrFocused() && this.active;

        int border = !this.active ? 0xFF262B33 : (this.selected || hovered ? this.accent : 0xFF2E4A5A);
        int body = !this.active ? 0xFF12161F : (this.selected ? 0xFF123042 : (hovered ? 0xFF16344A : 0xFF0C1E2B));

        extractor.fill(x, y, x + w, y + h, border);
        extractor.fill(x + 1, y + 1, x + w - 1, y + h - 1, body);
        extractor.fill(x + 1, y + 1, x + w - 1, y + 2, 0x22FFFFFF); // top sheen

        Font font = Minecraft.getInstance().font;
        int textColor = !this.active ? 0xFF6A7280 : (hovered || this.selected ? 0xFFFFFFFF : 0xFFCFE7FF);
        extractor.centeredText(font, getMessage(), x + w / 2, y + (h - 8) / 2, textColor);
    }
}
