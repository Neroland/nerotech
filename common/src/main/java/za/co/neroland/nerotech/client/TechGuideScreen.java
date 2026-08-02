package za.co.neroland.nerotech.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

import za.co.neroland.nerotech.NeroTechCommon;
import za.co.neroland.nerotech.guide.TechGuide;
import za.co.neroland.nerotech.guide.TechGuideMenu;

/**
 * The Tech Guide screen (Nerospace's Star Guide layout in NeroTech's teal): a chapter rail on the
 * left, the selected chapter's step nodes connected by a dotted circuit-trace line on the right, and
 * a guide-text panel underneath. Completed steps light up; completed-but-unseen steps pulse until
 * clicked once (which reports "seen" through the menu button).
 *
 * <p>Backed by a 240x200 hull panel in a 256x256 sheet at
 * {@code assets/nerotech/textures/gui/tech_guide.png} (painted by tools/gen_textures.py) — the one
 * NeroTech screen with a texture asset, mirroring how the Star Guide authors its background.</p>
 */
public class TechGuideScreen extends AbstractContainerScreen<TechGuideMenu> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(NeroTechCommon.MOD_ID, "textures/gui/tech_guide.png");
    /** Tech Guide accent: NeroTech cyan (MODELS.md T_CYAN). */
    private static final int ACCENT = 0xFF24D0DE;
    private static final int DONE = 0xFF58D08A;
    private static final int TITLE = 0xFFD6ECFF;

    private final List<TechButton> chapterButtons = new ArrayList<>();
    private final List<TechButton> stepButtons = new ArrayList<>();
    private int selectedChapter;
    private int selectedStep;

    public TechGuideScreen(TechGuideMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 240, 200);
        this.titleLabelX = 10;
        this.inventoryLabelY = 10_000; // no player inventory on this panel
    }

    @Override
    protected void init() {
        super.init();
        this.chapterButtons.clear();
        for (int i = 0; i < TechGuide.CHAPTER_COUNT; i++) {
            final int chapter = i;
            TechButton button = new TechButton(this.leftPos + 8, this.topPos + 22 + i * 17, 66, 14,
                    Component.translatable(TechGuide.CHAPTERS.get(i).titleKey()), ACCENT,
                    b -> selectChapter(chapter));
            this.addRenderableWidget(button);
            this.chapterButtons.add(button);
        }
        rebuildStepButtons();
    }

    private void selectChapter(int chapter) {
        this.selectedChapter = chapter;
        this.selectedStep = 0;
        rebuildStepButtons();
    }

    private void rebuildStepButtons() {
        this.stepButtons.forEach(this::removeWidget);
        this.stepButtons.clear();
        List<TechGuide.Step> steps = TechGuide.CHAPTERS.get(this.selectedChapter).steps();
        for (int i = 0; i < steps.size(); i++) {
            final int step = i;
            TechButton node = new TechButton(stepX(i), stepY(i), 42, 14,
                    Component.literal(String.valueOf(i + 1)), ACCENT, b -> selectStep(step));
            this.addRenderableWidget(node);
            this.stepButtons.add(node);
        }
    }

    /** Serpentine layout: odd rows run right-to-left so the path snakes without crossing nodes. */
    private int stepX(int index) {
        int col = index % 3;
        if ((index / 3) % 2 == 1) {
            col = 2 - col;
        }
        return this.leftPos + 82 + col * 52;
    }

    private int stepY(int index) {
        return this.topPos + 26 + (index / 3) * rowSpacing();
    }

    /**
     * Vertical pitch between node rows, compressed for long chapters so every row stays inside the
     * step canvas.
     */
    private int rowSpacing() {
        int steps = TechGuide.CHAPTERS.get(this.selectedChapter).steps().size();
        int rows = Math.max(1, (steps + 2) / 3);
        return rows <= 1 ? 32 : Math.min(32, 54 / (rows - 1));
    }

    private void selectStep(int step) {
        this.selectedStep = step;
        // Report "seen" so the completed-pulse stops (server writes the TechGuideSeenState store).
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(
                    this.menu.containerId, this.selectedChapter * 16 + step);
        }
    }

    @Override
    public void extractContents(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        // The hull panel (top-left 240x200 of the 256x256 sheet), then widgets, then the foreground.
        extractor.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.leftPos, this.topPos,
                0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
        super.extractContents(extractor, mouseX, mouseY, partialTick);
        extractForeground(extractor);
    }

    private void extractForeground(GuiGraphicsExtractor g) {
        List<TechGuide.Step> steps = TechGuide.CHAPTERS.get(this.selectedChapter).steps();

        // Chapter rail: light fully-completed chapters.
        for (int i = 0; i < this.chapterButtons.size(); i++) {
            int total = TechGuide.CHAPTERS.get(i).steps().size();
            boolean allDone = Integer.bitCount(this.menu.completionMask(i)) >= total;
            this.chapterButtons.get(i).setSelected(i == this.selectedChapter || allDone);
        }

        // Dotted circuit-trace line linking the chapter's nodes in order (the progression path).
        for (int i = 0; i < steps.size() - 1; i++) {
            boolean done = this.menu.isStepComplete(this.selectedChapter, i);
            int color = done ? DONE : 0xFF31506B;
            if (i / 3 == (i + 1) / 3) {
                int xa = Math.min(stepX(i), stepX(i + 1)) + 44;
                int xb = Math.max(stepX(i), stepX(i + 1)) - 2;
                dottedLine(g, xa, stepY(i) + 7, xb, stepY(i) + 7, color);
            } else {
                int cx = stepX(i) + 21; // same column as the next node (serpentine turn)
                dottedLine(g, cx, stepY(i) + 15, cx, stepY(i + 1) - 1, color);
            }
        }

        // Step nodes: completed steps lit (steady once seen, pulsing until clicked once).
        long now = System.currentTimeMillis();
        boolean pulseOn = (now / 400L) % 2L == 0L;
        for (int i = 0; i < this.stepButtons.size(); i++) {
            boolean done = this.menu.isStepComplete(this.selectedChapter, i);
            boolean seen = this.menu.isStepSeen(this.selectedChapter, i);
            TechButton node = this.stepButtons.get(i);
            node.setSelected(done && (seen || pulseOn));
            if (done) {
                g.fill(node.getX() + node.getWidth() - 5, node.getY() + 2, // completion pip
                        node.getX() + node.getWidth() - 2, node.getY() + 5, DONE);
            }
        }

        // Guide-text panel for the selected step.
        TechGuide.Step step = steps.get(Math.min(this.selectedStep, steps.size() - 1));
        boolean stepDone = this.menu.isStepComplete(this.selectedChapter, this.selectedStep);
        Component title = Component.translatable(step.titleKey());
        g.text(this.font, title, this.leftPos + 82, this.topPos + 100,
                stepDone ? DONE : 0xFFD2F4FF, false);
        if (stepDone) {
            Component complete = Component.translatable("gui.nerotech.tech_guide.complete");
            int tagX = 230 - this.font.width(complete);
            if (tagX >= 82 + this.font.width(title) + 6) {
                g.text(this.font, complete, this.leftPos + tagX, this.topPos + 100, DONE, false);
            }
        }
        // Description: wrapped to the text panel.
        List<FormattedCharSequence> lines = this.font.split(
                Component.translatable(step.textKey()), 146);
        int lineHeight = lines.size() > 8 ? 9 : 10;
        int y = 112;
        for (FormattedCharSequence line : lines) {
            if (y + this.font.lineHeight > 193) {
                break;
            }
            g.text(this.font, line, this.leftPos + 82, this.topPos + y, 0xFFB6C6D8, false);
            y += lineHeight;
        }
    }

    /** A dotted 2px line between two points (axis-aligned or otherwise), ~5px pitch. */
    private static void dottedLine(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int color) {
        int steps = Math.max(1, (int) (Math.hypot(x1 - x0, y1 - y0) / 5.0D));
        for (int s = 0; s <= steps; s++) {
            float t = s / (float) steps;
            int ax = Math.round(Mth.lerp(t, x0, x1));
            int ay = Math.round(Mth.lerp(t, y0, y1));
            g.fill(ax, ay, ax + 2, ay + 2, color);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor extractor, int mouseX, int mouseY) {
        extractor.text(this.font, this.title, this.titleLabelX, this.titleLabelY, TITLE, false);
        // No inventory label: the panel has no player slots.
    }
}
