package za.co.neroland.nerotech.machine.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import za.co.neroland.nerotech.machine.ProcessingSpec;
import za.co.neroland.nerotech.machine.components.RecipeFixture.Stack;
import za.co.neroland.nerotech.recipe.ProcessMatching;

/**
 * Laser Etcher rule: the lens is a tool. It sits in the catalyst slot, the recipe names which lens it
 * needs, the wrong lens (or none) matches nothing, and an operation never consumes it.
 */
class LaserEtcherRuleTest {

    private static final ProcessingSpec SPEC = ComponentsSpecs.LASER_ETCHER;

    @Test
    void lensSlotIsACatalystOutsideTheConsumedInputs() {
        assertTrue(SPEC.catalyst());
        int lens = SPEC.catalystSlot();
        assertTrue(SPEC.isRecipeSlot(lens), "the recipe reads the lens");
        assertFalse(SPEC.isOutput(lens));
        for (int i = 0; i < SPEC.inputs(); i++) {
            assertNotEquals(lens, SPEC.inputSlot(i), "the lens is never an input slot");
        }
    }

    @Test
    void lensMustMatchTheRecipe() {
        RecipeFixture logic = RecipeFixture.load("etching_logic_chip");
        List<Stack> parts = List.of(new Stack("nerotech:wafer", 1), new Stack("nerotech:copper_wire", 2));
        assertTrue(logic.matches(parts, "nerotech:quartz_focus_lens", null, 0L));
        assertFalse(logic.matches(parts, "nerotech:void_focus_lens", null, 0L), "wrong lens");
        assertFalse(logic.matches(parts, "nerotech:ember_focus_lens", null, 0L), "wrong lens");
        assertFalse(logic.matches(parts, null, null, 0L), "no lens");

        RecipeFixture quantum = RecipeFixture.load("etching_quantum_processor");
        List<Stack> quantumParts = List.of(new Stack("nerotech:wafer", 1),
                new Stack("nerotech:superconductor_wire", 2), new Stack("nerotech:logic_chip", 1));
        assertTrue(quantum.matches(quantumParts, "nerotech:void_focus_lens", null, 0L));
        assertFalse(quantum.matches(quantumParts, "nerotech:quartz_focus_lens", null, 0L), "wrong lens");
    }

    @Test
    void lensIsNeverAnIngredient() {
        for (String name : List.of("etching_logic_chip", "etching_quantum_processor")) {
            RecipeFixture recipe = RecipeFixture.load(name);
            assertEquals("nerotech:etching", recipe.type());
            assertTrue(recipe.catalyst().isPresent(), name + " names its lens as the catalyst");
            Set<String> lenses = RecipeFixture.resolve("#nerotech:focus_lenses", "item");
            assertFalse(recipe.inputIds().stream().anyMatch(lenses::contains),
                    name + " must not consume a lens");
            assertTrue(lenses.containsAll(recipe.catalyst().get()), name + " catalyst is a focus lens");
            assertTrue(recipe.requirements().size() <= SPEC.inputs(), name);
        }
    }

    @Test
    void lensSurvivesRepeatedOperations() {
        RecipeFixture logic = RecipeFixture.load("etching_logic_chip");
        // The machine's slots: inputs, then the lens. An operation spends inputs 0..inputs()-1 only
        // (ProcessingMachineBlockEntity.finish walks spec.inputSlot(i) for i < inputs()).
        String[] ids = {"nerotech:wafer", "nerotech:copper_wire", null};
        int[] slots = {4, 8, 0};
        int lensCount = 1;
        int done = 0;
        while (true) {
            List<Stack> stacks = new ArrayList<>();
            for (int i = 0; i < SPEC.inputs(); i++) {
                stacks.add(new Stack(ids[i] == null ? "minecraft:air" : ids[i], slots[i]));
            }
            if (!logic.matches(stacks, lensCount > 0 ? "nerotech:quartz_focus_lens" : null, null, 0L)) {
                break;
            }
            for (ProcessMatching.Requirement<Stack> requirement : logic.requirements()) {
                int need = requirement.count();
                for (int i = 0; i < SPEC.inputs() && need > 0; i++) {
                    if (slots[i] > 0 && requirement.test().test(new Stack(ids[i], slots[i]))) {
                        int take = Math.min(need, slots[i]);
                        slots[i] -= take;
                        need -= take;
                    }
                }
            }
            done++;
        }
        assertEquals(4, done, "four wafers, eight wires: four processors");
        assertEquals(1, lensCount, "the lens is still fitted");
        assertEquals(0, slots[0]);
        assertEquals(0, slots[1]);
    }
}
