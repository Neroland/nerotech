package za.co.neroland.nerotech.pollution;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import za.co.neroland.nerolandcore.data.SavedDataRecovery;

import za.co.neroland.nerotech.NeroTechCommon;

/**
 * Per-player OPT-OUT layer for pollution attribution — the store behind the NeroLink
 * {@code set_pollution_attribution} action (a player privacy control over their OWN data).
 *
 * <p>The server-wide {@code pollutionPerPlayerAttribution} config is a single global flag, not a
 * per-player opt-in. To give each player a genuine own-data control (as the link action requires)
 * without changing that server-authoritative flag, this SavedData records only the players who have
 * explicitly OPTED OUT. The effective rule is therefore:
 *
 * <pre>attribute(player) iff  global-attribution ON  AND  player is NOT opted out</pre>
 *
 * <p>Default (no row) = "follow the global flag", so nothing changes for servers/players that never
 * touch the control. {@link PollutionManager#record} consults {@link #isOptedOut(UUID)} before it
 * attributes.
 *
 * <p><b>Privacy (POPIA/GDPR):</b> data minimisation — a bare set of UUIDs, no names/timestamps.
 * Purged per-player through Core's shared data-erasure hook ({@link #forgetPlayer(UUID)}, registered
 * in {@code NeroTechCommon.init()}); an erased player simply returns to the global default.
 *
 * <p><b>Pending owner erasure (0.4.0):</b> the same store also carries the set of erased player
 * UUIDs whose machine ownership still has to be dropped. A machine's owner lives in its block
 * entity, and an erase request cannot reach a block entity in an unloaded chunk — so the eraser
 * records the UUID here with the epoch day of the request, and every
 * {@code NeroTechMachineBlockEntity} checks {@link #isPendingErasure(UUID)} on its first server
 * tick after (re)load and clears its owner if listed. Rows expire {@value #ERASURE_RETENTION_DAYS}
 * days after the request (purged on store load and on each new request), which bounds how long an
 * erased UUID lingers here: a machine unloaded for longer than that keeps a UUID nobody can be
 * matched to any more — the player's data in every other NeroTech store is gone by then.
 */
public final class PollutionAttributionPrefs extends SavedData {

    /** How long a pending owner-erasure row is kept before it expires (days). */
    public static final int ERASURE_RETENTION_DAYS = 30;

    public static final Identifier ID =
            Identifier.fromNamespaceAndPath(NeroTechCommon.MOD_ID, "pollution_attribution_prefs");

    public static final SavedDataType<PollutionAttributionPrefs> TYPE =
            new SavedDataType<>(ID, PollutionAttributionPrefs::new, codec(), null);

    /** Players who explicitly opted OUT of attribution (absence = follow the global flag). */
    private final Set<UUID> optedOut = new HashSet<>();

    /** Erased players whose machine owners still have to be cleared → epoch day of the request. */
    private final Map<UUID, Long> pendingErasure = new HashMap<>();

    /**
     * Bumped on every {@link #markOwnerErasure}; loaded machines compare it per tick (one static
     * read) and re-check their owner only when it moved. Process-local, never persisted: a fresh
     * process starts every machine at "unchecked" anyway.
     */
    private static volatile int erasureEpoch;

    public PollutionAttributionPrefs() {
    }

    /**
     * The store for this server (overworld saved data), through Core's crash-safe
     * {@link SavedDataRecovery} so a corrupt file degrades to the last backup or a fresh store
     * instead of crashing every tick that touches it.
     */
    public static PollutionAttributionPrefs get(MinecraftServer server) {
        return SavedDataRecovery.get(server.overworld(), TYPE, PollutionAttributionPrefs::new, ID.toString());
    }

    /** Whether {@code player} has opted out of having their pollution attributed. */
    public boolean isOptedOut(UUID player) {
        return this.optedOut.contains(player);
    }

    /**
     * Set a player's own attribution preference. {@code enabled == true} means "attribute my
     * pollution" (clear any opt-out); {@code false} records an opt-out. Returns the effective
     * opt-out state after the change.
     */
    public boolean setEnabled(UUID player, boolean enabled) {
        boolean changed = enabled ? this.optedOut.remove(player) : this.optedOut.add(player);
        if (changed) {
            setDirty();
        }
        return this.optedOut.contains(player);
    }

