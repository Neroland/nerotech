package za.co.neroland.nerotech.client.render;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import za.co.neroland.nerotech.machine.foundry.FoundryHeatPortBlockEntity;

/**
 * Foundry Heat Port BER: the collar glow. The copper collar on each open face lights by the port's
 * synced {@code glowBucket()} — dark at 0 (the "too cold" tell: nothing is reaching this port),
 * then dull, amber and white-hot. Steady, no motion, so it is the same with animations off; a cold
 * port draws nothing. Faces buried against an occluding neighbour are skipped. World data only, no
 * player data (POPIA/GDPR n/a).
 */
public class FoundryHeatPortRenderer
        implements BlockEntityRenderer<FoundryHeatPortBlockEntity, FoundryHeatPortRenderer.State> {

    private static final Identifier GLOW_TEX = MachineRenderHelper.texture("foundry_heat_port_glow");

    private static final int DIM_RGB = (150 << 16) | (98 << 8) | 38;
    /** The collar spans 4..12 px of each face and stands 1 px proud. */
    private static final float COLLAR_0 = 4.0F / 16.0F;
    private static final float COLLAR_1 = 12.0F / 16.0F;
    private static final float PROUD = 1.0F / 16.0F + 0.004F;

    public static class State extends BlockEntityRenderState {
        int bucket;
        /** Bit per {@link Direction} ordinal: that face is open to view. */
        int faces;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    /** The collar leaves the block by a pixel on every side. */
    public AABB getRenderBoundingBox(FoundryHeatPortBlockEntity port) {
        return new AABB(port.getBlockPos()).inflate(0.25);
    }

    @Override
    public void extractRenderState(FoundryHeatPortBlockEntity port, State state,
            float partialTick, Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(port, state, partialTick, cameraPos, breakProgress);
        state.bucket = port.glowBucket();
        state.faces = 0;
        Level level = port.getLevel();
        if (level == null || state.bucket <= 0) {
            return;
        }
        BlockPos pos = port.getBlockPos();
        for (Direction side : Direction.values()) {
            if (!level.getBlockState(pos.relative(side)).canOcclude()) {
                state.faces |= 1 << side.ordinal();
            }
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector,
            CameraRenderState cameraState) {
        if (state.bucket <= 0 || state.faces == 0) {
            return;
        }
        int rgb = state.bucket == 1 ? DIM_RGB
                : state.bucket == 2 ? BlastFoundryRenderer.WARN_RGB : BlastFoundryRenderer.WHITE_HOT_RGB;
        int faces = state.faces;
        collector.order(1).submitCustomGeometry(poseStack, RenderTypes.entityCutout(GLOW_TEX), (pose, c) -> {
            for (Direction side : Direction.values()) {
                if ((faces & (1 << side.ordinal())) != 0) {
                    BlastFoundryRenderer.cellFace(c, pose, rgb, side, 0.0F, 0.0F, 0.0F, COLLAR_0, COLLAR_1, PROUD);
                }
            }
        });
    }
}
