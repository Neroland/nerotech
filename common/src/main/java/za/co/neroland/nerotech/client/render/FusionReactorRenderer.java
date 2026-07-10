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
import za.co.neroland.nerotech.machine.FusionReactorBlockEntity;
import za.co.neroland.nerotech.machine.NeroTechMachineBlock;

/**
 * Fusion Reactor BER — Stage E multiblock form: when the 3³/5³/7³ shell is <b>formed</b>, a
 * plasma torus spins at the shell's interior centre (crossed quads around the vertical axis,
 * scaled with the shell, visible through the containment glass), coloured by the MODELS.md heat
 * lerp and dimmed when idle-hot; once heat crosses the throttle threshold the viewport carries
 * the alternating H_CRIT meltdown-telegraph strobe. <b>Unformed = dark</b>: nothing dynamic
 * renders at all (the inert-until-formed identity).
 *
 * <p>{@code renderAnimationsEnabled=false} freezes the torus and holds the strobe steady-on
 * while overheated, so the danger stays readable as a static frame.</p>
 */
public class FusionReactorRenderer
        implements BlockEntityRenderer<FusionReactorBlockEntity, FusionReactorRenderer.State> {

    private static final Identifier PLASMA_TEX = MachineRenderHelper.texture("fusion_reactor_plasma");

    /** Torus spin speed (degrees per tick). */
    private static final float SPIN_SPEED = 10.0F;
    /** Strobe half-period (ticks): 4 on, 4 off. */
    private static final long STROBE_PERIOD = 4L;

    public static class State extends BlockEntityRenderState {
        boolean formed;
        int shellSize;
        boolean active;
        float heat;
        float spin;
        boolean strobe;
        Direction facing = Direction.NORTH;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    /**
     * Covers the whole shell when formed (the torus draws up to (size-1)/2 blocks behind the
     * controller). NeoForge-only frustum hook, inert non-override on Fabric/Forge (MODELS.md).
     */
    public AABB getRenderBoundingBox(FusionReactorBlockEntity reactor) {
        int pad = Math.max(1, reactor.renderShellSize());
        return new AABB(reactor.getBlockPos()).inflate(pad);
    }

    @Override
    public void extractRenderState(FusionReactorBlockEntity reactor, State state, float partialTick,
            Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(reactor, state, partialTick, cameraPos, breakProgress);
        Level level = reactor.getLevel();
        state.formed = reactor.renderFormed();
        state.shellSize = reactor.renderShellSize();
        state.heat = reactor.heatFraction();
        state.facing = reactor.getBlockState().getValue(NeroTechMachineBlock.FACING);
        boolean overheated = reactor.overheated();
        // Explicit null check (not folded into a flag) so ecj's null-flow analysis can track it.
        if (level == null) {
            state.active = false;
            state.spin = 0.0F;
            state.strobe = false;
            return;
        }
        state.active = reactor.renderActive();
        if (NeroTechConfig.renderAnimationsEnabled()) {
            float now = level.getGameTime() + partialTick;
            state.spin = (now * SPIN_SPEED) % 360.0F;
            state.strobe = overheated && (level.getGameTime() / STROBE_PERIOD) % 2L == 0L;
        } else {
            state.spin = 0.0F;
            state.strobe = overheated; // static frame: hold the warning steady-on
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector,
            CameraRenderState cameraState) {
        if (!state.formed) {
            return; // inert until formed: the dark static shell is the whole machine
        }
        boolean plasma = state.active || state.heat > 0.02F;
        if (!plasma && !state.strobe) {
            return; // cold, dark, safe
        }
        int light = MachineRenderHelper.FULL_BRIGHT;
        poseStack.pushPose();
        MachineRenderHelper.rotateToFacing(poseStack, state.facing);

        // Plasma torus at the interior centre, scaled with the shell: crossed vertical quads +
        // a horizontal disc spinning around Y. Colour follows the heat lerp; an idle-but-hot
        // core dims to ~45% so brightness reads as output.
        if (plasma) {
            int rgb = MachineRenderHelper.heatColor(state.heat);
            if (!state.active) {
                rgb = ((MachineRenderHelper.red(rgb) * 115 / 255) << 16)
                        | ((MachineRenderHelper.green(rgb) * 115 / 255) << 8)
                        | (MachineRenderHelper.blue(rgb) * 115 / 255);
            }
            int torusRgb = rgb;
            // Interior centre in facing-local coords: (size-1)/2 blocks behind the controller
            // (local +Z after rotateToFacing puts the front face on local north/-Z).
            float inward = (state.shellSize - 1) / 2.0F;
            // Torus radius scales with the hollow interior ((size-2) blocks across).
            float radius = Math.max(0.25F, (state.shellSize - 2) * 0.38F);
            poseStack.pushPose();
            poseStack.translate(0.5F, 0.5F, 0.5F + inward);
            poseStack.mulPose(Axis.YP.rotationDegrees(state.spin));
            float r = radius;
            collector.order(1).submitCustomGeometry(poseStack, RenderTypes.entityCutout(PLASMA_TEX),
                    (pose, c) -> {
                        // Two crossed vertical quads...
                        MachineRenderHelper.face(c, pose, light, torusRgb, 0, 0, -1,
                                -r, -r, 0, 0, 1,
                                -r, r, 0, 0, 0,
                                r, r, 0, 1, 0,
                                r, -r, 0, 1, 1);
                        MachineRenderHelper.face(c, pose, light, torusRgb, -1, 0, 0,
                                0, -r, -r, 0, 1,
                                0, r, -r, 0, 0,
                                0, r, r, 1, 0,
                                0, -r, r, 1, 1);
                        // ...and the horizontal disc for the toroid read.
                        float h = r * 0.85F;
                        MachineRenderHelper.face(c, pose, light, torusRgb, 0, 1, 0,
                                -h, 0, -h, 0, 1,
                                -h, 0, h, 0, 0,
                                h, 0, h, 1, 0,
                                h, 0, -h, 1, 1);
                    });
            poseStack.popPose();
        }

        // Warning strobe: an H_CRIT overlay across the controller's viewport window (meltdown
        // telegraph — visible from the operator's side regardless of shell size).
        if (state.strobe) {
            collector.order(2).submitCustomGeometry(poseStack, RenderTypes.entityCutout(PLASMA_TEX),
                    (pose, c) -> MachineRenderHelper.face(c, pose, light, MachineRenderHelper.CRIT_RGB,
                            0, 0, -1,
                            5.0F / 16.0F, 5.0F / 16.0F, 0.5F / 16.0F, 0, 1,
                            5.0F / 16.0F, 12.0F / 16.0F, 0.5F / 16.0F, 0, 0,
                            11.0F / 16.0F, 12.0F / 16.0F, 0.5F / 16.0F, 1, 0,
                            11.0F / 16.0F, 5.0F / 16.0F, 0.5F / 16.0F, 1, 1));
        }
        poseStack.popPose();
    }
}
