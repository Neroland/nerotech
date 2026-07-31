package za.co.neroland.nerotech.link;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.link.LinkActionHandler;
import za.co.neroland.nerolandcore.link.LinkActionResult;
import za.co.neroland.nerolandcore.link.LinkAlert;
import za.co.neroland.nerolandcore.link.LinkAlerts;
import za.co.neroland.nerolandcore.link.LinkEvent;
import za.co.neroland.nerolandcore.link.LinkModuleInfo;
import za.co.neroland.nerolandcore.link.LinkSnapshotProvider;
import za.co.neroland.nerolandcore.link.NeroLinkRegistry;
import za.co.neroland.nerolandcore.progression.CoreGates;
import za.co.neroland.nerolandcore.progression.ProgressionGates;

import za.co.neroland.nerotech.config.NeroTechConfig;
import za.co.neroland.nerotech.guide.TechGuide;
import za.co.neroland.nerotech.guide.TechGuideSeenState;
import za.co.neroland.nerotech.machine.MachinePreset;
import za.co.neroland.nerotech.machine.NeroTechMachineBlock;
import za.co.neroland.nerotech.machine.NeroTechMachineBlockEntity;
import za.co.neroland.nerotech.pollution.PollutionAttributionPrefs;
import za.co.neroland.nerotech.pollution.PollutionState;
import za.co.neroland.nerotech.progression.NeroTechGates;

/**
 * NeroTech's NeroLink module — the single class that plugs NeroTech into Core's link SPI as both a
 * {@link LinkSnapshotProvider} (read side) and a {@link LinkActionHandler} (write side), so the
 * NeroLink companion bridge auto-serves NeroTech to app clients. Registered once from
 * {@code NeroTechCommon.init()} via {@link #register()}.
 *
 * <p><b>Sections</b> (all own-data-only except the public {@code wiki}):
 * <ul>
 *   <li>{@code pollution} — the player's OWN attributed pollution, only when server-wide attribution
 *       is on and the player has not opted out; otherwise an empty-with-note (attribution is opt-out
 *       and off by default — never other players' or regional aggregate data as "personal").</li>
 *   <li>{@code guide} — the player's Tech Guide "seen" progress (UUID-scoped).</li>
 *   <li>{@code gates} — this player's NeroTech-relevant gate states
 *       ({@code industrial_power}, {@code orbit_fabrication}, {@code fusion_online}).</li>
 *   <li>{@code wiki} — PUBLIC page index / page content (see {@link WikiLibrary}); {@code playerId}
 *       is ignored.</li>
 * </ul>
 *
 * <p><b>Actions</b>: {@code set_pollution_attribution} (a player's own privacy opt-out) and
 * {@code set_machine_preset} (remote overclock preset, owner-gated). The bridge validates token,
 * module presence, action-enabled config, rate limit and offline policy; this handler still
 * re-checks ownership, gates and rules — a compromised app can do no more than the player could.
 *
 * <p><b>Server handle.</b> The link SPI passes no server, and NeroTech keeps no other server holder,
 * so {@link #rememberServer(MinecraftServer)} captures it from the per-loader server tick (see
 * {@code PollutionManager.tick}). Every method here runs on the server thread when the bridge calls it.
 */
public final class NeroTechLinkModule implements LinkSnapshotProvider, LinkActionHandler {

    public static final String MODULE_ID = "nerotech";
    public static final int SCHEMA_VERSION = 1;

    /** Display version reported in discovery (matches NeroTech's mod version). */
    public static final String MOD_VERSION = "0.1.0-beta.1";

    private static final List<String> SECTIONS = List.of("pollution", "guide", "gates", "wiki");
    private static final List<String> ACTIONS = List.of("set_pollution_attribution", "set_machine_preset");

