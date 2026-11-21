package za.co.neroland.nerotech.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import za.co.neroland.nerotech.machine.NeroTechMachineBlock;
import za.co.neroland.nerotech.machine.components.ComponentForgeBlockEntity;

/**
 * Component Forge BER: two manipulator stalks on the back-corner shoulders. At REST they stand
 * folded as short stubs; in WORK they reach in over the pedestal and take turns touching it while a
 * wireframe hologram of the part grows above the pedestal and the fluid line under the bed lights
 * up. STALL holds the stalks extended exactly where they stopped (a stalled forge never looks
 * idle); HOT folds them away and shimmers the bed.
 *
 * <p>The touch rhythm is driven by the synced {@code progressFraction}, not by wall time, so it
 * speeds up, slows down and freezes with the job itself. The fold/unfold is eased once per game
 * tick on the block entity's display fields and lerped by partial tick (FPS-independent).
 * {@code renderAnimationsEnabled=false} draws the folded static frame. Reads only the synced render
 * surface; no player data (POPIA/GDPR n/a).</p>
 */
public class ComponentForgeRenderer
        implements BlockEntityRenderer<ComponentForgeBlockEntity, ComponentForgeRenderer.State> {

    /** The status lamp socket: the {@code status_lamp_socket} element of {@code models/block/component_forge.json} (pixels). */
    private static final StatusLampRenderer.Socket SOCKET = StatusLampRenderer.Socket.model(1.0F, 12.0F, -0.5F, 3.0F, 14.0F, 0.0F);

    private static final Identifier ARM_TEX = MachineRenderHelper.texture("component_forge_arm");
    private static final Identifier HOLO_TEX = MachineRenderHelper.texture("component_forge_holo");

    private static final float PX = 1.0F / 16.0F;
    /** Shoulder pivots: the tops of the two model shoulders (model px). */
    private static final float SHOULDER_Y = 13.0F * PX;
    private static final float SHOULDER_Z = 14.0F * PX;
    private static final float SHOULDER_WEST_X = 3.0F * PX;
    private static final float SHOULDER_EAST_X = 13.0F * PX;
    /** Folded stub height above the shoulder. */
    private static final float FOLDED = 2.5F * PX;
    /** Hover and touch points over the pedestal (pedestal top is y 11). */
    private static final float HOVER_Y = 13.5F * PX;
    private static final float TOUCH_Y = 11.4F * PX;
    private static final float HOVER_OFF = 1.6F * PX;
    private static final float TOUCH_OFF = 0.7F * PX;
    private static final float CENTRE = 8.0F * PX;
    private static final float ARM_HALF = 0.45F * PX;
    /** Touches per stalk per operation. */
    private static final float TOUCHES = 4.0F;
    private static final double EASE = 0.25;
    /** The body front is set back one pixel under the bed lip. */
    private static final float BODY_FRONT_Z = 1.0F * PX - 0.004F;

    public static class State extends BlockEntityRenderState {
        final StatusLampRenderer.Lamp lamp = new StatusLampRenderer.Lamp();
        Direction facing = Direction.NORTH;
        float extend;
        float progress;
        /** The part being assembled (C1 extra {@code currentResult()}), drawn inside the wireframe. */
        final ItemStackRenderState result = new ItemStackRenderState();
        boolean hasResult;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    /** One-block pad; NeoForge-only frustum hook, inert non-override on Fabric/Forge. */
    public AABB getRenderBoundingBox(ComponentForgeBlockEntity forge) {
        return new AABB(forge.getBlockPos()).inflate(1.0);
    }

    @Override
    public void extractRenderState(ComponentForgeBlockEntity forge, State state, float partialTick,
            Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(forge, state, partialTick, cameraPos, breakProgress);
        StatusLampRenderer.extract(forge, state.lamp, partialTick);
        state.facing = forge.getBlockState().getValue(NeroTechMachineBlock.FACING);
        state.progress = forge.progressFraction(partialTick);
        Level level = forge.getLevel();
        ItemStack result = forge.currentResult();
        state.hasResult = level != null && !result.isEmpty();
        if (state.hasResult) {
            //? if >=1.21.9 {
            Minecraft.getInstance().getItemModelResolver().updateForTopItem(
                    state.result, result, ItemDisplayContext.GROUND, level, null,
                    (int) forge.getBlockPos().asLong());
            //?} else {
            /*state.result.update(result, ItemDisplayContext.GROUND, level, (int) forge.getBlockPos().asLong());
            *///?}
        }
        if (level == null || !state.lamp.animate) {
            state.extend = 0.0F; // static folded frame
            return;
        }
        // WORK and STALL hold the stalks out; REST and HOT fold them.
        double target = state.lamp.readout == StatusLampRenderer.WORK
                || state.lamp.readout == StatusLampRenderer.STALL ? 1.0 : 0.0;
        long tick = level.getGameTime();
        if (!forge.displayInit) {
            forge.displayPos = forge.prevDisplayPos = target;
            forge.displayInit = true;
            forge.displayLastTick = tick;
        } else if (tick != forge.displayLastTick) {
            forge.displayLastTick = tick;
            forge.prevDisplayPos = forge.displayPos;
            forge.displayPos += (target - forge.displayPos) * EASE;
        }
        state.extend = (float) (forge.prevDisplayPos
                + (forge.displayPos - forge.prevDisplayPos) * partialTick);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector,
            CameraRenderState cameraState) {
        poseStack.pushPose();
        MachineRenderHelper.rotateToFacing(poseStack, state.facing);
        StatusLampRenderer.submit(state.lamp, poseStack, collector, SOCKET);
        StatusLampRenderer.submitShimmer(state.lamp, poseStack, collector, 8.0F * PX);

        float extend = state.extend;
        // The two stalks take turns: one dips on the first half of each cycle, the other on the second.
        float wave = Mth.sin(state.progress * TOUCHES * Mth.TWO_PI);
        stalk(poseStack, collector, SHOULDER_WEST_X, -1.0F, extend, Math.max(0.0F, wave));
        stalk(poseStack, collector, SHOULDER_EAST_X, 1.0F, extend, Math.max(0.0F, -wave));

        boolean working = state.lamp.readout == StatusLampRenderer.WORK;
        boolean job = state.lamp.readout != StatusLampRenderer.REST && state.progress > 0.03F
                && state.lamp.animate;
        if (working) {
            // Fluid line under the bed: the painted conduit brightens while a job draws on it.
            float glide = 0.5F + 0.5F * Mth.sin(state.lamp.time * Mth.TWO_PI / 40.0F);
            int rgb = MachineRenderHelper.lerpColor(glide, MachineRenderHelper.CYAN_RGB,
                    MachineRenderHelper.PLASMA_RGB);
            collector.order(1).submitCustomGeometry(poseStack,
                    RenderTypes.entityCutout(MachineRenderHelper.FLUID_TEX),
                    (pose, c) -> MachineRenderHelper.lampQuad(c, pose, rgb,
                            4.0F * PX, 4.0F * PX, 12.5F * PX, 4.5F * PX, BODY_FRONT_Z));
        }
        if (job) {
            // Result hologram: grows and brightens over the pedestal with progress.
            float p = state.progress;
            float r = (0.9F + 1.8F * p) * PX;
            int rgb = MachineRenderHelper.lerpColor(p, MachineRenderHelper.TEAL_RGB,
                    MachineRenderHelper.PLASMA_RGB);
            float spin = working ? state.lamp.time * 3.0F * Mth.DEG_TO_RAD : 0.6F;
            poseStack.pushPose();
            poseStack.translate(CENTRE, 11.6F * PX + r, CENTRE);
            rotate(poseStack, Axis.YP, spin);
            collector.order(2).submitCustomGeometry(poseStack, RenderTypes.entityCutout(HOLO_TEX),
                    (pose, c) -> {
                        MachineRenderHelper.face(c, pose, MachineRenderHelper.FULL_BRIGHT, rgb, 0, 0, -1,
                                -r, -r, 0, 0, 1, -r, r, 0, 0, 0, r, r, 0, 1, 0, r, -r, 0, 1, 1);
                        MachineRenderHelper.face(c, pose, MachineRenderHelper.FULL_BRIGHT, rgb, -1, 0, 0,
                                0, -r, -r, 0, 1, 0, r, -r, 0, 0, 0, r, r, 1, 0, 0, -r, r, 1, 1);
                    });
            if (state.hasResult) {
                // The real part, growing inside the wireframe with progress.
                float s = 0.12F + 0.22F * p;
                poseStack.translate(0.0F, -0.25F * s, 0.0F);
                poseStack.scale(s, s, s);
                state.result.submit(poseStack, collector, MachineRenderHelper.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
            }
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    /**
     * One stalk: a boom from its shoulder to the tool tip. {@code side} is -1 for the west stalk and
     * +1 for the east one; {@code extend} 0 = folded stub, 1 = reaching over the pedestal;
     * {@code dip} 0 = hovering, 1 = touching.
     */
    private static void stalk(PoseStack poseStack, SubmitNodeCollector collector, float shoulderX,
            float side, float extend, float dip) {
        float reachX = CENTRE + side * Mth.lerp(dip, HOVER_OFF, TOUCH_OFF);
        float reachY = Mth.lerp(dip, HOVER_Y, TOUCH_Y);
        float tipX = Mth.lerp(extend, shoulderX, reachX);
        float tipY = Mth.lerp(extend, SHOULDER_Y + FOLDED, reachY);
        float tipZ = Mth.lerp(extend, SHOULDER_Z, CENTRE);
        float dx = tipX - shoulderX;
        float dy = tipY - SHOULDER_Y;
        float dz = tipZ - SHOULDER_Z;
        float length = Mth.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < 1.0E-4F) {
            return;
        }
        float yaw = (float) Math.atan2(dx, dz);
        float pitch = (float) Math.acos(Mth.clamp(dy / length, -1.0F, 1.0F));
        poseStack.pushPose();
        poseStack.translate(shoulderX, SHOULDER_Y, SHOULDER_Z);
        rotate(poseStack, Axis.YP, yaw);
        rotate(poseStack, Axis.XP, pitch);
        collector.order(1).submitCustomGeometry(poseStack, RenderTypes.entityCutout(ARM_TEX),
                (pose, c) -> boom(c, pose, length));
        poseStack.popPose();
    }

    /** The boom along local +Y: a knuckle at the shoulder, the shaft, and the tool head at the tip. */
    private static void boom(VertexConsumer c, PoseStack.Pose pose, float length) {
        int light = MachineRenderHelper.FULL_BRIGHT;
        float k = ARM_HALF * 1.7F;
        MachineRenderHelper.box(c, pose, light, -k, -k, -k, k, k, k);
        MachineRenderHelper.box(c, pose, light, -ARM_HALF, 0.0F, -ARM_HALF, ARM_HALF, length, ARM_HALF);
        MachineRenderHelper.box(c, pose, light, -k, length - 1.2F * PX, -k, k, length, k);
    }

    private static void rotate(PoseStack poseStack, Axis axis, float radians) {
        //? if >=26.3 {
        /*poseStack.rotate(axis.rotation(radians));
        *///?} else {
        poseStack.mulPose(axis.rotation(radians));
        //?}
    }
}
