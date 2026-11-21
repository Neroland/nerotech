package za.co.neroland.nerotech.deposit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;

import org.junit.jupiter.api.Test;

/** Pure tests for deposit seeding, the table parser and the depletion store (no game bootstrap). */
class DepositTest {

    private static final List<DepositTable.Entry> TABLE = List.of(
            new DepositTable.Entry("c:ores/iron", null, 10, 2000, 6000, 600, 1200),
            new DepositTable.Entry("c:ores/copper", null, 5, 1000, 1000, 1000, 1000),
            new DepositTable.Entry("c:ores/none", null, 0, 1, 1, 1, 1));

    @Test
    void seedingIsDeterministic() {
        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 5; z++) {
                assertEquals(DepositSeed.of(42L, "minecraft:overworld", x, z, TABLE),
                        DepositSeed.of(42L, "minecraft:overworld", x, z, TABLE));
            }
        }
    }

    @Test
    void seedingVariesAndRespectsRangesAndWeights() {
        Set<Long> keys = new HashSet<>();
        Set<Integer> picked = new HashSet<>();
        for (int x = 0; x < 64; x++) {
            DepositSeed.Deposit d = DepositSeed.of(7L, "minecraft:overworld", x, 3, TABLE);
            assertNotNull(d);
            picked.add(d.entryIndex());
            assertTrue(d.entryIndex() != 2, "a weight-0 entry is never chosen");
            DepositTable.Entry e = TABLE.get(d.entryIndex());
            assertTrue(d.capacity() >= e.capacityMin() && d.capacity() <= e.capacityMax());
            assertTrue(d.richness() >= e.richnessMin() && d.richness() <= e.richnessMax());
            keys.add(DepositSeed.key(7L, "minecraft:overworld", x, 3));
        }
        assertEquals(64, keys.size(), "every chunk gets its own key");
        assertEquals(Set.of(0, 1), picked);
        assertFalse(DepositSeed.key(7L, "minecraft:overworld", 1, 1) == DepositSeed.key(7L, "minecraft:the_nether", 1, 1));
        assertFalse(DepositSeed.key(7L, "minecraft:overworld", 1, 1) == DepositSeed.key(8L, "minecraft:overworld", 1, 1));
    }

    @Test
    void emptyTableHasNoDeposit() {
        assertNull(DepositSeed.of(1L, "minecraft:the_end", 0, 0, List.of()));
    }

    @Test
    void depletionAndRecovery() {
        DepositState state = new DepositState();
        String key = DepositState.key("minecraft:overworld", 4, -9);
        assertEquals(1000, state.remaining(key, 1000, 0L, 240), "an untapped chunk is full");
        assertEquals(0, state.size(), "reading never creates a record");

        assertEquals(600, state.extract(key, 1000, 600, 0L, 240));
        assertEquals(400, state.peek(key, 1000, 0L, 240));
        assertEquals(400, state.extract(key, 1000, 999, 0L, 240), "only what is left can be taken");
        assertEquals(0, state.extract(key, 1000, 10, 0L, 240), "an exhausted deposit yields nothing");

        // 240 units per day: half a day later 120 are back.
        assertEquals(120, state.peek(key, 1000, DepositState.DAY_TICKS / 2, 240));
        assertEquals(1, state.size());
        // Recovery is capped at capacity, and a full record is pruned on read.
        assertEquals(1000, state.remaining(key, 1000, DepositState.DAY_TICKS * 10, 240));
        assertEquals(0, state.size());
    }

    @Test
    void noRecoveryWhenRateIsZero() {
        DepositState state = new DepositState();
        String key = DepositState.key("nerospace:mars", 0, 0);
        state.extract(key, 500, 500, 0L, 0);
        assertEquals(0, state.peek(key, 500, DepositState.DAY_TICKS * 100, 0));
    }

    @Test
    void pruneIsBoundedAndKeepsUnrecovered() {
        DepositState state = new DepositState();
        for (int i = 0; i < 10; i++) {
            state.extract(DepositState.key("minecraft:overworld", i, 0), 100, 50, 0L, 100);
        }
        state.extract(DepositState.key("minecraft:overworld", 99, 0), 100_000, 100_000, 0L, 100);
        assertEquals(11, state.size());
        long later = DepositState.DAY_TICKS; // 100 back: the small ones are full, the big one is not
        assertEquals(4, state.prune(later, 100, 4));
        assertEquals(7, state.size());
        assertEquals(6, state.prune(later, 100, 256));
        assertEquals(1, state.size());
    }

    @Test
    void storeRoundTripsAndResetsOnSeedVersionChange() {
        DepositState state = new DepositState();
        String key = DepositState.key("minecraft:overworld", 1, 2);
        state.extract(key, 800, 300, 50L, 0);
        com.google.gson.JsonElement json = DepositState.codec().encodeStart(JsonOps.INSTANCE, state).result().orElseThrow();
        DepositState back = DepositState.codec().parse(JsonOps.INSTANCE, json).result().orElseThrow();
        assertEquals(500, back.peek(key, 800, 50L, 0));

        JsonObject other = json.getAsJsonObject().deepCopy();
        other.addProperty("seedVersion", DepositSeed.SEED_VERSION + 1);
        DepositState reset = DepositState.codec().parse(JsonOps.INSTANCE, other).result().orElseThrow();
        assertEquals(0, reset.size(), "records from another seeding function are discarded");
    }

    @Test
    void disabledDimensionList() {
        assertTrue(DepositService.disabled("minecraft:the_end", "minecraft:the_nether, minecraft:the_end"));
        assertFalse(DepositService.disabled("minecraft:overworld", "minecraft:the_nether,minecraft:the_end"));
        assertFalse(DepositService.disabled("minecraft:overworld", ""));
    }

    @Test
    void yieldScalesByRichnessAndConfig() {
        assertEquals(100, DepositService.yieldFor(100, 1000, 1000));
        assertEquals(60, DepositService.yieldFor(100, 600, 1000));
        assertEquals(30, DepositService.yieldFor(100, 600, 500));
        assertEquals(0, DepositService.yieldFor(100, 600, 0));
    }

    @Test
    void tableParserMergesReplacesAndSkipsMalformed() {
        JsonObject a = JsonParser.parseString("{\"dimension\":\"minecraft:overworld\",\"entries\":["
                + "{\"ore\":\"#c:ores/iron\",\"weight\":10,\"capacity\":[2000,6000],\"richness\":[600,1200]},"
                + "{\"ore\":\"#c:ores/bad\",\"weight\":0},"
                + "{\"weight\":3}]}").getAsJsonObject();
        JsonObject b = JsonParser.parseString("{\"dimension\":\"minecraft:overworld\",\"entries\":["
                + "{\"ore\":\"c:ores/coal\",\"output\":\"minecraft:coal\",\"capacity\":[9,1]}]}").getAsJsonObject();
        DepositTable merged = DepositTable.parse(List.of(a, b));
        List<DepositTable.Entry> e = merged.rawEntries("minecraft:overworld");
        assertEquals(2, e.size());
        assertEquals("c:ores/iron", e.get(0).ore());
        assertEquals("minecraft:coal", e.get(1).output());
        assertEquals(1, e.get(1).capacityMin());
        assertEquals(9, e.get(1).capacityMax());

        JsonObject replace = JsonParser.parseString("{\"dimension\":\"minecraft:overworld\",\"replace\":true,"
                + "\"entries\":[{\"ore\":\"#c:ores/gold\"}]}").getAsJsonObject();
        assertEquals(1, DepositTable.parse(List.of(a, b, replace)).rawEntries("minecraft:overworld").size());
        assertTrue(merged.rawEntries("minecraft:the_end").isEmpty());
    }
}
