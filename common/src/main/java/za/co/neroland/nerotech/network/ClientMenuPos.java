package za.co.neroland.nerotech.network;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.BlockPos;

import org.jetbrains.annotations.Nullable;

/**
 * Client-side mailbox for {@link MachineMenuPosPayload}, keyed by container id — the same
 * split Core uses for its side-config sync ({@code ClientSideConfig}): the clientbound handler
 * stays free of client-only classes and just drops the position here; the machine screen polls
 * it and applies it to its menu once the container ids match, so a stale payload can never
 * retarget a different menu.
 *
 * <p>Bounded by vanilla's container-id counter (wraps at 100 per session) and drained on poll.
 * Holds only container ids and block positions — no player data (POPIA/GDPR).
 */
public final class ClientMenuPos {

    private static final Map<Integer, BlockPos> PENDING = new ConcurrentHashMap<>();

    private ClientMenuPos() {
    }

    /** Called by the clientbound handler. */
    public static void accept(int containerId, BlockPos pos) {
        PENDING.put(containerId, pos.immutable());
    }

    /** Take and clear the position for a container id, or {@code null} if none is pending. */
    @Nullable
    public static BlockPos poll(int containerId) {
        return PENDING.remove(containerId);
    }

    public static void clear() {
        PENDING.clear();
    }
}
