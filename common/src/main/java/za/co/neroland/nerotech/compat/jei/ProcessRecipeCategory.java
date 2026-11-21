package za.co.neroland.nerotech.compat.jei;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerotech.fluid.NeroTechFluids;
import za.co.neroland.nerotech.gas.NeroTechGases;
import za.co.neroland.nerotech.recipe.ProcessMedium;
import za.co.neroland.nerotech.recipe.ProcessRecipe;

/**
 * One JEI page shape for all eleven {@link ProcessRecipe} types: up to six counted inputs (two rows of
 * three), a fluid and a gas input, a catalyst slot, up to four outputs with their chance, a fluid and
 * a gas output, and a text line with energy, time and the heat/atmosphere conditions. Gases are shown
 * as their transport fluids. A fluid named by tag is listed in the text line instead of a slot.
 */
public final class ProcessRecipeCategory extends AbstractRecipeCategory<RecipeHolder<ProcessRecipe>> {

    private static final int WIDTH = 160;
    private static final int HEIGHT = 74;
    private static final int SLOT = 18;
    private static final int INPUT_X = 0;
    private static final int INPUT_Y = 0;
    private static final int MEDIA_IN_Y = 38;
    private static final int CATALYST_X = 60;
    private static final int CATALYST_Y = 0;
    private static final int ARROW_X = 58;
    private static final int ARROW_Y = 20;
    private static final int OUTPUT_X = 88;
    private static final int OUTPUT_Y = 0;
    private static final int MEDIA_OUT_X = 128;
    private static final int TEXT_Y = 58;
    private static final int TEXT_HEIGHT = 16;

    public ProcessRecipeCategory(IGuiHelper guiHelper, IRecipeType<RecipeHolder<ProcessRecipe>> recipeType,
            Component title, ItemLike icon) {
        super(recipeType, title, guiHelper.createDrawableItemLike(icon), WIDTH, HEIGHT);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<ProcessRecipe> holder, IFocusGroup focuses) {
        ProcessRecipe recipe = holder.value();
        List<ProcessRecipe.CountedIngredient> items = recipe.items();
        for (int i = 0; i < items.size(); i++) {
            IRecipeSlotBuilder slot = builder.addInputSlot(INPUT_X + (i % 3) * SLOT, INPUT_Y + (i / 3) * SLOT)
                    .setStandardSlotBackground();
            //? if >=1.21.2 {
            slot.add(items.get(i).ingredient());
            //?} else {
            /*slot.addIngredients(items.get(i).ingredient());
            *///?}
            int count = items.get(i).count();
            if (count > 1) {
                slot.addRichTooltipCallback((view, tooltip) ->
                        tooltip.add(Component.translatable("gui.nerotech.jei.process.count", count)));
            }
        }
        recipe.catalyst().ifPresent(catalyst -> {
            IRecipeSlotBuilder slot = builder.addInputSlot(CATALYST_X, CATALYST_Y).setStandardSlotBackground();
            //? if >=1.21.2 {
            slot.add(catalyst);
            //?} else {
            /*slot.addIngredients(catalyst);
            *///?}
            slot.addRichTooltipCallback((view, tooltip) ->
                    tooltip.add(Component.translatable("gui.nerotech.jei.process.catalyst")));
        });
        mediumSlot(builder, true, 0, recipe.fluidIn().orElse(null), false);
        mediumSlot(builder, true, SLOT, recipe.gasIn().orElse(null), true);

        List<ProcessRecipe.ChanceResult> results = recipe.results();
        for (int i = 0; i < results.size(); i++) {
            ProcessRecipe.ChanceResult result = results.get(i);
            IRecipeSlotBuilder slot = builder.addOutputSlot(OUTPUT_X + (i % 2) * SLOT, OUTPUT_Y + (i / 2) * SLOT)
                    .setOutputSlotBackground();
            ItemStack stack = result.create();
            //? if >=1.21.2 {
            slot.add(stack);
            //?} else {
            /*slot.addItemStack(stack);
            *///?}
            if (!result.guaranteed()) {
                String percent = String.format(Locale.ROOT, "%.1f", result.chancePermille() / 10.0);
                slot.addRichTooltipCallback((view, tooltip) ->
                        tooltip.add(Component.translatable("gui.nerotech.jei.process.chance", percent)));
            }
        }
        mediumSlot(builder, false, 0, recipe.fluidOut().orElse(null), false);
        mediumSlot(builder, false, SLOT, recipe.gasOut().orElse(null), true);
    }

    /** A fluid or gas slot (gas as its transport fluid); tags and unknown ids get no slot. */
    private static void mediumSlot(IRecipeLayoutBuilder builder, boolean input, int offset,
            @Nullable ProcessMedium medium, boolean gas) {
        if (medium == null || medium.isTag()) {
            return;
        }
        Fluid fluid = fluidFor(medium, gas);
        if (fluid == Fluids.EMPTY) {
            return;
        }
        long amount = gas ? (long) medium.amount() * NeroTechGases.UNIT_MB : medium.amount();
        IRecipeSlotBuilder slot = input
                ? builder.addInputSlot(INPUT_X + offset, MEDIA_IN_Y)
                : builder.addOutputSlot(MEDIA_OUT_X, OUTPUT_Y + offset);
        slot.setStandardSlotBackground().addFluidStack(fluid, amount);
    }

    static Fluid fluidFor(ProcessMedium medium, boolean gas) {
        Identifier id = Identifier.tryParse(medium.key());
        if (id == null) {
            return Fluids.EMPTY;
        }
        if (gas) {
            return NeroTechFluids.fluidFor(id);
        }
        Fluid fluid = BuiltInRegistries.FLUID.getValue(id);
        return fluid == null ? Fluids.EMPTY : fluid;
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, RecipeHolder<ProcessRecipe> holder,
            IFocusGroup focuses) {
        builder.addRecipeArrow().setPosition(ARROW_X, ARROW_Y);
        builder.addText(summary(holder.value()), WIDTH, TEXT_HEIGHT).setPosition(0, TEXT_Y);
    }

    /** Energy and time, then any heat floor, atmosphere and tag-named fluids. */
    static List<net.minecraft.network.chat.FormattedText> summary(ProcessRecipe recipe) {
        List<net.minecraft.network.chat.FormattedText> lines = new ArrayList<>();
        lines.add(Component.translatable("gui.nerotech.jei.process.energy",
                Integer.toString(recipe.energy()), Integer.toString(recipe.time())));
        List<Component> conditions = new ArrayList<>();
        if (recipe.minHeat() > 0) {
            conditions.add(Component.translatable("gui.nerotech.jei.process.min_heat", Integer.toString(recipe.minHeat())));
        }
        recipe.atmosphere().ifPresent(atmosphere ->
                conditions.add(Component.translatable("gui.nerotech.jei.process.atmosphere", atmosphere)));
        recipe.fluidIn().filter(ProcessMedium::isTag).ifPresent(medium ->
                conditions.add(Component.translatable("gui.nerotech.jei.process.fluid_tag",
                        medium.id(), Integer.toString(medium.amount()))));
        if (!conditions.isEmpty()) {
            net.minecraft.network.chat.MutableComponent joined = Component.empty();
            for (int i = 0; i < conditions.size(); i++) {
                if (i > 0) {
                    joined.append(" · ");
                }
                joined.append(conditions.get(i));
            }
            lines.add(joined);
        }
        return lines;
    }
}
