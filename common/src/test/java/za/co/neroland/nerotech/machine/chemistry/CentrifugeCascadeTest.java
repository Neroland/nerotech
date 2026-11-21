package za.co.neroland.nerotech.machine.chemistry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import com.google.gson.JsonObject;

import org.junit.jupiter.api.Test;

import za.co.neroland.nerotech.machine.components.RecipeFixture;

/**
 * Locks the cascade rule: deuterium becomes helium-3 only at the end of a three-centrifuge chain; a
 * lone centrifuge (or two) cannot, positions cap at four, and only a linked neighbour extends a chain.
 * The stage table mirrors the shipped {@code centrifuging_*_cascade_stage_*} recipes, whose amounts
 * (3 → 2, 2 → 2, 2 → 1 at 20/20/30 FE/t × 80 ticks) are locked below. Pure JVM.
 */
class CentrifugeCascadeTest {

    private static final String D = "nerotech:deuterium";
    private static final String HE3 = "nerotech:helium_3";

    /** The three shipped stage recipes (gas_in, gas_out, atmosphere). */
    private static final List<CentrifugeCascade.Stage> STAGES = List.of(
            new CentrifugeCascade.Stage("cascade_1", D, D),
            new CentrifugeCascade.Stage("cascade_2", D, D),
            new CentrifugeCascade.Stage("cascade_3", D, HE3));

    @Test
    void threeChainMakesHeliumThreeOnlyAtTheEnd() {
        assertEquals(HE3, CentrifugeCascade.runChain(STAGES, 3, D));
        assertEquals(D, CentrifugeCascade.run(STAGES, 1, D).orElseThrow());
        assertEquals(D, CentrifugeCascade.run(STAGES, 2, D).orElseThrow());
        assertEquals(HE3, CentrifugeCascade.run(STAGES, 3, D).orElseThrow());
    }

    @Test
    void aSingleCentrifugeCannot() {
        assertEquals(D, CentrifugeCascade.runChain(STAGES, 1, D));
        assertEquals(D, CentrifugeCascade.runChain(STAGES, 2, D));
        // A lone centrifuge is position 1 whatever it is fed, so even pre-enriched gas stays deuterium.
        int lone = CentrifugeCascade.position(CentrifugeCascade.upstreamCount(i -> false));
        assertEquals(1, lone);
        assertFalse(CentrifugeCascade.run(STAGES, lone, D).orElseThrow().equals(HE3));
    }

    @Test
    void positionsCountLinkedNeighboursAndCapAtFour() {
        assertEquals(3, CentrifugeCascade.position(CentrifugeCascade.upstreamCount(i -> i <= 2)));
        assertEquals(4, CentrifugeCascade.position(CentrifugeCascade.upstreamCount(i -> true)));
        assertEquals(CentrifugeCascade.MAX, CentrifugeCascade.position(99));
        // A gap stops the walk: only the neighbours before the first unlinked block count.
        assertEquals(2, CentrifugeCascade.position(CentrifugeCascade.upstreamCount(i -> i != 2)));
        assertTrue(CentrifugeCascade.pushesDownstream(3));
        assertFalse(CentrifugeCascade.pushesDownstream(CentrifugeCascade.MAX));
        assertEquals("cascade_4", CentrifugeCascade.stageKey(7));
        assertEquals("cascade_1", CentrifugeCascade.stageKey(0));
    }

    @Test
    void theFourthStageHoldsHeliumThree() {
        // Nothing consumes helium-3, so a fourth centrifuge only buffers it.
        assertEquals(HE3, CentrifugeCascade.runChain(STAGES, 4, D));
        assertTrue(CentrifugeCascade.run(STAGES, 4, HE3).isEmpty());
    }

    @Test
    void shippedStageAmountsAreTheBalancedCascade() {
        // stage file, gas in, gas out, FE/t, ticks: 3 deuterium → 1 helium-3 for 5,600 FE a run.
        Object[][] rows = {
                {"centrifuging_deuterium_cascade_stage_1", 3, 2, 20, 80},
                {"centrifuging_deuterium_cascade_stage_2", 2, 2, 20, 80},
                {"centrifuging_helium_3_cascade_stage_3", 2, 1, 30, 80}};
        int fe = 0;
        for (Object[] row : rows) {
            JsonObject recipe = RecipeFixture.resource("data/nerotech/recipe/" + row[0] + ".json").orElseThrow();
            assertEquals(row[1], recipe.getAsJsonObject("gas_in").get("amount").getAsInt(), row[0] + " gas_in");
            assertEquals(row[2], recipe.getAsJsonObject("gas_out").get("amount").getAsInt(), row[0] + " gas_out");
            assertEquals(row[3], recipe.get("energy").getAsInt(), row[0] + " energy");
            assertEquals(row[4], recipe.get("time").getAsInt(), row[0] + " time");
            fe += (int) row[3] * (int) row[4];
        }
        assertEquals(5_600, fe);
    }
}
