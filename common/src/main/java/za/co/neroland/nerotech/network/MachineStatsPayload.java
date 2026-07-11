package za.co.neroland.nerotech.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import za.co.neroland.nerolandcore.upgrade.UpgradeModifiers;

import za.co.neroland.nerotech.NeroTechCommon;
import za.co.neroland.nerotech.config.NeroTechConfig;
import za.co.neroland.nerotech.heat.ThermalEnvironment;
import za.co.neroland.nerotech.machine.MachinePreset;
import za.co.neroland.nerotech.machine.MachineStats;
import za.co.neroland.nerotech.machine.NeroTechMachineBlockEntity;
import za.co.neroland.nerotech.pollution.PollutionManager;

/**
 * Server &rarr; client: the Stage G analytics snapshot for the machine behind an open menu,
 * keyed by container id and sent by {@code menu.MachineMenu#broadcastChanges} every 20 ticks
 * <b>only while that menu is open</b> — analytics never broadcast to non-viewers. Carries the
 * current status/values plus the full 60-second history window (shorts for the permille series,
 * unsigned bytes for the ops deltas — compact by design; &le; ~560 bytes per push).
 *
 * <p><b>Wire format delta (pollution/efficiency enrichment, 2026-07-11):</b> after {@code opsRate}
 * the payload now also carries, in order — {@code effSpeedPct}, {@code effEnergyPct} (varint;
 * UpgradeModifiers × MachinePreset as whole percentages), {@code ambientHeat} (varint, signed —
 * cold planets go negative), {@code heatRaw}, {@code heatCapacity}, {@code heatThrottle} (varint;
 * raw heat units for the thermal-context line + headroom colouring), {@code regionPollution},
 * {@code pollutionThreshold} (varint; the region's current level and {@code pollutionEventThreshold},
 * 0 = off), and {@code pollutionPerMinute} (varint, signed — negative for the Scrubber/Remediator
 * removal rate). A fourth history series ({@code pollutionHistory}, shorts, RAW region units
 * short-clamped — not permille) follows {@code opsHistory} using the same sample count. All
 * server-computed — the client never duplicates config/preset math. Both codec sides ship
 * together (session-scoped payload; no cross-version decode).
 *
 * <p><b>Privacy (POPIA/GDPR):</b> machine-scoped numbers only — status, rates, heat, energy,
 * regional pollution (a region is a place, never a person). No player names/UUIDs, never logged,
 * never persisted.
 *
 * @param containerId        the open menu's container id (the client mailbox key)
 * @param status             {@link za.co.neroland.nerotech.machine.MachineStatus} ordinal
 * @param heatPermille       current heat, permille of capacity
 * @param energyPermille     current stored energy, permille of capacity
 * @param opsRate            work ops in the most recent one-second sample
 * @param effSpeedPct        effective speed multiplier (upgrades × preset), whole percent
 * @param effEnergyPct       effective energy-cost multiplier (upgrades × preset), whole percent
 * @param ambientHeat        ambient heat at the machine (raw units, signed)
 * @param heatRaw            current heat in raw units
 * @param heatCapacity       heat capacity in raw units (the gauge scale)
 * @param heatThrottle       {@code heatThrottleThreshold} in raw units (headroom colouring)
 * @param regionPollution    current pollution of the machine's region (raw units)
 * @param pollutionThreshold {@code pollutionEventThreshold} (0 = threshold off)
 * @param pollutionPerMinute signed nominal rate: + emits, &minus; removes (Scrubber/Remediator)
 * @param heatHistory        heat permille samples, oldest &rarr; newest (&le; 60)
 * @param energyHistory      stored-energy permille samples, oldest &rarr; newest (&le; 60)
 * @param opsHistory         per-second ops deltas, oldest &rarr; newest, clamped to 0..255 (&le; 60)
 * @param pollutionHistory   regional pollution samples (RAW units, short-clamped), oldest &rarr; newest
 */
