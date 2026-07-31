package za.co.neroland.nerotech.machine;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import za.co.neroland.nerotech.config.NeroTechConfig;
import za.co.neroland.nerotech.menu.AnalyticsTerminalMenu;
import za.co.neroland.nerotech.registry.ModBlockEntities;

/**
 * Analytics Terminal — the Stage G <b>overview</b> console: a passive reader that scans for
 * NeroTech machines within {@code analyticsTerminalRadius} every {@value #SCAN_INTERVAL} ticks
 * (phase-spread, loaded chunks only) and caches their positions nearest-first. It consumes no NE
 * and never touches the machines it watches — the dashboard payload re-reads each machine's live
 * status/heat only while the terminal's menu is open (see
 * {@code network.AnalyticsTerminalPayload}). Slotless like the Remediator ({@code machineSlots = 0}).
 *
 * <p><b>Privacy (POPIA/GDPR):</b> the roster is machine positions only, transient (never
 * persisted) — no player names/UUIDs anywhere in the analytics path.
 */
public class AnalyticsTerminalBlockEntity extends NeroTechMachineBlockEntity {

    /** Rescan cadence (ticks) — the Fusion Reactor's formed-shell revalidation cadence. */
    private static final int SCAN_INTERVAL = 100;

    /**
     * Last scan's machine positions, nearest-first. Immutable snapshot swapped atomically per
     * scan so the menu's payload build never sees a half-built roster.
     */
    private List<BlockPos> roster = List.of();

    /**
     * Stage D power history: {@value #POWER_HISTORY} samples of the <b>net change in aggregate
     * stored NE</b> across the roster, one per rescan — five seconds per sample, so the strip covers
     * the last five minutes. Positive means the watched machines gained charge over that window
     * (generation outran demand), negative means they drained it.
     *
     * <p>A ring buffer written once per scan, never per tick, and — like the roster — transient:
     * machine-scoped numbers only, never persisted, no player data (POPIA/GDPR).
     */
    public static final int POWER_HISTORY = 60;

    private final int[] powerHistory = new int[POWER_HISTORY];
    private int historyHead;
    private int historyFilled;

    /** Aggregate stored NE at the previous scan; {@code -1} until the first scan sets a baseline. */
    private long previousStored = -1L;

    public AnalyticsTerminalBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ANALYTICS_TERMINAL.get(), pos, state, 0);
        // Passive reader: no side config — it moves no items and uses no energy.
    }

    /**
     * No energy gating, no work — the terminal only rescans its radius on the phase-spread
     * cadence. The active flag (BER hologram shimmer + RUNNING status) reads "the last scan
     * found at least one machine".
     */
    @Override
    protected void tickMachine(Level level, BlockPos pos, BlockState state) {
        if ((level.getGameTime() + Math.floorMod(pos.hashCode(), SCAN_INTERVAL)) % SCAN_INTERVAL == 0) {
            rescan(level, pos);
        }
        setActive(!this.roster.isEmpty());
    }

    /**
     * One bounded scan: walk the radius box chunk-column by chunk-column, skipping unloaded
     * columns ({@code hasChunkAt} guard — a terminal at a chunk border must never force loads),
     * and collect every {@link NeroTechMachineBlockEntity} position via
     * {@link BlockPos#betweenClosed} over the loaded intersection. Sorted nearest-first once per
     * scan so the payload's row cap is a plain prefix.
     */
    private void rescan(Level level, BlockPos pos) {
        int radius = NeroTechConfig.analyticsTerminalRadius();
        int minY = Math.max(level.getMinY(), pos.getY() - radius);
        int maxY = Math.min(level.getMaxY(), pos.getY() + radius);
        int minX = pos.getX() - radius;
        int maxX = pos.getX() + radius;
        int minZ = pos.getZ() - radius;
        int maxZ = pos.getZ() + radius;

        List<BlockPos> found = new ArrayList<>();
        long stored = 0L;
        for (int chunkX = minX >> 4; chunkX <= maxX >> 4; chunkX++) {
            for (int chunkZ = minZ >> 4; chunkZ <= maxZ >> 4; chunkZ++) {
                if (!level.hasChunkAt(chunkX << 4, chunkZ << 4)) {
                    continue; // unloaded column — skip, never force-load
                }
                int x0 = Math.max(minX, chunkX << 4);
                int x1 = Math.min(maxX, (chunkX << 4) + 15);
                int z0 = Math.max(minZ, chunkZ << 4);
                int z1 = Math.min(maxZ, (chunkZ << 4) + 15);
                for (BlockPos scan : BlockPos.betweenClosed(x0, minY, z0, x1, maxY, z1)) {
                    if (scan.equals(pos)) {
                        continue; // a console needn't monitor itself
                    }
                    if (level.getBlockEntity(scan) instanceof NeroTechMachineBlockEntity machine) {
                        found.add(scan.immutable());
                        stored += machine.getEnergy().getAmount();
                    }
                }
            }
        }
        found.sort(Comparator.comparingDouble(machinePos -> machinePos.distSqr(pos)));
        this.roster = List.copyOf(found);
        recordPower(stored);
    }

    /**
     * Push one power-history sample: the net change in aggregate stored NE since the previous scan.
     * The very first scan only sets the baseline — a delta against "nothing measured yet" would
     * read as a huge spike.
     */
    private void recordPower(long stored) {
        if (this.previousStored >= 0L) {
            long delta = stored - this.previousStored;
            this.powerHistory[this.historyHead] =
                    (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, delta));
            this.historyHead = (this.historyHead + 1) % POWER_HISTORY;
            this.historyFilled = Math.min(POWER_HISTORY, this.historyFilled + 1);
        }
        this.previousStored = stored;
    }

    /** The cached roster (nearest-first) the menu's dashboard payload is built from. */
    public List<BlockPos> roster() {
        return this.roster;
    }

    /** The power-history window, oldest sample first (empty until the second scan). */
    public int[] powerHistory() {
        int[] window = new int[this.historyFilled];
        int start = (this.historyHead - this.historyFilled + POWER_HISTORY) % POWER_HISTORY;
        for (int i = 0; i < this.historyFilled; i++) {
            window[i] = this.powerHistory[(start + i) % POWER_HISTORY];
        }
        return window;
    }

    /** A passive console has no work rate for a Grid Controller to throttle. */
    @Override
    public boolean shedable() {
        return false;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.nerotech.analytics_terminal");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new AnalyticsTerminalMenu(containerId, playerInventory, this, this.data);
    }
}
