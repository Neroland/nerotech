package za.co.neroland.nerotech.network;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import za.co.neroland.nerotech.platform.Services;

/**
 * NeroTech's cross-loader networking registry, mirroring Core's {@code CoreNetwork} one
 * namespace down: payloads are declared here once (type + stream codec + common-safe handler);
 * each NeroTech loader module iterates the list and wires it to its own networking API
 * (NeoForge {@code PayloadRegistrar}, Forge {@code ChannelBuilder}, Fabric
 * {@code PayloadTypeRegistry} + receivers). Sending goes through {@link Services#NETWORK}.
 *
 * <p>NeroTech cannot simply add its payloads to {@code CoreNetwork}: Core's Forge and Fabric
 * modules drain those lists during Core's own bootstrap, and Core — as a hard dependency —
 * always initialises before NeroTech, so anything added from {@code NeroTechCommon.init()}
 * would arrive after Core's channel is already built (only NeoForge's event-based registrar
 * would tolerate the late addition). Same reasoning as NeroTech's own
 * {@link za.co.neroland.nerotech.registry.RegistrationProvider} seam.
 *
 * <p>V1 registers one payload: the clientbound {@link MachineMenuPosPayload} that tells the
 * client which machine its freshly opened menu belongs to. Its handler drops into the
 * {@link ClientMenuPos} mailbox — pure common code, safe to register from either side.
 */
public final class NeroTechNetwork {

    /** A server → client payload + the client-side handler that consumes it. */
    public record Clientbound<T extends CustomPacketPayload>(
            CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
            Consumer<T> handler) {
    }

    private static final List<Clientbound<?>> CLIENTBOUND = new ArrayList<>();

    private NeroTechNetwork() {
    }

    public static <T extends CustomPacketPayload> void clientbound(
            CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec, Consumer<T> handler) {
        CLIENTBOUND.add(new Clientbound<>(type, codec, handler));
    }

    public static List<Clientbound<?>> clientbound() {
        return CLIENTBOUND;
    }

    /** Called from common init so the payload list exists before each loader registers it. */
    public static void init() {
        clientbound(MachineMenuPosPayload.TYPE, MachineMenuPosPayload.STREAM_CODEC,
                payload -> ClientMenuPos.accept(payload.containerId(), payload.pos()));
    }

    /** Server → one client, through the loader's send seam. */
    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        Services.NETWORK.sendToPlayer(player, payload);
    }
}
