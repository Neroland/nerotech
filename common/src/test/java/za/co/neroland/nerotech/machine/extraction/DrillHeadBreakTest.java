package za.co.neroland.nerotech.machine.extraction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Head wear cannot be reset. Wear is a number on the head item, so taking the head out, breaking the drill
 * and putting the head back into a fresh controller continues from the same wear. Modelled here with the
 * int the item carries; a "fresh controller" holds no wear of its own. Pure JVM.
 */
class DrillHeadBreakTest {

    /** Run {@code ops} operations on a head holding {@code wear}; returns the new wear, or -1 once used up. */
    private static int run(int wear, DrillHeadTier tier, int ops) {
        int used = wear;
        for (int i = 0; i < ops; i++) {
            if (DrillRules.wornOut(used, tier)) {
                return -1;
            }
            used = DrillRules.wear(used, tier);
        }
        return used;
    }

    @Test
    void removingTheHeadAndReplacingTheControllerKeepsTheWear() {
        for (DrillHeadTier tier : DrillHeadTier.values()) {
            int ops = tier.operations();
            // One operation short of worn out, then out through the GUI: the wear goes with the item.
            int carried = run(0, tier, ops - 1);
            assertEquals(ops - 1, carried, tier + " wear before removal");
            // A broken and re-placed controller keeps nothing; the head brings its wear back in.
            assertEquals(1, DrillRules.operationsLeft(carried, tier), tier + " ops left after the round trip");
            assertEquals(-1, run(carried, tier, 1), tier + " the next operation uses the head up");
        }
    }

    @Test
    void aHeadGivesExactlyItsRatedOperations() {
        for (DrillHeadTier tier : DrillHeadTier.values()) {
            int used = 0;
            int done = 0;
            while (true) {
                done++;
                if (DrillRules.wornOut(used, tier)) {
                    break;
                }
                used = DrillRules.wear(used, tier);
                assertTrue(done < 100_000, "runaway");
            }
            assertEquals(tier.operations(), done, tier + " operations per head");
        }
    }

    @Test
    void theWearBarIsFullWhenFreshAndFallsWithUse() {
        DrillHeadTier tier = DrillHeadTier.IRON;
        assertEquals(13, DrillRules.wearBarWidth(0, tier));
        assertTrue(DrillRules.wearBarWidth(tier.operations() / 2, tier) < 13);
        assertEquals(0, DrillRules.wearBarWidth(tier.operations(), tier));
        assertEquals(0, DrillRules.operationsLeft(tier.operations() + 5, tier));
    }
}
