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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import za.co.neroland.nerotech.machine.NeroTechMachineBlock;
import za.co.neroland.nerotech.machine.metallurgy.PlasmaCrucibleBlockEntity;

/**
 * Plasma Crucible renderer: the melt surface inside the open pot and the plasma arc between the three
 * electrode tips, plus the shared status lamp and heat shimmer.
 *
 * <ul>
 *   <li><b>REST</b> — no arc. A crucible that still holds heat shows a low, dull melt (it keeps its
 *       temperature between jobs); a cold one shows the bare pot floor.</li>
 *   <li><b>WORK</b> — the arc jitters between the tips (a new shape each game tick) and the melt
 *       rises with progress, full-bright in its heat colour.</li>
 *   <li><b>STALL</b> — arc out, melt held at its level but dull.</li>
 *   <li><b>HOT</b> — the melt runs to the critical heat colour, the arc changes shape at a quarter
 *       of the rate, and the shimmer stands over the electrodes.</li>
 * </ul>
 *
 * <p>With {@code renderAnimationsEnabled=false} the arc is one fixed shape and nothing blinks. Reads
 * only the machine's synced render surface (readout, heat bucket, progress); no player data.</p>
 */
public class PlasmaCrucibleRenderer
        implements BlockEntityRenderer<PlasmaCrucibleBlockEntity, PlasmaCrucibleRenderer.State> {

    /** The status lamp socket: the {@code status_socket} element of {@code models/block/plasma_crucible.json} (pixels). */
    private static final StatusLampRenderer.Socket SOCKET = StatusLampRenderer.Socket.model(1.0F, 12.0F, -0.5F, 3.0F, 14.0F, 0.0F);

    private static final Identifier MELT_TEX = MachineRenderHelper.texture("plasma_crucible_melt");
    private static final Identifier ARC_TEX = MachineRenderHelper.texture("plasma_crucible_arc");

    private static final float PX = 1.0F / 16.0F;
    /** Pot interior (the model's walls are 2 px thick round x/z 3..13). */
    private static final float POT_MIN = 5.0F * PX;
    private static final float POT_MAX = 11.0F * PX;
    /** Melt surface height: just above the pot floor up to just under the rim. */
    private static final float MELT_LOW = 4.2F * PX;
    private static final float MELT_RISE = 7.0F * PX;
    /** Fill of a warm crucible between jobs, and the range a running job covers. */
    private static final float HELD_FILL = 0.25F;
    private static final float WORK_FILL_MIN = 0.3F;
    private static final float WORK_FILL_SPAN = 0.6F;
    /** How far a resting or stalled melt is pulled towards dark slag. */
    private static final float DULL = 0.55F;
    private static final int SLAG_RGB = 0x101418;

    /** Electrode tips (model space, facing north): front pair and the back mast. */
    private static final float[][] TIPS = {
            {5.5F * PX, 14.0F * PX, 4.5F * PX},
            {10.5F * PX, 14.0F * PX, 4.5F * PX},
            {8.0F * PX, 14.0F * PX, 11.0F * PX},
    };
    /** Where the three arcs meet, above the middle of the pot. */
    private static final float[] FOCUS = {8.0F * PX, 13.2F * PX, 7.4F * PX};
    private static final float ARC_HALF_WIDTH = 0.8F * PX;
    private static final float ARC_JITTER = 1.2F * PX;
    /** In HOT the arc changes shape every fourth tick (motion at a quarter speed). */
    private static final long HOT_ARC_HOLD_TICKS = 4L;

    public static class State extends BlockEntityRenderState {
        final StatusLampRenderer.Lamp lamp = new StatusLampRenderer.Lamp();
        Direction facing = Direction.NORTH;
        float progress;
        long tick;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    /** One-block pad for the shimmer above the masts; NeoForge frustum hook, unused elsewhere. */
    public AABB getRenderBoundingBox(PlasmaCrucibleBlockEntity crucible) {
        return new AABB(crucible.getBlockPos()).inflate(1.0);
    }

    @Override
    public void extractRenderState(PlasmaCrucibleBlockEntity crucible, State state, float partialTick,
            Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(crucible, state, partialTick, cameraPos, breakProgress);
        state.facing = crucible.getBlockState().getValue(NeroTechMachineBlock.FACING);
        StatusLampRenderer.extract(crucible, state.lamp, partialTick);
        state.progress = crucible.progressFraction(partialTick);
        Level level = crucible.getLevel();
        if (level == null) {
            state.tick = 0L;
            return;
        }
        state.tick = level.getGameTime();
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector,
            CameraRenderState cameraState) {
        StatusLampRenderer.Lamp lamp = state.lamp;
        boolean firing = lamp.readout == StatusLampRenderer.WORK || lamp.readout == StatusLampRenderer.HOT;
        poseStack.pushPose();
        MachineRenderHelper.rotateToFacing(poseStack, state.facing);

        // Melt surface: bright while firing, dull while held or stalled, absent in a cold idle pot.
        boolean stalled = StatusLampRenderer.stalled(lamp);
        if (firing || lamp.heatBucket >= 1 || (stalled && state.progress > 0.0F)) {
            float fill = firing || stalled ? WORK_FILL_MIN + WORK_FILL_SPAN * state.progress : HELD_FILL;
            float y = MELT_LOW + MELT_RISE * fill;
            int heat = MachineRenderHelper.heatColor(StatusLampRenderer.heatFraction(lamp));
            int rgb = firing ? heat : MachineRenderHelper.lerpColor(DULL, heat, SLAG_RGB);
            int light = firing ? MachineRenderHelper.FULL_BRIGHT : state.lightCoords;
            collector.order(1).submitCustomGeometry(poseStack, RenderTypes.entityCutout(MELT_TEX),
                    (pose, c) -> MachineRenderHelper.face(c, pose, light, rgb, 0, 1, 0,
                            POT_MIN, y, POT_MIN, 0, 0, POT_MIN, y, POT_MAX, 0, 1,
                            POT_MAX, y, POT_MAX, 1, 1, POT_MAX, y, POT_MIN, 1, 0));
        }

        // The arc: three legs from the tips to a shared focus that wanders each tick.
        if (firing && !lamp.dark) {
            long seed = !lamp.animate ? 0L
                    : lamp.readout == StatusLampRenderer.HOT ? state.tick / HOT_ARC_HOLD_TICKS : state.tick;
            float jitter = lamp.animate ? ARC_JITTER : 0.0F;
            float fx = FOCUS[0] + jitter * noise(seed, 1);
            float fy = FOCUS[1] + jitter * 0.6F * noise(seed, 2);
            float fz = FOCUS[2] + jitter * noise(seed, 3);
            int rgb = lamp.animate && (seed & 1L) == 0L ? 0xFFFFFF : MachineRenderHelper.PLASMA_RGB;
            collector.order(2).submitCustomGeometry(poseStack, RenderTypes.entityCutout(ARC_TEX),
                    (pose, c) -> {
                        for (float[] tip : TIPS) {
                            arcLeg(c, pose, rgb, tip[0], tip[1], tip[2], fx, fy, fz);
                        }
                    });
        }

        StatusLampRenderer.submitShimmer(lamp, poseStack, collector, 1.0F);
        StatusLampRenderer.submit(lamp, poseStack, collector, SOCKET);
        poseStack.popPose();
    }

    /** One arc leg as two crossed full-bright ribbons, so it shows from the side and from above. */
    private static void arcLeg(VertexConsumer c, PoseStack.Pose pose, int rgb,
            float tx, float ty, float tz, float fx, float fy, float fz) {
        int light = MachineRenderHelper.FULL_BRIGHT;
        float w = ARC_HALF_WIDTH;
        // upright ribbon
        MachineRenderHelper.face(c, pose, light, rgb, 0, 0, -1,
                tx, ty - w, tz, 0, 1, tx, ty + w, tz, 0, 0,
                fx, fy + w, fz, 1, 0, fx, fy - w, fz, 1, 1);
        // flat ribbon, widened across the leg's horizontal direction
        float dx = fx - tx;
        float dz = fz - tz;
        float length = (float) Math.sqrt(dx * dx + dz * dz);
        if (length < 1.0E-4F) {
            return;
        }
        float px = -dz / length * w;
        float pz = dx / length * w;
        MachineRenderHelper.face(c, pose, light, rgb, 0, 1, 0,
                tx - px, ty, tz - pz, 0, 1, tx + px, ty, tz + pz, 0, 0,
                fx + px, fy, fz + pz, 1, 0, fx - px, fy, fz - pz, 1, 1);
    }

    /** Deterministic noise in -1..1 from a tick seed and a channel (no per-frame randomness). */
    private static float noise(long seed, int channel) {
        long h = seed * 0x9E3779B97F4A7C15L + channel * 0xC2B2AE3D27D4EB4FL;
        h ^= h >>> 29;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 32;
        return ((h & 0xFFFFL) / 32767.5F) - 1.0F;
    }
}
