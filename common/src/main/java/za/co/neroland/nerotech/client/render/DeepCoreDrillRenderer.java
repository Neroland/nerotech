package za.co.neroland.nerotech.client.render;

import java.util.Map;
import java.util.WeakHashMap;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import za.co.neroland.nerotech.machine.NeroTechMachineBlock;
import za.co.neroland.nerotech.machine.extraction.DeepCoreDrillBlockEntity;
import za.co.neroland.nerotech.machine.extraction.DrillHeadTier;

/**
 * Deep Core Drill renderer. The rotary table and kelly turn on the deck; the drill string runs
 * down through the three derricks to a bit in the colour of the fitted head tier, bobbing two
 * pixels as it turns, with lit teal collars so the motion reads through the lattice; the exhaust stub puffs and chips fly at the derrick foot while it cuts.
 *
 * <p>What each state looks like: no head or not formed, no string at all. Stalled with a head (a
 * spent or missing deposit, a full output), the string creeps round at quarter speed with no chips
 * and no exhaust. Out of energy, everything stops where it was. Overheating, quarter speed and a
 * dark, heavy exhaust.</p>
 *
 * <p>Reads only synced state: {@code drilling()}, {@code headTier()}, {@code formed()}, the
 * readout and the heat bucket. No player data.</p>
 */
