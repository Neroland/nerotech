package za.co.neroland.nerotech.machine;

import net.minecraft.world.item.ItemStack;

import za.co.neroland.nerotech.registry.ModItems;
import za.co.neroland.nerotech.tag.NeroTechTags;

/**
 * In-code Advanced Fabricator recipes (Tier 2): refine space materials into reactor fuel. Inputs are
 * matched by Core {@code c:} tags ({@code c:gems/void_crystal}) — the soft Nerospace coupling, no
 * import. Isolated for a later datapack {@code RecipeType} swap.
 */
public final class AdvancedFabricatorRecipes {

    private AdvancedFabricatorRecipes() {
    }

    public static ItemStack resultFor(ItemStack input) {
        if (input.isEmpty()) {
            return ItemStack.EMPTY;
        }
        if (input.is(NeroTechTags.C_VOID_CRYSTAL_GEMS)) {
            return new ItemStack(ModItems.FUSION_CELL.get(), 1);
        }
        return ItemStack.EMPTY;
    }
}
