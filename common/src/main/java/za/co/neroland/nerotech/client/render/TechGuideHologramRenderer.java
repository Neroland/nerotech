package za.co.neroland.nerotech.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import za.co.neroland.nerotech.guide.TechGuideBlockEntity;
import za.co.neroland.nerotech.registry.ModItems;

/**
 * The Tech Guide pedestal hologram (Nerospace's {@code StarGuideHologramRenderer} recipe — the same
 * one NeroTech's machine BERs already borrow): a slowly spinning, bobbing icon floating above a
 * LOADED pedestal showing the nearest player's next incomplete progression step (server-computed,
 * BE-synced) — or the Tech Guide Datapad itself once everything is complete.
 *
 * <p>Registered via the {@link za.co.neroland.nerotech.client.ClientBlockEntityRenderers} seam.
 * Pure client visuals; no player data anywhere near this (POPIA/GDPR n/a).</p>
 */
public class TechGuideHologramRenderer
        implements BlockEntityRenderer<TechGuideBlockEntity, TechGuideHologramRenderState> {

    @Override
    public TechGuideHologramRenderState createRenderState() {
        return new TechGuideHologramRenderState();
    }

    /**
     * Widen the per-BE frustum-cull box upward to cover the hologram, which floats ~1.35 blocks ABOVE
     * the pedestal (and bobs). With the default single-block box it would be dropped whenever the
     * pedestal sits just below the bottom of the view frustum. NeoForge (only) routes the per-BE
     * frustum test through this method; on Fabric and Forge it is an inert unused method, hence no
     * {@code @Override} and a vanilla {@code AABB} (compiles on all six cells) — Nerospace's
     * {@code StarGuideHologramRenderer.getRenderBoundingBox} recipe.
     */
    public AABB getRenderBoundingBox(TechGuideBlockEntity guide) {
        BlockPos p = guide.getBlockPos();
        return new AABB(
                p.getX() - 0.5, p.getY(), p.getZ() - 0.5,
                p.getX() + 1.5, p.getY() + 2.5, p.getZ() + 1.5);
    }

    @Override
    public void extractRenderState(TechGuideBlockEntity guide, TechGuideHologramRenderState state,
            float partialTick, Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(guide, state, partialTick, cameraPos, breakProgress);
        state.visible = guide.hasDatapad() && guide.getLevel() != null;
        if (!state.visible) {
            return;
        }
        float now = guide.getLevel().getGameTime() + partialTick;
        state.spin = (now * 1.5F) % 360.0F;
        state.bob = Mth.sin(now * 0.06F) * 0.05F;

        ItemStack icon = guide.getHologram();
        if (icon.isEmpty()) {
            icon = new ItemStack(ModItems.TECH_GUIDE_DATAPAD.get()); // all complete (or no player near)
        }
        Minecraft.getInstance().getItemModelResolver().updateForTopItem(
                state.renderState, icon, ItemDisplayContext.GROUND, guide.getLevel(), null,
                (int) guide.getBlockPos().asLong());
    }

    @Override
    public void submit(TechGuideHologramRenderState state, PoseStack poseStack,
            SubmitNodeCollector collector, CameraRenderState cameraState) {
        if (!state.visible) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(0.5F, 1.35F + state.bob, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.spin));
        poseStack.scale(0.75F, 0.75F, 0.75F);
        // Emissive: a hologram is its own light source, so render full-bright instead of with the
        // pedestal's world light (it reads pitch-black at night otherwise).
        state.renderState.submit(poseStack, collector, MachineRenderHelper.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }
}
