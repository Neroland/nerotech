package za.co.neroland.nerotech.machine.components;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import za.co.neroland.nerotech.recipe.ProcessMatching;
import za.co.neroland.nerotech.recipe.ProcessMedium;

/**
 * A shipped {@code ProcessRecipe} JSON read without a Minecraft bootstrap, matched with the same
 * {@link ProcessMatching} and {@link ProcessMedium} logic the recipe class uses. Item ingredients are
 * an item id or a {@code #tag}, resolved from the tag files on the test classpath (a tag NeroTech does
 * not ship, such as a loader convention tag, resolves to nothing).
 *
 * <p>Public so the recipe-graph test ({@code recipe.graph.RecipeGraphTest}) reads recipes through the
 * same helper instead of a third copy: {@link #resource} and {@link #references} are its entry points.
 */
public final class RecipeFixture {

    /** One stack: an item id and a count. */
    record Stack(String id, int count) {
    }

    final JsonObject json;

    private RecipeFixture(JsonObject json) {
        this.json = json;
    }

    static RecipeFixture load(String name) {
        return new RecipeFixture(read("data/nerotech/recipe/" + name + ".json").orElseThrow(
                () -> new AssertionError("missing recipe " + name)));
    }

    String type() {
        return this.json.get("type").getAsString();
    }

    List<ProcessMatching.Requirement<Stack>> requirements() {
        List<ProcessMatching.Requirement<Stack>> out = new ArrayList<>();
        for (JsonElement element : this.json.getAsJsonArray("items")) {
            JsonObject entry = element.getAsJsonObject();
            Set<String> ids = resolveIngredient(entry.get("ingredient"));
            out.add(new ProcessMatching.Requirement<>(s -> ids.contains(s.id()), entry.get("count").getAsInt()));
        }
        return out;
    }

    /** Every item id the recipe's {@code items} name (tags expanded). */
    Set<String> inputIds() {
        Set<String> ids = new HashSet<>();
        for (JsonElement element : this.json.getAsJsonArray("items")) {
            ids.addAll(resolveIngredient(element.getAsJsonObject().get("ingredient")));
        }
        return ids;
    }

    Optional<Set<String>> catalyst() {
        return this.json.has("catalyst")
                ? Optional.of(resolveIngredient(this.json.get("catalyst"))) : Optional.empty();
    }

    /** The recipe's full match, as {@code ProcessRecipe.matches} runs it (items, catalyst, fluid in). */
    boolean matches(List<Stack> stacks, String catalystId, String fluidId, long fluidAmount) {
        if (!ProcessMatching.itemsSatisfied(requirements(), stacks, Stack::count)) {
            return false;
        }
        Optional<Set<String>> catalyst = catalyst();
        if (catalyst.isPresent() && (catalystId == null || !catalyst.get().contains(catalystId))) {
            return false;
        }
        if (this.json.has("fluid_in")) {
            JsonObject fluid = this.json.getAsJsonObject("fluid_in");
            ProcessMedium medium = new ProcessMedium(fluid.get("id").getAsString(), fluid.get("amount").getAsInt());
            return fluidId != null && medium.satisfiedBy(fluidId, fluidAmount, 1L,
                    tag -> resolve("#" + tag, "fluid").contains(fluidId));
        }
        return true;
    }

    /**
     * An item ingredient in either form: the 26.x short form ({@code "id"} / {@code "#tag"}) or the
     * object form the 1.21.1 build rewrites it to ({@code {"item": id}} / {@code {"tag": tag}}, or a list).
     */
    static Set<String> resolveIngredient(JsonElement ingredient) {
        Set<String> out = new HashSet<>();
        if (ingredient.isJsonArray()) {
            for (JsonElement each : ingredient.getAsJsonArray()) {
                out.addAll(resolveIngredient(each));
            }
        } else if (ingredient.isJsonObject()) {
            JsonObject object = ingredient.getAsJsonObject();
            if (object.has("item")) {
                out.add(object.get("item").getAsString());
            } else if (object.has("tag")) {
                out.addAll(resolve("#" + object.get("tag").getAsString(), "item"));
            }
        } else {
            out.addAll(resolve(ingredient.getAsString(), "item"));
        }
        return out;
    }

    static Set<String> resolve(String ingredient, String registry) {
        Set<String> out = new HashSet<>();
        resolveInto(ingredient, registry, out, 0);
        return out;
    }

    private static void resolveInto(String ingredient, String registry, Set<String> out, int depth) {
        if (!ingredient.startsWith("#")) {
            out.add(ingredient);
            return;
        }
        if (depth > 8) {
            return;
        }
        String tag = ingredient.substring(1);
        int colon = tag.indexOf(':');
        String path = "data/" + tag.substring(0, colon) + "/tags/" + registry + "/" + tag.substring(colon + 1) + ".json";
        Optional<JsonObject> file = read(path);
        if (file.isEmpty()) {
            return;
        }
        for (JsonElement value : file.get().getAsJsonArray("values")) {
            String id = value.isJsonObject() ? value.getAsJsonObject().get("id").getAsString() : value.getAsString();
            resolveInto(id, registry, out, depth + 1);
        }
    }

    /** A JSON resource on the test classpath, or empty when it is absent. */
    public static Optional<JsonObject> resource(String path) {
        return read(path);
    }

    /**
     * An ingredient's references, unexpanded ({@code "id"} or {@code "#tag"} strings), in either form:
     * the 26.x short form, the 1.21.1 object form ({@code {"item": id}} / {@code {"tag": tag}}), or a list.
     */
    public static List<String> references(JsonElement ingredient) {
        List<String> out = new ArrayList<>();
        if (ingredient.isJsonArray()) {
            for (JsonElement each : ingredient.getAsJsonArray()) {
                out.addAll(references(each));
            }
        } else if (ingredient.isJsonObject()) {
            JsonObject object = ingredient.getAsJsonObject();
            if (object.has("item")) {
                out.add(object.get("item").getAsString());
            } else if (object.has("tag")) {
                out.add("#" + object.get("tag").getAsString());
            }
        } else {
            out.add(ingredient.getAsString());
        }
        return out;
    }

    private static Optional<JsonObject> read(String path) {
        try (InputStream in = RecipeFixture.class.getClassLoader().getResourceAsStream(dataPath(path))) {
            if (in == null) {
                return Optional.empty();
            }
            return Optional.of(JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8))
                    .getAsJsonObject());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static String dataPath(String path) {
        //? if >=1.21 {
        return path;
        //?} else {
        /*return path.replaceFirst("^(data/[^/]+/)(recipe)(?=/|$)", "$1$2s")
                .replaceFirst("^(data/[^/]+/tags/)(block|entity_type|fluid|item)(?=/|$)", "$1$2s");
        *///?}
    }
}
