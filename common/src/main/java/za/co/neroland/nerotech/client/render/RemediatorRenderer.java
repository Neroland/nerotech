package za.co.neroland.nerotech.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import za.co.neroland.nerotech.config.NeroTechConfig;
import za.co.neroland.nerotech.machine.NeroTechMachineBlock;
import za.co.neroland.nerotech.machine.RemediatorBlockEntity;

/**
 * Remediator BER (MODELS.md Stage F): two spray booms on the model's mount posts — BER-drawn, so
 * they are always visible, parked upright when idle and sweeping on a slow sine while a region is
 * being remediated — plus faint plasma mist quads drifting over the deck while active (the
 * {@code fusion_reactor_plasma} wisp reused at a teal tint). Idle draws just the parked booms.
 *
 * <p>Sync discipline: extraction reads only the BE's synced render surface ({@code renderActive});
 * the sweep/mist are pure clock maths. {@code renderAnimationsEnabled=false} parks the booms and
 * holds a static mist frame while active, so low-end clients keep the working read.</p>
 */
public class RemediatorRenderer
        implements BlockEntityRenderer<RemediatorBlockEntity, RemediatorRenderer.State> {

    private static final Identifier BOOM_TEX = MachineRenderHelper.texture("remediator_boom");
    private static final Identifier MIST_TEX = MachineRenderHelper.texture("fusion_reactor_plasma");

    /** Boom pivot height: the top of the model's mount posts (y12). */
    private static final float PIVOT_Y = 12.0F / 16.0F;
    /** Boom pivot x-centres (the two mount posts straddle the block centre). */
    private static final float PIVOT_WEST_X = 3.5F / 16.0F;
    private static final float PIVOT_EAST_X = 12.5F / 16.0F;
    private static final float PIVOT_Z = 8.0F / 16.0F;
    /** Boom quad extent above the pivot / half-width. */
    private static final float BOOM_LEN = 7.0F / 16.0F;
    private static final float BOOM_HALF_W = 1.0F / 16.0F;
    /** Max sweep deflection either side of vertical. */
    private static final float SWEEP_MAX = 35.0F;
    /** Sweep speed (radians per tick into the sine). */
    private static final float SWEEP_SPEED = 0.08F;
    /** Mist spin speed (degrees per tick) — a lazy drift, much slower than the fusion torus. */
    private static final float MIST_SPIN_SPEED = 4.0F;
    /** Mist half-size and hover height over the deck. */
    private static final float MIST_R = 5.5F / 16.0F;
    private static final float MIST_Y = 13.5F / 16.0F;

    /** Faint teal mist tint (T_CYAN dimmed — "faint" per the MODELS.md row). */
    private static final int MIST_RGB = (26 << 16) | (150 << 8) | 160;

    /** Render state: everything extracted from the BE + clocks, nothing read at submit time. */
    public static class State extends BlockEntityRenderState {
        boolean active;
        float sweep;
        float mistSpin;
        Direction facing = Direction.NORTH;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    /**
     * One-block pad (MODELS.md constraint): the booms sweep above the block column. NeoForge-only
     * frustum hook; inert non-override on Fabric/Forge (Nerospace precedent — vanilla
     * {@code BlockEntityRenderer} has no such method).
     */
    public AABB getRenderBoundingBox(RemediatorBlockEntity remediator) {
        return new AABB(remediator.getBlockPos()).inflate(1.0);
    }

    @Override
    public void extractRenderState(RemediatorBlockEntity remediator, State state, float partialTick,
            Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(remediator, state, partialTick, cameraPos, breakProgress);
        Level level = remediator.getLevel();
        state.facing = remediator.getBlockState().getValue(NeroTechMachineBlock.FACING);
        // Explicit null check (not folded into the flag) so ecj's null-flow analysis can track it.
        if (level == null) {
            state.active = false;
            state.sweep = 0.0F;
            state.mistSpin = 0.0F;
            return;
        }
        state.active = remediator.renderActive();
        if (state.active && NeroTechConfig.renderAnimationsEnabled()) {
            float now = level.getGameTime() + partialTick;
            state.sweep = SWEEP_MAX * Mth.sin(now * SWEEP_SPEED);
            state.mistSpin = (now * MIST_SPIN_SPEED) % 360.0F;
        } else {
            state.sweep = 0.0F;    // parked upright (idle / static frame)
            state.mistSpin = 0.0F; // static mist frame while active with animations off
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector,
            CameraRenderState cameraState) {
        int light = MachineRenderHelper.FULL_BRIGHT;
        poseStack.pushPose();
        MachineRenderHelper.rotateToFacing(poseStack, state.facing);

        // The two spray booms — always drawn (the model only ships the mount posts). They sweep
        // in mirrored antiphase so the pair reads as a sprinkler gantry, not a metronome.
        boomQuad(poseStack, collector, light, PIVOT_WEST_X, state.sweep);
        boomQuad(poseStack, collector, light, PIVOT_EAST_X, -state.sweep);

        // Faint plasma mist drifting over the deck while remediating: two crossed vertical quads
        // around the centre column, slowly rotating, teal-dimmed so they read as vapour.
        if (state.active) {
            poseStack.pushPose();
            poseStack.translate(0.5F, MIST_Y, 0.5F);
            poseStack.mulPose(Axis.YP.rotationDegrees(state.mistSpin));
            collector.order(2).submitCustomGeometry(poseStack, RenderTypes.entityCutout(MIST_TEX),
                    (pose, c) -> {
                        MachineRenderHelper.face(c, pose, light, MIST_RGB, 0, 0, -1,
                                -MIST_R, -MIST_R * 0.5F, 0, 0, 1,
                                -MIST_R, MIST_R * 0.5F, 0, 0, 0,
                                MIST_R, MIST_R * 0.5F, 0, 1, 0,
                                MIST_R, -MIST_R * 0.5F, 0, 1, 1);
                        MachineRenderHelper.face(c, pose, light, MIST_RGB, -1, 0, 0,
                                0, -MIST_R * 0.5F, -MIST_R, 0, 1,
                                0, MIST_R * 0.5F, -MIST_R, 0, 0,
                                0, MIST_R * 0.5F, MIST_R, 1, 0,
                                0, -MIST_R * 0.5F, MIST_R, 1, 1);
                    });
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    /** One boom arm quad pivoting about its mount top, deflected {@code angle}° from vertical. */
    private static void boomQuad(PoseStack poseStack, SubmitNodeCollector collector, int light,
            float pivotX, float angle) {
        poseStack.pushPose();
        poseStack.translate(pivotX, PIVOT_Y, PIVOT_Z);
        poseStack.mulPose(Axis.ZP.rotationDegrees(angle));
        collector.order(1).submitCustomGeometry(poseStack, RenderTypes.entityCutout(BOOM_TEX),
                (pose, c) -> MachineRenderHelper.face(c, pose, light, 0xFFFFFF, 0, 0, -1,
                        -BOOM_HALF_W, 0, 0, 0, 1,
                        -BOOM_HALF_W, BOOM_LEN, 0, 0, 0,
                        BOOM_HALF_W, BOOM_LEN, 0, 1, 0,
                        BOOM_HALF_W, 0, 0, 1, 1));
        poseStack.popPose();
    }
}
