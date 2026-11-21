package za.co.neroland.nerotech.machine.metallurgy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Filament Spooler rule: a coil needs its core. Wire alone (however much) matches no coil recipe; wire
 * plus a rod or ingot core does; and every coil recipe names a non-wire core next to its wire.
 */
class FilamentSpoolerCoreTest {

    @Test
    void coilNeedsTheCore() {
        List<RecipeData.View> drawing = RecipeData.recipes("nerotech:drawing");
        assertTrue(RecipeData.results(drawing, List.of("nerotech:copper_wire"), 64, null).isEmpty(),
                "wire alone is no coil");
        assertTrue(RecipeData.results(drawing, List.of("nerotech:cindersteel_wire", "nerotech:cindersteel_wire"), 64,
                null).isEmpty(), "two stacks of wire are still no coil");
        assertEquals(List.of("nerotech:copper_coil"),
                RecipeData.results(drawing, List.of("nerotech:copper_wire", "minecraft:iron_ingot"), 4, null));
        assertEquals(List.of("nerotech:cindersteel_coil"),
                RecipeData.results(drawing, List.of("nerotech:cindersteel_wire", "nerotech:cindersteel_rod"), 4, null));
        assertTrue(RecipeData.results(drawing, List.of("nerotech:copper_wire", "minecraft:iron_ingot"), 3, null)
                .isEmpty(), "too little wire is no coil");
    }

    @Test
    void everyCoilRecipeNamesWireAndACore() {
        int coils = 0;
        for (RecipeData.View recipe : RecipeData.recipes("nerotech:drawing")) {
            if (!recipe.result().endsWith("_coil")) {
                continue;
            }
            coils++;
            assertEquals(2, recipe.ingredients().size(), recipe.file());
            boolean wire = recipe.ingredients().stream().anyMatch(i -> i.contains("wire"));
            boolean core = recipe.ingredients().stream().anyMatch(i -> !i.contains("wire"));
            assertTrue(wire && core, recipe.file() + " needs wire and a core");
        }
        assertTrue(coils >= 2);
    }

    @Test
    void ingotsDrawIntoWire() {
        List<RecipeData.View> drawing = RecipeData.recipes("nerotech:drawing");
        assertEquals(List.of("nerotech:copper_wire"), RecipeData.results(drawing, List.of("minecraft:copper_ingot"), 1, null));
        assertEquals(List.of("nerotech:cindersteel_wire"),
                RecipeData.results(drawing, List.of("nerotech:cindersteel_ingot"), 1, null));
        assertFalse(RecipeData.results(drawing, List.of("nerotech:cindersteel_ingot"), 1, null)
                .contains("nerotech:cindersteel_coil"));
    }
}
