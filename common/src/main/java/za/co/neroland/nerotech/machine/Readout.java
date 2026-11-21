package za.co.neroland.nerotech.machine;

/**
 * The four-state readout every 0.8.0 machine shows on its status lamp and BER (art direction §2):
 * one word a player can read from across the room. Derived from the analytics {@link MachineStatus}
 * plus the synced heat bucket, so the lamp, the GUI status line and the analytics tab never disagree.
 *
 * <p>Ordinals ride the block-entity update tag and the art kit's lamp constants
 * ({@code StatusLampRenderer.REST/WORK/STALL/HOT}), so the order is wire format: append only.
 * Machine-scoped state only, no player data.
 */
public enum Readout {

    /** Idle: nothing queued. Steady dim lamp, parked pose. */
    REST,
    /** Working. Steady bright lamp, signature motion. */
    WORK,
    /** Stalled: starved, blocked, unpaid, unformed or out of energy. Double blink, frozen mid-stroke. */
    STALL,
    /** Overheating: throttled, or heat bucket at or above {@link #HOT_FROM_BUCKET}. Heat colour. */
    HOT;

    /** Heat bucket (of {@code NeroTechMachineBlockEntity.HEAT_SYNC_BUCKETS}) from which a machine reads HOT. */
    public static final int HOT_FROM_BUCKET = 4;

    private static final Readout[] VALUES = values();

    /**
     * The readout for a status and heat bucket. HOT wins over everything (an overheating machine must
     * never look merely stalled or busy), then the status decides: {@code RUNNING} is WORK, {@code IDLE}
     * is REST, every other status is STALL.
     */
    public static Readout of(MachineStatus status, int heatBucket) {
        if (status == MachineStatus.THROTTLED || heatBucket >= HOT_FROM_BUCKET) {
            return HOT;
        }
        if (status == null) {
            return REST;
        }
        return switch (status) {
            case RUNNING -> WORK;
            case IDLE -> REST;
            default -> STALL;
        };
    }

    /** Whether the lamp is fully dark: the machine has no energy for its next step (art direction §2). */
    public static boolean dark(MachineStatus status) {
        return status == MachineStatus.NO_ENERGY;
    }

    /** Decode a synced ordinal, clamping unknown values to {@link #REST} (forward-compat). */
    public static Readout byOrdinal(int ordinal) {
        return ordinal >= 0 && ordinal < VALUES.length ? VALUES[ordinal] : REST;
    }
}
