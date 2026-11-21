package za.co.neroland.nerotech.machine.foundry;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.upgrade.UpgradeModifiers;

import za.co.neroland.nerotech.compat.neroeconomy.MachineCharges;
import za.co.neroland.nerotech.config.NeroTechConfig;
import za.co.neroland.nerotech.machine.MachineStatus;
import za.co.neroland.nerotech.machine.NeroTechMachineBlock;
import za.co.neroland.nerotech.machine.ProcessingMachineBlockEntity;
import za.co.neroland.nerotech.machine.ProcessingSpec;
import za.co.neroland.nerotech.machine.multiblock.FoundryShape;
import za.co.neroland.nerotech.machine.multiblock.MultiblockController;
import za.co.neroland.nerotech.machine.multiblock.MultiblockValidator;
import za.co.neroland.nerotech.menu.ProcessingLayout;
import za.co.neroland.nerotech.menu.ProcessingMenu;
import za.co.neroland.nerotech.progression.MilestoneGates;
import za.co.neroland.nerotech.recipe.ProcessMatching;
import za.co.neroland.nerotech.recipe.ProcessRecipe;
import za.co.neroland.nerotech.registry.ModRecipeTypes;

/**
 * Blast Foundry controller: a 3³ or 5³ shell of Foundry Bricks with Foundry Heat Ports in it, running
 * {@code nerotech:foundry_casting} recipes in <b>batches</b> (a 5³ runs {@code foundryLargeBatchMultiplier}
 * times the 3³ batch per cycle, for proportionally more energy per tick).
 *
 * <p><b>Heat.</b> {@link #heatAvailable()} is the furnace heat, and its only source is heat drained out
 * of the structure's heat ports, which Heat Pipes and Heat Exchangers fill. Energy never heats the
 * furnace; a recipe below its {@code min_heat} makes no progress however much power there is. The
 * controller's own machine heat (overheat, throttling, the HOT lamp) is the shared base model and is
 * never counted as furnace heat.
 *
 * <p><b>Batches.</b> Inputs stay in their slots until a batch completes ({@link FoundryBatch}); the
 * shell breaking, the recipe changing or the inputs being taken mid-batch abandons or shrinks the
 * payout, never duplicates it.
 *
 * <p><b>Structure.</b> Validated by {@link MultiblockController}: at once when a shell block changes,
 * otherwise on its cadence; never a per-tick scan. Persisted: the formed size, the furnace heat and the
 * batch in progress. World data only; credit for the first pour goes to the online operator in the
 * moment and nothing about them is stored.
 */
public class BlastFoundryBlockEntity extends ProcessingMachineBlockEntity {

    public static final ProcessingSpec SPEC = ProcessingSpec.items(4, 2);
    public static final ProcessingLayout LAYOUT = ProcessingLayout.builder(SPEC).build();

    /** Foundry-only detail code (above the base's codes): the shell is not formed. */
    public static final int DETAIL_UNFORMED = 20;
    public static final String DETAIL_UNFORMED_KEY = "nerotech.machine.detail.foundry_unformed";
    /** Below the recipe's furnace heat (parameter: the heat needed); the fix is the Heat Ports. */
    public static final int DETAIL_FOUNDRY_COLD = 21;
    public static final String DETAIL_FOUNDRY_COLD_KEY = "nerotech.machine.detail.foundry_too_cold";
    /** The inputs match a recipe but are short of one whole unit of it. */
    public static final int DETAIL_SHORT_BATCH = 22;
    public static final String DETAIL_SHORT_BATCH_KEY = "nerotech.machine.detail.foundry_short_batch";

    /** Gate credited to the online operator the first time a foundry completes a batch. */
    public static final Identifier FIRST_POUR_GATE = MilestoneGates.FIRST_FOUNDRY_POUR;

    private static final int CHARGE_RETRY_TICKS = 600;

    private final MultiblockController structure = new MultiblockController(FoundryShape.INSTANCE);
    private final FoundryBatch batch = new FoundryBatch();

    /** 0 (unformed), 3 or 5. Persisted. */
    private int formedSize;
    /** Furnace heat, from ports only. Persisted. */
    private int furnaceHeat;
    /** Whether the shell bricks are currently lit (mirrors the world). Persisted. */
    private boolean lit;
    private int unlitTimer;
    /** Port positions of the formed structure (transient; re-derived by the next scan). */
    private List<BlockPos> ports = List.of();
    private boolean validateSoon = true;

