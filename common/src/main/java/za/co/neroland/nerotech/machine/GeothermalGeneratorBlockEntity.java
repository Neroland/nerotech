package za.co.neroland.nerotech.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import za.co.neroland.nerolandcore.sideconfig.Channel;
import za.co.neroland.nerolandcore.sideconfig.SideConfig;
import za.co.neroland.nerolandcore.sideconfig.SidePreset;
import za.co.neroland.nerolandcore.upgrade.UpgradeModifiers;

import za.co.neroland.nerotech.config.NeroTechConfig;
import za.co.neroland.nerotech.menu.GeothermalGeneratorMenu;
import za.co.neroland.nerotech.registry.ModBlockEntities;

/**
 * Geothermal Generator (Stage D) — the steady, fuel-free baseline: it taps the {@value #FOOTPRINT}×
 * {@value #FOOTPRINT} of blocks <b>directly beneath</b> it and produces
 * {@code geothermalNePerTickPerSource} NE/tick for every lava or magma block it finds there (0–9).
 * Output never varies with time of day, weather or altitude — the trade is that it must sit on a
 * heat source and that tapping the mantle <b>runs the machine hot</b> (the Nero Generator's heat
 * curve, without its pollution).
 *
 * <p><b>Scan discipline:</b> the source count is cached and re-counted only when a neighbour change
 * invalidates it (piggybacking the thermal-link invalidation the block already fires) or on the
 * {@value #SCAN_INTERVAL}-tick revalidation cadence — never per tick. Unloaded chunk columns are
 * skipped rather than force-loaded.
 */
public class GeothermalGeneratorBlockEntity extends NeroTechMachineBlockEntity {

    /** Re-count cadence (ticks) — the Analytics Terminal / Fusion Reactor revalidation cadence. */
    private static final int SCAN_INTERVAL = 100;

    /** Edge length of the tapped footprint one block below the machine. */
    private static final int FOOTPRINT = 3;

    /** Cached heat-source count in the footprint; {@code -1} means "needs a re-count". */
    private int sourceCount = -1;

    /** Game time the cached count expires (the periodic revalidation half of the cache). */
    private long recountAt = Long.MIN_VALUE;

    public GeothermalGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GEOTHERMAL_GENERATOR.get(), pos, state, 0);
        setupSideConfig(SideConfig.builder()
                .channel(Channel.ENERGY)
                .defaultPreset(SidePreset.GENERATOR)
                .autoEject(Channel.ENERGY, true)
                .build());
    }

    /**
     * The block fires this on every neighbour change; the footprint sits one block below, so the
     * same event is exactly the right moment to drop the source cache too (event-driven, no polling).
     */
    @Override
    public void invalidateThermalLinks() {
        super.invalidateThermalLinks();
        this.sourceCount = -1;
    }

    @Override
    protected void tickMachine(Level level, BlockPos pos, BlockState state) {
        long now = level.getGameTime();
        if (this.sourceCount < 0 || now >= this.recountAt) {
            this.sourceCount = countSources(level, pos);
            this.recountAt = now + SCAN_INTERVAL;
        }

        boolean roomToStore = getEnergy().getAmount() < getEnergy().getCapacity();
        boolean producing = this.sourceCount > 0 && roomToStore;

        // Display hook: show "working" in the GUI while tapping (the Solar Array recipe).
        this.maxProgress = producing ? 1 : 0;
        this.progress = producing ? 1 : 0;
        setActive(producing);

        if (this.sourceCount <= 0) {
            // Analytics: a generator with nothing under it is starved, not merely idle.
            reportStatus(MachineStatus.STARVED);
        } else if (!roomToStore) {
            reportStatus(MachineStatus.BLOCKED);
        }

        if (producing) {
            UpgradeModifiers mods = modifiers();
            int rate = (int) Math.round((long) NeroTechConfig.geothermalNePerTickPerSource() * this.sourceCount
                    * mods.speedMultiplier() * presetSpeedFactor());
            if (rate > 0) {
                energyBuffer().generate(rate);
            }
            // Drawing on the mantle runs the machine hot — the geothermal trade-off. No pollution:
            // nothing is burned, so the emission path is never touched.
            addHeat(NeroTechConfig.heatPerOperation());
        }

        MachineEnergy.pushToNeighbours(level, pos, energyBuffer(), NeroTechConfig.machineMaxTransfer(), sideConfig());
    }

    /**
     * One bounded 3×3 count of lava / magma blocks in the layer directly beneath the machine,
     * skipping any unloaded column (the Analytics Terminal's {@code hasChunkAt} guard — a generator
     * at a chunk border must never force loads).
     */
    private int countSources(Level level, BlockPos pos) {
        int reach = FOOTPRINT / 2;
        int y = pos.getY() - 1;
        if (y < level.getMinY()) {
            return 0;
        }
        int found = 0;
        BlockPos.MutableBlockPos scan = new BlockPos.MutableBlockPos();
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dz = -reach; dz <= reach; dz++) {
                scan.set(pos.getX() + dx, y, pos.getZ() + dz);
                if (!level.hasChunkAt(scan)) {
                    continue; // unloaded column — skip, never force-load
                }
                BlockState below = level.getBlockState(scan);
                if (below.is(Blocks.LAVA) || below.is(Blocks.MAGMA_BLOCK)) {
                    found++;
                }
            }
        }
        return found;
    }

    /** Heat sources currently under the machine (0–9) — the menu's status readout. */
    public int sourceCount() {
        return Math.max(0, this.sourceCount);
    }

    /** A generator is never load-shed by a Grid Controller. */
    @Override
    public boolean shedable() {
        return false;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.nerotech.geothermal_generator");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new GeothermalGeneratorMenu(containerId, playerInventory, this, this.data);
    }
}
