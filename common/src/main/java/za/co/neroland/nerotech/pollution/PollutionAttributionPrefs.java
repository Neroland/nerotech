package za.co.neroland.nerotech.pollution;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

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
 */
public final class PollutionAttributionPrefs extends SavedData {

    public static final Identifier ID =
            Identifier.fromNamespaceAndPath(NeroTechCommon.MOD_ID, "pollution_attribution_prefs");

    public static final SavedDataType<PollutionAttributionPrefs> TYPE =
            new SavedDataType<>(ID, PollutionAttributionPrefs::new, codec(), null);

    /** Players who explicitly opted OUT of attribution (absence = follow the global flag). */
    private final Set<UUID> optedOut = new HashSet<>();

    public PollutionAttributionPrefs() {
    }

    public static PollutionAttributionPrefs get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
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

    // --- persistence --------------------------------------------------------

    private static Codec<PollutionAttributionPrefs> codec() {
        return RecordCodecBuilder.create(inst -> inst.group(
                Codec.STRING.listOf().optionalFieldOf("optedOut", List.of())
                        .forGetter(PollutionAttributionPrefs::optedOutStrings)
        ).apply(inst, PollutionAttributionPrefs::fromStrings));
    }

    private List<String> optedOutStrings() {
        List<String> out = new ArrayList<>();
        this.optedOut.forEach(uuid -> out.add(uuid.toString()));
        return out;
    }

    private static PollutionAttributionPrefs fromStrings(List<String> uuids) {
        PollutionAttributionPrefs state = new PollutionAttributionPrefs();
        for (String s : uuids) {
            try {
                state.optedOut.add(UUID.fromString(s));
            } catch (IllegalArgumentException ignored) {
                // skip malformed UUID rows
            }
        }
        return state;
    }
}