    /** POPIA/GDPR erasure: forget a player's preference (returns them to the global default). */
    public void forgetPlayer(UUID player) {
        if (this.optedOut.remove(player)) {
            setDirty();
        }
    }

    // --- pending owner erasure (POPIA/GDPR; see the class note) --------------

    /**
     * Record that {@code player} was erased on {@code epochDay}: every machine they own drops its
     * owner on its next load (see {@code NeroTechMachineBlockEntity}). Also purges rows older than
     * {@value #ERASURE_RETENTION_DAYS} days so the set never grows without bound.
     */
    public void markOwnerErasure(UUID player, long epochDay) {
        purgeExpiredErasures(epochDay);
        this.pendingErasure.put(player, epochDay);
        setDirty();
        erasureEpoch++;
    }

    /** The current owner-erasure epoch — bumped per erase request; wraps harmlessly. */
    public static int erasureEpoch() {
        return erasureEpoch;
    }

    /** Whether {@code owner} was erased recently enough that machines must still drop them. */
    public boolean isPendingErasure(UUID owner) {
        return this.pendingErasure.containsKey(owner);
    }

    /** Drop pending-erasure rows older than {@value #ERASURE_RETENTION_DAYS} days as of {@code today}. */
    public void purgeExpiredErasures(long today) {
        Iterator<Map.Entry<UUID, Long>> it = this.pendingErasure.entrySet().iterator();
        boolean changed = false;
        while (it.hasNext()) {
            Map.Entry<UUID, Long> entry = it.next();
            if (today - entry.getValue() > ERASURE_RETENTION_DAYS) {
                it.remove();
                changed = true;
            }
        }
        if (changed) {
            setDirty();
        }
    }

    // --- persistence --------------------------------------------------------

    /** One persisted pending-erasure row: the erased UUID (as text) and the request's epoch day. */
    private record ErasureRow(String uuid, long epochDay) {
        static final Codec<ErasureRow> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Codec.STRING.fieldOf("uuid").forGetter(ErasureRow::uuid),
                Codec.LONG.fieldOf("day").forGetter(ErasureRow::epochDay)
        ).apply(inst, ErasureRow::new));
    }

    private static Codec<PollutionAttributionPrefs> codec() {
        return RecordCodecBuilder.create(inst -> inst.group(
                Codec.STRING.listOf().optionalFieldOf("optedOut", List.of())
                        .forGetter(PollutionAttributionPrefs::optedOutStrings),
                ErasureRow.CODEC.listOf().optionalFieldOf("pendingErasure", List.of())
                        .forGetter(PollutionAttributionPrefs::erasureRows)
        ).apply(inst, PollutionAttributionPrefs::fromRows));
    }

    private List<String> optedOutStrings() {
        List<String> out = new ArrayList<>();
        this.optedOut.forEach(uuid -> out.add(uuid.toString()));
        return out;
    }

    private List<ErasureRow> erasureRows() {
        List<ErasureRow> out = new ArrayList<>();
        this.pendingErasure.forEach((uuid, day) -> out.add(new ErasureRow(uuid.toString(), day)));
        return out;
    }

    private static PollutionAttributionPrefs fromRows(List<String> uuids, List<ErasureRow> erasures) {
        PollutionAttributionPrefs state = new PollutionAttributionPrefs();
        for (String s : uuids) {
            try {
                state.optedOut.add(UUID.fromString(s));
            } catch (IllegalArgumentException ignored) {
                // skip malformed UUID rows
            }
        }
        for (ErasureRow row : erasures) {
            try {
                state.pendingErasure.put(UUID.fromString(row.uuid()), row.epochDay());
            } catch (IllegalArgumentException ignored) {
                // skip malformed UUID rows
            }
        }
        // Expired rows die on load, so a long-idle world does not carry them forward.
        state.purgeExpiredErasures(System.currentTimeMillis() / 86_400_000L);
        return state;
    }
}
