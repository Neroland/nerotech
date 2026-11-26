package za.co.neroland.nerotech.recipe;

import java.util.function.Supplier;

//? if >=26.1 {
import net.minecraft.world.item.ItemStackTemplate;
//?} else {
/*import net.minecraft.world.item.ItemStack;
*///?}
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
//? if >=1.21.2 {
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
//?}
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleItemRecipe;

/**
 * One datapack-driven machine processing recipe: single {@link Ingredient} in, one result out —
 * the Stage C swap (2026-07-10) that replaced the in-code lookup tables
 * ({@code OreProcessorRecipes}/{@code FabricatorRecipes}/{@code AdvancedFabricatorRecipes}).
 * Built on vanilla's {@link SingleItemRecipe} (the stonecutter shape), so JSON reads:
 *
 * <pre>{@code
 * {
 *   "type": "nerotech:ore_processing",
 *   "ingredient": ["minecraft:iron_ore", "minecraft:deepslate_iron_ore", "minecraft:raw_iron"],
 *   "result": { "id": "nerotech:iron_dust", "count": 2 }
 * }
 * }</pre>
 *
 * <p>One class serves all three machine recipe types ({@code ore_processing}, {@code fabricating},
 * {@code advanced_fabricating} — see {@code registry.ModRecipeTypes}); each serializer's factory
 * binds the instances it decodes to its own type. This is the "minimal in-jar + datapack" recipe
 * posture: the shipped JSON baseline is the balance surface, and any world datapack can override a
 * recipe file-by-file or add new ones — no code change, no jar rebuild.
 */
public class MachineRecipe extends SingleItemRecipe {

    private final Supplier<RecipeType<MachineRecipe>> type;
    private final Supplier<RecipeSerializer<MachineRecipe>> serializer;

    //? if >=26.1 {
    private MachineRecipe(Supplier<RecipeType<MachineRecipe>> type,
            Supplier<RecipeSerializer<MachineRecipe>> serializer,
            Recipe.CommonInfo commonInfo, Ingredient ingredient, ItemStackTemplate result) {
        super(commonInfo, ingredient, result);
        this.type = type;
        this.serializer = serializer;
    }

    /** A factory binding decoded recipes to one registered type + serializer pair. */
    public static SingleItemRecipe.Factory<MachineRecipe> factory(
            Supplier<RecipeType<MachineRecipe>> type,
            Supplier<RecipeSerializer<MachineRecipe>> serializer) {
        return (commonInfo, ingredient, result) -> new MachineRecipe(type, serializer, commonInfo, ingredient, result);
    }

