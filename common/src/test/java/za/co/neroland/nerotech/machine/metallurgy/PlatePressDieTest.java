package za.co.neroland.nerotech.machine.metallurgy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * Plate Press rule: the die selects the shape. The same ingot gives a plate under the plate die and
 * rods under the rod die; with no die nothing matches; and every die a recipe names is one the die slot
 * accepts ({@code nerotech:press_dies}).
 */
class PlatePressDieTest {

    private static final String INGOT = "nerotech:cindersteel_ingot";

    @Test
    void sameInputDifferentDieDifferentOutput() {
        List<RecipeData.View> pressing = RecipeData.recipes("nerotech:pressing");
        assertEquals(List.of("nerotech:cindersteel_plate"),
                RecipeData.results(pressing, List.of(INGOT), 1, "nerotech:plate_die"));
        assertEquals(List.of("nerotech:cindersteel_rod"),
                RecipeData.results(pressing, List.of(INGOT), 1, "nerotech:rod_die"));
        assertEquals(List.of("nerotech:wafer"),
                RecipeData.results(pressing, List.of("nerotech:crystal_boule"), 1, "nerotech:wafer_die"));
        assertTrue(RecipeData.results(pressing, List.of(INGOT), 1, "nerotech:wafer_die").isEmpty(),
                "a wafer die does not press ingots");
    }

    @Test
    void noDieNoMatch() {
        List<RecipeData.View> pressing = RecipeData.recipes("nerotech:pressing");
        assertFalse(pressing.isEmpty());
        for (RecipeData.View recipe : pressing) {
            assertNotNull(recipe.catalyst(), recipe.file() + " must name its die");
        }
        assertTrue(RecipeData.results(pressing, List.of(INGOT), 1, null).isEmpty());
        assertTrue(RecipeData.results(pressing, List.of("nerotech:crystal_boule"), 1, null).isEmpty());
    }

    @Test
    void everyNamedDieFitsTheDieSlot() {
        Set<String> dies = RecipeData.tag("nerotech:press_dies");
        assertEquals(Set.of("nerotech:plate_die", "nerotech:rod_die", "nerotech:wafer_die"), dies);
        for (RecipeData.View recipe : RecipeData.recipes("nerotech:pressing")) {
            assertTrue(dies.contains(recipe.catalyst()), recipe.file() + " names a die the slot refuses");
        }
    }
}
