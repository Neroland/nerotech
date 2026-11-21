package za.co.neroland.nerotech.machine.extraction;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Survey Scanner rules: energy cost, cooldown, charging and the reported bands. Pure JVM. */
class ScannerRulesTest {

    @Test
    void scanCostsEnergyAndStartsCooldown() {
        ScannerState state = new ScannerState(1_000, 0L);
        assertEquals(ScannerRules.Outcome.SCANNED, ScannerRules.attempt(state.energy(), 500, 100L, state.readyAt()));
        state = ScannerRules.afterScan(state, 500, 100L, 40);
        assertEquals(500, state.energy());
        assertEquals(140L, state.readyAt());
        assertEquals(ScannerRules.Outcome.COOLDOWN, ScannerRules.attempt(state.energy(), 500, 139L, state.readyAt()));
        assertEquals(ScannerRules.Outcome.SCANNED, ScannerRules.attempt(state.energy(), 500, 140L, state.readyAt()));
        state = ScannerRules.afterScan(state, 500, 140L, 40);
        assertEquals(0, state.energy());
        assertEquals(ScannerRules.Outcome.NO_ENERGY, ScannerRules.attempt(state.energy(), 500, 500L, state.readyAt()));
    }

    @Test
    void cooldownWinsOverEnergyAndEnergyNeverNegative() {
        assertEquals(ScannerRules.Outcome.COOLDOWN, ScannerRules.attempt(0, 500, 10L, 20L));
        assertEquals(0, ScannerRules.afterScan(new ScannerState(100, 0L), 500, 0L, 0).energy());
    }

    @Test
    void chargingFillsOnlyTheRoomLeft() {
        assertEquals(3_000, ScannerRules.chargeAmount(7_000, 10_000, 1_000_000L));
        assertEquals(200, ScannerRules.chargeAmount(0, 10_000, 200L));
        assertEquals(0, ScannerRules.chargeAmount(10_000, 10_000, 5_000L));
    }

    @Test
    void bandsAndRecovery() {
        assertEquals(0, ScannerRules.band(0, 1000));
        assertEquals(1, ScannerRules.band(1, 1000));
        assertEquals(1, ScannerRules.band(200, 1000));
        assertEquals(2, ScannerRules.band(201, 1000));
        assertEquals(4, ScannerRules.band(800, 1000));
        assertEquals(5, ScannerRules.band(801, 1000));
        assertEquals(5, ScannerRules.band(1000, 1000));
        assertEquals(ScannerRules.Recovery.UNTOUCHED, ScannerRules.recovery(1000, 1000));
        assertEquals(ScannerRules.Recovery.RECOVERING, ScannerRules.recovery(999, 1000));
        assertEquals(ScannerRules.Recovery.EXHAUSTED, ScannerRules.recovery(0, 1000));
    }
}
