package za.co.neroland.nerotech.recipe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import org.junit.jupiter.api.Test;

/**
 * The parts of {@link ProcessRecipe} that run without a Minecraft bootstrap: the fluid/gas amount
 * record and its JSON + network codecs (round trips for a maximal and a minimal entry), medium and tag
 * matching, and the counted item matching every process machine shares.
 *
 * <p>The full recipe codec embeds vanilla {@code Ingredient} and item-registry codecs, which need a
 * bootstrapped registry; no NeroTech test bootstraps one, so the whole-recipe round trip is left to
 * the runtime check and these tests lock the pure data and matching logic instead.
 */
class ProcessRecipeDataTest {

    @Test
    void mediumJsonRoundTripMaximalAndMinimal() {
        for (ProcessMedium medium : List.of(
                new ProcessMedium("#c:sulfuric_acid", Integer.MAX_VALUE),
                new ProcessMedium("nerotech:helium_3", 1))) {
            JsonElement json = ProcessMedium.CODEC.encodeStart(JsonOps.INSTANCE, medium).getOrThrow();
            assertEquals(medium, ProcessMedium.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
        }
    }

    @Test
    void mediumJsonFieldNamesAreFrozen() {
        JsonElement json = JsonParser.parseString("{\"id\": \"minecraft:water\", \"amount\": 250}");
        ProcessMedium medium = ProcessMedium.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
        assertEquals(new ProcessMedium("minecraft:water", 250), medium);
        assertTrue(ProcessMedium.CODEC.parse(JsonOps.INSTANCE,
                JsonParser.parseString("{\"id\": \"minecraft:water\", \"amount\": 0}")).isError(),
                "a zero amount is rejected");
    }

    @Test
    void mediumStreamRoundTrip() {
        ByteBuf buf = Unpooled.buffer();
        try {
            ProcessMedium medium = new ProcessMedium("#c:polymer_resin", 1_000);
            ProcessMedium.STREAM_CODEC.encode(buf, medium);
            assertEquals(medium, ProcessMedium.STREAM_CODEC.decode(buf));
        } finally {
            buf.release();
        }
    }

    @Test
    void mediumMatching() {
        ProcessMedium water = new ProcessMedium("minecraft:water", 500);
        assertTrue(water.satisfiedBy("minecraft:water", 500L, 1L, tag -> false));
        assertFalse(water.satisfiedBy("minecraft:water", 499L, 1L, tag -> false));
        assertFalse(water.satisfiedBy("minecraft:lava", 9_999L, 1L, tag -> false));
        assertFalse(water.satisfiedBy(null, 9_999L, 1L, tag -> false));

        ProcessMedium acid = new ProcessMedium("#c:sulfuric_acid", 100);
        assertTrue(acid.isTag());
        assertEquals("c:sulfuric_acid", acid.key());
        Set<String> tags = Set.of("c:sulfuric_acid");
        assertTrue(acid.satisfiedBy("nerotech:sulfuric_acid", 100L, 1L, tags::contains));
        assertFalse(acid.satisfiedBy("nerotech:sulfuric_acid", 100L, 1L, tag -> false));

        ProcessMedium deuterium = new ProcessMedium("nerotech:deuterium", 2);
        assertTrue(deuterium.satisfiedBy("nerotech:deuterium", 200L, 100L, tag -> false), "gas units are 100 mB");
        assertFalse(deuterium.satisfiedBy("nerotech:deuterium", 199L, 100L, tag -> false));
    }

    @Test
    void countedItemsAreSpentOnce() {
        record Stack(String id, int count) {
        }
        List<Stack> stacks = List.of(new Stack("iron", 3), new Stack("coal", 1));
        ProcessMatching.Requirement<Stack> twoIron = new ProcessMatching.Requirement<>(s -> s.id().equals("iron"), 2);
        ProcessMatching.Requirement<Stack> oneIron = new ProcessMatching.Requirement<>(s -> s.id().equals("iron"), 1);
        ProcessMatching.Requirement<Stack> oneCoal = new ProcessMatching.Requirement<>(s -> s.id().equals("coal"), 1);
        assertTrue(ProcessMatching.itemsSatisfied(List.of(twoIron, oneIron, oneCoal), stacks, Stack::count));
        assertFalse(ProcessMatching.itemsSatisfied(List.of(twoIron, twoIron), stacks, Stack::count),
                "one stack of three cannot serve two requirements of two");
        assertFalse(ProcessMatching.itemsSatisfied(List.of(new ProcessMatching.Requirement<Stack>(s -> true, 5)),
                stacks, Stack::count));
        assertTrue(ProcessMatching.itemsSatisfied(List.of(), List.of(), Stack::count), "no items needed, none given");
    }

    @Test
    void chanceRolls() {
        assertTrue(ProcessMatching.rolls(1000, 999));
        assertTrue(ProcessMatching.rolls(250, 249));
        assertFalse(ProcessMatching.rolls(250, 250));
    }
}
