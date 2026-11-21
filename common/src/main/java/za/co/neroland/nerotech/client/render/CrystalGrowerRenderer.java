package za.co.neroland.nerotech.client.render;

import java.util.Map;
import java.util.WeakHashMap;

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
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import za.co.neroland.nerotech.machine.NeroTechMachineBlock;
import za.co.neroland.nerotech.machine.components.CrystalGrowerBlockEntity;

/**
 * Crystal Grower BER: the bell jar's glass and the crystal hanging point-down from the seed clamp.
 * The crystal grows in six steps with the synced {@code progressFraction} and turns slowly while it
 * grows. With no job the clamp hangs bare. HOT swaps the glass for a fogged sprite and shimmers the
 * cap.
 *
 * <p>This is the one machine that keeps working without power: the lamp goes dark, the crystal
 * keeps growing, slowly. The server only syncs a progress rate in the WORK readout, so while the
 * grower is unpowered but still active the renderer creeps the shown growth forward itself at a
 * fixed slow rate, held just short of full, and snaps to the real value whenever a sync arrives.
 * The creep is a visual stand-in, not the true rate.</p>
 *
 * <p>The glass is drawn here rather than in the block model so the jar needs no translucent or
 * cutout block layer on any loader: five cutout quads with a binary-alpha sprite. It is the only
 * geometry drawn at REST. {@code renderAnimationsEnabled=false}: no spin and no creep, the crystal
 * shows at its last synced size. Reads only the synced render surface; no player data.</p>
 */
