package za.co.neroland.nerotech.machine.components;


import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerotech.NeroTechCommon;
import za.co.neroland.nerotech.machine.ProcessingMachineBlockEntity;
import za.co.neroland.nerotech.machine.ProcessingSpec;
import za.co.neroland.nerotech.menu.ProcessingLayout;
import za.co.neroland.nerotech.menu.ProcessingMenu;
import za.co.neroland.nerotech.progression.MilestoneGates;
import za.co.neroland.nerotech.recipe.ProcessRecipe;
import za.co.neroland.nerotech.recipe.ProcessRecipeInput;
import za.co.neroland.nerotech.registry.ModItems;
import za.co.neroland.nerotech.registry.ModRecipeTypes;

/**
 * Laser Etcher: a wafer plus parts, etched under a focus lens, makes a processor ({@code nerotech:etching}).
 * The lens sits in the catalyst slot and is a tool: it is never consumed, and the recipe names which lens
 * it needs, so the fitted lens sets what can be etched (quartz for logic, void for quantum). Only items
 * tagged {@code nerotech:focus_lenses} fit the slot.
 *
 * <p>Machine contract C3: {@link #lens()} is the fitted lens's item id only, synced to watching clients
 * when it changes (never per tick) so the turret shows the lens colour and the beam takes it.
 *
 * <p>Milestone credit: the first Quantum Processor records {@link #FIRST_QUANTUM_PROCESSOR} for the
 * operator in the moment ({@code lastOperator}, transient, never saved) if they are online. Nothing about
 * the player is stored here.
 */
public class LaserEtcherBlockEntity extends ProcessingMachineBlockEntity {

    /** Personal milestone record: etched a Quantum Processor. */
    public static final Identifier FIRST_QUANTUM_PROCESSOR = MilestoneGates.FIRST_QUANTUM_PROCESSOR;

    /** What fits the lens slot. */
    public static final TagKey<Item> FOCUS_LENSES =
            TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(NeroTechCommon.MOD_ID, "focus_lenses"));

    public static final ProcessingSpec SPEC = ComponentsSpecs.LASER_ETCHER;

    /** 176x186: wafer and two part slots in a row, the lens slot under the wafer, one output. */
    public static final ProcessingLayout LAYOUT = ProcessingLayout.builder(SPEC)
            .height(ProcessingLayout.TALL_HEIGHT)
            .catalyst(26, 52)
            .catalystHint(() -> new ItemStack(ModItems.QUARTZ_FOCUS_LENS.get()), "nerotech.gui.slot.lens")
            .output(0, 116, 34)
            .build();

    @Nullable
    private Item syncedLens;
    /** The inputs the lens check last ran for, and its answer (transient cache). */
    private int lensCheckKey = Integer.MIN_VALUE;
    private boolean lensCheckWrong;

    public LaserEtcherBlockEntity(BlockPos pos, BlockState state) {
        super(ComponentsContent.LASER_ETCHER_BE.get(), pos, state, SPEC);
    }

    @Override
    protected RecipeType<ProcessRecipe> recipeType() {
        return ModRecipeTypes.ETCHING.get();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.nerotech.laser_etcher");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new ProcessingMenu(ComponentsContent.LASER_ETCHER_MENU, id, inv, this, this.data, LAYOUT);
    }

    @Override
    protected boolean isValidCatalyst(ItemStack stack) {
        return stack.is(FOCUS_LENSES);
    }

    /** The fitted lens's item id, or null with the slot empty (contract C3; client side: the last sync). */
    @Nullable
    public Identifier lens() {
        ItemStack stack = this.items.get(SPEC.catalystSlot());
        return stack.isEmpty() ? null : BuiltInRegistries.ITEM.getKey(stack.getItem());
    }

    @Override
    protected boolean renderSyncDirty() {
        ItemStack stack = this.items.get(SPEC.catalystSlot());
        Item now = stack.isEmpty() ? null : stack.getItem();
        if (now != this.syncedLens) {
            this.syncedLens = now;
            return true;
        }
        return false;
    }

    /**
     * "Wrong or missing lens": no lens fitted, or another focus lens would etch these inputs. Re-checked
     * only when the inputs or the lens change (at most four recipe lookups then).
     */
    @Override
    protected int refineStarvedDetail(Level level, int detailCode) {
        if (detailCode == DETAIL_NEEDS_CATALYST) {
            return DETAIL_NEEDS_LENS;
        }
        if (detailCode != DETAIL_NO_RECIPE || !(level instanceof ServerLevel serverLevel)) {
            return detailCode;
        }
        ProcessRecipeInput input = recipeInput();
        int key = 17;
        for (ItemStack stack : input.items()) {
            key = key * 31 + (stack.isEmpty() ? 0 : System.identityHashCode(stack.getItem()));
            key = key * 31 + stack.getCount();
        }
        key = key * 31 + (input.catalyst().isEmpty() ? 0 : System.identityHashCode(input.catalyst().getItem()));
        key = key * 31 + System.identityHashCode(input.fluid()) + (int) input.fluidAmount();
        if (key != this.lensCheckKey) {
            this.lensCheckKey = key;
            this.lensCheckWrong = false;
            Item fitted = input.catalyst().getItem();
            for (Item lens : new Item[] {ModItems.QUARTZ_FOCUS_LENS.get(), ModItems.GLACITE_FOCUS_LENS.get(),
                    ModItems.EMBER_FOCUS_LENS.get(), ModItems.VOID_FOCUS_LENS.get()}) {
                if (lens == fitted) {
                    continue;
                }
                ProcessRecipeInput swapped = new ProcessRecipeInput(input.items(), new ItemStack(lens),
                        input.fluid(), input.fluidAmount(), input.gas(), input.gasAmount(), input.heat(),
                        input.atmosphere());
                if (serverLevel.recipeAccess().getRecipeFor(recipeType(), swapped, serverLevel).isPresent()) {
                    this.lensCheckWrong = true;
                    break;
                }
            }
        }
        return this.lensCheckWrong ? DETAIL_NEEDS_LENS : detailCode;
    }

    @Override
    protected void tickMachine(Level level, BlockPos pos, BlockState state) {
        int before = quantumInOutput();
        super.tickMachine(level, pos, state);
        if (quantumInOutput() > before && level instanceof ServerLevel serverLevel) {
            creditQuantum(serverLevel);
        }
    }

    private int quantumInOutput() {
        ItemStack out = this.items.get(SPEC.outputSlot(0));
        return out.is(ModItems.QUANTUM_PROCESSOR.get()) ? out.getCount() : 0;
    }

    /** Record the milestone for the operator in the moment, only while they are online. */
    private void creditQuantum(ServerLevel level) {
        MilestoneGates.credit(level, lastOperator().orElse(null), FIRST_QUANTUM_PROCESSOR);
    }
}
