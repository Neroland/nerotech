package za.co.neroland.nerotech.recipe;

//? if <1.20.2 {
/*import java.util.Collection;
import java.util.List;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;

public record LegacyRecipeHolder<R extends Recipe<?>>(ResourceLocation id, R value) {
    public static <R extends Recipe<?>> LegacyRecipeHolder<R> of(R recipe) {
        return new LegacyRecipeHolder<>(recipe.getId(), recipe);
    }

    public static List<LegacyRecipeHolder<?>> wrapAll(Collection<Recipe<?>> recipes) {
        return recipes.stream().<LegacyRecipeHolder<?>>map(LegacyRecipeHolder::of).toList();
    }
}
*///?}
