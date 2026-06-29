package za.co.neroland.nerotech.machine;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import za.co.neroland.nerotech.registry.ModItems;

/**
 * In-code Ore Processor recipes (Earth tier): raw ore / raw material → 2 dust. Isolated in one class so
 * a later swap to a datapack-driven {@code RecipeType}/{@code RecipeSerializer} is a localised change —
 * mirroring Nerospace's GrinderRecipes approach. Dust outputs carry {@code c:dusts/<metal>}, so they
 * interoperate with Mekanism/Create/other processing chains.
 */
public final class OreProcessorRecipes {

    private OreProcessorRecipes() {
    }

    /** One input → output pairing, for display/integration. */
    public record Processing(ItemStack input, ItemStack output) {
    }

    public static ItemStack resultFor(ItemStack input) {
        if (input.isEmpty()) {
            return ItemStack.EMPTY;
        }
        if (input.is(Items.IRON_ORE) || input.is(Items.DEEPSLATE_IRON_ORE) || input.is(Items.RAW_IRON)) {
            return new ItemStack(ModItems.IRON_DUST.get(), 2);
        }
        if (input.is(Items.COPPER_ORE) || input.is(Items.DEEPSLATE_COPPER_ORE) || input.is(Items.RAW_COPPER)) {
            return new ItemStack(ModItems.COPPER_DUST.get(), 2);
        }
        if (input.is(Items.GOLD_ORE) || input.is(Items.DEEPSLATE_GOLD_ORE) || input.is(Items.RAW_GOLD)) {
            return new ItemStack(ModItems.GOLD_DUST.get(), 2);
        }
        return ItemStack.EMPTY;
    }

    /** @return every processing pairing, derived through {@link #resultFor} so display can't drift. */
    public static List<Processing> all() {
        List<Processing> out = new ArrayList<>();
        for (net.minecraft.world.item.Item item : List.of(
                Items.IRON_ORE, Items.DEEPSLATE_IRON_ORE, Items.RAW_IRON,
                Items.COPPER_ORE, Items.DEEPSLATE_COPPER_ORE, Items.RAW_COPPER,
                Items.GOLD_ORE, Items.DEEPSLATE_GOLD_ORE, Items.RAW_GOLD)) {
            ItemStack in = new ItemStack(item);
            ItemStack result = resultFor(in);
            if (!result.isEmpty()) {
                out.add(new Processing(in, result));
            }
        }
        return List.copyOf(out);
    }
}
