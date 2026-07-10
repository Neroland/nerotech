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
import za.co.neroland.nerotech.machine.NeroGeneratorBlockEntity;
import za.co.neroland.nerotech.machine.NeroTechMachineBlock;

/**
 * Nero Generator BER: a turbine ring spinning behind the front intake grille while a fuel charge is
 * burning, over a heat-lerped glow plate (MODELS.md {@code T_CYAN → H_WARN → H_CRIT}). The block
 * model leaves the grille window open (x4–12, y2–10, behind the two slats at z0–1) for exactly these
 * parts. Idle + cold draws nothing; idle + hot keeps the static glow so residual heat stays readable.
 *
 * <p>Sync discipline: extraction reads only the BE's synced render surface ({@code renderActive},
 * {@code heatFraction}); with {@code renderAnimationsEnabled=false} the rotor draws as a static
 * parked frame (spin frozen at 0).</p>
 */
public class NeroGeneratorRenderer
        implements BlockEntityRenderer<NeroGeneratorBlockEntity, NeroGeneratorRenderer.State> {

    private static final Identifier ROTOR_TEX = MachineRenderHelper.texture("nero_generator_rotor");

    /** Rotor plane depth in the grille recess (between the z0–1 slats and the z=2 body face). */
    private static final float ROTOR_Z = 1.5F / 16.0F;
    /** Glow plate sits just in front of the body face, behind the rotor. */
    private static final float GLOW_Z = 1.9F / 16.0F;
    /** Rotor half-size: a 7×7px ring quad inside the 8×8px window. */
    private static final float ROTOR_R = 3.5F / 16.0F;
    /** Rotor/window centre (x8, y6 in model px). */
    private static final float CENTRE_X = 8.0F / 16.0F;
    private static final float CENTRE_Y = 6.0F / 16.0F;
    /** Turbine spin speed (degrees per tick) — constant read as "running". */
    private static final float SPIN_SPEED = 24.0F;

    /** Render state: everything extracted from the BE + clocks, nothing read at submit time. */
    public static class State extends BlockEntityRenderState {
        boolean active;
        float heat;
        float spin;
        Direction facing = Direction.NORTH;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    /**
     * Widen the per-BE frustum-cull box by one block (MODELS.md constraint). NeoForge (only) routes
     * the per-BE frustum test through this method; on Fabric and Forge it is an inert non-override —
     * vanilla's {@code BlockEntityRenderer} has no such method — and {@code AABB} is vanilla, so it
     * compiles on all six cells (Nerospace precedent).
     */
    public AABB getRenderBoundingBox(NeroGeneratorBlockEntity generator) {
        return new AABB(generator.getBlockPos()).inflate(1.0);
    }

    @Override
    public void extractRenderState(NeroGeneratorBlockEntity generator, State state, float partialTick,
            Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(generator, state, partialTick, cameraPos, breakProgress);
        Level level = generator.getLevel();
        state.heat = generator.heatFraction();
        state.facing = generator.getBlockState().getValue(NeroTechMachineBlock.FACING);
        // Explicit null check (not folded into the flag) so ecj's null-flow analysis can track it.
        if (level == null) {
            state.active = false;
            state.spin = 0.0F;
            return;
        }
        state.active = generator.renderActive();
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
        if (!glow) {
            return; // idle + cold: the static block model is the whole machine
        }
        int light = MachineRenderHelper.FULL_BRIGHT;
        int heatRgb = MachineRenderHelper.heatColor(state.heat);

        poseStack.pushPose();
        MachineRenderHelper.rotateToFacing(poseStack, state.facing);

        // Heat-lerped glow plate at the back of the grille recess (static — readable even parked).
        collector.order(1).submitCustomGeometry(poseStack, RenderTypes.entityCutout(ROTOR_TEX),
                (pose, c) -> MachineRenderHelper.face(c, pose, light, heatRgb, 0, 0, -1,
                        CENTRE_X - ROTOR_R - 1.0F / 16.0F, CENTRE_Y - ROTOR_R, GLOW_Z, 0, 1,
                        CENTRE_X - ROTOR_R - 1.0F / 16.0F, CENTRE_Y + ROTOR_R, GLOW_Z, 0, 0,
                        CENTRE_X + ROTOR_R + 1.0F / 16.0F, CENTRE_Y + ROTOR_R, GLOW_Z, 1, 0,
                        CENTRE_X + ROTOR_R + 1.0F / 16.0F, CENTRE_Y - ROTOR_R, GLOW_Z, 1, 1));

        // Turbine ring, spinning in the grille plane while burning (parked at 0° when animations off).
        if (state.active) {
            poseStack.pushPose();
            poseStack.translate(CENTRE_X, CENTRE_Y, ROTOR_Z);
            poseStack.mulPose(Axis.ZP.rotationDegrees(state.spin));
            collector.order(2).submitCustomGeometry(poseStack, RenderTypes.entityCutout(ROTOR_TEX),
                    (pose, c) -> MachineRenderHelper.face(c, pose, light, 0xFFFFFF, 0, 0, -1,
                            -ROTOR_R, -ROTOR_R, 0, 0, 1,
                            -ROTOR_R, ROTOR_R, 0, 0, 0,
                            ROTOR_R, ROTOR_R, 0, 1, 0,
                            ROTOR_R, -ROTOR_R, 0, 1, 1));
            poseStack.popPose();
        }
        poseStack.popPose();
    }
}
