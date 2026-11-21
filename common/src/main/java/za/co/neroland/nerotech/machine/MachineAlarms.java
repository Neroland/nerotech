package za.co.neroland.nerotech.machine;

/**
 * The five Annunciator alarm bits a machine exposes as {@code alarmMask()} (machine contract,
 * "Alarms"). Computed by the base from state it already has; read by the Annunciator Panel and the
 * link {@code alarm} event. Bit positions are wire format: append only. No player data.
 */
public final class MachineAlarms {

    /** HOT readout. */
    public static final int OVERHEAT = 1;
    /** STALL from {@code STARVED} or {@code NO_ENERGY}. */
    public static final int STARVED = 1 << 1;
    /** Output blocked ({@code BLOCKED}). */
    public static final int FULL = 1 << 2;
    /** Pollution emitted in the last {@value #POLLUTION_WINDOW_TICKS} ticks above the configured threshold. */
    public static final int POLLUTING = 1 << 3;
    /** A {@code MachineFailureEvents} stage of 1 or more is active. */
    public static final int FAILING = 1 << 4;

    /** Number of alarm bits. */
    public static final int COUNT = 5;

    /** The pollution look-back window (5 s). */
    public static final int POLLUTION_WINDOW_TICKS = 100;

    /** Stable lower-case ids, indexed by bit position (link event payloads, lang keys). */
    public static final String[] IDS = {"overheat", "starved", "full", "polluting", "failing"};

    private MachineAlarms() {
    }

    /**
     * The mask for one machine.
     *
     * @param readout        the machine's current readout
     * @param status         its analytics status
     * @param recentPollution pollution emitted in the last window
     * @param threshold      the configured threshold (emissions strictly above it raise the alarm; a
     *                       negative threshold disables the alarm)
     * @param failureStage   the machine's active failure stage (0 = none)
     */
    public static int mask(Readout readout, MachineStatus status, int recentPollution, int threshold,
            int failureStage) {
        int mask = 0;
        if (readout == Readout.HOT) {
            mask |= OVERHEAT;
        }
        if (status == MachineStatus.STARVED || status == MachineStatus.NO_ENERGY) {
            mask |= STARVED;
        }
        if (status == MachineStatus.BLOCKED) {
            mask |= FULL;
        }
        if (threshold >= 0 && recentPollution > threshold) {
            mask |= POLLUTING;
        }
        if (failureStage >= 1) {
            mask |= FAILING;
        }
        return mask;
    }

    /**
     * The active failure stage after a {@code MachineFailureEvents} crossing: a rising edge escalates to
     * {@code stage} (never de-escalates a worse active stage); a falling edge clears the alarm when the
     * stage it ends is the worst one active, and leaves a worse stage standing. Pure, so the
     * {@link #FAILING} wiring is unit-testable.
     *
     * @param current the machine's active stage (0 = none)
     * @param stage   the stage that crossed
     * @param rising  {@code true} when the stage was entered, {@code false} when it ended
     */
    public static int nextFailureStage(int current, int stage, boolean rising) {
        int now = Math.max(0, current);
        if (rising) {
            return Math.max(now, Math.max(0, stage));
        }
        return stage >= now ? 0 : now;
    }

    /** Whether {@code mask} has {@code bit} set. */
    public static boolean has(int mask, int bit) {
        return (mask & bit) != 0;
    }
}
