package za.co.neroland.nerotech.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import za.co.neroland.nerotech.config.NeroTechConfig;
import za.co.neroland.nerotech.machine.SolarArrayBlockEntity;

/**
 * Solar Array BER: the panel deck above the static pedestal + mast, pitching east–west with the real
 * day clock and folding flat at night — Nerospace's {@code SolarPanelRenderer} maths ported verbatim
 * (single block, so no anchor gating and no multiblock mast). The block model tops out at y=14
 * (the deck cradle posts), leaving the space above for exactly this deck.
 *
 * <p>The deck angle is pure clock maths (no synced state needed); {@code renderAnimationsEnabled=false}
 * parks it flat as the static frame. The deck is full-bright: a PV surface reads as reflective, and
 * submitted custom geometry can't rely on {@code lightCoords} (Nerospace finding).</p>
 */
public class SolarArrayRenderer
        implements BlockEntityRenderer<SolarArrayBlockEntity, SolarArrayRenderer.State> {

    private static final Identifier DECK_TEX = MachineRenderHelper.texture("solar_array_deck");

    /** Deck pivot height: just above the cradle posts (model tops at y=14). */
    private static final float PIVOT_Y = 14.5F / 16.0F;
    /** Deck thickness: a real 1px slab. */
    private static final float THICK = 1.0F / 16.0F;
    /** Max east–west tracking tilt; capped so the descending edge clears the mast bracket. */
    private static final float MAX_TILT = 40.0F;

    /** Render state: the resolved deck tilt (0 = flat/folded). */
    public static class State extends BlockEntityRenderState {
        float angle;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    /**
     * One-block pad (MODELS.md constraint): the deck pivots above the housing and its tilted corners
     * leave the block column. NeoForge-only frustum hook; inert non-override on Fabric/Forge
     * (Nerospace precedent — vanilla {@code BlockEntityRenderer} has no such method).
     */
    public AABB getRenderBoundingBox(SolarArrayBlockEntity array) {
        return new AABB(array.getBlockPos()).inflate(1.0);
    }

    @Override
    public void extractRenderState(SolarArrayBlockEntity array, State state, float partialTick,
            Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(array, state, partialTick, cameraPos, breakProgress);
        Level level = array.getLevel();
        if (level == null || !NeroTechConfig.renderAnimationsEnabled()) {
            state.angle = 0.0F; // static parked frame: deck flat on the cradle
            return;
        }
        // Day-of-time for sun tracking: 0 sunrise, 6000 noon, 18000 midnight (Nerospace recipe).
        long tod = dayOfTime(level);
        float sun = Mth.cos((float) ((tod - 6000L) / 24000.0 * 2.0 * Math.PI)); // +1 noon, -1 midnight
        float openness = Mth.clamp((sun + 0.05F) / 0.3F, 0.0F, 1.0F); // eases to 0 at night → folds flat
        float track = (float) ((tod - 6000L) / 24000.0) * 360.0F; // -90 sunrise .. 0 noon .. +90 sunset
        state.angle = openness * Mth.clamp(track, -MAX_TILT, MAX_TILT);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector,
            CameraRenderState cameraState) {
        int light = MachineRenderHelper.FULL_BRIGHT;
        poseStack.pushPose();
        poseStack.translate(0.5F, PIVOT_Y, 0.5F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(state.angle));
        collector.order(1).submitCustomGeometry(poseStack, RenderTypes.entityCutout(DECK_TEX),
                (pose, c) -> MachineRenderHelper.box(c, pose, light,
                        -0.5F, -THICK / 2.0F, -0.5F, 0.5F, THICK / 2.0F, 0.5F));
        poseStack.popPose();
    }

    /** Once a world's data-driven clock markers are found missing, stop calling the throwing path. */
    private static boolean clockAvailable = true;

    /**
     * Real day-of-time (0..23999). Prefers the 26.x data-driven clock ({@code getDefaultClockTime()});
     * that throws when a world's clock time-markers aren't loaded (seen in some dev/data setups), so
     * we try it once and fall back permanently to the free-running game clock — the deck still
     * animates without erroring (Nerospace's proven fallback).
     */
    private static long dayOfTime(Level level) {
        if (clockAvailable) {
            try {
                return level.getDefaultClockTime() % 24000L;
            } catch (RuntimeException ex) {
                clockAvailable = false;
            }
        }
        return level.getGameTime() % 24000L;
    }
}
