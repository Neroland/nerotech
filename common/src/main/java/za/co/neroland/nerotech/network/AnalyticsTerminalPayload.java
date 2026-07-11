package za.co.neroland.nerotech.network;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerotech.NeroTechCommon;
import za.co.neroland.nerotech.config.NeroTechConfig;
import za.co.neroland.nerotech.machine.AnalyticsTerminalBlockEntity;
import za.co.neroland.nerotech.machine.MachineStatus;
import za.co.neroland.nerotech.machine.NeroTechMachineBlockEntity;
import za.co.neroland.nerotech.pollution.PollutionManager;

/**
 * Server &rarr; client: the Analytics Terminal's dashboard snapshot, keyed by container id and
 * sent every 20 ticks while the terminal's menu is open (the {@link MachineStatsPayload} cadence).
 * Built from the terminal's cached roster of machine <i>positions</i> with each machine's status
 * and heat re-read live at send time — never any remote ring buffers (Stage G scope). Row offsets
 * are relative to the terminal and fit signed bytes (scan radius caps at 64).
 *
 * <p><b>Wire format delta (pollution enrichment, 2026-07-11):</b> two varints appended after the
 * rows — {@code regionPollution} (total pollution across the DISTINCT regions the roster's
 * machines occupy, deduped by region key server-side) and {@code pollutionThreshold}
 * ({@code pollutionEventThreshold}, 0 = off, the dashboard's severity scale). Both codec sides
 * ship together (session-scoped payload; no cross-version decode).
 *
 * <p><b>Privacy (POPIA/GDPR):</b> machine positions, statuses, heat and regional pollution only
 * (a region key is a place, never a person) — never player names/UUIDs, never logged, never
 * persisted.
 *
 * @param containerId        the open menu's container id (the client mailbox key)
 * @param machineCount       machines in the last scan's roster
 * @param activeCount        of those, how many are currently working
 * @param statusCounts       per-{@link MachineStatus}-ordinal machine counts (dashboard header)
 * @param hottestPos         absolute position of the hottest machine, or {@code null} when none
 * @param hottestPermille    that machine's heat, permille of capacity
 * @param rows               up to {@value #MAX_ROWS} nearest machines, nearest first
 * @param regionPollution    total pollution across the roster's distinct regions (deduped)
 * @param pollutionThreshold {@code pollutionEventThreshold} (0 = threshold off)
 */
