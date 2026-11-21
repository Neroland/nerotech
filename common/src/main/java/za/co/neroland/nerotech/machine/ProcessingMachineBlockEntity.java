package za.co.neroland.nerotech.machine;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.fluid.GatedFluidView;
import za.co.neroland.nerolandcore.fluid.NeroFluidStorage;
import za.co.neroland.nerolandcore.gas.NeroGasStorage;
import za.co.neroland.nerolandcore.gas.NeroGases;
import za.co.neroland.nerolandcore.sideconfig.Channel;
import za.co.neroland.nerolandcore.sideconfig.SideConfig;
import za.co.neroland.nerolandcore.sideconfig.SideConfigComponent;
import za.co.neroland.nerolandcore.sideconfig.SidePreset;
import za.co.neroland.nerolandcore.sideconfig.SlotGroup;
import za.co.neroland.nerolandcore.upgrade.UpgradeModifiers;

import za.co.neroland.nerotech.compat.neroeconomy.MachineCharges;
import za.co.neroland.nerotech.config.NeroTechConfig;
import za.co.neroland.nerotech.fluid.MachineFluidTank;
import za.co.neroland.nerotech.fluid.NeroTechFluids;
import za.co.neroland.nerotech.gas.MachineGasTank;
import za.co.neroland.nerotech.gas.NeroTechGases;
import za.co.neroland.nerotech.recipe.ProcessMedium;
import za.co.neroland.nerotech.recipe.ProcessRecipe;
import za.co.neroland.nerotech.recipe.ProcessRecipeInput;

/**
 * Shared base for the 0.8.0 recipe machines (machine contract, Classes): runs one
 * {@link ProcessRecipe} type over a fixed {@link ProcessingSpec} — up to six item inputs, an optional
 * catalyst (held, never consumed), up to four outputs with chance rolls, and optional fluid/gas input
 * and output tanks. A recipe sets the energy per tick, the time and the minimum heat; Speed and
 * Efficiency modules and the overclock preset scale them exactly as in
 * {@link AbstractProcessingBlockEntity}, and heat, pollution, analytics and the optional per-operation
 * charge work the same way.
 *
 * <p>The matched recipe is cached and only looked up again when the inputs change (a per-tick
 * fingerprint of the input slots, catalyst and input tanks) or, while nothing matches, once a second
 * (datapack reloads). A stalled machine names its cause in a detail key ({@link #DETAIL_KEYS}) that
 * the GUI status line and the analytics tab show.
 *
 * <p>Subclasses supply {@link #recipeType()}, the menu ({@code createMenu}) and the display name, and
 * may override {@link #heatAvailable()} (the Blast Foundry reads its heat ports), {@link #atmosphere()}
 * (the Atmospheric Condenser), the slot filters and {@link #configureSideChannels}. Machine-scoped state
 * only; no player data beyond the base's opt-in owner.
 */
public abstract class ProcessingMachineBlockEntity extends NeroTechMachineBlockEntity {

    // --- status detail (lang keys; the index rides ContainerData) --------------------------------

    public static final int DETAIL_NONE = -1;
    public static final int DETAIL_NO_INPUT = 0;
    public static final int DETAIL_NO_RECIPE = 1;
    public static final int DETAIL_NEEDS_CATALYST = 2;
    public static final int DETAIL_TOO_COLD = 3;
    public static final int DETAIL_OUTPUT_FULL = 4;
    public static final int DETAIL_TANK_FULL = 5;
    public static final int DETAIL_NO_ENERGY = 6;
    public static final int DETAIL_OVERHEATED = 7;
    public static final int DETAIL_UNPAID = 8;
    /** The machine is raising its own heat to the recipe's minimum before (or between) work ticks. */
    public static final int DETAIL_PREHEATING = 9;
    /** Crystal Grower: still growing, slowly, with no power (lamp dark, crystal moving). */
    public static final int DETAIL_GROWING_UNPOWERED = 10;
    /** Laser Etcher: no focus lens fitted, or the fitted one cannot etch these inputs. */
    public static final int DETAIL_NEEDS_LENS = 11;
    /** The output tank holds a different medium than the recipe makes. */
    public static final int DETAIL_TANK_OTHER = 12;
    /** Plate Press: no die in the die slot. */
    public static final int DETAIL_NEEDS_DIE = 13;
    /** Plate Press: the fitted die cannot shape these inputs. */
    public static final int DETAIL_WRONG_DIE = 14;
    /** A machine with no inputs (the Atmospheric Condenser) has nothing it can collect here. */
    public static final int DETAIL_NOTHING_TO_COLLECT = 15;
    /** Centrifuge: no recipe at this cascade stage (parameter: the stage). */
    public static final int DETAIL_WRONG_STAGE = 16;

