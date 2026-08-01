package za.co.neroland.nerotech.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

/**
 * The two particles arriving at an Accelerator Controller: {@code circulating} is the one that was
 * injected and has been lapping the ring, {@code target} is the one sitting in the collision slot.
 * Order carries no meaning — {@link ColliderRecipe#matches} tests both ways round.
 */
public record ColliderRecipeInput(ItemStack circulating, ItemStack target) implements RecipeInput {

    @Override
    public ItemStack getItem(int index) {
        return switch (index) {
            case 0 -> this.circulating;
            case 1 -> this.target;
            default -> throw new IllegalArgumentException("No item for index " + index);
        };
    }

    @Override
    public int size() {
        return 2;
    }
}
