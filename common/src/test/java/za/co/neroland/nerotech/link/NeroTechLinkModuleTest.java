package za.co.neroland.nerotech.link;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.UUID;

import com.google.gson.JsonObject;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import za.co.neroland.nerolandcore.link.LinkActionHandler;
import za.co.neroland.nerolandcore.link.LinkActionResult;
import za.co.neroland.nerolandcore.link.LinkModuleInfo;
import za.co.neroland.nerolandcore.link.LinkSnapshotProvider;
import za.co.neroland.nerolandcore.link.NeroLinkRegistry;

/**
 * Plain-JVM tests for NeroTech's NeroLink module. No game bootstrap and no running server: every
 * assertion here exercises a code path that resolves deterministically when the module's captured
 * server is absent — registration/discovery, the PUBLIC wiki section (classpath/fallback only), the
 * own-data sections' no-server branches, and pre-server action validation. Server-dependent behaviour
 * (machine presets) is covered in-game during runtime verification.
 */
class NeroTechLinkModuleTest {

    @BeforeAll
    static void registerModule() {
        NeroTechLinkModule.register();
    }

    private static LinkSnapshotProvider provider() {
        return NeroLinkRegistry.snapshotProvider("nerotech").orElseThrow();
    }

    private static LinkActionHandler handler() {
        return NeroLinkRegistry.actionHandler("nerotech").orElseThrow();
    }

    @Test
    void registersDiscoveryMetadataWithExpectedSectionsAndActions() {
        LinkModuleInfo info = NeroLinkRegistry.module("nerotech").orElseThrow();
        assertEquals("nerotech", info.moduleId());
        assertEquals(1, info.schemaVersion());
        assertTrue(info.dataSections().containsAll(
                java.util.List.of("pollution", "guide", "wiki")),
                "all three data sections must be advertised");
        assertFalse(info.dataSections().contains("gates"),
                "the gates section was removed with the progression gates (standalone-first design)");
        assertTrue(info.actionIds().containsAll(
                java.util.List.of("set_pollution_attribution", "set_machine_preset")),
                "both actions must be advertised");
    }

    @Test
    void wikiIndexListsPages() {
        JsonObject index = provider().snapshot(UUID.randomUUID(), "wiki", Map.of());
        assertEquals("nerotech", index.get("mod").getAsString());
        assertTrue(index.has("pages"), "wiki index must carry a pages array");
        assertTrue(index.getAsJsonArray("pages").size() > 0, "the page list must not be empty");
        assertTrue(index.has("asOf"));
    }

    @Test
    void wikiUnknownPageReturnsErrorObject() {
        JsonObject page = provider().snapshot(UUID.randomUUID(), "wiki",
                Map.of("page", "___definitely_not_a_page___"));
        assertEquals("unknown page", page.get("error").getAsString());
        assertEquals("___definitely_not_a_page___", page.get("slug").getAsString());
    }

    @Test
    void wikiKnownPageReturnsMarkdownWhenBundled() {
        // Only meaningful when the Gradle wiki-copy step put the pages on the classpath; skip otherwise.
        Assumptions.assumeTrue(WikiLibrary.content("Home").isPresent(),
                "wiki markdown resources not bundled on the test classpath");
        JsonObject page = provider().snapshot(UUID.randomUUID(), "wiki", Map.of("page", "Home"));
        assertEquals("nerotech", page.get("mod").getAsString());
        assertEquals("Home", page.get("slug").getAsString());
        assertEquals("markdown", page.get("format").getAsString());
        assertFalse(page.get("content").getAsString().isEmpty(), "page content must not be empty");
    }

    @Test
    void pollutionSectionIsEmptyWithNoteWhenUnavailable() {
        // No server captured ⇒ attribution is treated as off: the section returns the privacy note and
        // never any personal/aggregate pollution (own-data-only contract).
        JsonObject out = provider().snapshot(UUID.randomUUID(), "pollution", Map.of());
        assertFalse(out.get("attributionEnabled").getAsBoolean());
        assertTrue(out.has("note"), "the off state must explain the opt-out posture");
        assertFalse(out.has("attributed"), "no attributed total may be exposed when attribution is off");
    }

    @Test
    void removedGatesSectionAnswersAsUnknown() {
        // The gates section was deleted with the progression gates: an old client asking for it must
        // get the graceful unknown-section note, not a crash.
        JsonObject out = provider().snapshot(UUID.randomUUID(), "gates", Map.of());
        assertEquals("gates", out.get("section").getAsString());
        assertTrue(out.has("note"), "unknown sections must answer with an explanatory note");
    }

    @Test
    void guideSectionReportsChapterTotals() {
        JsonObject out = provider().snapshot(UUID.randomUUID(), "guide", Map.of());
        assertTrue(out.get("chapters").getAsInt() > 0);
        assertTrue(out.get("totalSteps").getAsInt() > 0);
        assertEquals(0, out.get("seenSteps").getAsInt(), "no server ⇒ no seen progress");
    }

    @Test
    void unknownActionIsValidationError() {
        LinkActionResult result = handler().execute(UUID.randomUUID(), "bogus_action", new JsonObject());
        assertFalse(result.ok());
        assertEquals(LinkActionResult.Error.VALIDATION, result.error());
    }

    @Test
    void setPollutionAttributionRejectsMissingEnabledParam() {
        LinkActionResult result =
                handler().execute(UUID.randomUUID(), "set_pollution_attribution", new JsonObject());
        assertFalse(result.ok());
        assertEquals(LinkActionResult.Error.VALIDATION, result.error());
    }
}
