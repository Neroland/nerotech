package za.co.neroland.nerotech.recipe;

//? if <1.20.5 {
/*import java.util.function.BiConsumer;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;

import za.co.neroland.nerolandcore.network.legacy.LegacyStreamCodec;

public final class LegacyRecipeSerializer<R extends Recipe<?>> implements RecipeSerializer<R> {
    private final Codec<R> json;
    private final LegacyStreamCodec<FriendlyByteBuf, R> wire;
    private final BiConsumer<R, ResourceLocation> bindId;

    public LegacyRecipeSerializer(Codec<R> json, LegacyStreamCodec<FriendlyByteBuf, R> wire,
            BiConsumer<R, ResourceLocation> bindId) {
        this.json = json;
        this.wire = wire;
        this.bindId = bindId;
    }

    //? if >=1.20.2 {
    @Override
    public Codec<R> codec() {
        return this.json;
    }

    @Override
    public R fromNetwork(FriendlyByteBuf buffer) {
        return this.wire.decode(buffer);
    }
    //?} else {
    @Override
    public R fromJson(ResourceLocation id, JsonObject data) {
        R recipe = this.json.parse(JsonOps.INSTANCE, data).getOrThrow(false, message -> {
            throw new JsonSyntaxException(message);
        });
        this.bindId.accept(recipe, id);
        return recipe;
    }

    @Override
    public R fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
        R recipe = this.wire.decode(buffer);
        this.bindId.accept(recipe, id);
        return recipe;
    }
    //?}

    @Override
    public void toNetwork(FriendlyByteBuf buffer, R recipe) {
        this.wire.encode(buffer, recipe);
    }
}
*///?}
