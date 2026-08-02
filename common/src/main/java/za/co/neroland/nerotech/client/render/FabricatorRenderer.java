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
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import za.co.neroland.nerotech.config.NeroTechConfig;
import za.co.neroland.nerotech.machine.AbstractProcessingBlockEntity;
import za.co.neroland.nerotech.machine.AdvancedFabricatorBlockEntity;
import za.co.neroland.nerotech.machine.NeroTechMachineBlock;

/**
 * Fabricator BER (shared by the Tier-1 and Advanced fabricator BE types): an assembly arm hanging
 * from the gantry bridge, traversing the workbed while a job runs and parking at the west end when
 * idle. Motion uses the quarry-renderer recipe — the traverse target is eased ONCE per game tick
 * (prev/curr stored on the BE) and lerped by {@code partialTick}, so it is FPS-independent and
 * glides to its park position instead of snapping. The Tier-1 model has one bridge (z6–10); the
 * Advanced model has twin gantries (z3–6 / z10–13) carrying two mirrored arms plus the suspended
 * void-crystal quad slowly rotating over the clamp.
 *
 * <p>{@code renderAnimationsEnabled=false} draws the parked static frame (arms at the west end,
 * crystal frozen).</p>
 */
public class FabricatorRenderer
        implements BlockEntityRenderer<AbstractProcessingBlockEntity, FabricatorRenderer.State> {

    private static final Identifier ARM_TEX = MachineRenderHelper.texture("fabricator_arm");
    /** The void-crystal reuses the fusion plasma sprite (a plasma-field crystal). */
    private static final Identifier CRYSTAL_TEX = MachineRenderHelper.texture("fusion_reactor_plasma");

    /** Arm traverse range along X (the bridge beam spans x2..14; the head stays clear of the posts). */
    private static final float PARK_X = 3.5F / 16.0F;
    private static final float CENTRE_X = 8.0F / 16.0F;
    private static final float SWEEP = 4.5F / 16.0F;
    /** Traverse sweep speed (radians per tick into the sine). */
    private static final float SWEEP_SPEED = 0.15F;
    /** Per-tick easing factor toward the target (quarry recipe). */
    private static final double EASE = 0.35;

    /** Tier-1 bridge underside/edges: beam at y11–13, z6–10. */
    private static final float BRIDGE_Y = 11.0F / 16.0F;
    private static final float T1_ARM_Z = 8.0F / 16.0F;
    /** Advanced twin gantry arm rails (z3–6 and z10–13 centres). */
    private static final float ADV_ARM_ZA = 4.5F / 16.0F;
    private static final float ADV_ARM_ZB = 11.5F / 16.0F;
    /** Workbed top (y5–7 slab): the tool tip hovers just above it. */
    private static final float BED_TOP = 7.0F / 16.0F;

    public static class State extends BlockEntityRenderState {
        boolean active;
        boolean advanced;
        float armX;
        float crystalSpin;
        Direction facing = Direction.NORTH;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    /** One-block pad (MODELS.md); NeoForge-only frustum hook, inert non-override on Fabric/Forge. */
    public AABB getRenderBoundingBox(AbstractProcessingBlockEntity fabricator) {
        return new AABB(fabricator.getBlockPos()).inflate(1.0);
    }

    @Override
    public void extractRenderState(AbstractProcessingBlockEntity fabricator, State state,
            float partialTick, Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(fabricator, state, partialTick, cameraPos, breakProgress);
        Level level = fabricator.getLevel();
        state.advanced = fabricator instanceof AdvancedFabricatorBlockEntity;
        state.active = fabricator.renderActive() && level != null;
        state.facing = fabricator.getBlockState().getValue(NeroTechMachineBlock.FACING);
        if (level == null || !NeroTechConfig.renderAnimationsEnabled()) {
            state.armX = PARK_X; // static parked frame
            state.crystalSpin = 0.0F;
            return;
        }
        long tick = level.getGameTime();
        double now = tick + partialTick;
        state.crystalSpin = (float) ((now * 2.0) % 360.0); // slow, continuous

        // Traverse target: a sine sweep across the bed while running, the park end when idle.
        double target = state.active ? CENTRE_X + Math.sin(now * SWEEP_SPEED) * SWEEP : PARK_X;
        if (!fabricator.displayInit) {
            fabricator.displayPos = fabricator.prevDisplayPos = target;
            fabricator.displayInit = true;
            fabricator.displayLastTick = tick;
        } else if (tick != fabricator.displayLastTick) {
            // Ease ONCE per tick (FPS-independent), saving the previous-tick position so the lerp
            // below interpolates across the tick — smooth at any frame rate (quarry recipe).
            fabricator.displayLastTick = tick;
            fabricator.prevDisplayPos = fabricator.displayPos;
            fabricator.displayPos += (target - fabricator.displayPos) * EASE;
        }
        state.armX = (float) (fabricator.prevDisplayPos
                + (fabricator.displayPos - fabricator.prevDisplayPos) * partialTick);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector,
            CameraRenderState cameraState) {
        int light = MachineRenderHelper.FULL_BRIGHT;
        poseStack.pushPose();
        MachineRenderHelper.rotateToFacing(poseStack, state.facing);

        float armX = state.armX;
        collector.order(1).submitCustomGeometry(poseStack, RenderTypes.entityCutout(ARM_TEX),
                (pose, c) -> {
                    if (state.advanced) {
                        // Twin arms on the twin gantries, the second mirrored across the bed centre.
                        arm(c, pose, light, armX, ADV_ARM_ZA);
                        arm(c, pose, light, 2.0F * CENTRE_X - armX, ADV_ARM_ZB);
                    } else {
                        arm(c, pose, light, armX, T1_ARM_Z);
                    }
                });

        // Advanced: the suspended void-crystal quad slowly rotating below the clamp (always present;
        // its spin freezes into the static frame when animations are off).
        if (state.advanced) {
            poseStack.pushPose();
            poseStack.translate(CENTRE_X, 8.75F / 16.0F, 8.0F / 16.0F);
            poseStack.mulPose(Axis.YP.rotationDegrees(state.crystalSpin));
            collector.order(2).submitCustomGeometry(poseStack, RenderTypes.entityCutout(CRYSTAL_TEX),
                    (pose, c) -> {
                        float r = 1.5F / 16.0F;
                        MachineRenderHelper.face(c, pose, light, 0xFFFFFF, 0, 0, -1,
                                -r, -r, 0, 0, 1, -r, r, 0, 0, 0, r, r, 0, 1, 0, r, -r, 0, 1, 1);
                        MachineRenderHelper.face(c, pose, light, 0xFFFFFF, -1, 0, 0,
                                0, -r, -r, 0, 1, 0, r, -r, 0, 0, 0, r, r, 1, 0, 0, -r, r, 1, 1);
                    });
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    /**
     * One assembly arm at traverse position {@code x}, rail centre {@code z}: carriage riding the
     * bridge underside, drop strut, and the tool head hovering just above the workbed.
     */
    private static void arm(VertexConsumer c, PoseStack.Pose pose, int light, float x, float z) {
        float px = 1.0F / 16.0F; // one model pixel
        // Carriage under the bridge (bridge underside at y=11).
        MachineRenderHelper.box(c, pose, light,
                x - 1.5F * px, BRIDGE_Y - 1.5F * px, z - 1.5F * px,
                x + 1.5F * px, BRIDGE_Y + 0.5F * px, z + 1.5F * px);
        // Drop strut down toward the bed.
        MachineRenderHelper.box(c, pose, light,
                x - 0.5F * px, BED_TOP + 1.0F * px, z - 0.5F * px,
                x + 0.5F * px, BRIDGE_Y - 1.5F * px, z + 0.5F * px);
        // Tool head, hovering a hair above the workbed surface.
        MachineRenderHelper.box(c, pose, light,
                x - 1.0F * px, BED_TOP + 0.2F * px, z - 1.0F * px,
                x + 1.0F * px, BED_TOP + 1.0F * px, z + 1.0F * px);
    }
}
