package za.co.neroland.nerotech.machine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import za.co.neroland.nerotech.config.NeroTechConfig;
import za.co.neroland.nerotech.menu.GridControllerMenu;
import za.co.neroland.nerotech.registry.ModBlockEntities;

/**
 * Grid Controller (Stage D) — brownout protection. A passive console (zero NE, no slots, no side
 * config, the Analytics Terminal's posture) that rescans for NeroTech machines inside
 * {@code gridControllerRadius} every {@value #SCAN_INTERVAL} ticks and sums stored NE and capacity
 * across their buffers to get one <b>aggregate grid fill</b>.
 *
 * <p><b>Load shedding:</b> when the aggregate drops below {@code gridShedThresholdPermille} the
 * controller drops every {@linkplain NeroTechMachineBlockEntity#shedable() shedable} machine to
 * {@link MachinePreset#ECO} — generators, Battery Banks and consoles are exempt, so the grid throttles
 * <i>demand</i>, never supply. When it recovers above {@code gridRestorePermille} each shed machine
 * goes back to the preset it had. The two thresholds are deliberately separate: the gap is the
 * hysteresis band that stops a grid hovering at the line from flapping presets every scan.
 *
 * <p><b>Privacy (POPIA/GDPR):</b> machine positions and presets only, all transient — never
 * persisted, never any player identity. The remembered-preset map is in-memory for the same reason
 * the roster is: a reload starts the controller from a clean slate (machines shed before a restart
 * stay on Eco until the next shed/restore cycle or a manual preset change).
 */
public class GridControllerBlockEntity extends NeroTechMachineBlockEntity {

    /** Rescan cadence (ticks) — the Analytics Terminal's cadence; never a per-tick scan. */
    private static final int SCAN_INTERVAL = 100;

    /** Last scan's machine positions. Immutable snapshot swapped atomically per scan. */
    private List<BlockPos> roster = List.of();

    /** Presets remembered for the machines this controller shed, so recovery restores intent. */
    private final Map<BlockPos, MachinePreset> shedPresets = new HashMap<>();

    /** Whether the controller is currently shedding load (drives the status readout + BER). */
    private boolean shedding;

    /** Last scan's aggregate fill, permille of total capacity (menu readout). */
    private int fillPermille;

    public GridControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GRID_CONTROLLER.get(), pos, state, 0);
        // Passive supervisor: no side config — it moves nothing and uses no energy.
    }

    @Override
    protected void tickMachine(Level level, BlockPos pos, BlockState state) {
        if ((level.getGameTime() + Math.floorMod(pos.hashCode(), SCAN_INTERVAL)) % SCAN_INTERVAL == 0) {
            rescan(level, pos);
            evaluate(level);
        }
        // BER surface / analytics: "active" means the controller is intervening, not merely watching.
        setActive(this.shedding);
        if (this.shedding) {
            reportStatus(MachineStatus.THROTTLED);
        }
    }

    /**
     * One bounded scan of the radius, chunk-column by chunk-column, skipping unloaded columns
     * ({@code hasChunkAt} guard — a controller at a chunk border must never force loads). Collects
     * machine positions and the aggregate stored/capacity totals in the same pass.
     */
    private void rescan(Level level, BlockPos pos) {
        int radius = NeroTechConfig.gridControllerRadius();
        int minY = Math.max(level.getMinY(), pos.getY() - radius);
        int maxY = Math.min(level.getMaxY(), pos.getY() + radius);
        int minX = pos.getX() - radius;
        int maxX = pos.getX() + radius;
        int minZ = pos.getZ() - radius;
        int maxZ = pos.getZ() + radius;

        List<BlockPos> found = new ArrayList<>();
        long stored = 0L;
        long capacity = 0L;
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
                        continue; // a controller needn't manage itself
                    }
                    if (level.getBlockEntity(scan) instanceof NeroTechMachineBlockEntity machine) {
                        found.add(scan.immutable());
                        stored += machine.getEnergy().getAmount();
                        capacity += machine.getEnergy().getCapacity();
                    }
                }
            }
        }
        this.roster = List.copyOf(found);
        this.fillPermille = permille(stored, capacity);
    }

    /**
     * Apply the shed / restore decision for the freshly scanned grid. Below the shed threshold every
     * shedable machine drops to Eco (its previous preset remembered once); above the restore
     * threshold every remembered preset is put back. Between the two nothing changes — that gap is
     * the hysteresis band.
     */
    private void evaluate(Level level) {
        int shedBelow = NeroTechConfig.gridShedThresholdPermille();
        int restoreAbove = NeroTechConfig.gridRestorePermille();

        if (!this.roster.isEmpty() && this.fillPermille < shedBelow) {
            this.shedding = true;
            for (BlockPos target : this.roster) {
                if (!(level.getBlockEntity(target) instanceof NeroTechMachineBlockEntity machine)
                        || !machine.shedable() || machine.preset() == MachinePreset.ECO) {
                    continue;
                }
                this.shedPresets.putIfAbsent(target, machine.preset());
                machine.setPreset(MachinePreset.ECO);
            }
            return;
        }

        if (this.shedding && this.fillPermille > restoreAbove) {
            for (Map.Entry<BlockPos, MachinePreset> entry : this.shedPresets.entrySet()) {
                if (level.getBlockEntity(entry.getKey()) instanceof NeroTechMachineBlockEntity machine
                        && machine.preset() == MachinePreset.ECO) {
                    // Only machines still sitting where we left them: a player who re-presets a
                    // machine mid-brownout has overruled us, and that choice stands.
                    machine.setPreset(entry.getValue());
                }
            }
            this.shedPresets.clear();
            this.shedding = false;
        }
    }

    /** Machines in the last scan. */
    public int machinesSeen() {
        return this.roster.size();
    }

    /** Aggregate stored NE across the scanned machines, permille of their combined capacity. */
    public int gridFillPermille() {
        return this.fillPermille;
    }

    /** Whether load shedding is currently in force. */
    public boolean shedding() {
        return this.shedding;
    }

    // --- menu sync: three extra ContainerData ints after the seven shared ones -------------------

    @Override
    protected int extraDataCount() {
        return 3;
    }

    @Override
    protected int extraData(int index) {
        return switch (index) {
            case 0 -> this.roster.size();
            case 1 -> this.fillPermille;
            case 2 -> this.shedding ? 1 : 0;
            default -> 0;
        };
    }

    /** The supervisor never sheds itself. */
    @Override
    public boolean shedable() {
        return false;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.nerotech.grid_controller");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new GridControllerMenu(containerId, playerInventory, this, this.data);
    }
}
