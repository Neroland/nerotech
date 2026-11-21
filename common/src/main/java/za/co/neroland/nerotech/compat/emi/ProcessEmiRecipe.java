package za.co.neroland.nerotech.compat.emi;

import java.util.ArrayList;
import java.util.List;

import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerotech.fluid.NeroTechFluids;
import za.co.neroland.nerotech.gas.NeroTechGases;
import za.co.neroland.nerotech.recipe.ProcessMedium;
import za.co.neroland.nerotech.recipe.ProcessRecipe;

/**
 * The EMI twin of {@code compat.jei.ProcessRecipeCategory}: one page shape for every
 * {@link ProcessRecipe} type — up to six counted inputs, fluid and gas inputs (gases as their transport
 * fluids), a catalyst, up to four outputs with chances, fluid and gas outputs, and an energy/time line.
 */
final class ProcessEmiRecipe extends BasicEmiRecipe {

    private static final int WIDTH = 160;
    private static final int HEIGHT = 74;
    private static final int SLOT = 18;
    /** Vanilla's dark-grey recipe-page text colour. */
    private static final int TEXT_COLOR = 0xFF404040;

    private final List<EmiIngredient> itemInputs = new ArrayList<>();
    private final List<EmiIngredient> mediaInputs = new ArrayList<>();
    private final List<EmiStack> itemOutputs = new ArrayList<>();
    private final List<EmiStack> mediaOutputs = new ArrayList<>();
    @Nullable
    private final EmiIngredient catalyst;
    private final int energy;
    private final int time;

    ProcessEmiRecipe(EmiRecipeCategory category, RecipeHolder<ProcessRecipe> holder) {
        //? if >=1.21.2 {
        super(category, holder.id().identifier(), WIDTH, HEIGHT);
        //?} else {
        /*super(category, holder.id(), WIDTH, HEIGHT);
        *///?}
        ProcessRecipe recipe = holder.value();
        for (ProcessRecipe.CountedIngredient counted : recipe.items()) {
            this.itemInputs.add(EmiIngredient.of(counted.ingredient(), counted.count()));
        }
        recipe.fluidIn().map(medium -> medium(medium, false)).ifPresent(this.mediaInputs::add);
        recipe.gasIn().map(medium -> medium(medium, true)).ifPresent(this.mediaInputs::add);
        this.catalyst = recipe.catalyst().map(EmiIngredient::of).orElse(null);
        for (ProcessRecipe.ChanceResult result : recipe.results()) {
            EmiStack stack = EmiStack.of(result.create());
            if (!result.guaranteed()) {
                stack.setChance(result.chancePermille() / 1000.0F);
            }
            this.itemOutputs.add(stack);
        }
        recipe.fluidOut().map(medium -> medium(medium, false)).filter(EmiStack.class::isInstance)
                .map(EmiStack.class::cast).ifPresent(this.mediaOutputs::add);
        recipe.gasOut().map(medium -> medium(medium, true)).filter(EmiStack.class::isInstance)
                .map(EmiStack.class::cast).ifPresent(this.mediaOutputs::add);
        this.energy = recipe.energy();
        this.time = recipe.time();

        this.inputs = new ArrayList<>(this.itemInputs);
        this.inputs.addAll(this.mediaInputs);
        this.catalysts = this.catalyst == null ? List.of() : List.of(this.catalyst);
        this.outputs = new ArrayList<>(this.itemOutputs);
        this.outputs.addAll(this.mediaOutputs);
    }

    /** A fluid (or a fluid tag) or a gas as its transport fluid; amounts in mB. */
    private static EmiIngredient medium(ProcessMedium medium, boolean gas) {
        long amount = gas ? (long) medium.amount() * NeroTechGases.UNIT_MB : medium.amount();
        Identifier id = Identifier.tryParse(medium.key());
        if (id == null) {
            return EmiStack.EMPTY;
        }
        if (!gas && medium.isTag()) {
            return EmiIngredient.of(TagKey.create(Registries.FLUID, id), amount);
        }
        Fluid fluid = gas ? NeroTechFluids.fluidFor(id) : BuiltInRegistries.FLUID.getValue(id);
        return fluid == null || fluid == Fluids.EMPTY ? EmiStack.EMPTY : EmiStack.of(fluid, amount);
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        for (int i = 0; i < this.itemInputs.size(); i++) {
            widgets.addSlot(this.itemInputs.get(i), (i % 3) * SLOT, (i / 3) * SLOT);
        }
        for (int i = 0; i < this.mediaInputs.size(); i++) {
            widgets.addSlot(this.mediaInputs.get(i), i * SLOT, 38);
        }
        if (this.catalyst != null) {
            widgets.addSlot(this.catalyst, 60, 0).catalyst(true);
        }
        widgets.addTexture(EmiTexture.EMPTY_ARROW, 58, 20);
        for (int i = 0; i < this.itemOutputs.size(); i++) {
            widgets.addSlot(this.itemOutputs.get(i), 88 + (i % 2) * SLOT, (i / 2) * SLOT).recipeContext(this);
        }
        for (int i = 0; i < this.mediaOutputs.size(); i++) {
            widgets.addSlot(this.mediaOutputs.get(i), 128, i * SLOT).recipeContext(this);
        }
        widgets.addText(Component.translatable("gui.nerotech.jei.process.energy",
                        Integer.toString(this.energy), Integer.toString(this.time)),
                0, 62, TEXT_COLOR, false);
    }
}
