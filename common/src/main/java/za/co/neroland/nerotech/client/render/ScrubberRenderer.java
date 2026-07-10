package za.co.neroland.nerotech.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

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

import za.co.neroland.nerotech.NeroTechCommon;
import za.co.neroland.nerotech.config.NeroTechConfig;
import za.co.neroland.nerotech.machine.NeroTechMachineBlock;
import za.co.neroland.nerotech.machine.ScrubberBlockEntity;

/**
 * Scrubber BER (MODELS.md Stage F): fan blades spinning behind the front intake cowl while the
 * machine is actually scrubbing (a crossed pair of radial-blade quads — the second offset 45° for
 * an eight-blade read), a filter-cartridge quad in the side bay whose colour darkens from
 * filter-white toward A_DARK with the synced {@code foulingFraction()}, and a heat-lerped status
 * LED plate over the cowl. The block model leaves the cowl window (x4–12, y3–11, z0–3) and the
 * east bay opening (y4–10, z5–14) open for exactly these parts. Idle + cold + empty draws nothing.
 *
 * <p>Sync discipline: extraction reads only the BE's synced render surface ({@code renderActive},
 * {@code heatFraction}, {@code foulingFraction} — fouling syncs on ~6-step bucket change); with
 * {@code renderAnimationsEnabled=false} the fan draws as a static parked frame (spin frozen at 0).</p>
 */