    @Nullable
    private RecipeHolder<ProcessRecipe> cachedRecipe;
    @Nullable
    private RecipeHolder<ProcessRecipe> batchRecipe;
    private long inputFingerprint = Long.MIN_VALUE;
    private int fDetail = DETAIL_NONE;
    private int fDetailParam;
    private int fEnergyRate;
    private int paidBatches;
    private long chargeRetryAt;
    @Nullable
    private String chargeRefusalKey;

    private int syncedSize = -1;
    private int syncedUnits = -1;
    private int syncedFurnaceBucket = -1;

    public BlastFoundryBlockEntity(BlockPos pos, BlockState state) {
        super(FoundryContent.BLAST_FOUNDRY_BE.get(), pos, state, SPEC);
    }

    @Override
    protected RecipeType<ProcessRecipe> recipeType() {
        return ModRecipeTypes.FOUNDRY_CASTING.get();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.nerotech.blast_foundry");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new ProcessingMenu(FoundryContent.BLAST_FOUNDRY_MENU, id, inv, this, this.data, LAYOUT);
    }

    // --- read surface (machine contract C1 extras; synced on change) ---------------------------

    /** 0 when unformed, else 3 or 5. */
    public int formedSize() {
        return this.formedSize;
    }

    /** The batch in progress as a fraction of the formed shell's maximum batch (sight-glass melt level). */
    public float batchFill() {
        return FoundryBatch.fill(this.batch.units(), maxUnits());
    }

    /**
     * Furnace heat (port-delivered, not the controller's own) as a bucket 0..5 of the furnace capacity —
     * the sight-glass and door-glow brightness. Synced on change (the furnace heat rides the update tag).
     */
    public int furnaceHeatBucket() {
        int capacity = FoundrySettings.furnaceCapacity();
        return capacity <= 0 ? 0 : Math.max(0, Math.min(5, (int) ((long) this.furnaceHeat * 6 / capacity)));
    }

    /** The pour pulse: changes once per batch that actually poured (synced; compare, never total). */
    public int pourPulse() {
        return completions();
    }

    /** Units committed to the batch in progress (0 when idle). */
    public int batchUnits() {
        return this.batch.units();
    }

    /** Furnace heat: heat delivered through the ports, never the controller's own. */
    @Override
    protected int heatAvailable() {
        return this.furnaceHeat;
    }

    private int maxUnits() {
        return FoundryBatch.maxUnits(this.formedSize, FoundrySettings.batchUnits(),
                FoundrySettings.largeBatchMultiplier());
    }

    // --- tick ----------------------------------------------------------------------------------------

    @Override
    protected void tickMachine(Level level, BlockPos pos, BlockState state) {
        Direction facing = state.hasProperty(NeroTechMachineBlock.FACING)
                ? state.getValue(NeroTechMachineBlock.FACING) : Direction.NORTH;
        boolean due = this.structure.due(level, pos, facing, this.formedSize > 0);
        if (due || this.validateSoon) {
            this.validateSoon = false;
            revalidate(level, pos, facing);
        }

        heatTick(level, pos);

        if (this.formedSize == 0) {
            abortBatch();
            setFoundryDetail(DETAIL_UNFORMED, 0);
            reportStatus(MachineStatus.UNFORMED, DETAIL_UNFORMED_KEY);
            setActive(false);
            updateLit(level, pos, facing, false);
            return;
        }
        runBatch(level);
        updateLit(level, pos, facing, this.active);
    }

