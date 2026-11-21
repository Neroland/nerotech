package za.co.neroland.nerotech.link;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerotech.machine.MachineAlarms;
import za.co.neroland.nerotech.machine.Readout;

/**
 * The pure half of the {@code nearby} section and the {@code alarm} / {@code meltdown} events (link
 * schema 2, decision 007): the reach rule, the state word, the alarm ids, the percentages and the
 * payload shapes. Nothing here touches the world, so every rule is unit-testable.
 *
 * <p><b>What a payload may carry:</b> a block id, a state word, alarm ids, three percentages and an
 * opaque handle. Never a coordinate, a direction, a distance, a dimension, an owner or a player.
 */
final class NearbyFormat {

    /** Reach, in blocks, on each axis (a box, horizontal and vertical). */
    static final int RADIUS = 128;
    /** Most entries in one {@code nearby} response. */
    static final int MAX_ENTRIES = 128;
    /** Loaded chunks scanned around the player: {@code (2 * CHUNK_RADIUS + 1)^2}, i.e. 9 x 9. */
    static final int CHUNK_RADIUS = 4;

    /** Alarm priority when one change raises several bits: the single id an event carries. */
    private static final int[] PRIORITY = {
            MachineAlarms.FAILING, MachineAlarms.OVERHEAT, MachineAlarms.FULL,
            MachineAlarms.STARVED, MachineAlarms.POLLUTING};

    private NearbyFormat() {
    }

    /** Whether {@code (x, y, z)} lies inside the {@link #RADIUS} box around {@code (ox, oy, oz)}. */
    static boolean inReach(int ox, int oy, int oz, int x, int y, int z) {
        return Math.abs((long) x - ox) <= RADIUS && Math.abs((long) y - oy) <= RADIUS
                && Math.abs((long) z - oz) <= RADIUS;
    }

    /**
     * The one reach rule {@code nearby} and the {@code alarm} / {@code meltdown} events share (review L5):
     * inside the {@link #RADIUS} box AND inside the {@code (2 * CHUNK_RADIUS + 1)^2} chunk square
     * {@code nearby} scans, so an event never reaches a player for a machine {@code nearby} would not list.
     * Walls and claims do not block it: it is what stands within reach, seen or not.
     */
    static boolean visible(int ox, int oy, int oz, int x, int y, int z) {
        return inReach(ox, oy, oz, x, y, z) && Math.abs((x >> 4) - (ox >> 4)) <= CHUNK_RADIUS
                && Math.abs((z >> 4) - (oz >> 4)) <= CHUNK_RADIUS;
    }

    /**
     * Shuffle the whole in-reach set, then cut it to {@code max} (review M1): list order, handle indices and
     * which entries a truncation keeps then carry no direction or chunk order. Mutates {@code found}.
     *
     * @return whether entries were cut
     */
    static <T> boolean shuffleAndCap(java.util.List<T> found, java.util.Random random, int max) {
        java.util.Collections.shuffle(found, random);
        if (found.size() <= max) {
            return false;
        }
        found.subList(max, found.size()).clear();
        return true;
    }

    /** Event topic of a bystander's copy: the bridge's push filter never pushes it (see {@link #bystanderNotice}). */
    static final String BYSTANDER_TOPIC = "alerts";
    /** The {@code event} verb of a bystander's copy; anything but {@code raised} is never pushed. */
    static final String BYSTANDER_VERB = "nearby";

    /**
     * A bystander's (non-owner's) copy of an {@code alarm} or {@code meltdown} body (review M2, decision 007
     * amendment). It is published on topic {@link #BYSTANDER_TOPIC} with {@code event} =
     * {@link #BYSTANDER_VERB}: the bridge pushes an {@code alerts} event only when its {@code event} is absent
     * or {@code raised}, so this copy reaches only a live socket of that player and is never a phone push.
     * It carries no {@code _notify}. Only a recorded owner's copy, on the {@code alarm} / {@code meltdown}
     * topic, is push-eligible.
     *
     * @param source {@code alarm} or {@code meltdown}
     */
    static JsonObject bystanderNotice(String source, JsonObject body) {
        JsonObject out = new JsonObject();
        out.addProperty("event", BYSTANDER_VERB);
        out.addProperty("source", source);
        for (java.util.Map.Entry<String, com.google.gson.JsonElement> e : body.entrySet()) {
            if (!e.getKey().startsWith("_") && !"event".equals(e.getKey()) && !"source".equals(e.getKey())) {
                out.add(e.getKey(), e.getValue().deepCopy());
            }
        }
        return out;
    }

