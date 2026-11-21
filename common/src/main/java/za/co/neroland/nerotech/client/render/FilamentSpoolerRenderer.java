package za.co.neroland.nerotech.client.render;

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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import za.co.neroland.nerotech.machine.NeroTechMachineBlock;
import za.co.neroland.nerotech.machine.metallurgy.FilamentSpoolerBlockEntity;

/**
 * Filament Spooler renderer: the spool in the open cradle (axle side-on and exposed), its winding
 * fattening with progress, and the wire running from the die tower's nozzle to the spool; plus the
 * shared status lamp and heat shimmer.
 *
 * <ul>
 *   <li><b>REST</b> — bare spool, parked, no wire.</li>
 *   <li><b>WORK</b> — the spool turns and fattens, the wire runs taut.</li>
 *   <li><b>STALL</b> — the spool stops where it is and the wire droops.</li>
 *   <li><b>HOT</b> — the die tower glows round the nozzle in the heat colour.</li>
 * </ul>
 *
 * <p>The spool's angle is a function of the machine's eased progress (six turns per job), so it is
 * frame-rate independent, turns at the machine's own rate, slows with it when throttled and stops
 * dead when the machine stalls. With {@code renderAnimationsEnabled=false} the spool does not turn;
 * its winding still shows the progress. Reads only the machine's synced render surface; no player
 * data.</p>
 */
