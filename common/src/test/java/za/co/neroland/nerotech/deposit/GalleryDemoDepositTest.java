package za.co.neroland.nerotech.deposit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The gallery's endless demo deposit serves only the exhibit drill, never a survival drill beside it. Pure JVM. */
class GalleryDemoDepositTest {

    @Test
    void onlyAnExhibitInTheDemoChunkReadsTheDemoDeposit() {
        assertTrue(DepositService.demoApplies(true, true));
        assertFalse(DepositService.demoApplies(false, true), "a survival drill in the demo chunk depletes normally");
        assertFalse(DepositService.demoApplies(true, false), "an exhibit elsewhere reads the world");
        assertFalse(DepositService.demoApplies(false, false));
    }
}
