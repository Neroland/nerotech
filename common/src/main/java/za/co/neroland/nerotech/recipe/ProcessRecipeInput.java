package za.co.neroland.nerotech.recipe;

import java.util.List;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import org.jetbrains.annotations.Nullable;

/**
 * What a process machine offers a {@link ProcessRecipe}: its input slots, its catalyst slot, the fluid
 * and gas in its input tanks (amounts in mB), its current heat and — for the Atmospheric Condenser —
 * the atmosphere name it reads. Slots are indexed items first, then the catalyst.
 */
public record ProcessRecipeInput(List<ItemStack> items, ItemStack catalyst, Fluid fluid, long fluidAmount,
        @Nullable Identifier gas, long gasAmount, int heat, String atmosphere) implements MachineRecipeInput {

    public ProcessRecipeInput {
        items = List.copyOf(items);
        catalyst = catalyst == null ? ItemStack.EMPTY : catalyst;
        fluid = fluid == null ? Fluids.EMPTY : fluid;
        atmosphere = atmosphere == null ? "" : atmosphere;
    }

    /** Items only: no catalyst, no media, cold, no atmosphere. */
    public static ProcessRecipeInput ofItems(List<ItemStack> items) {
        return new ProcessRecipeInput(items, ItemStack.EMPTY, Fluids.EMPTY, 0L, null, 0L, 0, "");
    }

    @Override
    public ItemStack getItem(int index) {
        if (index >= 0 && index < this.items.size()) {
            return this.items.get(index);
        }
        if (index == this.items.size()) {
            return this.catalyst;
        }
        throw new IllegalArgumentException("No item for index " + index);
    }

    @Override
    public int size() {
        return this.items.size() + 1;
    }
}
