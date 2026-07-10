package za.co.neroland.nerotech.machine;

import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.machine.AbstractMachineBlockEntity;
import za.co.neroland.nerolandcore.sideconfig.Channel;
import za.co.neroland.nerolandcore.sideconfig.SideConfig;
import za.co.neroland.nerolandcore.sideconfig.SideConfigComponent;

import za.co.neroland.nerotech.config.NeroTechConfig;
import za.co.neroland.nerotech.heat.ThermalEnvironment;
import za.co.neroland.nerotech.heat.ThermalMath;
import za.co.neroland.nerotech.pollution.PollutionManager;
import za.co.neroland.nerotech.upgrade.UpgradeModuleItem;

/**
 * Shared base for NeroTech's Tier-1 machine block-entities. Extends Core's
 * {@link AbstractMachineBlockEntity} (which supplies the {@code EnergyBuffer}, the
 * {@code UpgradeContainer} and their persistence) and adds the parts a GUI machine needs: a small
 * fixed bank of machine I/O slots, a combined {@link Container} view (machine slots followed by the
 * Core upgrade slots) for the menu, GUI {@link ContainerData} sync, and save/load of the extra state.
 *
 * <p>Energy and upgrades are NOT re-implemented — they come from Core. Subclasses override
 * {@link #serverTick} and {@link #createMenu}.
 */
