package za.co.neroland.nerotech.machine;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.sideconfig.Channel;
import za.co.neroland.nerolandcore.sideconfig.SideConfig;
import za.co.neroland.nerolandcore.sideconfig.SidePreset;
import za.co.neroland.nerolandcore.sideconfig.SlotGroup;
import za.co.neroland.nerolandcore.upgrade.UpgradeModifiers;

import za.co.neroland.nerotech.config.NeroTechConfig;
import za.co.neroland.nerotech.recipe.MachineRecipe;

/**
 * Shared base for NeroTech's recipe-driven processing machines (Ore Processor, Fabricator). One input
 * slot, one output slot. Each tick it consumes NE (scaled down by Efficiency modules) to advance a
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

    protected AbstractProcessingBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 2);
        // PROCESSOR preset: ITEM input on every face except BOTTOM (=output), ENERGY input on every face.
        setupSideConfig(SideConfig.builder()
                .channel(Channel.ITEM, SlotGroup.of("input", INPUT_SLOT), SlotGroup.of("output", OUTPUT_SLOT))
                .channel(Channel.ENERGY)
                .defaultPreset(SidePreset.PROCESSOR)
                .build());
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
        return this.lastRecipe.value().assemble(recipeInput);
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
        int effectiveTicks = Math.max(1,
                (int) Math.round(NeroTechConfig.machineBaseProcessTicks() / Math.max(0.01D, mods.speedMultiplier())));
        int cost = (int) Math.max(0, Math.round(NeroTechConfig.machineNePerTick() * mods.energyMultiplier()));
        this.maxProgress = effectiveTicks;

        // Heat throttle: a machine that's run too hard stalls until it sheds heat (base dissipation).
        if (overheated()) {
            reportStatus(MachineStatus.THROTTLED);
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
            }
            setChanged();
        } else {
            // Analytics: work is queued but the buffer can't cover the next tick's cost.
            reportStatus(MachineStatus.NO_ENERGY);
        }
    }

    private boolean canOutput(ItemStack result) {
        ItemStack out = this.items.get(OUTPUT_SLOT);
        if (out.isEmpty()) {
            return true;
        }
        return ItemStack.isSameItemSameComponents(out, result)
                && out.getCount() + result.getCount() <= out.getMaxStackSize();
    }

    private void craft(ItemStack result) {
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
