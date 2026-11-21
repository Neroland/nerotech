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
 * The {@code alarm} link event (schema 2, decision 007) and the proximity delivery it shares with the
 * {@code meltdown} event.
 *
 * <p><b>Hook.</b> The machine base calls {@link #onAlarmMaskChanged} when its server-side alarm mask
 * changes (compare-and-record in its readout sync, so at most once per change, never per tick). Only
 * newly raised bits count. Annunciator Panels are skipped: their mask repeats their neighbours'.
 *
 * <p><b>Delivery.</b> {@code LinkEvent.forPlayer} to each online player in the machine's dimension
 * within {@link NearbyFormat#RADIUS} blocks, plus the machine's recorded owner (only recorded under the
 * existing attribution opt-in), at most one per machine type per recipient per 30 s. The body is the
 * block id and one alarm id: no position, no owner, no player. Only the owner's copy carries the
 * {@code _notify} text the bridge uses for a rich push; nobody else is pushed anything of substance.
 * Never a broadcast.
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
                JsonObject payload = NearbyFormat.alarmPayload(type, alarm);
                if (recipient.equals(owner)) {
                    JsonObject notify = new JsonObject();
                    notify.addProperty("title", "NeroTech alarm");
                    notify.addProperty("body", readable(type) + ": " + alarm);
                    payload.add("_notify", notify);
                }
                NeroLinkRegistry.eventBus().publish(LinkEvent.forPlayer(NeroTechLinkModule.MODULE_ID, "alarm",
                        recipient, payload));
            }
        } catch (RuntimeException e) {
            NeroTechCommon.LOGGER.debug("[NeroTech] alarm link event skipped", e);
        }
    }

    /**
     * Online players in {@code level} within reach of {@code pos}, then {@code owner} if recorded. Ordered,
     * de-duplicated. Nobody outside the dimension or the box is included, whoever they are.
     */
    static Set<UUID> recipients(ServerLevel level, BlockPos pos, @Nullable UUID owner) {
        Set<UUID> out = new LinkedHashSet<>();
        for (ServerPlayer player : level.players()) {
            BlockPos at = player.blockPosition();
            if (NearbyFormat.inReach(pos.getX(), pos.getY(), pos.getZ(), at.getX(), at.getY(), at.getZ())) {
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
