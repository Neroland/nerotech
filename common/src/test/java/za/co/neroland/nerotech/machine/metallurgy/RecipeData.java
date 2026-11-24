package za.co.neroland.nerotech.machine.metallurgy;

import java.io.IOException;
import java.io.Reader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import za.co.neroland.nerotech.machine.components.RecipeFixture;
import za.co.neroland.nerotech.recipe.ProcessMatching;

/**
 * Test helper: reads the shipped process-recipe JSONs and item tags straight from the resources and
 * matches them on item ids with {@link ProcessMatching} (the matcher every process machine uses), so
 * the metallurgy rule tests run without a Minecraft bootstrap. A stack here is an item id string.
 */
final class RecipeData {

    /** One recipe as the tests see it: counted ingredients, optional catalyst, the first result. */
    record View(String file, List<String> ingredients, List<Integer> counts, String catalyst, String result,
            int resultCount, boolean fluidInput) {
    }

    private RecipeData() {
    }

    static Path resources() {
        URL marker = RecipeData.class.getClassLoader().getResource(
                RecipeFixture.dataPath("data/nerotech/tags/item/press_dies.json"));
        if (marker == null) {
            throw new IllegalStateException("press_dies tag missing from the test classpath");
        }
        try {
            return Path.of(marker.toURI()).getParent().getParent().getParent().getParent().getParent();
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    static JsonObject read(Path file) {
        try (Reader reader = Files.newBufferedReader(file)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException e) {
            throw new IllegalStateException(file.toString(), e);
        }
    }

    /** Every recipe of {@code type} (e.g. {@code nerotech:pressing}) under data/nerotech/recipe. */
    static List<View> recipes(String type) {
        List<View> out = new ArrayList<>();
        Path dir = resources().resolve(RecipeFixture.dataPath("data/nerotech/recipe"));
        try (DirectoryStream<Path> files = Files.newDirectoryStream(dir, "*.json")) {
            for (Path file : files) {
                JsonObject json = read(file);
                if (!type.equals(json.get("type").getAsString())) {
                    continue;
                }
                List<String> ingredients = new ArrayList<>();
                List<Integer> counts = new ArrayList<>();
                if (json.has("items")) {
                    for (JsonElement e : json.getAsJsonArray("items")) {
                        JsonObject item = e.getAsJsonObject();
                        ingredients.add(ingredient(item.get("ingredient")));
                        counts.add(item.has("count") ? item.get("count").getAsInt() : 1);
                    }
                }
                String catalyst = json.has("catalyst") ? ingredient(json.get("catalyst")) : null;
                JsonObject first = json.getAsJsonArray("results").get(0).getAsJsonObject();
                out.add(new View(file.getFileName().toString(), ingredients, counts, catalyst,
                        first.get("item").getAsString(), first.has("count") ? first.get("count").getAsInt() : 1,
                        json.has("fluid_in")));
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return out;
    }

    /**
     * An ingredient as a string ({@code #tag} or an item id): the short form the recipes are written in,
     * or the object form ({@code item} / {@code tag}) the 1.21.1 legacy rewrite turns them into.
     */
    static String ingredient(JsonElement element) {
        if (element.isJsonArray()) {
            return ingredient(element.getAsJsonArray().get(0));
        }
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            return object.has("tag") ? "#" + object.get("tag").getAsString() : object.get("item").getAsString();
        }
        return element.getAsString();
    }

    /** Item ids in tag {@code ns:path}, nested tags resolved; optional entries of absent tags are skipped. */
    static Set<String> tag(String id) {
        Set<String> out = new HashSet<>();
        collect(id, out, new HashSet<>());
        return out;
    }

    private static void collect(String id, Set<String> out, Set<String> seen) {
        if (!seen.add(id)) {
            return;
        }
        int colon = id.indexOf(':');
        Path file = resources().resolve(RecipeFixture.dataPath(
                "data/" + id.substring(0, colon) + "/tags/item/" + id.substring(colon + 1) + ".json"));
        if (!Files.isRegularFile(file)) {
            return;
        }
        for (JsonElement e : read(file).getAsJsonArray("values")) {
            String value = e.isJsonObject() ? e.getAsJsonObject().get("id").getAsString() : e.getAsString();
            if (value.startsWith("#")) {
                collect(value.substring(1), out, seen);
            } else {
                out.add(value);
            }
        }
    }

    /** Whether item {@code itemId} satisfies ingredient string {@code ingredient} ({@code #tag} or an id). */
    static boolean accepts(String ingredient, String itemId) {
        return ingredient.startsWith("#") ? tag(ingredient.substring(1)).contains(itemId) : ingredient.equals(itemId);
    }

    /**
     * Whether {@code recipe} matches these input stacks (one item id per stack, {@code count} each) and
     * this catalyst ({@code null} = empty slot) — the item and catalyst rules of {@code ProcessRecipe.matches}.
     */
    static boolean matches(View recipe, List<String> stacks, int count, String catalyst) {
        List<ProcessMatching.Requirement<String>> requirements = new ArrayList<>();
        for (int i = 0; i < recipe.ingredients().size(); i++) {
            String ingredient = recipe.ingredients().get(i);
            Predicate<String> test = stack -> accepts(ingredient, stack);
            requirements.add(new ProcessMatching.Requirement<>(test, recipe.counts().get(i)));
        }
        if (!ProcessMatching.itemsSatisfied(requirements, stacks, s -> count)) {
            return false;
        }
        return recipe.catalyst() == null || (catalyst != null && accepts(recipe.catalyst(), catalyst));
    }

    /** The results of every recipe matching these inputs. */
    static List<String> results(List<View> recipes, List<String> stacks, int count, String catalyst) {
        List<String> out = new ArrayList<>();
        for (View recipe : recipes) {
            if (!recipe.fluidInput() && matches(recipe, stacks, count, catalyst)) {
                out.add(recipe.result());
            }
        }
        return out;
    }
}
