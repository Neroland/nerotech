package za.co.neroland.nerotech.recipe;

//? if >=1.21.2 {
import java.util.List;
//?}
import java.util.function.Supplier;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
//? if >=26.1 {
import net.minecraft.world.item.ItemStackTemplate;
//?}
import net.minecraft.world.item.crafting.Ingredient;
//? if >=1.21.2 {
import net.minecraft.world.item.crafting.PlacementInfo;
//?}
import net.minecraft.world.item.crafting.Recipe;
//? if >=1.21.2 {
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
//?}
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;

/**
 * One Particle Accelerator collision: TWO ingredients smashed together above a minimum collision
 * energy, yielding one result. The {@code nerotech:collider} type is the only NeroTech recipe type
 * that is not a {@link MachineRecipe} — colliding needs a second input and an energy threshold, so it
 * implements {@link Recipe} directly rather than borrowing vanilla's single-item shape.
 *
 * <pre>{@code
 * {
 *   "type": "nerotech:collider",
 *   "input_a": "minecraft:netherite_scrap",
 *   "input_b": "nerotech:iron_dust",
 *   "min_energy": 3000,
 *   "result": { "id": "nerolandcore:starsteel_dust" }
 * }
 * }</pre>
 *
 * <p>{@code min_energy} is in the same joules the controller's gauge reads
 * ({@code E = 0.5·v²·acceleratorEnergyScale}), so a recipe's energy floor is really a minimum RING
 * SIZE — the loop's shortest bend segment caps its top speed. Inputs are order-free: the circulating
 * particle may match either side. Both are consumed one at a time.
 *
 * <p>Same "minimal in-jar + datapack" posture as the rest of NeroTech: the shipped JSON baseline is
 * the balance surface and any world datapack may override or extend it file-by-file.
 */
public class ColliderRecipe implements Recipe<ColliderRecipeInput> {

    private final Supplier<RecipeType<ColliderRecipe>> type;
    private final Supplier<RecipeSerializer<ColliderRecipe>> serializer;
    //? if >=26.1 {
    private final Recipe.CommonInfo commonInfo;
    //?}
    private final Ingredient inputA;
    private final Ingredient inputB;
    private final int minEnergy;
    //? if >=26.1 {
    private final ItemStackTemplate result;
    //?} else {
    /*private final ItemStack result;
    *///?}

    //? if >=1.21.2 {
    @Nullable
    private PlacementInfo placementInfo;
    //?}

    //? if >=26.1 {
    private ColliderRecipe(Supplier<RecipeType<ColliderRecipe>> type,
            Supplier<RecipeSerializer<ColliderRecipe>> serializer, Recipe.CommonInfo commonInfo,
            Ingredient inputA, Ingredient inputB, int minEnergy, ItemStackTemplate result) {
        this.type = type;
        this.serializer = serializer;
        this.commonInfo = commonInfo;
    //?} else {
    /*private ColliderRecipe(Supplier<RecipeType<ColliderRecipe>> type,
            Supplier<RecipeSerializer<ColliderRecipe>> serializer,
            Ingredient inputA, Ingredient inputB, int minEnergy, ItemStack result) {
        this.type = type;
        this.serializer = serializer;
    *///?}
        this.inputA = inputA;
        this.inputB = inputB;
        this.minEnergy = minEnergy;
        this.result = result;
    }

    /** The first collision partner. */
    public Ingredient inputA() {
        return this.inputA;
    }

    /** The second collision partner. */
    public Ingredient inputB() {
        return this.inputB;
    }

    /** Minimum collision energy in joules; below it the particles simply miss and keep circulating. */
    public int minEnergy() {
        return this.minEnergy;
    }

    /** The product (read-only consumer surface for the JEI page). */
    //? if >=26.1 {
    public ItemStackTemplate result() {
    //?} else {
    /*public ItemStack result() {
    *///?}
        return this.result;
    }

    /** Order-free: the circulating particle may match either ingredient. */
    @Override
    public boolean matches(ColliderRecipeInput input, Level level) {
        ItemStack circulating = input.circulating();
        ItemStack target = input.target();
        return (this.inputA.test(circulating) && this.inputB.test(target))
                || (this.inputA.test(target) && this.inputB.test(circulating));
    }

    //? if >=26.1 {
    @Override
    public ItemStack assemble(ColliderRecipeInput input) {
        return this.result.create();
    }

    @Override
    public boolean showNotification() {
        return this.commonInfo.showNotification();
    }
    //?} else {
    /*@Override
    public ItemStack assemble(ColliderRecipeInput input, net.minecraft.core.HolderLookup.Provider registries) {
        return this.result.copy();
    }
    *///?}

    //? if >=1.21.2 {
    @Override
    public String group() {
        return "";
    }

