package za.co.neroland.nerotech.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

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

import za.co.neroland.nerolandcore.sideconfig.Channel;
import za.co.neroland.nerolandcore.sideconfig.SideConfigComponent;
import za.co.neroland.nerolandcore.sideconfig.SideModeColors;

import za.co.neroland.nerotech.config.NeroTechConfig;
import za.co.neroland.nerotech.machine.ItemSorterBlockEntity;

/**
 * Item Sorter BER: six port-cap quads sitting proud of the collar ends, tinted per side-config mode
 * with Core's shared {@link SideModeColors} (disabled grey / input blue / output orange / push
 * yellow) — Side Config readability in-world. The tints resolve through the BE's synced
 * {@code sideConfig()} (Core's packed config rides the update tag; the sorter re-syncs whenever a
 * port mode changes) against the blockstate facing, so they are exact on the client. Each synced
 * sort pulse briefly brightens the caps toward white.
 *
 * <p>The caps are static indicator geometry — always drawn (that's their job) — so
 * {@code renderAnimationsEnabled=false} only suppresses the pulse brightening.</p>
 */
public class ItemSorterRenderer
        implements BlockEntityRenderer<ItemSorterBlockEntity, ItemSorterRenderer.State> {

    private static final Identifier CAP_TEX = MachineRenderHelper.texture("item_sorter_cap");

    /** Cap quad half-size (the collars are 8×8 px; the caps are 6×6 px, centred). */
    private static final float CAP_HALF = 3.0F / 16.0F;
    /** How far the cap sits proud of the block face (avoids z-fighting the collar's end face). */
    private static final float CAP_PROUD = 0.02F / 16.0F;
    /** Pulse brightening length (ticks). */
    private static final float PULSE_TICKS = 6.0F;

    public static class State extends BlockEntityRenderState {
        boolean visible;
        /** ARGB cap tint per {@link Direction} ordinal. */
        final int[] colors = new int[6];
        /** 0..1 whiten mix from the sort pulse. */
        float brighten;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    /** One-block pad (MODELS.md); NeoForge-only frustum hook, inert non-override on Fabric/Forge. */
    public AABB getRenderBoundingBox(ItemSorterBlockEntity sorter) {
        return new AABB(sorter.getBlockPos()).inflate(1.0);
    }

    @Override
    public void extractRenderState(ItemSorterBlockEntity sorter, State state, float partialTick,
            Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(sorter, state, partialTick, cameraPos, breakProgress);
        Level level = sorter.getLevel();
        SideConfigComponent component = sorter.sideConfig();
        state.brighten = 0.0F;
        // Explicit null checks (not folded into a flag) so ecj's null-flow analysis can track them.
        if (level == null || component == null) {
            state.visible = false;
            return;
        }
        state.visible = true;
        // Per-face tint: the synced packed config resolved against the blockstate facing.
        Direction facing = component.facing();
        for (Direction side : Direction.values()) {
            state.colors[side.ordinal()] =
                    SideModeColors.of(component.config().modeAbsolute(Channel.ITEM, facing, side));
        }
        // Sort-pulse brightening (same detection recipe as the Auto Crafter stamp).
        long tick = level.getGameTime();
        int pulse = sorter.renderPulse();
        if (pulse != sorter.clientSeenPulse) {
            if (sorter.clientSeenPulse != Integer.MIN_VALUE) {
                sorter.clientPulseTime = tick;
            }
            sorter.clientSeenPulse = pulse;
        }
        if (NeroTechConfig.renderAnimationsEnabled() && sorter.clientPulseTime != Long.MIN_VALUE) {
            float since = (tick + partialTick) - sorter.clientPulseTime;
            if (since >= 0.0F && since < PULSE_TICKS) {
                state.brighten = 1.0F - since / PULSE_TICKS;
            }
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector,
            CameraRenderState cameraState) {
        if (!state.visible) {
            return;
        }
        int light = MachineRenderHelper.FULL_BRIGHT;
        float brighten = state.brighten * 0.6F;
        collector.order(1).submitCustomGeometry(poseStack, RenderTypes.entityCutout(CAP_TEX),
                (pose, c) -> {
                    for (Direction side : Direction.values()) {
                        cap(c, pose, light, side, whiten(state.colors[side.ordinal()], brighten));
                    }
                });
    }

    /** Mix an ARGB tint toward white by {@code t} (the pulse flash), returning 0xRRGGBB. */
    private static int whiten(int argb, float t) {
        int r = (int) Mth.lerp(t, (argb >> 16) & 0xFF, 255);
        int g = (int) Mth.lerp(t, (argb >> 8) & 0xFF, 255);
        int b = (int) Mth.lerp(t, argb & 0xFF, 255);
        return (r << 16) | (g << 8) | b;
    }

    /** One tinted, double-sided cap quad centred on {@code side}'s collar end, slightly proud of it. */
    private static void cap(VertexConsumer c, PoseStack.Pose pose, int light, Direction side, int rgb) {
        float lo = 0.5F - CAP_HALF;
        float hi = 0.5F + CAP_HALF;
        switch (side) {
            case DOWN -> MachineRenderHelper.face(c, pose, light, rgb, 0, -1, 0,
                    lo, -CAP_PROUD, lo, 0, 0, hi, -CAP_PROUD, lo, 1, 0,
                    hi, -CAP_PROUD, hi, 1, 1, lo, -CAP_PROUD, hi, 0, 1);
            case UP -> MachineRenderHelper.face(c, pose, light, rgb, 0, 1, 0,
                    lo, 1.0F + CAP_PROUD, lo, 0, 0, lo, 1.0F + CAP_PROUD, hi, 0, 1,
                    hi, 1.0F + CAP_PROUD, hi, 1, 1, hi, 1.0F + CAP_PROUD, lo, 1, 0);
            case NORTH -> MachineRenderHelper.face(c, pose, light, rgb, 0, 0, -1,
                    lo, lo, -CAP_PROUD, 0, 1, lo, hi, -CAP_PROUD, 0, 0,
                    hi, hi, -CAP_PROUD, 1, 0, hi, lo, -CAP_PROUD, 1, 1);
            case SOUTH -> MachineRenderHelper.face(c, pose, light, rgb, 0, 0, 1,
                    hi, lo, 1.0F + CAP_PROUD, 0, 1, hi, hi, 1.0F + CAP_PROUD, 0, 0,
                    lo, hi, 1.0F + CAP_PROUD, 1, 0, lo, lo, 1.0F + CAP_PROUD, 1, 1);
            case WEST -> MachineRenderHelper.face(c, pose, light, rgb, -1, 0, 0,
                    -CAP_PROUD, lo, hi, 0, 1, -CAP_PROUD, hi, hi, 0, 0,
                    -CAP_PROUD, hi, lo, 1, 0, -CAP_PROUD, lo, lo, 1, 1);
            case EAST -> MachineRenderHelper.face(c, pose, light, rgb, 1, 0, 0,
                    1.0F + CAP_PROUD, lo, lo, 0, 1, 1.0F + CAP_PROUD, hi, lo, 0, 0,
                    1.0F + CAP_PROUD, hi, hi, 1, 0, 1.0F + CAP_PROUD, lo, hi, 1, 1);
        }
    }
}
