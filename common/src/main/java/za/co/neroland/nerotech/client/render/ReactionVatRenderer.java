package za.co.neroland.nerotech.client.render;

import java.util.Map;
import java.util.WeakHashMap;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import za.co.neroland.nerolandcore.fluid.NeroFluidStorage;

import za.co.neroland.nerotech.machine.NeroTechMachineBlock;
import za.co.neroland.nerotech.machine.chemistry.ReactionVatBlockEntity;

/**
 * Reaction Vat renderer, seen through the glass front: the contents in the colour of the fluid
 * the vat holds (the product once there is some), an agitator paddle turning in them and a
 * bubbling surface. Stalled, the paddle stops where it is and the contents settle flat;
 * overheating, the bubbles turn violent and the contents take a warm cast.
 *
 * <p>Reads only the synced readout and the fluid tanks as last synced. No player data.</p>
 */
public class ReactionVatRenderer implements BlockEntityRenderer<ReactionVatBlockEntity, ReactionVatRenderer.State> {

    /** The status lamp socket: the {@code status_socket} element of {@code models/block/reaction_vat.json} (pixels). */
    private static final StatusLampRenderer.Socket SOCKET = StatusLampRenderer.Socket.model(1.0F, 12.0F, 2.5F, 3.0F, 14.0F, 3.0F);

    private static final Identifier PADDLE_TEX = MachineRenderHelper.texture("reaction_vat_paddle");
    private static final Identifier BUBBLES_TEX = MachineRenderHelper.texture("reaction_vat_bubbles");

    /** Vessel interior (model space): x 2..14, y 3..11.5, z 4..12. */
    private static final float IN_X0 = 2.1F / 16.0F;
    private static final float IN_X1 = 13.9F / 16.0F;
    private static final float IN_Z0 = 4.1F / 16.0F;
    private static final float IN_Z1 = 11.9F / 16.0F;
    private static final float IN_Y0 = 3.0F / 16.0F;
    private static final float IN_HEIGHT = 8.0F / 16.0F;
    private static final float DEGREES_PER_TICK = 12.0F;
    private static final float RAMP_TICKS = 20.0F;
    /** Contents shown for a working vat whose tanks have not synced yet. */
    private static final int UNKNOWN_RGB = 0x4DB6AC;

    private final Map<BlockEntity, CentrifugeRenderer.Motion> motions = new WeakHashMap<>();

    public static class State extends BlockEntityRenderState {
        final StatusLampRenderer.Lamp lamp = new StatusLampRenderer.Lamp();
        Direction facing = Direction.NORTH;
        float angle;
        float fill;
        int rgb;
        boolean bubbling;
        int bubbleStep;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    /** One-block pad: the heat shimmer stands above the block. */
    public AABB getRenderBoundingBox(ReactionVatBlockEntity vat) {
        return new AABB(vat.getBlockPos()).inflate(1.0);
    }