    /** NeroTech-relevant gates, in unlock order, for the {@code gates} section. */
    private static final List<Identifier> GATES = List.of(
            CoreGates.INDUSTRIAL_POWER,
            NeroTechGates.ORBIT_FABRICATION,
            NeroTechGates.FUSION_ONLINE);

    private static final NeroTechLinkModule INSTANCE = new NeroTechLinkModule();

    /** Captured running server (see {@link #rememberServer}); volatile — written from the tick thread. */
    @Nullable
    private static volatile MinecraftServer server;

    private NeroTechLinkModule() {
    }

    /** Register the module (snapshot + action) with Core's link registry. Idempotent per process. */
    public static void register() {
        LinkModuleInfo info = new LinkModuleInfo(MODULE_ID, MOD_VERSION, SCHEMA_VERSION, SECTIONS, ACTIONS);
        NeroLinkRegistry.registerSnapshotProvider(INSTANCE, info);
        NeroLinkRegistry.registerActionHandler(INSTANCE, info);
    }

    /** Capture the running server so provider/action calls can resolve players + SavedData. */
    public static void rememberServer(MinecraftServer runningServer) {
        server = runningServer;
    }

    // --- LinkSnapshotProvider --------------------------------------------------------

    @Override
    public String moduleId() {
        return MODULE_ID;
    }

    @Override
    public int schemaVersion() {
        return SCHEMA_VERSION;
    }

    @Override
    public List<String> sections() {
        return SECTIONS;
    }

    @Override
    public JsonObject snapshot(UUID playerId, String section, Map<String, String> params) {
        return switch (section) {
            case "pollution" -> pollution(playerId);
            case "guide" -> guide(playerId);
            case "gates" -> gates(playerId);
            case "wiki" -> wiki(params);
            default -> {
                JsonObject obj = new JsonObject();
                obj.addProperty("section", section);
                obj.addProperty("note", "unknown nerotech section");
                yield obj;
            }
        };
    }

    /** The player's OWN attributed pollution — only when attribution is on and they haven't opted out. */
    private JsonObject pollution(UUID playerId) {
        JsonObject out = new JsonObject();
        out.addProperty("asOf", System.currentTimeMillis());
        MinecraftServer srv = server;
        if (srv == null || !NeroTechConfig.pollutionPerPlayerAttribution()) {
            // No server yet, or attribution is opt-out and OFF by default: never surface regional/aggregate
            // pollution as this player's personal data. (POPIA/GDPR — see PollutionState / the config comment.)
            out.addProperty("attributionEnabled", false);
            out.addProperty("note", "Per-player pollution attribution is off (opt-out, off by default). "
                    + "No personal pollution data is stored or shown. A server admin enables it with the "
                    + "pollutionPerPlayerAttribution config; you can then opt out again with "
                    + "set_pollution_attribution.");
            return out;
        }
        boolean optedOut = PollutionAttributionPrefs.get(srv).isOptedOut(playerId);
        out.addProperty("attributionEnabled", true);
        out.addProperty("optedOut", optedOut);
        out.addProperty("attributed", PollutionState.get(srv).attributed(playerId));
        out.addProperty("retentionDays", NeroTechConfig.pollutionAttributionRetentionDays());
        if (optedOut) {
            out.addProperty("note", "You have opted out; your future pollution is not attributed. "
                    + "Any existing total is retention-pruned and erasable.");
        }
        return out;
    }

    /** The player's Tech Guide "seen" progress (UUID-scoped; no advancement/completion data leaked). */
    private JsonObject guide(UUID playerId) {
        JsonObject out = new JsonObject();
        out.addProperty("asOf", System.currentTimeMillis());
        MinecraftServer srv = server;
        int chapters = TechGuide.CHAPTER_COUNT;
        int totalSteps = TechGuide.totalSteps();
        int seenSteps = 0;
        int chaptersStarted = 0;
        if (srv != null) {
            TechGuideSeenState state = TechGuideSeenState.get(srv);
            for (int c = 0; c < chapters; c++) {
                int mask = state.mask(playerId, c);
                int bits = Integer.bitCount(mask);
                seenSteps += bits;
                if (bits > 0) {
                    chaptersStarted++;
                }
            }
        }
        out.addProperty("chapters", chapters);
        out.addProperty("chaptersStarted", chaptersStarted);
        out.addProperty("totalSteps", totalSteps);
        out.addProperty("seenSteps", seenSteps);
        return out;
    }

