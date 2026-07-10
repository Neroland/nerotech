package za.co.neroland.nerotech.platform;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * Cross-loader packet-send seam for NeroTech's own payloads, resolved per loader via
 * {@link Services} ({@link java.util.ServiceLoader}) — the counterpart to Core's
 * {@code NetworkPlatform}, owned by NeroTech for the same timing reason as
 * {@link za.co.neroland.nerotech.registry.RegistrationProvider}: Core's loader modules wire
 * Core's payload lists during Core's own bootstrap, before any downstream {@code init()} runs.
 * Payload types and handlers are declared once in
 * {@link za.co.neroland.nerotech.network.NeroTechNetwork}; each loader registers them and
 * implements this send interface. Kept intentionally small — grow as needed.
 */
public interface INetworkHelper {

    /** Server → one client. */
    void sendToPlayer(ServerPlayer player, CustomPacketPayload payload);
}
