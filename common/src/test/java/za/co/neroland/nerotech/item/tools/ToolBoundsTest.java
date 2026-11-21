package za.co.neroland.nerotech.item.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

/** Bounds of every tool: cooldown and drain, the drill's area and vein caps, the lance's range. Pure JVM. */
class ToolBoundsTest {

    private static final BreakPlanner.Cell O = new BreakPlanner.Cell(0, 64, 0);

    @Test
    void cooldownAndEnergyGateEveryAction() {
        ToolCharge charge = new ToolCharge(1_000, 0L, 0);
        assertEquals(ToolRules.Outcome.OK, ToolRules.attempt(charge.energy(), 300, 10L, charge.readyAt()));
        charge = ToolRules.afterUse(charge, 300, 10L, 12);
        assertEquals(700, charge.energy());
        assertEquals(22L, charge.readyAt());
        assertEquals(ToolRules.Outcome.COOLDOWN, ToolRules.attempt(charge.energy(), 300, 21L, charge.readyAt()));
        assertEquals(ToolRules.Outcome.OK, ToolRules.attempt(charge.energy(), 300, 22L, charge.readyAt()));
        assertEquals(ToolRules.Outcome.NO_ENERGY, ToolRules.attempt(100, 300, 99L, 0L));
        assertEquals(0, ToolRules.afterUse(new ToolCharge(100, 0L, 2), 300, 0L, 0).energy());
        assertEquals(2, ToolRules.afterUse(new ToolCharge(100, 0L, 2), 1, 0L, 0).mode());
    }

    @Test
    void chargingNeverPassesCapacityAndDrainNeverGoesNegative() {
        assertEquals(3_000, ToolRules.chargeAmount(7_000, 10_000, 1_000_000L));
        assertEquals(0, ToolRules.chargeAmount(10_000, 10_000, 500L));
        assertEquals(0, ToolRules.chargeAmount(12_000, 10_000, 500L));
        assertEquals(0, ToolRules.drained(20, 25));
        assertEquals(15, ToolRules.drained(20, 5));
        assertEquals(20, ToolRules.drained(20, -5));
    }

    @Test
    void affordableBlocksAreCappedByChargeAndPlan() {
        assertEquals(8, ToolRules.affordable(10_000, 80, 8));
        assertEquals(3, ToolRules.affordable(250, 80, 8));
        assertEquals(0, ToolRules.affordable(79, 80, 8));
        assertEquals(8, ToolRules.affordable(0, 0, 8));
        assertEquals(0, ToolRules.affordable(10_000, 80, 0));
    }

    @Test
    void areaIsTheEightOtherCellsOfOnePlane() {
        for (BreakPlanner.Axis axis : BreakPlanner.Axis.values()) {
            List<BreakPlanner.Cell> area = BreakPlanner.area(O, axis);
            assertEquals(BreakPlanner.AREA_EXTRA, area.size());
            assertEquals(8, new HashSet<>(area).size());
            assertFalse(area.contains(O));
            for (BreakPlanner.Cell c : area) {
                int along = switch (axis) {
                    case X -> c.x() - O.x();
                    case Y -> c.y() - O.y();
                    case Z -> c.z() - O.z();
                };
                assertEquals(0, along, "an area break stays in its plane");
                assertTrue(Math.abs(c.x() - O.x()) <= 1 && Math.abs(c.y() - O.y()) <= 1 && Math.abs(c.z() - O.z()) <= 1);
            }
        }
    }

    @Test
    void veinIsCappedAtTheConfigAndNeverPastSixtyFour() {
        // An endless vein: everything matches.
        assertEquals(31, BreakPlanner.vein(O, c -> true, 32).size());
        assertEquals(BreakPlanner.VEIN_HARD_CAP - 1, BreakPlanner.vein(O, c -> true, 64).size());
        assertEquals(BreakPlanner.VEIN_HARD_CAP - 1, BreakPlanner.vein(O, c -> true, 10_000).size());
        assertEquals(0, BreakPlanner.vein(O, c -> true, 1).size());
        assertEquals(0, BreakPlanner.vein(O, c -> true, 0).size());
        // A long line of ore: the walk never leaves the radius.
        List<BreakPlanner.Cell> line = BreakPlanner.vein(O, c -> c.y() == O.y() && c.z() == O.z(), 64);
        assertEquals(2 * BreakPlanner.VEIN_RADIUS, line.size());
        for (BreakPlanner.Cell c : line) {
            assertTrue(Math.abs(c.x() - O.x()) <= BreakPlanner.VEIN_RADIUS);
        }
        // Only matching, connected cells.
        Set<BreakPlanner.Cell> ore = Set.of(new BreakPlanner.Cell(1, 64, 0), new BreakPlanner.Cell(2, 65, 0),
                new BreakPlanner.Cell(5, 64, 0));
        List<BreakPlanner.Cell> vein = BreakPlanner.vein(O, ore::contains, 64);
        assertEquals(Set.of(new BreakPlanner.Cell(1, 64, 0), new BreakPlanner.Cell(2, 65, 0)), new HashSet<>(vein));
    }

    @Test
    void lanceRangeCooldownAndEnergyAreBounded() {
        assertEquals(16, LanceLens.NONE.range(16));
        assertEquals(32, LanceLens.VOID.range(16));
        assertEquals(LanceLens.MAX_RANGE, LanceLens.VOID.range(32));
        assertEquals(LanceLens.MAX_RANGE, LanceLens.NONE.range(500));
        assertEquals(1, LanceLens.GLACITE.range(0));
        for (LanceLens lens : LanceLens.values()) {
            for (int base = -5; base <= 100; base++) {
                assertTrue(lens.range(base) >= 1 && lens.range(base) <= LanceLens.MAX_RANGE);
            }
        }
        assertEquals(6, LanceLens.QUARTZ.cooldown(12));
        assertEquals(12, LanceLens.EMBER.cooldown(12));
        assertEquals(450, LanceLens.EMBER.energy(300));
        assertEquals(600, LanceLens.VOID.energy(300));
        assertEquals(300, LanceLens.QUARTZ.energy(300));
        assertEquals(LanceLens.EMBER, LanceLens.ofItemPath("ember_focus_lens"));
        assertEquals(LanceLens.NONE, LanceLens.ofItemPath("ember_dust"));
        assertEquals(LanceLens.NONE, LanceLens.ofItemPath("none_focus_lens"));
        assertEquals(LanceLens.NONE, LanceLens.parse("bogus"));
        assertEquals(LanceLens.VOID, LanceLens.parse(LanceLens.VOID.serialized()));
    }

    @Test
    void filterCartridgeBlocksAFixedNumberOfPulsesThenFouls() {
        int uses = 3;
        int fouled = 0;
        int blocked = 0;
        for (int i = 0; i < 10; i++) {
            FilterRules.Pulse pulse = FilterRules.pulse(uses, 1_000, 20);
            uses = pulse.usesLeft();
            blocked += pulse.blocked() ? 1 : 0;
            fouled += pulse.fouled() ? 1 : 0;
        }
        assertEquals(3, blocked);
        assertEquals(1, fouled, "exactly one Dirty Filter per cartridge");
        assertFalse(FilterRules.pulse(5, 10, 20).blocked(), "no charge, no protection");
        assertEquals(5, FilterRules.pulse(5, 10, 20).usesLeft());
    }
}
