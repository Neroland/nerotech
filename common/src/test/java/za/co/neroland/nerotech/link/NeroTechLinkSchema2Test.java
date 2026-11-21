package za.co.neroland.nerotech.link;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import za.co.neroland.nerolandcore.link.LinkActionHandler;
import za.co.neroland.nerolandcore.link.LinkActionResult;
import za.co.neroland.nerolandcore.link.LinkSnapshotProvider;
import za.co.neroland.nerolandcore.link.NeroLinkRegistry;

import za.co.neroland.nerotech.link.ChainCatalogue.Stack;
import za.co.neroland.nerotech.link.ChainCatalogue.Step;
import za.co.neroland.nerotech.machine.MachineAlarms;
import za.co.neroland.nerotech.machine.Readout;

/**
 * Link schema 2: the {@code nearby} reach rule and handles, the action rate limit, the
 * public {@code catalogue}, the coordinate-free {@code meltdown} body, the opted-out {@code pollution}
 * shape, and that the schema-1 sections keep their shape. Plain JVM: no game bootstrap, no server.
 */
class NeroTechLinkSchema2Test {

    /** Words that must never appear as a key in any schema-2 payload. */
    private static final Set<String> FORBIDDEN_KEYS = Set.of("x", "y", "z", "pos", "position", "dim",
            "dimension", "owner", "ownerId", "player", "playerId", "uuid", "name", "direction", "distance");

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

    private static void assertNoForbiddenKeys(JsonElement json) {
        if (json.isJsonObject()) {
            for (Map.Entry<String, JsonElement> e : json.getAsJsonObject().entrySet()) {
                assertFalse(FORBIDDEN_KEYS.contains(e.getKey()), "forbidden key in payload: " + e.getKey());
                assertNoForbiddenKeys(e.getValue());
            }
        } else if (json.isJsonArray()) {
            for (JsonElement e : json.getAsJsonArray()) {
                assertNoForbiddenKeys(e);
            }
        }
    }

    // --- nearby ---------------------------------------------------------------------------

    @Test
    void nearbyIsEmptyForARequesterWhoIsNotOnline() {
        // No server / not online: the same empty list as "nothing in range", so the two are indistinguishable.
        JsonObject out = provider().snapshot(UUID.randomUUID(), "nearby", Map.of());
        assertEquals(0, out.getAsJsonArray("machines").size());
        assertFalse(out.get("truncated").getAsBoolean());
        assertNoForbiddenKeys(out);
    }

    @Test
    void reachIsA128BlockBoxOnEveryAxis() {
        assertTrue(NearbyFormat.inReach(0, 64, 0, 128, 64 - 128, -128));
        assertFalse(NearbyFormat.inReach(0, 64, 0, 129, 64, 0), "one block past the box is out of range");
        assertFalse(NearbyFormat.inReach(0, 64, 0, 0, 64 + 129, 0), "vertical reach is capped too");
        assertFalse(NearbyFormat.inReach(0, 64, 0, 0, 64, -129));
        assertTrue(NearbyFormat.MAX_ENTRIES <= 128);
        assertEquals(9, 2 * NearbyFormat.CHUNK_RADIUS + 1, "scan is at most 9 x 9 loaded chunks");
    }

    @Test
    void entryCarriesNoPlaceOwnerOrDirection() {
        JsonObject entry = NearbyFormat.entry("nerotech:blast_foundry", Readout.HOT,
                MachineAlarms.OVERHEAT | MachineAlarms.FULL, 820, 970, null, "abc-0", true, false);
        assertEquals(Set.of("type", "state", "alarms", "energyPct", "heatPct", "tankPct", "h", "ownedByYou"),
                entry.keySet());
        assertFalse(entry.get("ownedByYou").getAsBoolean());
        assertTrue(entry.get("ownedByYou").getAsJsonPrimitive().isBoolean(), "a boolean, never an identity");
        assertTrue(NearbyFormat.entry("nerotech:blast_foundry", Readout.HOT, 0, 0, 0, null, "abc-2", true, true)
                .get("ownedByYou").getAsBoolean());
        assertEquals("overheating", entry.get("state").getAsString());
        assertEquals(82, entry.get("energyPct").getAsInt());
        assertEquals(97, entry.get("heatPct").getAsInt());
        assertTrue(entry.get("tankPct").isJsonNull(), "no tank reads as null");
        JsonArray alarms = entry.getAsJsonArray("alarms");
        assertEquals(2, alarms.size());
        assertEquals("overheat", alarms.get(0).getAsString());
        assertEquals("full", alarms.get(1).getAsString());
        assertNoForbiddenKeys(entry);
    }

