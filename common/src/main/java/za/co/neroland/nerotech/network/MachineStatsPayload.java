package za.co.neroland.nerotech.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import za.co.neroland.nerotech.NeroTechCommon;
import za.co.neroland.nerotech.machine.MachineStats;
import za.co.neroland.nerotech.machine.NeroTechMachineBlockEntity;

/**
 * Server &rarr; client: the Stage G analytics snapshot for the machine behind an open menu,
 * keyed by container id and sent by {@code menu.MachineMenu#broadcastChanges} every 20 ticks
 * <b>only while that menu is open</b> — analytics never broadcast to non-viewers. Carries the
 * current status/values plus the full 60-second history window (shorts for the permille series,
 * unsigned bytes for the ops deltas — compact by design; &le; ~310 bytes per push).
 *
 * <p><b>Privacy (POPIA/GDPR):</b> machine-scoped numbers only — status, rates, heat, energy.
 * No player names/UUIDs, never logged, never persisted.
 *
 * @param containerId    the open menu's container id (the client mailbox key)
 * @param status         {@link za.co.neroland.nerotech.machine.MachineStatus} ordinal
 * @param heatPermille   current heat, permille of capacity
 * @param energyPermille current stored energy, permille of capacity
 * @param opsRate        work ops in the most recent one-second sample
 * @param heatHistory    heat permille samples, oldest &rarr; newest (&le; 60)
 * @param energyHistory  stored-energy permille samples, oldest &rarr; newest (&le; 60)
 * @param opsHistory     per-second ops deltas, oldest &rarr; newest, clamped to 0..255 (&le; 60)
 */
public record MachineStatsPayload(int containerId, int status, int heatPermille, int energyPermille,
        int opsRate, short[] heatHistory, short[] energyHistory, byte[] opsHistory)
        implements CustomPacketPayload {

    public static final Type<MachineStatsPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(NeroTechCommon.MOD_ID, "machine_stats"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MachineStatsPayload> STREAM_CODEC =
            StreamCodec.of(MachineStatsPayload::write, MachineStatsPayload::read);

    /** Snapshot a machine's live analytics window (server side; menu send path). */
    public static MachineStatsPayload of(int containerId, NeroTechMachineBlockEntity machine) {
        MachineStats stats = machine.stats();
        short[] ops = stats.opsHistory();
        byte[] opsBytes = new byte[ops.length];
        for (int i = 0; i < ops.length; i++) {
            opsBytes[i] = (byte) Math.min(255, Math.max(0, ops[i]));
        }
        return new MachineStatsPayload(containerId, stats.status().ordinal(),
                machine.heatPermille(), machine.energyPermille(), stats.currentOpsRate(),
                stats.heatHistory(), stats.energyHistory(), opsBytes);
    }

    private static void write(RegistryFriendlyByteBuf buf, MachineStatsPayload payload) {
        buf.writeVarInt(payload.containerId);
        buf.writeByte(payload.status);
        buf.writeShort(payload.heatPermille);
        buf.writeShort(payload.energyPermille);
        buf.writeVarInt(payload.opsRate);
        int samples = Math.min(MachineStats.WINDOW, payload.heatHistory.length);
        buf.writeByte(samples);
        for (int i = 0; i < samples; i++) {
            buf.writeShort(payload.heatHistory[i]);
        }
        for (int i = 0; i < samples; i++) {
            buf.writeShort(payload.energyHistory[i]);
        }
        for (int i = 0; i < samples; i++) {
            buf.writeByte(payload.opsHistory[i]);
        }
    }

    private static MachineStatsPayload read(RegistryFriendlyByteBuf buf) {
        int containerId = buf.readVarInt();
        int status = buf.readUnsignedByte();
        int heatPermille = buf.readShort();
        int energyPermille = buf.readShort();
        int opsRate = buf.readVarInt();
        int samples = Math.min(MachineStats.WINDOW, buf.readUnsignedByte());
        short[] heat = new short[samples];
        for (int i = 0; i < samples; i++) {
            heat[i] = buf.readShort();
        }
        short[] energy = new short[samples];
        for (int i = 0; i < samples; i++) {
            energy[i] = buf.readShort();
        }
        byte[] ops = new byte[samples];
        for (int i = 0; i < samples; i++) {
            ops[i] = buf.readByte();
        }
        return new MachineStatsPayload(containerId, status, heatPermille, energyPermille, opsRate,
                heat, energy, ops);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