    private void runBatch(Level level) {
        ProcessRecipe recipe = currentRecipe(level);
        if (recipe == null) {
            abortBatch();
            stallFoundry(MachineStatus.STARVED, starvedDetail(), 0);
            return;
        }
        if (this.batch.running() && this.batchRecipe != null && this.batchRecipe != this.cachedRecipe) {
            abortBatch(); // the inputs now make something else: start over, nothing consumed
        }
        if (heatAvailable() < recipe.minHeat()) {
            // No progress below the threshold, whatever the energy; the batch waits, it is not lost.
            stallFoundry(MachineStatus.STARVED, DETAIL_FOUNDRY_COLD, recipe.minHeat());
            return;
        }
        if (!this.batch.running()) {
            // Inputs and outputs are tested apart: inputs short of one unit are STARVED, never "output full".
            int inputUnits = inputMultiples(recipe, maxUnits());
            if (inputUnits <= 0) {
                stallFoundry(MachineStatus.STARVED, DETAIL_SHORT_BATCH, 0);
                return;
            }
            int units = FoundryBatch.unitsFor(maxUnits(), inputUnits, outputMultiples(recipe, maxUnits()));
            if (units <= 0) {
                stallFoundry(MachineStatus.BLOCKED, DETAIL_OUTPUT_FULL, 0);
                return;
            }
            this.batch.start(units);
            this.batchRecipe = this.cachedRecipe;
            this.progress = 0;
            setChanged();
        } else if (this.batchRecipe == null) {
            this.batchRecipe = this.cachedRecipe; // resumed from a save
        }

        int units = this.batch.units();
        UpgradeModifiers mods = modifiers();
        int ticks = Math.max(1, (int) Math.round(Math.max(1, recipe.time())
                / Math.max(0.01D, mods.speedMultiplier() * presetSpeedFactor())));
        long perUnit = Math.round(recipe.energy() * mods.energyMultiplier() * presetEnergyFactor());
        int cost = (int) Math.min(Integer.MAX_VALUE, FoundryBatch.energyPerTick(perUnit, units));
        this.maxProgress = ticks;
        this.fEnergyRate = cost;

        if (overheated()) {
            setFoundryDetail(DETAIL_OVERHEATED, 0);
            reportStatus(MachineStatus.THROTTLED, DETAIL_KEYS[DETAIL_OVERHEATED]);
            setActive(false);
            return;
        }
        if (this.batch.progress() == 0 && energyBuffer().has(cost) && !ensurePaid(level)) {
            setFoundryDetail(DETAIL_UNPAID, 0);
            reportStatus(MachineStatus.UNPAID, this.chargeRefusalKey);
            setActive(false);
            return;
        }
        if (!energyBuffer().has(cost)) {
            setFoundryDetail(DETAIL_NO_ENERGY, 0);
            reportStatus(MachineStatus.NO_ENERGY, DETAIL_KEYS[DETAIL_NO_ENERGY]);
            setActive(false);
            return;
        }
        setFoundryDetail(DETAIL_NONE, 0);
        setActive(true);
        energyBuffer().consume(cost);
        boolean done = this.batch.advance(ticks);
        this.progress = this.batch.progress();
        addHeat(NeroTechConfig.heatPerOperation());
        emitPollution(level, this.worldPosition);
        if (done) {
            int paid = this.batch.settle(inputMultiples(recipe, units), outputMultiples(recipe, units));
            if (paid > 0) {
                pour(level, recipe, paid);
                // The synced pour pulse: only a batch that really poured counts (never an aborted one).
                markCompleted();
            }
            this.batchRecipe = null;
            this.progress = 0;
            if (this.paidBatches > 0) {
                this.paidBatches--;
            }
            pulseClient();
        }
        setChanged();
    }

    private void stallFoundry(MachineStatus status, int detailCode, int param) {
        setFoundryDetail(detailCode, param);
        String key;
        if (detailCode == DETAIL_FOUNDRY_COLD) {
            key = DETAIL_FOUNDRY_COLD_KEY;
        } else if (detailCode == DETAIL_SHORT_BATCH) {
            key = DETAIL_SHORT_BATCH_KEY;
        } else {
            key = detailCode < 0 || detailCode >= DETAIL_KEYS.length ? null : DETAIL_KEYS[detailCode];
        }
        reportStatus(status, key);
        setActive(false);
        this.fEnergyRate = 0;
        if (!this.batch.running() && (this.progress != 0 || this.maxProgress != 0)) {
            this.progress = 0;
            this.maxProgress = 0;
            setChanged();
        }
    }

    private void abortBatch() {
        if (this.batch.running()) {
            this.batch.abort();
            this.batchRecipe = null;
            this.progress = 0;
            this.maxProgress = 0;
            this.fEnergyRate = 0;
            setChanged();
        }
    }

    private void setFoundryDetail(int code, int param) {
        this.fDetail = code;
        this.fDetailParam = param;
    }

    private int starvedDetail() {
        for (int i = 0; i < SPEC.inputs(); i++) {
            if (!this.items.get(SPEC.inputSlot(i)).isEmpty()) {
                return DETAIL_NO_RECIPE;
            }
        }
        return DETAIL_NO_INPUT;
    }

    // --- structure -----------------------------------------------------------------------------------

