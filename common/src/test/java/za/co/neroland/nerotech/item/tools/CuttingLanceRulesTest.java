package za.co.neroland.nerotech.item.tools;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Cutting Lance duplication fix (Stage 3 review M1): no cut from the off hand, and a Glacite cut yields
 * the preserved block exactly once. Pure JVM; the in-world drops (stone gives cobblestone through the
 * normal break path) are on the runtime script.
 */
class CuttingLanceRulesTest {

    @Test
    void offHandNeverCuts() {
        assertTrue(ToolRules.lanceFiresFrom(true), "main hand cuts");
        assertFalse(ToolRules.lanceFiresFrom(false), "off hand does nothing (Silk Touch / drill in the main hand)");
    }

    @Test
    void glaciteCutYieldsExactlyOne() {
        assertTrue(ToolRules.needsPreservedDrop(List.of(), "blue_ice"),
                "the break path dropped nothing: the lance drops the one block");
        assertFalse(ToolRules.needsPreservedDrop(List.of("blue_ice"), "blue_ice"),
                "the break path already dropped it (e.g. Silk Touch): no second copy");
        assertTrue(ToolRules.needsPreservedDrop(List.of("snowball"), "snow_block"),
                "an unrelated drop does not stand in for the block");
    }
}
