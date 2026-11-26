package za.co.neroland.nerotech.recipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
//? if >=1.20.5 {
import net.minecraft.network.RegistryFriendlyByteBuf;
//?}
//? if >=1.20.5 {
import net.minecraft.network.codec.ByteBufCodecs;
//?}
//? if >=1.20.5 {
import net.minecraft.network.codec.StreamCodec;
//?}
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerotech.gas.NeroTechGases;

/**
 * One multi-input / multi-output process step — the single recipe class behind every 0.8.0 machine.
 * Eleven recipe types share it and one serializer factory; the type says which machine
 * runs it.
 *
 * <pre>{@code
 * {
 *   "type": "nerotech:reacting",
 *   "items": [ { "ingredient": "#c:dusts/sulfur", "count": 2 } ],
 *   "fluid_in": { "id": "minecraft:water", "amount": 500 },
 *   "gas_in": { "id": "nerospace:oxygen", "amount": 2 },
 *   "catalyst": "nerotech:quartz_focus_lens",
 *   "results": [ { "item": "nerotech:polymer_sheet", "count": 1, "chance_permille": 250 } ],
 *   "fluid_out": { "id": "nerotech:sulfuric_acid", "amount": 250 },
 *   "gas_out": { "id": "nerotech:nitrogen", "amount": 1 },
 *   "energy": 80,
 *   "time": 200,
 *   "min_heat": 0,
 *   "atmosphere": "EARTHLIKE"
 * }
 * }</pre>
 *
 * <p>{@code items} holds 0–6 counted ingredients (consumed); {@code catalyst} is checked but never
 * consumed; {@code results} holds 0–4 outputs, each rolled against {@code chance_permille} (default
 * 1000). Fluids are in mB and may name a {@code #tag}; gases are in units of
 * {@link NeroTechGases#UNIT_MB} mB. {@code energy} is FE per tick, {@code time} ticks. JSON field names
 * are frozen at release.
 */
public class ProcessRecipe implements Recipe<ProcessRecipeInput> {

    //? if <1.20.2 {
    /*private Identifier legacyId;

    @Override
    public Identifier getId() {
        return this.legacyId;
    }

    public void bindRecipeId(Identifier id) {
        this.legacyId = java.util.Objects.requireNonNull(id, "recipe id");
    }
    *///?}


    /** Most counted item inputs a recipe may list. */
    public static final int MAX_ITEMS = 6;
    /** Most item results a recipe may list. */
    public static final int MAX_RESULTS = 4;

    /** One counted, consumed item input. */
    public record CountedIngredient(Ingredient ingredient, int count) {