public class ScrubberRenderer
        implements BlockEntityRenderer<ScrubberBlockEntity, ScrubberRenderer.State> {

    private static final Identifier FAN_TEX = MachineRenderHelper.texture("scrubber_fan");
    private static final Identifier FILTER_TEX = Identifier.fromNamespaceAndPath(
            NeroTechCommon.MOD_ID, "textures/item/filter_cartridge.png");

    /** Fan plane depth inside the cowl recess (window runs z0–3; body face at z=3). */
    private static final float FAN_Z = 1.5F / 16.0F;
    /** Second fan quad sits just behind the first (the 45°-offset cross partner). */
    private static final float FAN_Z2 = 2.1F / 16.0F;
    /** LED glow plate just in front of the body face at the back of the recess. */
    private static final float GLOW_Z = 2.7F / 16.0F;
    /** Fan half-size: a 7×7px quad inside the 8×8px cowl window. */
    private static final float FAN_R = 3.5F / 16.0F;
    /** Fan/window centre (x8, y7 in model px). */
    private static final float CENTRE_X = 8.0F / 16.0F;
    private static final float CENTRE_Y = 7.0F / 16.0F;
    /** Fan spin speed (degrees per tick) — constant read as "scrubbing". */
    private static final float SPIN_SPEED = 18.0F;

    // MODELS.md palette: the filter darkening lerp runs filter-white → A_DARK as it fouls.
    private static final int FILTER_WHITE = (236 << 16) | (238 << 8) | 242;
    private static final int[] A_DARK = {24, 30, 36};

    /** Render state: everything extracted from the BE + clocks, nothing read at submit time. */
    public static class State extends BlockEntityRenderState {
        boolean active;
        float heat;
        float fouling;
        boolean hasFilter;
        float spin;
        Direction facing = Direction.NORTH;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    /**
     * One-block pad (MODELS.md constraint). NeoForge-only frustum hook; inert non-override on
     * Fabric/Forge (Nerospace precedent — vanilla {@code BlockEntityRenderer} has no such method).
     */
    public AABB getRenderBoundingBox(ScrubberBlockEntity scrubber) {
        return new AABB(scrubber.getBlockPos()).inflate(1.0);
    }

    @Override
    public void extractRenderState(ScrubberBlockEntity scrubber, State state, float partialTick,
            Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(scrubber, state, partialTick, cameraPos, breakProgress);
        Level level = scrubber.getLevel();
        state.heat = scrubber.heatFraction();
        state.fouling = scrubber.foulingFraction();
        // The machine items ride the BE update tag (base getUpdateTag), so the bay read is synced.
        state.hasFilter = !scrubber.getItem(ScrubberBlockEntity.FILTER_SLOT).isEmpty();
        state.facing = scrubber.getBlockState().getValue(NeroTechMachineBlock.FACING);
        // Explicit null check (not folded into the flag) so ecj's null-flow analysis can track it.
        if (level == null) {
            state.active = false;
            state.spin = 0.0F;
            return;
        }
        state.active = scrubber.renderActive();
        if (state.active && NeroTechConfig.renderAnimationsEnabled()) {
            float now = level.getGameTime() + partialTick;
            state.spin = (now * SPIN_SPEED) % 360.0F;
        } else {
            state.spin = 0.0F; // static parked frame (animations off) / not extracted further (idle)
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector,
            CameraRenderState cameraState) {
        boolean glow = state.active || state.heat > 0.02F;
        if (!glow && !state.hasFilter) {
            return; // idle + cold + empty bay: the static block model is the whole machine
        }
        int light = MachineRenderHelper.FULL_BRIGHT;

        poseStack.pushPose();
        MachineRenderHelper.rotateToFacing(poseStack, state.facing);

        // Heat-lerped status LED plate at the back of the cowl recess (static — readable parked).
        if (glow) {
            int heatRgb = MachineRenderHelper.heatColor(state.heat);
            collector.order(1).submitCustomGeometry(poseStack, RenderTypes.entityCutout(FAN_TEX),
                    (pose, c) -> MachineRenderHelper.face(c, pose, light, heatRgb, 0, 0, -1,
                            CENTRE_X - FAN_R - 0.5F / 16.0F, CENTRE_Y - FAN_R, GLOW_Z, 0, 1,
                            CENTRE_X - FAN_R - 0.5F / 16.0F, CENTRE_Y + FAN_R, GLOW_Z, 0, 0,
                            CENTRE_X + FAN_R + 0.5F / 16.0F, CENTRE_Y + FAN_R, GLOW_Z, 1, 0,
                            CENTRE_X + FAN_R + 0.5F / 16.0F, CENTRE_Y - FAN_R, GLOW_Z, 1, 1));
        }

        // Fan cross behind the cowl while scrubbing: two radial-blade quads, the rear one offset
        // 45° so the pair reads as eight blades (parked at 0°/45° when animations are off).
        if (state.active) {
            fanQuad(poseStack, collector, light, FAN_Z, state.spin, 2);
            fanQuad(poseStack, collector, light, FAN_Z2, state.spin + 45.0F, 3);
        }

        // Filter cartridge in the east bay, darkening filter-white → A_DARK as it fouls. The bay
        // opening spans y4–10, z5–14 on the east face; the quad floats just inside it.
        if (state.hasFilter) {
            float t = Mth.clamp(state.fouling, 0.0F, 1.0F);
            int r = (int) Mth.lerp(t, (FILTER_WHITE >> 16) & 0xFF, A_DARK[0]);
            int g = (int) Mth.lerp(t, (FILTER_WHITE >> 8) & 0xFF, A_DARK[1]);
            int b = (int) Mth.lerp(t, FILTER_WHITE & 0xFF, A_DARK[2]);
            int filterRgb = (r << 16) | (g << 8) | b;
            float x = 15.6F / 16.0F;
            collector.order(1).submitCustomGeometry(poseStack, RenderTypes.entityCutout(FILTER_TEX),
                    (pose, c) -> MachineRenderHelper.face(c, pose, light, filterRgb, 1, 0, 0,
                            x, 4.0F / 16.0F, 14.0F / 16.0F, 0, 1,
                            x, 10.0F / 16.0F, 14.0F / 16.0F, 0, 0,
                            x, 10.0F / 16.0F, 5.0F / 16.0F, 1, 0,
                            x, 4.0F / 16.0F, 5.0F / 16.0F, 1, 1));
        }
        poseStack.popPose();
    }

    /** One radial-blade fan quad in the cowl plane, spun to {@code angle}. */
    private static void fanQuad(PoseStack poseStack, SubmitNodeCollector collector, int light,
            float z, float angle, int order) {
        poseStack.pushPose();
        poseStack.translate(CENTRE_X, CENTRE_Y, z);
        poseStack.mulPose(Axis.ZP.rotationDegrees(angle));
        collector.order(order).submitCustomGeometry(poseStack, RenderTypes.entityCutout(FAN_TEX),
                (pose, c) -> MachineRenderHelper.face(c, pose, light, 0xFFFFFF, 0, 0, -1,
                        -FAN_R, -FAN_R, 0, 0, 1,
                        -FAN_R, FAN_R, 0, 0, 0,
                        FAN_R, FAN_R, 0, 1, 0,
                        FAN_R, -FAN_R, 0, 1, 1));
        poseStack.popPose();
    }
}