    private void revalidate(Level level, BlockPos pos, Direction facing) {
        Optional<MultiblockValidator.Formed> formed = this.structure.validate(level, pos, facing);
        int size = formed.map(MultiblockValidator.Formed::size).orElse(0);
        if (size != this.formedSize) {
            if (this.lit) {
                setShellLit(level, pos, facing, this.formedSize, false); // the old box goes cold
                this.lit = false;
            }
            abortBatch(); // a shell that broke or changed size abandons its batch: inputs stay put
            this.formedSize = size;
            setChanged();
        }
        this.ports = formed.map(MultiblockValidator.Formed::ports).orElse(List.of());
    }

    /** Drain the ports on the intake interval, then cool. Port reads only, never a world scan. */
    private void heatTick(Level level, BlockPos pos) {
        int before = this.furnaceHeat;
        int capacity = FoundrySettings.furnaceCapacity();
        int interval = Math.max(1, FoundrySettings.heatIntakeIntervalTicks());
        if (this.formedSize > 0 && !this.ports.isEmpty()
                && (level.getGameTime() + Math.floorMod(pos.hashCode(), interval)) % interval == 0) {
            for (BlockPos portPos : this.ports) {
                int room = FoundryHeat.intake(this.furnaceHeat, capacity, Integer.MAX_VALUE);
                if (room <= 0) {
                    break;
                }
                // A port across a chunk border may sit in a chunk unloaded since the last validation.
                if (level.isLoaded(portPos)
                        && level.getBlockEntity(portPos) instanceof FoundryHeatPortBlockEntity port) {
                    this.furnaceHeat += port.drainHeat(room);
                }
            }
        }
        this.furnaceHeat = FoundryHeat.cool(this.furnaceHeat, ambient(level, pos),
                FoundrySettings.heatLossPermille(), FoundrySettings.heatDrawPerTick(), this.active);
        if (this.furnaceHeat != before) {
            setChanged();
        }
    }

    /** Lit follows formed-and-running, held briefly so a one-tick stall does not flicker the shell. */
    private void updateLit(Level level, BlockPos pos, Direction facing, boolean working) {
        boolean target = this.lit;
        if (working && this.formedSize > 0) {
            target = true;
            this.unlitTimer = 0;
        } else if (this.lit) {
            this.unlitTimer++;
            if (this.formedSize == 0 || this.unlitTimer >= FoundrySettings.litHoldTicks()) {
                target = false;
            }
        }
        if (target != this.lit) {
            if (this.formedSize > 0) {
                setShellLit(level, pos, facing, this.formedSize, target);
            }
            this.lit = target;
            this.unlitTimer = 0;
            setChanged();
        }
    }

    /** Set {@code lit} on every Foundry Brick in the box (once per change; clients only, no neighbour updates). */
    private static void setShellLit(Level level, BlockPos pos, Direction facing, int size, boolean value) {
        if (size <= 0) {
            return;
        }
        BlockPos min = FoundryShape.INSTANCE.min(pos, facing, size);
        BlockPos max = FoundryShape.INSTANCE.max(pos, facing, size);
        if (!level.hasChunksAt(min, max)) {
            return;
        }
        for (BlockPos p : BlockPos.betweenClosed(min, max)) {
            BlockState bs = level.getBlockState(p);
            if (bs.getBlock() instanceof FoundryBrickBlock && bs.getValue(FoundryBrickBlock.LIT) != value) {
                level.setBlock(p, bs.setValue(FoundryBrickBlock.LIT, value), Block.UPDATE_CLIENTS);
            }
        }
    }

