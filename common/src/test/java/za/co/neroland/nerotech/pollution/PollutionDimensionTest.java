package za.co.neroland.nerotech.pollution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;

import net.minecraft.core.BlockPos;

import org.junit.jupiter.api.Test;

/**
 * Pollution is kept per dimension (0.8.0, store schema 2): a reading in one dimension never shows in
 * another at the same x/z, and a schema-1 save (one flat region list) loads into the overworld.
 */
class PollutionDimensionTest {

    private static final String MARS = "nerospace:mars";
    private static final long REGION = PollutionManager.regionKey(new BlockPos(0, 64, 0));

    @Test
    void readingInOneDimensionNeverAppearsInAnother() {
        PollutionState state = new PollutionState();
        state.addRegion(MARS, REGION, 40);
        assertEquals(40, state.region(MARS, REGION));
        assertEquals(0, state.region(PollutionState.OVERWORLD, REGION), "same x/z, other dimension: nothing");
        assertEquals(0, state.region("minecraft:the_nether", REGION));
        assertEquals(0, state.region(REGION), "the legacy overload reads the overworld only");

        // Scrubbing the overworld cannot touch Mars.
        assertEquals(0, state.takeRegion(PollutionState.OVERWORLD, REGION, 40));
        assertEquals(40, state.region(MARS, REGION));
        assertTrue(state.regionsIn(PollutionState.OVERWORLD).isEmpty());
    }

    @Test
    void schemaOneSaveLoadsIntoTheOverworld() {
        JsonElement old = JsonParser.parseString("{\"regions\":[{\"key\":" + REGION + ",\"amount\":7},"
                + "{\"key\":99,\"amount\":3}],\"attribution\":[]}");
        PollutionState state = PollutionState.codec().parse(JsonOps.INSTANCE, old).result().orElseThrow();
        assertEquals(7, state.region(PollutionState.OVERWORLD, REGION));
        assertEquals(3, state.region(PollutionState.OVERWORLD, 99L));
        assertEquals(0, state.region(MARS, REGION));
        assertEquals(java.util.Set.of(PollutionState.OVERWORLD), state.dimensions());
    }

    @Test
    void schemaTwoRoundTripKeepsDimensions() {
        PollutionState state = new PollutionState();
        state.addRegion(MARS, REGION, 11);
        state.addRegion(PollutionState.OVERWORLD, REGION, 5);
        JsonElement json = PollutionState.codec().encodeStart(JsonOps.INSTANCE, state).result().orElseThrow();
        assertEquals(PollutionState.SCHEMA, json.getAsJsonObject().get("schema").getAsInt());
        assertTrue(!json.getAsJsonObject().has("regions") || json.getAsJsonObject().getAsJsonArray("regions").isEmpty(),
                "the flat schema-1 list is never written again");
        PollutionState back = PollutionState.codec().parse(JsonOps.INSTANCE, json).result().orElseThrow();
        assertEquals(11, back.region(MARS, REGION));
        assertEquals(5, back.region(PollutionState.OVERWORLD, REGION));
    }

    @Test
    void decayWalksEveryDimensionAndNamesIt() {
        PollutionState state = new PollutionState();
        state.addRegion(MARS, REGION, 12);
        state.addRegion(PollutionState.OVERWORLD, REGION, 3);
        List<String> recovered = new ArrayList<>();
        state.decayAll(5, 10, (dim, key) -> recovered.add(PollutionManager.scope(dim, key)));
        assertEquals(7, state.region(MARS, REGION));
        assertEquals(0, state.region(PollutionState.OVERWORLD, REGION));
        assertEquals(List.of(MARS + "|" + REGION), recovered);
        assertEquals(java.util.Set.of(MARS), state.dimensions(), "emptied dimensions are dropped");
    }
}