    @Test
    void stateWordsFollowReadoutAndAlarms() {
        assertEquals("idle", NearbyFormat.state(Readout.REST, 0));
        assertEquals("working", NearbyFormat.state(Readout.WORK, 0));
        assertEquals("starved", NearbyFormat.state(Readout.STALL, MachineAlarms.STARVED));
        assertEquals("full", NearbyFormat.state(Readout.STALL, MachineAlarms.FULL));
        assertEquals("overheating", NearbyFormat.state(Readout.HOT, 0));
        assertEquals("failing", NearbyFormat.state(Readout.WORK, MachineAlarms.FAILING));
        assertEquals("failing", NearbyFormat.firstAlarm(MachineAlarms.FAILING | MachineAlarms.STARVED));
        assertNull(NearbyFormat.firstAlarm(0));
    }

    @Test
    void annunciatorPanelsReportNoAlarms() {
        JsonObject entry = NearbyFormat.entry("nerotech:annunciator_panel", Readout.WORK,
                MachineAlarms.OVERHEAT | MachineAlarms.FAILING, 1000, 0, null, "abc-1", false, false);
        assertEquals(0, entry.getAsJsonArray("alarms").size());
        assertEquals("working", entry.get("state").getAsString());
    }

    @Test
    void handleFromAnotherPlayersResponseIsRejected() {
        AtomicLong now = new AtomicLong(1_000L);
        NearbyHandles<String> handles = new NearbyHandles<>(now::get);
        UUID alice = UUID.randomUUID();
        UUID bob = UUID.randomUUID();
        List<String> aliceHandles = handles.issue(alice, List.of("alice-machine-0", "alice-machine-1"));
        handles.issue(bob, List.of("bob-machine-0"));

        assertEquals(Optional.of("alice-machine-1"), handles.resolve(alice, aliceHandles.get(1)));
        assertEquals(Optional.empty(), handles.resolve(bob, aliceHandles.get(0)),
                "bob cannot use a handle minted for alice, even at an index his own table has");
        assertEquals(Optional.empty(), handles.resolve(bob, aliceHandles.get(1)));
        assertEquals(Optional.empty(), handles.resolve(alice, "garbage"));
        assertEquals(Optional.empty(), handles.resolve(alice, aliceHandles.get(0) + "0"));
    }

    @Test
    void handlesExpireAfterThirtySecondsAndOnTheNextResponse() {
        AtomicLong now = new AtomicLong(0L);
        NearbyHandles<String> handles = new NearbyHandles<>(now::get);
        UUID player = UUID.randomUUID();
        String first = handles.issue(player, List.of("m")).get(0);
        now.set(NearbyHandles.TTL_MS);
        assertTrue(handles.resolve(player, first).isPresent(), "valid for 30 s");
        now.set(NearbyHandles.TTL_MS + 1);
        assertTrue(handles.resolve(player, first).isEmpty(), "expired after 30 s");

        String second = handles.issue(player, List.of("m")).get(0);
        String third = handles.issue(player, List.of("m")).get(0);
        assertTrue(handles.resolve(player, second).isEmpty(), "superseded by the requester's newer response");
        assertTrue(handles.resolve(player, third).isPresent());

        handles.retainOnly(Set.of());
        assertTrue(handles.resolve(player, third).isEmpty(), "dropped once the player is offline");
    }

    @Test
    void schemaTwoPresetWithoutServerIsRefusedAndRevealsNothing() {
        JsonObject params = new JsonObject();
        params.addProperty("h", "deadbeef-0");
        params.addProperty("preset", "ECO");
        LinkActionResult result = handler().execute(UUID.randomUUID(), "set_machine_preset", params);
        assertFalse(result.ok());
    }

    // --- rate limit -----------------------------------------------------------------------

    @Test
    void rateLimiterAllowsTenActionsAMinute() {
        AtomicLong now = new AtomicLong(0L);
        LinkRateLimiter limiter = new LinkRateLimiter(now::get);
        UUID player = UUID.randomUUID();
        for (int i = 0; i < LinkRateLimiter.MAX_ACTIONS; i++) {
            assertTrue(limiter.tryAcquire(player), "action " + (i + 1) + " is within budget");
        }
        assertFalse(limiter.tryAcquire(player), "the eleventh action in a minute is refused");
        assertTrue(limiter.tryAcquire(UUID.randomUUID()), "budgets are per player");
        now.set(LinkRateLimiter.WINDOW_MS);
        assertTrue(limiter.tryAcquire(player), "the window slides");
        limiter.forget(player);
        for (int i = 0; i < LinkRateLimiter.MAX_ACTIONS; i++) {
            assertTrue(limiter.tryAcquire(player), "cleared on logout");
        }
    }

