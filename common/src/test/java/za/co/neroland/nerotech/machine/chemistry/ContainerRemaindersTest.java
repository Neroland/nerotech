package za.co.neroland.nerotech.machine.chemistry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Locks the Reaction Vat's remainder rule: a brine bucket (any filled bucket) returns an empty bucket. Pure JVM. */
class ContainerRemaindersTest {

    @Test
    void brineBucketReturnsAnEmptyBucket() {
        assertEquals("minecraft:bucket", ContainerRemainders.remainderFor("nerospace:brine_bucket").orElseThrow());
        assertEquals("minecraft:bucket", ContainerRemainders.remainderFor("minecraft:water_bucket").orElseThrow());
    }

    @Test
    void otherItemsLeaveNothing() {
        assertTrue(ContainerRemainders.remainderFor("minecraft:bucket").isEmpty());
        assertTrue(ContainerRemainders.remainderFor("minecraft:blue_ice").isEmpty());
        assertTrue(ContainerRemainders.remainderFor("nerotech:sulfur_dust").isEmpty());
        assertTrue(ContainerRemainders.remainderFor("nerotech:_bucket").isEmpty());
        assertTrue(ContainerRemainders.remainderFor(null).isEmpty());
    }
}
