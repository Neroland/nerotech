package za.co.neroland.nerotech.machine.utility;

import java.util.List;
import java.util.Locale;

/**
 * The keys the utility blocks write to disk beyond Core's energy/upgrade data, in one place so a test can
 * prove neither block persists anything about a player (POPIA/GDPR). The base machine's owner keys are
 * stripped from both blocks' saves: neither block ever records an owner.
 */
public final class UtilitySaveKeys {

    /** Base machine keys that would carry the placing player's UUID; both blocks drop them on save. */
    public static final List<String> STRIPPED_OWNER_KEYS = List.of("OwnerMost", "OwnerLeast");
    /** Keys the Induction Charger adds: none. Its budget, registration and charging flag are transient. */
    public static final List<String> CHARGER_KEYS = List.of();
    /** Keys the Annunciator Panel adds: the selected comparator channel (world data). */
    public static final List<String> PANEL_KEYS = List.of("AnChannel");
    /** Update-tag-only keys (client render sync; never on disk). */
    public static final List<String> SYNC_KEYS = List.of("IcCharging", "AnFresh", "AnChannel");

    private UtilitySaveKeys() {
    }

    /** Whether a key name looks like it holds player identity. */
    public static boolean looksPersonal(String key) {
        String k = key.toLowerCase(Locale.ROOT);
        return k.contains("uuid") || k.contains("owner") || k.contains("player") || k.contains("name")
                || k.endsWith("most") || k.endsWith("least");
    }
}
