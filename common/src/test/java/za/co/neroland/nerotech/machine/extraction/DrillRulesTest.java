package za.co.neroland.nerotech.machine.extraction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import za.co.neroland.nerotech.deposit.DepositService;

/** Deep Core Drill rules: head wear by tier, op-cadence extraction, stall precedence. Pure JVM. */
class DrillRulesTest {

    @Test
    void betterHeadsLastLongerAndYieldMore() {
        int prevOps = 0;
        int prevYield = 0;
        int prevUnits = 0;
        for (DrillHeadTier tier : DrillHeadTier.values()) {
            assertTrue(tier.operations() > prevOps, tier.name());
            assertTrue(tier.unitsPerOperation() > prevUnits, tier.name());
            int yield = DrillRules.headYield(DepositService.yieldFor(tier.unitsPerOperation(), 1000, 1000), tier);
            assertTrue(yield > prevYield, tier.name());
            prevOps = tier.operations();
            prevUnits = tier.unitsPerOperation();
            prevYield = yield;
        }
    }

    @Test
    void headWearsOutAfterItsOperations() {
        for (DrillHeadTier tier : DrillHeadTier.values()) {
            int used = 0;
            int consumed = 0;
            for (int op = 0; op < tier.operations() * 3; op++) {
                if (DrillRules.wornOut(used, tier)) {
                    consumed++;
                }
                used = DrillRules.wear(used, tier);
            }
            assertEquals(3, consumed, tier.name() + " uses exactly one head per " + tier.operations() + " ops");
            assertEquals(0, used);
        }
    }

    @Test
    void swappingTiersNeverResetsWear() {
        int used = 256; // half of a glacite head
        int asIron = DrillRules.carryWear(used, DrillHeadTier.GLACITE, DrillHeadTier.IRON);
        assertEquals(32, asIron);
        int back = DrillRules.carryWear(asIron, DrillHeadTier.IRON, DrillHeadTier.GLACITE);
        assertTrue(back >= used, "a round trip never refunds wear");
        assertEquals(DrillHeadTier.IRON.operations() - 1,
                DrillRules.carryWear(511, DrillHeadTier.GLACITE, DrillHeadTier.IRON), "never past one op left");
        assertEquals(10, DrillRules.carryWear(10, DrillHeadTier.IRON, DrillHeadTier.IRON));
    }

    @Test
    void depositIsReadOncePerOperationNotPerTick() {
        int opTicks = DrillRules.operationTicks(200, 1.0D, 1.0D);
        assertEquals(200, opTicks);
        int progress = 0;
        int reads = 0;
        for (int tick = 0; tick < 2_000; tick++) {
            int before = progress;
            progress = DrillRules.advance(progress, opTicks);
            if (DrillRules.completed(before, progress)) {
                reads++;
            }
        }
        assertEquals(10, reads);
        assertEquals(100, DrillRules.operationTicks(200, 2.0D, 1.0D), "speed halves the operation");
        assertEquals(1, DrillRules.operationTicks(1, 50.0D, 1.0D), "never below one tick");
    }

    @Test
    void stallPrecedence() {
        assertEquals(DrillRules.Stall.UNFORMED, DrillRules.stall(false, true, false, true, false));
        assertEquals(DrillRules.Stall.DIMENSION_DISABLED, DrillRules.stall(true, true, true, false, true));
        assertEquals(DrillRules.Stall.NO_HEAD, DrillRules.stall(true, false, false, false, true));
        assertEquals(DrillRules.Stall.DEPLETED, DrillRules.stall(true, false, true, true, true));
        assertEquals(DrillRules.Stall.OUTPUT_FULL, DrillRules.stall(true, false, true, false, false));
        assertEquals(DrillRules.Stall.NONE, DrillRules.stall(true, false, true, false, true));
    }

    @Test
    void disabledDimensionIdles() {
        String csv = "minecraft:the_end, nerospace:luna";
        assertTrue(DepositService.disabled("nerospace:luna", csv));
        assertFalse(DepositService.disabled("minecraft:overworld", csv));
        assertEquals(DrillRules.Stall.DIMENSION_DISABLED,
                DrillRules.stall(true, DepositService.disabled("minecraft:the_end", csv), true, false, true));
    }

    @Test
    void yieldNeverZeroWhenTheDepositGaveSome() {
        assertEquals(1, DrillRules.headYield(1, DrillHeadTier.IRON));
        assertEquals(0, DrillRules.headYield(0, DrillHeadTier.GLACITE));
        assertEquals(13, DrillRules.headYield(10, DrillHeadTier.GLACITE));
        assertEquals(DrillHeadTier.CINDERSTEEL, DrillHeadTier.byItemId("nerotech:cindersteel_drill_head"));
        assertEquals(null, DrillHeadTier.byItemId("minecraft:iron_pickaxe"));
    }
}