public class CrystalGrowerRenderer
        implements BlockEntityRenderer<CrystalGrowerBlockEntity, CrystalGrowerRenderer.State> {

    /** The status lamp socket: the {@code status_lamp_socket} element of {@code models/block/crystal_grower.json} (pixels). */
    private static final StatusLampRenderer.Socket SOCKET = StatusLampRenderer.Socket.model(1.0F, 12.0F, 0.5F, 3.0F, 14.0F, 1.0F);

    private static final Identifier GLASS_TEX = MachineRenderHelper.texture("crystal_grower_glass");
    private static final Identifier FOG_TEX = MachineRenderHelper.texture("crystal_grower_fog");
    private static final Identifier CRYSTAL_TEX = MachineRenderHelper.texture("crystal_grower_crystal");

    private static final float PX = 1.0F / 16.0F;
    private static final float CENTRE = 8.0F * PX;
    /** Jar panes sit in the middle of the corner ribs (model px 4.5 .. 11.5, y 5 .. 14). */
    private static final float JAR_MIN = 4.5F * PX;
    private static final float JAR_MAX = 11.5F * PX;
    private static final float JAR_Y0 = 5.0F * PX;
    private static final float JAR_Y1 = 14.0F * PX;
    /** The clamp's lower end: the crystal hangs from here. */
    private static final float CLAMP_Y = 11.5F * PX;
    private static final int STEPS = 6;
    private static final float FULL_HEIGHT = 6.3F * PX;
    private static final float FULL_HALF_WIDTH = 2.8F * PX;
    /** Fallback unpowered creep per tick (before the first rate sync) and the ceiling between syncs. */
    private static final float CREEP = 1.0F / 6000.0F;
    private static final float CREEP_CAP = 0.97F;
    private static final int GLASS_RGB = 0xA8C4CC;
    private static final int CRYSTAL_LIT_RGB = 0xFFFFFF;
    private static final int CRYSTAL_DARK_RGB = 0x9FB4C0;

    /** Client-only display scratch per grower (render thread; entries go when the BE is collected). */
    private static final class Growth {
        float lastSync = -1.0F;
        float shown;
        float prevShown;
        long lastTick = Long.MIN_VALUE;
    }

    private final Map<CrystalGrowerBlockEntity, Growth> growths = new WeakHashMap<>();

    public static class State extends BlockEntityRenderState {
        final StatusLampRenderer.Lamp lamp = new StatusLampRenderer.Lamp();
        Direction facing = Direction.NORTH;
        float growth;
        float spin;
        boolean passive;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    /** One-block pad (the HOT shimmer stands above the cap); NeoForge-only frustum hook. */
    public AABB getRenderBoundingBox(CrystalGrowerBlockEntity grower) {
        return new AABB(grower.getBlockPos()).inflate(1.0);
    }

    @Override
    public void extractRenderState(CrystalGrowerBlockEntity grower, State state, float partialTick,
            Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(grower, state, partialTick, cameraPos, breakProgress);
        StatusLampRenderer.extract(grower, state.lamp, partialTick);
        state.facing = grower.getBlockState().getValue(NeroTechMachineBlock.FACING);
        float synced = grower.progressFraction(partialTick);
        // Unpowered but still growing: lamp dark (NO_ENERGY) while the active flag stays set.
        state.passive = state.lamp.dark && grower.renderActive();
        Level level = grower.getLevel();
        if (level == null || !state.lamp.animate || !state.passive) {
            state.growth = synced;
            state.spin = StatusLampRenderer.moving(state.lamp) ? state.lamp.time * 0.6F : 30.0F;
            if (!state.passive) {
                this.growths.remove(grower);
            }
            return;
        }
        Growth g = this.growths.computeIfAbsent(grower, k -> new Growth());
        long tick = level.getGameTime();
        if (synced != g.lastSync) {
            // A sync arrived (new operation, or the first sight): take the real value.
            g.lastSync = synced;
            g.shown = synced;
            g.prevShown = synced;
            g.lastTick = tick;
        } else if (tick != g.lastTick) {
            long elapsed = Math.min(200L, Math.max(1L, tick - g.lastTick));
            g.lastTick = tick;
            g.prevShown = g.shown;
            // C1 extra: the synced unpowered rate; the fallback creep until the first sync arrives.
            float perTick = grower.passiveGrowthPerTick();
            g.shown = Math.max(g.shown, Math.min(CREEP_CAP, g.shown + (perTick > 0.0F ? perTick : CREEP) * elapsed));
        }
        state.growth = g.prevShown + (g.shown - g.prevShown) * partialTick;
        state.spin = state.lamp.time * 0.3F;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector,
            CameraRenderState cameraState) {
        poseStack.pushPose();
        MachineRenderHelper.rotateToFacing(poseStack, state.facing);
        // The instrument post's face is set back one pixel from the block edge.
        StatusLampRenderer.submit(state.lamp, poseStack, collector, SOCKET);
        StatusLampRenderer.submitShimmer(state.lamp, poseStack, collector,
                4.0F * PX, 4.0F * PX, 12.0F * PX, 12.0F * PX, 1.0F);

        boolean growing = state.growth > 0.0F || state.passive;
        if (growing && state.lamp.readout != StatusLampRenderer.REST) {
            int step = Mth.clamp(1 + (int) (state.growth * STEPS), 1, STEPS);
            float size = step / (float) STEPS;
            float height = FULL_HEIGHT * size;
            float half = FULL_HALF_WIDTH * (0.3F + 0.7F * size);
            int rgb = state.passive ? CRYSTAL_DARK_RGB : CRYSTAL_LIT_RGB;
            poseStack.pushPose();
            poseStack.translate(CENTRE, CLAMP_Y, CENTRE);
            //? if >=26.3 {
            /*poseStack.rotate(Axis.YP.rotationDegrees(state.spin));
            *///?} else {
            poseStack.mulPose(Axis.YP.rotationDegrees(state.spin));
            //?}
            collector.order(1).submitCustomGeometry(poseStack, RenderTypes.entityCutout(CRYSTAL_TEX),
                    (pose, c) -> crystal(c, pose, rgb, half, height));
            poseStack.popPose();
        }

        // Glass last, outside the crystal. HOT: the jar fogs.
        Identifier glass = StatusLampRenderer.hot(state.lamp) ? FOG_TEX : GLASS_TEX;
        collector.order(2).submitCustomGeometry(poseStack, RenderTypes.entityCutout(glass),
                (pose, c) -> jar(c, pose));
        poseStack.popPose();
    }

    /** Two crossed quads hanging down from the origin; the sprite's top edge is at the clamp. */
    private static void crystal(VertexConsumer c, PoseStack.Pose pose, int rgb, float half, float height) {
        int light = MachineRenderHelper.FULL_BRIGHT;
        MachineRenderHelper.face(c, pose, light, rgb, 0, 0, -1,
                -half, -height, 0, 0, 1, -half, 0, 0, 0, 0, half, 0, 0, 1, 0, half, -height, 0, 1, 1);
        MachineRenderHelper.face(c, pose, light, rgb, -1, 0, 0,
                0, -height, -half, 0, 1, 0, 0, -half, 0, 0, 0, 0, half, 1, 0, 0, -height, half, 1, 1);
    }

    /** The four jar panes between the corner ribs (the cap closes the top). */
    private static void jar(VertexConsumer c, PoseStack.Pose pose) {
        int light = MachineRenderHelper.FULL_BRIGHT;
        float a = JAR_MIN;
        float b = JAR_MAX;
        MachineRenderHelper.face(c, pose, light, GLASS_RGB, 0, 0, -1,
                a, JAR_Y0, a, 1, 1, a, JAR_Y1, a, 1, 0, b, JAR_Y1, a, 0, 0, b, JAR_Y0, a, 0, 1);
        MachineRenderHelper.face(c, pose, light, GLASS_RGB, 0, 0, 1,
                b, JAR_Y0, b, 1, 1, b, JAR_Y1, b, 1, 0, a, JAR_Y1, b, 0, 0, a, JAR_Y0, b, 0, 1);
        MachineRenderHelper.face(c, pose, light, GLASS_RGB, -1, 0, 0,
                a, JAR_Y0, b, 1, 1, a, JAR_Y1, b, 1, 0, a, JAR_Y1, a, 0, 0, a, JAR_Y0, a, 0, 1);
        MachineRenderHelper.face(c, pose, light, GLASS_RGB, 1, 0, 0,
                b, JAR_Y0, a, 1, 1, b, JAR_Y1, a, 1, 0, b, JAR_Y1, b, 0, 0, b, JAR_Y0, b, 0, 1);
    }
}
