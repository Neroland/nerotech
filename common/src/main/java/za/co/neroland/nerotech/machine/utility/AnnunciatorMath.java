package za.co.neroland.nerotech.machine.utility;

import za.co.neroland.nerotech.machine.MachineAlarms;

/**
 * Pure rules of the Annunciator Panel (decision 013): how the masks of the machines touching it combine,
 * what a comparator reads for the selected channel, and which lamps blink as new. No Minecraft types.
 */
public final class AnnunciatorMath {

    /** All five alarm bits. */
    public static final int ALL = (1 << MachineAlarms.COUNT) - 1;
    /** Channel 0: any alarm (comparator strength 3 per lit alarm). Channels 1..5: one alarm each. */
    public static final int CHANNEL_ANY = 0;
    /** Number of comparator channels (any + one per alarm). */
    public static final int CHANNEL_COUNT = MachineAlarms.COUNT + 1;
    /** A lamp blinks for this many ticks after its alarm lights (art direction §3). */
    public static final int FRESH_TICKS = 100;
    /** Comparator strength per lit alarm on the any channel. */
    public static final int STRENGTH_PER_ALARM = 3;

    private AnnunciatorMath() {
    }

    /** The OR of the neighbours' masks, limited to the five alarm bits. */
    public static int aggregate(int... masks) {
        int out = 0;
        for (int mask : masks) {
            out |= mask;
        }
        return out & ALL;
    }

    /**
     * Comparator strength for {@code mask} on {@code channel}: on the any channel 3 per lit alarm
     * (0, 3, 6, 9, 12, 15); on an alarm channel 15 while that alarm is lit, else 0.
     */
    public static int comparator(int mask, int channel) {
        int lit = mask & ALL;
        if (channel <= CHANNEL_ANY || channel >= CHANNEL_COUNT) {
            return Math.min(15, STRENGTH_PER_ALARM * Integer.bitCount(lit));
        }
        return (lit & (1 << (channel - 1))) != 0 ? 15 : 0;
    }

    /** The bits lit in {@code now} that were dark in {@code previous}. */
    public static int risen(int previous, int now) {
        return now & ~previous & ALL;
    }

    /** The channel after {@code channel}, wrapping back to any. */
    public static int nextChannel(int channel) {
        return Math.floorMod(channel + 1, CHANNEL_COUNT);
    }

    /** A valid channel for any stored value (bad data falls back to any). */
    public static int sanitizeChannel(int channel) {
        return channel < 0 || channel >= CHANNEL_COUNT ? CHANNEL_ANY : channel;
    }

    /** Stable id of a channel: {@code any} or the alarm id. */
    public static String channelId(int channel) {
        int c = sanitizeChannel(channel);
        return c == CHANNEL_ANY ? "any" : MachineAlarms.IDS[c - 1];
    }

    /**
     * Record the rise time of every bit that just lit in {@code risenAt} (one slot per alarm) and return
     * the fresh mask: lit bits that rose within the last {@link #FRESH_TICKS} ticks.
     */
    public static int fresh(int previous, int now, long[] risenAt, long gameTime) {
        int up = risen(previous, now);
        int out = 0;
        for (int bit = 0; bit < MachineAlarms.COUNT && bit < risenAt.length; bit++) {
            int flag = 1 << bit;
            if ((up & flag) != 0) {
                risenAt[bit] = gameTime;
            }
            if ((now & flag) != 0 && gameTime - risenAt[bit] < FRESH_TICKS) {
                out |= flag;
            }
        }
        return out;
    }
}
