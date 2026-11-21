package za.co.neroland.nerotech.deposit;

import java.io.Reader;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerotech.NeroTechCommon;

/**
 * The data-driven deposit table: {@code data/<namespace>/nerotech_deposits/<any path>.json}, each file
 * {@code { "dimension": "minecraft:overworld", "replace": false, "entries": [ { "ore": "#c:ores/iron",
 * "output": "minecraft:raw_iron" (optional), "weight": 10, "capacity": [2000, 6000],
 * "richness": [600, 1200] } ] }}. Files for the same dimension merge in resource-id order; {@code replace}
 * drops what earlier files added.
 *
 * <p>Loaded lazily per server and per resource-manager identity ({@link #forServer}), so a {@code /reload}
 * picks up changed files on the next lookup and nothing is held across worlds. An entry whose ore tag is
 * empty on this server (and has no explicit output) is skipped, so a table never yields an ore the pack
 * does not have. The tables ship only ores whose worldgen exists in that dimension.
 */
public final class DepositTable {

    /** Resource folder under {@code data/<namespace>/}. */
    public static final String FOLDER = "nerotech_deposits";

    /** One weighted table entry. {@code ore} is a tag id without the leading {@code #}. */
    public record Entry(String ore, @Nullable String output, int weight, int capacityMin, int capacityMax,
            int richnessMin, int richnessMax) {
    }

    private static volatile @Nullable Cached cache;

    private record Cached(WeakReference<MinecraftServer> server, WeakReference<ResourceManager> resources,
            DepositTable table) {
    }

    private final Map<String, List<Entry>> byDimension;
    private final Map<String, List<Entry>> resolved = new ConcurrentHashMap<>();
    private final Map<Entry, Item> outputs = new ConcurrentHashMap<>();

    public DepositTable(Map<String, List<Entry>> byDimension) {
        Map<String, List<Entry>> copy = new LinkedHashMap<>();
        byDimension.forEach((k, v) -> copy.put(k, List.copyOf(v)));
        this.byDimension = Map.copyOf(copy);
    }

    /** Raw entries for a dimension in file order (no tag resolution); empty when none. */
    public List<Entry> rawEntries(String dimensionId) {
        return this.byDimension.getOrDefault(dimensionId, List.of());
    }

    /** Dimensions with any table. */
    public java.util.Set<String> dimensions() {
        return this.byDimension.keySet();
    }

    /** Entries whose output resolves on this server (computed once per table). Needs a live registry. */
    public List<Entry> entries(String dimensionId) {
        return this.resolved.computeIfAbsent(dimensionId, dim -> {
            List<Entry> out = new ArrayList<>();
            for (Entry e : rawEntries(dim)) {
                if (output(e) != null) {
                    out.add(e);
                }
            }
            return List.copyOf(out);
        });
    }

    /** The item an entry yields: its explicit {@code output}, else the first item in the ore tag. */
    @Nullable
    public Item output(Entry entry) {
        Item cached = this.outputs.get(entry);
        if (cached != null) {
            return cached;
        }
        Item found = resolveOutput(entry);
        if (found != null) {
            this.outputs.put(entry, found);
        }
        return found;
    }

    @Nullable
    private static Item resolveOutput(Entry entry) {
        String explicit = entry.output();
        if (explicit != null) {
            Identifier id = Identifier.tryParse(explicit);
            if (id != null && BuiltInRegistries.ITEM.containsKey(id)) {
                Item item = BuiltInRegistries.ITEM.getValue(id);
                if (item != Items.AIR) {
                    return item;
                }
            }
        }
        Identifier tagId = Identifier.tryParse(entry.ore());
        if (tagId == null) {
            return null;
        }
        TagKey<Item> tag = TagKey.create(Registries.ITEM, tagId);
        Item first = null;
        for (Item item : BuiltInRegistries.ITEM) {
            if (item == Items.AIR || !new ItemStack(item).is(tag)) {
                continue;
            }
            Identifier key = BuiltInRegistries.ITEM.getKey(item);
            // Prefer vanilla's own item when the tag mixes namespaces; otherwise registry order.
            if (first == null || "minecraft".equals(key.getNamespace())) {
                first = item;
                if ("minecraft".equals(key.getNamespace())) {
                    break;
                }
            }
        }
        return first;
    }

