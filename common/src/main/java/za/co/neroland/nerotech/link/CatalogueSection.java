package za.co.neroland.nerotech.link;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerotech.link.ChainCatalogue.Stack;
import za.co.neroland.nerotech.link.ChainCatalogue.Step;
import za.co.neroland.nerotech.link.ChainCatalogue.StepRef;
import za.co.neroland.nerotech.recipe.ColliderRecipe;
import za.co.neroland.nerotech.recipe.MachineRecipe;
import za.co.neroland.nerotech.recipe.ProcessMedium;
import za.co.neroland.nerotech.recipe.ProcessRecipe;

/**
 * The public {@code catalogue} section (link schema 2): machines, materials and the six
 * chains with their standalone and offworld routes. Identical for every requester; {@code playerId}
 * is never read. Built from the loaded recipe manager once per datapack reload and cached, keyed by
 * the server and its recipe-manager instance (a reload builds a new one).
 */
final class CatalogueSection {

    /** What the builder needs to know about the running game. Server-backed in production, faked in tests. */
    interface Lookup {
        boolean blockExists(String id);

        boolean itemExists(String id);

        boolean fluidExists(String id);

        boolean itemHasTag(String itemId, String tagId);

        Optional<Step> recipe(String recipeId);
    }

    private record CacheKey(MinecraftServer server, Object recipeManager) {
    }

    @Nullable
    private static volatile CacheKey cachedKey;
    @Nullable
    private static volatile JsonObject cached;

    private CatalogueSection() {
    }

    /** The section for the running server (a copy of the cached build); a stable empty shell before one exists. */
    static JsonObject snapshot(@Nullable MinecraftServer srv) {
        if (srv == null) {
            return empty();
        }
        Object manager = srv.getRecipeManager();
        CacheKey key = new CacheKey(srv, manager);
        JsonObject built = cached;
        CacheKey builtKey = cachedKey;
        if (built == null || builtKey == null || builtKey.server() != srv || builtKey.recipeManager() != manager) {
            built = build(new ServerLookup(srv));
            cached = built;
            cachedKey = key;
        }
        return built.deepCopy();
    }

    /** Before the server is up: the same shape with nothing in it. */
    static JsonObject empty() {
        JsonObject out = new JsonObject();
        out.addProperty("schema", NeroTechLinkModule.SCHEMA_VERSION);
        out.add("machines", new JsonArray());
        out.add("materials", new JsonArray());
        out.add("chains", new JsonArray());
        return out;
    }

    /** Build the whole section. Deterministic for a given {@link Lookup}: no clock, no player, no order from a hash map. */
    static JsonObject build(Lookup lookup) {
        JsonObject out = new JsonObject();
        out.addProperty("schema", NeroTechLinkModule.SCHEMA_VERSION);

        JsonArray machines = new JsonArray();
        for (ChainCatalogue.Machine m : ChainCatalogue.MACHINES) {
            String id = "nerotech:" + m.id();
            if (!lookup.blockExists(id)) {
                continue;
            }
            JsonObject o = new JsonObject();
            o.addProperty("id", id);
            o.addProperty("family", m.family());
            o.addProperty("descKey", m.descKey());
            machines.add(o);
        }
        out.add("machines", machines);

        JsonArray materials = new JsonArray();
        for (ChainCatalogue.Material m : ChainCatalogue.MATERIALS) {
            String id = "nerotech:" + m.id();
            boolean present = switch (m.kind()) {
                case "item" -> lookup.itemExists(id);
                case "fluid" -> lookup.fluidExists(id);
                default -> true; // gases live in Core's gas registry; the list is fixed
            };
            if (!present) {
                continue;
            }
            JsonObject o = new JsonObject();
            o.addProperty("id", id);
            o.addProperty("kind", m.kind());
            JsonArray tags = new JsonArray();
            if ("item".equals(m.kind())) {
                for (String tag : m.tags()) {
                    if (lookup.itemHasTag(id, tag)) {
                        tags.add(tag);
                    }
                }
            }
            o.add("tags", tags);
            materials.add(o);
        }
        out.add("materials", materials);

        JsonArray chains = new JsonArray();
        for (ChainCatalogue.Chain chain : ChainCatalogue.CHAINS) {
            JsonArray products = new JsonArray();
            for (ChainCatalogue.Product product : chain.products()) {
                JsonArray standalone = route(product.standalone(), lookup);
                JsonArray offworld = route(product.offworld(), lookup);
                if (standalone.isEmpty() && offworld.isEmpty()) {
                    continue;
                }
                JsonObject p = new JsonObject();
                p.addProperty("id", product.id());
                p.add("standalone", standalone);
                p.add("offworld", offworld);
                products.add(p);
            }
            if (products.isEmpty()) {
                continue;
            }
            JsonObject c = new JsonObject();
            c.addProperty("id", chain.id());
            c.addProperty("nameKey", "nerotech.chain." + chain.id());
            c.add("products", products);
            chains.add(c);
        }
        out.add("chains", chains);
        return out;
    }

