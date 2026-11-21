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
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import za.co.neroland.nerotech.machine.NeroTechMachineBlock;
import za.co.neroland.nerotech.machine.components.LaserEtcherBlockEntity;

/**
 * Laser Etcher BER. The fitted focus lens (machine contract C3, the synced {@code lens()} item id)
 * shows as a coloured disc in the turret socket, on top and on the turret's front, so the lens can
 * be told from outside; with no lens the painted empty socket shows. In WORK a beam of the lens
 * colour runs from the turret down to the wafer stage and raster-scans it; the stage glows where
 * the beam lands. The scan position is the synced {@code progressFraction} itself (rows along Z,
 * sweeps along X), so a STALL leaves the hit point glowing mid-row with the beam out, and the scan
 * slows exactly as the job does. HOT shimmers the turret.
 *
 * <p>{@code renderAnimationsEnabled=false}: the lens disc only, no beam. Reads only the synced
 * render surface; no player data (POPIA/GDPR n/a).</p>
 */
public class LaserEtcherRenderer
        implements BlockEntityRenderer<LaserEtcherBlockEntity, LaserEtcherRenderer.State> {

    /** The status lamp socket: the {@code status_lamp_socket} element of {@code models/block/laser_etcher.json} (pixels). */
    private static final StatusLampRenderer.Socket SOCKET = StatusLampRenderer.Socket.model(1.0F, 12.0F, -0.5F, 3.0F, 14.0F, 0.0F);

    private static final Identifier LENS_TEX = MachineRenderHelper.texture("laser_etcher_lens");
    private static final Identifier BEAM_TEX = MachineRenderHelper.texture("laser_etcher_beam");

    /** Focus lens glass colours (the art kit's LENS table; the Cutting Lance uses the same four). */
    private static final int QUARTZ_RGB = (232 << 16) | (240 << 8) | 244;
    private static final int GLACITE_RGB = (120 << 16) | (190 << 8) | 255;
    private static final int EMBER_RGB = (236 << 16) | (124 << 8) | 48;
    private static final int VOID_RGB = (58 << 16) | (44 << 8) | 110;

    private static final float PX = 1.0F / 16.0F;
    private static final float GAP = 0.004F;
    private static final float CENTRE = 8.0F * PX;
    /** Beam origin: the roof underside below the turret. */
    private static final float EMIT_Y = 9.0F * PX;
    /** Wafer stage top. */
    private static final float STAGE_Y = 4.0F * PX;
    /** Scan area on the stage (model px 6..10 in X and Z). */
    private static final float SCAN_MIN = 6.0F * PX;
    private static final float SCAN_SPAN = 4.0F * PX;
    /** Raster rows per operation. */
    private static final float ROWS = 8.0F;
    private static final float BEAM_HALF = 0.3F * PX;
    private static final float SPOT_HALF = 0.8F * PX;

    public static class State extends BlockEntityRenderState {
        final StatusLampRenderer.Lamp lamp = new StatusLampRenderer.Lamp();
        Direction facing = Direction.NORTH;
        boolean hasLens;
        int lensRgb;
        float progress;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    /** One-block pad (the HOT shimmer stands above the turret); NeoForge-only frustum hook. */
    public AABB getRenderBoundingBox(LaserEtcherBlockEntity etcher) {
        return new AABB(etcher.getBlockPos()).inflate(1.0);
    }

    /** The glass colour for a lens item id; an unknown lens (an add-on's) reads as clear quartz. */
    static int lensColor(Identifier lens) {
        String path = lens.getPath();
        if (path.startsWith("glacite")) {
            return GLACITE_RGB;
        }
        if (path.startsWith("ember")) {
            return EMBER_RGB;
        }
        if (path.startsWith("void")) {
            return VOID_RGB;
        }
        return QUARTZ_RGB;
    }

    @Override
    public void extractRenderState(LaserEtcherBlockEntity etcher, State state, float partialTick,
            Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(etcher, state, partialTick, cameraPos, breakProgress);
        StatusLampRenderer.extract(etcher, state.lamp, partialTick);
        state.facing = etcher.getBlockState().getValue(NeroTechMachineBlock.FACING);
        Identifier lens = etcher.lens();
        state.hasLens = lens != null;
        state.lensRgb = lens == null ? 0 : lensColor(lens);
        state.progress = etcher.progressFraction(partialTick);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector,
            CameraRenderState cameraState) {
        poseStack.pushPose();
        MachineRenderHelper.rotateToFacing(poseStack, state.facing);
        StatusLampRenderer.submit(state.lamp, poseStack, collector, SOCKET);
        StatusLampRenderer.submitShimmer(state.lamp, poseStack, collector,
                5.0F * PX, 5.0F * PX, 11.0F * PX, 11.0F * PX, 1.0F);

        if (state.hasLens) {
            boolean firing = StatusLampRenderer.moving(state.lamp);
            // The lens sits dull in its socket and lights up while the beam is on.
            int rgb = firing ? state.lensRgb : MachineRenderHelper.lerpColor(0.35F, state.lensRgb, 0x101418);
            collector.order(1).submitCustomGeometry(poseStack, RenderTypes.entityCutout(LENS_TEX),
                    (pose, c) -> lens(c, pose, rgb));

            boolean midJob = state.progress > 0.0F && state.lamp.animate
                    && state.lamp.readout != StatusLampRenderer.REST;
            if (midJob) {
                // Raster: rows advance along Z with progress, each row sweeps X there and back.
                float row = state.progress * ROWS;
                float sweep = row - Mth.floor(row);
                float tri = ((int) row & 1) == 0 ? sweep : 1.0F - sweep;
                float hitX = SCAN_MIN + SCAN_SPAN * tri;
                float hitZ = SCAN_MIN + SCAN_SPAN * state.progress;
                int spot = MachineRenderHelper.lerpColor(firing ? 0.6F : 0.15F, state.lensRgb, 0xFFFFFF);
                collector.order(2).submitCustomGeometry(poseStack,
                        RenderTypes.entityCutout(MachineRenderHelper.LAMP_TEX),
                        (pose, c) -> MachineRenderHelper.face(c, pose, MachineRenderHelper.FULL_BRIGHT,
                                spot, 0, 1, 0,
                                hitX - SPOT_HALF, STAGE_Y + GAP, hitZ - SPOT_HALF, 0, 0,
                                hitX - SPOT_HALF, STAGE_Y + GAP, hitZ + SPOT_HALF, 0, 1,
                                hitX + SPOT_HALF, STAGE_Y + GAP, hitZ + SPOT_HALF, 1, 1,
                                hitX + SPOT_HALF, STAGE_Y + GAP, hitZ - SPOT_HALF, 1, 0));
                if (firing) {
                    int beamRgb = state.lensRgb;
                    collector.order(3).submitCustomGeometry(poseStack, RenderTypes.entityCutout(BEAM_TEX),
                            (pose, c) -> beam(c, pose, beamRgb, hitX, hitZ));
                }
            }
        }
        poseStack.popPose();
    }

    /** The lens disc: on the turret top, and on the turret's front so it reads at eye level. */
    private static void lens(VertexConsumer c, PoseStack.Pose pose, int rgb) {
        int light = MachineRenderHelper.FULL_BRIGHT;
        float y = 1.0F + GAP;
        float a = 6.0F * PX;
        float b = 10.0F * PX;
        MachineRenderHelper.face(c, pose, light, rgb, 0, 1, 0,
                a, y, a, 0, 0, a, y, b, 0, 1, b, y, b, 1, 1, b, y, a, 1, 0);
        float z = 5.0F * PX - GAP;
        float x0 = 7.0F * PX;
        float x1 = 9.0F * PX;
        float y0 = 13.5F * PX;
        float y1 = 15.5F * PX;
        MachineRenderHelper.face(c, pose, light, rgb, 0, 0, -1,
                x0, y0, z, 1, 1, x0, y1, z, 1, 0, x1, y1, z, 0, 0, x1, y0, z, 0, 1);
    }

    /** Two crossed quads from the emitter under the turret to the hit point on the stage. */
    private static void beam(VertexConsumer c, PoseStack.Pose pose, int rgb, float hitX, float hitZ) {
        int light = MachineRenderHelper.FULL_BRIGHT;
        float w = BEAM_HALF;
        MachineRenderHelper.face(c, pose, light, rgb, 0, 0, -1,
                hitX - w, STAGE_Y, hitZ, 0, 1, CENTRE - w, EMIT_Y, CENTRE, 0, 0,
                CENTRE + w, EMIT_Y, CENTRE, 1, 0, hitX + w, STAGE_Y, hitZ, 1, 1);
        MachineRenderHelper.face(c, pose, light, rgb, -1, 0, 0,
                hitX, STAGE_Y, hitZ - w, 0, 1, CENTRE, EMIT_Y, CENTRE - w, 0, 0,
                CENTRE, EMIT_Y, CENTRE + w, 1, 0, hitX, STAGE_Y, hitZ + w, 1, 1);
    }
}
