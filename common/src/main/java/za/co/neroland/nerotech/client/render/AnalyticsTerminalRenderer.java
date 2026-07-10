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
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import za.co.neroland.nerotech.config.NeroTechConfig;
import za.co.neroland.nerotech.machine.AnalyticsTerminalBlockEntity;
import za.co.neroland.nerotech.machine.NeroTechMachineBlock;

/**
 * Analytics Terminal BER (MODELS.md Stage G): a faint holographic shimmer — two crossed vertical
 * quads reusing the {@code fusion_reactor_plasma} wisp at a dim teal tint — floating above the
 * console while the last scan found at least one machine (the synced active flag). Idle draws
 * nothing dynamic.
 *
 * <p>Sync discipline: extraction reads only the BE's synced render surface ({@code renderActive});
 * the drift is pure clock maths. {@code renderAnimationsEnabled=false} holds a static shimmer
 * frame while active, so low-end clients keep the "machines in range" read.</p>
 */
public class AnalyticsTerminalRenderer
        implements BlockEntityRenderer<AnalyticsTerminalBlockEntity, AnalyticsTerminalRenderer.State> {

    private static final Identifier SHIMMER_TEX = MachineRenderHelper.texture("fusion_reactor_plasma");

    /** Shimmer spin speed (degrees per tick) — the Remediator mist's lazy drift. */
    private static final float SHIMMER_SPIN_SPEED = 3.0F;
    /** Shimmer half-size and hover height above the console screen. */
    private static final float SHIMMER_R = 4.5F / 16.0F;
    private static final float SHIMMER_Y = 20.0F / 16.0F;

    /** Faint teal hologram tint (T_CYAN dimmed — "faint" per the MODELS.md row). */
    private static final int SHIMMER_RGB = (24 << 16) | (140 << 8) | 150;

    /** Render state: everything extracted from the BE + clocks, nothing read at submit time. */
    public static class State extends BlockEntityRenderState {
        boolean active;
        float spin;
        Direction facing = Direction.NORTH;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    /**
     * One-block pad (MODELS.md constraint): the shimmer floats above the block column. NeoForge-only
     * frustum hook; inert non-override on Fabric/Forge (Nerospace precedent — vanilla
     * {@code BlockEntityRenderer} has no such method).
     */
    public AABB getRenderBoundingBox(AnalyticsTerminalBlockEntity terminal) {
        return new AABB(terminal.getBlockPos()).inflate(1.0);
    }

    @Override
    public void extractRenderState(AnalyticsTerminalBlockEntity terminal, State state, float partialTick,
            Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(terminal, state, partialTick, cameraPos, breakProgress);
        Level level = terminal.getLevel();
        state.facing = terminal.getBlockState().getValue(NeroTechMachineBlock.FACING);
        // Explicit null check (not folded into the flag) so ecj's null-flow analysis can track it.
        if (level == null) {
            state.active = false;
            state.spin = 0.0F;
            return;
        }
        state.active = terminal.renderActive();
        if (state.active && NeroTechConfig.renderAnimationsEnabled()) {
            float now = level.getGameTime() + partialTick;
            state.spin = (now * SHIMMER_SPIN_SPEED) % 360.0F;
        } else {
            state.spin = 0.0F; // static shimmer frame while active with animations off
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector,
            CameraRenderState cameraState) {
        if (!state.active) {
            return; // idle console: no machines in range, no hologram
        }
        int light = MachineRenderHelper.FULL_BRIGHT;
        poseStack.pushPose();
        MachineRenderHelper.rotateToFacing(poseStack, state.facing);

        // The holographic shimmer: two crossed vertical quads slowly rotating above the console,
        // teal-dimmed so they read as projection light (the Remediator mist recipe, raised).
        poseStack.translate(0.5F, SHIMMER_Y, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.spin));
        collector.order(1).submitCustomGeometry(poseStack, RenderTypes.entityCutout(SHIMMER_TEX),
                (pose, c) -> {
                    MachineRenderHelper.face(c, pose, light, SHIMMER_RGB, 0, 0, -1,
                            -SHIMMER_R, -SHIMMER_R * 0.5F, 0, 0, 1,
                            -SHIMMER_R, SHIMMER_R * 0.5F, 0, 0, 0,
                            SHIMMER_R, SHIMMER_R * 0.5F, 0, 1, 0,
                            SHIMMER_R, -SHIMMER_R * 0.5F, 0, 1, 1);
                    MachineRenderHelper.face(c, pose, light, SHIMMER_RGB, -1, 0, 0,
                            0, -SHIMMER_R * 0.5F, -SHIMMER_R, 0, 1,
                            0, SHIMMER_R * 0.5F, -SHIMMER_R, 0, 0,
                            0, SHIMMER_R * 0.5F, SHIMMER_R, 1, 0,
                            0, -SHIMMER_R * 0.5F, SHIMMER_R, 1, 1);
                });
        poseStack.popPose();
    }
}