public class FilamentSpoolerRenderer
        implements BlockEntityRenderer<FilamentSpoolerBlockEntity, FilamentSpoolerRenderer.State> {

    /** The status lamp socket: the {@code status_socket} element of {@code models/block/filament_spooler.json} (pixels). */
    private static final StatusLampRenderer.Socket SOCKET = StatusLampRenderer.Socket.model(1.0F, 12.0F, 0.5F, 3.0F, 14.0F, 1.0F);

    private static final Identifier FLANGE_TEX = MachineRenderHelper.texture("filament_spooler_flange");
    private static final Identifier WIRE_TEX = MachineRenderHelper.texture("filament_spooler_wire");
    private static final Identifier AXLE_TEX = MachineRenderHelper.texture("plate_press_rod");
    private static final Identifier GLOW_TEX = MachineRenderHelper.texture("filament_spooler_glow");

    private static final float PX = 1.0F / 16.0F;
    /** Spool axis: runs front to back through the cradle bearings. */
    private static final float SPOOL_X = 5.0F * PX;
    private static final float SPOOL_Y = 9.0F * PX;
    private static final float AXLE_Z0 = 3.0F * PX;
    private static final float AXLE_Z1 = 13.0F * PX;
    private static final float AXLE_R = 0.5F * PX;
    private static final float FLANGE_R = 4.0F * PX;
    private static final float FLANGE_Z0 = 3.6F * PX;
    private static final float FLANGE_Z1 = 12.4F * PX;
    private static final float WIND_Z0 = 4.0F * PX;
    private static final float WIND_Z1 = 12.0F * PX;
    /** Winding half-width: bare core to a full spool. */
    private static final float WIND_R_EMPTY = 1.25F * PX;
    private static final float WIND_R_GROWTH = 2.0F * PX;
    private static final float TURNS_PER_JOB = 6.0F;

    /** The wire: from the nozzle on the tower's flank to the top of the winding, in the plane z = 8. */
    private static final float WIRE_Z = 8.0F * PX;
    private static final float NOZZLE_X = 10.0F * PX;
    private static final float NOZZLE_Y = 12.0F * PX;
    private static final float WIRE_LAND_X = SPOOL_X + 0.6F * PX;
    private static final float WIRE_HALF = 0.25F * PX;
    private static final float WIRE_SAG = 2.2F * PX;
    private static final int WIRE_RGB = 0xD6DCE2;

    private static final float TOWER_TOP = 15.0F * PX;

    private static final int WIRE_NONE = 0;
    private static final int WIRE_TAUT = 1;
    private static final int WIRE_SLACK = 2;

    public static class State extends BlockEntityRenderState {
        final StatusLampRenderer.Lamp lamp = new StatusLampRenderer.Lamp();
        Direction facing = Direction.NORTH;
        float progress;
        float spin;
        int wire = WIRE_NONE;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    /** One-block pad for the shimmer above the tower; NeoForge frustum hook, unused elsewhere. */
    public AABB getRenderBoundingBox(FilamentSpoolerBlockEntity spooler) {
        return new AABB(spooler.getBlockPos()).inflate(1.0);
    }

    @Override
    public void extractRenderState(FilamentSpoolerBlockEntity spooler, State state, float partialTick,
            Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(spooler, state, partialTick, cameraPos, breakProgress);
        state.facing = spooler.getBlockState().getValue(NeroTechMachineBlock.FACING);
        StatusLampRenderer.extract(spooler, state.lamp, partialTick);
        StatusLampRenderer.Lamp lamp = state.lamp;
        boolean rest = lamp.readout == StatusLampRenderer.REST;
        state.progress = rest ? 0.0F : spooler.progressFraction(partialTick);
        state.spin = lamp.animate ? (state.progress * TURNS_PER_JOB * 360.0F) % 360.0F : 0.0F;
        state.wire = rest ? WIRE_NONE : StatusLampRenderer.stalled(lamp) ? WIRE_SLACK : WIRE_TAUT;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector,
            CameraRenderState cameraState) {
        int light = state.lightCoords;
        float windR = WIND_R_EMPTY + WIND_R_GROWTH * state.progress;
        poseStack.pushPose();
        MachineRenderHelper.rotateToFacing(poseStack, state.facing);

        // The spool, turning about its front-to-back axle.
        poseStack.pushPose();
        poseStack.translate(SPOOL_X, SPOOL_Y, 0.0F);
        rotateZ(poseStack, state.spin);
        collector.order(1).submitCustomGeometry(poseStack, RenderTypes.entityCutout(AXLE_TEX),
                (pose, c) -> MachineRenderHelper.box(c, pose, light,
                        -AXLE_R, -AXLE_R, AXLE_Z0, AXLE_R, AXLE_R, AXLE_Z1));
        // Two prisms 45 degrees apart read as an eight-sided winding.
        for (int i = 0; i < 2; i++) {
            collector.order(1).submitCustomGeometry(poseStack, RenderTypes.entityCutout(WIRE_TEX),
                    (pose, c) -> MachineRenderHelper.box(c, pose, light, WIRE_RGB,
                            -windR, -windR, WIND_Z0, windR, windR, WIND_Z1));
            rotateZ(poseStack, 45.0F);
        }
        collector.order(2).submitCustomGeometry(poseStack, RenderTypes.entityCutout(FLANGE_TEX),
                (pose, c) -> {
                    flange(c, pose, light, FLANGE_Z0);
                    flange(c, pose, light, FLANGE_Z1);
                });
        poseStack.popPose();

        // The wire: one straight run when taut, two drooping runs when slack.
        if (state.wire != WIRE_NONE) {
            float landY = SPOOL_Y + windR;
            boolean slack = state.wire == WIRE_SLACK;
            collector.order(2).submitCustomGeometry(poseStack, RenderTypes.entityCutout(WIRE_TEX),
                    (pose, c) -> {
                        if (slack) {
                            float midX = (NOZZLE_X + WIRE_LAND_X) / 2.0F;
                            float midY = (NOZZLE_Y + landY) / 2.0F - WIRE_SAG;
                            wireRun(c, pose, light, NOZZLE_X, NOZZLE_Y, midX, midY);
                            wireRun(c, pose, light, midX, midY, WIRE_LAND_X, landY);
                        } else {
                            wireRun(c, pose, light, NOZZLE_X, NOZZLE_Y, WIRE_LAND_X, landY);
                        }
                    });
        }

        // HOT: the die tower glows round the nozzle (front face and the flank facing the spool).
        if (StatusLampRenderer.hot(state.lamp)) {
            int rgb = MachineRenderHelper.heatColor(StatusLampRenderer.heatFraction(state.lamp));
            collector.order(3).submitCustomGeometry(poseStack, RenderTypes.entityCutout(GLOW_TEX),
                    (pose, c) -> {
                        int bright = MachineRenderHelper.FULL_BRIGHT;
                        float zf = 4.95F * PX;
                        MachineRenderHelper.face(c, pose, bright, rgb, 0, 0, -1,
                                11.5F * PX, 7.5F * PX, zf, 0, 1, 11.5F * PX, 13.5F * PX, zf, 0, 0,
                                14.5F * PX, 13.5F * PX, zf, 1, 0, 14.5F * PX, 7.5F * PX, zf, 1, 1);
                        float xf = 10.95F * PX;
                        MachineRenderHelper.face(c, pose, bright, rgb, -1, 0, 0,
                                xf, 9.0F * PX, 11.0F * PX, 0, 1, xf, 15.0F * PX, 11.0F * PX, 0, 0,
                                xf, 15.0F * PX, 5.0F * PX, 1, 0, xf, 9.0F * PX, 5.0F * PX, 1, 1);
                    });
        }

        StatusLampRenderer.submitShimmer(state.lamp, poseStack, collector,
                10.0F * PX, 4.0F * PX, 16.0F * PX, 12.0F * PX, TOWER_TOP);
        StatusLampRenderer.submit(state.lamp, poseStack, collector, SOCKET);
        poseStack.popPose();
    }

    private static void rotateZ(PoseStack poseStack, float degrees) {
        //? if >=26.3 {
        /*poseStack.rotate(Axis.ZP.rotationDegrees(degrees));
        *///?} else {
        poseStack.mulPose(Axis.ZP.rotationDegrees(degrees));
        //?}
    }

    /** One round flange: the cutout disc sprite on a quad across the axle, seen from both ends. */
    private static void flange(VertexConsumer c, PoseStack.Pose pose, int light, float z) {
        MachineRenderHelper.face(c, pose, light, 0xFFFFFF, 0, 0, -1,
                -FLANGE_R, -FLANGE_R, z, 1, 1, -FLANGE_R, FLANGE_R, z, 1, 0,
                FLANGE_R, FLANGE_R, z, 0, 0, FLANGE_R, -FLANGE_R, z, 0, 1);
    }

    /** One straight run of wire from (x0, y0) to (x1, y1) in the wire plane. */
    private static void wireRun(VertexConsumer c, PoseStack.Pose pose, int light,
            float x0, float y0, float x1, float y1) {
        MachineRenderHelper.face(c, pose, light, WIRE_RGB, 0, 0, -1,
                x0, y0 - WIRE_HALF, WIRE_Z, 0, 1, x0, y0 + WIRE_HALF, WIRE_Z, 0, 0,
                x1, y1 + WIRE_HALF, WIRE_Z, 1, 0, x1, y1 - WIRE_HALF, WIRE_Z, 1, 1);
    }
}