    private static JsonArray route(List<StepRef> refs, Lookup lookup) {
        JsonArray steps = new JsonArray();
        for (StepRef ref : refs) {
            Step step = resolve(ref, lookup);
            if (step != null) {
                steps.add(stepJson(step));
            }
        }
        return steps;
    }

    @Nullable
    private static Step resolve(StepRef ref, Lookup lookup) {
        if (ref.fixed() != null) {
            Step fixed = ref.fixed();
            if (!machinePresent(fixed.machine(), lookup)) {
                return null;
            }
            for (Stack s : fixed.outputs()) {
                if ("item".equals(s.kind()) && !lookup.itemExists(s.id())) {
                    return null;
                }
            }
            return fixed;
        }
        for (String id : ref.recipeIds()) {
            Optional<Step> step = lookup.recipe(id.contains(":") ? id : "nerotech:" + id);
            if (step.isPresent()) {
                return step.get();
            }
        }
        return null;
    }

    private static boolean machinePresent(String machine, Lookup lookup) {
        return !machine.startsWith("nerotech:") || lookup.blockExists(machine);
    }

    private static JsonObject stepJson(Step step) {
        JsonObject o = new JsonObject();
        o.addProperty("machine", step.machine());
        o.add("inputs", stacks(step.inputs()));
        o.add("outputs", stacks(step.outputs()));
        return o;
    }

    private static JsonArray stacks(List<Stack> stacks) {
        JsonArray out = new JsonArray();
        for (Stack s : stacks) {
            JsonObject o = new JsonObject();
            o.addProperty("id", s.id());
            o.addProperty("kind", s.kind());
            out.add(o);
        }
        return out;
    }

    // --- the server-backed lookup ---------------------------------------------------------

    /** Reads the live registries and recipe manager. Built once per reload, on the server thread. */
    private static final class ServerLookup implements Lookup {

        private final Map<String, RecipeHolder<?>> recipes = new HashMap<>();
        private final DynamicOps<JsonElement> ops;

        ServerLookup(MinecraftServer srv) {
            for (RecipeHolder<?> holder : srv.getRecipeManager().getRecipes()) {
                //? if >=1.21.2 {
                String id = holder.id().identifier().toString();
                //?} else {
                /*String id = holder.id().toString();
                *///?}
                this.recipes.put(id, holder);
            }
            this.ops = RegistryOps.create(JsonOps.INSTANCE, srv.registryAccess());
        }

        @Override
        public boolean blockExists(String id) {
            Identifier key = Identifier.tryParse(id);
            return key != null && BuiltInRegistries.BLOCK.containsKey(key);
        }

        @Override
        public boolean itemExists(String id) {
            Identifier key = Identifier.tryParse(id);
            return key != null && BuiltInRegistries.ITEM.containsKey(key);
        }

        @Override
        public boolean fluidExists(String id) {
            Identifier key = Identifier.tryParse(id);
            return key != null && BuiltInRegistries.FLUID.containsKey(key);
        }