    /** This player's NeroTech gate states (scope-correct online; server-scope fallback offline). */
    private JsonObject gates(UUID playerId) {
        JsonObject out = new JsonObject();
        out.addProperty("asOf", System.currentTimeMillis());
        JsonArray arr = new JsonArray();
        MinecraftServer srv = server;
        ServerPlayer player = srv == null ? null : srv.getPlayerList().getPlayer(playerId);
        for (Identifier gate : GATES) {
            JsonObject g = new JsonObject();
            g.addProperty("id", gate.getPath());
            boolean unlocked;
            if (player != null) {
                unlocked = ProgressionGates.isOpen(player, gate);
            } else if (srv != null) {
                // Offline: player-scope gates need a ServerPlayer, so fall back to server-scope openness.
                unlocked = ProgressionGates.isServerOpen(srv, gate);
            } else {
                unlocked = false;
            }
            g.addProperty("unlocked", unlocked);
            arr.add(g);
        }
        out.add("gates", arr);
        return out;
    }

    /** PUBLIC wiki section (WIKI CONTRACT v1). {@code playerId} is ignored — same for every player. */
    private JsonObject wiki(Map<String, String> params) {
        long now = System.currentTimeMillis();
        String page = params == null ? null : params.get("page");
        if (page == null || page.isEmpty()) {
            JsonObject out = new JsonObject();
            out.addProperty("mod", MODULE_ID);
            out.addProperty("title", "NeroTech");
            JsonArray pages = new JsonArray();
            for (WikiLibrary.Page p : WikiLibrary.pages()) {
                JsonObject entry = new JsonObject();
                entry.addProperty("slug", p.slug());
                entry.addProperty("title", p.title());
                pages.add(entry);
            }
            out.add("pages", pages);
            out.addProperty("asOf", now);
            return out;
        }
        JsonObject out = new JsonObject();
        return WikiLibrary.content(page).map(md -> {
            out.addProperty("mod", MODULE_ID);
            out.addProperty("slug", page);
            out.addProperty("title", WikiLibrary.titleOf(page));
            out.addProperty("format", "markdown");
            out.addProperty("content", md);
            out.addProperty("asOf", now);
            return out;
        }).orElseGet(() -> {
            out.addProperty("error", "unknown page");
            out.addProperty("slug", page);
            return out;
        });
    }

    // --- LinkActionHandler -----------------------------------------------------------

    @Override
    public List<String> actionIds() {
        return ACTIONS;
    }

    @Override
    public boolean allowOffline(String actionId) {
        // Toggling your own attribution preference is a privacy control — fine while offline. Remotely
        // re-presetting a machine changes world state, so require the player to be online.
        return "set_pollution_attribution".equals(actionId);
    }

    @Override
    public LinkActionResult execute(UUID playerId, String actionId, JsonObject params) {
        return switch (actionId) {
            case "set_pollution_attribution" -> setPollutionAttribution(playerId, params);
            case "set_machine_preset" -> setMachinePreset(playerId, params);
            default -> LinkActionResult.error(LinkActionResult.Error.VALIDATION,
                    "unknown nerotech action: " + actionId);
        };
    }

