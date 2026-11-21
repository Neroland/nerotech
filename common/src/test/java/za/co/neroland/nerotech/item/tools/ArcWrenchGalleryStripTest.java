package za.co.neroland.nerotech.item.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * A wrenched gallery exhibit becomes an ordinary machine: the carried save drops the
 * {@code GalleryExhibit} flag (and the owner), and keeps everything else. Pure JVM, on {@link WrenchRules}.
 */
class ArcWrenchGalleryStripTest {

    @Test
    void theCarriedCopyDropsTheExhibitFlagAndOwner() {
        Map<String, Object> save = new HashMap<>();
        save.put("GalleryExhibit", true);
        save.put("OwnerMost", 1L);
        save.put("OwnerLeast", 2L);
        save.put("Progress", 40);
        save.put("Item0", "nerotech:steel_ingot");
        Map<String, Object> carried = WrenchRules.stripCarried(save, Map::remove);
        assertSame(save, carried);
        assertFalse(carried.containsKey("GalleryExhibit"));
        assertFalse(carried.containsKey("OwnerMost"));
        assertFalse(carried.containsKey("OwnerLeast"));
        assertEquals(40, carried.get("Progress"));
        assertTrue(carried.containsKey("Item0"));
    }

    @Test
    void theExhibitFlagKeyMatchesTheMachineSave() {
        assertTrue(WrenchRules.STRIPPED_ON_PICKUP.contains("GalleryExhibit"));
    }
}
