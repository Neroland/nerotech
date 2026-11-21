package za.co.neroland.nerotech.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import za.co.neroland.nerotech.config.NeroTechConfig;
import za.co.neroland.nerotech.machine.NeroTechMachineBlock;
import za.co.neroland.nerotech.machine.utility.AnnunciatorPanelBlockEntity;

/**
 * Annunciator Panel renderer: lights the five alarm lamps and, with each lamp, the glyph engraved
 * under it (flame = overheat, empty hopper = starved, full crate = output full, cloud = polluting,
 * cracked gear = failing; left to right in the alarm-bit order).
 *
 * <p>A lamp is steady while its alarm is active and blinks (5 ticks on, 5 off) while the alarm is
 * new, from the synced {@code alarmMask()} and {@code freshMask()}. Every lamp uses the one warning
 * colour: which alarm it is comes from the lamp's position and its lit glyph, never from hue. All
 * dark means healthy; the panel has no status lamp of its own. With
 * {@code renderAnimationsEnabled=false} nothing blinks and a new alarm is simply lit.</p>
 *
 * <p>All geometry lies on the panel face, inside the block, so no bounding-box widening is needed.
 * Reads machine alarm bits only; no player data.</p>
 */
public class AnnunciatorPanelRenderer
        implements BlockEntityRenderer<AnnunciatorPanelBlockEntity, AnnunciatorPanelRenderer.State> {

    private static final Identifier GLYPH_TEX = MachineRenderHelper.texture("annunciator_panel_glyphs");

    private static final int ALARMS = 5;
    private static final int WARN_RGB = 0xFFB238;
    private static final int BLINK_PERIOD = 10;

    /** Face pixels (32 across the block, top-left origin as you face the panel) to block units. */
    private static final float FACE_PX = 1.0F / 32.0F;
    private static final float GAP = 0.004F;
    /** Lenses stand half a model pixel proud of the panel face (model z 14). */
    private static final float LENS_Z = 13.5F / 16.0F - GAP;
    private static final float FACE_Z = 14.0F / 16.0F - GAP;
    /** Lens rectangles: x0, y0, x1, y1 in face pixels (x1/y1 exclusive). */
    private static final int LENS_Y0 = 6;
    private static final int LENS_Y1 = 10;
    private static final int LENS_X0 = 4;
    private static final int LENS_PITCH = 5;
    private static final int LENS_WIDTH = 4;
    /** Glyph rectangles in face pixels, one per alarm bit: x0, y0, x1, y1 (exclusive). */
    private static final int[][] GLYPH = {
            {4, 12, 10, 19},
            {8, 20, 15, 27},
            {13, 12, 19, 19},
            {17, 20, 24, 27},
            {22, 12, 28, 19},
    };

    public static class State extends BlockEntityRenderState {
        Direction facing = Direction.NORTH;
        /** Alarm bits to draw this frame (active, and not in the dark half of a blink). */
        int lit;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(AnnunciatorPanelBlockEntity panel, State state, float partialTick,
            Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(panel, state, partialTick, cameraPos, breakProgress);
        state.facing = panel.getBlockState().getValue(NeroTechMachineBlock.FACING);
        int mask = panel.alarmMask() & ((1 << ALARMS) - 1);
        int fresh = panel.freshMask() & mask;
        Level level = panel.getLevel();
        boolean blinkOff = fresh != 0 && level != null && NeroTechConfig.renderAnimationsEnabled()
                && level.getGameTime() % BLINK_PERIOD >= BLINK_PERIOD / 2;
        state.lit = blinkOff ? mask & ~fresh : mask;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector,
            CameraRenderState cameraState) {
        int lit = state.lit;
        if (lit == 0) {
            return; // healthy: nothing dynamic
        }
        poseStack.pushPose();
        MachineRenderHelper.rotateToFacing(poseStack, state.facing);

        collector.order(1).submitCustomGeometry(poseStack, RenderTypes.entityCutout(MachineRenderHelper.LAMP_TEX),
                (pose, c) -> {
                    for (int k = 0; k < ALARMS; k++) {
                        if ((lit >> k & 1) != 0) {
                            int x0 = LENS_X0 + LENS_PITCH * k;
                            MachineRenderHelper.lampQuad(c, pose, WARN_RGB, modelX(x0 + LENS_WIDTH), modelY(LENS_Y1),
                                    modelX(x0), modelY(LENS_Y0), LENS_Z);
                        }
                    }
                });
        collector.order(1).submitCustomGeometry(poseStack, RenderTypes.entityCutout(GLYPH_TEX), (pose, c) -> {
            for (int k = 0; k < ALARMS; k++) {
                if ((lit >> k & 1) != 0) {
                    int[] g = GLYPH[k];
                    glyphQuad(c, pose, g[0], g[1], g[2], g[3]);
                }
            }
        });
        poseStack.popPose();
    }

    /** Face pixel column to model x: the face is seen from the north, so pixel 0 is the high-x edge. */
    private static float modelX(int facePx) {
        return 1.0F - facePx * FACE_PX;
    }

    /** Face pixel row to model y: row 0 is the top. */
    private static float modelY(int facePx) {
        return 1.0F - facePx * FACE_PX;
    }

    /** A full-bright north-facing quad showing one rectangle of the glyph sheet in place. */
    private static void glyphQuad(VertexConsumer c, PoseStack.Pose pose, int px0, int py0, int px1, int py1) {
        float xLow = modelX(px1);
        float xHigh = modelX(px0);
        float yLow = modelY(py1);
        float yHigh = modelY(py0);
        float u0 = px0 * FACE_PX;
        float u1 = px1 * FACE_PX;
        float v0 = py0 * FACE_PX;
        float v1 = py1 * FACE_PX;
        MachineRenderHelper.face(c, pose, MachineRenderHelper.FULL_BRIGHT, WARN_RGB, 0, 0, -1,
                xLow, yLow, FACE_Z, u1, v1, xLow, yHigh, FACE_Z, u1, v0,
                xHigh, yHigh, FACE_Z, u0, v0, xHigh, yLow, FACE_Z, u0, v1);
    }
}
