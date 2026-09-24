package za.co.neroland.nerotech.compat.emi;

import java.util.List;

import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeHolder;

import za.co.neroland.nerotech.recipe.ColliderRecipe;

/**
 * The Particle Accelerator's EMI page — the twin of {@code compat.jei.ColliderRecipeCategory}: two inputs,
 * one result, and the line that matters most to a player, the minimum collision energy (in practice the
 * minimum ring size they have to build).
 */
final class ColliderEmiRecipe extends BasicEmiRecipe {

    private static final int WIDTH = 116;
    private static final int HEIGHT = 40;
    /** Vanilla's dark-grey recipe-page text colour, as JEI's category uses by default. */
    private static final int TEXT_COLOR = 0xFF404040;

    private final int minEnergy;

    ColliderEmiRecipe(RecipeHolder<ColliderRecipe> holder) {
        super(NeroTechEmiPlugin.COLLIDER, holder.id().identifier(), WIDTH, HEIGHT);
        ColliderRecipe recipe = holder.value();
        this.minEnergy = recipe.minEnergy();
        this.inputs = List.of(EmiIngredient.of(recipe.inputA()), EmiIngredient.of(recipe.inputB()));
        this.outputs = List.of(EmiStack.of(recipe.result().create()));
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        widgets.addSlot(inputs.get(0), 4, 4);
        widgets.addSlot(inputs.get(1), 24, 4);
        widgets.addTexture(EmiTexture.EMPTY_ARROW, 48, 5);
        widgets.addSlot(outputs.get(0), 84, 0).large(true).recipeContext(this);
        widgets.addText(Component.translatable("gui.nerotech.jei.collider.energy", Integer.toString(minEnergy)),
                2, 30, TEXT_COLOR, false);
    }
}
