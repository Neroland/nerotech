package za.co.neroland.nerotech.fluid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.jupiter.api.Test;

/**
 * Decision 002: every entry of NeroTech's {@code nerotech:inputs/*} item tags points at another mod's
 * material and must be written {@code {"id": ..., "required": false}}, so the tag still loads on a
 * Core + NeroTech server without Nerospace. Also checks the three liquids ship their fluid tags.
 */
class InputTagsTest {

    @Test
    void everyInputTagEntryIsOptional() throws IOException, URISyntaxException {
        Path root = resourcesRoot();
        Path inputs = root.resolve("data/nerotech/tags/item/inputs");
        List<String> problems = new ArrayList<>();
        int files = 0;
        try (Stream<Path> walk = Files.walk(inputs)) {
            for (Path file : walk.filter(p -> p.toString().endsWith(".json")).toList()) {
                files++;
                JsonObject tag = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
                for (JsonElement value : tag.getAsJsonArray("values")) {
                    if (!value.isJsonObject() || !value.getAsJsonObject().has("required")
                            || value.getAsJsonObject().get("required").getAsBoolean()) {
                        problems.add(inputs.relativize(file) + ": " + value);
                    }
                }
            }
        }
        // Decision 002's eighteen, plus inputs/brine (the Reaction Vat's optional Nerospace brine bucket).
        assertEquals(19, files, "decision 002 lists eighteen input tags, plus brine");
        assertTrue(problems.isEmpty(), "entries not written required:false: " + problems);
    }

    @Test
    void liquidsShipTheirFluidTags() throws URISyntaxException {
        Path root = resourcesRoot();
        for (String tag : List.of("data/c/tags/fluid/sulfuric_acid.json", "data/c/tags/fluid/polymer_resin.json",
                "data/nerotech/tags/fluid/cryo_coolant.json")) {
            assertTrue(Files.isRegularFile(root.resolve(tag)), tag);
        }
    }

    /** The common resources root, found through the test classpath (skips when packaged as a jar). */
    static Path resourcesRoot() throws URISyntaxException {
        URL marker = InputTagsTest.class.getClassLoader().getResource("data/nerotech/tags/item/inputs/glacite.json");
        assumeTrue(marker != null && "file".equals(marker.getProtocol()), "tag resources not on the classpath as files");
        return Path.of(marker.toURI()).getParent().getParent().getParent().getParent().getParent().getParent();
    }
}
