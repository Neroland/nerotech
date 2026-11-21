package za.co.neroland.nerotech.compliance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.core.BlockPos;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import za.co.neroland.nerolandcore.data.ErasureConformance;
import za.co.neroland.nerolandcore.data.PlayerDataEraser;
import za.co.neroland.nerolandcore.data.PlayerDataErasure;
import za.co.neroland.nerotech.guide.TechGuide;
import za.co.neroland.nerotech.guide.TechGuideSeenState;
import za.co.neroland.nerotech.pollution.PollutionAttributionPrefs;
import za.co.neroland.nerotech.pollution.PollutionManager;
import za.co.neroland.nerotech.pollution.PollutionState;

/**
 * Core's erasure conformance harness over every store NeroTech keeps for a player: opt-in pollution
 * attribution (dimension-keyed regional store), the attribution opt-out, and the Tech Guide "seen" masks.
 * Plain JVM: the stores are constructed directly, and the eraser registered here mirrors the production one
 * in {@code NeroTechCommon.init()} minus the last-known-good backups (which need a live server). The
 * in-memory link state is covered by {@code link.LinkMemoryErasureTest} against the production eraser.
 */
class ErasureConformanceTest {

    private final List<PlayerDataEraser> registered = new ArrayList<>();

    @AfterEach
    void unregister() {
        registered.forEach(PlayerDataErasure::unregister);
        registered.clear();
    }

    @Test
    void erasurePurgesEveryPlayerStore() {
        UUID player = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        PollutionState pollution = new PollutionState();
        PollutionAttributionPrefs prefs = new PollutionAttributionPrefs();
        TechGuideSeenState seen = new TechGuideSeenState();
        long region = PollutionManager.regionKey(new BlockPos(300, 70, -40));

        // Seed, so the probes are not vacuous; a second player and the aggregate must survive.
        pollution.addRegion("minecraft:the_nether", region, 30);
        pollution.attribute(player, 30, 10L);
        pollution.attribute(other, 12, 10L);
        prefs.setEnabled(player, false);
        seen.markSeen(player, 0, 0);
        seen.markSeen(other, 0, 1);

        PlayerDataEraser eraser = (server, uuid) -> {
            pollution.forgetPlayer(uuid);
            prefs.forgetPlayer(uuid);
            prefs.markOwnerErasure(uuid, 20_000L);
            seen.forgetPlayer(uuid);
        };
        registered.add(eraser);
        PlayerDataErasure.register(eraser);

        ErasureConformance.create()
                .probe("nerotech:pollution_attribution", uuid -> pollution.attributed(uuid) > 0)
                .probe("nerotech:attribution_opt_out", prefs::isOptedOut)
                .probe("nerotech:tech_guide_seen", uuid -> anySeen(seen, uuid))
                .verify(null, player);

        assertEquals(12, pollution.attributed(other), "another player's row survives");
        assertTrue(anySeen(seen, other), "another player's guide progress survives");
        assertEquals(30, pollution.region("minecraft:the_nether", region), "aggregate world data survives");
        assertTrue(prefs.isPendingErasure(player), "machines still drop the erased owner on their next tick");
    }

    private static boolean anySeen(TechGuideSeenState seen, UUID player) {
        for (int chapter = 0; chapter < TechGuide.CHAPTER_COUNT; chapter++) {
            if (seen.mask(player, chapter) != 0) {
                return true;
            }
        }
        return false;
    }
}
