package za.co.neroland.nerotech.link;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import za.co.neroland.nerolandcore.link.LinkActionHandler;
import za.co.neroland.nerolandcore.link.LinkActionResult;
import za.co.neroland.nerolandcore.link.LinkEvent;
import za.co.neroland.nerolandcore.link.NeroLinkRegistry;

import za.co.neroland.nerotech.machine.MachinePreset;

/**
 * The Stage 7 security fixes (decision 007, amendment 2026-11-21): in-person reach and the owner rule
 * for handle presets (H1), one uniform refusal (H2, L2), shuffled {@code nearby} (M1), bystander
 * copies the bridge never pushes (M2), rate windows that survive a relog and a bounded opt-out budget
 * (L1), typed bodies (L3) and one reach rule for {@code nearby} and events (L5). Plain JVM.
 */
class LinkSecurityFixesTest {

    @BeforeAll
    static void registerModule() {
        NeroTechLinkModule.register();
    }

    private static LinkActionHandler handler() {
        return NeroLinkRegistry.actionHandler("nerotech").orElseThrow();
    }

    // --- H1 ------------------------------------------------------------------------------

    @Test
    void handlePresetsNeedInPersonReachOfSixteenBlocks() {
        assertEquals(16, PresetAccess.ACTION_REACH);
        assertTrue(PresetAccess.inActionReach(0.5D, 0.0D, 0.5D, 0, 0, 0), "standing on it");
        assertTrue(PresetAccess.inActionReach(0.5D, 0.5D, 16.5D, 0, 0, 0), "exactly 16 blocks");
        assertFalse(PresetAccess.inActionReach(0.5D, 0.5D, 17.0D, 0, 0, 0), "past 16 blocks");
        assertFalse(PresetAccess.inActionReach(12.5D, 12.5D, 0.5D, 0, 0, 0), "a 12 x 12 diagonal is 17 blocks");
        assertFalse(PresetAccess.inActionReach(100.5D, 0.5D, 0.5D, 0, 0, 0),
                "nearby reach (128) is not action reach");
    }

    @Test
    void someoneElsesRecordedOwnerIsRefusedAnOwnerlessOrOwnMachineIsNot() {
        UUID me = UUID.randomUUID();
        assertTrue(PresetAccess.ownerAllows(null, me), "ownerless: allowed only within in-person reach");
        assertTrue(PresetAccess.ownerAllows(me, me));
        assertFalse(PresetAccess.ownerAllows(UUID.randomUUID(), me));
    }

    // --- H2 / L2 -------------------------------------------------------------------------

    @Test
    void everyRefusalIsByteIdentical() {
        LinkActionResult first = PresetAccess.refuse(PresetAccess.Refusal.values()[0]);
        assertFalse(first.ok());
        assertEquals(PresetAccess.UNAVAILABLE, first.message());
        for (PresetAccess.Refusal why : PresetAccess.Refusal.values()) {
            LinkActionResult r = PresetAccess.refuse(why);
            assertFalse(r.ok(), why.name());
            assertEquals(first.error(), r.error(), "same code for " + why);
            assertEquals(first.message(), r.message(), "same message for " + why);
            assertNull(r.state(), "no state for " + why);
        }
    }

    // --- L3 ------------------------------------------------------------------------------

    @Test
    void malformedValuesAreRejectedNotThrown() {
        assertTrue(PresetAccess.preset(new JsonPrimitive("ECO")).isPresent());
        assertEquals(MachinePreset.OVERDRIVE, PresetAccess.preset(new JsonPrimitive("OVERDRIVE")).orElseThrow());
        assertTrue(PresetAccess.preset(null).isEmpty());
        assertTrue(PresetAccess.preset(JsonNull.INSTANCE).isEmpty());
        assertTrue(PresetAccess.preset(new JsonArray()).isEmpty());
        assertTrue(PresetAccess.preset(new JsonPrimitive(1)).isEmpty());
        assertTrue(PresetAccess.preset(new JsonPrimitive("eco")).isEmpty());

        assertEquals(12, PresetAccess.integer(new JsonPrimitive(12)).orElseThrow());
        assertTrue(PresetAccess.integer(new JsonPrimitive("12")).isEmpty());
        assertTrue(PresetAccess.integer(new JsonPrimitive(1.5D)).isEmpty());
        assertTrue(PresetAccess.integer(new JsonPrimitive(1e12)).isEmpty());
        assertTrue(PresetAccess.integer(new JsonObject()).isEmpty());

        JsonObject flag = new JsonObject();
        flag.addProperty("enabled", "false");
        assertTrue(PresetAccess.bool(flag, "enabled").isEmpty(), "a string is not a boolean");
        assertTrue(PresetAccess.bool(null, "enabled").isEmpty());
    }

