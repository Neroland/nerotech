package za.co.neroland.nerotech.forge;

//? if >=1.20.5 {
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.Channel;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.payload.PayloadFlow;

import za.co.neroland.nerotech.NeroTechCommon;
import za.co.neroland.nerotech.network.NeroTechNetwork;
import za.co.neroland.nerotech.platform.INetworkHelper;

/**
 * Forge side of NeroTech's networking seam: builds NeroTech's own {@code nerotech:main} channel
 * from the {@link NeroTechNetwork} payload list (Core's channel is sealed during Core's earlier
 * bootstrap, so NeroTech cannot piggyback on it) and implements the send seam. Registered via
 * {@code META-INF/services}.
 */
public final class ForgeNetwork implements INetworkHelper {

    private static Channel<CustomPacketPayload> channel;

    public static void register() {
        PayloadFlow<RegistryFriendlyByteBuf, CustomPacketPayload> play =
                ChannelBuilder.named(Identifier.fromNamespaceAndPath(NeroTechCommon.MOD_ID, "main"))
                        .optional()
                        .payloadChannel()
                        .play()
                        .bidirectional();
        for (NeroTechNetwork.Clientbound<?> cb : NeroTechNetwork.clientbound()) {
            registerClientbound(play, cb);
        }
        for (NeroTechNetwork.Serverbound<?> sb : NeroTechNetwork.serverbound()) {
            registerServerbound(play, sb);
        }
        // Forge has no recipe-sync API: NeroTech's machine recipes ride this channel (see ForgeRecipeSync).
        //? if >=1.21.2 {
        ForgeRecipeSync.registerPayload(play);
        //?}
        channel = play.build();
    }

    private static <T extends CustomPacketPayload> void registerClientbound(
            PayloadFlow<RegistryFriendlyByteBuf, CustomPacketPayload> play, NeroTechNetwork.Clientbound<T> cb) {
        // The flow is bidirectional, so ignore this clientbound payload if a client sends it upstream.
        play.addMain(cb.type(), registryCodec(cb.codec()), (payload, context) -> {
            if (context.isClientSide()) {
                cb.handler().accept(payload);
            }
        });
    }

    private static <T extends CustomPacketPayload> void registerServerbound(
            PayloadFlow<RegistryFriendlyByteBuf, CustomPacketPayload> play, NeroTechNetwork.Serverbound<T> sb) {
        // The flow is bidirectional; only handle this serverbound payload when a real player sent it.
        play.addMain(sb.type(), registryCodec(sb.codec()), (payload, context) -> {
            if (context.getSender() instanceof ServerPlayer serverPlayer) {
                sb.handler().accept(payload, serverPlayer);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private static <T extends CustomPacketPayload> StreamCodec<RegistryFriendlyByteBuf, T> registryCodec(
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
        return (StreamCodec<RegistryFriendlyByteBuf, T>) codec;
    }

    @Override
    public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        if (channel != null) {
            channel.send(payload, PacketDistributor.PLAYER.with(player));
        }
    }

    @Override
    public void sendToServer(CustomPacketPayload payload) {
        if (channel != null) {
            channel.send(payload, PacketDistributor.SERVER.noArg());
        }
    }
}
//?} else {
/*import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
//? if >=1.20.2 {
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.SimpleChannel;
//?} else {
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
//?}

import za.co.neroland.nerotech.NeroTechCommon;
import za.co.neroland.nerotech.network.NeroTechNetwork;
import za.co.neroland.nerolandcore.network.legacy.LegacyPayload;
import za.co.neroland.nerotech.platform.INetworkHelper;

public final class ForgeNetwork implements INetworkHelper {
    private static final int PROTOCOL_VERSION = 1;
    private static SimpleChannel channel;

    public static void register() {
        //? if >=1.20.2 {
        channel = ChannelBuilder.named(new ResourceLocation(NeroTechCommon.MOD_ID, "main"))
                .networkProtocolVersion(PROTOCOL_VERSION).optional().simpleChannel();
        //?} else {
        channel = NetworkRegistry.newSimpleChannel(new ResourceLocation(NeroTechCommon.MOD_ID, "main"),
                () -> Integer.toString(PROTOCOL_VERSION), ForgeNetwork::acceptVersion, ForgeNetwork::acceptVersion);
        //?}
        int id = 0;
        for (NeroTechNetwork.Clientbound<?> packet : NeroTechNetwork.clientbound()) {
            registerClientbound(packet, id++);
        }
        for (NeroTechNetwork.Serverbound<?> packet : NeroTechNetwork.serverbound()) {
            registerServerbound(packet, id++);
        }
    }

    //? if <1.20.2 {
    private static boolean acceptVersion(String version) {
        return Integer.toString(PROTOCOL_VERSION).equals(version) || NetworkRegistry.ABSENT.version().equals(version)
                || NetworkRegistry.ACCEPTVANILLA.equals(version);
    }
    //?}

    private static <T extends LegacyPayload> void registerClientbound(NeroTechNetwork.Clientbound<T> packet, int id) {
        channel.messageBuilder(packet.type().payloadClass(), id, NetworkDirection.PLAY_TO_CLIENT)
                .encoder((payload, buffer) -> packet.codec().encode(buffer, payload))
                .decoder(packet.codec()::decode)
                .consumerMainThread((payload, context) -> packet.handler().accept(payload))
                .add();
    }

    private static <T extends LegacyPayload> void registerServerbound(NeroTechNetwork.Serverbound<T> packet, int id) {
        channel.messageBuilder(packet.type().payloadClass(), id, NetworkDirection.PLAY_TO_SERVER)
                .encoder((payload, buffer) -> packet.codec().encode(buffer, payload))
                .decoder(packet.codec()::decode)
                .consumerMainThread((payload, context) -> {
                    //? if >=1.20.2 {
                    ServerPlayer player = context.getSender();
                    //?} else {
                    ServerPlayer player = context.get().getSender();
                    //?}
                    if (player != null) {
                        packet.handler().accept(payload, player);
                    }
                })
                .add();
    }

    @Override
    public void sendToPlayer(ServerPlayer player, LegacyPayload payload) {
        //? if >=1.20.2 {
        var connection = player.connection.getConnection();
        //?} else {
        var connection = player.connection.connection;
        //?}
        if (channel != null && channel.isRemotePresent(connection)) {
            //? if >=1.20.2 {
            channel.send(payload, connection);
            //?} else {
            channel.send(PacketDistributor.PLAYER.with(() -> player), payload);
            //?}
        }
    }

    @Override
    public void sendToServer(LegacyPayload payload) {
        var connection = Minecraft.getInstance().getConnection();
        if (channel != null && connection != null && channel.isRemotePresent(connection.getConnection())) {
            //? if >=1.20.2 {
            channel.send(payload, connection.getConnection());
            //?} else {
            channel.sendToServer(payload);
            //?}
        }
    }
}
*///?}
