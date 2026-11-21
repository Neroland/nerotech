package za.co.neroland.nerotech.machine.extraction;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Adversarial pass: breaking and re-placing a Deep Core Drill must not reset head wear. A drill broken with
 * a part-worn head spends that head; a fresh head (no wear) is handed back whole. Pure JVM.
 */
class DrillHeadBreakTest {

    @Test
    void aWornHeadIsSpentWhenTheDrillBreaks() {
        assertFalse(DrillRules.headSpentOnBreak(0), "an unused head comes back");
        assertTrue(DrillRules.headSpentOnBreak(1));
        for (DrillHeadTier tier : DrillHeadTier.values()) {
            int used = 0;
            boolean everFreeReset = false;
            for (int op = 0; op < 5; op++) {
                used = DrillRules.wear(used, tier);
                // Any wear at all makes a break cost the head: there is no window to reset it for free.
                everFreeReset |= used > 0 && !DrillRules.headSpentOnBreak(used);
            }
            assertFalse(everFreeReset, tier + " wear could be reset by breaking the drill");
        }
    }
}
