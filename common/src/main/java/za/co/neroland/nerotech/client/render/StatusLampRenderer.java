package za.co.neroland.nerotech.client.render;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

import za.co.neroland.nerotech.config.NeroTechConfig;
import za.co.neroland.nerotech.machine.NeroTechMachineBlockEntity;

/**
 * The shared status lamp every 0.8.0 machine carries: one full-bright quad in the socket at the top
 * right of the front face, telling the machine's state without a GUI. Not a renderer of its own —
 * each machine's block-entity renderer keeps a {@link Lamp} in its render state and calls
 * {@link #extract} and {@link #submit}:
 *
 * <pre>{@code
 * public static class State extends BlockEntityRenderState {
 *     final StatusLampRenderer.Lamp lamp = new StatusLampRenderer.Lamp();
 * }
 * // extractRenderState:
 * StatusLampRenderer.extract(machine, state.lamp, partialTick);
 * // submit, after MachineRenderHelper.rotateToFacing(poseStack, state.facing):
 * StatusLampRenderer.submit(state.lamp, poseStack, collector);
 * StatusLampRenderer.submitShimmer(state.lamp, poseStack, collector, 1.0F); // HOT only, no-op otherwise
 * }</pre>
 *
 * <p>Rhythm carries the meaning and colour only reinforces it, so the lamp reads without colour
 * vision: REST steady dim teal; WORK steady cyan gliding to plasma; STALL a double blink (on 4,
 * off 4, on 4, off 28 ticks); HOT the heat colour, blinking 4 on / 4 off in the top heat bucket.
 * With {@code renderAnimationsEnabled=false} nothing blinks: STALL shows the half-lit sprite and
 * HOT the steady heat colour. A lamp marked {@link Lamp#dark} (no energy) draws nothing at all.</p>
 *
 * <p>Reads only the machine's synced render surface ({@code readout()}, {@code heatBucket()}) —
 * never a menu, and no player data (POPIA/GDPR n/a).</p>
 */
public final class StatusLampRenderer {

    /** Readout ordinals, in the machine contract's order. */
    public static final int REST = 0;
    public static final int WORK = 1;
    public static final int STALL = 2;
    public static final int HOT = 3;

    /** Heat buckets run 0..5; the lamp blinks in the top one. */
    public static final int TOP_HEAT_BUCKET = 5;
    /** Heat bucket from which a machine reads HOT (4 of 6). */
    public static final int HOT_FROM_BUCKET = 4;

    /**
     * The socket in model space (block units, machine authored facing north). It is the top right
     * as the player faces the machine, which is the LOW x side of a north-facing model.
     */
    public static final float SOCKET_X0 = 1.0F / 16.0F;
    public static final float SOCKET_X1 = 3.0F / 16.0F;
    public static final float SOCKET_Y0 = 12.0F / 16.0F;
    public static final float SOCKET_Y1 = 14.0F / 16.0F;
    /** The socket collar stands half a pixel proud of the front face; the quad sits just off it. */
    private static final float COLLAR = 0.5F / 16.0F;
    private static final float GAP = 0.004F;

    /**
     * Where a model's lamp socket sits (block units, machine authored facing north): the socket's
     * x/y box and the z of the quad, just in front of the socket's front face. Each renderer passes the
     * box of its own model's {@code status_socket} element, since sockets stand on posts, plates and
     * recessed faces rather than always on the block's front face.
     */
    public record Socket(float x0, float y0, float x1, float y1, float z) {

        /** The standard socket: model x 1-3, y 12-14, on a front face at z 0 (collar from z -0.5). */
        public static final Socket STANDARD = model(1.0F, 12.0F, -0.5F, 3.0F, 14.0F, 0.0F);

        /**
         * From a model element's {@code from}/{@code to} in pixels (as in the model JSON). The quad sits
         * just north of the element's north face ({@code from} z).
         */
        public static Socket model(float fromX, float fromY, float fromZ, float toX, float toY, float toZ) {
            return new Socket(Math.min(fromX, toX) / 16.0F, Math.min(fromY, toY) / 16.0F,
                    Math.max(fromX, toX) / 16.0F, Math.max(fromY, toY) / 16.0F,
                    Math.min(fromZ, toZ) / 16.0F - GAP);
        }
    }

