package za.co.neroland.nerotech.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import za.co.neroland.nerotech.NeroTechCommon;
import za.co.neroland.nerotech.machine.MachinePreset;
import za.co.neroland.nerotech.machine.NeroTechMachineBlockEntity;
import za.co.neroland.nerotech.menu.MachineMenu;

/**
 * Client &rarr; server intent for the Stage H overclock selector: "set the machine behind my open
 * menu {@code containerId} to preset {@code ordinal}" — NeroTech's first serverbound payload,
 * mirroring Core's {@code SideConfigIntentPayload} flow one namespace down. The server validates
 * everything ({@link #handle}): the container id must match the player's open menu, that menu must
 * be a {@link MachineMenu} with a live machine block-entity behind it (the server ctor path), the
 * player must be within reach (64 sq), and the ordinal is clamped by
 * {@link MachinePreset#byOrdinal}. Clients never mutate the preset directly.
 *
 * <p><b>Privacy (POPIA/GDPR):</b> carries only a container id and a preset ordinal — no player
 * identity, never logged.
 */
public record MachinePresetPayload(int containerId, int ordinal) implements CustomPacketPayload {

    public static final Type<MachinePresetPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(NeroTechCommon.MOD_ID, "machine_preset"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MachinePresetPayload> STREAM_CODEC =
            StreamCodec.of(MachinePresetPayload::write, MachinePresetPayload::read);

    private static final double REACH_SQR = 64.0D;

    private static void write(RegistryFriendlyByteBuf buf, MachinePresetPayload payload) {
        buf.writeVarInt(payload.containerId);
        buf.writeByte(payload.ordinal);
    }

    private static MachinePresetPayload read(RegistryFriendlyByteBuf buf) {
        int containerId = buf.readVarInt();
        int ordinal = buf.readByte();
        return new MachinePresetPayload(containerId, ordinal);
    }

    /** Server-side handler: validate the intent against the sender's open menu, then apply. */
    public static void handle(MachinePresetPayload payload, ServerPlayer player) {
        if (player.containerMenu == null || player.containerMenu.containerId != payload.containerId) {
            return; // stale intent — the menu already closed or changed
        }
        if (!(player.containerMenu instanceof MachineMenu menu)) {
            return;
        }
        NeroTechMachineBlockEntity machine = menu.serverMachine();
        if (machine == null || machine.isRemoved()) {
            return;
        }
        BlockPos pos = machine.getBlockPos();
        if (player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) > REACH_SQR) {
            return;
        }
        machine.setPreset(MachinePreset.byOrdinal(payload.ordinal));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
