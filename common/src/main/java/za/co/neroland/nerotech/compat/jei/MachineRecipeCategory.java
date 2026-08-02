package za.co.neroland.nerotech.compat.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.ItemLike;

import za.co.neroland.nerotech.recipe.MachineRecipe;

/**
 * One JEI page shape for all three NeroTech machine recipe types: single ingredient in, single result
 * out (the {@link MachineRecipe} shape). The category is parametrised by recipe type, title and icon,
 * so {@code ore_processing}, {@code fabricating} and {@code advanced_fabricating} share this class
 * instead of each getting a near-identical copy.
 */
public final class MachineRecipeCategory extends AbstractRecipeCategory<RecipeHolder<MachineRecipe>> {

    private static final int WIDTH = 94;
    private static final int HEIGHT = 26;
    private static final int INPUT_X = 4;
    private static final int INPUT_Y = 5;
    private static final int ARROW_X = 26;
    private static final int ARROW_Y = 6;
    private static final int OUTPUT_X = 60;
    private static final int OUTPUT_Y = 0;

    /**
     * @param guiHelper  JEI's drawable factory (from {@code IJeiHelpers})
     * @param recipeType the JEI recipe type this category draws
     * @param title      the category tab/page title
     * @param icon       the machine whose item is the category icon
     */
    public MachineRecipeCategory(IGuiHelper guiHelper, IRecipeType<RecipeHolder<MachineRecipe>> recipeType,
            Component title, ItemLike icon) {
        super(recipeType, title, guiHelper.createDrawableItemLike(icon), WIDTH, HEIGHT);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<MachineRecipe> recipe, IFocusGroup focuses) {
        MachineRecipe machineRecipe = recipe.value();
        builder.addInputSlot(INPUT_X, INPUT_Y)
                .setStandardSlotBackground()
                .add(machineRecipe.input());
        builder.addOutputSlot(OUTPUT_X, OUTPUT_Y)
                .setOutputSlotBackground()
                .add(machineRecipe.result());
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, RecipeHolder<MachineRecipe> recipe,
            IFocusGroup focuses) {
        builder.addRecipeArrow().setPosition(ARROW_X, ARROW_Y);
    }
}
