package za.co.neroland.nerotech.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;

import org.junit.jupiter.api.Test;

import za.co.neroland.nerolandcore.event.ThresholdEvents;
import za.co.neroland.nerolandcore.event.ThresholdEvents.ThresholdCrossing;

/**
 * Locks the {@link MachineFailureEvents} wire contract an add-on or NeroEvents listens on: the
 * channel id, the {@code machineId@dimension:packedPos} scope (a machine and a place — never a
 * player), and stage/rising riding the crossing's value/threshold. Plain JVM: Core's
 * {@link ThresholdEvents} is a static listener list and {@link BlockPos} needs no bootstrap.
 */
class MachineFailureEventsTest {

    @Test
    void channelIsNamespacedToNerotech() {
        assertEquals("nerotech", MachineFailureEvents.CHANNEL.getNamespace());
        assertEquals("machine_failure", MachineFailureEvents.CHANNEL.getPath());
    }

    @Test
    void scopeIsMachineAtDimensionColonPackedPos() {
        BlockPos pos = new BlockPos(10, 64, -20);
        String scope = MachineFailureEvents.scope("nerotech:fusion_reactor", "minecraft:overworld", pos);
        assertEquals("nerotech:fusion_reactor@minecraft:overworld:" + pos.asLong(), scope);
    }

    @Test
    void fireReachesCoreListenersWithStageAsValueAndThreshold() {
        List<ThresholdCrossing> heard = new ArrayList<>();
        ThresholdEvents.onCrossing(crossing -> {
            if (crossing.channel().equals(MachineFailureEvents.CHANNEL)) {
                heard.add(crossing);
            }
        });
        BlockPos pos = new BlockPos(1, 2, 3);

        MachineFailureEvents.fire("nerotech:fusion_reactor", "minecraft:the_nether", pos,
                MachineFailureEvents.STAGE_FAILURE, true);
        MachineFailureEvents.fire("nerotech:fusion_reactor", "minecraft:the_nether", pos,
                MachineFailureEvents.STAGE_WARNING, false);

        assertEquals(2, heard.size());
        ThresholdCrossing meltdown = heard.get(0);
        assertEquals(MachineFailureEvents.CHANNEL, meltdown.channel());
        assertEquals(MachineFailureEvents.scope("nerotech:fusion_reactor", "minecraft:the_nether", pos),
                meltdown.scope());
        assertEquals(3L, meltdown.value());
        assertEquals(3L, meltdown.threshold());
        assertTrue(meltdown.rising());

        ThresholdCrossing recovered = heard.get(1);
        assertEquals(1L, recovered.value());
        assertFalse(recovered.rising(), "a falling edge is the end of the episode");
    }

    @Test
    void stagesAreOrderedWorstLast() {
        assertTrue(MachineFailureEvents.STAGE_WARNING < MachineFailureEvents.STAGE_UNSTABLE);
        assertTrue(MachineFailureEvents.STAGE_UNSTABLE < MachineFailureEvents.STAGE_FAILURE);
        assertTrue(MachineFailureEvents.STAGE_FAILURE < MachineFailureEvents.STAGE_BREACH);
    }
}
