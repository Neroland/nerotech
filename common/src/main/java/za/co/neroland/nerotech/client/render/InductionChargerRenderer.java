package za.co.neroland.nerotech.client.render;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import za.co.neroland.nerotech.machine.NeroTechMachineBlock;
import za.co.neroland.nerotech.machine.utility.InductionChargerBlockEntity;

/**
 * Induction Charger renderer: the three coil rings lighting outward in sequence and the faint field
 * column over the post while the pad is charging something, plus the shared status lamp, which on
 * this block sits in a socket on the centre post rather than the standard front-face position.
 *
 * <p>An idle pad draws only its lamp: rings dark, no column. While the synced {@code charging()}
 * flag is set, a light runs from the inner ring to the outer one and fades behind itself, and a
 * dithered column rises from the post to a block and a half up. In the HOT readout the post cap
 * takes the heat colour and shimmers. With {@code renderAnimationsEnabled=false} a charging pad
 * shows all three rings steadily lit and no column.</p>
 *
 * <p>Reads only the machine's synced render surface. The charger keeps no player data and this
 * renderer never looks for players.</p>
 */
public class InductionChargerRenderer
        implements BlockEntityRenderer<InductionChargerBlockEntity, InductionChargerRenderer.State> {

    private static final Identifier[] RING_TEX = {
            MachineRenderHelper.texture("induction_charger_ring_0"),
            MachineRenderHelper.texture("induction_charger_ring_1"),
            MachineRenderHelper.texture("induction_charger_ring_2"),
    };
    private static final Identifier FIELD_TEX = MachineRenderHelper.texture("induction_charger_field");

    private static final float PX = 1.0F / 16.0F;
    private static final float GAP = 0.004F;
    /** Ring tops in the model, inner ring first. */
    private static final float[] RING_Y = {4.5F * PX + GAP, 4.0F * PX + GAP, 3.5F * PX + GAP};
    /** Centre post: footprint, top, and the lamp socket on its front face. */
    private static final float POST_MIN = 6.0F * PX;
    private static final float POST_MAX = 10.0F * PX;
    private static final float POST_TOP = 9.0F * PX;
    /** The {@code status_lamp_socket} element of {@code models/block/induction_charger.json} (pixels). */
    private static final StatusLampRenderer.Socket SOCKET = StatusLampRenderer.Socket.model(7.0F, 6.0F, 5.5F, 9.0F, 8.0F, 6.0F);
    /** Field column: from the post top to model y 24. */
    private static final float FIELD_MIN = 5.0F * PX;
    private static final float FIELD_MAX = 11.0F * PX;
    private static final float FIELD_HEIGHT = 15.0F * PX;
    private static final float FIELD_SPEED = 0.025F;

    /** Ticks for the light to run inner ring, middle ring, outer ring. */
    private static final float CHASE_TICKS = 24.0F;
    /** How fast a ring fades once the light has passed it (1 = over the whole cycle). */
    private static final float RING_FADE = 1.8F;

    public static class State extends BlockEntityRenderState {
        final StatusLampRenderer.Lamp lamp = new StatusLampRenderer.Lamp();
        Direction facing = Direction.NORTH;
        /** Ring brightness 0..1, inner ring first. */
        final float[] ring = new float[3];
        boolean field;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    /** The field column rises half a block past the top; NeoForge frustum hook, unused elsewhere. */
    public AABB getRenderBoundingBox(InductionChargerBlockEntity charger) {
        return new AABB(charger.getBlockPos()).inflate(1.0);
    }

    @Override
    public void extractRenderState(InductionChargerBlockEntity charger, State state, float partialTick,
            Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(charger, state, partialTick, cameraPos, breakProgress);
        state.facing = charger.getBlockState().getValue(NeroTechMachineBlock.FACING);
        StatusLampRenderer.extract(charger, state.lamp, partialTick);

        boolean charging = charger.charging() && !state.lamp.dark;
        state.field = charging && state.lamp.animate;
        for (int k = 0; k < state.ring.length; k++) {
            if (!charging) {
                state.ring[k] = 0.0F;
            } else if (!state.lamp.animate) {
                state.ring[k] = 1.0F; // static frame: all rings lit
            } else {
                float phase = state.lamp.time / CHASE_TICKS - k / 3.0F;
                phase -= Mth.floor(phase);
                state.ring[k] = Math.max(0.0F, 1.0F - phase * RING_FADE);
            }
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector,
            CameraRenderState cameraState) {
        poseStack.pushPose();
        MachineRenderHelper.rotateToFacing(poseStack, state.facing);

        for (int k = 0; k < state.ring.length; k++) {
            float level = state.ring[k];
            if (level <= 0.02F) {
                continue;
            }
            int rgb = MachineRenderHelper.lerpColor(level, MachineRenderHelper.TEAL_RGB,
                    MachineRenderHelper.PLASMA_RGB);
            float y = RING_Y[k];
            collector.order(1).submitCustomGeometry(poseStack, RenderTypes.entityCutout(RING_TEX[k]),
                    (pose, c) -> MachineRenderHelper.face(c, pose, MachineRenderHelper.FULL_BRIGHT, rgb, 0, 1, 0,
                            0.0F, y, 0.0F, 0, 0, 0.0F, y, 1.0F, 0, 1,
                            1.0F, y, 1.0F, 1, 1, 1.0F, y, 0.0F, 1, 0));
        }

        if (state.field) {
            float scroll = state.lamp.time * FIELD_SPEED;
            collector.order(2).submitCustomGeometry(poseStack, RenderTypes.entityCutout(FIELD_TEX),
                    (pose, c) -> MachineRenderHelper.shimmerQuads(c, pose, MachineRenderHelper.CYAN_RGB,
                            FIELD_MIN, FIELD_MIN, FIELD_MAX, FIELD_MAX, POST_TOP, FIELD_HEIGHT, scroll));
        }

        if (StatusLampRenderer.hot(state.lamp)) {
            int rgb = MachineRenderHelper.heatColor(StatusLampRenderer.heatFraction(state.lamp));
            float y = POST_TOP + GAP;
            collector.order(2).submitCustomGeometry(poseStack,
                    RenderTypes.entityCutout(MachineRenderHelper.LAMP_TEX),
                    (pose, c) -> MachineRenderHelper.face(c, pose, MachineRenderHelper.FULL_BRIGHT, rgb, 0, 1, 0,
                            POST_MIN, y, POST_MIN, 0, 0, POST_MIN, y, POST_MAX, 0, 1,
                            POST_MAX, y, POST_MAX, 1, 1, POST_MAX, y, POST_MIN, 1, 0));
            StatusLampRenderer.submitShimmer(state.lamp, poseStack, collector, POST_MIN, POST_MIN, POST_MAX,
                    POST_MAX, POST_TOP);
        }

        StatusLampRenderer.submit(state.lamp, poseStack, collector, SOCKET);
        poseStack.popPose();
    }
}
