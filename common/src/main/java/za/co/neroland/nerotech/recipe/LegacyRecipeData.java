package za.co.neroland.nerotech.recipe;

//? if <1.20.5 {
/*import java.util.List;
import java.util.function.Function;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

public final class LegacyRecipeData {
    //? if >=1.20.2 {
    public static final Codec<Ingredient> INGREDIENT = Ingredient.CODEC_NONEMPTY;
    //?} else {
    public static final Codec<Ingredient> INGREDIENT = Codec.PASSTHROUGH.comapFlatMap(data -> {
        try {
            return DataResult.success(Ingredient.fromJson(data.convert(JsonOps.INSTANCE).getValue()));
        } catch (com.google.gson.JsonParseException e) {
            return DataResult.error(e::getMessage);
        }
    }, ingredient -> new Dynamic<>(JsonOps.INSTANCE, ingredient.toJson()));
    //?}

    static final Codec<ItemStack> RESULT = RecordCodecBuilder.create(instance -> instance.group(
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(ItemStack::getItem),
            Codec.intRange(1, 64).optionalFieldOf("count", 1).forGetter(ItemStack::getCount)
    ).apply(instance, ItemStack::new));

    static <T> Codec<List<T>> list(Codec<T> element, int maximum) {
        Function<List<T>, DataResult<List<T>>> validate = entries -> entries.size() <= maximum
                ? DataResult.success(entries)
                : DataResult.error(() -> "Expected at most " + maximum + " recipe entries");
        return element.listOf().flatXmap(validate, validate);
    }

    static int size(FriendlyByteBuf buffer, int maximum) {
        int count = buffer.readVarInt();
        if (count < 0 || count > maximum) {
            throw new IllegalArgumentException("Invalid NeroTech recipe list size " + count);
        }
        return count;
    }

    private LegacyRecipeData() {
    }
}
*///?}
