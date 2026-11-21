package za.co.neroland.nerotech.machine.utility;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** POPIA/GDPR: neither utility block persists a UUID or any other player key. Pure JVM. */
class UtilitySaveKeysTest {

    @Test
    void chargerAddsNoKeysAtAll() {
        assertTrue(UtilitySaveKeys.CHARGER_KEYS.isEmpty());
    }

    @Test
    void noOwnKeyLooksPersonal() {
        for (String key : UtilitySaveKeys.CHARGER_KEYS) {
            assertFalse(UtilitySaveKeys.looksPersonal(key), key);
        }
        for (String key : UtilitySaveKeys.PANEL_KEYS) {
            assertFalse(UtilitySaveKeys.looksPersonal(key), key);
        }
        for (String key : UtilitySaveKeys.SYNC_KEYS) {
            assertFalse(UtilitySaveKeys.looksPersonal(key), key);
        }
    }

    @Test
    void theBaseOwnerKeysAreTheOnesStripped() {
        assertTrue(UtilitySaveKeys.STRIPPED_OWNER_KEYS.contains("OwnerMost"));
        assertTrue(UtilitySaveKeys.STRIPPED_OWNER_KEYS.contains("OwnerLeast"));
        for (String key : UtilitySaveKeys.STRIPPED_OWNER_KEYS) {
            assertTrue(UtilitySaveKeys.looksPersonal(key), key);
        }
    }

    @Test
    void detectorCatchesUuidShapedNames() {
        assertTrue(UtilitySaveKeys.looksPersonal("PlayerId"));
        assertTrue(UtilitySaveKeys.looksPersonal("LastUuid"));
        assertTrue(UtilitySaveKeys.looksPersonal("ChargedLeast"));
        assertFalse(UtilitySaveKeys.looksPersonal("Heat"));
    }
}
