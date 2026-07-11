package za.co.neroland.nerotech.guide;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import za.co.neroland.nerotech.NeroTechCommon;

/**
 * Server-authoritative store for the Tech Guide's per-player "seen" bitmasks — a completed step
 * pulses in the GUI until the player clicks it once (Nerospace's Star Guide "seen" recipe). Guide
 * COMPLETION is never stored here: it is always derived live from the player's advancements
 * ({@link TechGuideProgress}); this state only remembers which completed steps a player has already
 * looked at.
 *
 * <p><b>Privacy (POPIA/GDPR):</b> data minimisation — one small int bitmask list per player, keyed by
 * UUID only (no names, no timestamps, no positions). Nerospace stores the same masks in per-loader
 * player attachments; NeroTech has no attachment seam, so the architecturally equivalent UUID-keyed
 * {@link SavedData} is used (the same store shape as {@link
 * za.co.neroland.nerotech.pollution.PollutionState}). Rows are purged through Core's shared
 * data-erasure hook ({@link #forgetPlayer(UUID)}, registered in {@code NeroTechCommon.init()} beside
 * {@code PollutionManager::erasePlayer}); an erased player simply sees fresh "unseen" pulses again.
 * No retention sweep is needed — the masks carry no personal signal beyond the UUID itself — but they
 * remain erasable on request like every other Nero player-keyed store.
 */
public final class TechGuideSeenState extends SavedData {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(NeroTechCommon.MOD_ID, "tech_guide_seen");

    public static final SavedDataType<TechGuideSeenState> TYPE =
            new SavedDataType<>(ID, TechGuideSeenState::new, codec(), null);

    /** player UUID -> per-chapter seen bitmasks (index = chapter, bit i = step i seen). */
    private final Map<UUID, List<Integer>> seen = new HashMap<>();

    public TechGuideSeenState() {
    }

    public static TechGuideSeenState get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    /** The player's seen bitmask for {@code chapter} (0 = nothing clicked yet). */
    public int mask(UUID player, int chapter) {
        List<Integer> masks = seen.get(player);
        return masks != null && chapter >= 0 && chapter < masks.size() ? masks.get(chapter) : 0;
    }

    /** Marks step {@code step} of {@code chapter} seen for the player (stops the completed-pulse). */
    public void markSeen(UUID player, int chapter, int step) {
        if (chapter < 0 || chapter >= TechGuide.CHAPTER_COUNT || step < 0 || step >= 16) {
            return;
        }
        List<Integer> masks = seen.computeIfAbsent(player, k -> new ArrayList<>());
        while (masks.size() < TechGuide.CHAPTER_COUNT) {
            masks.add(0);
        }
        masks.set(chapter, masks.get(chapter) | (1 << step));
        setDirty();
    }

    /** POPIA/GDPR erasure: drop everything stored for a player. */
    public void forgetPlayer(UUID player) {
        if (seen.remove(player) != null) {
            setDirty();
        }
    }

    // --- persistence --------------------------------------------------------

    private record SeenEntry(String uuid, List<Integer> masks) {
        static final Codec<SeenEntry> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Codec.STRING.fieldOf("uuid").forGetter(SeenEntry::uuid),
                Codec.INT.listOf().fieldOf("masks").forGetter(SeenEntry::masks)
        ).apply(inst, SeenEntry::new));
    }

    private static Codec<TechGuideSeenState> codec() {
        return RecordCodecBuilder.create(inst -> inst.group(
                SeenEntry.CODEC.listOf().optionalFieldOf("seen", List.of()).forGetter(TechGuideSeenState::entries)
        ).apply(inst, TechGuideSeenState::fromData));
    }

    private List<SeenEntry> entries() {
        List<SeenEntry> out = new ArrayList<>();
        seen.forEach((uuid, masks) -> out.add(new SeenEntry(uuid.toString(), List.copyOf(masks))));
        return out;
    }

    private static TechGuideSeenState fromData(List<SeenEntry> entries) {
        TechGuideSeenState state = new TechGuideSeenState();
        for (SeenEntry e : entries) {
            try {
                state.seen.put(UUID.fromString(e.uuid()), new ArrayList<>(e.masks()));
            } catch (IllegalArgumentException ignored) {
                // skip malformed UUID rows
            }
        }
        return state;
    }
}