        public static final Codec<CountedIngredient> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Ingredient.CODEC.fieldOf("ingredient").forGetter(CountedIngredient::ingredient),
                Codec.intRange(1, 64).optionalFieldOf("count", 1).forGetter(CountedIngredient::count)
        ).apply(inst, CountedIngredient::new));

        //? if >=1.20.5 {
        public static final StreamCodec<RegistryFriendlyByteBuf, CountedIngredient> STREAM_CODEC =
                StreamCodec.composite(
                        Ingredient.CONTENTS_STREAM_CODEC, CountedIngredient::ingredient,
                        ByteBufCodecs.VAR_INT, CountedIngredient::count,
                        CountedIngredient::new);
        //?}
    }

    /** One item output, produced with probability {@code chancePermille}/1000. */
    public record ChanceResult(Item item, int count, int chancePermille) {

        public static final Codec<ChanceResult> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(ChanceResult::item),
                Codec.intRange(1, 64).optionalFieldOf("count", 1).forGetter(ChanceResult::count),
                Codec.intRange(1, 1000).optionalFieldOf("chance_permille", 1000).forGetter(ChanceResult::chancePermille)
        ).apply(inst, ChanceResult::new));

        //? if >=1.20.5 {
        public static final StreamCodec<RegistryFriendlyByteBuf, ChanceResult> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.registry(Registries.ITEM), ChanceResult::item,
                        ByteBufCodecs.VAR_INT, ChanceResult::count,
                        ByteBufCodecs.VAR_INT, ChanceResult::chancePermille,
                        ChanceResult::new);
        //?}

        public ItemStack create() {
            return new ItemStack(this.item, this.count);
        }

        public boolean guaranteed() {
            return this.chancePermille >= 1000;
        }
    }

    private final Supplier<RecipeType<ProcessRecipe>> type;
    private final Supplier<RecipeSerializer<ProcessRecipe>> serializer;
    //? if >=26.1 {
    private final Recipe.CommonInfo commonInfo;
    //?}
    private final List<CountedIngredient> items;
    private final Optional<ProcessMedium> fluidIn;
    private final Optional<ProcessMedium> gasIn;
    private final Optional<Ingredient> catalyst;
    private final List<ChanceResult> results;
    private final Optional<ProcessMedium> fluidOut;
    private final Optional<ProcessMedium> gasOut;
    private final int energy;
    private final int time;
    private final int minHeat;
    private final Optional<String> atmosphere;

    //? if >=1.21.2 {
    @Nullable
    private PlacementInfo placementInfo;
    //?}

    //? if >=26.1 {
    public ProcessRecipe(Supplier<RecipeType<ProcessRecipe>> type, Supplier<RecipeSerializer<ProcessRecipe>> serializer,
            Recipe.CommonInfo commonInfo, List<CountedIngredient> items, Optional<ProcessMedium> fluidIn,
            Optional<ProcessMedium> gasIn, Optional<Ingredient> catalyst, List<ChanceResult> results,
            Optional<ProcessMedium> fluidOut, Optional<ProcessMedium> gasOut, int energy, int time, int minHeat,
            Optional<String> atmosphere) {
        this.commonInfo = commonInfo;
    //?} else {
    /*public ProcessRecipe(Supplier<RecipeType<ProcessRecipe>> type, Supplier<RecipeSerializer<ProcessRecipe>> serializer,
            List<CountedIngredient> items, Optional<ProcessMedium> fluidIn,
            Optional<ProcessMedium> gasIn, Optional<Ingredient> catalyst, List<ChanceResult> results,
            Optional<ProcessMedium> fluidOut, Optional<ProcessMedium> gasOut, int energy, int time, int minHeat,
            Optional<String> atmosphere) {
    *///?}
        this.type = type;
        this.serializer = serializer;
        this.items = List.copyOf(items);
        this.fluidIn = fluidIn;
        this.gasIn = gasIn;
        this.catalyst = catalyst;
        this.results = List.copyOf(results);
        this.fluidOut = fluidOut;
        this.gasOut = gasOut;
        this.energy = energy;
        this.time = time;
        this.minHeat = minHeat;
        this.atmosphere = atmosphere;
    }

    public List<CountedIngredient> items() {
        return this.items;
    }

    public Optional<ProcessMedium> fluidIn() {
        return this.fluidIn;
    }

    public Optional<ProcessMedium> gasIn() {
        return this.gasIn;
    }

    public Optional<Ingredient> catalyst() {
        return this.catalyst;
    }

    public List<ChanceResult> results() {
        return this.results;
    }

    public Optional<ProcessMedium> fluidOut() {
        return this.fluidOut;
    }

    public Optional<ProcessMedium> gasOut() {
        return this.gasOut;
    }

    /** FE per tick while running. */
    public int energy() {
        return this.energy;
    }

    /** Ticks per operation. */
    public int time() {
        return this.time;
    }

    /** Minimum machine heat to run (0 for none). */
    public int minHeat() {
        return this.minHeat;
    }

    /** Atmosphere this recipe needs (condenser only); empty for any. */
    public Optional<String> atmosphere() {
        return this.atmosphere;
    }

    // --- matching ----------------------------------------------------------------------------------

    @Override
    public boolean matches(ProcessRecipeInput input, Level level) {
        List<ProcessMatching.Requirement<ItemStack>> requirements = new ArrayList<>(this.items.size());
        for (CountedIngredient counted : this.items) {
            requirements.add(new ProcessMatching.Requirement<>(counted.ingredient()::test, counted.count()));
        }
        if (!ProcessMatching.itemsSatisfied(requirements, input.items(), ItemStack::getCount)) {
            return false;
        }
        if (this.catalyst.isPresent() && !this.catalyst.get().test(input.catalyst())) {
            return false;
        }
        if (this.fluidIn.isPresent() && !fluidSatisfied(this.fluidIn.get(), input.fluid(), input.fluidAmount())) {
            return false;
        }
        if (this.gasIn.isPresent()) {
            Identifier gas = NeroTechGases.canonical(input.gas());
            if (gas == null || !this.gasIn.get().satisfiedBy(gas.toString(), input.gasAmount(),
                    NeroTechGases.UNIT_MB, tag -> false)) {
                return false;
            }
        }
        if (input.heat() < this.minHeat) {
            return false;
        }
        return this.atmosphere.isEmpty() || this.atmosphere.get().equalsIgnoreCase(input.atmosphere());
    }

    private static boolean fluidSatisfied(ProcessMedium required, Fluid fluid, long amount) {
        if (fluid == null || fluid == Fluids.EMPTY) {
            return false;
        }
        Fluid source = fluid instanceof FlowingFluid flowing ? flowing.getSource() : fluid;
        String id = BuiltInRegistries.FLUID.getKey(source).toString();
        return required.satisfiedBy(id, amount, 1L, tag -> {
            Identifier tagId = Identifier.tryParse(tag);
            return tagId != null && source.is(TagKey.create(Registries.FLUID, tagId));
        });
    }

    // --- Recipe plumbing (version-split exactly like ColliderRecipe) -------------------------------

    /** The first result as a stack (chances are rolled by the machine, not here). */
    private ItemStack firstResult() {
        return this.results.isEmpty() ? ItemStack.EMPTY : this.results.get(0).create();
    }

    private List<Ingredient> allIngredients() {
        List<Ingredient> out = new ArrayList<>(this.items.size() + 1);
        for (CountedIngredient counted : this.items) {
            out.add(counted.ingredient());
        }
        this.catalyst.ifPresent(out::add);
        return out;
    }

    //? if >=26.1 {
    @Override
    public ItemStack assemble(ProcessRecipeInput input) {
        return firstResult();
    }

    @Override
    public boolean showNotification() {
        return this.commonInfo.showNotification();
    }
    //?} else {
    /*@Override
    public ItemStack assemble(ProcessRecipeInput input, net.minecraft.core.HolderLookup.Provider registries) {
        return firstResult();
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
            List<Ingredient> ingredients = allIngredients();
            this.placementInfo = ingredients.isEmpty() ? PlacementInfo.NOT_PLACEABLE : PlacementInfo.create(ingredients);
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
        return firstResult();
    }

    @Override
    public net.minecraft.core.NonNullList<Ingredient> getIngredients() {
        net.minecraft.core.NonNullList<Ingredient> list = net.minecraft.core.NonNullList.create();
        list.addAll(allIngredients());
        return list;
    }
    *///?}

    @Override
    public RecipeSerializer<ProcessRecipe> getSerializer() {
        return this.serializer.get();
    }

    @Override
    public RecipeType<ProcessRecipe> getType() {
        return this.type.get();
    }

    //? if >=1.21.2 {
    @Override
    public RecipeBookCategory recipeBookCategory() {
        // Process recipes never appear in a vanilla recipe book (same choice as MachineRecipe).
        return RecipeBookCategories.CRAFTING_MISC;
    }
    //?}

    // --- codecs ------------------------------------------------------------------------------------

    /** The JSON codec, bound to one registered type + serializer pair. */
    public static MapCodec<ProcessRecipe> mapCodec(Supplier<RecipeType<ProcessRecipe>> type,
            Supplier<RecipeSerializer<ProcessRecipe>> serializer) {
        //? if >=26.1 {
        return RecordCodecBuilder.mapCodec(instance -> instance.group(
                        Recipe.CommonInfo.MAP_CODEC.forGetter(recipe -> recipe.commonInfo),
                        CountedIngredient.CODEC.listOf(0, MAX_ITEMS).optionalFieldOf("items", List.of())
                                .forGetter(ProcessRecipe::items),
                        ProcessMedium.CODEC.optionalFieldOf("fluid_in").forGetter(ProcessRecipe::fluidIn),
                        ProcessMedium.CODEC.optionalFieldOf("gas_in").forGetter(ProcessRecipe::gasIn),
                        Ingredient.CODEC.optionalFieldOf("catalyst").forGetter(ProcessRecipe::catalyst),
                        ChanceResult.CODEC.listOf(0, MAX_RESULTS).optionalFieldOf("results", List.of())
                                .forGetter(ProcessRecipe::results),
                        ProcessMedium.CODEC.optionalFieldOf("fluid_out").forGetter(ProcessRecipe::fluidOut),
                        ProcessMedium.CODEC.optionalFieldOf("gas_out").forGetter(ProcessRecipe::gasOut),
                        Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("energy", 0).forGetter(ProcessRecipe::energy),
                        Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("time", 200).forGetter(ProcessRecipe::time),
                        Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("min_heat", 0).forGetter(ProcessRecipe::minHeat),
                        Codec.STRING.optionalFieldOf("atmosphere").forGetter(ProcessRecipe::atmosphere))
                .apply(instance, (commonInfo, items, fluidIn, gasIn, catalyst, results, fluidOut, gasOut, energy,
                        time, minHeat, atmosphere) -> new ProcessRecipe(type, serializer, commonInfo, items, fluidIn,
                        gasIn, catalyst, results, fluidOut, gasOut, energy, time, minHeat, atmosphere)));
        //?} else {
        /*return RecordCodecBuilder.mapCodec(instance -> instance.group(
                        CountedIngredient.CODEC.listOf(0, MAX_ITEMS).optionalFieldOf("items", List.of())
                                .forGetter(ProcessRecipe::items),
                        ProcessMedium.CODEC.optionalFieldOf("fluid_in").forGetter(ProcessRecipe::fluidIn),
                        ProcessMedium.CODEC.optionalFieldOf("gas_in").forGetter(ProcessRecipe::gasIn),
                        Ingredient.CODEC.optionalFieldOf("catalyst").forGetter(ProcessRecipe::catalyst),
                        ChanceResult.CODEC.listOf(0, MAX_RESULTS).optionalFieldOf("results", List.of())
                                .forGetter(ProcessRecipe::results),
                        ProcessMedium.CODEC.optionalFieldOf("fluid_out").forGetter(ProcessRecipe::fluidOut),
                        ProcessMedium.CODEC.optionalFieldOf("gas_out").forGetter(ProcessRecipe::gasOut),
                        Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("energy", 0).forGetter(ProcessRecipe::energy),
                        Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("time", 200).forGetter(ProcessRecipe::time),
                        Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("min_heat", 0).forGetter(ProcessRecipe::minHeat),
                        Codec.STRING.optionalFieldOf("atmosphere").forGetter(ProcessRecipe::atmosphere))
                .apply(instance, (items, fluidIn, gasIn, catalyst, results, fluidOut, gasOut, energy,
                        time, minHeat, atmosphere) -> new ProcessRecipe(type, serializer, items, fluidIn,
                        gasIn, catalyst, results, fluidOut, gasOut, energy, time, minHeat, atmosphere)));
        *///?}
    }

    //? if >=1.20.5 {
    /** The network codec (the opt-in recipe sync that feeds recipe viewers). Written by hand: too many fields for composite. */
    public static StreamCodec<RegistryFriendlyByteBuf, ProcessRecipe> streamCodec(
            Supplier<RecipeType<ProcessRecipe>> type, Supplier<RecipeSerializer<ProcessRecipe>> serializer) {
        StreamCodec<RegistryFriendlyByteBuf, List<CountedIngredient>> itemsCodec =
                CountedIngredient.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_ITEMS));
        StreamCodec<RegistryFriendlyByteBuf, List<ChanceResult>> resultsCodec =
                ChanceResult.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_RESULTS));
        StreamCodec<RegistryFriendlyByteBuf, Optional<ProcessMedium>> mediumCodec =
                ByteBufCodecs.optional(ProcessMedium.STREAM_CODEC).cast();
        StreamCodec<RegistryFriendlyByteBuf, Optional<Ingredient>> catalystCodec =
                ByteBufCodecs.optional(Ingredient.CONTENTS_STREAM_CODEC);
        StreamCodec<RegistryFriendlyByteBuf, Optional<String>> atmosphereCodec =
                ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8).cast();
        return StreamCodec.of((buf, recipe) -> {
            //? if >=26.1 {
            Recipe.CommonInfo.STREAM_CODEC.encode(buf, recipe.commonInfo);
            //?}
            itemsCodec.encode(buf, recipe.items);
            mediumCodec.encode(buf, recipe.fluidIn);
            mediumCodec.encode(buf, recipe.gasIn);
            catalystCodec.encode(buf, recipe.catalyst);
            resultsCodec.encode(buf, recipe.results);
            mediumCodec.encode(buf, recipe.fluidOut);
            mediumCodec.encode(buf, recipe.gasOut);
            buf.writeVarInt(recipe.energy);
            buf.writeVarInt(recipe.time);
            buf.writeVarInt(recipe.minHeat);
            atmosphereCodec.encode(buf, recipe.atmosphere);
        }, buf -> {
            //? if >=26.1 {
            Recipe.CommonInfo commonInfo = Recipe.CommonInfo.STREAM_CODEC.decode(buf);
            //?}
            List<CountedIngredient> items = itemsCodec.decode(buf);
            Optional<ProcessMedium> fluidIn = mediumCodec.decode(buf);
            Optional<ProcessMedium> gasIn = mediumCodec.decode(buf);
            Optional<Ingredient> catalyst = catalystCodec.decode(buf);
            List<ChanceResult> results = resultsCodec.decode(buf);
            Optional<ProcessMedium> fluidOut = mediumCodec.decode(buf);
            Optional<ProcessMedium> gasOut = mediumCodec.decode(buf);
            int energy = buf.readVarInt();
            int time = buf.readVarInt();
            int minHeat = buf.readVarInt();
            Optional<String> atmosphere = atmosphereCodec.decode(buf);
            //? if >=26.1 {
            return new ProcessRecipe(type, serializer, commonInfo, items, fluidIn, gasIn, catalyst, results,
                    fluidOut, gasOut, energy, time, minHeat, atmosphere);
            //?} else {
            /*return new ProcessRecipe(type, serializer, items, fluidIn, gasIn, catalyst, results,
                    fluidOut, gasOut, energy, time, minHeat, atmosphere);
            *///?}
        });
    }
    //?}
    //? if <1.20.5 {
    /*public static za.co.neroland.nerolandcore.network.legacy.LegacyStreamCodec<net.minecraft.network.FriendlyByteBuf, ProcessRecipe>
            legacyStreamCodec(Supplier<RecipeType<ProcessRecipe>> type, Supplier<RecipeSerializer<ProcessRecipe>> serializer) {
        return za.co.neroland.nerolandcore.network.legacy.LegacyStreamCodec.of((buffer, recipe) -> {
            buffer.writeVarInt(recipe.items.size());
            for (CountedIngredient input : recipe.items) {
                input.ingredient().toNetwork(buffer);
                buffer.writeVarInt(input.count());
            }
            writeMedium(buffer, recipe.fluidIn);
            writeMedium(buffer, recipe.gasIn);
            buffer.writeOptional(recipe.catalyst, (buf, ingredient) -> ingredient.toNetwork(buf));
            buffer.writeVarInt(recipe.results.size());
            for (ChanceResult result : recipe.results) {
                buffer.writeId(BuiltInRegistries.ITEM, result.item());
                buffer.writeVarInt(result.count());
                buffer.writeVarInt(result.chancePermille());
            }
            writeMedium(buffer, recipe.fluidOut);
            writeMedium(buffer, recipe.gasOut);
            buffer.writeVarInt(recipe.energy);
            buffer.writeVarInt(recipe.time);
            buffer.writeVarInt(recipe.minHeat);
            buffer.writeOptional(recipe.atmosphere, (buf, atmosphere) -> buf.writeUtf(atmosphere));
        }, buffer -> {
            int itemCount = LegacyRecipeData.size(buffer, MAX_ITEMS);
            List<CountedIngredient> items = new ArrayList<>(itemCount);
            for (int i = 0; i < itemCount; i++) {
                items.add(new CountedIngredient(Ingredient.fromNetwork(buffer), buffer.readVarInt()));
            }
            Optional<ProcessMedium> fluidIn = readMedium(buffer);
            Optional<ProcessMedium> gasIn = readMedium(buffer);
            Optional<Ingredient> catalyst = buffer.readOptional(Ingredient::fromNetwork);
            int resultCount = LegacyRecipeData.size(buffer, MAX_RESULTS);
            List<ChanceResult> results = new ArrayList<>(resultCount);
            for (int i = 0; i < resultCount; i++) {
                results.add(new ChanceResult(buffer.readById(BuiltInRegistries.ITEM), buffer.readVarInt(), buffer.readVarInt()));
            }
            Optional<ProcessMedium> fluidOut = readMedium(buffer);
            Optional<ProcessMedium> gasOut = readMedium(buffer);
            int energy = buffer.readVarInt();
            int time = buffer.readVarInt();
            int minHeat = buffer.readVarInt();
            return new ProcessRecipe(type, serializer, items, fluidIn, gasIn, catalyst, results,
                    fluidOut, gasOut, energy, time, minHeat, buffer.readOptional(buf -> buf.readUtf()));
        });
    }

    private static void writeMedium(net.minecraft.network.FriendlyByteBuf buffer, Optional<ProcessMedium> medium) {
        buffer.writeOptional(medium, ProcessMedium.STREAM_CODEC::encode);
    }

    private static Optional<ProcessMedium> readMedium(net.minecraft.network.FriendlyByteBuf buffer) {
        return buffer.readOptional(ProcessMedium.STREAM_CODEC::decode);
    }
    *///?}

}