public record MachineStatsPayload(int containerId, int status, int heatPermille, int energyPermille,
        int opsRate, int effSpeedPct, int effEnergyPct, int ambientHeat, int heatRaw, int heatCapacity,
        int heatThrottle, int regionPollution, int pollutionThreshold, int pollutionPerMinute,
        short[] heatHistory, short[] energyHistory, byte[] opsHistory, short[] pollutionHistory)
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
        // Efficiency cluster: UpgradeModifiers × Stage H preset, folded to whole percentages here
        // on the server so the client renders numbers instead of re-deriving config math.
        UpgradeModifiers mods = machine.modifiers();
        MachinePreset preset = machine.preset();
        int effSpeedPct = (int) Math.round(mods.speedMultiplier() * preset.speedFactor() * 100.0D);
        int effEnergyPct = (int) Math.round(mods.energyMultiplier() * preset.energyFactor() * 100.0D);
        // Thermal + pollution context (level is present on the live server BE; null-guarded for
        // the ecj null-flow discipline anyway).
        Level level = machine.getLevel();
        int ambient = level == null ? 0 : ThermalEnvironment.ambientAt(level, machine.getBlockPos());
        int regionPollution = level instanceof ServerLevel serverLevel
                ? PollutionManager.regionPollution(serverLevel, machine.getBlockPos()) : 0;
        return new MachineStatsPayload(containerId, stats.status().ordinal(),
                machine.heatPermille(), machine.energyPermille(), stats.currentOpsRate(),
                effSpeedPct, effEnergyPct, ambient, machine.heat(), NeroTechConfig.heatCapacity(),
                NeroTechConfig.heatThrottleThreshold(), regionPollution,
                NeroTechConfig.pollutionEventThreshold(), machine.pollutionPerMinute(),
                stats.heatHistory(), stats.energyHistory(), opsBytes, stats.pollutionHistory());
    }

    private static void write(RegistryFriendlyByteBuf buf, MachineStatsPayload payload) {
        buf.writeVarInt(payload.containerId);
        buf.writeByte(payload.status);
        buf.writeShort(payload.heatPermille);
        buf.writeShort(payload.energyPermille);
        buf.writeVarInt(payload.opsRate);
        buf.writeVarInt(payload.effSpeedPct);
        buf.writeVarInt(payload.effEnergyPct);
        buf.writeVarInt(payload.ambientHeat);
        buf.writeVarInt(payload.heatRaw);
        buf.writeVarInt(payload.heatCapacity);
        buf.writeVarInt(payload.heatThrottle);
        buf.writeVarInt(payload.regionPollution);
        buf.writeVarInt(payload.pollutionThreshold);
        buf.writeVarInt(payload.pollutionPerMinute);
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
        // The pollution series is exported from the same ring — always the same length.
        for (int i = 0; i < samples; i++) {
            buf.writeShort(payload.pollutionHistory[i]);
        }
    }

    private static MachineStatsPayload read(RegistryFriendlyByteBuf buf) {
        int containerId = buf.readVarInt();
        int status = buf.readUnsignedByte();
        int heatPermille = buf.readShort();
        int energyPermille = buf.readShort();
        int opsRate = buf.readVarInt();
        int effSpeedPct = buf.readVarInt();
        int effEnergyPct = buf.readVarInt();
        int ambientHeat = buf.readVarInt();
        int heatRaw = buf.readVarInt();
        int heatCapacity = buf.readVarInt();
        int heatThrottle = buf.readVarInt();
        int regionPollution = buf.readVarInt();
        int pollutionThreshold = buf.readVarInt();
        int pollutionPerMinute = buf.readVarInt();
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
        short[] pollution = new short[samples];
        for (int i = 0; i < samples; i++) {
            pollution[i] = buf.readShort();
        }
        return new MachineStatsPayload(containerId, status, heatPermille, energyPermille, opsRate,
                effSpeedPct, effEnergyPct, ambientHeat, heatRaw, heatCapacity, heatThrottle,
                regionPollution, pollutionThreshold, pollutionPerMinute, heat, energy, ops, pollution);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
