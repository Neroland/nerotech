package za.co.neroland.nerotech.machine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Progress survives only on the recipe it was made on. Pure JVM. */
class RecipeProgressTest {

    @Test
    void swappingToAnotherRecipeStartsAgain() {
        assertFalse(RecipeProgress.keepsProgress("nerotech:long_cheap", "nerotech:short_dear"));
        assertTrue(RecipeProgress.keepsProgress("nerotech:long_cheap", "nerotech:long_cheap"));
        assertTrue(RecipeProgress.keepsProgress(RecipeProgress.NONE, "nerotech:short_dear"),
                "a machine with no recorded recipe has nothing to compare");
        assertTrue(RecipeProgress.keepsProgress(null, "nerotech:x"));
        assertFalse(RecipeProgress.keepsProgress("nerotech:x", RecipeProgress.NONE));
    }

    @Test
    void aSwapOneTickShortOfDoneFinishesNothingCheaply() {
        // Run a 400-tick recipe to 399, swap to a 20-tick recipe: the swap restarts at 0, so the short
        // recipe still takes its full 20 ticks (and its full energy) to finish.
        String running = "nerotech:long_cheap";
        int progress = 399;
        String now = "nerotech:short_dear";
        if (progress > 0 && !RecipeProgress.keepsProgress(running, now)) {
            progress = 0;
        }
        int ticks = 0;
        while (progress < 20) {
            progress++;
            ticks++;
        }
        assertEquals(20, ticks);
    }
}
