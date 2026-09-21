package za.co.neroland.nerotech.compat.jei;

import java.util.Collection;
import java.util.List;

import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.crafting.RecipeType;

/**
 * The client-side copy of NeroTech's machine recipes, as synced by the server.
 *
 * <p>Since 26.x the vanilla client keeps no full recipe list — only the vanilla recipe-book display
 * entries — so a recipe viewer cannot read modded recipes off the level. Both loaders therefore ship
 * an <b>opt-in</b> recipe sync, and both halves of it are loader API:
 *
 * <ul>
 *   <li><b>server</b> — NeoForge: {@code OnDatapackSyncEvent.sendRecipes(...)};
 *       Fabric: {@code RecipeSynchronization.synchronizeRecipeSerializer(...)};</li>
 *   <li><b>client</b> — NeoForge: {@code RecipesReceivedEvent}; Fabric:
 *       {@code ClientRecipeSynchronizedEvent}.</li>
 * </ul>
 *
 * <p>Each loader's entry point does its half and hands the resulting {@link RecipeMap} to
 * {@link #accept(RecipeMap)}; {@link NeroTechJeiPlugin} then reads it from shared code. Nothing here
 * is personal data — these are recipe definitions from the server's datapacks.
 */
public final class JeiSyncedRecipes {

    /** Written from the network thread, read from the render thread — hence volatile. */
    private static volatile List<RecipeHolder<?>> recipes = List.of();

    private JeiSyncedRecipes() {
    }

    /** Called by each loader's client wiring when the server's recipe payload arrives. */
    public static void accept(RecipeMap recipeMap) {
        accept(recipeMap == null ? null : recipeMap.values());
    }

    /**
     * Same as {@link #accept(RecipeMap)} for loaders that hand over a plain collection (Fabric). Minecraft
     * 26.3 no longer builds a {@link RecipeMap} from a collection, so this is the version-neutral path.
     */
    public static void accept(Collection<RecipeHolder<?>> holders) {
        recipes = holders == null ? List.of() : List.copyOf(holders);
    }

    /**
     * Every synced recipe of one machine recipe type. Empty before the first sync, on a server that
     * does not send them (an older NeroTech), and on any loader without a recipe-sync API.
     */
    public static <I extends RecipeInput, T extends Recipe<I>> List<RecipeHolder<T>> byType(RecipeType<T> type) {
        List<RecipeHolder<T>> out = new java.util.ArrayList<>();
        for (RecipeHolder<?> holder : recipes) {
            if (holder.value().getType() == type) {
                @SuppressWarnings("unchecked")
                RecipeHolder<T> typed = (RecipeHolder<T>) holder;
                out.add(typed);
            }
        }
        return List.copyOf(out);
    }
}
