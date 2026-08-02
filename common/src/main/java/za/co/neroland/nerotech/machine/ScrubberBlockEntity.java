package za.co.neroland.nerotech.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import za.co.neroland.nerolandcore.sideconfig.Channel;
import za.co.neroland.nerolandcore.sideconfig.SideConfig;
import za.co.neroland.nerolandcore.sideconfig.SidePreset;
import za.co.neroland.nerolandcore.sideconfig.SlotGroup;
import za.co.neroland.nerolandcore.upgrade.UpgradeModifiers;

import za.co.neroland.nerotech.config.NeroTechConfig;
import za.co.neroland.nerotech.menu.ScrubberMenu;
import za.co.neroland.nerotech.pollution.PollutionManager;
import za.co.neroland.nerotech.registry.ModBlockEntities;
import za.co.neroland.nerotech.registry.ModItems;

/**
 * Scrubber — the Stage F <b>prevention</b> machine: it eats NE and Filter Cartridges to pull
 * pollution out of its own region (and a permille share out of the 8 neighbouring regions)
 * through {@link PollutionManager#scrub}. Every point removed fouls the installed cartridge;
 * at {@code scrubberFilterCapacity} the cartridge is spent and a Dirty Filter drops into the
 * output slot (reprocessable in the Ore Processor for a partial material refund). Runs only
 * mildly warm ({@code heatPerOperation()/2} per op) and — by definition — emits no pollution.
 *
 * <p>Slots: 0 = filter cartridge (input), 1 = dirty filter (output). Ops batch on the
 * pollution-contribution interval with a per-machine phase (the {@code emitPollution} pattern),
 * so the region map is never touched per-tick.
 */
public class ScrubberBlockEntity extends NeroTechMachineBlockEntity {

    public static final int FILTER_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;

    /** Client-visible fouling granularity: sync fires on BUCKET change, never per scrub op. */
    public static final int FOULING_SYNC_BUCKETS = 6;

    /** Pollution scrubbed onto the current cartridge; persists (a half-fouled filter stays half-fouled). */
    private int fouling;

    /** Last fouling bucket pushed to clients (the renderSyncDirty compare-and-record state). */
    private int syncedFoulingBucket;

    /** Spreads scrub ops across ticks so scrubbers don't all hit the pollution map on the same tick. */
    private final int scrubPhase = Math.floorMod(System.identityHashCode(this), 40);

