package za.co.neroland.nerotech.machine.heat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

/** Heat Exchanger direction rules. Pure JVM. */
class ExchangerModelTest {

    private static final Map<String, Integer> COOL = ExchangerModel.parseTable(HeatSettings.heatExchangerCoolants());
    private static final Map<String, Integer> HOT = ExchangerModel.parseTable(HeatSettings.heatExchangerHotMedia());

    private static ExchangerModel.Medium medium(String id) {
        return ExchangerModel.classify(id, COOL, HOT);
    }

    @Test
    void coolantOnlyEverTakesMachineHeatAndIsUsedUp() {
        ExchangerModel.Plan plan = ExchangerModel.plan(medium("nerotech:cryo_coolant"), 1000, 900, 0, 0, 700, 60, 300);
        assertEquals(ExchangerModel.TO_TANK, plan.direction());
        assertEquals(60, plan.heat());
        assertEquals(15, plan.mediumMb(), "4 heat per mB, rounded up");
        // A machine at ambient: nothing to absorb.
        assertEquals(ExchangerModel.NONE, ExchangerModel.plan(medium("nerotech:cryo_coolant"), 1000, 0, 900, 0, 700, 60, 300).direction());
        // Little coolant left caps the heat absorbed.
        assertEquals(8, ExchangerModel.plan(medium("nerotech:cryo_coolant"), 2, 900, 0, 0, 700, 60, 300).heat());
    }

    @Test
    void hotMediumOnlyEverHeatsTheMachine() {
        ExchangerModel.Plan plan = ExchangerModel.plan(medium("minecraft:lava"), 1000, 100, 0, 0, 700, 60, 300);
        assertEquals(ExchangerModel.TO_MACHINE, plan.direction());
        assertEquals(30, plan.mediumMb());
        // At or above the medium's temperature: no more heat.
        assertEquals(ExchangerModel.NONE, ExchangerModel.plan(medium("minecraft:lava"), 1000, 700, 0, 0, 700, 60, 300).direction());
        // A coolant-heat machine never pushes heat into lava.
        assertEquals(ExchangerModel.NONE, ExchangerModel.plan(medium("minecraft:lava"), 1000, 900, 0, 0, 700, 60, 300).direction());
    }

    @Test
    void storingMediumGoesEitherWay() {
        ExchangerModel.Medium water = medium("minecraft:water");
        assertEquals(ExchangerModel.Role.STORE, water.role());
        ExchangerModel.Plan in = ExchangerModel.plan(water, 1000, 900, 100, 0, 700, 60, 300);
        assertEquals(ExchangerModel.TO_TANK, in.direction(), "hot reactor heats the stored water");
        assertEquals(0, in.mediumMb(), "storing uses nothing up");
        ExchangerModel.Plan out = ExchangerModel.plan(water, 1000, 100, 900, 0, 700, 60, 300);
        assertEquals(ExchangerModel.TO_MACHINE, out.direction(), "hot water heats a cold Foundry port");
        assertEquals(ExchangerModel.NONE, ExchangerModel.plan(water, 1000, 400, 400, 0, 700, 60, 300).direction());
        assertTrue(out.heat() <= 60, "capped per exchange");
    }

    @Test
    void emptyTankDoesNothing() {
        assertEquals(ExchangerModel.Plan.IDLE, ExchangerModel.plan(medium("minecraft:water"), 0, 900, 0, 0, 700, 60, 300));
    }

    @Test
    void storeTransferIsExact() {
        HeatRunMathTest.Box machine = new HeatRunMathTest.Box(900, 1000, false);
        HeatRunMathTest.Box tank = new HeatRunMathTest.Box(990, 1000, false);
        int moved = ExchangerModel.transfer(machine, tank, 60);
        assertEquals(10, moved, "the tank only had room for 10");
        assertEquals(1890, machine.heat + tank.heat, "what the tank refused went back");
    }
}
