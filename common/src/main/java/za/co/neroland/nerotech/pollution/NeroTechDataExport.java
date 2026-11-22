package za.co.neroland.nerotech.pollution;

import java.util.List;
import java.util.UUID;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.server.MinecraftServer;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerotech.guide.TechGuideSeenState;

/**
 * NeroTech's part of a player's data export (POPIA s23 / GDPR Art. 15 and 20), registered with Core's
 * {@code PlayerDataExport} beside the erasure hook in {@code NeroTechCommon}. It mirrors exactly what that
 * hook erases: the opt-in pollution attribution row, the attribution opt-out, and the Tech Guide "seen"
 * masks. It never includes the player's UUID, another player's data or the pending-erasure set (which only
 * holds already-erased owners), never writes anything and never logs.
 *
 * <p>Machine ownership is not enumerated: the placing player's UUID lives in each machine's block entity,
 * which an export cannot reach in unloaded chunks without loading them. The export says so instead.</p>
 */
public final class NeroTechDataExport {

    static final String MACHINE_OWNERSHIP_NOTE = "Machines you placed record you as their owner (only used for "
            + "operating charges and opt-in pollution attribution). They are not listed here because they sit in "
            + "the world; an erasure request clears that ownership from every machine.";

    private NeroTechDataExport() {
    }

    /** Core's exporter entry point; {@code null} when NeroTech stores nothing for the player. */
    @Nullable
    public static JsonElement export(MinecraftServer server, UUID player) {
        PollutionState state = PollutionState.get(server);
        long lastDay = state.attributedLastDay(player);
        boolean optedOut = PollutionAttributionPrefs.get(server).isOptedOut(player);
        List<Integer> masks = TechGuideSeenState.get(server).masks(player);
        return build(lastDay < 0 ? null : state.attributed(player), lastDay < 0 ? null : lastDay, optedOut, masks);
    }

    /**
     * Pure builder (tested). {@code attributed}/{@code lastDay} are {@code null} when no attribution row is
     * stored; {@code optedOut} is the stored preference. Returns {@code null} when nothing is stored at all.
     */
    @Nullable
    static JsonObject build(@Nullable Integer attributed, @Nullable Long lastDay, boolean optedOut,
            List<Integer> masks) {
        boolean anySeen = masks.stream().anyMatch(m -> m != null && m != 0);
        if (attributed == null && !optedOut && !anySeen) {
            return null;
        }
        JsonObject out = new JsonObject();
        if (attributed != null) {
            JsonObject row = new JsonObject();
            row.addProperty("attributedPollution", attributed);
            if (lastDay != null) {
                row.addProperty("lastUpdatedEpochDay", lastDay);
            }
            out.add("pollutionAttribution", row);
        }
        out.addProperty("pollutionAttributionOptedOut", optedOut);
        if (anySeen) {
            JsonArray arr = new JsonArray();
            for (Integer m : masks) {
                arr.add(m == null ? 0 : m);
            }
            out.add("techGuideSeenStepMasks", arr);
        }
        out.addProperty("machineOwnership", MACHINE_OWNERSHIP_NOTE);
        return out;
    }
}
