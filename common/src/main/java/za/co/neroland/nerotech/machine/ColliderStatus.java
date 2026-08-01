package za.co.neroland.nerotech.machine;

import java.util.Locale;

/**
 * The Accelerator Controller's beam status — the one-line story its GUI tells: why nothing is
 * circulating, or how the last run ended. Distinct from the shared {@link MachineStatus} analytics
 * enum (which the collider still reports into): this one is accelerator-specific and rides the menu's
 * synced gauges.
 *
 * <p>Ordinals ride {@code ContainerData}, so the order here is wire format — append only.
 */
public enum ColliderStatus {

    /** No guides reachable at all — the controller is not looking at an accelerator. */
    NO_PATH,
    /** Guides found, but the line never comes back: a particle would fly off the end. */
    OPEN_LOOP,
    /** A closed loop, waiting for a particle in the injection slot. */
    READY,
    /** Circulating and picking up speed at every guide. */
    ACCELERATING,
    /** Circulating on momentum — the buffer cannot pay for the boost, so the beam is bleeding speed. */
    COASTING,
    /** Too hot to inject; the thermal model has to bring it down first. */
    THROTTLED,
    /** Not enough stored NE to inject a particle. */
    NO_ENERGY,
    /** The collision succeeded but the output slot could not take the result. */
    BLOCKED,
    /** A collision resolved: both particles consumed, product in the output slot. */
    COLLIDED,
    /** Lost on a stretch that was too long for its speed (the gap rule). */
    FIZZLED_GAP,
    /** Crashed into a bend it was going too fast to take (the bend rule). */
    CRASHED_BEND;

    /** Stable view for ordinal decoding — never call {@code values()} per frame. */
    public static final ColliderStatus[] VALUES = values();

    /** Decode a synced ordinal, clamping unknown values to {@link #NO_PATH} (forward-compat). */
    public static ColliderStatus byOrdinal(int ordinal) {
        return ordinal >= 0 && ordinal < VALUES.length ? VALUES[ordinal] : NO_PATH;
    }

    /** {@code gui.nerotech.collider.status.<lowercase name>} (see en_us.json). */
    public String translationKey() {
        return "gui.nerotech.collider.status." + name().toLowerCase(Locale.ROOT);
    }
}
