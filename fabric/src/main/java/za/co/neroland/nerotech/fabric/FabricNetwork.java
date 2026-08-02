package za.co.neroland.nerotech.fabric;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import za.co.neroland.nerotech.network.NeroTechNetwork;
import za.co.neroland.nerotech.platform.INetworkHelper;

/**
 * Fabric side of NeroTech's networking seam. {@link #registerCommon()} (mod init, both sides)
 * registers every payload type; {@link #registerClient()} (client init) registers the clientbound
 * receivers, keeping {@code ClientPlayNetworking} off the dedicated server until then. Payload
 * types are NeroTech's own — Core drains its list during Core's earlier bootstrap, so NeroTech
 * registers here itself. Registered via {@code META-INF/services}.
 */
public final class FabricNetwork implements INetworkHelper {

    /** Mod-init (both sides): payload types + serverbound receivers. */
    public static void registerCommon() {
        for (NeroTechNetwork.Clientbound<?> cb : NeroTechNetwork.clientbound()) {
            registerClientboundType(cb);
        }
        for (NeroTechNetwork.Serverbound<?> sb : NeroTechNetwork.serverbound()) {
            registerServerbound(sb);
        }
    }

    /** Client-init: clientbound receivers (client-only API). */
    public static void registerClient() {
        for (NeroTechNetwork.Clientbound<?> cb : NeroTechNetwork.clientbound()) {
            registerClientReceiver(cb);
        }
    }

    private static <T extends CustomPacketPayload> void registerClientboundType(NeroTechNetwork.Clientbound<T> cb) {
        PayloadTypeRegistry.clientboundPlay().register(cb.type(), cb.codec());
    }

    private static <T extends CustomPacketPayload> void registerServerbound(NeroTechNetwork.Serverbound<T> sb) {
        PayloadTypeRegistry.serverboundPlay().register(sb.type(), sb.codec());
        ServerPlayNetworking.registerGlobalReceiver(sb.type(), (payload, context) -> {
            ServerPlayer player = context.player();
            player.level().getServer().execute(() -> sb.handler().accept(payload, player));
        });
    }

    private static <T extends CustomPacketPayload> void registerClientReceiver(NeroTechNetwork.Clientbound<T> cb) {
        ClientPlayNetworking.registerGlobalReceiver(cb.type(), (payload, context) ->
                context.client().execute(() -> cb.handler().accept(payload)));
    }

    @Override
    public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        ServerPlayNetworking.send(player, payload);
    }

    @Override
    public void sendToServer(CustomPacketPayload payload) {
        ClientPlayNetworking.send(payload);
    }
}