    @Test
    void attributionToggleWithAWrongTypeIsAValidationAnswer() {
        for (JsonElement bad : List.of(new JsonPrimitive("false"), new JsonArray(), JsonNull.INSTANCE,
                new JsonPrimitive(0))) {
            JsonObject params = new JsonObject();
            params.add("enabled", bad);
            LinkActionResult r = handler().execute(UUID.randomUUID(), "set_pollution_attribution", params);
            assertFalse(r.ok());
            assertEquals(LinkActionResult.Error.VALIDATION, r.error(), "for " + bad);
        }
    }

    @Test
    void malformedPresetBodiesNeverThrow() {
        JsonObject handle = new JsonObject();
        handle.add("h", new JsonArray());
        handle.add("preset", JsonNull.INSTANCE);
        JsonObject coords = new JsonObject();
        coords.addProperty("dim", "minecraft:overworld");
        coords.addProperty("x", "one");
        coords.add("y", new JsonObject());
        coords.addProperty("z", 1);
        coords.add("preset", new JsonArray());
        for (JsonObject body : List.of(handle, coords, new JsonObject())) {
            LinkActionResult r = handler().execute(UUID.randomUUID(), "set_machine_preset", body);
            assertFalse(r.ok());
        }
    }

    // --- M1 ------------------------------------------------------------------------------

    @Test
    void nearbyOrderAndTruncationAreRandom() {
        Random random = new Random(7L);
        Set<Integer> firstSeen = new HashSet<>();
        Set<Integer> dropped = new HashSet<>();
        for (int run = 0; run < 40; run++) {
            List<Integer> found = new ArrayList<>();
            for (int i = 0; i < 130; i++) {
                found.add(i);
            }
            assertTrue(NearbyFormat.shuffleAndCap(found, random, 128));
            assertEquals(128, found.size());
            assertEquals(128, new HashSet<>(found).size(), "no duplicates");
            firstSeen.add(found.get(0));
            for (int i = 0; i < 130; i++) {
                if (!found.contains(i)) {
                    dropped.add(i);
                }
            }
        }
        assertTrue(firstSeen.size() > 10, "scan order does not decide the first entry");
        assertTrue(dropped.size() > 10, "truncation does not always drop the last-scanned entries");

        List<Integer> few = new ArrayList<>(List.of(1, 2, 3));
        assertFalse(NearbyFormat.shuffleAndCap(few, random, 128), "nothing cut under the cap");
        assertEquals(3, few.size());
    }

    // --- L5 ------------------------------------------------------------------------------

    @Test
    void eventsAndNearbyShareOneReachRule() {
        assertTrue(NearbyFormat.visible(8, 64, 8, 8 + 64, 64, 8), "inside the chunk square");
        assertTrue(NearbyFormat.inReach(8, 64, 8, 8 + 100, 64, 8), "inside the 128 box");
        assertFalse(NearbyFormat.visible(8, 64, 8, 8 + 100, 64, 8),
                "outside the 9 x 9 chunk square nearby scans, so no event either");
        assertFalse(NearbyFormat.visible(8, 64, 8, 8, 64 + 129, 8), "outside the box vertically");
    }

    // --- M2 ------------------------------------------------------------------------------

    /** The bridge's push rule, as read in the bridge (no bridge change): alerts push only when raised. */
    private static boolean bridgeWouldPush(LinkEvent e) {
        if (!"alerts".equals(e.topic())) {
            return true;
        }
        JsonElement verb = e.payload() == null ? null : e.payload().get("event");
        return verb == null || verb.isJsonNull() || "raised".equals(verb.getAsString());
    }

