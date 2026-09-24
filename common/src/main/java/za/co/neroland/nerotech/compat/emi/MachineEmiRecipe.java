package za.co.neroland.nerotech.compat.emi;

import java.util.List;

import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;

import net.minecraft.world.item.crafting.RecipeHolder;

import za.co.neroland.nerotech.recipe.MachineRecipe;

/**
 * One page shape for every one-in/one-out NeroTech machine recipe ({@code ore_processing}, {@code fabricating},
 * {@code advanced_fabricating}, {@code chemical_processing}) — the EMI twin of
 * {@code compat.jei.MachineRecipeCategory}.
 */
final class MachineEmiRecipe extends BasicEmiRecipe {

    private static final int WIDTH = 94;
    private static final int HEIGHT = 26;

    MachineEmiRecipe(EmiRecipeCategory category, RecipeHolder<MachineRecipe> holder) {
        super(category, holder.id().identifier(), WIDTH, HEIGHT);
        MachineRecipe recipe = holder.value();
        this.inputs = List.of(EmiIngredient.of(recipe.input()));
        this.outputs = List.of(EmiStack.of(recipe.result().create()));
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        widgets.addSlot(inputs.get(0), 4, 4);
        widgets.addTexture(EmiTexture.EMPTY_ARROW, 30, 5);
        widgets.addSlot(outputs.get(0), 64, 0).large(true).recipeContext(this);
    }
}