public record AnalyticsTerminalPayload(int containerId, int machineCount, int activeCount,
        int[] statusCounts, @Nullable BlockPos hottestPos, int hottestPermille, List<Row> rows,
        int regionPollution, int pollutionThreshold)
        implements CustomPacketPayload {

    /** One dashboard row: a machine's offset from the terminal, its status ordinal and heat. */
    public record Row(int dx, int dy, int dz, int status, int heatPermille) {
    }

    /** Dashboard row cap — the nearest machines only; the header still counts the whole roster. */
    public static final int MAX_ROWS = 12;

    public static final Type<AnalyticsTerminalPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(NeroTechCommon.MOD_ID, "analytics_terminal"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AnalyticsTerminalPayload> STREAM_CODEC =
            StreamCodec.of(AnalyticsTerminalPayload::write, AnalyticsTerminalPayload::read);

    /**
     * Snapshot the terminal's dashboard (server side; menu send path): walk the cached
     * nearest-first roster, re-reading each machine's live status/heat and dropping entries whose
     * block entity has vanished since the last scan (the next scan prunes them properly).
     */
    public static AnalyticsTerminalPayload of(int containerId, AnalyticsTerminalBlockEntity terminal) {
        int[] counts = new int[MachineStatus.VALUES.length];
        List<Row> rows = new ArrayList<>(MAX_ROWS);
        int machines = 0;
        int active = 0;
        BlockPos hottestPos = null;
        int hottestPermille = -1;
        int threshold = NeroTechConfig.pollutionEventThreshold();
        Level level = terminal.getLevel();
        if (level == null) {
            return new AnalyticsTerminalPayload(containerId, 0, 0, counts, null, 0, rows, 0, threshold);
        }
        ServerLevel serverLevel = level instanceof ServerLevel sl ? sl : null;
        // Aggregate pollution across the DISTINCT regions the roster's machines sit in — deduped
        // by region key so ten machines in one region count that region's level exactly once.
        Set<Long> regions = new HashSet<>();
        int regionPollution = 0;
        BlockPos origin = terminal.getBlockPos();
        for (BlockPos pos : terminal.roster()) {
            if (!(level.getBlockEntity(pos) instanceof NeroTechMachineBlockEntity machine)) {
                continue; // broken since the last scan
            }
            machines++;
            counts[machine.stats().status().ordinal()]++;
            if (machine.renderActive()) {
                active++;
            }
            if (serverLevel != null && regions.add(PollutionManager.regionKey(pos))) {
                regionPollution += PollutionManager.regionPollution(serverLevel, pos);
            }
            int heat = machine.heatPermille();
            if (heat > hottestPermille) {
                hottestPermille = heat;
                hottestPos = pos;
            }
            if (rows.size() < MAX_ROWS) {
                rows.add(new Row(pos.getX() - origin.getX(), pos.getY() - origin.getY(),
                        pos.getZ() - origin.getZ(), machine.stats().status().ordinal(), heat));
            }
        }
        return new AnalyticsTerminalPayload(containerId, machines, active, counts,
                hottestPos, Math.max(0, hottestPermille), rows, regionPollution, threshold);
    }

    private static void write(RegistryFriendlyByteBuf buf, AnalyticsTerminalPayload payload) {
        buf.writeVarInt(payload.containerId);
        buf.writeVarInt(payload.machineCount);
        buf.writeVarInt(payload.activeCount);
        int statuses = Math.min(MachineStatus.VALUES.length, payload.statusCounts.length);
        buf.writeByte(statuses);
        for (int i = 0; i < statuses; i++) {
            buf.writeVarInt(payload.statusCounts[i]);
        }
        BlockPos hottest = payload.hottestPos;
        buf.writeBoolean(hottest != null);
        if (hottest != null) {
            buf.writeBlockPos(hottest);
            buf.writeShort(payload.hottestPermille);
        }
        buf.writeByte(Math.min(MAX_ROWS, payload.rows.size()));
        for (int i = 0; i < Math.min(MAX_ROWS, payload.rows.size()); i++) {
            Row row = payload.rows.get(i);
            buf.writeByte(row.dx);
            buf.writeByte(row.dy);
            buf.writeByte(row.dz);
            buf.writeByte(row.status);
            buf.writeShort(row.heatPermille);
        }
        buf.writeVarInt(payload.regionPollution);
        buf.writeVarInt(payload.pollutionThreshold);
    }

    private static AnalyticsTerminalPayload read(RegistryFriendlyByteBuf buf) {
        int containerId = buf.readVarInt();
        int machineCount = buf.readVarInt();
        int activeCount = buf.readVarInt();
        int statuses = Math.min(MachineStatus.VALUES.length, buf.readUnsignedByte());
        int[] counts = new int[MachineStatus.VALUES.length];
        for (int i = 0; i < statuses; i++) {
            counts[i] = buf.readVarInt();
        }
        BlockPos hottestPos = null;
        int hottestPermille = 0;
        if (buf.readBoolean()) {
            hottestPos = buf.readBlockPos();
            hottestPermille = buf.readShort();
        }
        int rowCount = Math.min(MAX_ROWS, buf.readUnsignedByte());
        List<Row> rows = new ArrayList<>(rowCount);
        for (int i = 0; i < rowCount; i++) {
            rows.add(new Row(buf.readByte(), buf.readByte(), buf.readByte(),
                    buf.readUnsignedByte(), buf.readShort()));
        }
        int regionPollution = buf.readVarInt();
        int pollutionThreshold = buf.readVarInt();
        return new AnalyticsTerminalPayload(containerId, machineCount, activeCount, counts,
                hottestPos, hottestPermille, rows, regionPollution, pollutionThreshold);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
