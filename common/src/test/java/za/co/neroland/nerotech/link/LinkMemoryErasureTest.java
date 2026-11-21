package za.co.neroland.nerotech.link;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import za.co.neroland.nerolandcore.data.ErasureConformance;

/**
 * POPIA/GDPR: the link module's in-memory state (nearby handles, action rate window, alarm throttle) is
 * dropped by Core's shared erasure hook at once, through the production eraser, and by the logout sweep.
 */
class LinkMemoryErasureTest {

    @Test
    void erasureDropsInMemoryLinkState() {
        UUID player = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        NeroTechLinkModule.registerErasure();
        seed(player);
        seed(other);
        assertTrue(NeroTechLinkModule.holdsPlayer(player));

        ErasureConformance.create()
                .probe("nerotech:link_handles", NeroTechLinkModule.HANDLES::holds)
                .probe("nerotech:link_rate_window", NeroTechLinkModule.LIMITER::holds)
                .probe("nerotech:link_alarm_throttle", MachineAlarmLink::holds)
                .verify(null, player);

        assertFalse(NeroTechLinkModule.holdsPlayer(player));
        assertTrue(NeroTechLinkModule.holdsPlayer(other), "another player's state is untouched");
        NeroTechLinkModule.forgetPlayer(other);
    }

    @Test
    void logoutSweepDropsOfflinePlayers() {
        UUID online = UUID.randomUUID();
        UUID gone = UUID.randomUUID();
        seed(online);
        seed(gone);
        Set<UUID> stillOnline = Set.of(online);
        NeroTechLinkModule.HANDLES.retainOnly(stillOnline);
        NeroTechLinkModule.LIMITER.retainOnly(stillOnline);
        MachineAlarmLink.retainOnly(stillOnline);
        assertFalse(NeroTechLinkModule.holdsPlayer(gone));
        assertTrue(NeroTechLinkModule.holdsPlayer(online));
        NeroTechLinkModule.forgetPlayer(online);
    }

    private static void seed(UUID player) {
        NeroTechLinkModule.HANDLES.issue(player, List.of());
        NeroTechLinkModule.LIMITER.tryAcquire(player);
        MachineAlarmLink.THROTTLE.tryFire(player, "nerotech:blast_foundry");
    }
}