    /**
     * The state word: {@code idle|working|starved|overheating|failing|full}. Failing wins, then the
     * readout; a stalled machine whose output is blocked reads {@code full}.
     */
    static String state(@Nullable Readout readout, int alarmMask) {
        if (MachineAlarms.has(alarmMask, MachineAlarms.FAILING)) {
            return "failing";
        }
        Readout r = readout == null ? Readout.REST : readout;
        return switch (r) {
            case HOT -> "overheating";
            case WORK -> "working";
            case STALL -> MachineAlarms.has(alarmMask, MachineAlarms.FULL) ? "full" : "starved";
            case REST -> MachineAlarms.has(alarmMask, MachineAlarms.FULL) ? "full" : "idle";
        };
    }

    /** The alarm ids set in {@code mask}, in bit order. */
    static JsonArray alarms(int mask) {
        JsonArray out = new JsonArray();
        for (int bit = 0; bit < MachineAlarms.COUNT && bit < MachineAlarms.IDS.length; bit++) {
            if ((mask & (1 << bit)) != 0) {
                out.add(MachineAlarms.IDS[bit]);
            }
        }
        return out;
    }

    /** The one alarm id an event reports for newly raised bits, or null when none were raised. */
    @Nullable
    static String firstAlarm(int gained) {
        for (int bit : PRIORITY) {
            if ((gained & bit) != 0) {
                return MachineAlarms.IDS[Integer.numberOfTrailingZeros(bit)];
            }
        }
        return null;
    }

    /** Permille (0..1000) to a whole percentage, clamped to 0..100. */
    static int pct(int permille) {
        return Math.max(0, Math.min(100, Math.round(permille / 10.0F)));
    }

    /** {@code amount / capacity} as permille, or null when there is no tank. */
    @Nullable
    static Integer permille(long amount, long capacity) {
        if (capacity <= 0L) {
            return null;
        }
        return (int) Math.max(0L, Math.min(1000L, amount * 1000L / capacity));
    }

    /**
     * One {@code nearby} entry.
     *
     * @param reportAlarms false for machines whose mask only repeats their neighbours' (the Annunciator
     *                     Panel): they list no alarms, and their state ignores the alarm bits
     */
    static JsonObject entry(String type, @Nullable Readout readout, int alarmMask, int energyPermille,
            int heatPermille, @Nullable Integer tankPermille, String handle, boolean reportAlarms) {
        int mask = reportAlarms ? alarmMask : 0;
        JsonObject out = new JsonObject();
        out.addProperty("type", type);
        out.addProperty("state", state(readout, mask));
        out.add("alarms", alarms(mask));
        out.addProperty("energyPct", pct(energyPermille));
        out.addProperty("heatPct", pct(heatPermille));
        if (tankPermille == null) {
            out.add("tankPct", com.google.gson.JsonNull.INSTANCE);
        } else {
            out.addProperty("tankPct", pct(tankPermille));
        }
        out.addProperty("h", handle);
        return out;
    }

    /** The {@code alarm} event body: the machine type and one alarm id, nothing else. */
    static JsonObject alarmPayload(String type, String alarm) {
        JsonObject out = new JsonObject();
        out.addProperty("type", type);
        out.addProperty("alarm", alarm);
        return out;
    }

    /**
     * The {@code meltdown} event body: the kind and the shell size. Since schema 2 it carries no
     * coordinates (it used to be broadcast with x/y/z; see the changelog).
     */
    static JsonObject meltdownPayload(String kind, int shellSize) {
        JsonObject out = new JsonObject();
        out.addProperty("kind", kind);
        out.addProperty("shell", shellSize);
        return out;
    }
}
