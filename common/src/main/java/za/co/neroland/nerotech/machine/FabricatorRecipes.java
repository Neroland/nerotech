package za.co.neroland.nerotech.machine;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import za.co.neroland.nerotech.registry.ModItems;

/**
 * In-code Fabricator recipes (Earth tier): refined material → NeroTech component. Isolated in one class
 * so a later swap to a datapack-driven {@code RecipeType}/{@code RecipeSerializer} is localised
 * (mirrors Nerospace's GrinderRecipes approach).
 */
public final class FabricatorRecipes {

    private FabricatorRecipes() {
    }

    public record Fabrication(ItemStack input, ItemStack output) {
    }

    public static ItemStack resultFor(ItemStack input) {
        if (input.isEmpty()) {
            return ItemStack.EMPTY;
        }
        if (input.is(Items.IRON_INGOT)) {
            return new ItemStack(ModItems.MACHINE_FRAME.get(), 1);
        }
        if (input.is(ModItems.COPPER_DUST.get())) {
            return new ItemStack(ModItems.NERO_COIL.get(), 1);
        }
        if (input.is(ModItems.GOLD_DUST.get())) {
            return new ItemStack(ModItems.CIRCUIT_BOARD.get(), 1);
        }
        return ItemStack.EMPTY;
    }

    public static List<Fabrication> all() {
        List<Fabrication> out = new ArrayList<>();
        for (net.minecraft.world.item.Item item : List.of(
                Items.IRON_INGOT, ModItems.COPPER_DUST.get(), ModItems.GOLD_DUST.get())) {
            ItemStack in = new ItemStack(item);
            ItemStack result = resultFor(in);
            if (!result.isEmpty()) {
                out.add(new Fabrication(in, result));
            }
        }
        return List.copyOf(out);
    }
}
