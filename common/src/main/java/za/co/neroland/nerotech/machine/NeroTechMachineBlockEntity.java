package za.co.neroland.nerotech.machine;

import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
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

    /** Client-visible heat granularity: sync fires on BUCKET change over heatCapacity, never per raw heat tick. */
    public static final int HEAT_SYNC_BUCKETS = 6;

    /**
     * Client-visible "the machine is working" flag (burning / processing / generating), driven by
     * subclasses from {@link #tickMachine} via {@link #setActive}. BERs gate their dynamic geometry on
     * it. Synced to watching clients only when it flips (see {@link #syncRenderState}).
     */
    protected boolean active;

    /**
     * Client-visible pulse counter for one-shot machines (Auto Crafter craft, Item Sorter sort): each
     * server-side {@link #pulseClient} bumps it, and the BER plays a short animation when the synced
     * value changes. Wraps harmlessly; synced only on change.
     */
    protected int clientPulse;

    /** Last values pushed to clients — the sync-discipline comparators (active flip / heat bucket / pulse). */
    private boolean syncedActive;
    private int syncedHeatBucket;
    private int syncedPulse;

    // --- client-side BER display state (never saved, never synced; POPIA: pure visuals, no player data).
    // The quarry-renderer easing recipe: the BER eases displayPos toward its target ONCE per game tick
    // (storing the previous tick's value) and lerps by partialTick, so motion is FPS-independent.

    /** Whether the display easing has been seeded (client render thread only). */
    public boolean displayInit;
    /** Current eased display scalar (e.g. the Fabricator arm's traverse position). */
    public double displayPos;
    /** Previous tick's eased display scalar (lerp partner). */
    public double prevDisplayPos;
    /** Game tick the easing last advanced on. */
    public long displayLastTick;
    /** Pulse-change detection: the last synced {@link #renderPulse()} the client renderer consumed... */
    public int clientSeenPulse = Integer.MIN_VALUE;
    /** ...and the game time it arrived (start of the client-side pulse animation). */
    public long clientPulseTime = Long.MIN_VALUE;

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
     * Stage G analytics window: 60 one-second samples of heat/energy/ops plus the current
     * {@link MachineStatus}. Transient — never persisted, and it leaves the server only inside
     * the menu-open stats payload. Machine-scoped numbers only, no player data (POPIA/GDPR).
     */
    private final MachineStats stats = new MachineStats();

    /** Whether a subclass called {@link #reportStatus} this tick (else the RUNNING/IDLE default applies). */
    private boolean statusReported;

    /**
     * Stage H overclock preset — a free GUI selector trading speed against energy/heat/pollution
     * (see {@link MachinePreset} for the curve). Persisted as its ordinal ({@code "Preset"}), which
     * also rides the BE update tag automatically via {@link #saveAdditional}, so the BER-side heat
     * consequences stay in sync. Synced to menus as {@code ContainerData} index 6.
     */
    protected MachinePreset preset = MachinePreset.BALANCED;

    /** Spreads the once-per-second stats sample across ticks (the pollutionPhase recipe, mod 20). */
    private final int statsPhase = Math.floorMod(System.identityHashCode(this) * 61, 20);

    /**
     * Synced to the menu: [0]=energy permille, [1]=1000, [2]=work permille, [3]=1000 when working,
     * [4]=heat permille, [5]=1000, [6]=preset ordinal ({@link MachinePreset}).
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
                case 6 -> preset.ordinal();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            // Client-authoritative sync target is the menu's own SimpleContainerData; nothing to store here.
        }

        @Override
        public int getCount() {
            return 7;
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
        this.statusReported = false;
        tickMachine(level, pos, state);
        // Analytics default: RUNNING while visibly working, IDLE otherwise — subclasses that know
        // the sharper cause (STARVED/BLOCKED/THROTTLED/...) reported it from tickMachine.
        if (!this.statusReported) {
            this.stats.status(this.active ? MachineStatus.RUNNING : MachineStatus.IDLE);
        }
        thermalTick(level, pos);
        statsTick(level);
        syncRenderState(level, pos, state);
    }

    /** Per-machine server logic. The thermal exchange runs automatically after this each tick. */
    protected abstract void tickMachine(Level level, BlockPos pos, BlockState state);

    // --- BER client sync (active flag + heat bucket + pulse ride the BE update packet) -----------

    /** Subclasses flag work-in-progress from {@link #tickMachine}; drives the BER "running" animations. */
    protected void setActive(boolean value) {
        this.active = value;
    }

    /** Bump the client pulse counter (one-shot machines); the BER plays a short animation per bump. */
    protected void pulseClient() {
        this.clientPulse = (this.clientPulse + 1) & 0xFFFF;
        // One-shot machines count each pulse as a work op toward the analytics rate.
        this.stats.countOps(1);
    }

    // --- Stage G analytics (menu-open-only sync; see network.MachineStatsPayload) ----------------

    /**
     * Name what currently limits this machine (called from {@link #tickMachine}); the base default
     * (RUNNING while active, IDLE otherwise) applies on any tick without a report, so subclasses
     * only report where they know better.
     */
    protected void reportStatus(MachineStatus status) {
        this.stats.status(status);
        this.statusReported = true;
    }

    /**
     * Count working ticks and take the once-per-second analytics sample on this machine's phase
     * of the 20-tick interval (the pollutionPhase recipe) — never a per-tick array write.
     */
    private void statsTick(Level level) {
        if (this.active) {
            this.stats.countOps(1);
        }
        if ((level.getGameTime() + this.statsPhase) % 20 == 0) {
            this.stats.sample(heatPermille(), energyPermille());
        }
    }

    /** The analytics window (server-side read surface for the menu's stats payload). */
    public MachineStats stats() {
        return this.stats;
    }

    /** Heat as permille of capacity (analytics payload scale). */
    public int heatPermille() {
        return permille(this.heat, NeroTechConfig.heatCapacity());
    }

    /** Stored energy as permille of capacity (analytics payload scale). */
    public int energyPermille() {
        return permille(getEnergy().getAmount(), getEnergy().getCapacity());
    }

    /**
     * Hook for subclass-specific synced render state (compare-and-record inside the override; return
     * true when a client-visible visual changed — e.g. the Auto Crafter's hologram item, the Item
     * Sorter's port modes). Called once per server tick; default is never dirty.
     */
    protected boolean renderSyncDirty() {
        return false;
    }

    /**
     * Push a BE update packet when — and only when — the client-visible render surface changed: the
     * active flag flipped, the heat BUCKET moved ({@value #HEAT_SYNC_BUCKETS} buckets over
     * {@code heatCapacity}), the pulse counter bumped, or {@link #renderSyncDirty} reports a change.
     * Never per-tick (the MODELS.md sync discipline).
     */
    private void syncRenderState(Level level, BlockPos pos, BlockState state) {
        int bucket = heatBucket();
        boolean dirty = this.active != this.syncedActive
                || bucket != this.syncedHeatBucket
                || this.clientPulse != this.syncedPulse;
        // Evaluate the hook even when already dirty so its compare-and-record state stays current.
        dirty |= renderSyncDirty();
        if (dirty) {
            this.syncedActive = this.active;
            this.syncedHeatBucket = bucket;
            this.syncedPulse = this.clientPulse;
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }
    }

    private int heatBucket() {
        int capacity = NeroTechConfig.heatCapacity();
        return capacity <= 0 ? 0 : Math.min(HEAT_SYNC_BUCKETS - 1, this.heat * HEAT_SYNC_BUCKETS / capacity);
    }

    /** Whether the machine is visibly working (BER read surface; fed by the update tag client-side). */
    public boolean renderActive() {
        return this.active;
    }

    /** The synced pulse counter (BER read surface; fed by the update tag client-side). */
    public int renderPulse() {
        return this.clientPulse;
    }

    /** Heat as a 0..1 fraction of capacity — the BER heat-glow lerp input. */
    public float heatFraction() {
        int capacity = NeroTechConfig.heatCapacity();
        return capacity <= 0 ? 0.0F : Math.min(1.0F, (float) this.heat / capacity);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        // The full custom save: active + heat for every BER, the machine items (the Auto Crafter
        // hologram reads its grid), and Core's packed side config (super.saveAdditional writes it —
        // the Item Sorter's port-cap tints read it back on the client).
        return saveCustomOnly(registries);
    }

    // --- Stage H overclock preset (scaled ONCE, here at the base) --------------------------------

    /** The active overclock preset (server-authoritative; clients read ContainerData index 6). */
    public MachinePreset preset() {
        return this.preset;
    }

    /**
     * Apply a preset (server side, from the validated {@code MachinePresetPayload} intent). On a
     * real change it marks the BE dirty and pushes a BE update packet — the same render-sync path
     * {@link #syncRenderState} uses — so watching clients pick the new preset up immediately.
     * Player-driven and rare, so the eager packet is fine.
     */
    public void setPreset(MachinePreset newPreset) {
        if (newPreset == null || newPreset == this.preset) {
            return;
        }
        this.preset = newPreset;
        setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            BlockState state = getBlockState();
            this.level.sendBlockUpdated(this.worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    /** Preset work-rate multiplier — apply at the machine's work site next to Speed modules. */
    public double presetSpeedFactor() {
        return this.preset.speedFactor();
    }

    /** Preset energy-cost multiplier — apply at the machine's work site next to Efficiency modules. */
    public double presetEnergyFactor() {
        return this.preset.energyFactor();
    }

    /**
     * Add heat, clamped to capacity. The Stage H preset scales the amount here at the base (Eco
     * halves it, Overdrive doubles it — the Overdrive BER glow), with a floor of 1 so a working
     * machine on Eco never becomes heat-free.
     */
    protected void addHeat(int amount) {
        if (amount > 0) {
            int scaled = Math.max(1, amount * this.preset.heatPermille() / 1000);
            this.heat = Math.min(NeroTechConfig.heatCapacity(), this.heat + scaled);
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
        // Stage H preset scaling, once at the base (floor 1: a polluting machine on Eco still
        // pollutes; a config of 0 above stays fully disabled).
        amount = Math.max(1, amount * this.preset.pollutionPermille() / 1000);
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
        output.putInt("Preset", this.preset.ordinal());
        output.putBoolean("Active", this.active);
        output.putInt("Pulse", this.clientPulse);
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
        this.preset = MachinePreset.byOrdinal(input.getIntOr("Preset", MachinePreset.BALANCED.ordinal()));
        this.active = input.getBooleanOr("Active", false);
        this.clientPulse = input.getIntOr("Pulse", 0);
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
