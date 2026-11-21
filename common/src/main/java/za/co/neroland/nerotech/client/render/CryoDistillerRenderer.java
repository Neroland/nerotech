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

import za.co.neroland.nerotech.machine.NeroTechMachineBlock;
import za.co.neroland.nerotech.machine.chemistry.CryoDistillerBlockEntity;

/**
 * Cryo Distiller renderer: a frost sleeve on the column. At rest it covers the upper third; while
 * working it creeps down with progress and cold vapour spills off the top collar; stalled it
 * recedes to a thin cap; overheating it is gone and the column stands bare.
 *
 * <p>Reads only the synced readout and progress. No player data.</p>
 */
public class CryoDistillerRenderer
        implements BlockEntityRenderer<CryoDistillerBlockEntity, CryoDistillerRenderer.State> {

    /** The status lamp socket: the {@code status_socket} element of {@code models/block/cryo_distiller.json} (pixels). */
    private static final StatusLampRenderer.Socket SOCKET = StatusLampRenderer.Socket.model(1.0F, 12.0F, 0.5F, 3.0F, 14.0F, 1.0F);

    private static final Identifier FROST_TEX = MachineRenderHelper.texture("cryo_distiller_frost");
    private static final Identifier VAPOUR_TEX = MachineRenderHelper.texture("cryo_distiller_vapour");

    /** The sleeve sits just outside the column (model x/z 5..11, y 3..16). */
    private static final float SLEEVE_0 = 4.85F / 16.0F;
    private static final float SLEEVE_1 = 11.15F / 16.0F;
    private static final float SLEEVE_TOP = 16.05F / 16.0F;
    private static final float COLUMN_HEIGHT = 13.0F / 16.0F;
    /** Frost reach as a share of the column: resting cap, stalled cap, and the working span. */
    private static final float REST_REACH = 5.0F / 13.0F;
    private static final float STALL_REACH = 2.5F / 13.0F;
    private static final float WORK_SPAN = 7.0F / 13.0F;
    private static final float GROW_TICKS = 60.0F;
    private static final float MELT_TICKS = 30.0F;
    private static final int FROST_RGB = 0xE4F2FA;

    private final Map<BlockEntity, CentrifugeRenderer.Motion> motions = new WeakHashMap<>();

    public static class State extends BlockEntityRenderState {
        final StatusLampRenderer.Lamp lamp = new StatusLampRenderer.Lamp();
        Direction facing = Direction.NORTH;
        float reach;
        boolean vapour;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    /** One-block pad: the heat shimmer stands above the block. */
    public AABB getRenderBoundingBox(CryoDistillerBlockEntity distiller) {
        return new AABB(distiller.getBlockPos()).inflate(1.0);
    }

    @Override
    public void extractRenderState(CryoDistillerBlockEntity distiller, State state, float partialTick,
            Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(distiller, state, partialTick, cameraPos, breakProgress);
        state.facing = distiller.getBlockState().getValue(NeroTechMachineBlock.FACING);
        StatusLampRenderer.extract(distiller, state.lamp, partialTick);
        float target;
        switch (state.lamp.readout) {
            case StatusLampRenderer.WORK:
                target = REST_REACH + WORK_SPAN * distiller.progressFraction(partialTick);
                break;
            case StatusLampRenderer.STALL:
                target = STALL_REACH;
                break;
            case StatusLampRenderer.HOT:
                target = 0.0F;
                break;
            default:
                target = REST_REACH;
                break;
        }
        if (!state.lamp.animate) {
            // Static frame: the resting cap (none when overheating), no vapour.
            state.reach = state.lamp.readout == StatusLampRenderer.HOT ? 0.0F : REST_REACH;
            state.vapour = false;
            return;
        }
        CentrifugeRenderer.Motion motion = CentrifugeRenderer.Motion.of(this.motions, distiller);
        motion.advance(state.lamp.time, target, GROW_TICKS, MELT_TICKS);
        state.reach = motion.speed;
        state.vapour = state.lamp.readout == StatusLampRenderer.WORK;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector,
            CameraRenderState cameraState) {
        int light = MachineRenderHelper.FULL_BRIGHT;
        poseStack.pushPose();
        MachineRenderHelper.rotateToFacing(poseStack, state.facing);
        StatusLampRenderer.submit(state.lamp, poseStack, collector, SOCKET);
        StatusLampRenderer.submitShimmer(state.lamp, poseStack, collector,
                5.0F / 16.0F, 5.0F / 16.0F, 11.0F / 16.0F, 11.0F / 16.0F, 1.0F);

        if (state.reach > 0.02F) {
            float bottom = SLEEVE_TOP - COLUMN_HEIGHT * state.reach;
            collector.order(1).submitCustomGeometry(poseStack, RenderTypes.entityCutout(FROST_TEX),
                    (pose, c) -> MachineRenderHelper.box(c, pose, light, FROST_RGB,
                            SLEEVE_0, bottom, SLEEVE_0, SLEEVE_1, SLEEVE_TOP, SLEEVE_1));
        }
        if (state.vapour) {
            // Falling, so the scroll runs backwards.
            float scroll = -state.lamp.time * 0.015F;
            collector.order(2).submitCustomGeometry(poseStack, RenderTypes.entityCutout(VAPOUR_TEX),
                    (pose, c) -> MachineRenderHelper.shimmerQuads(c, pose, FROST_RGB,
                            3.0F / 16.0F, 3.0F / 16.0F, 13.0F / 16.0F, 13.0F / 16.0F,
                            10.0F / 16.0F, 4.0F / 16.0F, scroll));
        }
        poseStack.popPose();
    }
}
