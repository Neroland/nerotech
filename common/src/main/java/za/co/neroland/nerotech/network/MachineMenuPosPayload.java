package za.co.neroland.nerotech.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import za.co.neroland.nerotech.NeroTechCommon;

/**
 * Server &rarr; client: the world position of the machine whose menu the player just opened,
 * keyed by the menu's container id. NeroTech menus are plain vanilla {@code MenuType}s (no
 * loader-specific extended menu types), so the position the server baked into
 * {@link za.co.neroland.nerotech.menu.MachineMenu} never reaches the client on its own; this
 * payload closes that gap so the Side Config widget can target the machine without ray-tracing.
 *
 * <p><b>Privacy (POPIA/GDPR):</b> carries only a container id and a block position — the
 * machine GUI the player opened, world/block data only, no identity, never logged.
 */
public record MachineMenuPosPayload(int containerId, BlockPos pos) implements CustomPacketPayload {

    public static final Type<MachineMenuPosPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(NeroTechCommon.MOD_ID, "machine_menu_pos"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MachineMenuPosPayload> STREAM_CODEC =
            StreamCodec.of(MachineMenuPosPayload::write, MachineMenuPosPayload::read);

    private static void write(RegistryFriendlyByteBuf buf, MachineMenuPosPayload payload) {
        buf.writeVarInt(payload.containerId);
        buf.writeBlockPos(payload.pos);
    }

    private static MachineMenuPosPayload read(RegistryFriendlyByteBuf buf) {
        int containerId = buf.readVarInt();
        BlockPos pos = buf.readBlockPos();
        return new MachineMenuPosPayload(containerId, pos);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
