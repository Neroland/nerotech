package za.co.neroland.nerotech.pollution;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.data.SavedDataCompat;
import za.co.neroland.nerolandcore.data.SavedDataRecovery;

import za.co.neroland.nerotech.NeroTechCommon;

/**
 * Server-authoritative, persistent pollution store. Pollution is aggregated by <b>region</b> (a coarse
 * 64×64-block grid keyed by a packed long), never per-block, and updated periodically — so the global
 * decay sweep iterates a small map, never a block scan.
 *
 * <p>Since 0.8.0 (store schema 2) regions are kept per <b>dimension</b>: a reading recorded on Mars never
 * appears at the same x/z in the overworld. A schema-1 save (one flat {@code regions} list) loads into
 * {@code minecraft:overworld}, where every reading of that era was assumed to belong; the next save
 * writes schema 2.
 *
 * <p>Privacy (POPIA/GDPR): the per-player attribution map is populated <b>only</b> when the server
 * opts in ({@code pollutionPerPlayerAttribution}); it is keyed by UUID (no names), retention-pruned,
 * and cleared per-player through the shared data-erasure hook ({@link #forgetPlayer(UUID)}).
 */
public final class PollutionState extends SavedData {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(NeroTechCommon.MOD_ID, "pollution");

    public static final SavedDataType<PollutionState> TYPE =
            SavedDataCompat.type(ID, PollutionState::new, codec());

    /** Store format: 1 = flat {@code regions} list (pre-0.8.0), 2 = per-dimension {@code dimensions}. */
    public static final int SCHEMA = 2;

    /** Dimension a schema-1 save's flat region list (and the legacy dimensionless overloads) land in. */
    public static final String OVERWORLD = "minecraft:overworld";

    /** dimension id -> (regionKey -> aggregate pollution). */
    private final Map<String, Map<Long, Integer>> regions = new HashMap<>();
    /** player UUID -> attributed total + last-updated epoch day (only used when attribution is on). */
    private final Map<UUID, Attrib> attribution = new HashMap<>();

    public PollutionState() {
    }

    /**
     * The store for this server (overworld saved data), through Core's crash-safe
     * {@link SavedDataRecovery} so a corrupt file degrades to the last backup or a fresh store
     * instead of crashing every tick that touches it.
     */
    public static PollutionState get(MinecraftServer server) {
        return SavedDataRecovery.get(server.overworld(), TYPE, PollutionState::new, ID.toString());
    }

    // --- regional aggregate (per dimension) ---------------------------------

    public void addRegion(String dimension, long regionKey, int amount) {
        if (amount <= 0) {
            return;
        }
        regions.computeIfAbsent(dimension, k -> new HashMap<>()).merge(regionKey, amount, Integer::sum);
        setDirty();
    }

    public int region(String dimension, long regionKey) {
        Map<Long, Integer> dim = regions.get(dimension);
        return dim == null ? 0 : dim.getOrDefault(regionKey, 0);
    }

    /** Remove up to {@code amount} pollution from one region; returns what was actually removed. */
    public int takeRegion(String dimension, long regionKey, int amount) {
        if (amount <= 0) {
            return 0;
        }
        Map<Long, Integer> dim = regions.get(dimension);
        if (dim == null) {
            return 0;
        }
        int current = dim.getOrDefault(regionKey, 0);
        if (current <= 0) {
            return 0;
        }
        int removed = Math.min(current, amount);
        int next = current - removed;
        if (next <= 0) {
            dim.remove(regionKey);
            if (dim.isEmpty()) {
                regions.remove(dimension);
            }
        } else {
            dim.put(regionKey, next);
        }
        setDirty();
        return removed;
    }

    /** Read-only copy of one dimension's regions (regionKey -> amount); empty when it has none. */
    public Map<Long, Integer> regionsIn(String dimension) {
        Map<Long, Integer> dim = regions.get(dimension);
        return dim == null ? Map.of() : Map.copyOf(dim);
    }

    /** Dimensions that currently hold any pollution. */
    public java.util.Set<String> dimensions() {
        return java.util.Set.copyOf(regions.keySet());
    }

    /** Overworld shorthand kept for existing callers: {@link #addRegion(String, long, int)} in the overworld. */
    public void addRegion(long regionKey, int amount) {
        addRegion(OVERWORLD, regionKey, amount);
    }

    /** Overworld shorthand kept for existing callers. */
    public int region(long regionKey) {
        return region(OVERWORLD, regionKey);
    }

    /** Overworld shorthand kept for existing callers. */
    public int takeRegion(long regionKey, int amount) {
        return takeRegion(OVERWORLD, regionKey, amount);
    }

