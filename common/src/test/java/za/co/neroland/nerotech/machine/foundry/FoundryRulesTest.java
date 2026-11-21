package za.co.neroland.nerotech.machine.foundry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Pure rule tests for the Blast Foundry (no Minecraft bootstrap): batch size by shell size, no progress
 * below {@code min_heat} even with full energy, heat only from the ports, and the adversarial rule that
 * breaking the shell mid-batch never duplicates inputs (kept or turned into output, never both).
 */
class FoundryRulesTest {

    private static final int BASE = 4;
    private static final int MULT = 4;

    @Test
    void batchSizeFollowsShellSize() {
        assertEquals(BASE, FoundryBatch.maxUnits(3, BASE, MULT));
        assertEquals(BASE * MULT, FoundryBatch.maxUnits(5, BASE, MULT), "a 5³ runs 4x the 3³ batch");
        assertEquals(0, FoundryBatch.maxUnits(0, BASE, MULT), "unformed runs nothing");
        assertEquals(0, FoundryBatch.maxUnits(7, BASE, MULT), "only 3 and 5 are foundry sizes");
        // Energy scales with the batch, so a bigger shell is not cheaper per unit.
        assertEquals(40L * 16, FoundryBatch.energyPerTick(40, FoundryBatch.maxUnits(5, BASE, MULT)));
        assertEquals(40L * 4, FoundryBatch.energyPerTick(40, FoundryBatch.maxUnits(3, BASE, MULT)));
        // A partial batch runs only what the inputs and outputs allow.
        assertEquals(3, FoundryBatch.unitsFor(16, 3, 64));
        assertEquals(2, FoundryBatch.unitsFor(16, 9, 2));
        assertEquals(0, FoundryBatch.unitsFor(16, 0, 64));
    }

    /** A tiny model of the controller tick: the gate order the block entity uses. */
    private static final class Sim {
        final FoundryBatch batch = new FoundryBatch();
        int furnaceHeat;
        long energy;
        int inputs;
        int outputs;
        int formedSize = 5;

        /** One tick of a recipe needing 1 input per unit, minHeat, energyPerUnit, ticks. */
        void tick(int minHeat, int energyPerUnit, int ticks) {
            if (this.formedSize == 0) {
                this.batch.abort();
                return;
            }
            if (this.furnaceHeat < minHeat) {
                return; // too cold: no progress whatever the energy
            }
            if (!this.batch.running()) {
                int units = FoundryBatch.unitsFor(FoundryBatch.maxUnits(this.formedSize, BASE, MULT), this.inputs, 64);
                if (units <= 0) {
                    return;
                }
                this.batch.start(units);
            }
            long cost = FoundryBatch.energyPerTick(energyPerUnit, this.batch.units());
            if (this.energy < cost) {
                return;
            }
            this.energy -= cost;
            if (this.batch.advance(ticks)) {
                int paid = this.batch.settle(this.inputs, 64);
                this.inputs -= paid;
                this.outputs += paid;
            }
        }
    }

    @Test
    void noProgressBelowMinHeatEvenWithFullEnergy() {
        Sim sim = new Sim();
        sim.energy = Long.MAX_VALUE / 2;
        sim.inputs = 32;
        sim.furnaceHeat = 399;
        long before = sim.energy;
        for (int t = 0; t < 1_000; t++) {
            sim.tick(400, 20, 100);
        }
        assertFalse(sim.batch.running());
        assertEquals(0, sim.batch.progress());
        assertEquals(before, sim.energy, "no energy spent while too cold");
        assertEquals(32, sim.inputs);
        assertEquals(0, sim.outputs);

        sim.furnaceHeat = 400;
        for (int t = 0; t < 100; t++) {
            sim.tick(400, 20, 100);
        }
        assertEquals(16, sim.outputs, "at the threshold one full 5³ batch completes");
        assertEquals(16, sim.inputs);
    }

    @Test
    void heatComesOnlyFromPorts() {
        // No intake: the furnace never warms, not from running, not from a hot environment.
        int heat = 0;
        for (int t = 0; t < 10_000; t++) {
            heat = FoundryHeat.cool(heat, 150, 5, 2, t % 2 == 0);
        }
        assertEquals(0, heat, "a nether-hot ambient does not heat the furnace");
        heat = 600;
        for (int t = 0; t < 100; t++) {
            int next = FoundryHeat.cool(heat, 0, 5, 2, true);
            assertTrue(next <= heat, "without port heat the furnace can only fall");
            heat = next;
        }
        // Hot ambient: loses heat more slowly.
        int coldWorld = FoundryHeat.cool(600, 0, 5, 0, false);
        int hotWorld = FoundryHeat.cool(600, 300, 5, 0, false);
        assertTrue(600 - hotWorld < 600 - coldWorld);

        // The only way in: intake from the ports, bounded by room.
        assertEquals(250, FoundryHeat.intake(100, 1_000, 250));
        assertEquals(100, FoundryHeat.intake(900, 1_000, 250), "never past capacity");
        assertEquals(0, FoundryHeat.intake(500, 1_000, 0));
        assertEquals(0, FoundryHeat.intake(500, 1_000, -50), "a negative offer never removes heat");

        // Port buffer: takes heat up to capacity, never negative.
        assertEquals(500, FoundryHeat.portAccept(0, 500, 9_999));
        assertEquals(20, FoundryHeat.portAccept(480, 500, 50));
        assertEquals(0, FoundryHeat.portAccept(500, 500, 50));
        assertEquals(0, FoundryHeat.portAccept(0, 500, -10));
        assertEquals(0, FoundryHeat.bucket(0, 500, 4), "an empty port reads cold");
        assertEquals(3, FoundryHeat.bucket(500, 500, 4));
    }

    @Test
    void breakingTheShellMidBatchNeverDuplicatesInputs() {
        Sim sim = new Sim();
        sim.energy = Long.MAX_VALUE / 2;
        sim.furnaceHeat = 1_000;
        sim.inputs = 16;
        for (int t = 0; t < 50; t++) {
            sim.tick(400, 20, 100);
        }
        assertTrue(sim.batch.running());
        // The shell breaks: the batch is abandoned and the inputs are all still there, no output made.
        sim.formedSize = 0;
        sim.tick(400, 20, 100);
        assertFalse(sim.batch.running());
        assertEquals(16, sim.inputs, "inputs kept");
        assertEquals(0, sim.outputs, "and not also turned into output");
        assertEquals(16, sim.inputs + sim.outputs, "conserved");

        // Rebuilt as a 3³: a fresh, smaller batch, still conserved.
        sim.formedSize = 3;
        for (int t = 0; t < 1_000; t++) {
            sim.tick(400, 20, 100);
        }
        assertEquals(16, sim.inputs + sim.outputs, "every input is either kept or poured, once");
        assertEquals(16, sim.outputs);
    }

    @Test
    void inputsTakenMidBatchShrinkThePayout() {
        FoundryBatch batch = new FoundryBatch();
        batch.start(16);
        for (int t = 0; t < 99; t++) {
            assertFalse(batch.advance(100));
        }
        assertTrue(batch.advance(100));
        // A player pulled all but five inputs out: only five units are consumed and produced.
        assertEquals(5, batch.settle(5, 64));
        assertFalse(batch.running());
        batch.start(4);
        batch.abort();
        assertEquals(0, batch.units());
        assertEquals(0, batch.settle(64, 64), "an aborted batch pays nothing");
        batch.restore(-3, 10);
        assertFalse(batch.running());
        assertEquals(0.25F, FoundryBatch.fill(4, 16));
        assertEquals(0.0F, FoundryBatch.fill(4, 0));
    }
}
