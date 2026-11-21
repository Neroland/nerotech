package za.co.neroland.nerotech.machine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Locks the five Annunciator alarm bits (machine contract, Alarms). Pure JVM. */
class MachineAlarmsTest {

    @Test
    void healthyMachineRaisesNothing() {
        assertEquals(0, MachineAlarms.mask(Readout.WORK, MachineStatus.RUNNING, 0, 8, 0));
    }

    @Test
    void eachConditionSetsItsOwnBit() {
        assertEquals(MachineAlarms.OVERHEAT, MachineAlarms.mask(Readout.HOT, MachineStatus.RUNNING, 0, 8, 0));
        assertEquals(MachineAlarms.STARVED, MachineAlarms.mask(Readout.STALL, MachineStatus.STARVED, 0, 8, 0));
        assertEquals(MachineAlarms.STARVED, MachineAlarms.mask(Readout.STALL, MachineStatus.NO_ENERGY, 0, 8, 0));
        assertEquals(MachineAlarms.FULL, MachineAlarms.mask(Readout.STALL, MachineStatus.BLOCKED, 0, 8, 0));
        assertEquals(MachineAlarms.POLLUTING, MachineAlarms.mask(Readout.WORK, MachineStatus.RUNNING, 9, 8, 0));
        assertEquals(MachineAlarms.FAILING, MachineAlarms.mask(Readout.WORK, MachineStatus.RUNNING, 0, 8, 1));
    }

    @Test
    void pollutionMustExceedTheThreshold() {
        assertEquals(0, MachineAlarms.mask(Readout.WORK, MachineStatus.RUNNING, 8, 8, 0));
        assertEquals(MachineAlarms.POLLUTING, MachineAlarms.mask(Readout.WORK, MachineStatus.RUNNING, 1, 0, 0));
    }

    @Test
    void bitsCombineAndFitInFive() {
        int mask = MachineAlarms.mask(Readout.HOT, MachineStatus.BLOCKED, 50, 8, 3);
        assertTrue(MachineAlarms.has(mask, MachineAlarms.OVERHEAT));
        assertTrue(MachineAlarms.has(mask, MachineAlarms.FULL));
        assertTrue(MachineAlarms.has(mask, MachineAlarms.POLLUTING));
        assertTrue(MachineAlarms.has(mask, MachineAlarms.FAILING));
        assertEquals(0, mask >>> MachineAlarms.COUNT);
        assertEquals(MachineAlarms.COUNT, MachineAlarms.IDS.length);
    }
}
