package za.co.neroland.nerotech.client.render;

import java.util.Map;
import java.util.WeakHashMap;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import za.co.neroland.nerotech.machine.NeroTechMachineBlock;
import za.co.neroland.nerotech.machine.foundry.BlastFoundryBlockEntity;
import za.co.neroland.nerotech.machine.foundry.FoundryBrickBlock;
import za.co.neroland.nerotech.machine.multiblock.FoundryShape;

/**
 * Blast Foundry controller BER. The interior of a foundry is never visible, so the controller face
 * is the whole readout:
 *
 * <ul>
 * <li><b>Sight glass</b> — the molten batch as a level in the door's 4 px window, from the synced
 * {@code batchFill()}. A 3-cube fills the lower tier only; a 5-cube uses both tiers. Bright while
 * working, dull while the batch waits (too cold, no energy), heat-critical in the HOT readout.</li>
 * <li><b>Door glow</b> — the ash vents under the glass breathe while the foundry works and are dark
 * otherwise (with the dark Heat Port collars, the "too cold" tell).</li>
 * <li><b>Pour</b> — a short stream off the pour lip when a running batch completes.</li>
 * <li><b>Form wave</b> — when the shell forms, the mortar lines light in a wave running outward from
 * the controller over 15 ticks (client effect keyed on distance; the bricks' own {@code lit} state
 * is the server's and only follows work).</li>
 * <li><b>Ghost outline</b> — while unformed, a player holding a Foundry Brick nearby sees one
 * outline box per missing block of the smallest (3-cube) shell.</li>
 * <li><b>Stack shimmer</b> — a chimney shimmer over a working 5-cube, and the shared HOT shimmer
 * over the whole roof.</li>
 * </ul>
 *
 * <p>Everything dynamic is gated on synced state; an idle formed foundry draws the lamp only. With
 * {@code renderAnimationsEnabled=false} the wave, pour and shimmer are skipped and the glow is
 * steady. The few client-side memories (when the shell formed, when the batch ended, the ghost
 * scan) live in a weak map keyed by the block entity. Reads the held item of the local player only,
 * for the ghost outline; nothing is stored or sent (POPIA/GDPR n/a).</p>
 */
