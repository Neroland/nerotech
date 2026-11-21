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
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import za.co.neroland.nerotech.machine.NeroTechMachineBlock;
import za.co.neroland.nerotech.machine.fusion.FuelInjectorBlockEntity;

/**
 * Fuel Injector renderer: the two gas coupling rings on the front (blue deuterium at the viewer's
 * left, rose helium-3 at the right), the nozzle-tip glow on the reactor side, and the shared status
 * lamp and heat shimmer.
 *
 * <p>Each charge the reactor draws bumps the injector's synced pulse counter; the rings answer with
 * one flash each, deuterium first and helium-3 after it, so a burning reactor shows the two rings
 * pulsing in turn. Between pulses a fuelled injector holds both rings at a dim glow, and a starved
 * one (STALL) leaves them dark. The nozzle tip glows plasma for as long as the injector is feeding
 * and turns to the warning heat colour in the HOT readout. The rings are told apart by position and
 * by the painted collar, never by hue alone.</p>
 *
 * <p>With {@code renderAnimationsEnabled=false} nothing flashes: a feeding injector shows both rings
 * steadily lit. Reads only the machine's synced render surface; no player data.</p>
 */
public class FuelInjectorRenderer
        implements BlockEntityRenderer<FuelInjectorBlockEntity, FuelInjectorRenderer.State> {

    /** The status lamp socket: the {@code status_socket} element of {@code models/block/fuel_injector.json} (pixels). */
    private static final StatusLampRenderer.Socket SOCKET = StatusLampRenderer.Socket.model(1.0F, 12.0F, -0.5F, 3.0F, 14.0F, 0.0F);

    private static final Identifier RING_TEX = MachineRenderHelper.texture("fuel_injector_ring");
    private static final Identifier GLOW_TEX = MachineRenderHelper.texture("fuel_injector_glow");

    /** Gas colours (art direction: deuterium blue, helium-3 rose), packed 0xRRGGBB. */
    private static final int DEUTERIUM_RGB = 0x7092F0;
    private static final int HELIUM_3_RGB = 0xF4BCC8;
    private static final int UNLIT_RGB = 0x181E24;
    private static final int WARN_RGB = 0xFFB238;

    private static final float PX = 1.0F / 16.0F;
    private static final float GAP = 0.004F;
    /** Coupling collar faces (model space, north = front): deuterium at high x, helium-3 at low x. */
    private static final float RING_Y0 = 4.0F * PX;
    private static final float RING_Y1 = 8.0F * PX;
    private static final float DEUTERIUM_X0 = 9.5F * PX;
    private static final float HELIUM_3_X0 = 2.5F * PX;
    private static final float RING_SIZE = 4.0F * PX;
    private static final float RING_Z = -1.5F * PX - GAP;
    /** Nozzle tip: the cone ends 3 px past the back face. */
    private static final float GLOW_MIN = 5.5F * PX;
    private static final float GLOW_MAX = 10.5F * PX;
    private static final float GLOW_Z = 19.0F * PX + GAP;

    /** One injection: the deuterium ring flashes over the first half, helium-3 over the second. */
    private static final float PULSE_TICKS = 16.0F;
    /** Ring level of a fuelled injector between pulses. */
    private static final float IDLE_LEVEL = 0.35F;

    public static class State extends BlockEntityRenderState {
        final StatusLampRenderer.Lamp lamp = new StatusLampRenderer.Lamp();
        Direction facing = Direction.NORTH;
        /** 0 = dark, 1 = full flash. */
        float deuterium;
        float helium3;
        /** The injector fed the reactor within its hold time. */
        boolean feeding;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    /** The couplings and the nozzle leave the block; NeoForge frustum hook, unused elsewhere. */
    public AABB getRenderBoundingBox(FuelInjectorBlockEntity injector) {
        return new AABB(injector.getBlockPos()).inflate(1.0);
    }

    @Override
    public void extractRenderState(FuelInjectorBlockEntity injector, State state, float partialTick,
            Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(injector, state, partialTick, cameraPos, breakProgress);
        state.facing = injector.getBlockState().getValue(NeroTechMachineBlock.FACING);
        StatusLampRenderer.extract(injector, state.lamp, partialTick);
        state.feeding = injector.renderActive();

        float flashDeuterium = 0.0F;
        float flashHelium3 = 0.0F;
        Level level = injector.getLevel();
        if (level != null) {
            long tick = level.getGameTime();
            // Injection pulse: detect a synced counter change once, timestamp it, animate briefly.
            int pulse = injector.renderPulse();
            if (pulse != injector.clientSeenPulse) {
                // First sight after (re)load only records the counter: no phantom pulse on chunk load.
                if (injector.clientSeenPulse != Integer.MIN_VALUE) {
                    injector.clientPulseTime = tick;
                }
                injector.clientSeenPulse = pulse;
            }
            if (state.lamp.animate && injector.clientPulseTime != Long.MIN_VALUE) {
                float since = (tick - injector.clientPulseTime) + partialTick;
                float half = PULSE_TICKS / 2.0F;
                if (since >= 0.0F && since < half) {
                    flashDeuterium = Mth.sin((float) Math.PI * since / half);
                } else if (since >= half && since < PULSE_TICKS) {
                    flashHelium3 = Mth.sin((float) Math.PI * (since - half) / half);
                }
            }
        }
        // Starved: both rings dark. Fuelled: a dim hold between pulses. Static frame: steady.
        float hold = StatusLampRenderer.stalled(state.lamp) ? 0.0F : IDLE_LEVEL;
        if (!state.lamp.animate && state.feeding) {
            hold = 1.0F;
        }
        // C1 extra: an empty tank's ring stays dark between pulses (its own gas is missing).
        state.deuterium = Math.max(injector.hasDeuterium() ? hold : 0.0F, flashDeuterium);
        state.helium3 = Math.max(injector.hasHelium3() ? hold : 0.0F, flashHelium3);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector,
            CameraRenderState cameraState) {
        poseStack.pushPose();
        MachineRenderHelper.rotateToFacing(poseStack, state.facing);

        if (state.deuterium > 0.0F || state.helium3 > 0.0F) {
            int deuterium = MachineRenderHelper.lerpColor(state.deuterium, UNLIT_RGB, DEUTERIUM_RGB);
            int helium3 = MachineRenderHelper.lerpColor(state.helium3, UNLIT_RGB, HELIUM_3_RGB);
            boolean drawDeuterium = state.deuterium > 0.0F;
            boolean drawHelium3 = state.helium3 > 0.0F;
            collector.order(1).submitCustomGeometry(poseStack, RenderTypes.entityCutout(RING_TEX), (pose, c) -> {
                if (drawDeuterium) {
                    MachineRenderHelper.lampQuad(c, pose, deuterium, DEUTERIUM_X0, RING_Y0,
                            DEUTERIUM_X0 + RING_SIZE, RING_Y1, RING_Z);
                }
                if (drawHelium3) {
                    MachineRenderHelper.lampQuad(c, pose, helium3, HELIUM_3_X0, RING_Y0,
                            HELIUM_3_X0 + RING_SIZE, RING_Y1, RING_Z);
                }
            });
        }

        if (state.feeding) {
            int rgb = StatusLampRenderer.hot(state.lamp) ? WARN_RGB : MachineRenderHelper.PLASMA_RGB;
            collector.order(2).submitCustomGeometry(poseStack, RenderTypes.entityCutout(GLOW_TEX),
                    (pose, c) -> MachineRenderHelper.face(c, pose, MachineRenderHelper.FULL_BRIGHT, rgb, 0, 0, 1,
                            GLOW_MAX, GLOW_MIN, GLOW_Z, 0, 1, GLOW_MAX, GLOW_MAX, GLOW_Z, 0, 0,
                            GLOW_MIN, GLOW_MAX, GLOW_Z, 1, 0, GLOW_MIN, GLOW_MIN, GLOW_Z, 1, 1));
        }

        StatusLampRenderer.submitShimmer(state.lamp, poseStack, collector, 1.0F);
        StatusLampRenderer.submit(state.lamp, poseStack, collector, SOCKET);
        poseStack.popPose();
    }
}