public abstract class NeroTechMachineBlockEntity extends AbstractMachineBlockEntity
        implements WorldlyContainer, MenuProvider {

    public static final int UPGRADE_SLOTS = 4;

    protected final int machineSlots;
    protected final NonNullList<ItemStack> items;
    /** Machine I/O slot indices (excludes Core upgrade slots) — the sided handoff face for pipes/logistics. */
    private final int[] machineFaceSlots;

    /** Work progress (ticks) — burn time for generators, processing time for processors. */
    protected int progress;
    protected int maxProgress;

    /**
     * Heat (Stage 3 consequence axis; full thermal model per Stage C decision 2026-07-10):
     * accumulates while working, relaxes toward the location's ambient level (dimension + biome,
     * see {@link za.co.neroland.nerotech.heat.ThermalEnvironment}), conducts to/from adjacent
     * machines, and sheds extra next to coolant blocks.
     */
    protected int heat;

    /**
     * Cached positions of adjacent machine BEs for heat conduction. Rebuilt lazily after
     * {@link #invalidateThermalLinks()} (neighbour change / load) — never scanned per tick.
     */
    @Nullable
    private java.util.List<BlockPos> thermalLinks;

    /** Adjacent coolant-block faces (water/ice/snow), cached with the links. */
    private int coolantFaces;

    /** Cached ambient heat for this position; refreshed on an interval (dimension never changes in-place). */
    private int ambientCache;
    private long ambientCacheUntil = Long.MIN_VALUE;

    /** Spreads conduction exchanges across ticks so linked pairs don't all fire on the same tick. */
    private final int thermalPhase = Math.floorMod(System.identityHashCode(this) * 31, 1024);

    /** Placing player's UUID — captured only when per-player pollution attribution is enabled. */
    @Nullable
    protected UUID ownerId;

    /** Spreads pollution contributions across ticks so machines don't all flush on the same tick. */
    private final int pollutionPhase = Math.floorMod(System.identityHashCode(this), 40);

    /**
     * Synced to the menu: [0]=energy permille, [1]=1000, [2]=work permille, [3]=1000 when working,
     * [4]=heat permille, [5]=1000.
     */
    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> permille(getEnergy().getAmount(), getEnergy().getCapacity());
                case 1 -> 1000;
                case 2 -> permille(progress, maxProgress);
                case 3 -> maxProgress > 0 ? 1000 : 0;
                case 4 -> permille(heat, NeroTechConfig.heatCapacity());
                case 5 -> 1000;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            // Client-authoritative sync target is the menu's own SimpleContainerData; nothing to store here.
        }

        @Override
        public int getCount() {
            return 6;
        }
    };

    protected NeroTechMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
            int machineSlots) {
        super(type, pos, state,
                NeroTechConfig.machineEnergyCapacity(), NeroTechConfig.machineMaxTransfer(),
                UPGRADE_SLOTS, UpgradeModuleItem.CLASSIFIER);
        this.machineSlots = machineSlots;
        this.items = NonNullList.withSize(machineSlots, ItemStack.EMPTY);
        this.machineFaceSlots = java.util.stream.IntStream.range(0, machineSlots).toArray();
    }

    private static int permille(long amount, long max) {
        return max <= 0 ? 0 : (int) Math.max(0, Math.min(1000, amount * 1000L / max));
    }

    public ContainerData containerData() {
        return this.data;
    }

    /** Whether a machine I/O slot (0..machineSlots-1) accepts {@code stack}. Override per machine. */
    public boolean canPlaceMachineItem(int slot, ItemStack stack) {
        return true;
    }

    /** Whether a machine I/O slot may be extracted from through a face (e.g. output slots). Override. */
    public boolean canTakeMachineItem(int slot) {
        return false;
    }

    /**
     * Install Core's universal side-config component for this machine and wire it to this BE's own
     * {@link Container} view (machine slots followed by the upgrade slots). Energy is pre-wired by
     * Core. Call once from a subclass constructor after {@code super(...)} with the machine's declared
     * {@link SideConfig}. Persistence + auto-transfer are then handled automatically by Core's tick.
     */
    protected void setupSideConfig(SideConfig config) {
        installSideConfig(config).withItems(() -> this);
    }

    /** True when this machine has an installed side config that declares the item channel. */
    private boolean hasItemSideConfig() {
        SideConfigComponent comp = sideConfig();
        return comp != null && comp.config().has(Channel.ITEM);
    }

    // --- WorldlyContainer: the sided item handoff surface for pipes / NeroLogistics --------------
    // When a side config with an ITEM channel is installed it drives per-face routing; otherwise the
    // legacy all-face exposure applies. Core upgrade slots are never exposed to automation either way.

    @Override
    public int[] getSlotsForFace(net.minecraft.core.Direction side) {
        if (hasItemSideConfig()) {
            return sideConfig().itemSlotsForFace(side);
        }
        return this.machineFaceSlots;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable net.minecraft.core.Direction side) {
        if (hasItemSideConfig()) {
            return side != null && sideConfig().canInsertItem(slot, side) && canPlaceMachineItem(slot, stack);
        }
        return slot < this.machineSlots && canPlaceMachineItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, net.minecraft.core.Direction side) {
        if (hasItemSideConfig()) {
            return side != null && sideConfig().canExtractItem(slot, side);
        }
        return slot < this.machineSlots && canTakeMachineItem(slot);
    }

    // --- heat + pollution (Stage 3 consequence systems) ---------------------

    @Override
    protected final void serverTick(Level level, BlockPos pos, BlockState state) {
        tickMachine(level, pos, state);
        thermalTick(level, pos);
    }

    /** Per-machine server logic. The thermal exchange runs automatically after this each tick. */
    protected abstract void tickMachine(Level level, BlockPos pos, BlockState state);

    /** Add heat, clamped to capacity. */
    protected void addHeat(int amount) {
        if (amount > 0) {
            this.heat = Math.min(NeroTechConfig.heatCapacity(), this.heat + amount);
            setChanged();
        }
    }

    /**
     * Full thermal model, one tick: relax toward ambient (dimension + biome), shed extra next to
     * coolant blocks, and — on this machine's phase of the exchange interval — conduct heat
     * with cached adjacent machines. Costs a few integer ops per tick; the neighbour scan happens
     * only when the link cache was invalidated by a neighbour change.
     */
    protected void thermalTick(Level level, BlockPos pos) {
        int before = this.heat;
        int ambient = ambient(level, pos);

        // Environmental exchange: cool toward ambient when hot, warm toward it when cold.
        this.heat += ThermalMath.ambientStep(this.heat, ambient, NeroTechConfig.thermalEnvLossPermille());

        // Coolant adjacency only ever cools, and never below ambient. The coolant count is
        // cached alongside the conduction links (rebuilt on neighbour change, not per tick).
        if (this.heat > ambient && this.coolantFaces > 0) {
            this.heat = Math.max(ambient,
                    this.heat - this.coolantFaces * NeroTechConfig.heatDissipationPerTick());
        }

        // Machine-to-machine conduction, on this machine's phase of the interval.
        int interval = NeroTechConfig.thermalExchangeIntervalTicks();
        if ((level.getGameTime() + this.thermalPhase) % interval == 0) {
            if (this.thermalLinks == null) {
                rebuildThermalLinks(level);
            }
            conductWithNeighbours(level);
        }

        this.heat = ThermalMath.clampHeat(this.heat, NeroTechConfig.heatCapacity());
        if (this.heat != before) {
            setChanged();
        }
    }

    /** Ambient heat here, cached and refreshed every 200 ticks (biome/dimension are near-static). */
    private int ambient(Level level, BlockPos pos) {
        long now = level.getGameTime();
        if (now >= this.ambientCacheUntil) {
            this.ambientCache = ThermalEnvironment.ambientAt(level, pos);
            this.ambientCacheUntil = now + 200;
        }
        return this.ambientCache;
    }

    /**
     * Exchange heat with cached adjacent machines ({@link ThermalMath#conductionStep} — moves a
     * share of the temperature difference, symmetric, hot to cold). Both sides of a pair run
     * this on their own phase, so a pair exchanges ~twice per interval; the default
     * conductivity accounts for that.
     */
    private void conductWithNeighbours(Level level) {
        int conductivity = NeroTechConfig.thermalConductivityPermille();
        if (conductivity <= 0 || this.thermalLinks == null) {
            return;
        }
        for (BlockPos neighbourPos : this.thermalLinks) {
            if (level.getBlockEntity(neighbourPos) instanceof NeroTechMachineBlockEntity other) {
                int step = ThermalMath.conductionStep(this.heat, other.heat, conductivity);
                if (step != 0) {
                    int capacity = NeroTechConfig.heatCapacity();
                    this.heat = ThermalMath.clampHeat(this.heat - step, capacity);
                    other.heat = ThermalMath.clampHeat(other.heat + step, capacity);
                    other.setChanged();
                }
            } else {
                // Stale link (machine broken without a neighbour-change reaching us): rebuild next pass.
                this.thermalLinks = null;
                return;
            }
        }
    }

    /** One 6-face scan for machine links + coolant faces — only after invalidation, never per tick. */
    private void rebuildThermalLinks(Level level) {
        java.util.List<BlockPos> links = new java.util.ArrayList<>(6);
        int coolants = 0;
        for (Direction side : Direction.values()) {
            BlockPos neighbourPos = this.worldPosition.relative(side);
            if (level.getBlockEntity(neighbourPos) instanceof NeroTechMachineBlockEntity) {
                links.add(neighbourPos.immutable());
                continue;
            }
            BlockState ns = level.getBlockState(neighbourPos);
            if (ns.is(Blocks.WATER) || ns.is(Blocks.ICE) || ns.is(Blocks.PACKED_ICE) || ns.is(Blocks.BLUE_ICE)
                    || ns.is(Blocks.SNOW_BLOCK) || ns.is(Blocks.POWDER_SNOW)) {
                coolants++;
            }
        }
        this.thermalLinks = links;
        this.coolantFaces = coolants;
    }

    /** Drop the cached conduction links; called by the block on neighbour changes and on load. */
    public void invalidateThermalLinks() {
        this.thermalLinks = null;
    }

    /** True once heat reaches the throttle threshold — processing machines stall until cooled. */
    public boolean overheated() {
        return this.heat >= NeroTechConfig.heatThrottleThreshold();
    }

    public int heat() {
        return this.heat;
    }

    /** Capture the placing player (only stored when per-player attribution is enabled). */
    public void setOwner(@Nullable UUID owner) {
        this.ownerId = owner;
    }

    /**
     * Emit this machine's pollution into its region, batched on a per-machine phase so contributions
     * spread across ticks (never a global per-tick scan). Call from {@link #tickMachine} while working.
     */
    protected void emitPollution(Level level, BlockPos pos) {
        int amount = NeroTechConfig.pollutionPerOperation();
        if (amount <= 0 || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        int interval = NeroTechConfig.pollutionContributionIntervalTicks();
        if ((serverLevel.getGameTime() + this.pollutionPhase) % interval == 0) {
            PollutionManager.record(serverLevel, pos, amount, this.ownerId);
        }
    }

    // --- persistence (Core's super handles energy + upgrades) ----------------

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Progress", this.progress);
        output.putInt("MaxProgress", this.maxProgress);
        output.putInt("Heat", this.heat);
        output.putLong("OwnerMost", this.ownerId == null ? 0L : this.ownerId.getMostSignificantBits());
        output.putLong("OwnerLeast", this.ownerId == null ? 0L : this.ownerId.getLeastSignificantBits());
        for (int i = 0; i < this.items.size(); i++) {
            output.store("Item" + i, ItemStack.OPTIONAL_CODEC, this.items.get(i));
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.progress = input.getIntOr("Progress", 0);
        this.maxProgress = input.getIntOr("MaxProgress", 0);
        this.heat = input.getIntOr("Heat", 0);
        long ownerMost = input.getLongOr("OwnerMost", 0L);
        long ownerLeast = input.getLongOr("OwnerLeast", 0L);
        this.ownerId = (ownerMost == 0L && ownerLeast == 0L) ? null : new UUID(ownerMost, ownerLeast);
        for (int i = 0; i < this.items.size(); i++) {
            this.items.set(i, input.read("Item" + i, ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
        }
    }

    // --- Container (machine slots then Core upgrade slots) -------------------

    @Override
    public int getContainerSize() {
        return this.machineSlots + upgrades().slots();
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < getContainerSize(); i++) {
            if (!getItem(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        if (slot < this.machineSlots) {
            return this.items.get(slot);
        }
        return upgrades().getStack(slot - this.machineSlots);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack current = getItem(slot);
        if (current.isEmpty() || amount <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack taken = current.split(amount);
        if (!taken.isEmpty()) {
            setChanged();
        }
        return taken;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack current = getItem(slot);
        setItem(slot, ItemStack.EMPTY);
        return current;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot < this.machineSlots) {
            this.items.set(slot, stack);
            setChanged();
        } else {
            upgrades().setStack(slot - this.machineSlots, stack);
        }
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot < this.machineSlots) {
            return canPlaceMachineItem(slot, stack);
        }
        return upgrades().isModule(stack);
    }

    @Override
    public boolean stillValid(Player player) {
        return this.level != null && this.level.getBlockEntity(this.worldPosition) == this
                && player.distanceToSqr(this.worldPosition.getX() + 0.5D, this.worldPosition.getY() + 0.5D,
                this.worldPosition.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public void clearContent() {
        this.items.clear();
        for (int i = 0; i < upgrades().slots(); i++) {
            upgrades().setStack(i, ItemStack.EMPTY);
        }
    }
}