    /** Lang keys for the detail codes, indexed by code. The GUI formats {@code too_cold} with the minimum heat. */
    public static final String[] DETAIL_KEYS = {
            "nerotech.machine.detail.no_input",
            "nerotech.machine.detail.no_recipe",
            "nerotech.machine.detail.needs_catalyst",
            "nerotech.machine.detail.too_cold",
            "nerotech.machine.detail.output_full",
            "nerotech.machine.detail.tank_full",
            "nerotech.machine.detail.no_energy",
            "nerotech.machine.detail.overheated",
            "nerotech.machine.detail.unpaid",
            "nerotech.machine.detail.preheating",
            "nerotech.machine.detail.growing_unpowered",
            "nerotech.machine.detail.needs_lens",
            "nerotech.machine.detail.tank_other",
            "nerotech.machine.detail.needs_die",
            "nerotech.machine.detail.wrong_die",
            "nerotech.machine.detail.nothing_to_collect",
            "nerotech.machine.detail.wrong_stage"};

    // --- ContainerData extension (indices after the seven shared ones; see MachineMenu) ----------

    /** Extra index: the live {@link MachineStatus} ordinal. */
    public static final int X_STATUS = 0;
    /** Extra index: the detail code ({@link #DETAIL_NONE} when none). */
    public static final int X_DETAIL = 1;
    /** Extra index: the detail parameter (the recipe's minimum heat for {@code too_cold}). */
    public static final int X_DETAIL_PARAM = 2;
    /** Extra index: {@link #heatAvailable()}, clamped to a short. */
    public static final int X_HEAT = 3;
    /** Extra index: the current recipe's NE per tick after modules and preset (0 when idle). */
    public static final int X_ENERGY_RATE = 4;
    /** Extra index of the first tank block; each tank takes {@link #TANK_STRIDE} values. */
    public static final int X_TANKS = 5;
    /** Per tank: medium id (fluid raw id or gas index, -1 empty), amount hi, amount lo, capacity hi, capacity lo. */
    public static final int TANK_STRIDE = 5;
    public static final int TANK_FLUID_IN = 0;
    public static final int TANK_FLUID_OUT = 1;
    public static final int TANK_GAS_IN = 2;
    public static final int TANK_GAS_OUT = 3;
    /** Number of extra values this base syncs. */
    public static final int EXTRA_COUNT = X_TANKS + 4 * TANK_STRIDE;
    /** Total ContainerData size a processing menu's client side must allocate. */
    public static final int DATA_COUNT = 7 + EXTRA_COUNT;

    /** Ticks a refused per-operation charge waits before it is tried again (30 s). */
    private static final int CHARGE_RETRY_TICKS = 600;

    protected final ProcessingSpec spec;
    @Nullable
    protected final MachineFluidTank fluidIn;
    @Nullable
    protected final MachineFluidTank fluidOut;
    @Nullable
    protected final MachineGasTank gasIn;
    @Nullable
    protected final MachineGasTank gasOut;
    private final NeroFluidStorage fluidView;
    private final NeroGasStorage gasView;

    @Nullable
    private RecipeHolder<ProcessRecipe> cachedRecipe;
    /** The recipe the current progress was made on ({@link RecipeProgress#NONE} when none); saved. */
    private String progressRecipe = RecipeProgress.NONE;
    private long inputFingerprint = Long.MIN_VALUE;
    private int detail = DETAIL_NONE;
    private int detailParam;
    private int energyRate;
    private int paidOps;
    private long chargeRetryAt;
    @Nullable
    private String chargeRefusalKey;

