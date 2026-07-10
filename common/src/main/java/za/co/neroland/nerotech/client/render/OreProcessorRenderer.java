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
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import za.co.neroland.nerotech.config.NeroTechConfig;
import za.co.neroland.nerotech.machine.AbstractProcessingBlockEntity;
import za.co.neroland.nerotech.machine.AdvancedOreProcessorBlockEntity;
import za.co.neroland.nerotech.machine.NeroTechMachineBlock;

/**
 * Ore Processor BER (shared by the Tier-1 and Advanced processor BE types): twin crusher drums
 * spinning inside the open-topped drum housing while a recipe is progressing — the model's side
 * walls (x1–3 / x13–15) and back (z13–15) leave the interior throat open for exactly these drums.
 * The Advanced variant additionally draws a plasma arc flickering across the throat.
 *
 * <p>Idle machines keep their drums PARKED (static, visible through the open top — "idle machines
 * visibly rest"); only the spin and the arc are gated on the synced active flag, and
 * {@code renderAnimationsEnabled=false} freezes both into the static frame.</p>
 */
public class OreProcessorRenderer
        implements BlockEntityRenderer<AbstractProcessingBlockEntity, OreProcessorRenderer.State> {

    private static final Identifier DRUM_TEX = MachineRenderHelper.texture("ore_processor_drum");
    /** The Advanced plasma arc reuses the fusion plasma sprite (MODELS.md allowance). */
    private static final Identifier PLASMA_TEX = MachineRenderHelper.texture("fusion_reactor_plasma");

    /** Drum axis runs north–south through the housing (interior z4..13). */
    private static final float DRUM_Z0 = 4.5F / 16.0F;
    private static final float DRUM_Z1 = 12.5F / 16.0F;
    /** Twin drum centres and radius (interior x3..13, crushing height y≈9). */
    private static final float DRUM_XA = 5.5F / 16.0F;
    private static final float DRUM_XB = 10.5F / 16.0F;
    private static final float DRUM_Y = 9.0F / 16.0F;
    private static final float DRUM_R = 2.0F / 16.0F;
    /** Counter-rotating drum spin speed (degrees per tick). */
    private static final float SPIN_SPEED = 20.0F;

    public static class State extends BlockEntityRenderState {
        boolean active;
        boolean advanced;
        float spin;
        boolean arcFlick;
        Direction facing = Direction.NORTH;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    /** One-block pad (MODELS.md); NeoForge-only frustum hook, inert non-override on Fabric/Forge. */
    public AABB getRenderBoundingBox(AbstractProcessingBlockEntity processor) {
        return new AABB(processor.getBlockPos()).inflate(1.0);
    }

    @Override
    public void extractRenderState(AbstractProcessingBlockEntity processor, State state,
            float partialTick, Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(processor, state, partialTick, cameraPos, breakProgress);
        Level level = processor.getLevel();
        state.advanced = processor instanceof AdvancedOreProcessorBlockEntity;
        state.facing = processor.getBlockState().getValue(NeroTechMachineBlock.FACING);
        // Explicit null check (not folded into the flag) so ecj's null-flow analysis can track it.
        if (level == null) {
            state.active = false;
            state.spin = 0.0F;
            state.arcFlick = false;
            return;
        }
        state.active = processor.renderActive();
        if (state.active && NeroTechConfig.renderAnimationsEnabled()) {
            float now = level.getGameTime() + partialTick;
            state.spin = (now * SPIN_SPEED) % 360.0F;
            state.arcFlick = (level.getGameTime() & 3L) < 2L; // cheap 2-on/2-off flicker
        } else {
            state.spin = 0.0F; // parked drums (idle, or static frame with animations off)
            state.arcFlick = false;
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector,
            CameraRenderState cameraState) {
        int light = MachineRenderHelper.FULL_BRIGHT;
        poseStack.pushPose();
        MachineRenderHelper.rotateToFacing(poseStack, state.facing);

        // Twin drums, counter-rotating about the north–south axis (parked at 0/45° when idle).
        drum(poseStack, collector, light, DRUM_XA, state.spin);
        drum(poseStack, collector, light, DRUM_XB, 45.0F - state.spin);

        // Advanced: plasma arc flickering across the throat above the drums, only while running.
        if (state.advanced && state.active) {
            int rgb = state.arcFlick ? 0xFFFFFF : 0x82F8FF; // T_PLASMA dip between flashes
            collector.order(2).submitCustomGeometry(poseStack, RenderTypes.entityCutout(PLASMA_TEX),
                    (pose, c) -> MachineRenderHelper.face(c, pose, light, rgb, 0, 0, -1,
                            4.0F / 16.0F, 10.0F / 16.0F, 8.5F / 16.0F, 0, 1,
                            4.0F / 16.0F, 12.5F / 16.0F, 8.5F / 16.0F, 0, 0,
                            12.0F / 16.0F, 12.5F / 16.0F, 8.5F / 16.0F, 1, 0,
                            12.0F / 16.0F, 10.0F / 16.0F, 8.5F / 16.0F, 1, 1));
        }
        poseStack.popPose();
    }

    /** One crusher drum: a textured box spun about its long (Z) axis at {@code xCentre}. */
    private static void drum(PoseStack poseStack, SubmitNodeCollector collector, int light,
            float xCentre, float spin) {
        poseStack.pushPose();
        poseStack.translate(xCentre, DRUM_Y, (DRUM_Z0 + DRUM_Z1) / 2.0F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(spin));
        float halfLen = (DRUM_Z1 - DRUM_Z0) / 2.0F;
        collector.order(1).submitCustomGeometry(poseStack, RenderTypes.entityCutout(DRUM_TEX),
                (pose, c) -> MachineRenderHelper.box(c, pose, light,
                        -DRUM_R, -DRUM_R, -halfLen, DRUM_R, DRUM_R, halfLen));
        poseStack.popPose();
    }
}