    /** STALL double blink: on 4, off 4, on 4, off 28 (a 40-tick cycle). */
    private static final int STALL_CYCLE = 40;
    /** WORK glide between cyan and plasma, in ticks per full swing. */
    private static final float WORK_GLIDE_TICKS = 40.0F;
    /** Heat shimmer: height above the top face (block units) and upward scroll per tick. */
    private static final float SHIMMER_HEIGHT = 0.5F;
    private static final float SHIMMER_SPEED = 0.02F;
    /** Motion slows to a quarter speed in the HOT readout. */
    private static final float HOT_MOTION = 0.25F;

    /** The lamp's share of a renderer's render state. Filled by {@link #extract}. */
    public static final class Lamp {
        /** One of {@link #REST}, {@link #WORK}, {@link #STALL}, {@link #HOT}. */
        public int readout = REST;
        /** 0..5. */
        public int heatBucket;
        /**
         * Lamp fully dark (the machine has no energy). {@link #extract} copies it from the
         * machine's synced {@code lampDark()}.
         */
        public boolean dark;
        /** False with {@code renderAnimationsEnabled=false}: steady colours, no blink, parked pose. */
        public boolean animate;
        /** Game time plus partial tick; 0 when the level is not there yet. */
        public float time;
        long tick;
    }

    private StatusLampRenderer() {
    }

    /** Copy the machine's synced readout and heat bucket into {@code lamp}. Call every extract. */
    public static void extract(NeroTechMachineBlockEntity machine, Lamp lamp, float partialTick) {
        // Machine contract C1: the synced readout, heat bucket and lamp-dark flag (NO_ENERGY).
        lamp.readout = machine.readout().ordinal();
        lamp.heatBucket = Mth.clamp(machine.heatBucket(), 0, TOP_HEAT_BUCKET);
        lamp.dark = machine.lampDark();
        lamp.animate = NeroTechConfig.renderAnimationsEnabled();
        Level level = machine.getLevel();
        if (level == null) {
            lamp.tick = 0L;
            lamp.time = 0.0F;
            return;
        }
        lamp.tick = level.getGameTime();
        // Wrapped so the float keeps sub-tick precision however old the world is (period 2400 t
        // is a multiple of every cycle used here and by the shimmer scroll).
        lamp.time = (lamp.tick % 2400L) + partialTick;
    }

    /**
     * Set the readout directly — for a renderer whose block entity is not a
     * {@link NeroTechMachineBlockEntity}, or a preview. Call after {@link #extract} or instead of it
     * (then also set {@link Lamp#animate} and {@link Lamp#time}).
     */
    public static void set(Lamp lamp, int readout, int heatBucket) {
        lamp.readout = Mth.clamp(readout, REST, HOT);
        lamp.heatBucket = Mth.clamp(heatBucket, 0, TOP_HEAT_BUCKET);
    }

    /** Whether the signature motion should run at all (WORK, or HOT at reduced speed). */
    public static boolean moving(Lamp lamp) {
        return lamp.animate && (lamp.readout == WORK || lamp.readout == HOT);
    }

    /**
     * Speed factor for the machine's signature motion: 1 in WORK, 0.25 in HOT, 0 otherwise. In
     * STALL the renderer must hold its last mid-stroke pose rather than park — a stalled machine
     * never looks idle — and in REST it parks at the home pose.
     */
    public static float motionScale(Lamp lamp) {
        if (!lamp.animate) {
            return 0.0F;
        }
        return lamp.readout == WORK ? 1.0F : lamp.readout == HOT ? HOT_MOTION : 0.0F;
    }

    /** True in STALL: freeze mid-stroke. */
    public static boolean stalled(Lamp lamp) {
        return lamp.readout == STALL;
    }

    /** True in HOT: shimmer, steam wisps, heat-coloured parts. */
    public static boolean hot(Lamp lamp) {
        return lamp.readout == HOT;
    }

    /** Heat as 0..1 from the synced bucket — the input for {@link MachineRenderHelper#heatColor}. */
    public static float heatFraction(Lamp lamp) {
        return lamp.heatBucket / (float) TOP_HEAT_BUCKET;
    }

    /** Whether the lamp is lit this tick (the blink rhythm). */
    public static boolean lit(Lamp lamp) {
        if (lamp.dark) {
            return false;
        }
        if (!lamp.animate) {
            return true;
        }
        if (lamp.readout == STALL) {
            int phase = (int) (lamp.tick % STALL_CYCLE);
            return phase < 4 || (phase >= 8 && phase < 12);
        }
        if (lamp.readout == HOT && lamp.heatBucket >= TOP_HEAT_BUCKET) {
            return (lamp.tick & 7L) < 4L;
        }
        return true;
    }

