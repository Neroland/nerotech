package za.co.neroland.nerotech.compat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.types.IRecipeHolderType;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeHolder;

import za.co.neroland.nerotech.NeroTechCommon;
import za.co.neroland.nerotech.recipe.MachineRecipe;
import za.co.neroland.nerotech.registry.ModBlocks;
import za.co.neroland.nerotech.registry.ModRecipeTypes;

/**
 * NeroTech's Just Enough Items integration: one page per machine recipe type, plus the machines that
 * run them as crafting stations.
 *
 * <p>The plugin lives in {@code common} and uses only the loader-agnostic JEI common API, so the one
 * class serves every loader — JEI scans for {@link JeiPlugin} on all of them. JEI is an optional
 * compile-time dependency; without it this class is simply never loaded.
 *
 * <p>The recipes themselves come from {@link JeiSyncedRecipes} rather than the level: since 26.x the
 * client holds no full recipe list, so each loader opts into the server's recipe sync and hands the
 * result to that holder.
 */
@JeiPlugin
public final class NeroTechJeiPlugin implements IModPlugin {

    private static final Identifier PLUGIN_UID =
            Identifier.fromNamespaceAndPath(NeroTechCommon.MOD_ID, "jei");

    /** Ore Processor + Advanced Ore Processor pages ({@code nerotech:ore_processing}). */
    public static final IRecipeType<RecipeHolder<MachineRecipe>> ORE_PROCESSING = machineType("ore_processing");
    /** Fabricator pages ({@code nerotech:fabricating}). */
    public static final IRecipeType<RecipeHolder<MachineRecipe>> FABRICATING = machineType("fabricating");
    /** Advanced Fabricator pages ({@code nerotech:advanced_fabricating}). */
    public static final IRecipeType<RecipeHolder<MachineRecipe>> ADVANCED_FABRICATING =
            machineType("advanced_fabricating");
    /** Particle Collider pages ({@code nerotech:collider}). */
    public static final IRecipeType<RecipeHolder<MachineRecipe>> COLLIDER = machineType("collider");
    /** Chemical Processor pages ({@code nerotech:chemical_processing} — the oxygen wash). */
    public static final IRecipeType<RecipeHolder<MachineRecipe>> CHEMICAL_PROCESSING =
            machineType("chemical_processing");

    @Override
    public Identifier getPluginUid() {
        return PLUGIN_UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper guiHelper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(
                new MachineRecipeCategory(guiHelper, ORE_PROCESSING,
                        Component.translatable("gui.nerotech.jei.ore_processing"),
                        ModBlocks.ORE_PROCESSOR.get()),
                new MachineRecipeCategory(guiHelper, FABRICATING,
                        Component.translatable("gui.nerotech.jei.fabricating"),
                        ModBlocks.FABRICATOR.get()),
                new MachineRecipeCategory(guiHelper, ADVANCED_FABRICATING,
                        Component.translatable("gui.nerotech.jei.advanced_fabricating"),
                        ModBlocks.ADVANCED_FABRICATOR.get()),
                new MachineRecipeCategory(guiHelper, COLLIDER,
                        Component.translatable("gui.nerotech.jei.collider"),
                        ModBlocks.COLLIDER_CORE.get()),
                // The oxygen cost is a single config value, not a recipe field, so the shared
                // one-in/one-out page shape fits; the block-item tooltip names the cost.
                new MachineRecipeCategory(guiHelper, CHEMICAL_PROCESSING,
                        Component.translatable("gui.nerotech.jei.chemical_processing"),
                        ModBlocks.CHEMICAL_PROCESSOR.get()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(ORE_PROCESSING,
                JeiSyncedRecipes.byType(ModRecipeTypes.ORE_PROCESSING.get()));
        registration.addRecipes(FABRICATING,
                JeiSyncedRecipes.byType(ModRecipeTypes.FABRICATING.get()));
        registration.addRecipes(ADVANCED_FABRICATING,
                JeiSyncedRecipes.byType(ModRecipeTypes.ADVANCED_FABRICATING.get()));
        registration.addRecipes(COLLIDER,
                JeiSyncedRecipes.byType(ModRecipeTypes.COLLIDER.get()));
        registration.addRecipes(CHEMICAL_PROCESSING,
                JeiSyncedRecipes.byType(ModRecipeTypes.CHEMICAL_PROCESSING.get()));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        // The Advanced Ore Processor runs the very same ore_processing recipes as the Tier-1 machine
        // (it adds a yield bonus on top), so both are stations for that page.
        registration.addCraftingStation(ORE_PROCESSING,
                ModBlocks.ORE_PROCESSOR.get(), ModBlocks.ADVANCED_ORE_PROCESSOR.get());
        registration.addCraftingStation(FABRICATING, ModBlocks.FABRICATOR.get());
        registration.addCraftingStation(ADVANCED_FABRICATING, ModBlocks.ADVANCED_FABRICATOR.get());
        registration.addCraftingStation(COLLIDER, ModBlocks.COLLIDER_CORE.get());
        registration.addCraftingStation(CHEMICAL_PROCESSING, ModBlocks.CHEMICAL_PROCESSOR.get());
    }

    /** A JEI recipe type keyed by the same id as the registered vanilla {@code RecipeType}. */
    private static IRecipeType<RecipeHolder<MachineRecipe>> machineType(String path) {
        return IRecipeHolderType.<MachineRecipe>create(
                Identifier.fromNamespaceAndPath(NeroTechCommon.MOD_ID, path));
    }
}
