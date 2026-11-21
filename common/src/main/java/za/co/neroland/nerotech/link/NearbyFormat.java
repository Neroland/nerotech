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
