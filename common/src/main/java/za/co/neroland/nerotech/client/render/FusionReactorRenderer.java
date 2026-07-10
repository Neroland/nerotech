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
 * Fusion Reactor BER: the plasma torus spinning in the north viewport recess (the model's window at
 * x5–11, y5–12 between the front frame columns) as two crossed plasma quads, coloured by the
 * MODELS.md heat lerp and dimmed when the reactor idles hot; plus the meltdown-telegraph warning
 * strobe — an alternating H_CRIT overlay across the viewport once heat crosses the throttle
 * threshold (the synced heat bucket + the server-synced threshold config make this client-exact).
 *
 * <p>{@code renderAnimationsEnabled=false} freezes the torus and holds the strobe steady-on while
 * overheated, so the danger stays readable as a static frame.</p>
 */
public class FusionReactorRenderer
        implements BlockEntityRenderer<FusionReactorBlockEntity, FusionReactorRenderer.State> {

    private static final Identifier PLASMA_TEX = MachineRenderHelper.texture("fusion_reactor_plasma");

    /** Viewport window centre + torus radius (window x5–11, y5–12, recess depth z0–2). */
    private static final float CENTRE_X = 8.0F / 16.0F;
    private static final float CENTRE_Y = 8.5F / 16.0F;
    private static final float TORUS_Z = 1.0F / 16.0F;
    private static final float TORUS_R = 2.5F / 16.0F;
    /** Torus spin speed (degrees per tick). */
    private static final float SPIN_SPEED = 10.0F;
    /** Strobe half-period (ticks): 4 on, 4 off. */
    private static final long STROBE_PERIOD = 4L;

    public static class State extends BlockEntityRenderState {
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

    /** One-block pad (MODELS.md); NeoForge-only frustum hook, inert non-override on Fabric/Forge. */
    public AABB getRenderBoundingBox(FusionReactorBlockEntity reactor) {
        return new AABB(reactor.getBlockPos()).inflate(1.0);
    }

    @Override
    public void extractRenderState(FusionReactorBlockEntity reactor, State state, float partialTick,
            Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(reactor, state, partialTick, cameraPos, breakProgress);
        Level level = reactor.getLevel();
        state.active = reactor.renderActive() && level != null;
        state.heat = reactor.heatFraction();
        state.facing = reactor.getBlockState().getValue(NeroTechMachineBlock.FACING);
        boolean overheated = reactor.overheated();
        if (level != null && NeroTechConfig.renderAnimationsEnabled()) {
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
        boolean plasma = state.active || state.heat > 0.02F;
        if (!plasma && !state.strobe) {
            return; // cold, dark, safe: the static shell is the whole machine
        }
        int light = MachineRenderHelper.FULL_BRIGHT;
        poseStack.pushPose();
        MachineRenderHelper.rotateToFacing(poseStack, state.facing);

        // Plasma torus: two crossed quads spinning in the viewport plane. Colour follows the heat
        // lerp; an idle-but-hot core dims to ~45% so brightness reads as output.
        if (plasma) {
            int rgb = MachineRenderHelper.heatColor(state.heat);
            if (!state.active) {
                rgb = ((MachineRenderHelper.red(rgb) * 115 / 255) << 16)
                        | ((MachineRenderHelper.green(rgb) * 115 / 255) << 8)
                        | (MachineRenderHelper.blue(rgb) * 115 / 255);
            }
            int torusRgb = rgb;
            poseStack.pushPose();
            poseStack.translate(CENTRE_X, CENTRE_Y, TORUS_Z);
            poseStack.mulPose(Axis.ZP.rotationDegrees(state.spin));
            collector.order(1).submitCustomGeometry(poseStack, RenderTypes.entityCutout(PLASMA_TEX),
                    (pose, c) -> {
                        MachineRenderHelper.face(c, pose, light, torusRgb, 0, 0, -1,
                                -TORUS_R, -TORUS_R, 0, 0, 1,
                                -TORUS_R, TORUS_R, 0, 0, 0,
                                TORUS_R, TORUS_R, 0, 1, 0,
                                TORUS_R, -TORUS_R, 0, 1, 1);
                        // The crossed second quad, 45° out of phase for a volumetric read.
                        float r = TORUS_R * 0.7071F;
                        MachineRenderHelper.face(c, pose, light, torusRgb, 0, 0, -1,
                                -r, -r, -0.02F, 0, 1,
                                -r, r, -0.02F, 0, 0,
                                r, r, -0.02F, 1, 0,
                                r, -r, -0.02F, 1, 1);
                    });
            poseStack.popPose();
        }

        // Warning strobe: an H_CRIT overlay across the whole viewport window (meltdown telegraph).
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
