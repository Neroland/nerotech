package za.co.neroland.nerotech.pollution;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
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
 * Server-authoritative, persistent pollution store. Pollution is aggregated by <b>region</b> (a coarse
 * 64×64-block grid keyed by a packed long), never per-block, and updated periodically — so the global
 * decay sweep iterates a small map, never a block scan.
 *
 * <p>Privacy (POPIA/GDPR): the per-player attribution map is populated <b>only</b> when the server
 * opts in ({@code pollutionPerPlayerAttribution}); it is keyed by UUID (no names), retention-pruned,
 * and cleared per-player through the shared data-erasure hook ({@link #forgetPlayer(UUID)}).
 */
public final class PollutionState extends SavedData {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(NeroTechCommon.MOD_ID, "pollution");

    public static final SavedDataType<PollutionState> TYPE =
            new SavedDataType<>(ID, PollutionState::new, codec(), null);

    /** regionKey -> aggregate pollution. */
    private final Map<Long, Integer> regions = new HashMap<>();
    /** player UUID -> attributed total + last-updated epoch day (only used when attribution is on). */
    private final Map<UUID, Attrib> attribution = new HashMap<>();

    public PollutionState() {
    }

    public static PollutionState get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    // --- regional aggregate -------------------------------------------------

    public void addRegion(long regionKey, int amount) {
        if (amount <= 0) {
            return;
        }
        regions.merge(regionKey, amount, Integer::sum);
        setDirty();
    }

    public int region(long regionKey) {
        return regions.getOrDefault(regionKey, 0);
    }

    /** Decay every region by {@code amount}, dropping emptied regions. */
    public void decay(int amount) {
        if (amount <= 0 || regions.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<Long, Integer>> it = regions.entrySet().iterator();
        boolean changed = false;
        while (it.hasNext()) {
            Map.Entry<Long, Integer> e = it.next();
            int next = e.getValue() - amount;
            if (next <= 0) {
                it.remove();
            } else {
                e.setValue(next);
            }
            changed = true;
        }
        if (changed) {
            setDirty();
        }
    }

    // --- per-player attribution (opt-in only) -------------------------------

    public void attribute(UUID player, int amount, long epochDay) {
        if (amount <= 0) {
            return;
        }
        Attrib a = attribution.computeIfAbsent(player, k -> new Attrib(0, epochDay));
        a.amount += amount;
        a.lastDay = epochDay;
        setDirty();
    }

    public int attributed(UUID player) {
        Attrib a = attribution.get(player);
        return a == null ? 0 : a.amount;
    }

    /** Prune attribution rows untouched for longer than {@code retentionDays} (0 disables). */
    public void pruneStale(int retentionDays, long nowDay) {
        if (retentionDays <= 0 || attribution.isEmpty()) {
            return;
        }
        boolean changed = attribution.values().removeIf(a -> nowDay - a.lastDay > retentionDays);
        if (changed) {
            setDirty();
        }
    }

    /** POPIA/GDPR erasure: drop everything stored for a player. */
    public void forgetPlayer(UUID player) {
        if (attribution.remove(player) != null) {
            setDirty();
        }
    }

    // --- persistence --------------------------------------------------------

    private static final class Attrib {
        private int amount;
        private long lastDay;

        Attrib(int amount, long lastDay) {
            this.amount = amount;
            this.lastDay = lastDay;
        }
    }

    private record RegionEntry(long key, int amount) {
        static final Codec<RegionEntry> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Codec.LONG.fieldOf("key").forGetter(RegionEntry::key),
                Codec.INT.fieldOf("amount").forGetter(RegionEntry::amount)
        ).apply(inst, RegionEntry::new));
    }

    private record AttribEntry(String uuid, int amount, long lastDay) {
        static final Codec<AttribEntry> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Codec.STRING.fieldOf("uuid").forGetter(AttribEntry::uuid),
                Codec.INT.fieldOf("amount").forGetter(AttribEntry::amount),
                Codec.LONG.fieldOf("lastDay").forGetter(AttribEntry::lastDay)
        ).apply(inst, AttribEntry::new));
    }

    private static Codec<PollutionState> codec() {
        return RecordCodecBuilder.create(inst -> inst.group(
                RegionEntry.CODEC.listOf().optionalFieldOf("regions", List.of()).forGetter(PollutionState::regionEntries),
                AttribEntry.CODEC.listOf().optionalFieldOf("attribution", List.of()).forGetter(PollutionState::attribEntries)
        ).apply(inst, PollutionState::fromData));
    }

    private List<RegionEntry> regionEntries() {
        List<RegionEntry> out = new ArrayList<>();
        regions.forEach((k, v) -> out.add(new RegionEntry(k, v)));
        return out;
    }

    private List<AttribEntry> attribEntries() {
        List<AttribEntry> out = new ArrayList<>();
        attribution.forEach((uuid, a) -> out.add(new AttribEntry(uuid.toString(), a.amount, a.lastDay)));
        return out;
    }

    private static PollutionState fromData(List<RegionEntry> regions, List<AttribEntry> attribution) {
        PollutionState state = new PollutionState();
        for (RegionEntry e : regions) {
            state.regions.put(e.key(), e.amount());
        }
        for (AttribEntry e : attribution) {
            try {
                state.attribution.put(UUID.fromString(e.uuid()), new Attrib(e.amount(), e.lastDay()));
            } catch (IllegalArgumentException ignored) {
                // skip malformed UUID rows
            }
        }
        return state;
    }
}
