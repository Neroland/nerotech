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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import za.co.neroland.nerotech.machine.NeroTechMachineBlock;
import za.co.neroland.nerotech.machine.metallurgy.PlatePressBlockEntity;

/**
 * Plate Press renderer: the ram between the columns, the die fitted on the bolster and the flash on
 * impact, plus the shared status lamp and heat shimmer.
 *
 * <p>The fitted die is the readout of what the press will make: its cavity (wide pocket, three
 * grooves, round pocket) is drawn on the bolster from the synced {@code die()} id, and an empty die
 * slot leaves the bolster's dark seat bare. A die from a datapack that is not one of the three shows
 * the plate die's face.</p>
 *
 * <p>The ram's position is a function of the machine's eased progress (three strokes per job), so
 * it is frame-rate independent, runs at the machine's own rate, slows with it when throttled and
 * freezes where it is when the machine stalls. A stall that would leave the ram at home hangs it
 * half-way instead, so a stalled press never looks idle. REST and
 * {@code renderAnimationsEnabled=false} park it under the crosshead.</p>
 *
 * <p>Reads only the machine's synced render surface; no player data.</p>
 */
public class PlatePressRenderer
        implements BlockEntityRenderer<PlatePressBlockEntity, PlatePressRenderer.State> {

    /** The status lamp socket: the {@code status_socket} element of {@code models/block/plate_press.json} (pixels). */
    private static final StatusLampRenderer.Socket SOCKET = StatusLampRenderer.Socket.model(1.0F, 12.0F, 3.5F, 3.0F, 14.0F, 4.0F);

    private static final Identifier RAM_TEX = MachineRenderHelper.texture("plate_press_ram");
    private static final Identifier ROD_TEX = MachineRenderHelper.texture("plate_press_rod");
    private static final Identifier FLASH_TEX = MachineRenderHelper.texture("plate_press_flash");
    private static final Identifier[] DIE_TEX = {
            MachineRenderHelper.texture("plate_press_die_plate"),
            MachineRenderHelper.texture("plate_press_die_rod"),
            MachineRenderHelper.texture("plate_press_die_wafer"),
    };
    private static final String ROD_DIE = "nerotech:rod_die";
    private static final String WAFER_DIE = "nerotech:wafer_die";

    private static final float PX = 1.0F / 16.0F;
    private static final float CROSSHEAD_BOTTOM = 12.0F * PX;
    /** Ram head footprint and height; the die lies on the bolster (model y 6) under it. */
    private static final float RAM_MIN = 5.0F * PX;
    private static final float RAM_MAX = 11.0F * PX;
    private static final float RAM_HEIGHT = 2.0F * PX;
    private static final float RAM_HOME = 10.0F * PX;
    private static final float RAM_TRAVEL = 3.0F * PX;
    private static final float ROD_MIN = 7.0F * PX;
    private static final float ROD_MAX = 9.0F * PX;
    private static final float DIE_BOTTOM = 6.0F * PX;
    private static final float DIE_TOP = 7.0F * PX;
    /** Impact flash: a flat burst just over the die, wider than the ram. */
    private static final float FLASH_Y = 7.15F * PX;
    private static final float FLASH_MIN = 2.0F * PX;
    private static final float FLASH_MAX = 14.0F * PX;

    /** Strokes per job, and the stroke's shape as fractions of one stroke: drop, dwell, lift. */
    private static final float STROKES = 3.0F;
    private static final float DROP_END = 0.25F;
    private static final float DWELL_END = 0.35F;
    private static final float STALL_HANG = 0.5F;

    public static class State extends BlockEntityRenderState {
        final StatusLampRenderer.Lamp lamp = new StatusLampRenderer.Lamp();
        Direction facing = Direction.NORTH;
        /** 0 = parked under the crosshead, 1 = on the die. */
        float drop;
        /** 0 = no flash, 1 = the moment of impact. */
        float flash;
        /** -1 = no die fitted, otherwise an index into the die sprites. */
        int die = -1;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    /** One-block pad for the shimmer above the crosshead; NeoForge frustum hook, unused elsewhere. */
    public AABB getRenderBoundingBox(PlatePressBlockEntity press) {
        return new AABB(press.getBlockPos()).inflate(1.0);
    }

    @Override
    public void extractRenderState(PlatePressBlockEntity press, State state, float partialTick,
            Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(press, state, partialTick, cameraPos, breakProgress);
        state.facing = press.getBlockState().getValue(NeroTechMachineBlock.FACING);
        StatusLampRenderer.extract(press, state.lamp, partialTick);

        String die = press.die();
        state.die = die.isEmpty() ? -1 : ROD_DIE.equals(die) ? 1 : WAFER_DIE.equals(die) ? 2 : 0;

        state.drop = 0.0F;
        state.flash = 0.0F;
        StatusLampRenderer.Lamp lamp = state.lamp;
        if (!lamp.animate || lamp.readout == StatusLampRenderer.REST) {
            return; // parked: idle, or the static frame
        }
        float stroke = press.progressFraction(partialTick) * STROKES;
        float phase = stroke - Mth.floor(stroke);
        if (phase < DROP_END) {
            float t = phase / DROP_END;
            state.drop = t * t; // accelerating fall
        } else if (phase < DWELL_END) {
            state.drop = 1.0F;
            state.flash = 1.0F - (phase - DROP_END) / (DWELL_END - DROP_END);
        } else {
            float t = (phase - DWELL_END) / (1.0F - DWELL_END);
            state.drop = 1.0F - t * t * (3.0F - 2.0F * t); // eased lift
        }
        if (StatusLampRenderer.stalled(lamp)) {
            state.flash = 0.0F;
            if (state.drop < STALL_HANG) {
                state.drop = STALL_HANG; // hangs half-way: a stalled press never looks idle
            }
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector,
            CameraRenderState cameraState) {
        int light = state.lightCoords;
        poseStack.pushPose();
        MachineRenderHelper.rotateToFacing(poseStack, state.facing);

        if (state.die >= 0) {
            collector.order(1).submitCustomGeometry(poseStack, RenderTypes.entityCutout(DIE_TEX[state.die]),
                    (pose, c) -> MachineRenderHelper.box(c, pose, light,
                            RAM_MIN, DIE_BOTTOM, RAM_MIN, RAM_MAX, DIE_TOP, RAM_MAX));
        }

        float ramBottom = RAM_HOME - RAM_TRAVEL * state.drop;
        float ramTop = ramBottom + RAM_HEIGHT;
        if (ramTop < CROSSHEAD_BOTTOM) {
            collector.order(1).submitCustomGeometry(poseStack, RenderTypes.entityCutout(ROD_TEX),
                    (pose, c) -> MachineRenderHelper.box(c, pose, light,
                            ROD_MIN, ramTop, ROD_MIN, ROD_MAX, CROSSHEAD_BOTTOM, ROD_MAX));
        }
        collector.order(1).submitCustomGeometry(poseStack, RenderTypes.entityCutout(RAM_TEX),
                (pose, c) -> MachineRenderHelper.box(c, pose, light,
                        RAM_MIN, ramBottom, RAM_MIN, RAM_MAX, ramTop, RAM_MAX));

        if (state.flash > 0.0F) {
            int rgb = MachineRenderHelper.lerpColor(state.flash, MachineRenderHelper.CYAN_RGB, 0xFFFFFF);
            float grow = (1.0F - state.flash) * PX;
            float lo = FLASH_MIN - grow;
            float hi = FLASH_MAX + grow;
            collector.order(2).submitCustomGeometry(poseStack, RenderTypes.entityCutout(FLASH_TEX),
                    (pose, c) -> MachineRenderHelper.face(c, pose, MachineRenderHelper.FULL_BRIGHT, rgb, 0, 1, 0,
                            lo, FLASH_Y, lo, 0, 0, lo, FLASH_Y, hi, 0, 1,
                            hi, FLASH_Y, hi, 1, 1, hi, FLASH_Y, lo, 1, 0));
        }

        StatusLampRenderer.submitShimmer(state.lamp, poseStack, collector, 1.0F);
        StatusLampRenderer.submit(state.lamp, poseStack, collector, SOCKET);
        poseStack.popPose();
    }
}
