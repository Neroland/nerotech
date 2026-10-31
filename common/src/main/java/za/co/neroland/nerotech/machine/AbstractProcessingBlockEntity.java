package za.co.neroland.nerotech.machine;

import java.util.Optional;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.sideconfig.Channel;
import za.co.neroland.nerolandcore.sideconfig.SideConfig;
import za.co.neroland.nerolandcore.sideconfig.SidePreset;
import za.co.neroland.nerolandcore.sideconfig.SlotGroup;
import za.co.neroland.nerolandcore.upgrade.UpgradeModifiers;

import za.co.neroland.nerotech.compat.neroeconomy.MachineCharges;
import za.co.neroland.nerotech.config.NeroTechConfig;
import za.co.neroland.nerotech.recipe.MachineRecipe;

/**
 * Shared base for NeroTech's recipe-driven processing machines (Ore Processor, Fabricator, and —
 * with its own tick loop over the same recipe plumbing — the Collider Core). One input slot, one
 * output slot. Each tick it consumes NE (scaled down by Efficiency modules) to advance a
 * progress timer (shortened by Speed modules); on completion it consumes one input and yields the
 * recipe result. Pure NE sink — it receives power pushed by generators through Core's energy seam.
 *
 * <p>Recipes are <b>datapack-driven</b> (Stage C, 2026-07-10): each machine family declares its
 * {@link #recipeType()} and this base resolves results through the level's recipe manager (with a
 * last-recipe hint so steady-state processing is a single {@code matches} test, not a full scan).
 * The in-jar JSON baseline under {@code data/nerotech/recipe/} is the balance surface; world
 * datapacks may override or extend it file-by-file.
 */
public abstract class AbstractProcessingBlockEntity extends NeroTechMachineBlockEntity {

    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;

    /** Last matched recipe — passed back as a hint so repeat lookups skip the type scan. */
    @Nullable
    private RecipeHolder<MachineRecipe> lastRecipe;

    /** Ticks a refused per-operation charge waits before it is tried again (30 s). */
    private static final int CHARGE_RETRY_TICKS = 600;

    /**
     * Operations already paid for by the owner's last {@code machineOperationChargeCost} batch
     * (compat/neroeconomy). Persisted ("PaidOps") so a chunk reload never charges twice for one batch.
     * Machine-scoped count only — no player data.
     */
    private int paidOps;

    /** Game time before which a refused charge is not retried (transient). */
    private long chargeRetryAt;

    /** The economy's lang key for the last refusal, or null when not refused (transient). */
    @Nullable
    private String chargeRefusalKey;

