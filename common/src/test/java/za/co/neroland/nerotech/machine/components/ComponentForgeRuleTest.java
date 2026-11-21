package za.co.neroland.nerotech.machine.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import za.co.neroland.nerotech.machine.components.RecipeFixture.Stack;

/**
 * Component Forge rule: up to six item inputs plus one fluid make one component, and the fluid is part
 * of the match. Checked on the shipped capacitor recipe (six inputs + sulfuric acid).
 */
class ComponentForgeRuleTest {

    private static final List<Stack> CAPACITOR_INPUTS = List.of(
            new Stack("nerotech:polymer_sheet", 2),
            new Stack("nerotech:copper_wire", 2),
            new Stack("minecraft:redstone", 2),
            new Stack("minecraft:gold_nugget", 2),
            new Stack("minecraft:iron_ingot", 1),
            new Stack("nerotech:cindersteel_plate", 1));

    @Test
    void forgeHasSixInputsOneFluidAndNoCatalyst() {
        assertEquals(6, ComponentsSpecs.COMPONENT_FORGE.inputs());
        assertTrue(ComponentsSpecs.COMPONENT_FORGE.fluidIn() > 0);
        assertFalse(ComponentsSpecs.COMPONENT_FORGE.catalyst(), "no patterns, no catalyst");
        assertFalse(ComponentsSpecs.COMPONENT_FORGE.hasGas());
        assertEquals(0L, ComponentsSpecs.COMPONENT_FORGE.fluidOut());
    }

    @Test
    void sixInputMatchWithTheRightFluid() {
        RecipeFixture capacitor = RecipeFixture.load("component_forging_capacitor");
        assertEquals("nerotech:component_forging", capacitor.type());
        assertEquals(6, capacitor.requirements().size(), "the capacitor uses all six input slots");
        assertTrue(capacitor.matches(CAPACITOR_INPUTS, null, "nerotech:sulfuric_acid", 250L));
        assertTrue(capacitor.matches(CAPACITOR_INPUTS, null, "nerotech:flowing_sulfuric_acid", 1_000L),
                "any member of c:sulfuric_acid counts");
    }

    @Test
    void wrongFluidFails() {
        RecipeFixture capacitor = RecipeFixture.load("component_forging_capacitor");
        assertFalse(capacitor.matches(CAPACITOR_INPUTS, null, "minecraft:water", 8_000L));
        assertFalse(capacitor.matches(CAPACITOR_INPUTS, null, "nerotech:polymer_resin", 8_000L));
        assertFalse(capacitor.matches(CAPACITOR_INPUTS, null, null, 0L), "no fluid at all");
        assertFalse(capacitor.matches(CAPACITOR_INPUTS, null, "nerotech:sulfuric_acid", 249L), "too little acid");
    }

    @Test
    void everyInputIsNeeded() {
        RecipeFixture capacitor = RecipeFixture.load("component_forging_capacitor");
        for (int skip = 0; skip < CAPACITOR_INPUTS.size(); skip++) {
            int drop = skip;
            List<Stack> five = java.util.stream.IntStream.range(0, CAPACITOR_INPUTS.size())
                    .filter(i -> i != drop).mapToObj(CAPACITOR_INPUTS::get).toList();
            assertFalse(capacitor.matches(five, null, "nerotech:sulfuric_acid", 250L), "missing input " + skip);
        }
    }

    @Test
    void shippedForgeRecipesFitTheForge() {
        for (String name : List.of("component_forging_servo", "component_forging_capacitor",
                "component_forging_heat_sink")) {
            RecipeFixture recipe = RecipeFixture.load(name);
            assertEquals("nerotech:component_forging", recipe.type(), name);
            assertTrue(recipe.requirements().size() <= ComponentsSpecs.COMPONENT_FORGE.inputs(), name);
            assertTrue(recipe.catalyst().isEmpty(), name + " must not need a catalyst");
        }
    }
}