    /**
     * Decay every region of every dimension by {@code amount}, dropping emptied regions. When
     * {@code threshold > 0}, every region that fell from at-or-above to below it is reported to
     * {@code recovered} with its dimension (the "region recovered" crossing).
     */
    public void decayAll(int amount, int threshold, @Nullable BiConsumer<String, Long> recovered) {
        if (amount <= 0 || regions.isEmpty()) {
            return;
        }
        boolean changed = false;
        Iterator<Map.Entry<String, Map<Long, Integer>>> dims = regions.entrySet().iterator();
        while (dims.hasNext()) {
            Map.Entry<String, Map<Long, Integer>> dim = dims.next();
            Iterator<Map.Entry<Long, Integer>> it = dim.getValue().entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Long, Integer> e = it.next();
                int before = e.getValue();
                int next = before - amount;
                if (threshold > 0 && recovered != null && before >= threshold && next < threshold) {
                    recovered.accept(dim.getKey(), e.getKey());
                }
                if (next <= 0) {
                    it.remove();
                } else {
                    e.setValue(next);
                }
                changed = true;
            }
            if (dim.getValue().isEmpty()) {
                dims.remove();
            }
        }
        if (changed) {
            setDirty();
        }
    }

    /**
     * Decay every region in every dimension, reporting recoveries by region key only. Kept for existing
     * callers; prefer {@link #decayAll}, which also names the dimension.
     */
    public void decay(int amount, int threshold, @Nullable java.util.function.LongConsumer recovered) {
        decayAll(amount, threshold, recovered == null ? null : (dim, key) -> recovered.accept(key));
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

    /**
     * Epoch day the player's attribution row was last updated, or {@code -1} when no row is stored.
     * Read-only; used by the per-player data export.
     */
    public long attributedLastDay(UUID player) {
        Attrib a = attribution.get(player);
        return a == null ? -1L : a.lastDay;
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

    private record DimensionEntry(String dimension, List<RegionEntry> regions) {
        static final Codec<DimensionEntry> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Codec.STRING.fieldOf("dimension").forGetter(DimensionEntry::dimension),
                RegionEntry.CODEC.listOf().optionalFieldOf("regions", List.of()).forGetter(DimensionEntry::regions)
        ).apply(inst, DimensionEntry::new));
    }

    /** The store's codec (package-visible so the schema migration can be tested without a server). */
    static Codec<PollutionState> codec() {
        return RecordCodecBuilder.create(inst -> inst.group(
                Codec.INT.optionalFieldOf("schema", 1).forGetter(s -> SCHEMA),
                // Schema 1 only: the flat, dimensionless list. Never written again (empty is omitted).
                RegionEntry.CODEC.listOf().optionalFieldOf("regions", List.of()).forGetter(s -> List.of()),
                DimensionEntry.CODEC.listOf().optionalFieldOf("dimensions", List.of()).forGetter(PollutionState::dimensionEntries),
                AttribEntry.CODEC.listOf().optionalFieldOf("attribution", List.of()).forGetter(PollutionState::attribEntries)
        ).apply(inst, PollutionState::fromData));
    }

    private List<DimensionEntry> dimensionEntries() {
        List<DimensionEntry> out = new ArrayList<>();
        regions.forEach((dim, map) -> {
            List<RegionEntry> list = new ArrayList<>();
            map.forEach((k, v) -> list.add(new RegionEntry(k, v)));
            out.add(new DimensionEntry(dim, list));
        });
        return out;
    }

    private List<AttribEntry> attribEntries() {
        List<AttribEntry> out = new ArrayList<>();
        attribution.forEach((uuid, a) -> out.add(new AttribEntry(uuid.toString(), a.amount, a.lastDay)));
        return out;
    }

    private static PollutionState fromData(int schema, List<RegionEntry> legacyRegions, List<DimensionEntry> dimensions,
            List<AttribEntry> attribution) {
        PollutionState state = new PollutionState();
        // Migration: a schema-1 store had no dimension, so its readings land in the overworld.
        for (RegionEntry e : legacyRegions) {
            if (e.amount() > 0) {
                state.regions.computeIfAbsent(OVERWORLD, k -> new HashMap<>()).merge(e.key(), e.amount(), Integer::sum);
            }
        }
        for (DimensionEntry d : dimensions) {
            for (RegionEntry e : d.regions()) {
                if (e.amount() > 0) {
                    state.regions.computeIfAbsent(d.dimension(), k -> new HashMap<>())
                            .merge(e.key(), e.amount(), Integer::sum);
                }
            }
        }
        if (schema < SCHEMA && !legacyRegions.isEmpty()) {
            state.setDirty(); // rewrite in the new format at the next save
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