    public ScrubberBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SCRUBBER.get(), pos, state, 2);
        // PROCESSOR preset: cartridges (ITEM) in on every face except BOTTOM (= dirty-filter output),
        // ENERGY input on every face.
        setupSideConfig(SideConfig.builder()
                .channel(Channel.ITEM, SlotGroup.of("input", FILTER_SLOT), SlotGroup.of("output", OUTPUT_SLOT))
                .channel(Channel.ENERGY)
                .defaultPreset(SidePreset.PROCESSOR)
                .build());
    }

    /**
     * The Scrubber REMOVES pollution — a negative analytics rate: its nominal per-op scrub
     * (rate + the 8-region adjacent share, mirroring {@link PollutionManager#scrub}) per minute
     * on the contribution interval. Server-computed; the client never duplicates this math.
     */
    @Override
    public int pollutionPerMinute() {
        int rate = (int) Math.max(1, Math.round(NeroTechConfig.scrubberPollutionPerOp()
                * modifiers().speedMultiplier() * presetSpeedFactor()));
        int adjacent = (int) ((long) rate * NeroTechConfig.scrubberAdjacentPermille() / 1000L);
        int interval = Math.max(1, NeroTechConfig.pollutionContributionIntervalTicks());
        return -((rate + 8 * adjacent) * 1200 / interval);
    }

    @Override
    protected void tickMachine(Level level, BlockPos pos, BlockState state) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        UpgradeModifiers mods = modifiers();
        // Stage H preset: energy per op scales with the energy factor, scrub rate with the speed factor.
        int cost = (int) Math.max(0,
                Math.round(NeroTechConfig.scrubberNePerOp() * mods.energyMultiplier() * presetEnergyFactor()));

        // Analytics: the item/energy preconditions are cheap, so name the limiting cause every
        // tick (a status reported only on op ticks would be clobbered by the RUNNING/IDLE default
        // in between). "Nothing to scrub" stays the default IDLE — pollution is only knowable on
        // op ticks (region-map discipline), where a dry scrub leaves the active flag off.
        if (!this.items.get(FILTER_SLOT).is(ModItems.FILTER_CARTRIDGE.get())) {
            reportStatus(MachineStatus.STARVED);
        } else if (!outputHasRoom()) {
            reportStatus(MachineStatus.BLOCKED);
        } else if (!energyBuffer().has(cost)) {
            reportStatus(MachineStatus.NO_ENERGY);
        }

        // Batch on the pollution-contribution interval with a per-machine phase (emitPollution's
        // recipe) — between op ticks the active flag and gauges simply hold their last state.
        int interval = NeroTechConfig.pollutionContributionIntervalTicks();
        if ((serverLevel.getGameTime() + this.scrubPhase) % interval != 0) {
            return;
        }

        int rate = (int) Math.max(1, Math.round(NeroTechConfig.scrubberPollutionPerOp()
                * mods.speedMultiplier() * presetSpeedFactor()));

        boolean scrubbed = false;
        if (this.items.get(FILTER_SLOT).is(ModItems.FILTER_CARTRIDGE.get())
                && outputHasRoom()
                && energyBuffer().has(cost)) {
            int removed = PollutionManager.scrub(serverLevel, pos, rate,
                    NeroTechConfig.scrubberAdjacentPermille());
            if (removed > 0) {
                energyBuffer().consume(cost);
                this.fouling += removed;
                // Scrubbing runs only mildly warm — half a normal operation's heat, at least 1.
                addHeat(Math.max(1, NeroTechConfig.heatPerOperation() / 2));
                if (this.fouling >= NeroTechConfig.scrubberFilterCapacity()) {
                    spendFilter();
                }
                scrubbed = true;
                setChanged();
            }
        }

        // BER surface: the intake fan spins exactly while pollution is actually being removed.
        setActive(scrubbed);
        // GUI surface: the work bar reads as cartridge fouling while scrubbing.
        int capacity = NeroTechConfig.scrubberFilterCapacity();
        this.progress = scrubbed ? Math.min(this.fouling, capacity) : 0;
        this.maxProgress = scrubbed ? capacity : 0;
    }

    /** Room for one more Dirty Filter in the output slot. */
    private boolean outputHasRoom() {
        ItemStack out = this.items.get(OUTPUT_SLOT);
        return out.isEmpty()
                || (out.is(ModItems.DIRTY_FILTER.get()) && out.getCount() < out.getMaxStackSize());
    }

    /** Cartridge fully fouled: consume it, emit a Dirty Filter, reset the fouling meter. */
    private void spendFilter() {
        this.items.get(FILTER_SLOT).shrink(1);
        ItemStack out = this.items.get(OUTPUT_SLOT);
        if (out.isEmpty()) {
            this.items.set(OUTPUT_SLOT, new ItemStack(ModItems.DIRTY_FILTER.get()));
        } else {
            out.grow(1);
        }
        this.fouling = 0;
    }

    // --- BER client surface (fouling rides the update tag; sync on bucket change only) ----------

    /** Fouling as a 0..1 fraction of cartridge capacity — the BER's filter-darkening lerp input. */
    public float foulingFraction() {
        int capacity = NeroTechConfig.scrubberFilterCapacity();
        return capacity <= 0 ? 0.0F : Math.min(1.0F, (float) this.fouling / capacity);
    }

    /**
     * Re-sync when the fouling BUCKET moves ({@value #FOULING_SYNC_BUCKETS} buckets over cartridge
     * capacity) — the heat-bucket sync discipline applied to the cartridge-darkening visual.
     */
    @Override
    protected boolean renderSyncDirty() {
        int capacity = NeroTechConfig.scrubberFilterCapacity();
        int bucket = capacity <= 0 ? 0
                : Math.min(FOULING_SYNC_BUCKETS - 1, this.fouling * FOULING_SYNC_BUCKETS / capacity);
        if (bucket != this.syncedFoulingBucket) {
            this.syncedFoulingBucket = bucket;
            return true;
        }
        return false;
    }

    // --- persistence ----------------------------------------------------------------------------

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Fouling", this.fouling);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.fouling = input.getIntOr("Fouling", 0);
    }

    // --- container ------------------------------------------------------------------------------

    @Override
    public boolean canPlaceMachineItem(int slot, ItemStack stack) {
        return slot == FILTER_SLOT && stack.is(ModItems.FILTER_CARTRIDGE.get());
    }

    @Override
    public boolean canTakeMachineItem(int slot) {
        return slot == OUTPUT_SLOT;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.nerotech.scrubber");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ScrubberMenu(containerId, playerInventory, this, this.data);
    }
}