    @Override
    public void extractRenderState(ReactionVatBlockEntity vat, State state, float partialTick,
            Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(vat, state, partialTick, cameraPos, breakProgress);
        state.facing = vat.getBlockState().getValue(NeroTechMachineBlock.FACING);
        StatusLampRenderer.extract(vat, state.lamp, partialTick);

        NeroFluidStorage fluids = vat.fluidStorage(null);
        Fluid fluid = fluids == null ? Fluids.EMPTY : fluids.getFluid();
        long amount = fluids == null ? 0L : fluids.getAmount();
        long capacity = fluids == null ? 0L : fluids.getCapacity();
        boolean working = state.lamp.readout == StatusLampRenderer.WORK;
        if (amount > 0L && capacity > 0L && fluid != null && fluid != Fluids.EMPTY) {
            state.fill = Mth.clamp(0.2F + 0.8F * amount / (float) capacity, 0.0F, 1.0F);
            state.rgb = ProcessTankRenderer.mediumColor(BuiltInRegistries.FLUID.getKey(fluid).getPath());
        } else if (working) {
            state.fill = 0.4F;
            state.rgb = UNKNOWN_RGB;
        } else {
            state.fill = 0.0F;
            state.rgb = UNKNOWN_RGB;
        }
        boolean hot = StatusLampRenderer.hot(state.lamp);
        if (hot) {
            state.rgb = MachineRenderHelper.lerpColor(0.35F, state.rgb,
                    MachineRenderHelper.heatColor(StatusLampRenderer.heatFraction(state.lamp)));
        }
        if (!state.lamp.animate) {
            state.angle = 0.0F;
            state.bubbling = false;
            return;
        }
        CentrifugeRenderer.Motion motion = CentrifugeRenderer.Motion.of(this.motions, vat);
        motion.advance(state.lamp.time, StatusLampRenderer.motionScale(state.lamp), RAMP_TICKS, RAMP_TICKS);
        state.angle = (motion.phase * DEGREES_PER_TICK) % 360.0F;
        state.bubbling = state.fill > 0.0F && (working || hot);
        state.bubbleStep = (int) (state.lamp.time / (hot ? 2.0F : 6.0F)) & 3;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector,
            CameraRenderState cameraState) {
        int light = MachineRenderHelper.FULL_BRIGHT;
        poseStack.pushPose();
        MachineRenderHelper.rotateToFacing(poseStack, state.facing);
        StatusLampRenderer.submit(state.lamp, poseStack, collector, SOCKET);
        StatusLampRenderer.submitShimmer(state.lamp, poseStack, collector, 1.0F);

        float surface = IN_Y0 + IN_HEIGHT * state.fill;
        if (state.fill > 0.0F) {
            int rgb = state.rgb;
            collector.order(1).submitCustomGeometry(poseStack,
                    RenderTypes.entityCutout(MachineRenderHelper.FLUID_TEX),
                    (pose, c) -> MachineRenderHelper.fluidColumn(c, pose, light, rgb,
                            IN_X0, IN_Y0, IN_Z0, IN_X1, surface, IN_Z1, 0.0F));
        }

        // Shaft from the motor box, then the blade turning about it.
        collector.order(2).submitCustomGeometry(poseStack, RenderTypes.entityCutout(PADDLE_TEX),
                (pose, c) -> MachineRenderHelper.box(c, pose, light, 7.5F / 16.0F, 4.0F / 16.0F, 7.5F / 16.0F,
                        8.5F / 16.0F, 12.0F / 16.0F, 8.5F / 16.0F));
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.pushPose();
        CentrifugeRenderer.spinY(poseStack, state.angle);
        collector.order(2).submitCustomGeometry(poseStack, RenderTypes.entityCutout(PADDLE_TEX),
                (pose, c) -> MachineRenderHelper.box(c, pose, light, -3.25F / 16.0F, 4.0F / 16.0F, -0.5F / 16.0F,
                        3.25F / 16.0F, 6.5F / 16.0F, 0.5F / 16.0F));
        poseStack.popPose();

        if (state.bubbling) {
            // The ring sprite turned a quarter each step reads as bubbles forming and bursting.
            int rgb = MachineRenderHelper.lerpColor(0.6F, state.rgb, 0xFFFFFF);
            float half = 3.75F / 16.0F;
            float y = surface + 0.004F;
            boolean violent = StatusLampRenderer.hot(state.lamp);
            poseStack.pushPose();
            CentrifugeRenderer.spinY(poseStack, 90.0F * state.bubbleStep);
            collector.order(3).submitCustomGeometry(poseStack, RenderTypes.entityCutout(BUBBLES_TEX), (pose, c) -> {
                MachineRenderHelper.face(c, pose, light, rgb, 0, 1, 0,
                        -half, y, -half, 0, 0, -half, y, half, 0, 1, half, y, half, 1, 1, half, y, -half, 1, 0);
                if (violent) {
                    float y2 = y + 0.75F / 16.0F;
                    MachineRenderHelper.face(c, pose, light, rgb, 0, 1, 0,
                            -half, y2, -half, 1, 1, -half, y2, half, 1, 0, half, y2, half, 0, 0, half, y2, -half, 0, 1);
                }
            });
            poseStack.popPose();
        }
        poseStack.popPose();
    }
}