    /** Widened to public for read-only consumers (JEI category display). */
    @Override
    public ItemStackTemplate result() {
        return super.result();
    }
    //?} else if >=1.20.5 {
    /*// Before 26.1 a single-item recipe is (group, ingredient, result stack); 1.21.1 also passes the
    // type and serializer to the superclass, which this class overrides anyway.
    private MachineRecipe(Supplier<RecipeType<MachineRecipe>> type,
            Supplier<RecipeSerializer<MachineRecipe>> serializer,
            String group, Ingredient ingredient, ItemStack result) {
        *///?}
    //? if >=1.21.2 && <26.1 {
        /*super(group, ingredient, result);
        *///?}
    //? if >=1.20.5 && <1.21.2 {
        /*super(null, null, group, ingredient, result);
        *///?}
    //? if >=1.20.5 && <26.1 {
        /*this.type = type;
        this.serializer = serializer;
    }

    // A factory binding decoded recipes to one registered type + serializer pair.
    public static SingleItemRecipe.Factory<MachineRecipe> factory(
            Supplier<RecipeType<MachineRecipe>> type,
            Supplier<RecipeSerializer<MachineRecipe>> serializer) {
        return (group, ingredient, result) -> new MachineRecipe(type, serializer, group, ingredient, result);
    }
    *///?}
    //? if <1.20.5 {
    /*private MachineRecipe(Supplier<RecipeType<MachineRecipe>> type,
            Supplier<RecipeSerializer<MachineRecipe>> serializer,
            net.minecraft.resources.ResourceLocation id, String group, Ingredient ingredient, ItemStack result) {
        //? if <1.20.2 {
        super(type.get(), serializer.get(), id, group, ingredient, result);
        //?} else {
        super(type.get(), serializer.get(), group, ingredient, result);
        //?}
        this.type = type;
        this.serializer = serializer;
    }

    public static RecipeSerializer<MachineRecipe> legacySerializer(Supplier<RecipeType<MachineRecipe>> type,
            Supplier<RecipeSerializer<MachineRecipe>> serializer) {
        //? if >=1.20.2 {
        var codec = com.mojang.serialization.codecs.RecordCodecBuilder.<MachineRecipe>create(instance -> instance.group(
                com.mojang.serialization.Codec.STRING.optionalFieldOf("group", "").forGetter(MachineRecipe::getGroup),
                LegacyRecipeData.INGREDIENT.fieldOf("ingredient").forGetter(MachineRecipe::input),
                LegacyRecipeData.RESULT.fieldOf("result").forGetter(MachineRecipe::result)
        ).apply(instance, (group, ingredient, result) -> new MachineRecipe(type, serializer, null, group, ingredient, result)));
        var wire = za.co.neroland.nerolandcore.network.legacy.LegacyStreamCodec
                .<net.minecraft.network.FriendlyByteBuf, MachineRecipe>of(MachineRecipe::encodeLegacy,
                        buffer -> new MachineRecipe(type, serializer, null, buffer.readUtf(),
                                Ingredient.fromNetwork(buffer), buffer.readItem()));
        return new LegacyRecipeSerializer<>(codec, wire, null);
        //?} else {
        return new RecipeSerializer<>() {
            @Override
            public MachineRecipe fromJson(net.minecraft.resources.ResourceLocation id, com.google.gson.JsonObject data) {
                var ingredient = Ingredient.fromJson(data.get("ingredient"));
                var result = net.minecraft.world.item.crafting.ShapedRecipe.itemStackFromJson(
                        net.minecraft.util.GsonHelper.getAsJsonObject(data, "result"));
                return new MachineRecipe(type, serializer, id,
                        net.minecraft.util.GsonHelper.getAsString(data, "group", ""), ingredient, result);
            }

            @Override
            public MachineRecipe fromNetwork(net.minecraft.resources.ResourceLocation id, net.minecraft.network.FriendlyByteBuf buffer) {
                return new MachineRecipe(type, serializer, id, buffer.readUtf(), Ingredient.fromNetwork(buffer),
                        buffer.readItem());
            }

            @Override
            public void toNetwork(net.minecraft.network.FriendlyByteBuf buffer, MachineRecipe recipe) {
                encodeLegacy(buffer, recipe);
            }
        };
        //?}
    }

    private static void encodeLegacy(net.minecraft.network.FriendlyByteBuf buffer, MachineRecipe recipe) {
        buffer.writeUtf(recipe.getGroup());
        recipe.input().toNetwork(buffer);
        buffer.writeItem(recipe.result());
    }
    *///?}

    //? if >=1.21.2 && <26.1 {
    /*// Widened to public for read-only consumers.
    @Override
    public ItemStack result() {
        return super.result();
    }
    *///?}
    //? if <1.21.2 {
    /*// The recipe's result stack (1.21.1 keeps it in a protected field).
    public ItemStack result() {
        return this.result;
    }

    // The recipe's ingredient (1.21.1 keeps it in a protected field; later lines have input()).
    public Ingredient input() {
        return this.ingredient;
    }
    *///?}

    //? if >=1.21 && <1.21.2 {
    /*@Override
    public boolean matches(net.minecraft.world.item.crafting.SingleRecipeInput input,
            net.minecraft.world.level.Level level) {
        return this.ingredient.test(input.item());
    }
    *///?} else if <1.21 {
    /*@Override
    public boolean matches(net.minecraft.world.Container input, net.minecraft.world.level.Level level) {
        return this.ingredient.test(input.getItem(0));
    }
    *///?}

    @Override
    public RecipeType<MachineRecipe> getType() {
        return this.type.get();
    }

    @Override
    public RecipeSerializer<MachineRecipe> getSerializer() {
        return this.serializer.get();
    }

    //? if >=1.21.2 {
    @Override
    public String group() {
        return "";
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        // Machine recipes never appear in a vanilla recipe book; any registered category satisfies
        // the interface without adding a custom recipe-book tab.
        return RecipeBookCategories.CRAFTING_MISC;
    }
    //?} else {
    /*@Override
    public String getGroup() {
        return "";
    }
    *///?}
}