    @Override
    public PlacementInfo placementInfo() {
        if (this.placementInfo == null) {
            this.placementInfo = PlacementInfo.create(List.of(this.inputA, this.inputB));
        }
        return this.placementInfo;
    }

    //?} else {
    /*@Override
    public String getGroup() {
        return "";
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(net.minecraft.core.HolderLookup.Provider registries) {
        return this.result;
    }

    @Override
    public net.minecraft.core.NonNullList<Ingredient> getIngredients() {
        return net.minecraft.core.NonNullList.of(Ingredient.EMPTY, this.inputA, this.inputB);
    }
    *///?}

    @Override
    public RecipeSerializer<ColliderRecipe> getSerializer() {
        return this.serializer.get();
    }

    @Override
    public RecipeType<ColliderRecipe> getType() {
        return this.type.get();
    }

    //? if >=1.21.2 {
    @Override
    public RecipeBookCategory recipeBookCategory() {
        // Collider recipes never appear in a vanilla recipe book; any registered category satisfies
        // the interface without adding a custom recipe-book tab (same choice as MachineRecipe).
        return RecipeBookCategories.CRAFTING_MISC;
    }
    //?}

    /**
     * The JSON codec, bound to one registered type + serializer pair (suppliers, so registration
     * order does not matter). Mirrors {@code SingleItemRecipe.simpleMapCodec} with the extra
     * second ingredient and the energy floor.
     */
    public static MapCodec<ColliderRecipe> mapCodec(Supplier<RecipeType<ColliderRecipe>> type,
            Supplier<RecipeSerializer<ColliderRecipe>> serializer) {
        //? if >=26.1 {
        return RecordCodecBuilder.mapCodec(instance -> instance.group(
                        Recipe.CommonInfo.MAP_CODEC.forGetter(recipe -> recipe.commonInfo),
                        Ingredient.CODEC.fieldOf("input_a").forGetter(ColliderRecipe::inputA),
                        Ingredient.CODEC.fieldOf("input_b").forGetter(ColliderRecipe::inputB),
                        Codec.INT.optionalFieldOf("min_energy", 0).forGetter(ColliderRecipe::minEnergy),
                        ItemStackTemplate.CODEC.fieldOf("result").forGetter(ColliderRecipe::result))
                .apply(instance, (commonInfo, inputA, inputB, minEnergy, result) ->
                        new ColliderRecipe(type, serializer, commonInfo, inputA, inputB, minEnergy, result)));
        //?} else {
        /*return RecordCodecBuilder.mapCodec(instance -> instance.group(
                        Ingredient.CODEC.fieldOf("input_a").forGetter(ColliderRecipe::inputA),
                        Ingredient.CODEC.fieldOf("input_b").forGetter(ColliderRecipe::inputB),
                        Codec.INT.optionalFieldOf("min_energy", 0).forGetter(ColliderRecipe::minEnergy),
                        ItemStack.STRICT_CODEC.fieldOf("result").forGetter(ColliderRecipe::result))
                .apply(instance, (inputA, inputB, minEnergy, result) ->
                        new ColliderRecipe(type, serializer, inputA, inputB, minEnergy, result)));
        *///?}
    }

    /** The network codec (the opt-in recipe sync that feeds recipe viewers). */
    public static StreamCodec<RegistryFriendlyByteBuf, ColliderRecipe> streamCodec(
            Supplier<RecipeType<ColliderRecipe>> type,
            Supplier<RecipeSerializer<ColliderRecipe>> serializer) {
        //? if >=26.1 {
        return StreamCodec.composite(
                Recipe.CommonInfo.STREAM_CODEC, recipe -> recipe.commonInfo,
                Ingredient.CONTENTS_STREAM_CODEC, ColliderRecipe::inputA,
                Ingredient.CONTENTS_STREAM_CODEC, ColliderRecipe::inputB,
                ByteBufCodecs.VAR_INT, ColliderRecipe::minEnergy,
                ItemStackTemplate.STREAM_CODEC, ColliderRecipe::result,
                (commonInfo, inputA, inputB, minEnergy, result) ->
                        new ColliderRecipe(type, serializer, commonInfo, inputA, inputB, minEnergy, result));
        //?} else {
        /*return StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC, ColliderRecipe::inputA,
                Ingredient.CONTENTS_STREAM_CODEC, ColliderRecipe::inputB,
                ByteBufCodecs.VAR_INT, ColliderRecipe::minEnergy,
                ItemStack.STREAM_CODEC, ColliderRecipe::result,
                (inputA, inputB, minEnergy, result) ->
                        new ColliderRecipe(type, serializer, inputA, inputB, minEnergy, result));
        *///?}
    }
}
