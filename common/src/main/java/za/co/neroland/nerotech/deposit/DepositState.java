package za.co.neroland.nerotech.deposit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import za.co.neroland.nerolandcore.data.SavedDataCompat;
import za.co.neroland.nerolandcore.data.SavedDataRecovery;

import za.co.neroland.nerotech.NeroTechCommon;

/**
 * Depletion records for the Deep Core Drill's virtual deposits, one store per server ({@code nerotech:deposits},
 * overworld saved data, through Core's {@link SavedDataRecovery}). Keyed {@code <dimension id>|<chunk long>}
 * to {@code remaining}, {@code capacity} and the game time it was last touched.
 *
 * <p><b>World data only:</b> no player, no owner, no name. An entry exists only for a chunk that has been
 * drilled and has not yet recovered: recovery is computed on read
 * ({@code remaining + elapsed * recoveryPerDay / 24000}, capped at capacity) and a fully recovered entry is
 * removed on that read or by the bounded daily {@link #prune}. A store written under another
 * {@link DepositSeed#SEED_VERSION} is discarded on load (its chunks reset to full).
 */
public final class DepositState extends SavedData {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(NeroTechCommon.MOD_ID, "deposits");

    public static final SavedDataType<DepositState> TYPE =
            SavedDataCompat.type(ID, DepositState::new, codec());

    /** Ticks in one in-game day (the recovery rate's unit). */
    public static final long DAY_TICKS = 24_000L;

    private final Map<String, Depletion> records = new HashMap<>();

    public DepositState() {
    }

    /** The store for this server, through Core's crash-safe recovery guard. */
    public static DepositState get(MinecraftServer server) {
        return SavedDataRecovery.get(server.overworld(), TYPE, DepositState::new, ID.toString());
    }

    /** {@code <dimension id>|<packed chunk>} (the vanilla chunk packing: x low, z high). */
    public static String key(String dimensionId, int chunkX, int chunkZ) {
        long packed = (chunkX & 0xFFFFFFFFL) | ((chunkZ & 0xFFFFFFFFL) << 32);
        return dimensionId + "|" + packed;
    }

    private static final class Depletion {
        private int remaining;
        private final int capacity;
        private long lastTouched;

        Depletion(int remaining, int capacity, long lastTouched) {
            this.remaining = remaining;
            this.capacity = capacity;
            this.lastTouched = lastTouched;
        }

        int recovered(long now, int recoveryPerDay) {
            long elapsed = Math.max(0L, now - this.lastTouched);
            long gained = recoveryPerDay <= 0 ? 0L : elapsed * recoveryPerDay / DAY_TICKS;
            return (int) Math.min(this.capacity, this.remaining + gained);
        }
    }

    /**
     * Units left in a chunk's deposit at {@code now}. An untouched chunk is full ({@code capacity}); a
     * fully recovered record is pruned here.
     */
    public int remaining(String key, int capacity, long now, int recoveryPerDay) {
        Depletion r = this.records.get(key);
        if (r == null) {
            return capacity;
        }
        int value = r.recovered(now, recoveryPerDay);
        if (value >= r.capacity) {
            this.records.remove(key);
            setDirty();
        }
        return value;
    }

    /** Like {@link #remaining} but never modifies the store (the Survey Scanner's read). */
    public int peek(String key, int capacity, long now, int recoveryPerDay) {
        Depletion r = this.records.get(key);
        return r == null ? capacity : r.recovered(now, recoveryPerDay);
    }

    /**
     * Take up to {@code units} from a chunk's deposit; returns how many were taken. The record is created on
     * the first extraction and keeps the capacity it was created with.
     */
    public int extract(String key, int capacity, int units, long now, int recoveryPerDay) {
        if (units <= 0 || capacity <= 0) {
            return 0;
        }
        Depletion r = this.records.get(key);
        int cap = r == null ? capacity : r.capacity;
        int current = r == null ? capacity : r.recovered(now, recoveryPerDay);
        int taken = Math.min(units, current);
        if (taken <= 0) {
            return 0;
        }
        if (r == null) {
            this.records.put(key, new Depletion(current - taken, cap, now));
        } else {
            r.remaining = current - taken;
            r.lastTouched = now;
        }
        setDirty();
        return taken;
    }

    /** Remove up to {@code limit} records that have recovered to full; returns how many were removed. */
    public int prune(long now, int recoveryPerDay, int limit) {
        int removed = 0;
        Iterator<Depletion> it = this.records.values().iterator();
        while (it.hasNext() && removed < limit) {
            Depletion r = it.next();
            if (r.recovered(now, recoveryPerDay) >= r.capacity) {
                it.remove();
                removed++;
            }
        }
        if (removed > 0) {
            setDirty();
        }
        return removed;
    }

    /** Number of stored records (only chunks drilled and not yet recovered). */
    public int size() {
        return this.records.size();
    }

    // --- persistence --------------------------------------------------------

    private record Entry(String key, int remaining, int capacity, long lastTouched) {
        static final Codec<Entry> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Codec.STRING.fieldOf("key").forGetter(Entry::key),
                Codec.INT.fieldOf("remaining").forGetter(Entry::remaining),
                Codec.INT.fieldOf("capacity").forGetter(Entry::capacity),
                Codec.LONG.fieldOf("lastTouched").forGetter(Entry::lastTouched)
        ).apply(inst, Entry::new));
    }

    /** The store's codec (package-visible for tests). */
    static Codec<DepositState> codec() {
        return RecordCodecBuilder.create(inst -> inst.group(
                Codec.INT.optionalFieldOf("seedVersion", DepositSeed.SEED_VERSION).forGetter(s -> DepositSeed.SEED_VERSION),
                Entry.CODEC.listOf().optionalFieldOf("entries", List.of()).forGetter(DepositState::entries)
        ).apply(inst, DepositState::fromData));
    }

    private List<Entry> entries() {
        List<Entry> out = new ArrayList<>();
        this.records.forEach((k, r) -> out.add(new Entry(k, r.remaining, r.capacity, r.lastTouched)));
        return out;
    }

    private static DepositState fromData(int seedVersion, List<Entry> entries) {
        DepositState state = new DepositState();
        if (seedVersion != DepositSeed.SEED_VERSION) {
            state.setDirty(); // another seeding function: its depletion maps to different deposits; reset
            return state;
        }
        for (Entry e : entries) {
            if (e.capacity() > 0) {
                state.records.put(e.key(), new Depletion(Math.max(0, Math.min(e.remaining(), e.capacity())),
                        e.capacity(), e.lastTouched()));
            }
        }
        return state;
    }
}
