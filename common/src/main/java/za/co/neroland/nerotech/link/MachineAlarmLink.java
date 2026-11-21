package za.co.neroland.nerotech.link;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import com.google.gson.JsonObject;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.link.LinkEvent;
import za.co.neroland.nerolandcore.link.NeroLinkRegistry;

import za.co.neroland.nerotech.NeroTechCommon;
import za.co.neroland.nerotech.machine.NeroTechMachineBlockEntity;
import za.co.neroland.nerotech.machine.utility.AnnunciatorPanelBlockEntity;

/**
 * The {@code alarm} link event (schema 2) and the proximity delivery it shares with the
 * {@code meltdown} event.
 *
 * <p><b>Hook.</b> The machine base calls {@link #onAlarmMaskChanged} when its server-side alarm mask
 * changes (compare-and-record in its readout sync, so at most once per change, never per tick). Only
 * newly raised bits count. Annunciator Panels are skipped: their mask repeats their neighbours'.
 *
 * <p><b>Delivery.</b> {@code LinkEvent.forPlayer} to each online player in the machine's dimension
 * within {@link NearbyFormat#RADIUS} blocks, plus the machine's recorded owner (only recorded under the
 * existing attribution opt-in), at most one per machine type per recipient per 30 s. The body is the
 * block id and one alarm id: no position, no owner, no player. Only the recorded owner's copy goes on
 * the {@code alarm} topic with the {@code _notify} text the bridge uses for a push; every other
 * recipient gets a {@link NearbyFormat#bystanderNotice bystander notice}, which the bridge never pushes.
 * An ownerless machine therefore pushes nobody. Never a broadcast.
 */
public final class MachineAlarmLink {

    static final AlarmThrottle THROTTLE = new AlarmThrottle(System::currentTimeMillis);

    private MachineAlarmLink() {
    }

    /**
     * Called by the machine base when its alarm mask changes on the server.
     *
     * @param before the previously recorded mask
     * @param after  the mask now
     */
    public static void onAlarmMaskChanged(ServerLevel level, NeroTechMachineBlockEntity machine, int before,
            int after) {
        int gained = after & ~before;
        if (gained == 0 || level == null || machine == null || machine instanceof AnnunciatorPanelBlockEntity) {
            return;
        }
        try {
            String alarm = NearbyFormat.firstAlarm(gained);
            if (alarm == null) {
                return;
            }
            String type = BuiltInRegistries.BLOCK.getKey(machine.getBlockState().getBlock()).toString();
            UUID owner = machine.ownerId();
            for (UUID recipient : recipients(level, machine.getBlockPos(), owner)) {
                if (!THROTTLE.tryFire(recipient, type)) {
                    continue;
                }
                NeroLinkRegistry.eventBus().publish(alarmEvent(recipient, owner, type, alarm));
            }
        } catch (RuntimeException e) {
            NeroTechCommon.LOGGER.debug("[NeroTech] alarm link event skipped", e);
        }
    }

    /**
     * The event for one recipient: the recorded owner's copy on {@code alarm} with push text, anyone
     * else's as a never-pushed bystander notice. Pure; {@code owner} null means nobody is the owner.
     */
    static LinkEvent alarmEvent(UUID recipient, @Nullable UUID owner, String type, String alarm) {
        JsonObject payload = NearbyFormat.alarmPayload(type, alarm);
        if (!recipient.equals(owner)) {
            return LinkEvent.forPlayer(NeroTechLinkModule.MODULE_ID, NearbyFormat.BYSTANDER_TOPIC, recipient,
                    NearbyFormat.bystanderNotice("alarm", payload));
        }
        JsonObject notify = new JsonObject();
        notify.addProperty("title", "NeroTech alarm");
        notify.addProperty("body", readable(type) + ": " + alarm);
        payload.add("_notify", notify);
        return LinkEvent.forPlayer(NeroTechLinkModule.MODULE_ID, "alarm", recipient, payload);
    }

    /** The {@code meltdown} event for one recipient, routed like {@link #alarmEvent}. Pure. */
    static LinkEvent meltdownEvent(UUID recipient, @Nullable UUID owner, String kind, int shellSize) {
        JsonObject payload = NearbyFormat.meltdownPayload(kind, shellSize);
        if (!recipient.equals(owner)) {
            return LinkEvent.forPlayer(NeroTechLinkModule.MODULE_ID, NearbyFormat.BYSTANDER_TOPIC, recipient,
                    NearbyFormat.bystanderNotice("meltdown", payload));
        }
        return LinkEvent.forPlayer(NeroTechLinkModule.MODULE_ID, "meltdown", recipient, payload);
    }

    /**
     * Online players in {@code level} who could see the machine at {@code pos} in {@code nearby}
     * ({@link NearbyFormat#visible}, the shared reach rule), then {@code owner} if recorded. Ordered,
     * de-duplicated. Nobody outside the dimension or that reach is included, whoever they are.
     */
    static Set<UUID> recipients(ServerLevel level, BlockPos pos, @Nullable UUID owner) {
        Set<UUID> out = new LinkedHashSet<>();
        for (ServerPlayer player : level.players()) {
            BlockPos at = player.blockPosition();
            if (NearbyFormat.visible(at.getX(), at.getY(), at.getZ(), pos.getX(), pos.getY(), pos.getZ())) {
                out.add(player.getUUID());
            }
        }
        if (owner != null) {
            out.add(owner);
        }
        return out;
    }

    /** {@code nerotech:blast_foundry} to {@code Blast foundry}, for the owner's push line. */
    static String readable(String blockId) {
        int colon = blockId.indexOf(':');
        String path = (colon >= 0 ? blockId.substring(colon + 1) : blockId).replace('_', ' ').trim();
        return path.isEmpty() ? blockId : Character.toUpperCase(path.charAt(0)) + path.substring(1);
    }

    static void forget(UUID player) {
        THROTTLE.forget(player);
    }

    static void retainOnly(Set<UUID> online) {
        THROTTLE.retainOnly(online);
    }

    static boolean holds(UUID player) {
        return THROTTLE.holds(player);
    }
}
