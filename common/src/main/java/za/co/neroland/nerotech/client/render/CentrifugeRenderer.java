package za.co.neroland.nerotech.client.render;

import java.util.Map;
import java.util.WeakHashMap;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import za.co.neroland.nerotech.machine.NeroTechMachineBlock;
import za.co.neroland.nerotech.machine.chemistry.CentrifugeBlockEntity;

/**
 * Centrifuge renderer: an eight-sided band spinning round the drum. It runs up over two seconds,
 * coasts down over three, turns at quarter speed and wobbles half a pixel when overheating, and
 * stops where it is when the machine stalls. A centrifuge with another one upstream of it (a
 * cascade) also draws a light bar across the joint, bright while it works.
 *
 * <p>Reads only the synced readout and the cached cascade position. No player data.</p>
 */
public class CentrifugeRenderer implements BlockEntityRenderer<CentrifugeBlockEntity, CentrifugeRenderer.State> {

    /** The status lamp socket: the {@code status_socket} element of {@code models/block/centrifuge.json} (pixels). */
    private static final StatusLampRenderer.Socket SOCKET = StatusLampRenderer.Socket.model(1.0F, 12.0F, -0.5F, 3.0F, 14.0F, 0.0F);

    private static final Identifier BAND_TEX = MachineRenderHelper.texture("centrifuge_band");

    /** The band clears the drum's widest corner (5.83 px from the axis) at every angle. */
    private static final float BAND_R = 6.4F / 16.0F;
    private static final float BAND_Y0 = 6.0F / 16.0F;
    private static final float BAND_Y1 = 11.0F / 16.0F;
    private static final float DEGREES_PER_TICK = 24.0F;
    private static final float RUN_UP_TICKS = 40.0F;
    private static final float RUN_DOWN_TICKS = 60.0F;
    private static final float WOBBLE = 0.5F / 16.0F;

    private final Map<BlockEntity, Motion> motions = new WeakHashMap<>();

    public static class State extends BlockEntityRenderState {
        final StatusLampRenderer.Lamp lamp = new StatusLampRenderer.Lamp();
        Direction facing = Direction.NORTH;
        float angle;
        float wobble;
        boolean linked;
    }

    /**
     * Motion that runs up and down instead of snapping, kept per block entity by the renderer and
     * advanced by game time, so it does not depend on the frame rate. Shared by the chemistry
     * renderers.
     */
    static final class Motion {
        /** Distance travelled, in ticks at full speed. */
        float phase;
        /** Current speed, 0..1 (also usable as a plain eased level). */
        float speed;
        private float last = Float.NaN;

        /** Move {@link #speed} towards {@code target} and advance {@link #phase}. */
        void advance(float time, float target, float upTicks, float downTicks) {
            float dt = Float.isNaN(this.last) ? 0.0F : time - this.last;
            this.last = time;
            if (dt < 0.0F) {
                dt += 2400.0F; // StatusLampRenderer wraps its clock every 2400 ticks
            }
            if (dt > 40.0F) {
                // Not drawn for a while: take up the target at once.
                this.speed = target;
                return;
            }
            if (this.speed < target) {
                this.speed = Math.min(target, this.speed + dt / upTicks);
            } else if (this.speed > target) {
                this.speed = Math.max(target, this.speed - dt / downTicks);
            }
            this.phase = (this.phase + this.speed * dt) % 3600.0F;
        }

        static Motion of(Map<BlockEntity, Motion> motions, BlockEntity blockEntity) {
            return motions.computeIfAbsent(blockEntity, key -> new Motion());
        }
    }

