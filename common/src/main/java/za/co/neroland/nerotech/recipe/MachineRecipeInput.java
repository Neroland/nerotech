package za.co.neroland.nerotech.recipe;

//? if >=1.21 {
import net.minecraft.world.item.crafting.RecipeInput;

interface MachineRecipeInput extends RecipeInput { }
//?} else {
/*import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

interface MachineRecipeInput extends Container {
    int size();

    @Override
    default int getContainerSize() {
        return size();
    }

    @Override
    default boolean isEmpty() {
        for (int slot = 0; slot < size(); slot++) {
            if (!getItem(slot).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    default ItemStack removeItem(int slot, int count) {
        throw new UnsupportedOperationException("Recipe inputs are read-only");
    }

    @Override
    default ItemStack removeItemNoUpdate(int slot) {
        throw new UnsupportedOperationException("Recipe inputs are read-only");
    }

    @Override
    default void setItem(int slot, ItemStack stack) {
        throw new UnsupportedOperationException("Recipe inputs are read-only");
    }

    @Override
    default void clearContent() {
        throw new UnsupportedOperationException("Recipe inputs are read-only");
    }

    @Override
    default void setChanged() { }

    @Override
    default boolean stillValid(Player player) {
        return false;
    }
}
*///?}