    @Test
    void onlyARecordedOwnersCopyIsPushEligible() {
        UUID owner = UUID.randomUUID();
        UUID bystander = UUID.randomUUID();

        LinkEvent own = MachineAlarmLink.alarmEvent(owner, owner, "nerotech:centrifuge", "starved");
        assertEquals("alarm", own.topic());
        assertTrue(own.payload().has("_notify"));
        assertTrue(bridgeWouldPush(own));

        LinkEvent other = MachineAlarmLink.alarmEvent(bystander, owner, "nerotech:centrifuge", "starved");
        assertEquals("alerts", other.topic());
        assertEquals(bystander, other.playerId());
        assertFalse(other.isBroadcast());
        assertFalse(other.payload().has("_notify"));
        assertEquals("nearby", other.payload().get("event").getAsString());
        assertEquals("alarm", other.payload().get("source").getAsString());
        assertEquals("nerotech:centrifuge", other.payload().get("type").getAsString());
        assertFalse(bridgeWouldPush(other), "a bystander is never pushed");

        LinkEvent ownerless = MachineAlarmLink.alarmEvent(bystander, null, "nerotech:centrifuge", "starved");
        assertFalse(bridgeWouldPush(ownerless), "an ownerless machine pushes nobody");

        LinkEvent melt = MachineAlarmLink.meltdownEvent(bystander, owner, "meltdown", 5);
        assertFalse(bridgeWouldPush(melt));
        assertEquals("meltdown", melt.payload().get("source").getAsString());
        assertFalse(melt.payload().has("x"));
        LinkEvent ownMelt = MachineAlarmLink.meltdownEvent(owner, owner, "meltdown", 5);
        assertEquals("meltdown", ownMelt.topic());
        assertNotEquals("alerts", ownMelt.topic());
    }

    @Test
    void bystanderNoticeDropsReservedMembers() {
        JsonObject body = NearbyFormat.alarmPayload("nerotech:blast_foundry", "overheat");
        body.add("_notify", new JsonObject());
        body.addProperty("event", "raised");
        JsonObject notice = NearbyFormat.bystanderNotice("alarm", body);
        assertFalse(notice.has("_notify"));
        assertEquals("nearby", notice.get("event").getAsString());
        assertEquals("overheat", notice.get("alarm").getAsString());
    }

    // --- L1 ------------------------------------------------------------------------------

    @Test
    void rateWindowsExpireByTimeNotByLogout() {
        AtomicLong now = new AtomicLong(0L);
        LinkRateLimiter limiter = new LinkRateLimiter(now::get);
        UUID player = UUID.randomUUID();
        for (int i = 0; i < LinkRateLimiter.MAX_ACTIONS; i++) {
            assertTrue(limiter.tryAcquire(player));
        }
        now.set(5_000L);
        limiter.pruneExpired();
        assertTrue(limiter.holds(player), "a relog five seconds later keeps the window");
        assertFalse(limiter.tryAcquire(player), "and the budget stays spent");
        now.set(LinkRateLimiter.WINDOW_MS + 1L);
        limiter.pruneExpired();
        assertFalse(limiter.holds(player), "dropped once nothing in it can refuse a call");
    }

    @Test
    void optOutsHaveTheirOwnBoundedBudget() {
        AtomicLong now = new AtomicLong(0L);
        LinkRateLimiter optOuts = new LinkRateLimiter(now::get, LinkRateLimiter.MAX_OPT_OUTS);
        UUID player = UUID.randomUUID();
        for (int i = 0; i < LinkRateLimiter.MAX_OPT_OUTS; i++) {
            assertTrue(optOuts.tryAcquire(player));
        }
        assertFalse(optOuts.tryAcquire(player), "bounded, though generous");
        assertTrue(LinkRateLimiter.MAX_OPT_OUTS > LinkRateLimiter.MAX_ACTIONS);
    }

    @Test
    void discoveryReportsTheReleaseVersion() {
        assertEquals("0.8.0-beta.1", NeroTechLinkModule.MOD_VERSION);
    }
}
