package za.co.neroland.nerotech.forge;

//? if >=1.21.2 {
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.event.OnDatapackSyncEvent;
//? if <1.21.6 {
/*import net.minecraftforge.common.MinecraftForge;
*///?}
import net.minecraftforge.network.payload.PayloadFlow;

import za.co.neroland.nerotech.NeroTechCommon;
import za.co.neroland.nerotech.compat.jei.JeiSyncedRecipes;
import za.co.neroland.nerotech.network.NeroTechNetwork;
import za.co.neroland.nerotech.registry.ModRecipeTypes;

/**
 * Forge's half of NeroTech's machine-recipe sync. Since 1.21.2 the vanilla client holds no full recipe
 * list, and Forge (unlike NeoForge's {@code OnDatapackSyncEvent.sendRecipes} or Fabric's recipe
 * synchronisation module) has no API to opt recipe types back in. So NeroTech sends its own sixteen
 * machine recipe types over its own channel when datapacks sync (join and {@code /reload}), and the
 * client hands them to {@link JeiSyncedRecipes} for JEI/EMI. Recipe definitions only — no player data.
 */
public final class ForgeRecipeSync {

    private ForgeRecipeSync() {
    }

    /** Add the clientbound payload to NeroTech's channel (called before the channel is built). */
    public static void registerPayload(PayloadFlow<RegistryFriendlyByteBuf, CustomPacketPayload> play) {
        play.addMain(RecipeSyncPayload.TYPE, RecipeSyncPayload.STREAM_CODEC, (payload, context) -> {
            if (context.isClientSide()) {
                JeiSyncedRecipes.accept(payload.recipes());
            }
        });
    }

    /** Send the recipes whenever the server syncs datapacks to one player or to everyone. */
    public static void registerListener() {
        //? if >=1.21.6 {
        OnDatapackSyncEvent.BUS.addListener(ForgeRecipeSync::onDatapackSync);
        //?} else {
        /*MinecraftForge.EVENT_BUS.addListener(ForgeRecipeSync::onDatapackSync);
        *///?}
    }

    private static void onDatapackSync(OnDatapackSyncEvent event) {
        Set<RecipeType<?>> types = Set.copyOf(ModRecipeTypes.syncedTypes());
        List<RecipeHolder<?>> recipes = new ArrayList<>();
        for (RecipeHolder<?> holder : event.getPlayerList().getServer().getRecipeManager().getRecipes()) {
            if (types.contains(holder.value().getType())) {
                recipes.add(holder);
            }
        }
        RecipeSyncPayload payload = new RecipeSyncPayload(List.copyOf(recipes));
        for (ServerPlayer player : event.getPlayers()) {
            NeroTechNetwork.sendToPlayer(player, payload);
        }
    }

    /** Server to client: NeroTech's machine recipes, each as id + serializer id + serializer payload. */
    public record RecipeSyncPayload(List<RecipeHolder<?>> recipes) implements CustomPacketPayload {

        public static final Type<RecipeSyncPayload> TYPE =
                new Type<>(Identifier.fromNamespaceAndPath(NeroTechCommon.MOD_ID, "recipe_sync"));

        private static final StreamCodec<io.netty.buffer.ByteBuf, ResourceKey<Recipe<?>>> KEY_CODEC =
                ResourceKey.streamCodec(Registries.RECIPE);

        public static final StreamCodec<RegistryFriendlyByteBuf, RecipeSyncPayload> STREAM_CODEC =
                StreamCodec.of(RecipeSyncPayload::write, RecipeSyncPayload::read);

        @SuppressWarnings("unchecked")
        private static void write(RegistryFriendlyByteBuf buf, RecipeSyncPayload payload) {
            buf.writeVarInt(payload.recipes.size());
            for (RecipeHolder<?> holder : payload.recipes) {
                Recipe<?> recipe = holder.value();
                RecipeSerializer<Recipe<?>> serializer = (RecipeSerializer<Recipe<?>>) recipe.getSerializer();
                KEY_CODEC.encode(buf, holder.id());
                buf.writeUtf(String.valueOf(BuiltInRegistries.RECIPE_SERIALIZER.getKey(serializer)));
                serializer.streamCodec().encode(buf, recipe);
            }
        }

        private static RecipeSyncPayload read(RegistryFriendlyByteBuf buf) {
            int count = buf.readVarInt();
            List<RecipeHolder<?>> out = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                ResourceKey<Recipe<?>> key = KEY_CODEC.decode(buf);
                Identifier serializerId = Identifier.parse(buf.readUtf());
                RecipeSerializer<?> serializer = BuiltInRegistries.RECIPE_SERIALIZER.getValue(serializerId);
                if (serializer == null) {
                    // An unknown serializer cannot be skipped (its payload length is unknown): stop here.
                    break;
                }
                Recipe<?> recipe = serializer.streamCodec().decode(buf);
                out.add(new RecipeHolder<>(key, recipe));
            }
            return new RecipeSyncPayload(List.copyOf(out));
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
//?} else {
/*// Before 1.21.2 vanilla still syncs every recipe to the client, so Forge needs no recipe sync.
public final class ForgeRecipeSync {

    private ForgeRecipeSync() {
    }
}
*///?}