        @Override
        public boolean itemHasTag(String itemId, String tagId) {
            Identifier item = Identifier.tryParse(itemId);
            Identifier tag = Identifier.tryParse(tagId);
            if (item == null || tag == null || !BuiltInRegistries.ITEM.containsKey(item)) {
                return false;
            }
            Item value = BuiltInRegistries.ITEM.getValue(item);
            return new ItemStack(value).is(TagKey.create(Registries.ITEM, tag));
        }

        @Override
        public Optional<Step> recipe(String recipeId) {
            RecipeHolder<?> holder = this.recipes.get(recipeId);
            if (holder == null) {
                return Optional.empty();
            }
            try {
                return Optional.ofNullable(toStep(holder.value()));
            } catch (RuntimeException e) {
                return Optional.empty();
            }
        }

        @Nullable
        private Step toStep(Recipe<?> recipe) {
            Identifier type = BuiltInRegistries.RECIPE_TYPE.getKey(recipe.getType());
            String machine = type == null ? null : ChainCatalogue.machineForRecipeType(type.toString());
            if (machine == null) {
                return null;
            }
            List<Stack> in = new ArrayList<>();
            List<Stack> out = new ArrayList<>();
            if (recipe instanceof ProcessRecipe p) {
                for (ProcessRecipe.CountedIngredient counted : p.items()) {
                    ingredient(counted.ingredient(), in);
                }
                p.catalyst().ifPresent(c -> ingredient(c, in));
                p.fluidIn().ifPresent(m -> in.add(medium("fluid", m)));
                p.gasIn().ifPresent(m -> in.add(medium("gas", m)));
                for (ProcessRecipe.ChanceResult result : p.results()) {
                    Identifier key = BuiltInRegistries.ITEM.getKey(result.item());
                    if (key != null) {
                        out.add(ChainCatalogue.item(key.toString()));
                    }
                }
                p.fluidOut().ifPresent(m -> out.add(medium("fluid", m)));
                p.gasOut().ifPresent(m -> out.add(medium("gas", m)));
            } else if (recipe instanceof ColliderRecipe c) {
                ingredient(c.inputA(), in);
                ingredient(c.inputB(), in);
                //? if >=26.1 {
                ItemStack result = c.result().create();
                //?} else {
                /*ItemStack result = c.result();
                *///?}
                out.add(ChainCatalogue.item(BuiltInRegistries.ITEM.getKey(result.getItem()).toString()));
            } else if (recipe instanceof MachineRecipe m) {
                ingredient(m.input(), in);
                //? if >=26.1 {
                ItemStack result = m.result().create();
                //?} else {
                /*ItemStack result = m.result();
                *///?}
                out.add(ChainCatalogue.item(BuiltInRegistries.ITEM.getKey(result.getItem()).toString()));
            } else {
                return null;
            }
            return new Step(machine, List.copyOf(in), List.copyOf(out));
        }

        private static Stack medium(String kind, ProcessMedium medium) {
            return new Stack(kind, medium.id());
        }

        /** An ingredient as ids: item ids, or {@code #tag} ids, via the vanilla codec (version-neutral). */
        private void ingredient(Ingredient ingredient, List<Stack> out) {
            try {
                Ingredient.CODEC.encodeStart(this.ops, ingredient).result()
                        .ifPresent(json -> collect(json, out));
            } catch (RuntimeException e) {
                // An ingredient the codec cannot write (a loader-custom one) is left out of the step.
            }
        }

        private static void collect(JsonElement json, List<Stack> out) {
            if (json == null || json.isJsonNull()) {
                return;
            }
            if (json.isJsonPrimitive()) {
                out.add(ChainCatalogue.item(json.getAsString()));
            } else if (json.isJsonArray()) {
                for (JsonElement e : json.getAsJsonArray()) {
                    collect(e, out);
                }
            } else if (json.isJsonObject()) {
                JsonObject o = json.getAsJsonObject();
                if (o.has("item")) {
                    out.add(ChainCatalogue.item(o.get("item").getAsString()));
                } else if (o.has("tag")) {
                    out.add(ChainCatalogue.item("#" + o.get("tag").getAsString()));
                }
            }
        }
    }
}
