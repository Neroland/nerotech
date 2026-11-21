package za.co.neroland.nerotech.progression;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import za.co.neroland.nerotech.api.MachineFailureEvents;

/** The pure credit decisions for the meltdown-averted and vault-operated hooks (operator in the moment). */
class OperatorCreditTest {

    static final UUID OP = UUID.fromString("00000000-0000-4000-8000-0000000000a1");
    static final UUID OTHER = UUID.fromString("00000000-0000-4000-8000-0000000000b2");

    @Test
    void meltdownAvertedNeedsStageThreeOrWorseARestoreAndAnOperator() {
        assertTrue(OperatorCredit.meltdownAverted(MachineFailureEvents.STAGE_FAILURE, true, OP));
        assertTrue(OperatorCredit.meltdownAverted(MachineFailureEvents.STAGE_BREACH, true, OP));
        assertFalse(OperatorCredit.meltdownAverted(MachineFailureEvents.STAGE_UNSTABLE, true, OP), "stage 2 is not a meltdown");
        assertFalse(OperatorCredit.meltdownAverted(MachineFailureEvents.STAGE_WARNING, true, OP));
        assertFalse(OperatorCredit.meltdownAverted(0, true, OP), "a first forming is not a recovery");
        assertFalse(OperatorCredit.meltdownAverted(MachineFailureEvents.STAGE_BREACH, false, OP), "not restored yet");
        assertFalse(OperatorCredit.meltdownAverted(MachineFailureEvents.STAGE_BREACH, true, null), "no operator, no credit");
    }

    @Test
    void vaultOperatedOnceAStorePerOperator() {
        assertTrue(OperatorCredit.vaultOperated(1, OP, null));
        assertFalse(OperatorCredit.vaultOperated(0, OP, null), "nothing stored");
        assertFalse(OperatorCredit.vaultOperated(64, null, null), "no operator (placed by a machine, or after reload)");
        assertFalse(OperatorCredit.vaultOperated(64, OP, OP), "already tried for this operator");
        assertTrue(OperatorCredit.vaultOperated(64, OTHER, OP), "a new operator is tried");
    }
}