    protected ProcessingMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, ProcessingSpec spec) {
        super(type, pos, state, spec.slotCount());
        this.spec = spec;
        this.fluidIn = spec.fluidIn() > 0
                ? new MachineFluidTank(spec.fluidIn(), this::acceptsFluidInput, this::setChanged) : null;
        this.fluidOut = spec.fluidOut() > 0 ? new MachineFluidTank(spec.fluidOut(), f -> false, this::setChanged) : null;
        this.gasIn = spec.gasIn() > 0
                ? new MachineGasTank(spec.gasIn(), this::acceptsGasInput, this::setChanged) : null;
        this.gasOut = spec.gasOut() > 0 ? new MachineGasTank(spec.gasOut(), g -> false, this::setChanged) : null;
        this.fluidView = new InOutFluid(this.fluidIn, this.fluidOut);
        this.gasView = new InOutGas(this.gasIn, this.gasOut);

        int[] recipeSlots = java.util.stream.IntStream.range(0, spec.firstOutput()).toArray();
        int[] outputSlots = java.util.stream.IntStream.range(spec.firstOutput(), spec.slotCount()).toArray();
        SideConfig.Builder builder = SideConfig.builder()
                .channel(Channel.ITEM, SlotGroup.of("input", recipeSlots), SlotGroup.of("output", outputSlots))
                .channel(Channel.ENERGY);
        if (spec.hasFluid()) {
            builder.channel(Channel.FLUID).preset(Channel.FLUID, SidePreset.STORAGE);
        }
        if (spec.hasGas()) {
            builder.channel(Channel.GAS).preset(Channel.GAS, SidePreset.STORAGE);
        }
        builder.defaultPreset(SidePreset.PROCESSOR);
        configureSideChannels(builder);
        setupSideConfig(builder.build());
        if (spec.hasFluid()) {
            sideConfig().withFluid(() -> this.fluidView);
        }
        if (spec.hasGas()) {
            sideConfig().withGas(() -> this.gasView);
        }
    }

    /**
     * Hook to adjust the side config before it is built (presets per channel, auto input/eject).
     * Called from the constructor: touch only the builder, never subclass fields.
     */
    protected void configureSideChannels(SideConfig.Builder builder) {
    }

    /** This machine's recipe type (one of {@code ModRecipeTypes.processTypes()}). */
    protected abstract RecipeType<ProcessRecipe> recipeType();

    /**
     * The heat a recipe's {@code min_heat} is checked against. Default: this machine's own heat; the
     * Blast Foundry overrides it with the heat its ports deliver.
     */
    protected int heatAvailable() {
        return this.heat;
    }

    /**
     * Hook for machines that heat themselves (the Plasma Crucible): called when a matched recipe needs
     * {@code heatNeeded} and {@link #heatAvailable()} is below it and the outputs have room. Raise the
     * heat and return the energy spent this tick (&gt; 0); return 0 when this machine cannot heat
     * itself (the default: the machine stalls as too cold), or a negative value when it could but has
     * no energy. Progress is kept while a machine heats itself.
     */
    protected int supplyHeat(int heatNeeded) {
        return 0;
    }

    /** The atmosphere a recipe's {@code atmosphere} field is matched against (Atmospheric Condenser). */
    protected String atmosphere() {
        return "";
    }

    /** Whether the input tank takes {@code fluid} from outside. Default: any fluid. */
    protected boolean acceptsFluidInput(Fluid fluid) {
        return fluid != null && fluid != Fluids.EMPTY;
    }

    /** Whether the input tank takes {@code gas} from outside. Default: any real gas. */
    protected boolean acceptsGasInput(Identifier gas) {
        return gas != null && !NeroGases.EMPTY.equals(gas);
    }

    /** Whether input slot {@code index} (0-based among the inputs) takes {@code stack}. Default: anything. */
    protected boolean isValidInput(int index, ItemStack stack) {
        return true;
    }

    /** Whether the catalyst slot takes {@code stack}. Default: anything. */
    protected boolean isValidCatalyst(ItemStack stack) {
        return true;
    }

    public ProcessingSpec spec() {
        return this.spec;
    }

    @Override
    protected boolean syncsProgress() {
        return true;
    }

    @Override
    public int pollutionPerMinute() {
        return emissionPerMinute();
    }

    // --- tick --------------------------------------------------------------------------------------

    @Override
    protected void tickMachine(Level level, BlockPos pos, BlockState state) {
        ProcessRecipe recipe = currentRecipe(level);
        if (recipe == null) {
            int starved = refineStarvedDetail(level, starvedDetail());
            stall(MachineStatus.STARVED, starved, starvedDetailParam(starved));
            return;
        }
        trackRecipeProgress(this.cachedRecipe);
        int heatNeeded = recipe.minHeat();
        if (heatAvailable() < heatNeeded) {
            int blockedCold = outputBlocked(recipe);
            if (blockedCold != DETAIL_NONE) {
                stall(MachineStatus.BLOCKED, blockedCold, 0);
                return;
            }
            int spent = supplyHeat(heatNeeded);
            if (spent > 0) {
                // Self-heating: the job keeps its progress, nothing advances until the heat is there.
                setDetail(DETAIL_PREHEATING, heatNeeded);
                reportStatus(MachineStatus.RUNNING, DETAIL_KEYS[DETAIL_PREHEATING]);
                setActive(true);
                this.energyRate = spent;
                setChanged();
                return;
            }
            if (spent < 0) {
                setDetail(DETAIL_NO_ENERGY, 0);
                reportStatus(MachineStatus.NO_ENERGY, DETAIL_KEYS[DETAIL_NO_ENERGY]);
                setActive(false);
                this.energyRate = 0;
                return;
            }
            stall(MachineStatus.STARVED, DETAIL_TOO_COLD, heatNeeded);
            return;
        }
        int blocked = outputBlocked(recipe);
        if (blocked != DETAIL_NONE) {
            stall(MachineStatus.BLOCKED, blocked, 0);
            return;
        }

        UpgradeModifiers mods = modifiers();
        int ticks = Math.max(1, (int) Math.round(Math.max(1, recipe.time())
                / Math.max(0.01D, mods.speedMultiplier() * presetSpeedFactor())));
        int cost = (int) Math.max(0, Math.round(recipe.energy() * mods.energyMultiplier() * presetEnergyFactor()));
        this.maxProgress = ticks;
        this.energyRate = cost;

        if (overheated()) {
            setDetail(DETAIL_OVERHEATED, 0);
            reportStatus(MachineStatus.THROTTLED, DETAIL_KEYS[DETAIL_OVERHEATED]);
            setActive(false);
            return;
        }
        if (this.progress == 0 && energyBuffer().has(cost) && !ensurePaid(level)) {
            setDetail(DETAIL_UNPAID, 0);
            reportStatus(MachineStatus.UNPAID, this.chargeRefusalKey);
            setActive(false);
            return;
        }
        if (!energyBuffer().has(cost)) {
            setDetail(DETAIL_NO_ENERGY, 0);
            reportStatus(MachineStatus.NO_ENERGY, DETAIL_KEYS[DETAIL_NO_ENERGY]);
            setActive(false);
            return;
        }
        setDetail(DETAIL_NONE, 0);
        setActive(true);
        energyBuffer().consume(cost);
        this.progress++;
        addHeat(NeroTechConfig.heatPerOperation());
        emitPollution(level, pos);
        if (this.progress >= ticks) {
            finish(level, recipe);
            markCompleted();
            this.progress = 0;
            if (this.paidOps > 0) {
                this.paidOps--;
            }
        }
        setChanged();
    }

    private void stall(MachineStatus status, int detailCode, int param) {
        setDetail(detailCode, param);
        reportStatus(status, detailCode == DETAIL_NONE ? null : DETAIL_KEYS[detailCode]);
        setActive(false);
        this.energyRate = 0;
        if (this.progress != 0 || this.maxProgress != 0) {
            this.progress = 0;
            this.maxProgress = 0;
            setChanged();
        }
    }

    private void setDetail(int code, int param) {
        this.detail = code;
        this.detailParam = param;
    }

    /** Why nothing matches: no inputs at all, a missing catalyst, or simply no recipe. */
    private int starvedDetail() {
        boolean anyInput = false;
        for (int i = 0; i < this.spec.inputs(); i++) {
            anyInput |= !this.items.get(this.spec.inputSlot(i)).isEmpty();
        }
        anyInput |= this.fluidIn != null && this.fluidIn.getAmount() > 0;
        anyInput |= this.gasIn != null && this.gasIn.getAmount() > 0;
        if (this.spec.inputs() == 0 && this.fluidIn == null && this.gasIn == null) {
            return DETAIL_NOTHING_TO_COLLECT; // nothing a player could add: "add inputs" would mislead
        }
        if (!anyInput) {
            return DETAIL_NO_INPUT;
        }
        if (this.spec.catalyst() && this.items.get(this.spec.catalystSlot()).isEmpty()) {
            return DETAIL_NEEDS_CATALYST;
        }
        return DETAIL_NO_RECIPE;
    }

    /**
     * The recipe for the current inputs: the cached one while the input fingerprint is unchanged and it
     * still matches, otherwise a fresh lookup (at most once a second while nothing matches).
     */
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
        ProcessRecipeInput input = recipeInput();
        Optional<RecipeHolder<ProcessRecipe>> match = serverLevel.recipeAccess()
                .getRecipeFor(recipeType(), input, serverLevel, this.cachedRecipe);
        this.cachedRecipe = match.orElse(null);
        return this.cachedRecipe == null ? null : this.cachedRecipe.value();
    }

    /**
     * The recipe matched for the current inputs (server side; null before a match or after the inputs
     * changed). For render-sync extras such as the Component Forge's result hologram; additive.
     */
    /**
     * Subclass hook: a more precise cause for "nothing matches" (the Laser Etcher names a wrong or
     * missing lens). Called each starved tick: cache anything costly by input. Default: unchanged.
     */
    protected int refineStarvedDetail(Level level, int detailCode) {
        return detailCode;
    }

    /** Subclass hook: the parameter a starved detail is formatted with (the Centrifuge's stage). Default 0. */
    protected int starvedDetailParam(int detailCode) {
        return 0;
    }

    @Nullable
    protected ProcessRecipe matchedRecipe() {
        return this.cachedRecipe == null ? null : this.cachedRecipe.value();
    }

    /**
     * Progress belongs to one recipe: when the matched recipe is not the one the progress was made on,
     * start again from zero. Returns true when progress was reset (subclasses drop their own carries).
     */
    protected final boolean trackRecipeProgress(@Nullable RecipeHolder<?> holder) {
        String key = holder == null ? RecipeProgress.NONE : String.valueOf(holder.id());
        boolean reset = false;
        if (this.progress > 0 && !RecipeProgress.keepsProgress(this.progressRecipe, key)) {
            this.progress = 0;
            reset = true;
            setChanged();
        }
        this.progressRecipe = key;
        return reset;
    }

    /** Forget the cached recipe (subclasses call this when something {@link #recipeInput()} reads changes). */
    protected void invalidateRecipe() {
        this.cachedRecipe = null;
        this.inputFingerprint = Long.MIN_VALUE;
    }

    /** Cheap identity of everything a recipe matches on except heat (which is checked separately). */
    private long fingerprint() {
        long h = 17L;
        for (int i = 0; i < this.spec.firstOutput(); i++) {
            ItemStack stack = this.items.get(i);
            h = h * 31L + (stack.isEmpty() ? 0 : System.identityHashCode(stack.getItem()));
            h = h * 31L + stack.getCount();
        }
        if (this.fluidIn != null) {
            h = h * 31L + System.identityHashCode(this.fluidIn.getFluid());
            h = h * 31L + this.fluidIn.getAmount();
        }
        if (this.gasIn != null) {
            h = h * 31L + String.valueOf(this.gasIn.getGas()).hashCode();
            h = h * 31L + this.gasIn.getAmount();
        }
        return h * 31L + atmosphere().hashCode();
    }

    /** The recipe input for the current slots and tanks. Heat is maxed: {@code min_heat} is checked apart. */
    protected ProcessRecipeInput recipeInput() {
        List<ItemStack> inputs = new ArrayList<>(this.spec.inputs());
        for (int i = 0; i < this.spec.inputs(); i++) {
            inputs.add(this.items.get(this.spec.inputSlot(i)));
        }
        ItemStack catalyst = this.spec.catalyst() ? this.items.get(this.spec.catalystSlot()) : ItemStack.EMPTY;
        Fluid fluid = this.fluidIn == null ? Fluids.EMPTY : this.fluidIn.getFluid();
        long fluidAmount = this.fluidIn == null ? 0L : this.fluidIn.getAmount();
        Identifier gas = this.gasIn == null ? null : this.gasIn.getGas();
        long gasAmount = this.gasIn == null ? 0L : this.gasIn.getAmount();
        return new ProcessRecipeInput(inputs, catalyst, fluid, fluidAmount, gas, gasAmount, Integer.MAX_VALUE,
                atmosphere());
    }

    /** {@link #DETAIL_NONE} when every output fits, else why not. Chance outputs need room for their full count. */
    private int outputBlocked(ProcessRecipe recipe) {
        List<ItemStack> results = new ArrayList<>(recipe.results().size());
        for (ProcessRecipe.ChanceResult result : recipe.results()) {
            results.add(result.create());
        }
        results.addAll(extraOutputs(recipe));
        if (!fitsAll(results)) {
            return DETAIL_OUTPUT_FULL;
        }
        if (recipe.fluidOut().isPresent()) {
            ProcessMedium out = recipe.fluidOut().get();
            Fluid fluid = outputFluid(out);
            if (this.fluidOut == null || fluid == Fluids.EMPTY) {
                return DETAIL_TANK_FULL;
            }
            if (this.fluidOut.getAmount() > 0 && this.fluidOut.getFluid() != fluid) {
                return DETAIL_TANK_OTHER;
            }
            if (!this.fluidOut.hasRoomFor(out.amount())) {
                return DETAIL_TANK_FULL;
            }
        }
        if (recipe.gasOut().isPresent()) {
            ProcessMedium out = recipe.gasOut().get();
            Identifier gas = Identifier.tryParse(out.key());
            if (this.gasOut == null || gas == null) {
                return DETAIL_TANK_FULL;
            }
            if (this.gasOut.getAmount() > 0 && !NeroTechGases.canonical(gas).equals(this.gasOut.getGas())) {
                return DETAIL_TANK_OTHER;
            }
            if (!this.gasOut.hasRoomFor(NeroTechGases.units(out.amount()))) {
                return DETAIL_TANK_FULL;
            }
        }
        return DETAIL_NONE;
    }

    /**
     * Items an operation leaves in the output slots besides the recipe's results (the Reaction Vat's
     * returned buckets), counted in the room check so they never have to go anywhere else. Default none.
     */
    protected List<ItemStack> extraOutputs(ProcessRecipe recipe) {
        return List.of();
    }

    private static Fluid outputFluid(ProcessMedium medium) {
        if (medium.isTag()) {
            return Fluids.EMPTY; // an output must name one fluid
        }
        Identifier id = Identifier.tryParse(medium.id());
        return id == null ? Fluids.EMPTY : BuiltInRegistries.FLUID.getValue(id);
    }

    /** Whether every stack fits into the output slots together (simulated on copies). */
    private boolean fitsAll(List<ItemStack> stacks) {
        List<ItemStack> slots = new ArrayList<>(this.spec.outputs());
        for (int i = 0; i < this.spec.outputs(); i++) {
            slots.add(this.items.get(this.spec.outputSlot(i)).copy());
        }
        for (ItemStack stack : stacks) {
            if (!insert(slots, stack.copy(), false).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /** Merge {@code stack} into {@code slots} (same item first, then empty ones); returns what is left. */
    private static ItemStack insert(List<ItemStack> slots, ItemStack stack, boolean intoLive) {
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
                    slots.set(i, stack.copy());
                    stack.setCount(0);
                }
            }
        }
        return stack;
    }

    /** Complete one operation: consume inputs and media, roll and place the outputs. */
    private void finish(Level level, ProcessRecipe recipe) {
        for (ProcessRecipe.CountedIngredient counted : recipe.items()) {
            int need = counted.count();
            for (int i = 0; i < this.spec.inputs() && need > 0; i++) {
                ItemStack stack = this.items.get(this.spec.inputSlot(i));
                if (!stack.isEmpty() && counted.ingredient().test(stack)) {
                    int take = Math.min(need, stack.getCount());
                    stack.shrink(take);
                    need -= take;
                }
            }
        }
        recipe.fluidIn().ifPresent(m -> {
            if (this.fluidIn != null) {
                this.fluidIn.drain(m.amount(), false);
            }
        });
        recipe.gasIn().ifPresent(m -> {
            if (this.gasIn != null) {
                this.gasIn.drain(NeroTechGases.units(m.amount()), false);
            }
        });
        List<ItemStack> live = new ArrayList<>(this.spec.outputs());
        for (int i = 0; i < this.spec.outputs(); i++) {
            live.add(this.items.get(this.spec.outputSlot(i)));
        }
        for (ProcessRecipe.ChanceResult result : recipe.results()) {
            if (result.guaranteed() || level.getRandom().nextInt(1000) < result.chancePermille()) {
                insert(live, result.create(), true);
            }
        }
        for (int i = 0; i < live.size(); i++) {
            this.items.set(this.spec.outputSlot(i), live.get(i));
        }
        recipe.fluidOut().ifPresent(m -> {
            Fluid fluid = outputFluid(m);
            if (this.fluidOut != null && fluid != Fluids.EMPTY) {
                this.fluidOut.forceFill(fluid, m.amount());
            }
        });
        recipe.gasOut().ifPresent(m -> {
            Identifier gas = Identifier.tryParse(m.key());
            if (this.gasOut != null && gas != null) {
                this.gasOut.produce(gas, NeroTechGases.units(m.amount()));
            }
        });
    }

    /** The optional per-operation charge, exactly as {@link AbstractProcessingBlockEntity} settles it. */
    private boolean ensurePaid(Level level) {
        int price = NeroTechConfig.machineOperationChargeCost();
        UUID owner = this.ownerId;
        if (price <= 0 || owner == null || this.paidOps > 0 || !MachineCharges.active()
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
            this.paidOps = NeroTechConfig.machineOperationChargeBatch();
            this.chargeRefusalKey = null;
            setChanged();
            return true;
        }
        String key = outcome.refusalKey();
        this.chargeRefusalKey = key != null ? key : MachineCharges.GENERIC_REFUSAL_KEY;
        this.chargeRetryAt = now + CHARGE_RETRY_TICKS;
        return false;
    }

    // --- adjacent fluid and gas exchange (no pipes) -----------------------------------------------------

    /**
     * Every {@link MediumTransfer#INTERVAL_TICKS} ticks: push the output tanks out of every face whose
     * FLUID or GAS mode lets medium out, and pull the input tanks in through every face that takes input
     * automatically (INPUT, I/O), drawing only a medium one of this machine's recipes uses here. Pushing
     * skips neighbouring recipe machines: they pull for themselves, so nothing is forced into an input a
     * recipe cannot read. The player's Side Config faces decide everything; DISABLED moves nothing.
     */
    @Override
    protected void exchangeMedia(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel) || !MediumTransfer.due(level.getGameTime(), exchangePhase())) {
            return;
        }
        SideConfigComponent config = sideConfig();
        MachineFluidTank fOut = this.fluidOut;
        if (fOut != null && fOut.getAmount() > 0) {
            MediumExchange.pushFluid(level, pos, config, fOut, (fluid, amount) -> fOut.forceFill(fluid, amount),
                    MediumTransfer::machinePushes, this::skipsPushTo);
        }
        MachineGasTank gOut = this.gasOut;
        if (gOut != null && gOut.getAmount() > 0) {
            MediumExchange.pushGas(level, pos, config, gOut, (gas, amount) -> gOut.produce(gas, amount),
                    MediumTransfer::machinePushes, this::skipsPushTo);
        }
        MachineFluidTank fIn = this.fluidIn;
        if (fIn != null && fIn.getAmount() < fIn.getCapacity()) {
            MediumExchange.pullFluid(level, pos, config, fIn, MediumTransfer::machinePulls,
                    fluid -> wantsFluid(serverLevel, fIn, fluid), this::skipsPullFrom);
        }
        MachineGasTank gIn = this.gasIn;
        if (gIn != null && gIn.getAmount() < gIn.getCapacity()) {
            MediumExchange.pullGas(level, pos, config, gIn, MediumTransfer::machinePulls,
                    gas -> wantsGas(serverLevel, gIn, gas), this::skipsPullFrom);
        }
    }

    /** Neighbours this machine does not push into: other recipe machines, which pull what they can use. */
    protected boolean skipsPushTo(@Nullable net.minecraft.world.level.block.entity.BlockEntity neighbour) {
        return neighbour instanceof ProcessingMachineBlockEntity;
    }

    /** Neighbours this machine does not pull from. Default none; the Centrifuge skips its cascade. */
    protected boolean skipsPullFrom(@Nullable net.minecraft.world.level.block.entity.BlockEntity neighbour) {
        return false;
    }

    private boolean wantsFluid(ServerLevel level, MachineFluidTank tank, Fluid fluid) {
        if (tank.getAmount() > 0) {
            return MachineFluidTank.normalize(fluid) == tank.getFluid();
        }
        return acceptsFluidInput(fluid) && RecipeMedia.usesFluid(level, recipeType(), atmosphere(), fluid);
    }

    private boolean wantsGas(ServerLevel level, MachineGasTank tank, Identifier gas) {
        Identifier canonical = NeroTechGases.canonical(gas);
        if (tank.getAmount() > 0) {
            return canonical != null && canonical.equals(NeroTechGases.canonical(tank.getGas()));
        }
        return acceptsGasInput(canonical) && RecipeMedia.usesGas(level, recipeType(), atmosphere(), canonical);
    }

    // --- slots ---------------------------------------------------------------------------------------

    @Override
    public boolean canPlaceMachineItem(int slot, ItemStack stack) {
        if (slot < this.spec.inputs()) {
            return isValidInput(slot, stack);
        }
        if (slot == this.spec.catalystSlot()) {
            return isValidCatalyst(stack);
        }
        return false;
    }

    @Override
    public boolean canTakeMachineItem(int slot) {
        return this.spec.isOutput(slot);
    }

    // --- surfaces ------------------------------------------------------------------------------------

    @Nullable
    @Override
    public NeroFluidStorage fluidStorage(@Nullable Direction side) {
        if (!this.spec.hasFluid()) {
            return null;
        }
        SideConfigComponent config = sideConfig();
        return config == null ? this.fluidView : config.fluidView(side);
    }

    @Nullable
    @Override
    public NeroGasStorage gasStorage(@Nullable Direction side) {
        if (!this.spec.hasGas()) {
            return null;
        }
        SideConfigComponent config = sideConfig();
        return config == null ? this.gasView : config.gasView(side);
    }

    @Override
    public List<GatedFluidView> standardFluidViews(@Nullable Direction side) {
        List<GatedFluidView> views = new ArrayList<>(2);
        if (this.spec.hasFluid()) {
            views.add(gatedView(this.fluidView, Channel.FLUID, side));
        }
        if (this.spec.hasGas()) {
            views.add(gatedView(NeroTechFluids.asFluid(this.gasView), Channel.GAS, side));
        }
        return views;
    }

    // --- GUI sync ----------------------------------------------------------------------------------

    @Override
    protected int extraDataCount() {
        return EXTRA_COUNT;
    }

    @Override
    protected int extraData(int index) {
        switch (index) {
            case X_STATUS:
                return stats().status().ordinal();
            case X_DETAIL:
                return this.detail;
            case X_DETAIL_PARAM:
                return clampShort(this.detailParam);
            case X_HEAT:
                return clampShort(heatAvailable());
            case X_ENERGY_RATE:
                return clampShort(this.energyRate);
            default:
                break;
        }
        int t = index - X_TANKS;
        if (t < 0 || t >= 4 * TANK_STRIDE) {
            return 0;
        }
        int tank = t / TANK_STRIDE;
        int field = t % TANK_STRIDE;
        long amount;
        long capacity;
        int id;
        if (tank == TANK_FLUID_IN || tank == TANK_FLUID_OUT) {
            MachineFluidTank fluidTank = tank == TANK_FLUID_IN ? this.fluidIn : this.fluidOut;
            if (fluidTank == null) {
                return field == 0 ? -1 : 0;
            }
            amount = fluidTank.getAmount();
            capacity = fluidTank.getCapacity();
            id = amount <= 0 ? -1 : BuiltInRegistries.FLUID.getId(fluidTank.getFluid());
        } else {
            MachineGasTank gasTank = tank == TANK_GAS_IN ? this.gasIn : this.gasOut;
            if (gasTank == null) {
                return field == 0 ? -1 : 0;
            }
            amount = gasTank.getAmount();
            capacity = gasTank.getCapacity();
            id = amount <= 0 ? -1 : NeroTechGases.ALL.indexOf(NeroTechGases.canonical(gasTank.getGas()));
        }
        return switch (field) {
            case 0 -> id;
            case 1 -> hi(amount);
            case 2 -> lo(amount);
            case 3 -> hi(capacity);
            default -> lo(capacity);
        };
    }

    private static int clampShort(int value) {
        return Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, value));
    }

    /** Upper 15 bits of a non-negative amount (ContainerData values travel as shorts). */
    public static int hi(long value) {
        return (int) Math.min(Short.MAX_VALUE, Math.max(0L, value) >> 15);
    }

    /** Lower 15 bits of a non-negative amount. */
    public static int lo(long value) {
        return (int) (Math.max(0L, value) & 0x7FFF);
    }

    /** Rebuild an amount from {@link #hi} and {@link #lo}. */
    public static long join(int hi, int lo) {
        return ((long) Math.max(0, hi) << 15) | (lo & 0x7FFF);
    }

    // --- persistence ---------------------------------------------------------------------------------

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
        output.putInt("PaidOps", this.paidOps);
        if (this.progress > 0 && !this.progressRecipe.isEmpty()) {
            output.putString("ProgressRecipe", this.progressRecipe);
        }
        if (this.fluidIn != null) {
            this.fluidIn.save(output, "FluidIn");
        }
        if (this.fluidOut != null) {
            this.fluidOut.save(output, "FluidOut");
        }
        if (this.gasIn != null) {
            this.gasIn.save(output, "GasIn");
        }
        if (this.gasOut != null) {
            this.gasOut.save(output, "GasOut");
        }
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
        this.paidOps = Math.max(0, input.getIntOr("PaidOps", 0));
        this.progressRecipe = input.getStringOr("ProgressRecipe", RecipeProgress.NONE);
        if (this.fluidIn != null) {
            this.fluidIn.load(input, "FluidIn");
        }
        if (this.fluidOut != null) {
            this.fluidOut.load(input, "FluidOut");
        }
        if (this.gasIn != null) {
            this.gasIn.load(input, "GasIn");
        }
        if (this.gasOut != null) {
            this.gasOut.load(input, "GasOut");
        }
        invalidateRecipe();
    }

    // --- combined tank views: fill goes to the input tank, drain comes from the output tank --------

    /** One fluid surface over an input and an output tank (Core's side config carries one per channel). */
    private record InOutFluid(@Nullable MachineFluidTank in, @Nullable MachineFluidTank out)
            implements NeroFluidStorage {
        @Override
        public Fluid getFluid() {
            if (this.out != null && this.out.getAmount() > 0) {
                return this.out.getFluid();
            }
            return this.in == null ? Fluids.EMPTY : this.in.getFluid();
        }

        @Override
        public long getAmount() {
            if (this.out != null && this.out.getAmount() > 0) {
                return this.out.getAmount();
            }
            return this.in == null ? 0L : this.in.getAmount();
        }

        @Override
        public long getCapacity() {
            if (this.out != null && (this.out.getAmount() > 0 || this.in == null)) {
                return this.out.getCapacity();
            }
            return this.in == null ? 0L : this.in.getCapacity();
        }

        @Override
        public long fill(Fluid fluid, long amount, boolean simulate) {
            return this.in == null ? 0L : this.in.fill(fluid, amount, simulate);
        }

        @Override
        public long drain(long amount, boolean simulate) {
            return this.out == null ? 0L : this.out.drain(amount, simulate);
        }
    }

    /** The gas twin of {@link InOutFluid}. */
    private record InOutGas(@Nullable MachineGasTank in, @Nullable MachineGasTank out) implements NeroGasStorage {
        @Override
        public Identifier getGas() {
            if (this.out != null && this.out.getAmount() > 0) {
                return this.out.getGas();
            }
            return this.in == null ? NeroGases.EMPTY : this.in.getGas();
        }

        @Override
        public long getAmount() {
            if (this.out != null && this.out.getAmount() > 0) {
                return this.out.getAmount();
            }
            return this.in == null ? 0L : this.in.getAmount();
        }

        @Override
        public long getCapacity() {
            if (this.out != null && (this.out.getAmount() > 0 || this.in == null)) {
                return this.out.getCapacity();
            }
            return this.in == null ? 0L : this.in.getCapacity();
        }

        @Override
        public long fill(Identifier gas, long amount, boolean simulate) {
            return this.in == null ? 0L : this.in.fill(gas, amount, simulate);
        }

        @Override
        public long drain(long amount, boolean simulate) {
            return this.out == null ? 0L : this.out.drain(amount, simulate);
        }
    }
}
