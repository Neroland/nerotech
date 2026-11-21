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
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import za.co.neroland.nerolandcore.gas.NeroGasStorage;

import za.co.neroland.nerotech.machine.NeroTechMachineBlock;
import za.co.neroland.nerotech.machine.chemistry.AtmosphericCondenserBlockEntity;

/**
 * Atmospheric Condenser renderer: a louvre rotor turning slowly in the intake bell, and droplets
 * running down the sight window, which is tinted with the gas being collected. Stalled (no
 * atmosphere is the usual cause) the louvres are shut: a flat lid closes the bell.
 *
 * <p>Reads only the synced readout and the gas tank as last synced. No player data.</p>
 */
public class AtmosphericCondenserRenderer
        implements BlockEntityRenderer<AtmosphericCondenserBlockEntity, AtmosphericCondenserRenderer.State> {

    /** The status lamp socket: the {@code status_socket} element of {@code models/block/atmospheric_condenser.json} (pixels). */
    private static final StatusLampRenderer.Socket SOCKET = StatusLampRenderer.Socket.model(1.0F, 12.0F, 1.0F, 3.0F, 14.0F, 1.5F);

    private static final Identifier LOUVRE_TEX = MachineRenderHelper.texture("atmospheric_condenser_louvre");
    private static final Identifier SHUTTER_TEX = MachineRenderHelper.texture("atmospheric_condenser_shutter");

    private static final float DEGREES_PER_TICK = 1.5F;
    private static final float RAMP_TICKS = 30.0F;
    /** Sight window on the bulb front (model x 6..10, y 3..6, face at z 3). */
    private static final float WIN_X0 = 6.0F / 16.0F;
    private static final float WIN_X1 = 10.0F / 16.0F;
    private static final float WIN_Y0 = 3.0F / 16.0F;
    private static final float WIN_Y1 = 6.0F / 16.0F;
    private static final float WIN_Z = 3.0F / 16.0F - 0.004F;
    private static final float DROP_W = 0.5F / 16.0F;
    private static final float DROP_H = 0.75F / 16.0F;

    private final Map<BlockEntity, CentrifugeRenderer.Motion> motions = new WeakHashMap<>();

    public static class State extends BlockEntityRenderState {
        final StatusLampRenderer.Lamp lamp = new StatusLampRenderer.Lamp();
        Direction facing = Direction.NORTH;
        float angle;
        boolean shut;
        boolean hasGas;
        int gasRgb;
        boolean dripping;
        float drip;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    /** One-block pad: the heat shimmer stands above the bell. */
    public AABB getRenderBoundingBox(AtmosphericCondenserBlockEntity condenser) {
        return new AABB(condenser.getBlockPos()).inflate(1.0);
    }

    @Override
    public void extractRenderState(AtmosphericCondenserBlockEntity condenser, State state, float partialTick,
            Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(condenser, state, partialTick, cameraPos, breakProgress);
        state.facing = condenser.getBlockState().getValue(NeroTechMachineBlock.FACING);
        StatusLampRenderer.extract(condenser, state.lamp, partialTick);
        // C1 extra: the louvres close in a vacuum (and on any stall) and turn at the draw rate.
        state.shut = StatusLampRenderer.stalled(state.lamp) || !condenser.renderAtmosphere().condensable();
        NeroGasStorage gas = condenser.gasStorage(null);
        state.hasGas = gas != null && gas.getAmount() > 0L && gas.getGas() != null;
        state.gasRgb = state.hasGas ? ProcessTankRenderer.mediumColor(gas.getGas().getPath()) : 0;
        if (!state.lamp.animate) {
            state.angle = 0.0F;
            state.dripping = false;
            return;
        }
        CentrifugeRenderer.Motion motion = CentrifugeRenderer.Motion.of(this.motions, condenser);
        motion.advance(state.lamp.time, StatusLampRenderer.motionScale(state.lamp) * condenser.renderDrawRate(),
                RAMP_TICKS, RAMP_TICKS);
        state.angle = (motion.phase * DEGREES_PER_TICK) % 360.0F;
        state.dripping = StatusLampRenderer.moving(state.lamp);
        state.drip = (motion.phase * 0.03F) % 1.0F;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector,
            CameraRenderState cameraState) {
        int light = MachineRenderHelper.FULL_BRIGHT;
        poseStack.pushPose();
        MachineRenderHelper.rotateToFacing(poseStack, state.facing);
        StatusLampRenderer.submit(state.lamp, poseStack, collector, SOCKET);
        StatusLampRenderer.submitShimmer(state.lamp, poseStack, collector, 1.0F);

        if (state.hasGas) {
            int tint = MachineRenderHelper.lerpColor(0.45F, state.gasRgb, 0x08282F);
            collector.order(1).submitCustomGeometry(poseStack,
                    RenderTypes.entityCutout(MachineRenderHelper.LAMP_TEX),
                    (pose, c) -> MachineRenderHelper.lampQuad(c, pose, tint, WIN_X0, WIN_Y0, WIN_X1, WIN_Y1, WIN_Z));
        }
        if (state.dripping) {
            int rgb = state.hasGas ? MachineRenderHelper.lerpColor(0.5F, state.gasRgb, 0xFFFFFF) : 0xCFEFF5;
            float drip = state.drip;
            collector.order(2).submitCustomGeometry(poseStack,
                    RenderTypes.entityCutout(MachineRenderHelper.LAMP_TEX), (pose, c) -> {
                        for (int i = 0; i < 3; i++) {
                            float f = (drip + i * 0.37F) % 1.0F;
                            float x = WIN_X0 + (0.75F + i * 1.25F) / 16.0F;
                            float y = WIN_Y1 - DROP_H - f * (WIN_Y1 - WIN_Y0 - DROP_H);
                            MachineRenderHelper.lampQuad(c, pose, rgb, x, y, x + DROP_W, y + DROP_H, WIN_Z - 0.002F);
                        }
                    });
        }

        if (state.shut) {
            collector.order(1).submitCustomGeometry(poseStack, RenderTypes.entityCutout(SHUTTER_TEX),
                    (pose, c) -> MachineRenderHelper.box(c, pose, light,
                            3.0F / 16.0F, 15.0F / 16.0F, 3.0F / 16.0F,
                            13.0F / 16.0F, 15.5F / 16.0F, 13.0F / 16.0F));
        } else {
            poseStack.translate(0.5F, 0.0F, 0.5F);
            CentrifugeRenderer.spinY(poseStack, state.angle);
            collector.order(1).submitCustomGeometry(poseStack, RenderTypes.entityCutout(LOUVRE_TEX), (pose, c) -> {
                MachineRenderHelper.box(c, pose, light, -4.75F / 16.0F, 14.25F / 16.0F, -0.75F / 16.0F,
                        4.75F / 16.0F, 15.0F / 16.0F, 0.75F / 16.0F);
                MachineRenderHelper.box(c, pose, light, -0.75F / 16.0F, 14.25F / 16.0F, -4.75F / 16.0F,
                        0.75F / 16.0F, 15.0F / 16.0F, 4.75F / 16.0F);
            });
        }
        poseStack.popPose();
    }
}