    /** Removing the controller leaves its bricks cold. */
    //? if >=1.21.5 {
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
    //?} else {
    /*// Before 1.21.5 there is no block-entity removal hook; the block's onRemove calls this.
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
    *///?}
        if (this.level != null && !this.level.isClientSide() && this.lit) {
            Direction facing = state.hasProperty(NeroTechMachineBlock.FACING)
                    ? state.getValue(NeroTechMachineBlock.FACING) : Direction.NORTH;
            setShellLit(this.level, pos, facing, this.formedSize, false);
            this.lit = false;
        }
    }

    @Override
    public void setRemoved() {
        if (this.level != null) {
            this.structure.release(this.level, this.worldPosition);
        }
        super.setRemoved();
    }

    // --- recipe + batch arithmetic -------------------------------------------------------------------

    @Nullable
    private ProcessRecipe currentRecipe(Level level) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return null;
        }
        long fingerprint = fingerprint();
        boolean changed = fingerprint != this.inputFingerprint;
        this.inputFingerprint = fingerprint;
        if (!changed && this.cachedRecipe != null) {
            return this.cachedRecipe.value();
        }
        if (!changed && (level.getGameTime() % 20L) != 0L) {
            return null;
        }
        Optional<RecipeHolder<ProcessRecipe>> match = serverLevel.recipeAccess()
                .getRecipeFor(recipeType(), recipeInput(), serverLevel, this.cachedRecipe);
        this.cachedRecipe = match.orElse(null);
        return this.cachedRecipe == null ? null : this.cachedRecipe.value();
    }

    /** Item identity and presence of the input slots (counts excluded, so topping up never resets a batch). */
    private long fingerprint() {
        long h = 17L;
        for (int i = 0; i < SPEC.inputs(); i++) {
            ItemStack stack = this.items.get(SPEC.inputSlot(i));
            h = h * 31L + (stack.isEmpty() ? 0 : System.identityHashCode(stack.getItem()));
        }
        return h;
    }

    private List<ItemStack> inputStacks() {
        List<ItemStack> stacks = new ArrayList<>(SPEC.inputs());
        for (int i = 0; i < SPEC.inputs(); i++) {
            stacks.add(this.items.get(SPEC.inputSlot(i)));
        }
        return stacks;
    }

    /** How many whole units of {@code recipe} the input slots cover, up to {@code cap}. */
    private int inputMultiples(ProcessRecipe recipe, int cap) {
        List<ItemStack> stacks = inputStacks();
        for (int k = cap; k > 0; k--) {
            List<ProcessMatching.Requirement<ItemStack>> needs = new ArrayList<>(recipe.items().size());
            for (ProcessRecipe.CountedIngredient counted : recipe.items()) {
                needs.add(new ProcessMatching.Requirement<>(counted.ingredient()::test, counted.count() * k));
            }
            if (ProcessMatching.itemsSatisfied(needs, stacks, ItemStack::getCount)) {
                return k;
            }
        }
        return 0;
    }

    /** How many units of every result (full count, chance or not) fit the outputs, up to {@code cap}. */
    private int outputMultiples(ProcessRecipe recipe, int cap) {
        for (int k = cap; k > 0; k--) {
            List<ItemStack> slots = new ArrayList<>(SPEC.outputs());
            for (int i = 0; i < SPEC.outputs(); i++) {
                slots.add(this.items.get(SPEC.outputSlot(i)).copy());
            }
            boolean fits = true;
            for (ProcessRecipe.ChanceResult result : recipe.results()) {
                ItemStack stack = result.create();
                stack.setCount(stack.getCount() * k);
                if (!insert(slots, stack).isEmpty()) {
                    fits = false;
                    break;
                }
            }
            if (fits) {
                return k;
            }
        }
        return 0;
    }

    /** Consume {@code units} units of inputs and place {@code units} units of results (chance per unit). */
    private void pour(Level level, ProcessRecipe recipe, int units) {
        for (ProcessRecipe.CountedIngredient counted : recipe.items()) {
            int need = counted.count() * units;
            for (int i = 0; i < SPEC.inputs() && need > 0; i++) {
                ItemStack stack = this.items.get(SPEC.inputSlot(i));
                if (!stack.isEmpty() && counted.ingredient().test(stack)) {
                    int take = Math.min(need, stack.getCount());
                    stack.shrink(take);
                    need -= take;
                }
            }
        }
        List<ItemStack> live = new ArrayList<>(SPEC.outputs());
        for (int i = 0; i < SPEC.outputs(); i++) {
            live.add(this.items.get(SPEC.outputSlot(i)));
        }
        for (ProcessRecipe.ChanceResult result : recipe.results()) {
            int made = 0;
            for (int u = 0; u < units; u++) {
                if (result.guaranteed() || level.getRandom().nextInt(1000) < result.chancePermille()) {
                    made++;
                }
            }
            if (made > 0) {
                ItemStack stack = result.create();
                stack.setCount(stack.getCount() * made);
                insert(live, stack);
            }
        }
        for (int i = 0; i < live.size(); i++) {
            this.items.set(SPEC.outputSlot(i), live.get(i));
        }
        creditFirstPour(level);
    }

    /** Merge {@code stack} into {@code slots} (same item first, then empty ones), never past a stack's max. */
    private static ItemStack insert(List<ItemStack> slots, ItemStack stack) {
        for (int pass = 0; pass < 2 && !stack.isEmpty(); pass++) {
            for (int i = 0; i < slots.size() && !stack.isEmpty(); i++) {
                ItemStack slot = slots.get(i);
                if (pass == 0 && !slot.isEmpty() && ItemStack.isSameItemSameComponents(slot, stack)) {
                    int move = Math.min(stack.getCount(), slot.getMaxStackSize() - slot.getCount());
                    if (move > 0) {
                        slot.grow(move);
                        stack.shrink(move);
                    }
                } else if (pass == 1 && slot.isEmpty()) {
                    int move = Math.min(stack.getCount(), stack.getMaxStackSize());
                    ItemStack placed = stack.copy();
                    placed.setCount(move);
                    slots.set(i, placed);
                    stack.shrink(move);
                }
            }
        }
        return stack;
    }

    /** The first pour is a milestone: credit the online operator in the moment (never stored). */
    private void creditFirstPour(Level level) {
        if (!(level instanceof ServerLevel serverLevel) || serverLevel.getServer() == null) {
            return;
        }
        MilestoneGates.credit(serverLevel, lastOperator().orElse(null), FIRST_POUR_GATE);
    }

    /** The optional per-operation charge, settled per batch exactly as the base settles it per operation. */
    private boolean ensurePaid(Level level) {
        int price = NeroTechConfig.machineOperationChargeCost();
        UUID owner = this.ownerId;
        if (price <= 0 || owner == null || this.paidBatches > 0 || !MachineCharges.active()
                || !(level instanceof ServerLevel serverLevel)) {
            this.chargeRefusalKey = null;
            return true;
        }
        long now = level.getGameTime();
        if (this.chargeRefusalKey != null && now < this.chargeRetryAt) {
            return false;
        }
        MachineCharges.Outcome outcome = MachineCharges.charge(serverLevel.getServer(), owner, price);
        if (outcome.ok()) {
            this.paidBatches = NeroTechConfig.machineOperationChargeBatch();
            this.chargeRefusalKey = null;
            setChanged();
            return true;
        }
        String key = outcome.refusalKey();
        this.chargeRefusalKey = key != null ? key : MachineCharges.GENERIC_REFUSAL_KEY;
        this.chargeRetryAt = now + CHARGE_RETRY_TICKS;
        return false;
    }

    // --- GUI + client sync ---------------------------------------------------------------------------

    @Override
    protected int extraData(int index) {
        return switch (index) {
            case X_DETAIL -> this.fDetail;
            case X_DETAIL_PARAM -> Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, this.fDetailParam));
            case X_ENERGY_RATE -> Math.max(0, Math.min(Short.MAX_VALUE, this.fEnergyRate));
            default -> super.extraData(index);
        };
    }

    @Override
    protected boolean renderSyncDirty() {
        int furnaceBucket = furnaceHeatBucket();
        boolean dirty = this.formedSize != this.syncedSize || this.batch.units() != this.syncedUnits
                || furnaceBucket != this.syncedFurnaceBucket;
        this.syncedSize = this.formedSize;
        this.syncedUnits = this.batch.units();
        this.syncedFurnaceBucket = furnaceBucket;
        return dirty;
    }

    //? if >=1.21.6 {
    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
    //?} else {
    /*@Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ValueOutput output = ValueOutput.of(tag, registries);
    *///?}
        // Rides the update tag too: formedSize()/batchFill() on the client come from these.
        output.putInt("FoundrySize", this.formedSize);
        output.putInt("FurnaceHeat", this.furnaceHeat);
        output.putInt("BatchUnits", this.batch.units());
        output.putInt("BatchProgress", this.batch.progress());
        output.putBoolean("ShellLit", this.lit);
        output.putInt("PaidBatches", this.paidBatches);
    }

    //? if >=1.21.6 {
    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
    //?} else {
    /*@Override
    protected void loadAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ValueInput input = ValueInput.of(tag, registries);
    *///?}
        int size = input.getIntOr("FoundrySize", 0);
        this.formedSize = size == FoundryBatch.SMALL || size == FoundryBatch.LARGE ? size : 0;
        this.furnaceHeat = Math.max(0, Math.min(FoundrySettings.furnaceCapacity(), input.getIntOr("FurnaceHeat", 0)));
        this.batch.restore(Math.min(input.getIntOr("BatchUnits", 0), maxUnits()), input.getIntOr("BatchProgress", 0));
        this.progress = this.batch.progress();
        this.lit = input.getBooleanOr("ShellLit", false);
        this.paidBatches = Math.max(0, input.getIntOr("PaidBatches", 0));
        this.batchRecipe = null;
        this.cachedRecipe = null;
        this.inputFingerprint = Long.MIN_VALUE;
        this.validateSoon = true;
    }
}