    /** The lamp colour for the current readout, packed 0xRRGGBB. */
    public static int color(Lamp lamp) {
        switch (lamp.readout) {
            case WORK: {
                if (!lamp.animate) {
                    return MachineRenderHelper.CYAN_RGB;
                }
                float swing = 0.5F - 0.5F * (float) Math.cos(lamp.time * 2.0 * Math.PI / WORK_GLIDE_TICKS);
                return MachineRenderHelper.lerpColor(swing, MachineRenderHelper.CYAN_RGB,
                        MachineRenderHelper.PLASMA_RGB);
            }
            case STALL:
                return MachineRenderHelper.CYAN_RGB;
            case HOT:
                return MachineRenderHelper.heatColor(heatFraction(lamp));
            default:
                return MachineRenderHelper.TEAL_RGB;
        }
    }

    /** Draw the lamp in the standard socket of a machine whose front face is at model z = 0. */
    public static void submit(Lamp lamp, PoseStack poseStack, SubmitNodeCollector collector) {
        submit(lamp, poseStack, collector, 0.0F);
    }

    /**
     * Draw the lamp in the standard socket of a machine whose front face is set back to
     * {@code zFront} (block units from the north edge). Call inside the facing rotation.
     */
    public static void submit(Lamp lamp, PoseStack poseStack, SubmitNodeCollector collector, float zFront) {
        submitAt(lamp, poseStack, collector, SOCKET_X0, SOCKET_Y0, SOCKET_X1, SOCKET_Y1,
                zFront - COLLAR - GAP);
    }

    /** Draw the lamp in {@code socket} (the model's own socket box). Call inside the facing rotation. */
    public static void submit(Lamp lamp, PoseStack poseStack, SubmitNodeCollector collector, Socket socket) {
        submitAt(lamp, poseStack, collector, socket.x0(), socket.y0(), socket.x1(), socket.y1(), socket.z());
    }

    /** Draw the lamp as a north-facing quad anywhere (block units) — for a non-standard socket. */
    public static void submitAt(Lamp lamp, PoseStack poseStack, SubmitNodeCollector collector,
            float x0, float y0, float x1, float y1, float z) {
        if (!lit(lamp)) {
            return;
        }
        int rgb = color(lamp);
        Identifier sprite = !lamp.animate && lamp.readout == STALL
                ? MachineRenderHelper.LAMP_HALF_TEX : MachineRenderHelper.LAMP_TEX;
        collector.order(3).submitCustomGeometry(poseStack, RenderTypes.entityCutout(sprite),
                (pose, c) -> MachineRenderHelper.lampQuad(c, pose, rgb, x0, y0, x1, y1, z));
    }

    /**
     * Draw the heat shimmer over the whole top face at height {@code yTop} (block units; 1 for a
     * full-height machine). Does nothing outside HOT or with animations off.
     */
    public static void submitShimmer(Lamp lamp, PoseStack poseStack, SubmitNodeCollector collector,
            float yTop) {
        submitShimmer(lamp, poseStack, collector, 2.0F / 16.0F, 2.0F / 16.0F, 14.0F / 16.0F,
                14.0F / 16.0F, yTop);
    }

    /** Heat shimmer over a chosen footprint (block units) — a narrow column, a stack, a chimney. */
    public static void submitShimmer(Lamp lamp, PoseStack poseStack, SubmitNodeCollector collector,
            float x0, float z0, float x1, float z1, float yTop) {
        if (lamp.readout != HOT || !lamp.animate) {
            return;
        }
        int rgb = MachineRenderHelper.lerpColor(0.5F, MachineRenderHelper.heatColor(heatFraction(lamp)),
                0xFFFFFF);
        // The sprite window slides down the texture as time passes, so the waves rise.
        float scroll = lamp.time * SHIMMER_SPEED;
        collector.order(4).submitCustomGeometry(poseStack,
                RenderTypes.entityCutout(MachineRenderHelper.SHIMMER_TEX),
                (pose, c) -> MachineRenderHelper.shimmerQuads(c, pose, rgb, x0, z0, x1, z1, yTop,
                        SHIMMER_HEIGHT, scroll));
    }
}
