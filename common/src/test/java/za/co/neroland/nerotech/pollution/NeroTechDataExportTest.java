package za.co.neroland.nerotech.pollution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import com.google.gson.JsonObject;

import org.junit.jupiter.api.Test;

/** The NeroTech export carries only what the erasure hook erases, and nothing when nothing is stored. Pure JVM. */
class NeroTechDataExportTest {

    @Test
    void nothingStoredExportsNull() {
        assertNull(NeroTechDataExport.build(null, null, false, List.of()));
        assertNull(NeroTechDataExport.build(null, null, false, List.of(0, 0, 0)));
    }

    @Test
    void attributionOptOutAndMasksAreExported() {
        JsonObject out = NeroTechDataExport.build(42, 20_000L, false, List.of(3, 0));
        assertEquals(42, out.getAsJsonObject("pollutionAttribution").get("attributedPollution").getAsInt());
        assertEquals(20_000L, out.getAsJsonObject("pollutionAttribution").get("lastUpdatedEpochDay").getAsLong());
        assertFalse(out.get("pollutionAttributionOptedOut").getAsBoolean());
        assertEquals(2, out.getAsJsonArray("techGuideSeenStepMasks").size());
        assertTrue(out.has("machineOwnership"));
    }

    @Test
    void optOutAloneIsStillExported() {
        JsonObject out = NeroTechDataExport.build(null, null, true, List.of());
        assertTrue(out.get("pollutionAttributionOptedOut").getAsBoolean());
        assertFalse(out.has("pollutionAttribution"));
        assertFalse(out.has("techGuideSeenStepMasks"));
    }

    @Test
    void exportNeverCarriesAnIdentifier() {
        String json = NeroTechDataExport.build(7, 1L, true, List.of(1)).toString();
        assertFalse(json.matches("(?s).*[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}.*"));
    }
}
