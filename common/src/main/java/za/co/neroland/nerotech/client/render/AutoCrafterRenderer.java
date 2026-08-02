package za.co.neroland.nerotech.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import za.co.neroland.nerotech.config.NeroTechConfig;
import za.co.neroland.nerotech.machine.AutoCrafterBlockEntity;

/**
 * Auto Crafter BER: a spinning, bobbing hologram of the locked template floating above the emitter
 * lens (the Star-Guide hologram recipe), plus a press-stamp plate that drives down over the plunger
 * on every synced craft pulse. The crafted RESULT is not client-resolvable (recipes no longer sync),
 * so the hologram shows the first locked grid item — {@link AutoCrafterBlockEntity#hologramStack()},
 * which rides the BE update tag.
 *
 * <p>Pulse detection: the BE's synced pulse counter is compared against the client-side
 * {@code clientSeenPulse} once per extraction; a change stamps {@code clientPulseTime} and the plate
 * animates for a few ticks from that moment. {@code renderAnimationsEnabled=false} draws the parked
 * plate + a static (non-spinning) hologram.</p>
 */
public class AutoCrafterRenderer
        implements BlockEntityRenderer<AutoCrafterBlockEntity, AutoCrafterRenderer.State> {

    private static final Identifier PRESS_TEX = MachineRenderHelper.texture("auto_crafter_press");

    /** Stamp animation length (ticks). */
    private static final float STAMP_TICKS = 8.0F;
    /** Plate rest height and stroke depth (model px): parks above the plunger cap (y13). */
    private static final float PLATE_REST_Y = 13.5F / 16.0F;
    private static final float PLATE_STROKE = 2.5F / 16.0F;
    private static final float PLATE_HALF = 2.5F / 16.0F;
    private static final float PLATE_THICK = 1.0F / 16.0F;

    public static class State extends BlockEntityRenderState {
        boolean visible;
        float spin;
        float bob;
        float stamp; // 0 = parked, 1 = bottom of the stroke
        final ItemStackRenderState hologram = new ItemStackRenderState();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    /**
     * One-block pad (MODELS.md): the hologram floats above the block. NeoForge-only frustum hook;
     * inert non-override on Fabric/Forge (Nerospace precedent).
     */
    public AABB getRenderBoundingBox(AutoCrafterBlockEntity crafter) {
        return new AABB(crafter.getBlockPos()).inflate(1.0);
    }

    @Override
    public void extractRenderState(AutoCrafterBlockEntity crafter, State state, float partialTick,
            Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(crafter, state, partialTick, cameraPos, breakProgress);
        Level level = crafter.getLevel();
        boolean animate = NeroTechConfig.renderAnimationsEnabled();
        state.stamp = 0.0F;
        state.spin = 0.0F;
        state.bob = 0.0F;

        ItemStack icon = crafter.hologramStack();
        state.visible = level != null && !icon.isEmpty();
        if (level == null) {
            return;
        }
        long tick = level.getGameTime();
        float now = tick + partialTick;

        // Press-stamp pulse: detect a synced counter change once, timestamp it, animate briefly.
        int pulse = crafter.renderPulse();
        if (pulse != crafter.clientSeenPulse) {
            // First sight after (re)load just records the counter — no phantom stamp on chunk load.
            if (crafter.clientSeenPulse != Integer.MIN_VALUE) {
                crafter.clientPulseTime = tick;
            }
            crafter.clientSeenPulse = pulse;
        }
        if (animate && crafter.clientPulseTime != Long.MIN_VALUE) {
            float since = now - crafter.clientPulseTime;
            if (since >= 0.0F && since < STAMP_TICKS) {
                state.stamp = Mth.sin((float) Math.PI * since / STAMP_TICKS); // down and back up
            }
        }

        if (state.visible) {
            if (animate) {
                state.spin = (now * 1.5F) % 360.0F;
                state.bob = Mth.sin(now * 0.06F) * 0.05F;
            }
            Minecraft.getInstance().getItemModelResolver().updateForTopItem(
                    state.hologram, icon, ItemDisplayContext.GROUND, level, null,
                    (int) crafter.getBlockPos().asLong());
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector,
            CameraRenderState cameraState) {
        int light = MachineRenderHelper.FULL_BRIGHT;

        // Press-stamp plate over the central plunger (parked static when idle / animations off).
        float plateY = PLATE_REST_Y - state.stamp * PLATE_STROKE;
        collector.order(1).submitCustomGeometry(poseStack, RenderTypes.entityCutout(PRESS_TEX),
                (pose, c) -> MachineRenderHelper.box(c, pose, light,
                        0.5F - PLATE_HALF, plateY, 0.5F - PLATE_HALF,
                        0.5F + PLATE_HALF, plateY + PLATE_THICK, 0.5F + PLATE_HALF));

        // The template hologram: emissive (its own light source), spinning + bobbing above the lens.
        if (state.visible) {
            poseStack.pushPose();
            poseStack.translate(0.5F, 1.3F + state.bob, 0.5F);
            poseStack.mulPose(Axis.YP.rotationDegrees(state.spin));
            poseStack.scale(0.6F, 0.6F, 0.6F);
            state.hologram.submit(poseStack, collector, light, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }
    }
}