    @Test
    void moduleEnforcesTheRateLimitButNeverOnAnOptOut() {
        UUID player = UUID.randomUUID();
        JsonObject preset = new JsonObject();
        preset.addProperty("h", "x-0");
        preset.addProperty("preset", "ECO");
        for (int i = 0; i < LinkRateLimiter.MAX_ACTIONS; i++) {
            LinkActionResult r = handler().execute(player, "set_machine_preset", preset);
            assertFalse(r.message() != null && r.message().startsWith("rate limited"), "within budget");
        }
        LinkActionResult limited = handler().execute(player, "set_machine_preset", preset);
        assertFalse(limited.ok());
        assertEquals(LinkActionResult.Error.VALIDATION, limited.error());
        assertTrue(limited.message().startsWith("rate limited"));

        JsonObject optOut = new JsonObject();
        optOut.addProperty("enabled", false);
        LinkActionResult privacy = handler().execute(player, "set_pollution_attribution", optOut);
        assertFalse(privacy.message() != null && privacy.message().startsWith("rate limited"),
                "an opt-out is a privacy control and is never rate limited");
    }

    @Test
    void alarmThrottleIsOnePerTypePerPlayerPerThirtySeconds() {
        AtomicLong now = new AtomicLong(0L);
        AlarmThrottle throttle = new AlarmThrottle(now::get);
        UUID player = UUID.randomUUID();
        assertTrue(throttle.tryFire(player, "nerotech:centrifuge"));
        assertFalse(throttle.tryFire(player, "nerotech:centrifuge"));
        assertTrue(throttle.tryFire(player, "nerotech:plate_press"), "a different type is its own budget");
        assertTrue(throttle.tryFire(UUID.randomUUID(), "nerotech:centrifuge"), "another player is their own budget");
        now.set(AlarmThrottle.WINDOW_MS);
        assertTrue(throttle.tryFire(player, "nerotech:centrifuge"));
    }

    @Test
    void alarmPayloadIsTypeAndAlarmOnly() {
        JsonObject payload = NearbyFormat.alarmPayload("nerotech:centrifuge", "starved");
        assertEquals(Set.of("type", "alarm"), payload.keySet());
        assertEquals("Centrifuge", MachineAlarmLink.readable("nerotech:centrifuge"));
        assertEquals("Blast foundry", MachineAlarmLink.readable("nerotech:blast_foundry"));
    }

    // --- meltdown -------------------------------------------------------------------------

    @Test
    void meltdownPayloadHasNoCoordinates() {
        JsonObject payload = NearbyFormat.meltdownPayload("meltdown", 5);
        assertEquals(Set.of("kind", "shell"), payload.keySet());
        assertFalse(payload.has("x") || payload.has("y") || payload.has("z"));
        assertNoForbiddenKeys(payload);
    }

    // --- catalogue ------------------------------------------------------------------------

    /** A fake game: every NeroTech id exists, items carry their first listed tag, and a few recipes are loaded. */
    private static final class FakeLookup implements CatalogueSection.Lookup {
        private final Set<String> missingRecipes;

        FakeLookup(Set<String> missingRecipes) {
            this.missingRecipes = missingRecipes;
        }

        @Override
        public boolean blockExists(String id) {
            return true;
        }

        @Override
        public boolean itemExists(String id) {
            return true;
        }

        @Override
        public boolean fluidExists(String id) {
            return true;
        }

        @Override
        public boolean itemHasTag(String itemId, String tagId) {
            return tagId.startsWith("c:") && !tagId.equals("c:dusts");
        }

        @Override
        public Optional<Step> recipe(String recipeId) {
            if (this.missingRecipes.contains(recipeId)) {
                return Optional.empty();
            }
            return Optional.of(new Step("nerotech:machine_for_" + recipeId.substring(recipeId.indexOf(':') + 1),
                    List.of(new Stack("item", "#c:in")), List.of(new Stack("item", "nerotech:out"))));
        }
    }