    // --- loading -----------------------------------------------------------------------------

    /** The table for this server's current data packs (reloaded after {@code /reload}). */
    public static DepositTable forServer(MinecraftServer server) {
        ResourceManager resources = server.getResourceManager();
        Cached c = cache;
        if (c != null && c.server().get() == server && c.resources().get() == resources) {
            return c.table();
        }
        DepositTable table = load(resources);
        cache = new Cached(new WeakReference<>(server), new WeakReference<>(resources), table);
        return table;
    }

    private static DepositTable load(ResourceManager resources) {
        List<JsonObject> docs = new ArrayList<>();
        try {
            Map<Identifier, Resource> found = resources.listResources(FOLDER, id -> id.getPath().endsWith(".json"));
            List<Identifier> ids = new ArrayList<>(found.keySet());
            ids.sort(Identifier::compareTo);
            for (Identifier id : ids) {
                try (Reader r = found.get(id).openAsReader()) {
                    JsonElement e = JsonParser.parseReader(r);
                    if (e.isJsonObject()) {
                        docs.add(e.getAsJsonObject());
                    }
                } catch (Exception ex) {
                    NeroTechCommon.LOGGER.warn("[NeroTech] skipped deposit table {}: {}", id, ex.toString());
                }
            }
        } catch (RuntimeException ex) {
            NeroTechCommon.LOGGER.warn("[NeroTech] could not list deposit tables: {}", ex.toString());
        }
        return parse(docs);
    }

    /** Merge parsed documents in order (pure; tests call this directly). Malformed entries are skipped. */
    public static DepositTable parse(List<JsonObject> docs) {
        Map<String, List<Entry>> out = new HashMap<>();
        for (JsonObject doc : docs) {
            String dim = string(doc, "dimension");
            if (dim == null) {
                continue;
            }
            List<Entry> list = out.computeIfAbsent(dim, k -> new ArrayList<>());
            if (doc.has("replace") && doc.get("replace").isJsonPrimitive() && doc.get("replace").getAsBoolean()) {
                list.clear();
            }
            JsonElement entries = doc.get("entries");
            if (entries == null || !entries.isJsonArray()) {
                continue;
            }
            for (JsonElement el : entries.getAsJsonArray()) {
                Entry e = el.isJsonObject() ? entry(el.getAsJsonObject()) : null;
                if (e != null) {
                    list.add(e);
                }
            }
        }
        return new DepositTable(out);
    }

    @Nullable
    private static Entry entry(JsonObject o) {
        String ore = string(o, "ore");
        if (ore == null) {
            return null;
        }
        if (ore.startsWith("#")) {
            ore = ore.substring(1);
        }
        int weight = o.has("weight") && o.get("weight").isJsonPrimitive() ? o.get("weight").getAsInt() : 1;
        int[] cap = range(o, "capacity", 2_000, 6_000);
        int[] rich = range(o, "richness", 1_000, 1_000);
        if (weight <= 0 || cap == null || rich == null || cap[1] <= 0) {
            return null;
        }
        return new Entry(ore, string(o, "output"), weight, Math.max(1, cap[0]), cap[1], Math.max(0, rich[0]),
                Math.max(0, rich[1]));
    }

    @Nullable
    private static int[] range(JsonObject o, String key, int defMin, int defMax) {
        JsonElement el = o.get(key);
        if (el == null) {
            return new int[] {defMin, defMax};
        }
        try {
            if (el.isJsonArray()) {
                JsonArray a = el.getAsJsonArray();
                if (a.size() == 2) {
                    int lo = a.get(0).getAsInt();
                    int hi = a.get(1).getAsInt();
                    return new int[] {Math.min(lo, hi), Math.max(lo, hi)};
                }
                return null;
            }
            int v = el.getAsInt();
            return new int[] {v, v};
        } catch (RuntimeException ex) {
            return null;
        }
    }

    @Nullable
    private static String string(JsonObject o, String key) {
        JsonElement el = o.get(key);
        return el != null && el.isJsonPrimitive() ? el.getAsString() : null;
    }
}