public class BlastFoundryRenderer
        implements BlockEntityRenderer<BlastFoundryBlockEntity, BlastFoundryRenderer.State> {

    /** The status lamp socket: the {@code status_socket} element of {@code models/block/blast_foundry.json} (pixels). */
    private static final StatusLampRenderer.Socket SOCKET = StatusLampRenderer.Socket.model(1.0F, 12.0F, -0.5F, 3.0F, 14.0F, 0.0F);

    private static final Identifier MELT_TEX = MachineRenderHelper.texture("blast_foundry_melt");
    private static final Identifier GLOW_TEX = MachineRenderHelper.texture("blast_foundry_glow");
    private static final Identifier POUR_TEX = MachineRenderHelper.texture("blast_foundry_pour");
    private static final Identifier WAVE_TEX = MachineRenderHelper.texture("foundry_wave");
    private static final Identifier WAVE_TOP_TEX = MachineRenderHelper.texture("foundry_wave_top");
    private static final Identifier GHOST_TEX = MachineRenderHelper.texture("foundry_ghost");

    /** Renderer-only heat colours (never in base art): warning amber and pour-peak white. */
    static final int WARN_RGB = (255 << 16) | (178 << 8) | 56;
    static final int WHITE_HOT_RGB = (255 << 16) | (236 << 8) | 190;
    private static final int DULL_RGB = (118 << 16) | (78 << 8) | 34;
    private static final int MORTAR_LIT_RGB = (168 << 16) | (96 << 8) | 52;

    private static final float PX = 1.0F / 16.0F;
    private static final float GAP = 0.004F;

    /** Sight glass (model space): x 6..10, y 5..9, standing a quarter pixel proud of the door. */
    private static final float GLASS_X0 = 6.0F * PX;
    private static final float GLASS_X1 = 10.0F * PX;
    private static final float GLASS_Y0 = 5.0F * PX;
    private static final float GLASS_TIER = 2.0F * PX;
    private static final float GLASS_Z = 0.25F * PX - GAP;

    /** Ash vents on the door face (z 0.5): x 4..12, y 3.5..4.5. */
    private static final float VENT_X0 = 4.0F * PX;
    private static final float VENT_X1 = 12.0F * PX;
    private static final float VENT_Y0 = 3.5F * PX;
    private static final float VENT_Y1 = 4.5F * PX;
    private static final float VENT_Z = 0.5F * PX - GAP;
    private static final float BREATH_TICKS = 60.0F;

    private static final int WAVE_TICKS = 15;
    private static final float WAVE_BAND = 1.6F;
    private static final int POUR_TICKS = 20;
    private static final int GHOST_SIZE = 3;
    private static final int GHOST_CAP = 40;
    private static final int GHOST_SCAN_TICKS = 10;
    private static final double GHOST_RANGE_SQR = 12.0 * 12.0;
    private static final int LARGE = 5;

    /** Client-only memory per controller; entries die with their block entity. */
    private static final Map<BlastFoundryBlockEntity, Track> TRACKS = new WeakHashMap<>();

    private static final class Track {
        int lastSize = -1;
        int lastPour = Integer.MIN_VALUE;
        long formedAt = Long.MIN_VALUE;
        long pouredAt = Long.MIN_VALUE;
        long ghostAt = Long.MIN_VALUE;
        int ghostMask;
    }

    public static class State extends BlockEntityRenderState {
        final StatusLampRenderer.Lamp lamp = new StatusLampRenderer.Lamp();
        Direction facing = Direction.NORTH;
        int size;
        float fill;
        boolean active;
        /** Ticks since the shell formed, or -1 when no wave is running. */
        float wave = -1.0F;
        /** Ticks since the batch poured, or -1. */
        float pour = -1.0F;
        /** Furnace heat 0..1 (synced bucket): the door glow's brightness. */
        float furnace;
        /** Bit {@code i + 3 * j + 9 * k} set = that cell of the 3-cube shell is missing. */
        int ghostMask;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    /** The wave, the ghost and the stack shimmer cover the whole structure (up to a 5-cube + 1). */
    public AABB getRenderBoundingBox(BlastFoundryBlockEntity foundry) {
        return new AABB(foundry.getBlockPos()).inflate(LARGE);
    }

    @Override
    public void extractRenderState(BlastFoundryBlockEntity foundry, State state,
            float partialTick, Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(foundry, state, partialTick, cameraPos, breakProgress);
        StatusLampRenderer.extract(foundry, state.lamp, partialTick);
        state.facing = foundry.getBlockState().getValue(NeroTechMachineBlock.FACING);
        state.size = foundry.formedSize();
        state.fill = Mth.clamp(foundry.batchFill(), 0.0F, 1.0F);
        state.active = foundry.renderActive();
        state.wave = -1.0F;
        state.pour = -1.0F;
        state.ghostMask = 0;
        Level level = foundry.getLevel();
        if (level == null) {
            return;
        }
        long now = level.getGameTime();
        Track track = TRACKS.computeIfAbsent(foundry, key -> new Track());
        if (track.lastSize >= 0) {
            if (track.lastSize == 0 && state.size > 0) {
                track.formedAt = now;
            }
            // The synced pour pulse: only a batch that really poured (never an aborted one).
            if (track.lastPour != Integer.MIN_VALUE && foundry.pourPulse() != track.lastPour && state.size > 0) {
                track.pouredAt = now;
            }
        }
        track.lastPour = foundry.pourPulse();
        state.furnace = foundry.furnaceHeatBucket() / 5.0F;
        track.lastSize = state.size;

        if (state.lamp.animate && state.size > 0) {
            long sinceForm = now - track.formedAt;
            if (track.formedAt != Long.MIN_VALUE && sinceForm >= 0 && sinceForm <= WAVE_TICKS + 4) {
                state.wave = sinceForm + partialTick;
            }
            long sincePour = now - track.pouredAt;
            if (track.pouredAt != Long.MIN_VALUE && sincePour >= 0 && sincePour < POUR_TICKS) {
                state.pour = sincePour + partialTick;
            }
        }
        if (state.size == 0) {
            state.ghostMask = ghostMask(foundry, level, track, now, state.facing);
        } else {
            track.ghostAt = Long.MIN_VALUE;
        }
    }

    /**
     * The missing cells of the 3-cube shell, rescanned at most every 10 ticks and only for a nearby
     * local player with a Foundry Brick in hand; 0 otherwise.
     */
    private static int ghostMask(BlastFoundryBlockEntity foundry, Level level, Track track, long now,
            Direction facing) {
        Player player = Minecraft.getInstance().player;
        BlockPos pos = foundry.getBlockPos();
        if (player == null || !(holdsBrick(player.getMainHandItem()) || holdsBrick(player.getOffhandItem()))
                || player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > GHOST_RANGE_SQR) {
            track.ghostAt = Long.MIN_VALUE;
            return 0;
        }
        if (track.ghostAt != Long.MIN_VALUE && now >= track.ghostAt && now - track.ghostAt < GHOST_SCAN_TICKS) {
            return track.ghostMask;
        }
        int half = (GHOST_SIZE - 1) / 2;
        Direction back = facing.getOpposite();
        Direction right = facing.getClockWise();
        int mask = 0;
        for (int k = 0; k < GHOST_SIZE; k++) {
            for (int j = -half; j <= half; j++) {
                for (int i = -half; i <= half; i++) {
                    boolean interior = k > 0 && k < GHOST_SIZE - 1 && Math.abs(i) < half && Math.abs(j) < half;
                    if (interior || (i == 0 && j == 0 && k == 0)) {
                        continue;
                    }
                    BlockPos cell = pos.relative(back, k).relative(right, i).above(j);
                    BlockState there = level.getBlockState(cell);
                    if (!FoundryShape.INSTANCE.isShell(there) && !FoundryShape.INSTANCE.isPort(there)) {
                        mask |= 1 << ((i + half) + GHOST_SIZE * (j + half) + GHOST_SIZE * GHOST_SIZE * k);
                    }
                }
            }
        }
        track.ghostAt = now;
        track.ghostMask = mask;
        return mask;
    }

    private static boolean holdsBrick(ItemStack stack) {
        return stack.getItem() instanceof BlockItem item && item.getBlock() instanceof FoundryBrickBlock;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector,
            CameraRenderState cameraState) {
        StatusLampRenderer.Lamp lamp = state.lamp;
        boolean hot = StatusLampRenderer.hot(lamp);
        int size = state.size;
        int half = Math.max(0, (size - 1) / 2);
        poseStack.pushPose();
        MachineRenderHelper.rotateToFacing(poseStack, state.facing);

        StatusLampRenderer.submit(lamp, poseStack, collector, SOCKET);

        // Breathing 0..1 (steady 0.5 with animations off; quarter speed in HOT, like all motion).
        float breath = 0.5F;
        if (lamp.animate) {
            float speed = hot ? 0.25F : 1.0F;
            breath = 0.5F + 0.5F * (float) Math.sin(lamp.time * speed * 2.0 * Math.PI / BREATH_TICKS);
        }

        // Sight glass: the batch as a level. Formed only; an unformed controller keeps a dark door.
        if (size > 0 && state.fill > 0.0F) {
            float height = state.fill * GLASS_TIER * (size >= LARGE ? 2.0F : 1.0F);
            int melt = hot ? MachineRenderHelper.CRIT_RGB
                    : state.active ? MachineRenderHelper.lerpColor(0.35F + 0.4F * breath, WARN_RGB, WHITE_HOT_RGB)
                    : DULL_RGB;
            int skin = hot ? WARN_RGB
                    : state.active ? WHITE_HOT_RGB : MachineRenderHelper.lerpColor(0.4F, DULL_RGB, WARN_RGB);
            float top = GLASS_Y0 + height;
            float vSpan = height / (2.0F * GLASS_TIER);
            float line = Math.min(height, 0.5F * PX);
            collector.order(1).submitCustomGeometry(poseStack, RenderTypes.entityCutout(MELT_TEX), (pose, c) -> {
                northQuad(c, pose, melt, GLASS_X0, GLASS_Y0, GLASS_X1, top, GLASS_Z, 1.0F - vSpan, 1.0F);
                northQuad(c, pose, skin, GLASS_X0, top - line, GLASS_X1, top, GLASS_Z - GAP, 0.0F, 0.125F);
            });
        }

        // Door glow: the ash vents, only while the foundry actually works (absent below the heat threshold).
        if (size > 0 && (state.active || hot)) {
            int glow = hot ? MachineRenderHelper.CRIT_RGB
                    : MachineRenderHelper.lerpColor((0.15F + 0.6F * breath) * (0.5F + 0.5F * state.furnace),
                            WARN_RGB, WHITE_HOT_RGB);
            collector.order(1).submitCustomGeometry(poseStack, RenderTypes.entityCutout(GLOW_TEX),
                    (pose, c) -> northQuad(c, pose, glow, VENT_X0, VENT_Y0, VENT_X1, VENT_Y1, VENT_Z, 0.0F, 0.125F));
        }

        // Pour: the lip channel fills and a stream falls off it; the head drops first, the tail follows.
        if (state.pour >= 0.0F) {
            float t = state.pour;
            float head = 3.0F * PX * (1.0F - Mth.clamp(t / 4.0F, 0.0F, 1.0F));
            float tail = 3.0F * PX * (1.0F - Mth.clamp((t - (POUR_TICKS - 5)) / 5.0F, 0.0F, 1.0F));
            int stream = MachineRenderHelper.lerpColor(Mth.clamp(t / POUR_TICKS, 0.0F, 1.0F), WHITE_HOT_RGB, WARN_RGB);
            float fall = -t * 0.12F;
            boolean channel = t < POUR_TICKS - 5;
            collector.order(2).submitCustomGeometry(poseStack, RenderTypes.entityCutout(POUR_TEX), (pose, c) -> {
                MachineRenderHelper.fluidColumn(c, pose, MachineRenderHelper.FULL_BRIGHT, stream,
                        6.5F * PX, head, -1.6F * PX, 9.5F * PX, tail, -1.1F * PX, fall);
                if (channel) {
                    MachineRenderHelper.box(c, pose, MachineRenderHelper.FULL_BRIGHT, stream,
                            6.0F * PX, 3.0F * PX + GAP, -1.1F * PX, 10.0F * PX, 3.3F * PX, 0.5F * PX - GAP);
                }
            });
        }

        // Form wave: mortar lines flash outward from the controller over the outside of the shell.
        if (state.wave >= 0.0F && size > 0) {
            float reach = (float) Math.sqrt(2.0 * half * half + (size - 1.0) * (size - 1.0));
            float front = state.wave / WAVE_TICKS * (reach + WAVE_BAND);
            collector.order(2).submitCustomGeometry(poseStack, RenderTypes.entityCutout(WAVE_TEX),
                    (pose, c) -> wave(c, pose, size, half, front, false));
            collector.order(2).submitCustomGeometry(poseStack, RenderTypes.entityCutout(WAVE_TOP_TEX),
                    (pose, c) -> wave(c, pose, size, half, front, true));
        }

        // Ghost: one outline box per missing block of the 3-cube shell.
        if (size == 0 && state.ghostMask != 0) {
            int mask = state.ghostMask;
            collector.order(2).submitCustomGeometry(poseStack, RenderTypes.entityCutout(GHOST_TEX),
                    (pose, c) -> ghost(c, pose, mask));
        }

        // Stack: a working 5-cube shimmers over its chimney; any overheating foundry over the whole roof.
        if (size > 0) {
            float roof = half + 1.0F;
            if (size >= LARGE && state.active && lamp.animate && !hot) {
                int rgb = MachineRenderHelper.lerpColor(0.5F, WARN_RGB, 0xFFFFFF);
                float scroll = lamp.time * 0.02F;
                float mid = size / 2.0F;
                collector.order(4).submitCustomGeometry(poseStack,
                        RenderTypes.entityCutout(MachineRenderHelper.SHIMMER_TEX),
                        (pose, c) -> MachineRenderHelper.shimmerQuads(c, pose, rgb,
                                0.0F, mid - 0.5F, 1.0F, mid + 0.5F, roof, 0.75F, scroll));
            }
            StatusLampRenderer.submitShimmer(lamp, poseStack, collector,
                    -half + 0.25F, 0.25F, half + 0.75F, size - 0.25F, roof);
        } else {
            StatusLampRenderer.submitShimmer(lamp, poseStack, collector, 1.0F);
        }
        poseStack.popPose();
    }

    /** A full-bright north-facing quad with the sprite's full width and the V window {@code v0..v1}. */
    private static void northQuad(VertexConsumer c, PoseStack.Pose pose, int rgb,
            float x0, float y0, float x1, float y1, float z, float v0, float v1) {
        MachineRenderHelper.face(c, pose, MachineRenderHelper.FULL_BRIGHT, rgb, 0, 0, -1,
                x0, y0, z, 1, v1, x0, y1, z, 1, v0, x1, y1, z, 0, v0, x1, y0, z, 0, v1);
    }

    /** The wave band on every outward shell face: white-hot at the front, cooling to lit mortar behind. */
    private static void wave(VertexConsumer c, PoseStack.Pose pose, int size, int half, float front,
            boolean caps) {
        for (int k = 0; k < size; k++) {
            for (int j = -half; j <= half; j++) {
                for (int i = -half; i <= half; i++) {
                    float behind = front - (float) Math.sqrt(i * i + j * j + k * k);
                    if (behind < 0.0F || behind > WAVE_BAND) {
                        continue;
                    }
                    int rgb = MachineRenderHelper.lerpColor(behind / WAVE_BAND, WHITE_HOT_RGB, MORTAR_LIT_RGB);
                    if (caps) {
                        if (j == half) {
                            cellFace(c, pose, rgb, Direction.UP, i, j, k, 0.0F, 1.0F, GAP);
                        }
                        if (j == -half) {
                            cellFace(c, pose, rgb, Direction.DOWN, i, j, k, 0.0F, 1.0F, GAP);
                        }
                        continue;
                    }
                    if (k == 0 && !(i == 0 && j == 0)) {
                        cellFace(c, pose, rgb, Direction.NORTH, i, j, k, 0.0F, 1.0F, GAP);
                    }
                    if (k == size - 1) {
                        cellFace(c, pose, rgb, Direction.SOUTH, i, j, k, 0.0F, 1.0F, GAP);
                    }
                    if (i == -half) {
                        cellFace(c, pose, rgb, Direction.WEST, i, j, k, 0.0F, 1.0F, GAP);
                    }
                    if (i == half) {
                        cellFace(c, pose, rgb, Direction.EAST, i, j, k, 0.0F, 1.0F, GAP);
                    }
                }
            }
        }
    }

    private static void ghost(VertexConsumer c, PoseStack.Pose pose, int mask) {
        int half = (GHOST_SIZE - 1) / 2;
        int drawn = 0;
        for (int bit = 0; bit < GHOST_SIZE * GHOST_SIZE * GHOST_SIZE && drawn < GHOST_CAP; bit++) {
            if ((mask & (1 << bit)) == 0) {
                continue;
            }
            int i = bit % GHOST_SIZE - half;
            int j = (bit / GHOST_SIZE) % GHOST_SIZE - half;
            int k = bit / (GHOST_SIZE * GHOST_SIZE);
            for (Direction side : Direction.values()) {
                cellFace(c, pose, MachineRenderHelper.CYAN_RGB, side, i, j, k, 0.0F, 1.0F, GAP);
            }
            drawn++;
        }
    }

    /**
     * A full-bright quad on one face of the block cell at {@code (cx, cy, cz)} (controller space: the
     * controller is cell 0,0,0, +z runs into the structure), covering {@code a0..a1} of the face in
     * both in-plane axes with the whole sprite, pushed {@code out} blocks off the face. Vertical
     * faces map V downward and top/bottom faces map U along x and V along z, like the block models.
     */
    static void cellFace(VertexConsumer c, PoseStack.Pose pose, int rgb, Direction side,
            float cx, float cy, float cz, float a0, float a1, float out) {
        int light = MachineRenderHelper.FULL_BRIGHT;
        float x0 = cx + a0;
        float x1 = cx + a1;
        float y0 = cy + a0;
        float y1 = cy + a1;
        float z0 = cz + a0;
        float z1 = cz + a1;
        if (side == Direction.UP) {
            float y = cy + 1.0F + out;
            MachineRenderHelper.face(c, pose, light, rgb, 0, 1, 0,
                    x0, y, z0, 0, 0, x0, y, z1, 0, 1, x1, y, z1, 1, 1, x1, y, z0, 1, 0);
        } else if (side == Direction.DOWN) {
            float y = cy - out;
            MachineRenderHelper.face(c, pose, light, rgb, 0, -1, 0,
                    x0, y, z0, 0, 0, x1, y, z0, 1, 0, x1, y, z1, 1, 1, x0, y, z1, 0, 1);
        } else if (side == Direction.NORTH) {
            float z = cz - out;
            MachineRenderHelper.face(c, pose, light, rgb, 0, 0, -1,
                    x0, y0, z, 0, 1, x0, y1, z, 0, 0, x1, y1, z, 1, 0, x1, y0, z, 1, 1);
        } else if (side == Direction.SOUTH) {
            float z = cz + 1.0F + out;
            MachineRenderHelper.face(c, pose, light, rgb, 0, 0, 1,
                    x1, y0, z, 0, 1, x1, y1, z, 0, 0, x0, y1, z, 1, 0, x0, y0, z, 1, 1);
        } else if (side == Direction.WEST) {
            float x = cx - out;
            MachineRenderHelper.face(c, pose, light, rgb, -1, 0, 0,
                    x, y0, z1, 0, 1, x, y1, z1, 0, 0, x, y1, z0, 1, 0, x, y0, z0, 1, 1);
        } else {
            float x = cx + 1.0F + out;
            MachineRenderHelper.face(c, pose, light, rgb, 1, 0, 0,
                    x, y0, z0, 0, 1, x, y1, z0, 0, 0, x, y1, z1, 1, 0, x, y0, z1, 1, 1);
        }
    }
}
