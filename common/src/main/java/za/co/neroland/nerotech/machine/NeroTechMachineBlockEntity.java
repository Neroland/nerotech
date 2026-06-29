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

import za.co.neroland.nerotech.config.NeroTechConfig;
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

    /** Heat (Stage 3 consequence axis): accumulates while working, sheds passively + via cooling. */
    protected int heat;

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

    // --- WorldlyContainer: the sided item handoff surface for pipes / NeroLogistics --------------
    // Inputs are insertable, outputs extractable; Core upgrade slots are never exposed to automation.

    @Override
    public int[] getSlotsForFace(net.minecraft.core.Direction side) {
        return this.machineFaceSlots;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable net.minecraft.core.Direction side) {
        return slot < this.machineSlots && canPlaceMachineItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, net.minecraft.core.Direction side) {
        return slot < this.machineSlots && canTakeMachineItem(slot);
    }

    // --- heat + pollution (Stage 3 consequence systems) ---------------------

    @Override
    protected final void serverTick(Level level, BlockPos pos, BlockState state) {
        tickMachine(level, pos, state);
        dissipateHeat(level, pos);
    }

    /** Per-machine server logic. The base shed of heat runs automatically after this each tick. */
    protected abstract void tickMachine(Level level, BlockPos pos, BlockState state);

    /** Add heat, clamped to capacity. */
    protected void addHeat(int amount) {
        if (amount > 0) {
            this.heat = Math.min(NeroTechConfig.heatCapacity(), this.heat + amount);
            setChanged();
        }
    }

    /** Shed heat passively each tick, faster when adjacent to water/ice/snow (cooling). */
    protected void dissipateHeat(Level level, BlockPos pos) {
        if (this.heat <= 0) {
            return;
        }
        this.heat = Math.max(0, this.heat - NeroTechConfig.heatDissipationPerTick() - coolingBonus(level, pos));
        setChanged();
    }

    private int coolingBonus(Level level, BlockPos pos) {
        int perCoolant = NeroTechConfig.heatDissipationPerTick();
        int bonus = 0;
        for (Direction side : Direction.values()) {
            BlockState ns = level.getBlockState(pos.relative(side));
            if (ns.is(Blocks.WATER) || ns.is(Blocks.ICE) || ns.is(Blocks.PACKED_ICE) || ns.is(Blocks.BLUE_ICE)
                    || ns.is(Blocks.SNOW_BLOCK) || ns.is(Blocks.POWDER_SNOW)) {
                bonus += perCoolant;
            }
        }
        return bonus;
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
