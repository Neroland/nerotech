package za.co.neroland.nerotech.command.gallery;

import java.util.List;

import com.mojang.brigadier.Command;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import za.co.neroland.nerotech.command.gallery.GalleryLayout.Bay;
import za.co.neroland.nerotech.pollution.PollutionManager;

/**
 * {@code /nerotech gallery tour [next|stop]}: walks the player through the plant's bays, one teleport and one
 * chat line per bay. Stateless: the current bay is the one whose stop the player stands nearest, so nothing
 * about the player is stored, not even in memory. Works only inside a gallery recorded in {@link GalleryState}.
 */
public final class GalleryTour {

    private GalleryTour() {
    }

    /** Start at the first bay. */
    public static int start(CommandSourceStack source, ServerPlayer player, ServerLevel level) {
        GalleryState.Site site = site(player, level);
        if (site == null) {
            source.sendFailure(Component.translatable("command.nerotech.gallery.tour.none"));
            return 0;
        }
        return visit(source, player, level, site, 0);
    }

    /** Move on to the bay after the nearest one; past the last, end the tour at the entrance. */
    public static int next(CommandSourceStack source, ServerPlayer player, ServerLevel level) {
        GalleryState.Site site = site(player, level);
        if (site == null) {
            source.sendFailure(Component.translatable("command.nerotech.gallery.tour.none"));
            return 0;
        }
        int next = nextStop(nearestStop(player.getBlockX() - site.x(), player.getBlockZ() - site.z()));
        if (next < 0) {
            return stop(source, player, level);
        }
        return visit(source, player, level, site, next);
    }

    /** End the tour: back to the gallery origin. */
    public static int stop(CommandSourceStack source, ServerPlayer player, ServerLevel level) {
        GalleryState.Site site = site(player, level);
        if (site == null) {
            source.sendFailure(Component.translatable("command.nerotech.gallery.tour.none"));
            return 0;
        }
        teleport(player, level, new BlockPos(site.x(), site.y(), site.z()), player.getYRot());
        source.sendSuccess(() -> Component.translatable("command.nerotech.gallery.tour.stopped"), false);
        return Command.SINGLE_SUCCESS;
    }

    /** The index of the bay whose tour stop is nearest a relative x/z. Pure. */
    public static int nearestStop(int relX, int relZ) {
        List<Bay> bays = GalleryLayout.bays();
        int best = 0;
        long bestDist = Long.MAX_VALUE;
        for (int i = 0; i < bays.size(); i++) {
            long dx = relX - bays.get(i).stopX();
            long dz = relZ - bays.get(i).stopZ();
            long d = dx * dx + dz * dz;
            if (d < bestDist) {
                bestDist = d;
                best = i;
            }
        }
        return best;
    }

    /** The stop after {@code index}, or -1 past the last. Pure. */
    public static int nextStop(int index) {
        return index + 1 < GalleryLayout.bays().size() ? index + 1 : -1;
    }

    private static int visit(CommandSourceStack source, ServerPlayer player, ServerLevel level,
            GalleryState.Site site, int index) {
        Bay bay = GalleryLayout.bays().get(index);
        teleport(player, level, new BlockPos(site.x() + bay.stopX(), site.y() + 1, site.z() + bay.stopZ()), 0.0F);
        int number = index + 1;
        int total = GalleryLayout.bays().size();
        String key = "gallery.nerotech.bay." + bay.id();
        source.sendSuccess(() -> Component.translatable("command.nerotech.gallery.tour.step", number, total,
                Component.translatable(key + ".title"), Component.translatable(key + ".flow")), false);
        return Command.SINGLE_SUCCESS;
    }

    private static GalleryState.Site site(ServerPlayer player, ServerLevel level) {
        return GalleryState.get(level.getServer()).find(PollutionManager.dimensionId(level), player.getBlockX(),
                player.getBlockY(), player.getBlockZ());
    }

    /** Teleport within the level, facing {@code yaw} (0 = south, toward the machine row). */
    private static void teleport(ServerPlayer player, ServerLevel level, BlockPos feet, float yaw) {
        //? if >=1.21.2 {
        player.teleportTo(level, feet.getX() + 0.5D, feet.getY(), feet.getZ() + 0.5D, java.util.Set.of(), yaw,
                0.0F, true);
        //?} else {
        /*player.teleportTo(level, feet.getX() + 0.5D, feet.getY(), feet.getZ() + 0.5D, java.util.Set.of(), yaw,
                0.0F);
        *///?}
    }
}