public class DeepCoreDrillRenderer
        implements BlockEntityRenderer<DeepCoreDrillBlockEntity, DeepCoreDrillRenderer.State> {

    /** The status lamp socket: the {@code status_socket} element of {@code models/block/deep_core_drill.json} (pixels). */
    private static final StatusLampRenderer.Socket SOCKET = StatusLampRenderer.Socket.model(1.0F, 12.0F, -0.5F, 3.0F, 14.0F, 0.0F);

    private static final Identifier TABLE_TEX = MachineRenderHelper.texture("deep_core_drill_table");
    private static final Identifier STRING_TEX = MachineRenderHelper.texture("deep_core_drill_string");
    private static final Identifier HEAD_TEX = MachineRenderHelper.texture("deep_core_drill_head");
    private static final Identifier PUFF_TEX = MachineRenderHelper.texture("deep_core_drill_puff");

    /** Derricks under the controller. */
    private static final int DERRICKS = 3;
    private static final float PX = 1.0F / 16.0F;
    private static final float DEGREES_PER_TICK = 15.0F;
    private static final float RUN_UP_TICKS = 20.0F;
    private static final float RUN_DOWN_TICKS = 40.0F;
    private static final float CREEP = 0.25F;
    private static final float BOB = 2.0F * PX;
    /** One bob every 20 ticks of travel; the motion phase wraps on a whole number of them. */
    private static final float BOB_RATE = (float) (2.0 * Math.PI / 20.0);
    /** The string's low end at rest: clear of the ground with the bit under it and the bob below. */
    private static final float STRING_BOTTOM = -DERRICKS + 8.0F * PX;
    private static final float STRING_TOP = 5.0F * PX;
    private static final float STRING_HALF = 1.25F * PX;
    /** Teal wear collars down the string, half a block apart: they carry the bob at a distance. */
    private static final float COLLAR_HALF = 1.5F * PX;
    private static final float COLLAR_PITCH = 8.0F * PX;
    private static final float COLLAR_HEIGHT = 1.0F * PX;
    private static final float BIT_HALF = 1.75F * PX;
    private static final float BIT_HEIGHT = 4.0F * PX;

    /** Dark pipe, so the string stands out against the bright lattice it runs through. */
    private static final int PIPE_RGB = 0x566672;
    private static final int TABLE_RGB = 0xC8D0D8;
    private static final int IRON_RGB = (150 << 16) | (154 << 8) | 162;
    private static final int CINDER_RGB = (128 << 16) | (104 << 8) | 94;
    private static final int GLACITE_RGB = (176 << 16) | (220 << 8) | 240;
    private static final int EXHAUST_RGB = 0xC8CED2;
    private static final int EXHAUST_HOT_RGB = 0x34302E;

    /** Exhaust stub mouth (model space): centre and top. */
    private static final float STUB_X = 11.25F * PX;
    private static final float STUB_Z = 13.75F * PX;
    private static final int PUFFS = 2;
    private static final float PUFF_TICKS = 24.0F;

    private final Map<BlockEntity, CentrifugeRenderer.Motion> motions = new WeakHashMap<>();

    public static class State extends BlockEntityRenderState {
        final StatusLampRenderer.Lamp lamp = new StatusLampRenderer.Lamp();
        Direction facing = Direction.NORTH;
        boolean string;
        boolean cutting;
        boolean turning;
        int bitRgb = IRON_RGB;
        float angle;
        float bob;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    /** The string runs three blocks below the controller; the exhaust and shimmer stand above it. */
    public AABB getRenderBoundingBox(DeepCoreDrillBlockEntity drill) {
        BlockPos pos = drill.getBlockPos();
        return new AABB(pos.getX() - 1.0, pos.getY() - DERRICKS - 1.0, pos.getZ() - 1.0,
                pos.getX() + 2.0, pos.getY() + 2.0, pos.getZ() + 2.0);
    }

    @Override
    public void extractRenderState(DeepCoreDrillBlockEntity drill, State state, float partialTick,
            Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(drill, state, partialTick, cameraPos, breakProgress);
        state.facing = drill.getBlockState().getValue(NeroTechMachineBlock.FACING);
        StatusLampRenderer.extract(drill, state.lamp, partialTick);
        DrillHeadTier tier = drill.headTier();
        state.string = tier != null && drill.formed();
        state.bitRgb = tier == DrillHeadTier.GLACITE ? GLACITE_RGB
                : tier == DrillHeadTier.CINDERSTEEL ? CINDER_RGB : IRON_RGB;
        boolean drilling = drill.drilling() && state.string;
        state.cutting = drilling && state.lamp.animate;
        state.turning = drilling;
        if (!state.lamp.animate) {
            state.angle = 0.0F;
            state.bob = 0.0F;
            return;
        }
        float target;
        if (drilling) {
            target = StatusLampRenderer.hot(state.lamp) ? CREEP : 1.0F;
        } else if (state.string && StatusLampRenderer.stalled(state.lamp) && !state.lamp.dark
                && drill.nothingToCut()) {
            target = CREEP; // deposit used up (or none): the string still creeps round, without chips
        } else if (state.string && StatusLampRenderer.stalled(state.lamp)) {
            target = 0.0F; // any other stall (output full, unpowered): frozen mid-stroke, never parked
        } else {
            target = 0.0F;
        }
        CentrifugeRenderer.Motion motion = CentrifugeRenderer.Motion.of(this.motions, drill);
        motion.advance(state.lamp.time, target, RUN_UP_TICKS, RUN_DOWN_TICKS);
        state.angle = (motion.phase * DEGREES_PER_TICK) % 360.0F;
        state.bob = BOB * (float) Math.sin(motion.phase * BOB_RATE);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector,
            CameraRenderState cameraState) {
        int light = state.lightCoords;
        poseStack.pushPose();
        MachineRenderHelper.rotateToFacing(poseStack, state.facing);
        StatusLampRenderer.submit(state.lamp, poseStack, collector, SOCKET);
        StatusLampRenderer.submitShimmer(state.lamp, poseStack, collector, 1.0F);

        if (state.cutting) {
            boolean hot = StatusLampRenderer.hot(state.lamp);
            float time = state.lamp.time;
            collector.order(1).submitCustomGeometry(poseStack, RenderTypes.entityCutout(PUFF_TEX),
                    (pose, c) -> exhaust(c, pose, light, hot, time));
            // Rock chips at the derrick foot are rock_chip particles from the client tick (MachineEffects).
        }

        poseStack.translate(0.5F, 0.0F, 0.5F);
        CentrifugeRenderer.spinY(poseStack, state.angle);
        collector.order(3).submitCustomGeometry(poseStack, RenderTypes.entityCutout(TABLE_TEX),
                (pose, c) -> MachineRenderHelper.box(c, pose, light, TABLE_RGB,
                        -3.5F * PX, 6.0F * PX, -3.5F * PX, 3.5F * PX, 7.0F * PX, 3.5F * PX));
        float bottom = STRING_BOTTOM + state.bob;
        boolean string = state.string;
        collector.order(4).submitCustomGeometry(poseStack, RenderTypes.entityCutout(STRING_TEX),
                (pose, c) -> {
                    // Kelly: the square bar the table turns, up into the swivel.
                    MachineRenderHelper.box(c, pose, light, PIPE_RGB,
                            -0.75F * PX, 7.0F * PX, -0.75F * PX, 0.75F * PX, 12.0F * PX, 0.75F * PX);
                    if (string) {
                        MachineRenderHelper.fluidColumn(c, pose, light, PIPE_RGB,
                                -STRING_HALF, bottom, -STRING_HALF, STRING_HALF, STRING_TOP, STRING_HALF, 0.0F);
                    }
                });
        if (string) {
            int collarRgb = state.turning ? MachineRenderHelper.CYAN_RGB : MachineRenderHelper.TEAL_RGB;
            collector.order(6).submitCustomGeometry(poseStack,
                    RenderTypes.entityCutout(MachineRenderHelper.LAMP_TEX),
                    (pose, c) -> collars(c, pose, collarRgb, bottom));
            int bitRgb = state.bitRgb;
            collector.order(5).submitCustomGeometry(poseStack, RenderTypes.entityCutout(HEAD_TEX),
                    (pose, c) -> MachineRenderHelper.box(c, pose, light, bitRgb,
                            -BIT_HALF, bottom - BIT_HEIGHT, -BIT_HALF, BIT_HALF, bottom, BIT_HALF));
        }
        poseStack.popPose();
    }

    /** Full-bright collars from just above the bit up to the underside of the deck. */
    private static void collars(VertexConsumer c, PoseStack.Pose pose, int rgb, float bottom) {
        for (float y = bottom + 4.0F * PX; y < -COLLAR_HEIGHT; y += COLLAR_PITCH) {
            MachineRenderHelper.box(c, pose, MachineRenderHelper.FULL_BRIGHT, rgb,
                    -COLLAR_HALF, y, -COLLAR_HALF, COLLAR_HALF, y + COLLAR_HEIGHT, COLLAR_HALF);
        }
    }

    /** Two puffs rising off the exhaust stub and spreading; darker and heavier when overheating. */
    private static void exhaust(VertexConsumer c, PoseStack.Pose pose, int light, boolean hot, float time) {
        int rgb = hot ? EXHAUST_HOT_RGB : EXHAUST_RGB;
        for (int i = 0; i < PUFFS; i++) {
            float t = time / PUFF_TICKS + i / (float) PUFFS;
            t -= (float) Math.floor(t);
            float half = (hot ? 2.5F : 1.5F) * PX + t * (hot ? 3.5F : 2.5F) * PX;
            float y0 = 1.0F + t * (hot ? 8.0F : 12.0F) * PX;
            float y1 = y0 + 2.0F * half;
            MachineRenderHelper.face(c, pose, light, rgb, 0, 0, -1,
                    STUB_X - half, y0, STUB_Z, 0, 1, STUB_X - half, y1, STUB_Z, 0, 0,
                    STUB_X + half, y1, STUB_Z, 1, 0, STUB_X + half, y0, STUB_Z, 1, 1);
            MachineRenderHelper.face(c, pose, light, rgb, -1, 0, 0,
                    STUB_X, y0, STUB_Z - half, 0, 1, STUB_X, y1, STUB_Z - half, 0, 0,
                    STUB_X, y1, STUB_Z + half, 1, 0, STUB_X, y0, STUB_Z + half, 1, 1);
        }
    }
}
