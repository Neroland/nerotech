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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import za.co.neroland.nerotech.machine.NeroTechMachineBlock;
import za.co.neroland.nerotech.machine.heat.HeatExchangerBlockEntity;

/**
 * Heat Exchanger renderer: chevrons travelling along the top rail and the two side rails in the
 * direction heat is moving (towards the back while the machine heats the tank, towards the front
 * while the tank heats the machine), and fin edges that take the heat colour as the block warms.
 * No flow means no chevrons, which together with the lamp's double blink is the "no tank" tell.
 *
 * <p>Reads only the synced flow direction, readout and heat bucket. No player data.</p>
 */
public class HeatExchangerRenderer
        implements BlockEntityRenderer<HeatExchangerBlockEntity, HeatExchangerRenderer.State> {

    /** The status lamp socket: the {@code status_socket} element of {@code models/block/heat_exchanger.json} (pixels). */
    private static final StatusLampRenderer.Socket SOCKET = StatusLampRenderer.Socket.model(1.0F, 12.0F, -0.5F, 3.0F, 14.0F, 0.0F);

    private static final Identifier CHEVRON_TEX = MachineRenderHelper.texture("heat_exchanger_chevron");

    private static final int CHEVRONS = 3;
    /** The chevrons run between the two manifolds (model z 1.5..14.5). */
    private static final float PATH_START = 1.5F / 16.0F;
    private static final float PATH_LENGTH = 13.0F / 16.0F;
    private static final float CHEVRON_HALF = 1.25F / 16.0F;
    /** Path lengths per tick; the lamp clock's 2400-tick wrap is a whole number of cycles. */
    private static final float SPEED = 1.0F / 60.0F;
    private static final float GAP = 0.004F;
    private static final float TOP_Y = 15.0F / 16.0F + GAP;
    private static final float TOP_X0 = 6.5F / 16.0F;
    private static final float TOP_X1 = 9.5F / 16.0F;
    private static final float SIDE_Y0 = 7.0F / 16.0F;
    private static final float SIDE_Y1 = 9.0F / 16.0F;
    private static final float WEST_X = 0.25F / 16.0F - GAP;
    private static final float EAST_X = 15.75F / 16.0F + GAP;
    /** Fins: seven plates, 1 px thick, every 2 px from model z 1.5, standing to model y 12. */
    private static final int FINS = 7;
    private static final int FIN_GLOW_FROM_BUCKET = 2;

    public static class State extends BlockEntityRenderState {
        final StatusLampRenderer.Lamp lamp = new StatusLampRenderer.Lamp();
        Direction facing = Direction.NORTH;
        /** +1 towards the back (machine to tank), -1 towards the front, 0 none. */
        int flow;
        /** 0..1 along one chevron spacing. */
        float travel;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    /** One-block pad: the heat shimmer stands above the block. */
    public AABB getRenderBoundingBox(HeatExchangerBlockEntity exchanger) {
        return new AABB(exchanger.getBlockPos()).inflate(1.0);
    }

    @Override
    public void extractRenderState(HeatExchangerBlockEntity exchanger, State state, float partialTick,
            Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(exchanger, state, partialTick, cameraPos, breakProgress);
        state.facing = exchanger.getBlockState().getValue(NeroTechMachineBlock.FACING);
        StatusLampRenderer.extract(exchanger, state.lamp, partialTick);
        // Flow is +1 machine-to-tank; the tank normally sits on the back face, so flip for a front tank.
        state.flow = exchanger.tankAtFront() ? -exchanger.flow() : exchanger.flow();
        if (!state.lamp.animate) {
            state.travel = 0.5F;
            return;
        }
        // HOT runs at a quarter speed; 2400 ticks hold a whole number of cycles at both speeds.
        float speed = StatusLampRenderer.hot(state.lamp) ? SPEED / 4.0F : SPEED;
        float t = state.lamp.time * speed * CHEVRONS;
        state.travel = t - (float) Math.floor(t);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector,
            CameraRenderState cameraState) {
        poseStack.pushPose();
        MachineRenderHelper.rotateToFacing(poseStack, state.facing);
        StatusLampRenderer.submit(state.lamp, poseStack, collector, SOCKET);
        StatusLampRenderer.submitShimmer(state.lamp, poseStack, collector, 1.0F);

        if (state.lamp.heatBucket >= FIN_GLOW_FROM_BUCKET) {
            int rgb = MachineRenderHelper.heatColor(StatusLampRenderer.heatFraction(state.lamp));
            collector.order(1).submitCustomGeometry(poseStack,
                    RenderTypes.entityCutout(MachineRenderHelper.LAMP_TEX),
                    (pose, c) -> finEdges(c, pose, rgb));
        }
        if (state.flow != 0) {
            int rgb = StatusLampRenderer.hot(state.lamp)
                    ? MachineRenderHelper.heatColor(StatusLampRenderer.heatFraction(state.lamp))
                    : MachineRenderHelper.PLASMA_RGB;
            int dir = state.flow > 0 ? 1 : -1;
            float travel = state.travel;
            collector.order(2).submitCustomGeometry(poseStack, RenderTypes.entityCutout(CHEVRON_TEX),
                    (pose, c) -> chevrons(c, pose, rgb, dir, travel));
        }
        poseStack.popPose();
    }

    /** A glowing cap over the top edge of every fin. */
    private static void finEdges(VertexConsumer c, PoseStack.Pose pose, int rgb) {
        for (int n = 0; n < FINS; n++) {
            float z0 = (1.5F + 2.0F * n) / 16.0F;
            MachineRenderHelper.box(c, pose, MachineRenderHelper.FULL_BRIGHT, rgb,
                    2.0F / 16.0F, 11.25F / 16.0F, z0 - GAP,
                    14.0F / 16.0F, 12.0F / 16.0F + GAP, z0 + 1.0F / 16.0F + GAP);
        }
    }

    /** Three chevrons on each of the three rails, all pointing the way the heat goes. */
    private static void chevrons(VertexConsumer c, PoseStack.Pose pose, int rgb, int dir, float travel) {
        int light = MachineRenderHelper.FULL_BRIGHT;
        float pitch = PATH_LENGTH / CHEVRONS;
        for (int i = 0; i < CHEVRONS; i++) {
            float along = (i + travel) * pitch;
            float centre = dir > 0 ? PATH_START + along : PATH_START + PATH_LENGTH - along;
            float head = centre + dir * CHEVRON_HALF;
            float tail = centre - dir * CHEVRON_HALF;
            // The sprite's chevron points at v = 0, so v = 0 sits on the leading edge.
            MachineRenderHelper.face(c, pose, light, rgb, 0, 1, 0,
                    TOP_X0, TOP_Y, tail, 0, 1, TOP_X0, TOP_Y, head, 0, 0,
                    TOP_X1, TOP_Y, head, 1, 0, TOP_X1, TOP_Y, tail, 1, 1);
            MachineRenderHelper.face(c, pose, light, rgb, -1, 0, 0,
                    WEST_X, SIDE_Y0, tail, 0, 1, WEST_X, SIDE_Y0, head, 0, 0,
                    WEST_X, SIDE_Y1, head, 1, 0, WEST_X, SIDE_Y1, tail, 1, 1);
            MachineRenderHelper.face(c, pose, light, rgb, 1, 0, 0,
                    EAST_X, SIDE_Y0, tail, 0, 1, EAST_X, SIDE_Y0, head, 0, 0,
                    EAST_X, SIDE_Y1, head, 1, 0, EAST_X, SIDE_Y1, tail, 1, 1);
        }
    }
}
