package za.co.neroland.nerotech.machine.utility;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import za.co.neroland.nerotech.machine.MachineAlarms;

/** Annunciator Panel rules (decision 013): mask aggregation, comparator mapping, fresh lamps. Pure JVM. */
class AnnunciatorMathTest {

    @Test
    void masksOrTogetherAndStayInFiveBits() {
        assertEquals(0, AnnunciatorMath.aggregate());
        assertEquals(MachineAlarms.OVERHEAT | MachineAlarms.FULL,
                AnnunciatorMath.aggregate(MachineAlarms.OVERHEAT, MachineAlarms.FULL, 0));
        assertEquals(AnnunciatorMath.ALL, AnnunciatorMath.aggregate(-1));
        assertEquals(MachineAlarms.STARVED, AnnunciatorMath.aggregate(MachineAlarms.STARVED, MachineAlarms.STARVED));
    }

    @Test
    void anyChannelCountsLitAlarms() {
        assertEquals(0, AnnunciatorMath.comparator(0, AnnunciatorMath.CHANNEL_ANY));
        assertEquals(3, AnnunciatorMath.comparator(MachineAlarms.FAILING, AnnunciatorMath.CHANNEL_ANY));
        assertEquals(6, AnnunciatorMath.comparator(MachineAlarms.FAILING | MachineAlarms.FULL, 0));
        assertEquals(15, AnnunciatorMath.comparator(AnnunciatorMath.ALL, 0));
    }

    @Test
    void alarmChannelIsFifteenOnlyForItsAlarm() {
        for (int bit = 0; bit < MachineAlarms.COUNT; bit++) {
            int channel = bit + 1;
            assertEquals(15, AnnunciatorMath.comparator(1 << bit, channel));
            assertEquals(0, AnnunciatorMath.comparator(AnnunciatorMath.ALL & ~(1 << bit), channel));
            assertEquals(MachineAlarms.IDS[bit], AnnunciatorMath.channelId(channel));
        }
    }

    @Test
    void channelsCycleAndBadValuesFallBackToAny() {
        int channel = AnnunciatorMath.CHANNEL_ANY;
        for (int i = 0; i < AnnunciatorMath.CHANNEL_COUNT; i++) {
            channel = AnnunciatorMath.nextChannel(channel);
        }
        assertEquals(AnnunciatorMath.CHANNEL_ANY, channel);
        assertEquals(AnnunciatorMath.CHANNEL_ANY, AnnunciatorMath.sanitizeChannel(42));
        assertEquals(AnnunciatorMath.CHANNEL_ANY, AnnunciatorMath.sanitizeChannel(-1));
        assertEquals("any", AnnunciatorMath.channelId(99));
        assertEquals(6, AnnunciatorMath.comparator(MachineAlarms.FAILING | MachineAlarms.FULL, 99));
    }

    @Test
    void onlyNewlyLitBitsRise() {
        assertEquals(MachineAlarms.FULL, AnnunciatorMath.risen(MachineAlarms.OVERHEAT,
                MachineAlarms.OVERHEAT | MachineAlarms.FULL));
        assertEquals(0, AnnunciatorMath.risen(AnnunciatorMath.ALL, MachineAlarms.FULL));
    }

    @Test
    void freshLampsBlinkForAHundredTicks() {
        long[] risenAt = new long[MachineAlarms.COUNT];
        Arrays.fill(risenAt, Long.MIN_VALUE / 2);
        assertEquals(MachineAlarms.FULL, AnnunciatorMath.fresh(0, MachineAlarms.FULL, risenAt, 1000));
        assertEquals(MachineAlarms.FULL, AnnunciatorMath.fresh(MachineAlarms.FULL, MachineAlarms.FULL, risenAt, 1099));
        assertEquals(0, AnnunciatorMath.fresh(MachineAlarms.FULL, MachineAlarms.FULL, risenAt, 1100));
        // A cleared alarm is never fresh; lighting again restarts the blink.
        assertEquals(0, AnnunciatorMath.fresh(MachineAlarms.FULL, 0, risenAt, 1101));
        assertEquals(MachineAlarms.FULL, AnnunciatorMath.fresh(0, MachineAlarms.FULL, risenAt, 1200));
    }
}
