package za.co.neroland.nerotech.progression;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.event.CoreEvents;
import za.co.neroland.nerolandcore.progression.GateScope;
import za.co.neroland.nerolandcore.progression.ProgressionGates;

import za.co.neroland.nerotech.NeroTechCommon;
import za.co.neroland.nerotech.guide.TechGuide;
import za.co.neroland.nerotech.guide.TechGuideProgress;

/**
 * Listener for the <b>hook gates</b> a quest mod opens when a player completes one of NeroTech's own
 * quests ({@code data/nerotech/neroland_gates/hook/*.json}, named by the pack's {@code mod_hook}
 * rewards). NeroTech never imports the quest mod: it only hears Core's progression events.
 *
 * <p>{@code nerotech:hook/tech_guide_unlock} — unlocks the player's <b>next Tech Guide entry</b>:
 * the first step of the guide the player has not completed yet is marked complete (its advancement
 * is awarded, so it lights up in the guide and pulses until clicked) and a chat line names it.
 * Information only — NeroTech advancements carry no recipe or item rewards, and a Tech Guide step
 * gates nothing. Once every entry is complete the hook only sends the chat line.
 *
 * <p>Delivery is bounded and idempotent: one entry per hook opening, after which the gate is closed
 * again to re-arm it. A hook that opened while the player was offline is delivered on their next
 * login ({@link #onJoin}); logins are detected loader-free from the server tick ({@link #tick}).
 *
 * <p><b>Privacy (POPIA/GDPR):</b> the gate change's target is the player's UUID string; it is used
 * only to find the online player and to close the gate, never logged or broadcast. Hook gates are
 * records that gate nothing and are never checked for access. The online-player set kept for join
 * detection is process-local, holds UUIDs only and is dropped when the server changes.
 */
public final class QuestHooks {

    /** The hook gate the "Industrial Record" pack rewards with a Tech Guide entry. */
    public static final Identifier TECH_GUIDE_UNLOCK =
            Identifier.fromNamespaceAndPath(NeroTechCommon.MOD_ID, "hook/tech_guide_unlock");

    /** Login sweep cadence (ticks). */
    private static final int JOIN_SWEEP_INTERVAL = 20;

    /** The running server, captured from the server tick so the listener can resolve online players. */
    private static volatile @Nullable MinecraftServer server;
    /** Players seen online at the last sweep — a join is a UUID that was not in it. Process-local. */
    private static final Set<UUID> ONLINE = new HashSet<>();
    private static @Nullable MinecraftServer sweptServer;

    private QuestHooks() {
    }

    /** Register the progression listener; call once from common init. */
    public static void init() {
        CoreEvents.onProgression(change -> {
            if (!change.open() || change.scope() != GateScope.PLAYER || !TECH_GUIDE_UNLOCK.equals(change.gate())) {
                return;
            }
            MinecraftServer running = server;
            if (running == null) {
                return; // no server yet: the gate stays open and onJoin delivers it
            }
            UUID id;
            try {
                id = UUID.fromString(change.target());
            } catch (IllegalArgumentException e) {
                return;
            }
            ServerPlayer player = running.getPlayerList().getPlayer(id);
            if (player == null) {
                return; // offline: the gate stays open, see onJoin
            }
            unlockNextGuideEntry(player);
            ProgressionGates.close(running, id, TECH_GUIDE_UNLOCK); // re-arm; the open=false change is ignored above
        });
    }

    /**
     * Server tick: remember the server and, once a second, deliver pending hooks to players who
     * logged in since the last sweep. Cheap — a set diff over the online player list.
     */
    public static void tick(MinecraftServer running) {
        server = running;
        if (running.getTickCount() % JOIN_SWEEP_INTERVAL != 0) {
            return;
        }
        if (sweptServer != running) {
            ONLINE.clear(); // a new server in this process (singleplayer world switch) starts fresh
            sweptServer = running;
        }
        Set<UUID> now = new HashSet<>();
        for (ServerPlayer player : running.getPlayerList().getPlayers()) {
            now.add(player.getUUID());
            if (ONLINE.add(player.getUUID())) {
                onJoin(player);
            }
        }
        ONLINE.retainAll(now);
    }

    /** On login: deliver a hook that opened while the player was offline. */
    public static void onJoin(ServerPlayer player) {
        if (ProgressionGates.isOpen(player, TECH_GUIDE_UNLOCK)) {
            unlockNextGuideEntry(player);
            ProgressionGates.close(player, TECH_GUIDE_UNLOCK);
        }
    }

    /**
     * Mark the player's first incomplete Tech Guide step complete and tell them which one. With the
     * guide already complete, only the chat line is sent.
     */
    static void unlockNextGuideEntry(ServerPlayer player) {
        for (TechGuide.Chapter chapter : TechGuide.CHAPTERS) {
            for (TechGuide.Step step : chapter.steps()) {
                if (TechGuideProgress.isComplete(player, step)) {
                    continue;
                }
                if (award(player, step)) {
                    player.sendSystemMessage(Component.translatable("message.nerotech.tech_guide_unlock",
                            Component.translatable(step.titleKey())));
                    return;
                }
                // The step's advancement is missing from the datapack: skip it like the guide does.
            }
        }
        player.sendSystemMessage(Component.translatable("message.nerotech.tech_guide_unlock.complete"));
    }

    /** Award every remaining criterion of the step's advancement; false when it cannot be resolved. */
    private static boolean award(ServerPlayer player, TechGuide.Step step) {
        AdvancementHolder holder = player.level().getServer().getAdvancements().get(step.advancement());
        if (holder == null) {
            return false;
        }
        AdvancementProgress progress = player.getAdvancements().getOrStartProgress(holder);
        for (String criterion : progress.getRemainingCriteria()) {
            player.getAdvancements().award(holder, criterion);
        }
        return true;
    }
}
