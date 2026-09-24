package za.co.neroland.nerotech.compat.emi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ItemLike;

import za.co.neroland.nerotech.NeroTechCommon;
import za.co.neroland.nerotech.compat.jei.JeiSyncedRecipes;
import za.co.neroland.nerotech.recipe.ColliderRecipe;
import za.co.neroland.nerotech.recipe.MachineRecipe;
import za.co.neroland.nerotech.registry.ModBlocks;
import za.co.neroland.nerotech.registry.ModRecipeTypes;

/**
 * NeroTech's EMI integration — the native counterpart of {@code compat.jei.NeroTechJeiPlugin}: one page
 * per machine recipe type, with the machines that run them as workstations.
 *
 * <p>Built against the community "EMI Unofficial Port" (official EMI has no Minecraft 26.x release), which
 * keeps the upstream {@code dev.emi.emi.api} package. Only that API is used, so the class lives in
 * {@code common}: NeoForge finds it through {@link EmiEntrypoint}, Fabric through the {@code emi} entrypoint
 * in {@code fabric.mod.json}. EMI is compile-time only; without it this class is never loaded. With a native
 * plugin present, EMI's JEI bridge skips JEI categories in the {@code nerotech} namespace, so JEI + EMI
 * together do not show these pages twice.</p>
 *
 * <p>Recipes: since 26.x the client keeps no full recipe list, so both loaders opt NeroTech's machine
 * serializers into the server's recipe sync. EMI hands the synced recipes to plugins itself, and reloads
 * only after they arrive — that copy is read first. {@link JeiSyncedRecipes} (filled by NeroTech's own
 * loader wiring from the same packet) is the fallback.</p>
 */
@EmiEntrypoint
public final class NeroTechEmiPlugin implements EmiPlugin {

    public static final EmiRecipeCategory ORE_PROCESSING = category("ore_processing", ModBlocks.ORE_PROCESSOR.get());
    public static final EmiRecipeCategory FABRICATING = category("fabricating", ModBlocks.FABRICATOR.get());
    public static final EmiRecipeCategory ADVANCED_FABRICATING =
            category("advanced_fabricating", ModBlocks.ADVANCED_FABRICATOR.get());
    public static final EmiRecipeCategory COLLIDER = category("collider", ModBlocks.COLLIDER_CORE.get());
    public static final EmiRecipeCategory CHEMICAL_PROCESSING =
            category("chemical_processing", ModBlocks.CHEMICAL_PROCESSOR.get());

    @Override
    public void register(EmiRegistry registry) {
        registry.addCategory(ORE_PROCESSING);
        registry.addCategory(FABRICATING);
        registry.addCategory(ADVANCED_FABRICATING);
        registry.addCategory(COLLIDER);
        registry.addCategory(CHEMICAL_PROCESSING);

        // The Advanced Ore Processor runs the same ore_processing recipes as the Tier-1 machine (it adds a
        // yield bonus on top), so both are workstations for that page.
        registry.addWorkstation(ORE_PROCESSING, EmiStack.of(ModBlocks.ORE_PROCESSOR.get()));
        registry.addWorkstation(ORE_PROCESSING, EmiStack.of(ModBlocks.ADVANCED_ORE_PROCESSOR.get()));
        registry.addWorkstation(FABRICATING, EmiStack.of(ModBlocks.FABRICATOR.get()));
        registry.addWorkstation(ADVANCED_FABRICATING, EmiStack.of(ModBlocks.ADVANCED_FABRICATOR.get()));
        registry.addWorkstation(COLLIDER, EmiStack.of(ModBlocks.COLLIDER_CORE.get()));
        registry.addWorkstation(CHEMICAL_PROCESSING, EmiStack.of(ModBlocks.CHEMICAL_PROCESSOR.get()));

        Collection<RecipeHolder<?>> synced = syncedRecipes(registry);
        addMachine(registry, synced, ORE_PROCESSING, ModRecipeTypes.ORE_PROCESSING.get());
        addMachine(registry, synced, FABRICATING, ModRecipeTypes.FABRICATING.get());
        addMachine(registry, synced, ADVANCED_FABRICATING, ModRecipeTypes.ADVANCED_FABRICATING.get());
        addMachine(registry, synced, CHEMICAL_PROCESSING, ModRecipeTypes.CHEMICAL_PROCESSING.get());
        for (RecipeHolder<ColliderRecipe> holder : byType(synced, ModRecipeTypes.COLLIDER.get())) {
            registry.addRecipe(new ColliderEmiRecipe(holder));
        }
    }

    /** EMI's copy of the synced recipes, or {@code null} when it has none (see {@link #byType}). */
    private static Collection<RecipeHolder<?>> syncedRecipes(EmiRegistry registry) {
        //? if >=26.3 {
        /*Collection<RecipeHolder<?>> fromEmi = registry.getRecipes();
        *///?} else {
        net.minecraft.world.item.crafting.RecipeMap recipeMap = registry.getRecipeMap();
        Collection<RecipeHolder<?>> fromEmi = recipeMap == null ? null : recipeMap.values();
        //?}
        return fromEmi != null && !fromEmi.isEmpty() ? fromEmi : null;
    }

    private static void addMachine(EmiRegistry registry, Collection<RecipeHolder<?>> synced,
            EmiRecipeCategory category, RecipeType<MachineRecipe> type) {
        for (RecipeHolder<MachineRecipe> holder : byType(synced, type)) {
            registry.addRecipe(new MachineEmiRecipe(category, holder));
        }
    }

    /** Recipes of one type from EMI's synced copy, falling back to {@link JeiSyncedRecipes}. */
    private static <I extends RecipeInput, T extends Recipe<I>> List<RecipeHolder<T>> byType(
            Collection<RecipeHolder<?>> synced, RecipeType<T> type) {
        if (synced == null) {
            return JeiSyncedRecipes.byType(type);
        }
        List<RecipeHolder<T>> out = new ArrayList<>();
        for (RecipeHolder<?> holder : synced) {
            if (holder.value().getType() == type) {
                @SuppressWarnings("unchecked")
                RecipeHolder<T> typed = (RecipeHolder<T>) holder;
                out.add(typed);
            }
        }
        return out;
    }

    private static EmiRecipeCategory category(String path, ItemLike icon) {
        return new EmiRecipeCategory(Identifier.fromNamespaceAndPath(NeroTechCommon.MOD_ID, path), EmiStack.of(icon));
    }
}