    protected AbstractProcessingBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 2);
        // PROCESSOR preset: ITEM input on every face except BOTTOM (=output), ENERGY input on every face.
        SideConfig.Builder builder = SideConfig.builder()
                .channel(Channel.ITEM, SlotGroup.of("input", INPUT_SLOT), SlotGroup.of("output", OUTPUT_SLOT))
                .channel(Channel.ENERGY)
                .defaultPreset(SidePreset.PROCESSOR);
        configureSideChannels(builder);
        setupSideConfig(builder.build());
    }

    /**
     * Hook for subclasses that expose more than items and power — the Chemical Processor adds its
     * GAS intake here, which is what puts a Gas tab in its side-config screen. Called from this
     * constructor, so an override may only touch the builder, never the subclass's own fields
     * (they are not initialised yet).
     */
    protected void configureSideChannels(SideConfig.Builder builder) {
    }

    /** This machine family's datapack recipe type (see {@code registry.ModRecipeTypes}). */
    protected abstract RecipeType<MachineRecipe> recipeType();

    /**
     * The recipe result for an input stack (single output), or EMPTY if there is no recipe.
     * Server-side this queries the recipe manager; client-side (no recipe manager access for
     * custom types) it returns the input unchanged wrapped as "maybe" — callers on the client
     * only use this for permissive slot-placement prediction, and the server stays authoritative.
     */
    protected ItemStack resultFor(ItemStack input) {
        if (input.isEmpty()) {
            return ItemStack.EMPTY;
        }
        if (!(this.level instanceof ServerLevel serverLevel)) {
            return input; // client-side placement prediction: permissive, server validates.
        }
        SingleRecipeInput recipeInput = new SingleRecipeInput(input);
        Optional<RecipeHolder<MachineRecipe>> match = serverLevel.recipeAccess()
                .getRecipeFor(recipeType(), recipeInput, serverLevel, this.lastRecipe);
        if (match.isEmpty()) {
            return ItemStack.EMPTY;
        }
        this.lastRecipe = match.get();
        //? if >=26.1 {
        return this.lastRecipe.value().assemble(recipeInput);
        //?} else {
        /*return this.lastRecipe.value().assemble(recipeInput, serverLevel.registryAccess());
        *///?}
    }

    /** Processing emits — the analytics panel shows the config-derived nominal rate. */
    @Override
    public int pollutionPerMinute() {
        return emissionPerMinute();
    }

    @Override
    protected void tickMachine(Level level, BlockPos pos, BlockState state) {
        ItemStack input = this.items.get(INPUT_SLOT);
        ItemStack result = resultFor(input);

        if (result.isEmpty() || !canOutput(result)) {
            // Analytics: no usable input (empty slot / no recipe) reads STARVED; output jam BLOCKED.
            reportStatus(result.isEmpty() ? MachineStatus.STARVED : MachineStatus.BLOCKED);
            setActive(false); // nothing to do — BER drums/arms park
            if (this.maxProgress != 0 || this.progress != 0) {
                this.progress = 0;
                this.maxProgress = 0;
                setChanged();
            }
            return;
        }

        UpgradeModifiers mods = modifiers();
        // Stage H preset: speed shortens the duration alongside Speed modules; energy scales the
        // per-tick cost alongside Efficiency modules (heat/pollution scale at the base).
        int effectiveTicks = Math.max(1, (int) Math.round(NeroTechConfig.machineBaseProcessTicks()
                / Math.max(0.01D, mods.speedMultiplier() * presetSpeedFactor())));
        int cost = (int) Math.max(0,
                Math.round(NeroTechConfig.machineNePerTick() * mods.energyMultiplier() * presetEnergyFactor()));
        this.maxProgress = effectiveTicks;

        // Heat throttle: a machine that's run too hard stalls until it sheds heat (base dissipation).
        if (overheated()) {
            reportStatus(MachineStatus.THROTTLED);
            setActive(false);
            return;
        }

        // Optional per-operation charge (compat/neroeconomy): settled at the start of an operation, in
        // batches, and only for an owned machine with the charge configured. Refused = idle.
        if (this.progress == 0 && energyBuffer().has(cost) && !ensurePaid(level)) {
            reportStatus(MachineStatus.UNPAID, this.chargeRefusalKey);
            setActive(false);
            return;
        }

        // BER surface: dynamic geometry runs exactly while progress actually advances.
        setActive(energyBuffer().has(cost));

        if (energyBuffer().has(cost)) {
            energyBuffer().consume(cost);
            this.progress++;
            addHeat(NeroTechConfig.heatPerOperation());
            emitPollution(level, pos);
            if (this.progress >= effectiveTicks) {
                craft(result);
                this.progress = 0;
                if (this.paidOps > 0) {
                    this.paidOps--;
                }
            }
            setChanged();
        } else {
            // Analytics: work is queued but the buffer can't cover the next tick's cost.
            reportStatus(MachineStatus.NO_ENERGY);
        }
    }

    /**
     * Whether the next operation may start as far as the optional owner charge is concerned. Free when
     * the charge is off ({@code machineOperationChargeCost = 0}), NeroEconomy is absent, the machine has
     * no owner, or a paid batch still has operations left. Otherwise one batch is charged; a refusal is
     * retried after {@link #CHARGE_RETRY_TICKS}, so a refused machine never calls the economy per tick.
     */
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
    }

    /** Whether the output slot can take {@code result} (empty, or a mergeable identical stack). */
    protected boolean canOutput(ItemStack result) {
        ItemStack out = this.items.get(OUTPUT_SLOT);
        if (out.isEmpty()) {
            return true;
        }
        return ItemStack.isSameItemSameComponents(out, result)
                && out.getCount() + result.getCount() <= out.getMaxStackSize();
    }

    /** Complete one operation: merge {@code result} into the output slot and eat one input. */
    protected void craft(ItemStack result) {
        ItemStack out = this.items.get(OUTPUT_SLOT);
        if (out.isEmpty()) {
            this.items.set(OUTPUT_SLOT, result.copy());
        } else {
            out.grow(result.getCount());
        }
        this.items.get(INPUT_SLOT).shrink(1);
    }

    @Override
    public boolean canPlaceMachineItem(int slot, ItemStack stack) {
        return slot == INPUT_SLOT && !resultFor(stack).isEmpty();
    }

    @Override
    public boolean canTakeMachineItem(int slot) {
        return slot == OUTPUT_SLOT;
    }
}