    /** Rotate about the vertical axis (one place for the version-specific call). */
    static void spinY(PoseStack poseStack, float degrees) {
        //? if >=26.3 {
        /*poseStack.rotate(Axis.YP.rotationDegrees(degrees));
        *///?} else {
        poseStack.mulPose(Axis.YP.rotationDegrees(degrees));
        //?}
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    /** One-block pad: the cascade bar and the heat shimmer leave the block. */
    public AABB getRenderBoundingBox(CentrifugeBlockEntity centrifuge) {
        return new AABB(centrifuge.getBlockPos()).inflate(1.0);
    }

    @Override
    public void extractRenderState(CentrifugeBlockEntity centrifuge, State state, float partialTick,
            Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(centrifuge, state, partialTick, cameraPos, breakProgress);
        state.facing = centrifuge.getBlockState().getValue(NeroTechMachineBlock.FACING);
        StatusLampRenderer.extract(centrifuge, state.lamp, partialTick);
        // Cached on the block entity (re-checked every 100 ticks), so this is not a world scan.
        state.linked = centrifuge.getLevel() != null && centrifuge.cascadePosition() > 1;
        if (!state.lamp.animate) {
            state.angle = 0.0F;
            state.wobble = 0.0F;
            return;
        }
        Motion motion = Motion.of(this.motions, centrifuge);
        motion.advance(state.lamp.time, StatusLampRenderer.motionScale(state.lamp), RUN_UP_TICKS, RUN_DOWN_TICKS);
        state.angle = (motion.phase * DEGREES_PER_TICK) % 360.0F;
        state.wobble = StatusLampRenderer.hot(state.lamp) ? WOBBLE * (float) Math.sin(state.lamp.time * 0.9F) : 0.0F;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector,
            CameraRenderState cameraState) {
        int light = MachineRenderHelper.FULL_BRIGHT;
        poseStack.pushPose();
        MachineRenderHelper.rotateToFacing(poseStack, state.facing);
        StatusLampRenderer.submit(state.lamp, poseStack, collector, SOCKET);
        StatusLampRenderer.submitShimmer(state.lamp, poseStack, collector,
                4.0F / 16.0F, 4.0F / 16.0F, 12.0F / 16.0F, 12.0F / 16.0F, 1.0F);

        if (state.linked) {
            // Upstream is on the low-x side of a north-facing model: a bar across that joint.
            int rgb = state.lamp.readout == StatusLampRenderer.WORK
                    ? MachineRenderHelper.PLASMA_RGB : MachineRenderHelper.TEAL_RGB;
            collector.order(2).submitCustomGeometry(poseStack,
                    RenderTypes.entityCutout(MachineRenderHelper.LAMP_TEX),
                    (pose, c) -> MachineRenderHelper.box(c, pose, light, rgb,
                            -3.0F / 16.0F, 8.25F / 16.0F, 7.25F / 16.0F,
                            3.0F / 16.0F, 9.75F / 16.0F, 8.75F / 16.0F));
        }

        int bandRgb = StatusLampRenderer.hot(state.lamp)
                ? MachineRenderHelper.lerpColor(0.5F, 0xFFFFFF,
                        MachineRenderHelper.heatColor(StatusLampRenderer.heatFraction(state.lamp)))
                : 0xFFFFFF;
        poseStack.translate(0.5F + state.wobble, 0.0F, 0.5F);
        spinY(poseStack, state.angle);
        collector.order(1).submitCustomGeometry(poseStack, RenderTypes.entityCutout(BAND_TEX),
                (pose, c) -> band(c, pose, light, bandRgb));
        poseStack.popPose();
    }

    /** Eight quads round the vertical axis, each carrying the whole band sprite. */
    private static void band(VertexConsumer c, PoseStack.Pose pose, int light, int rgb) {
        for (int i = 0; i < 8; i++) {
            double a0 = Math.PI / 8.0 + i * Math.PI / 4.0;
            double a1 = a0 + Math.PI / 4.0;
            float xa = BAND_R * (float) Math.cos(a0);
            float za = BAND_R * (float) Math.sin(a0);
            float xb = BAND_R * (float) Math.cos(a1);
            float zb = BAND_R * (float) Math.sin(a1);
            float nx = (float) Math.cos((a0 + a1) / 2.0);
            float nz = (float) Math.sin((a0 + a1) / 2.0);
            MachineRenderHelper.face(c, pose, light, rgb, nx, 0, nz,
                    xa, BAND_Y0, za, 0, 1, xa, BAND_Y1, za, 0, 0,
                    xb, BAND_Y1, zb, 1, 0, xb, BAND_Y0, zb, 1, 1);
        }
    }
}
