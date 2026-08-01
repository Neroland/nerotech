package za.co.neroland.nerotech.compat.jei;

import java.util.List;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.ItemLike;

import za.co.neroland.nerotech.recipe.ColliderRecipe;

/**
 * The Particle Accelerator's JEI page. The shared {@link MachineRecipeCategory} draws one input, and
 * a collision needs two — plus the line that actually matters to a player: the minimum collision
 * energy, which is really the minimum ring size they have to build.
 */
public final class ColliderRecipeCategory extends AbstractRecipeCategory<RecipeHolder<ColliderRecipe>> {

    private static final int WIDTH = 116;
    private static final int HEIGHT = 38;
    private static final int INPUT_A_X = 4;
    private static final int INPUT_B_X = 24;
    private static final int INPUT_Y = 5;
    private static final int ARROW_X = 46;
    private static final int ARROW_Y = 6;
    private static final int OUTPUT_X = 80;
    private static final int OUTPUT_Y = 0;
    private static final int TEXT_X = 2;
    private static final int TEXT_Y = 27;
    private static final int TEXT_HEIGHT = 10;

    public ColliderRecipeCategory(IGuiHelper guiHelper, IRecipeType<RecipeHolder<ColliderRecipe>> recipeType,
            Component title, ItemLike icon) {
        super(recipeType, title, guiHelper.createDrawableItemLike(icon), WIDTH, HEIGHT);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<ColliderRecipe> recipe, IFocusGroup focuses) {
        ColliderRecipe collision = recipe.value();
        builder.addInputSlot(INPUT_A_X, INPUT_Y)
                .setStandardSlotBackground()
                .add(collision.inputA());
        builder.addInputSlot(INPUT_B_X, INPUT_Y)
                .setStandardSlotBackground()
                .add(collision.inputB());
        builder.addOutputSlot(OUTPUT_X, OUTPUT_Y)
                .setOutputSlotBackground()
                .add(collision.result());
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, RecipeHolder<ColliderRecipe> recipe,
            IFocusGroup focuses) {
        builder.addRecipeArrow().setPosition(ARROW_X, ARROW_Y);
        builder.addText(List.of(Component.translatable("gui.nerotech.jei.collider.energy",
                                Integer.toString(recipe.value().minEnergy()))),
                        WIDTH - TEXT_X * 2, TEXT_HEIGHT)
                .setPosition(TEXT_X, TEXT_Y);
    }
}
