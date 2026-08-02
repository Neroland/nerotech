package za.co.neroland.nerotech.neoforge;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import za.co.neroland.nerotech.network.NeroTechNetwork;
import za.co.neroland.nerotech.platform.INetworkHelper;

/**
 * NeoForge side of NeroTech's networking seam: registers every {@link NeroTechNetwork} payload
 * during {@code RegisterPayloadHandlersEvent} (on NeroTech's mod event bus) and implements the
 * send seam. Registered via {@code META-INF/services}.
 */
public final class NeoForgeNetwork implements INetworkHelper {

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(NeoForgeNetwork::onRegister);
    }

    private static void onRegister(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1").optional();
        for (NeroTechNetwork.Clientbound<?> cb : NeroTechNetwork.clientbound()) {
            registerClientbound(registrar, cb);
        }
        for (NeroTechNetwork.Serverbound<?> sb : NeroTechNetwork.serverbound()) {
            registerServerbound(registrar, sb);
        }
    }

    private static <T extends CustomPacketPayload> void registerClientbound(PayloadRegistrar registrar,
            NeroTechNetwork.Clientbound<T> cb) {
        registrar.playToClient(cb.type(), cb.codec(),
                (payload, context) -> context.enqueueWork(() -> cb.handler().accept(payload)));
    }

    private static <T extends CustomPacketPayload> void registerServerbound(PayloadRegistrar registrar,
            NeroTechNetwork.Serverbound<T> sb) {
        registrar.playToServer(sb.type(), sb.codec(),
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer serverPlayer) {
                        sb.handler().accept(payload, serverPlayer);
                    }
                }));
    }

    @Override
    public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }

    @Override
    public void sendToServer(CustomPacketPayload payload) {
        ClientPacketDistributor.sendToServer(payload);
    }
}