    @Test
    void catalogueIsIdenticalForEveryRequester() {
        JsonObject first = provider().snapshot(UUID.randomUUID(), "catalogue", Map.of());
        JsonObject second = provider().snapshot(UUID.randomUUID(), "catalogue", Map.of());
        assertEquals(first, second, "the catalogue never depends on who asks");
        assertTrue(first.has("machines") && first.has("materials") && first.has("chains"));

        JsonObject builtA = CatalogueSection.build(new FakeLookup(Set.of()));
        JsonObject builtB = CatalogueSection.build(new FakeLookup(Set.of()));
        assertEquals(builtA, builtB, "deterministic for the same game state");
        assertNoForbiddenKeys(builtA);
    }

    @Test
    void catalogueListsSixChainsWithBothRoutes() {
        JsonObject built = CatalogueSection.build(new FakeLookup(Set.of()));
        JsonArray chains = built.getAsJsonArray("chains");
        Set<String> ids = new HashSet<>();
        for (JsonElement c : chains) {
            ids.add(c.getAsJsonObject().get("id").getAsString());
            for (JsonElement p : c.getAsJsonObject().getAsJsonArray("products")) {
                JsonObject product = p.getAsJsonObject();
                assertTrue(product.has("standalone") && product.has("offworld"));
            }
        }
        assertEquals(Set.of("structural", "superconductor", "semiconductor", "optics", "chemistry", "fusion_fuel"), ids);
        JsonObject machine = built.getAsJsonArray("machines").get(0).getAsJsonObject();
        assertEquals(Set.of("id", "family", "descKey"), machine.keySet());
        JsonObject material = built.getAsJsonArray("materials").get(0).getAsJsonObject();
        assertEquals("nerotech:cindersteel_ingot", material.get("id").getAsString());
        assertEquals("c:ingots/cindersteel", material.getAsJsonArray("tags").get(0).getAsString());
    }

    @Test
    void missingRecipesAreSkippedNotFatal() {
        JsonObject full = CatalogueSection.build(new FakeLookup(Set.of()));
        JsonObject partial = CatalogueSection.build(new FakeLookup(Set.of("nerotech:pressing_cindersteel_plate")));
        JsonObject plateFull = product(full, "structural", "nerotech:cindersteel_plate");
        JsonObject platePartial = product(partial, "structural", "nerotech:cindersteel_plate");
        assertEquals(plateFull.getAsJsonArray("offworld").size() - 1,
                platePartial.getAsJsonArray("offworld").size(), "the missing step is dropped");
        JsonObject titanium = product(full, "structural", "nerotech:titanium_plate");
        assertEquals(0, titanium.getAsJsonArray("standalone").size(), "titanium plate has no standalone route");
    }

    private static JsonObject product(JsonObject catalogue, String chain, String productId) {
        for (JsonElement c : catalogue.getAsJsonArray("chains")) {
            if (chain.equals(c.getAsJsonObject().get("id").getAsString())) {
                for (JsonElement p : c.getAsJsonObject().getAsJsonArray("products")) {
                    if (productId.equals(p.getAsJsonObject().get("id").getAsString())) {
                        return p.getAsJsonObject();
                    }
                }
            }
        }
        throw new AssertionError("no product " + productId + " in " + chain);
    }

    // --- schema-1 sections unchanged ----------------------------------------------------------

    @Test
    void schemaOneSectionsKeepTheirShape() {
        JsonObject guide = provider().snapshot(UUID.randomUUID(), "guide", Map.of());
        assertEquals(Set.of("asOf", "chapters", "chaptersStarted", "totalSteps", "seenSteps"), guide.keySet());
        JsonObject pollution = provider().snapshot(UUID.randomUUID(), "pollution", Map.of());
        assertEquals(Set.of("asOf", "attributionEnabled", "note"), pollution.keySet());
        JsonObject wiki = provider().snapshot(UUID.randomUUID(), "wiki", Map.of());
        assertEquals(Set.of("mod", "title", "pages", "asOf"), wiki.keySet());
    }

    @Test
    void optedOutPollutionOmitsTheAttributedValue() {
        JsonObject optedOut = new JsonObject();
        NeroTechLinkModule.ownPollution(optedOut, true, 1240L, 30);
        assertFalse(optedOut.has("attributed"), "an opted-out requester is sent no attributed value at all");
        assertTrue(optedOut.get("optedOut").getAsBoolean());
        assertEquals(30, optedOut.get("retentionDays").getAsInt());

        JsonObject counting = new JsonObject();
        NeroTechLinkModule.ownPollution(counting, false, 1240L, 30);
        assertEquals(1240L, counting.get("attributed").getAsLong());
        assertEquals(Set.of("attributionEnabled", "optedOut", "attributed", "retentionDays"), counting.keySet());
    }
}
