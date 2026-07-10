package za.co.neroland.nerotech.machine;

import java.util.Locale;

/**
 * The Stage G analytics status — ONE word naming what currently limits a machine, shown as the
 * single status line in the per-machine Analytics tab and the Analytics Terminal dashboard.
 * The base {@link NeroTechMachineBlockEntity} defaults to {@link #RUNNING}/{@link #IDLE} from the
 * active flag; subclasses report the sharper causes via
 * {@link NeroTechMachineBlockEntity#reportStatus}.
 *
 * <p>Ordinals ride the analytics payloads (a single byte), so the order here is wire format —
 * append only. Colour hints stay client-side ({@code client.AnalyticsWidget}); this enum carries
 * only the lang key. Machine-scoped state only — no player data anywhere (POPIA/GDPR).
 */
public enum MachineStatus {

    /** Actively working (burning / processing / generating / scrubbing). */
    RUNNING,
    /** Nothing to do (no work queued, night for solar, clean region for the Remediator). */
    IDLE,
    /** No valid input — empty fuel/input slot, no matching recipe, missing filter cartridge. */
    STARVED,
    /** Output has nowhere to go — output slot or energy buffer full. */
    BLOCKED,
    /** Stalled by the heat throttle until the thermal model cools it. */
    THROTTLED,
    /** Not enough stored NE for the next work tick / operation. */
    NO_ENERGY,
    /** Fusion Reactor multiblock shell not (or no longer) formed. */
    UNFORMED;

    /** Stable view for ordinal decoding — never call {@code values()} per packet. */
    public static final MachineStatus[] VALUES = values();

    /** Decode a payload byte, clamping unknown ordinals to {@link #IDLE} (forward-compat). */
    public static MachineStatus byOrdinal(int ordinal) {
        return ordinal >= 0 && ordinal < VALUES.length ? VALUES[ordinal] : IDLE;
    }

    /** {@code nerotech.analytics.status.<lowercase name>} (see en_us.json). */
    public String translationKey() {
        return "nerotech.analytics.status." + name().toLowerCase(Locale.ROOT);
    }
}
