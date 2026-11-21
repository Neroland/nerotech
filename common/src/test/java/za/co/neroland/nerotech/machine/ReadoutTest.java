package za.co.neroland.nerotech.machine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Locks the art-direction §2 readout mapping and its precedence rules. Pure JVM. */
class ReadoutTest {

    @Test
    void statusesMapToTheFourReadouts() {
        assertEquals(Readout.WORK, Readout.of(MachineStatus.RUNNING, 0));
        assertEquals(Readout.REST, Readout.of(MachineStatus.IDLE, 0));
        for (MachineStatus stall : new MachineStatus[] {MachineStatus.STARVED, MachineStatus.BLOCKED,
                MachineStatus.UNPAID, MachineStatus.UNFORMED, MachineStatus.NO_ENERGY}) {
            assertEquals(Readout.STALL, Readout.of(stall, 0), stall.name());
        }
        assertEquals(Readout.HOT, Readout.of(MachineStatus.THROTTLED, 0));
    }

    @Test
    void hotOverridesEverythingFromBucketFour() {
        assertEquals(Readout.WORK, Readout.of(MachineStatus.RUNNING, 3));
        assertEquals(Readout.HOT, Readout.of(MachineStatus.RUNNING, 4));
        assertEquals(Readout.HOT, Readout.of(MachineStatus.IDLE, 5));
        assertEquals(Readout.HOT, Readout.of(MachineStatus.STARVED, 4));
    }

    @Test
    void onlyNoEnergyDarkensTheLamp() {
        assertTrue(Readout.dark(MachineStatus.NO_ENERGY));
        assertFalse(Readout.dark(MachineStatus.STARVED));
        assertFalse(Readout.dark(MachineStatus.IDLE));
    }

    @Test
    void ordinalsAreWireFormat() {
        assertEquals(0, Readout.REST.ordinal());
        assertEquals(1, Readout.WORK.ordinal());
        assertEquals(2, Readout.STALL.ordinal());
        assertEquals(3, Readout.HOT.ordinal());
        assertEquals(Readout.REST, Readout.byOrdinal(99));
        assertEquals(Readout.REST, Readout.of(null, 0));
    }
}