    /**
     * Toggle the CALLING player's OWN attribution preference — a per-player opt-out layered on the
     * server-wide flag (see {@link PollutionAttributionPrefs} for why: the config exposes only a global
     * flag, so own-data control is stored as a UUID-keyed opt-out and consulted by
     * {@code PollutionManager.record}). Own data only; always compliant.
     */
    private LinkActionResult setPollutionAttribution(UUID playerId, JsonObject params) {
        if (params == null || !params.has("enabled") || !params.get("enabled").isJsonPrimitive()) {
            return LinkActionResult.error(LinkActionResult.Error.VALIDATION, "boolean 'enabled' is required");
        }
        MinecraftServer srv = server;
        if (srv == null) {
            return LinkActionResult.error(LinkActionResult.Error.INTERNAL, "server not available");
        }
        boolean enabled = params.get("enabled").getAsBoolean();
        boolean optedOut = PollutionAttributionPrefs.get(srv).setEnabled(playerId, enabled);
        JsonObject state = new JsonObject();
        state.addProperty("enabled", !optedOut);
        state.addProperty("optedOut", optedOut);
        state.addProperty("attributionGlobal", NeroTechConfig.pollutionPerPlayerAttribution());
        return LinkActionResult.ok(state);
    }

    /**
     * Remotely set a NeroTech machine's overclock preset. Server re-checks everything: the player is
     * ONLINE, the target block is a live NeroTech machine, the player OWNS it (established from the
     * machine's own owner field — only recorded when attribution was on at placement; a null/mismatched
     * owner is refused), and any gate the machine requires is open. Reuses the machine's own
     * server-side {@code setPreset} path — the same one the in-game preset payload drives.
     */
    private LinkActionResult setMachinePreset(UUID playerId, JsonObject params) {
        MinecraftServer srv = server;
        if (srv == null) {
            return LinkActionResult.error(LinkActionResult.Error.INTERNAL, "server not available");
        }
        ServerPlayer player = srv.getPlayerList().getPlayer(playerId);
        if (player == null) {
            return LinkActionResult.error(LinkActionResult.Error.PLAYER_OFFLINE_REQUIRED,
                    "you must be online to change a machine preset");
        }
        if (params == null || !params.has("dim") || !params.has("x") || !params.has("y")
                || !params.has("z") || !params.has("preset")) {
            return LinkActionResult.error(LinkActionResult.Error.VALIDATION,
                    "dim, x, y, z and preset are required");
        }
        MachinePreset preset;
        try {
            preset = MachinePreset.valueOf(params.get("preset").getAsString());
        } catch (IllegalArgumentException e) {
            return LinkActionResult.error(LinkActionResult.Error.VALIDATION,
                    "preset must be ECO, BALANCED or OVERDRIVE");
        }
        Identifier dimId;
        try {
            dimId = Identifier.parse(params.get("dim").getAsString());
        } catch (RuntimeException e) {
            return LinkActionResult.error(LinkActionResult.Error.VALIDATION, "invalid dimension id");
        }
        ServerLevel level = srv.getLevel(ResourceKey.create(Registries.DIMENSION, dimId));
        if (level == null) {
            return LinkActionResult.error(LinkActionResult.Error.VALIDATION, "unknown dimension");
        }
        BlockPos pos = new BlockPos(params.get("x").getAsInt(), params.get("y").getAsInt(),
                params.get("z").getAsInt());
        if (!level.hasChunkAt(pos)) {
            return LinkActionResult.error(LinkActionResult.Error.VALIDATION, "target chunk is not loaded");
        }
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof NeroTechMachineBlockEntity machine)) {
            return LinkActionResult.error(LinkActionResult.Error.VALIDATION, "no NeroTech machine there");
        }
        UUID owner = machine.ownerId();
        if (owner == null || !owner.equals(playerId)) {
            // Ownership cannot be established (attribution off at placement, or someone else's machine).
            return LinkActionResult.error(LinkActionResult.Error.NOT_OWNER, "you do not own this machine");
        }
        Block block = machine.getBlockState().getBlock();
        Identifier gate = block instanceof NeroTechMachineBlock ntb ? ntb.requiredGate() : null;
        if (gate != null && !ProgressionGates.isOpen(player, gate)) {
            return LinkActionResult.error(LinkActionResult.Error.GATE_LOCKED,
                    "a progression gate for this machine is locked");
        }
        machine.setPreset(preset);
        JsonObject state = new JsonObject();
        state.addProperty("dim", dimId.toString());
        state.addProperty("x", pos.getX());
        state.addProperty("y", pos.getY());
        state.addProperty("z", pos.getZ());
        state.addProperty("preset", preset.name());
        return LinkActionResult.ok(state);
    }

    // --- live events (published from the gameplay code paths) -------------------------

    /**
     * Personal pollution threshold crossing (attribution ON only): when a player's OWN attributed
     * total first crosses {@code pollutionEventThreshold}, push a player-scoped {@code pollution} event
     * and raise a WARN alert (module {@code nerotech}). Only the owner's own UUID is touched.
     */
    public static void onAttributedPollution(MinecraftServer srv, UUID owner, int before, int after) {
        int threshold = NeroTechConfig.pollutionEventThreshold();
        if (srv == null || owner == null || threshold <= 0 || before >= threshold || after < threshold) {
            return;
        }
        JsonObject payload = new JsonObject();
        payload.addProperty("attributed", after);
        payload.addProperty("threshold", threshold);
        NeroLinkRegistry.eventBus().publish(LinkEvent.forPlayer(MODULE_ID, "pollution", owner, payload));
        LinkAlerts.get(srv).raise(srv, owner, LinkAlert.raise("pollution_threshold", MODULE_ID,
                LinkAlert.Severity.WARN, "Your attributed pollution has crossed the alert threshold."));
    }

    /**
     * Open {@code fusion_online} for a reactor's owner when it first ignites, and publish a gate event.
     * Player-scoped gate: needs an online owner (skipped when the owner is unknown/offline).
     */
    public static void openFusionOnline(MinecraftServer srv, @Nullable UUID owner) {
        if (srv == null || owner == null) {
            return;
        }
        ServerPlayer player = srv.getPlayerList().getPlayer(owner);
        if (player != null && ProgressionGates.tryOpen(player, NeroTechGates.FUSION_ONLINE)) {
            onGateOpened(player, NeroTechGates.FUSION_ONLINE);
        }
    }

    /** Publish a player-scoped {@code gate} event when a NeroTech gate opens for a player. */
    public static void onGateOpened(ServerPlayer player, Identifier gate) {
        JsonObject payload = new JsonObject();
        payload.addProperty("gate", gate.getPath());
        payload.addProperty("unlocked", true);
        NeroLinkRegistry.eventBus().publish(
                LinkEvent.forPlayer(MODULE_ID, "gate", player.getUUID(), payload));
    }

    /**
     * A reactor meltdown or containment breach: broadcast a {@code meltdown} world event (no personal
     * data — world coordinates only) and raise a CRITICAL alert to the owner if one is known.
     */
    public static void onReactorCritical(MinecraftServer srv, @Nullable UUID owner, BlockPos pos,
            String kind, int shellSize) {
        if (srv == null) {
            return;
        }
        JsonObject payload = new JsonObject();
        payload.addProperty("kind", kind);
        payload.addProperty("x", pos.getX());
        payload.addProperty("y", pos.getY());
        payload.addProperty("z", pos.getZ());
        payload.addProperty("shell", shellSize);
        NeroLinkRegistry.eventBus().publish(LinkEvent.broadcast(MODULE_ID, "meltdown", payload));
        if (owner != null) {
            String text = "meltdown".equals(kind)
                    ? "Your Fusion Reactor has melted down."
                    : "Your Fusion Reactor suffered a containment breach.";
            LinkAlerts.get(srv).raise(srv, owner,
                    LinkAlert.raise("fusion_" + kind, MODULE_ID, LinkAlert.Severity.CRITICAL, text));
        }
    }
}
