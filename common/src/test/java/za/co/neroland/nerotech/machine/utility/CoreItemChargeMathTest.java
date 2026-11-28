package za.co.neroland.nerotech.machine.utility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** How much an Induction Charger offers an item that stores Neroland Core energy. Pure JVM. */
class CoreItemChargeMathTest {

    @Test
    void offerIsCappedByTheChargersRate() {
        // 100 FE at 1 FE/NE into an item with 10,000 NE of room: the charger's offer is the limit.
        assertEquals(100L, ChargeMath.coreItemRequestNe(100L, 1.0, 0L, 10_000L));
    }

    @Test
    void offerIsCappedByTheItemsFreeRoom() {
        assertEquals(25L, ChargeMath.coreItemRequestNe(1_000L, 1.0, 975L, 1_000L));
    }

    @Test
    void fullOrBrokenItemsGetNothing() {
        assertEquals(0L, ChargeMath.coreItemRequestNe(1_000L, 1.0, 1_000L, 1_000L));
        assertEquals(0L, ChargeMath.coreItemRequestNe(1_000L, 1.0, 2_000L, 1_000L));
        assertEquals(0L, ChargeMath.coreItemRequestNe(1_000L, 1.0, 0L, 0L));
        assertEquals(0L, ChargeMath.coreItemRequestNe(1_000L, 1.0, 0L, -5L));
    }

    @Test
    void negativeStoredCountsAsEmpty() {
        assertEquals(50L, ChargeMath.coreItemRequestNe(50L, 1.0, -10L, 100L));
    }

    @Test
    void noOfferOrNoRatioGivesNothing() {
        assertEquals(0L, ChargeMath.coreItemRequestNe(0L, 1.0, 0L, 100L));
        assertEquals(0L, ChargeMath.coreItemRequestNe(-1L, 1.0, 0L, 100L));
        assertEquals(0L, ChargeMath.coreItemRequestNe(100L, 0.0, 0L, 100L));
        assertEquals(0L, ChargeMath.coreItemRequestNe(100L, Double.NaN, 0L, 100L));
    }

    @Test
    void ratioConvertsTheFeOfferToNeRoundingDown() {
        // 4 FE per NE: 10 FE pays for 2 NE, never 3.
        assertEquals(2L, ChargeMath.coreItemRequestNe(10L, 4.0, 0L, 100L));
        // 0.5 FE per NE: 10 FE pays for 20 NE.
        assertEquals(20L, ChargeMath.coreItemRequestNe(10L, 0.5, 0L, 100L));
    }

    @Test
    void reportedFeNeverExceedsTheOfferAndCoversWhatTheItemGained() {
        double ratio = 4.0;
        long maxFe = 10L;
        long request = ChargeMath.coreItemRequestNe(maxFe, ratio, 0L, 100L);
        long fe = ChargeMath.acceptedFe(request, ratio, maxFe);
        assertTrue(fe <= maxFe);
        assertTrue(fe >= request * ratio - 1e-9);
    }

    @Test
    void acceptedAmountIsClampedToWhatWasAsked() {
        assertEquals(30L, ChargeMath.clampAccepted(30L, 40L));
        assertEquals(40L, ChargeMath.clampAccepted(90L, 40L));
        assertEquals(0L, ChargeMath.clampAccepted(-3L, 40L));
        assertEquals(0L, ChargeMath.clampAccepted(10L, 0L));
    }
}
