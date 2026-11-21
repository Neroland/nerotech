package za.co.neroland.nerotech.item.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.junit.jupiter.api.Test;

import za.co.neroland.nerotech.machine.components.RecipeFixture;

/**
 * Power Drill vein mode is restricted to {@code nerotech:power_drill/vein}: ores,
 * gravel, clay and glowstone walk a vein; stone and the other quarry blocks never do. Pure JVM over the
 * shipped tag file; the in-world break is on the runtime script.
 */
class PowerDrillVeinTagTest {

    private static Set<String> veinTag() {
        JsonObject tag = RecipeFixture.resource("data/nerotech/tags/block/power_drill/vein.json")
                .orElseThrow(() -> new AssertionError("nerotech:power_drill/vein is missing"));
        assertFalse(tag.has("replace") && tag.get("replace").getAsBoolean(), "the tag must not replace others");
        Set<String> values = new HashSet<>();
        for (JsonElement value : tag.getAsJsonArray("values")) {
            values.add(value.isJsonObject() ? value.getAsJsonObject().get("id").getAsString() : value.getAsString());
        }
        return values;
    }

    @Test
    void defaultTagIsOresGravelClayGlowstone() {
        assertEquals(Set.of("#c:ores", "minecraft:gravel", "minecraft:clay", "minecraft:glowstone"), veinTag());
        assertEquals("power_drill/vein", ToolRules.DRILL_VEIN_TAG_PATH, "PowerDrillItem.VEIN_BLOCKS reads this path");
    }

    @Test
    void noQuarryBlockWalksAVein() {
        Set<String> values = veinTag();
        for (String quarry : new String[] {"minecraft:stone", "minecraft:deepslate", "minecraft:cobblestone",
                "minecraft:netherrack", "minecraft:end_stone", "minecraft:dirt", "minecraft:sand",
                "#minecraft:base_stone_overworld", "#minecraft:base_stone_nether", "#minecraft:mineable/pickaxe",
                "#minecraft:mineable/shovel"}) {
            assertFalse(values.contains(quarry), quarry + " would make vein mode a free quarry");
        }
    }

    @Test
    void onlyTaggedBlocksWalk() {
        assertTrue(ToolRules.drillWalksVein(true), "a tagged block walks its vein");
        assertFalse(ToolRules.drillWalksVein(false), "anything else breaks alone");
    }
}
