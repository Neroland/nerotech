package za.co.neroland.nerotech.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import za.co.neroland.nerotech.item.tools.HeadToolItem;
import za.co.neroland.nerotech.item.tools.ThermalGogglesItem;
import za.co.neroland.nerotech.machine.NeroTechMachineBlockEntity;
import za.co.neroland.nerotech.machine.heat.HeatPipeBlock;

/**
 * Client only. The Thermal Goggles' heat view: while the local player wears charged goggles, every NeroTech
 * machine in view within {@value #RANGE} blocks is tinted by its synced {@code heatBucket()} with a
 * coloured mote above it (blue cold → red at the ceiling), and Heat Pipes within {@value #PIPE_RADIUS}
 * blocks by their {@code heat} block-state level. Reads only what the client already has from the normal
 * block-entity and block-state sync; adds no packet and stores nothing. Driven from the machine client
 * ticker ({@link MachineEffects}); bounded: one mote per machine per {@value #GAP} ticks, one pipe sweep
 * per second capped at {@value #PIPE_MOTES} motes.
 */
public final class ThermalGogglesView {

    static final int RANGE = 24;
    static final int GAP = 10;
    static final int PIPE_RADIUS = 8;
    static final int PIPE_MOTES = 64;

    /** Heat bucket 0..5 → mote colour. */
    private static final int[] COLOURS = {0x3A7BFF, 0x3AD0FF, 0x5BE36B, 0xF5D547, 0xFF8A2A, 0xFF3030};

    private static long checkedTick = Long.MIN_VALUE;
    private static boolean wearing;
    private static long pipeSweepTick = Long.MIN_VALUE;

    private ThermalGogglesView() {
    }

    /** Whether the local player wears charged goggles this tick (computed once per game tick). */
    private static boolean wearing(Level level) {
        long now = level.getGameTime();
        if (now != checkedTick) {
            checkedTick = now;
            Player player = Minecraft.getInstance().player;
            ItemStack head = player == null ? ItemStack.EMPTY : player.getItemBySlot(EquipmentSlot.HEAD);
            wearing = head.getItem() instanceof ThermalGogglesItem && HeadToolItem.powered(head);
        }
        return wearing;
    }

    public static void tick(NeroTechMachineBlockEntity machine) {
        Level level = machine.getLevel();
        if (level == null || !level.isClientSide() || !wearing(level)) {
            return;
        }
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        long now = level.getGameTime();
        if (now >= pipeSweepTick + 20) {
            pipeSweepTick = now;
            sweepPipes(level, player);
        }
        BlockPos pos = machine.getBlockPos();
        if (Math.floorMod(now + pos.hashCode(), GAP) != 0 || !inView(player, pos, RANGE)) {
            return;
        }
        mote(level, pos, machine.heatBucket());
    }

    private static void sweepPipes(Level level, Player player) {
        BlockPos centre = player.blockPosition();
        int motes = 0;
        for (BlockPos at : BlockPos.betweenClosed(centre.offset(-PIPE_RADIUS, -PIPE_RADIUS, -PIPE_RADIUS),
                centre.offset(PIPE_RADIUS, PIPE_RADIUS, PIPE_RADIUS))) {
            if (motes >= PIPE_MOTES) {
                return;
            }
            BlockState state = level.getBlockState(at);
            if (!state.hasProperty(HeatPipeBlock.HEAT) || !inView(player, at, PIPE_RADIUS * 2)) {
                continue;
            }
            int heatLevel = state.getValue(HeatPipeBlock.HEAT);
            mote(level, at.immutable(), new int[] {0, 2, 4, 5}[Math.max(0, Math.min(3, heatLevel))]);
            motes++;
        }
    }

    private static boolean inView(Player player, BlockPos pos, int range) {
        Vec3 eye = player.getEyePosition();
        Vec3 to = new Vec3(pos.getX() + 0.5 - eye.x, pos.getY() + 0.5 - eye.y, pos.getZ() + 0.5 - eye.z);
        double distance = to.length();
        return distance <= range && (distance < 2.0 || to.scale(1.0 / distance).dot(player.getViewVector(1.0F)) > 0.5);
    }

    /** Per heat step 0..5 (screens/tools-hud.md): mote size, count and rise, so colour is never the only cue. */
    private static final float[] SIZES = {0.8F, 1.0F, 1.2F, 1.4F, 1.6F, 1.8F};
    private static final int[] COUNTS = {1, 1, 1, 2, 2, 3};
    private static final double[] RISE = {0.0, 0.006, 0.008, 0.012, 0.018, 0.018};

    private static void mote(Level level, BlockPos pos, int bucket) {
        int step = Math.max(0, Math.min(COLOURS.length - 1, bucket));
        // Step 5 (at the ceiling) blinks at 2 Hz: motes only in the lit half of each 10-tick period.
        if (step == COLOURS.length - 1 && (level.getGameTime() / 5L) % 2L == 1L) {
            return;
        }
        int colour = COLOURS[step];
        float size = SIZES[step];
        //? if >=1.21.2 {
        DustParticleOptions dust = new DustParticleOptions(colour, size);
        //?} else {
        /*DustParticleOptions dust = new DustParticleOptions(new org.joml.Vector3f(((colour >> 16) & 0xFF) / 255F,
                ((colour >> 8) & 0xFF) / 255F, (colour & 0xFF) / 255F), size);
        *///?}
        for (int i = 0; i < COUNTS[step]; i++) {
            double dx = (i - (COUNTS[step] - 1) / 2.0) * 0.22;
            level.addParticle(dust, pos.getX() + 0.5 + dx, pos.getY() + 1.05, pos.getZ() + 0.5, 0.0, RISE[step], 0.0);
        }
    }
}
