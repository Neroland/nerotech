package za.co.neroland.nerotech.machine.chemistry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import za.co.neroland.nerotech.machine.ProcessingMachineBlockEntity;
import za.co.neroland.nerotech.machine.ProcessingSpec;
import za.co.neroland.nerotech.menu.ProcessingLayout;
import za.co.neroland.nerotech.menu.ProcessingMenu;
import za.co.neroland.nerotech.recipe.ProcessRecipe;
import za.co.neroland.nerotech.registry.ModRecipeTypes;

/**
 * Reaction Vat ({@code nerotech:reacting}): fluid plus fluid, gas or item into fluid — sulfuric acid,
 * polymer resin, cryo coolant. Filled buckets fed as items (Nerospace's brine bucket) come back empty,
 * exactly like a crafting remainder ({@link ContainerRemainders}): into the output slot when it fits,
 * otherwise back into the slot the bucket left, otherwise dropped on top of the vat.
 */
public class ReactionVatBlockEntity extends ProcessingMachineBlockEntity {

    /** 2 items in, 1 output (bucket returns, by-products), a fluid and a gas tank in, a fluid tank out. */
    public static final ProcessingSpec SPEC = new ProcessingSpec(2, false, 1, 8_000L, 8_000L, 4_000L, 0L);
    public static final ProcessingLayout LAYOUT = ProcessingLayout.builder(SPEC)
            .height(ProcessingLayout.TALL_HEIGHT)
            .input(0, 26, 30).input(1, 26, 48)
            .fluidIn(44, 28, 40)
            .gasIn(62, 28, 40)
            .arrow(84, 34, 22, 16)
            .output(0, 110, 30)
            .fluidOut(136, 28, 40)
            .build();

    public ReactionVatBlockEntity(BlockPos pos, BlockState state) {
        super(ChemistryContent.REACTION_VAT_BE.get(), pos, state, SPEC);
    }

    @Override
    protected RecipeType<ProcessRecipe> recipeType() {
        return ModRecipeTypes.REACTING.get();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.nerotech.reaction_vat");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new ProcessingMenu(ChemistryContent.REACTION_VAT_MENU, id, inv, this, this.data, LAYOUT);
    }

    @Override
    protected void tickMachine(Level level, BlockPos pos, BlockState state) {
        int inputs = this.spec.inputs();
        Item[] before = new Item[inputs];
        int[] counts = new int[inputs];
        for (int i = 0; i < inputs; i++) {
            ItemStack stack = this.items.get(this.spec.inputSlot(i));
            before[i] = stack.isEmpty() ? null : stack.getItem();
            counts[i] = stack.getCount();
        }
        super.tickMachine(level, pos, state);
        for (int i = 0; i < inputs; i++) {
            if (before[i] == null) {
                continue;
            }
            ItemStack now = this.items.get(this.spec.inputSlot(i));
            int left = now.isEmpty() || now.getItem() != before[i] ? 0 : now.getCount();
            int used = counts[i] - left;
            if (used > 0 && ContainerRemainders.remainderFor(
                    BuiltInRegistries.ITEM.getKey(before[i]).toString()).isPresent()) {
                returnRemainder(level, pos, i, new ItemStack(Items.BUCKET, used));
            }
        }
    }

    /**
     * The empty buckets an operation hands back, reserved in the output-room check so they always land
     * in the output slot (never back in an input slot, where automation could not take them out).
     */
    @Override
    protected java.util.List<ItemStack> extraOutputs(ProcessRecipe recipe) {
        int buckets = 0;
        for (ProcessRecipe.CountedIngredient counted : recipe.items()) {
            for (int i = 0; i < this.spec.inputs(); i++) {
                ItemStack stack = this.items.get(this.spec.inputSlot(i));
                if (!stack.isEmpty() && counted.ingredient().test(stack) && ContainerRemainders.remainderFor(
                        BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()).isPresent()) {
                    buckets += counted.count();
                    break;
                }
            }
        }
        return buckets <= 0 ? java.util.List.of() : java.util.List.of(new ItemStack(Items.BUCKET, buckets));
    }

    private void returnRemainder(Level level, BlockPos pos, int inputIndex, ItemStack remainder) {
        int out = this.spec.outputSlot(0);
        ItemStack slot = this.items.get(out);
        if (slot.isEmpty()) {
            this.items.set(out, remainder);
        } else if (ItemStack.isSameItemSameComponents(slot, remainder)
                && slot.getCount() + remainder.getCount() <= slot.getMaxStackSize()) {
            slot.grow(remainder.getCount());
        } else {
            Containers.dropItemStack(level, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D, remainder);
        }
        setChanged();
    }
}
