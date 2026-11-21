package za.co.neroland.nerotech.link;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.link.LinkActionHandler;
import za.co.neroland.nerolandcore.link.LinkActionResult;
import za.co.neroland.nerolandcore.link.LinkAlert;
import za.co.neroland.nerolandcore.link.LinkAlerts;
import za.co.neroland.nerolandcore.link.LinkEvent;
import za.co.neroland.nerolandcore.link.LinkModuleInfo;
import za.co.neroland.nerolandcore.link.LinkSnapshotProvider;
import za.co.neroland.nerolandcore.link.NeroLinkRegistry;

import za.co.neroland.nerotech.config.NeroTechConfig;
import za.co.neroland.nerotech.guide.TechGuide;
import za.co.neroland.nerotech.guide.TechGuideSeenState;
import za.co.neroland.nerotech.machine.MachinePreset;
import za.co.neroland.nerotech.machine.NeroTechMachineBlockEntity;
import za.co.neroland.nerotech.machine.utility.AnnunciatorPanelBlockEntity;
import za.co.neroland.nerotech.pollution.PollutionAttributionPrefs;
import za.co.neroland.nerotech.pollution.PollutionState;

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
 *   <li>{@code wiki} — PUBLIC page index / page content (see {@link WikiLibrary}); {@code playerId}
 *       is ignored.</li>
 *   <li>{@code nearby} (schema 2) — PROXIMITY-scoped: NeroTech machines in loaded chunks around the
 *       ONLINE requester, same dimension, within 128 blocks; type, state, alarms and three percentages
 *       plus an opaque 30 s handle. No coordinates, no owner, no direction (see {@link NearbyFormat}).</li>
 *   <li>{@code catalogue} (schema 2) — PUBLIC machines, materials and the six chains, identical for
 *       every requester (see {@link CatalogueSection}).</li>
 * </ul>
 *
 * <p><b>Actions</b>: {@code set_pollution_attribution} (a player's own privacy opt-out) and
 * {@code set_machine_preset} (remote overclock preset: owner-gated by position, or by a {@code nearby}
 * handle within in-person reach — see {@link PresetAccess}). Ten actions per player per minute, in
 * memory; opt-outs have their own budget of thirty. Every refusal after validation is the one answer
 * {@code unavailable}. The bridge validates token,
 * module presence, action-enabled config, rate limit and offline policy; this handler still
 * re-checks ownership and rules — a compromised app can do no more than the player could.
 *
 * <p><b>Server handle.</b> The link SPI passes no server, and NeroTech keeps no other server holder,
 * so {@link #rememberServer(MinecraftServer)} captures it from the per-loader server tick (see
 * {@code PollutionManager.tick}). Every method here runs on the server thread when the bridge calls it.
 */
public final class NeroTechLinkModule implements LinkSnapshotProvider, LinkActionHandler {

    public static final String MODULE_ID = "nerotech";
    public static final int SCHEMA_VERSION = 2;

    /** Display version reported in discovery (matches NeroTech's mod version). */
    // TODO: hand-maintained duplicate — bump this in step with `mod_version` in gradle.properties,
    // or the companion app reports a stale NeroTech version.
    public static final String MOD_VERSION = "0.8.0-beta.1";

    private static final List<String> SECTIONS = List.of("pollution", "guide", "wiki", "nearby", "catalogue");
    private static final List<String> ACTIONS = List.of("set_pollution_attribution", "set_machine_preset");

    private static final NeroTechLinkModule INSTANCE = new NeroTechLinkModule();

    /** Captured running server (see {@link #rememberServer}); volatile — written from the tick thread. */
    @Nullable
    private static volatile MinecraftServer server;

    /** Where a {@code nearby} handle points. Server-side only; never serialised to a client. */
    record Target(ResourceKey<Level> dimension, BlockPos pos) {
    }

    /** Handles minted by {@code nearby}, per requester, in memory (decision 007). */
    static final NearbyHandles<Target> HANDLES = new NearbyHandles<>(System::currentTimeMillis);

    /** Ten actions per player per minute, in memory (decision 007). */
    static final LinkRateLimiter LIMITER = new LinkRateLimiter(System::currentTimeMillis);

    /** Privacy opt-outs: their own generous budget, so they are never refused in normal use (review L1). */
    static final LinkRateLimiter OPT_OUTS = new LinkRateLimiter(System::currentTimeMillis,
            LinkRateLimiter.MAX_OPT_OUTS);

    /** Shuffles each {@code nearby} answer so order and truncation say nothing about direction (review M1). */
    private static final SecureRandom SHUFFLE = new SecureRandom();

    /** Ticks between sweeps that drop in-memory link state of players who have logged out. */
    private static final int PRUNE_INTERVAL_TICKS = 100;

    private static int pruneCountdown = PRUNE_INTERVAL_TICKS;

    private static boolean erasureRegistered;

    private NeroTechLinkModule() {
    }

    /**
     * POPIA/GDPR: register {@link #forgetPlayer(UUID)} with Core's shared data-erasure hook, so one erase
     * request also drops the player's in-memory link state at once instead of at the next logout sweep.
     * Idempotent per process; called from {@link #register()}.
     */
    public static synchronized void registerErasure() {
        if (erasureRegistered) {
            return;
        }
        erasureRegistered = true;
        za.co.neroland.nerolandcore.data.PlayerDataErasure.register((srv, uuid) -> forgetPlayer(uuid));
    }

    /** Drop every in-memory entry for {@code player}: nearby handles, action rate window, alarm throttle. */
    public static void forgetPlayer(UUID player) {
        HANDLES.forget(player);
        LIMITER.forget(player);
        OPT_OUTS.forget(player);
        MachineAlarmLink.forget(player);
    }

    /** Whether any in-memory link state still refers to {@code player} (erasure probe; never persisted). */
    public static boolean holdsPlayer(UUID player) {
        return HANDLES.holds(player) || LIMITER.holds(player) || OPT_OUTS.holds(player)
                || MachineAlarmLink.holds(player);
    }

    /** Register the module (snapshot + action) with Core's link registry. Idempotent per process. */
    public static void register() {
        LinkModuleInfo info = new LinkModuleInfo(MODULE_ID, MOD_VERSION, SCHEMA_VERSION, SECTIONS, ACTIONS);
        NeroLinkRegistry.registerSnapshotProvider(INSTANCE, info);
        NeroLinkRegistry.registerActionHandler(INSTANCE, info);
        registerErasure();
    }

    /** Capture the running server so provider/action calls can resolve players + SavedData. */
    public static void rememberServer(MinecraftServer runningServer) {
        server = runningServer;
        // Same per-tick hook: every few seconds, drop handles and alarm throttles of players who are no
        // longer online ("cleared on logout"), and rate-limit windows once they have expired (not on
        // logout, so a relog does not refill the budget). Nothing of it is ever persisted.
        if (--pruneCountdown <= 0) {
            pruneCountdown = PRUNE_INTERVAL_TICKS;
            Set<UUID> online = new HashSet<>();
            for (ServerPlayer player : runningServer.getPlayerList().getPlayers()) {
                online.add(player.getUUID());
            }
            HANDLES.retainOnly(online);
            LIMITER.pruneExpired();
            OPT_OUTS.pruneExpired();
            MachineAlarmLink.retainOnly(online);
        }
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
            case "wiki" -> wiki(params);
            case "nearby" -> nearby(playerId);
            case "catalogue" -> CatalogueSection.snapshot(server);
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
        if (srv != null) {
            // The region the requester stands in, read in the requester's OWN dimension (0.8.0: readings
            // are per dimension). A coarse band only, no coordinates; absent while they are offline.
            ServerPlayer self = srv.getPlayerList().getPlayer(playerId);
            if (self != null && self.level() instanceof ServerLevel here) {
                JsonObject local = new JsonObject();
                local.addProperty("dimension", za.co.neroland.nerotech.pollution.PollutionManager.dimensionId(here));
                local.addProperty("band", za.co.neroland.nerotech.pollution.PollutionManager.band(
                        za.co.neroland.nerotech.pollution.PollutionManager.regionPollution(here, self.blockPosition())));
                out.add("localRegion", local);
            }
        }
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
        ownPollution(out, optedOut, optedOut ? 0 : PollutionState.get(srv).attributed(playerId),
                NeroTechConfig.pollutionAttributionRetentionDays());
        return out;
    }

    /**
     * The attribution-on fields of {@code pollution}. An opted-out requester gets no {@code attributed}
     * value at all (not zero, not null): what they asked not to be counted is not sent.
     */
    static void ownPollution(JsonObject out, boolean optedOut, long attributed, int retentionDays) {
        out.addProperty("attributionEnabled", true);
        out.addProperty("optedOut", optedOut);
        if (!optedOut) {
            out.addProperty("attributed", attributed);
        }
        out.addProperty("retentionDays", retentionDays);
        if (optedOut) {
            out.addProperty("note", "You have opted out; your future pollution is not attributed. "
                    + "Any existing total is retention-pruned and erasable.");
        }
    }

    /**
     * PROXIMITY-scoped machines around the ONLINE requester (decision 007): their current dimension,
     * the loaded chunks of a 9 x 9 square around them (nothing is loaded to answer), a 128-block box,
     * at most 128 entries. Walls and claims do not hide a machine: this is what stands within reach.
     * The whole in-reach set is shuffled before it is cut to 128 and before handles are minted, so the
     * order, the handle indices and which entries a truncation keeps say nothing about direction.
     * Each entry carries a handle valid for 30 s for {@code set_machine_preset}; the handle table
     * replaces the requester's previous one. Offline (or before the server is up) the list is empty,
     * indistinguishable from "nothing in range".
     */
    private JsonObject nearby(UUID playerId) {
        JsonObject out = new JsonObject();
        out.addProperty("asOf", System.currentTimeMillis());
        out.addProperty("ttlSeconds", NearbyHandles.TTL_MS / 1000L);
        JsonArray machines = new JsonArray();
        boolean truncated = false;
        MinecraftServer srv = server;
        ServerPlayer player = srv == null ? null : srv.getPlayerList().getPlayer(playerId);
        if (player != null && player.level() instanceof ServerLevel level) {
            BlockPos origin = player.blockPosition();
            List<NeroTechMachineBlockEntity> found = new ArrayList<>();
            int ocx = origin.getX() >> 4;
            int ocz = origin.getZ() >> 4;
            for (int dx = -NearbyFormat.CHUNK_RADIUS; dx <= NearbyFormat.CHUNK_RADIUS; dx++) {
                for (int dz = -NearbyFormat.CHUNK_RADIUS; dz <= NearbyFormat.CHUNK_RADIUS; dz++) {
                    LevelChunk chunk = level.getChunkSource().getChunkNow(ocx + dx, ocz + dz);
                    if (chunk == null) {
                        continue;
                    }
                    for (BlockEntity be : chunk.getBlockEntities().values()) {
                        if (!(be instanceof NeroTechMachineBlockEntity machine) || be.isRemoved()) {
                            continue;
                        }
                        BlockPos at = be.getBlockPos();
                        if (!NearbyFormat.visible(origin.getX(), origin.getY(), origin.getZ(),
                                at.getX(), at.getY(), at.getZ())) {
                            continue;
                        }
                        found.add(machine);
                    }
                }
            }
            truncated = NearbyFormat.shuffleAndCap(found, SHUFFLE, NearbyFormat.MAX_ENTRIES);
            List<Target> targets = new ArrayList<>(found.size());
            for (NeroTechMachineBlockEntity machine : found) {
                targets.add(new Target(level.dimension(), machine.getBlockPos().immutable()));
            }
            List<String> handles = HANDLES.issue(playerId, targets);
            for (int i = 0; i < found.size(); i++) {
                NeroTechMachineBlockEntity machine = found.get(i);
                String type = BuiltInRegistries.BLOCK.getKey(machine.getBlockState().getBlock()).toString();
                machines.add(NearbyFormat.entry(type, machine.readout(), machine.alarmMask(),
                        machine.energyPermille(), machine.heatPermille(), tankPermille(machine), handles.get(i),
                        !(machine instanceof AnnunciatorPanelBlockEntity)));
            }
        } else {
            HANDLES.forget(playerId);
        }
        out.add("machines", machines);
        out.addProperty("truncated", truncated);
        return out;
    }

    /** The machine's fluid tank fill, else its gas tank fill, as permille; null when it has neither. */
    @Nullable
    private static Integer tankPermille(NeroTechMachineBlockEntity machine) {
        try {
            za.co.neroland.nerolandcore.fluid.NeroFluidStorage fluid = machine.fluidStorage(null);
            if (fluid != null && fluid.getCapacity() > 0L) {
                return NearbyFormat.permille(fluid.getAmount(), fluid.getCapacity());
            }
            za.co.neroland.nerolandcore.gas.NeroGasStorage gas = machine.gasStorage(null);
            if (gas != null && gas.getCapacity() > 0L) {
                return NearbyFormat.permille(gas.getAmount(), gas.getCapacity());
            }
        } catch (RuntimeException e) {
            // A machine whose tank view needs a side: report no tank rather than fail the section.
        }
        return null;
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
        if (ACTIONS.contains(actionId)) {
            LinkRateLimiter budget = isOptOut(actionId, params) ? OPT_OUTS : LIMITER;
            if (!budget.tryAcquire(playerId)) {
                return LinkActionResult.error(LinkActionResult.Error.VALIDATION,
                        "rate limited: at most " + budget.max() + " nerotech actions of this kind a minute");
            }
        }
        return switch (actionId) {
            case "set_pollution_attribution" -> setPollutionAttribution(playerId, params);
            case "set_machine_preset" -> setMachinePreset(playerId, params);
            default -> LinkActionResult.error(LinkActionResult.Error.VALIDATION,
                    "unknown nerotech action: " + actionId);
        };
    }

    /** Turning attribution OFF is a privacy control: it spends the separate, generous opt-out budget. */
    private static boolean isOptOut(String actionId, @Nullable JsonObject params) {
        return "set_pollution_attribution".equals(actionId)
                && PresetAccess.bool(params, "enabled").map(enabled -> !enabled).orElse(false);
    }

    /**
     * Toggle the CALLING player's OWN attribution preference — a per-player opt-out layered on the
     * server-wide flag (see {@link PollutionAttributionPrefs} for why: the config exposes only a global
     * flag, so own-data control is stored as a UUID-keyed opt-out and consulted by
     * {@code PollutionManager.record}). Own data only; always compliant.
     */
    private LinkActionResult setPollutionAttribution(UUID playerId, JsonObject params) {
        Optional<Boolean> flag = PresetAccess.bool(params, "enabled");
        if (flag.isEmpty()) {
            return LinkActionResult.error(LinkActionResult.Error.VALIDATION, "boolean 'enabled' is required");
        }
        MinecraftServer srv = server;
        if (srv == null) {
            return LinkActionResult.error(LinkActionResult.Error.INTERNAL, "server not available");
        }
        boolean enabled = flag.get();
        boolean optedOut = PollutionAttributionPrefs.get(srv).setEnabled(playerId, enabled);
        JsonObject state = new JsonObject();
        state.addProperty("enabled", !optedOut);
        state.addProperty("optedOut", optedOut);
        state.addProperty("attributionGlobal", NeroTechConfig.pollutionPerPlayerAttribution());
        return LinkActionResult.ok(state);
    }

    /**
     * Remotely set a NeroTech machine's overclock preset. Server re-checks everything: the player is
     * ONLINE, the target block is a live NeroTech machine, and the player OWNS it (established from the
     * machine's own owner field — only recorded when attribution was on at placement; a null/mismatched
     * owner is refused). Reuses the machine's own server-side {@code setPreset} path — the same one the
     * in-game preset payload drives.
     *
     * <p>Once the body is well formed, every refusal is the one {@link PresetAccess#UNAVAILABLE} answer:
     * an unknown dimension, an unloaded chunk, no machine and someone else's machine look the same, so
     * the action is no oracle for loaded chunks or machine positions (review H2).
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
        if (params != null && params.has("h")) {
            return setMachinePresetByHandle(player, params);
        }
        if (params == null) {
            return LinkActionResult.error(LinkActionResult.Error.VALIDATION,
                    "dim, x, y, z and preset are required");
        }
        Optional<String> dimName = PresetAccess.string(params.get("dim"));
        OptionalInt x = PresetAccess.integer(params.get("x"));
        OptionalInt y = PresetAccess.integer(params.get("y"));
        OptionalInt z = PresetAccess.integer(params.get("z"));
        if (dimName.isEmpty() || x.isEmpty() || y.isEmpty() || z.isEmpty() || !params.has("preset")) {
            return LinkActionResult.error(LinkActionResult.Error.VALIDATION,
                    "dim (string), x, y, z (integers) and preset are required");
        }
        Optional<MachinePreset> preset = PresetAccess.preset(params.get("preset"));
        if (preset.isEmpty()) {
            return LinkActionResult.error(LinkActionResult.Error.VALIDATION,
                    "preset must be ECO, BALANCED or OVERDRIVE");
        }
        Identifier dimId = Identifier.tryParse(dimName.get());
        if (dimId == null) {
            return LinkActionResult.error(LinkActionResult.Error.VALIDATION, "invalid dimension id");
        }
        ServerLevel level = srv.getLevel(ResourceKey.create(Registries.DIMENSION, dimId));
        if (level == null) {
            return PresetAccess.refuse(PresetAccess.Refusal.UNKNOWN_DIMENSION);
        }
        BlockPos pos = new BlockPos(x.getAsInt(), y.getAsInt(), z.getAsInt());
        if (!level.hasChunkAt(pos)) {
            return PresetAccess.refuse(PresetAccess.Refusal.NOT_LOADED);
        }
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof NeroTechMachineBlockEntity machine) || be.isRemoved()) {
            return PresetAccess.refuse(PresetAccess.Refusal.NO_MACHINE);
        }
        UUID owner = machine.ownerId();
        if (owner == null || !owner.equals(playerId)) {
            // Ownership cannot be established (attribution off at placement, or someone else's machine).
            return PresetAccess.refuse(PresetAccess.Refusal.NOT_OWNER);
        }
        machine.setPreset(preset.get());
        JsonObject state = new JsonObject();
        state.addProperty("dim", dimId.toString());
        state.addProperty("x", pos.getX());
        state.addProperty("y", pos.getY());
        state.addProperty("z", pos.getZ());
        state.addProperty("preset", preset.get().name());
        return LinkActionResult.ok(state);
    }

    /**
     * Schema-2 body {@code {h, preset}} (decision 007, amendment 2026-11-21): the handle must come from
     * the requester's OWN latest {@code nearby} response and be under 30 s old, and the requester must be
     * able to make the change in person, now: same dimension, within {@link PresetAccess#ACTION_REACH}
     * blocks (not the 128 of {@code nearby}), the chunk already loaded, the interaction checks a
     * right-click passes at that block (spawn protection, world border, adventure rules, build
     * restrictions), and no recorded owner or the requester as owner. Every refusal is the same
     * {@code unavailable} answer.
     */
    private LinkActionResult setMachinePresetByHandle(ServerPlayer player, JsonObject params) {
        Optional<String> handle = PresetAccess.string(params.get("h"));
        if (handle.isEmpty() || !params.has("preset")) {
            return LinkActionResult.error(LinkActionResult.Error.VALIDATION, "h and preset are required");
        }
        Optional<MachinePreset> preset = PresetAccess.preset(params.get("preset"));
        if (preset.isEmpty()) {
            return LinkActionResult.error(LinkActionResult.Error.VALIDATION,
                    "preset must be ECO, BALANCED or OVERDRIVE");
        }
        Optional<Target> target = HANDLES.resolve(player.getUUID(), handle.get());
        if (target.isEmpty()) {
            return PresetAccess.refuse(PresetAccess.Refusal.UNKNOWN_HANDLE);
        }
        Target t = target.get();
        if (!(player.level() instanceof ServerLevel level) || !level.dimension().equals(t.dimension())) {
            return PresetAccess.refuse(PresetAccess.Refusal.WRONG_DIMENSION);
        }
        BlockPos pos = t.pos();
        if (!PresetAccess.inActionReach(player.getX(), player.getY(), player.getZ(),
                pos.getX(), pos.getY(), pos.getZ())) {
            return PresetAccess.refuse(PresetAccess.Refusal.OUT_OF_REACH);
        }
        if (!level.hasChunkAt(pos)
                || !(level.getBlockEntity(pos) instanceof NeroTechMachineBlockEntity machine)
                || machine.isRemoved()) {
            return PresetAccess.refuse(PresetAccess.Refusal.NO_MACHINE);
        }
        if (!mayInteractInPerson(player, level, pos)) {
            return PresetAccess.refuse(PresetAccess.Refusal.NOT_PERMITTED);
        }
        if (!PresetAccess.ownerAllows(machine.ownerId(), player.getUUID())) {
            return PresetAccess.refuse(PresetAccess.Refusal.NOT_OWNER);
        }
        machine.setPreset(preset.get());
        JsonObject state = new JsonObject();
        state.addProperty("h", handle.get());
        state.addProperty("preset", preset.get().name());
        return LinkActionResult.ok(state);
    }

    /**
     * The interaction checks a right-click on that block passes, as the Arc Wrench applies them:
     * {@code mayInteract} (spawn protection, world border), {@code mayUseItemAt} and the adventure /
     * spectator build restriction.
     */
    private static boolean mayInteractInPerson(ServerPlayer player, ServerLevel level, BlockPos pos) {
        return level.mayInteract(player, pos) && player.mayUseItemAt(pos, Direction.UP, ItemStack.EMPTY)
                && !player.blockActionRestricted(level, pos, player.gameMode.getGameModeForPlayer());
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
     * A reactor meltdown or containment breach (schema 2): a {@code meltdown} event with the kind and
     * shell size only, delivered {@code forPlayer} to each online player in the reactor's dimension
     * within reach ({@link NearbyFormat#visible}) and to the recorded owner, if any. Only the owner's copy
     * is on the {@code meltdown} topic and push-eligible; bystanders get a never-pushed notice. Never
     * broadcast and never with coordinates (schema 1 broadcast x/y/z to every client; changed as a
     * privacy fix). The owner also gets the CRITICAL alert, as before. (The server-only overload that
     * guessed the reactor's dimension was removed: every caller passes the level.)
     */
    public static void onReactorCritical(ServerLevel level, @Nullable UUID owner, BlockPos pos, String kind,
            int shellSize) {
        if (level == null) {
            return;
        }
        MinecraftServer srv = level.getServer();
        for (UUID recipient : MachineAlarmLink.recipients(level, pos, owner)) {
            NeroLinkRegistry.eventBus().publish(MachineAlarmLink.meltdownEvent(recipient, owner, kind, shellSize));
        }
        if (owner != null) {
            String text = "meltdown".equals(kind)
                    ? "Your Fusion Reactor has melted down."
                    : "Your Fusion Reactor suffered a containment breach.";
            LinkAlerts.get(srv).raise(srv, owner,
                    LinkAlert.raise("fusion_" + kind, MODULE_ID, LinkAlert.Severity.CRITICAL, text));
        }
    }
}
