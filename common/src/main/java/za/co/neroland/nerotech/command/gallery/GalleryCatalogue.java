package za.co.neroland.nerotech.command.gallery;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Pure rules for the gallery's display chests: which family each NeroTech item belongs to, and how the
 * families are packed into the catalogue bay's chests (one family per chest, split at 27 stacks). The
 * builder feeds it the item ids it reads from the registry, so a new or renamed item shows up without an
 * edit here. Also the explicit list of blocks the gallery cannot place (fluid source blocks: they would flow;
 * they are on display as buckets instead).
 */
public final class GalleryCatalogue {

    /** Stacks one chest holds. */
    public static final int CHEST_SIZE = 27;

    /** Family ids in display order (lang keys {@code gallery.nerotech.catalogue.<family>}). */
    public static final List<String> FAMILIES = List.of("blocks", "materials", "components", "lenses",
            "drill_heads", "dies", "modules", "tools", "fuel", "buckets");

    /** The tools (and the Configurator) on the rack and in the tools chest. */
    public static final Set<String> TOOLS = Set.of("power_drill", "cutting_lance", "arc_wrench", "multimeter",
            "survey_scanner", "thermal_goggles", "filter_mask", "configurator");

    /**
     * NeroTech blocks never placed as blocks: the three fluid source blocks (they would flow across the
     * gallery). Their buckets are in the {@code buckets} chest.
     */
    public static final Set<String> UNPLACEABLE_BLOCKS = Set.of("cryo_coolant", "sulfuric_acid", "polymer_resin");

    private static final List<String> MATERIAL_SUFFIXES = List.of("_dust", "_ingot", "_plate", "_rod", "_wire",
            "_coil");
    private static final Set<String> MATERIALS = Set.of("synthetic_glacite", "crystal_boule", "crystal_seed",
            "wafer", "polymer_sheet");

    /** One display chest: its family and the item id paths in it. */
    public record Chest(String family, List<String> items) {

        public Chest {
            items = List.copyOf(items);
        }
    }

    /** One item as the plan sees it. */
    public record Entry(String path, boolean blockItem) {
    }

    private GalleryCatalogue() {
    }

    /** The family of an item id path. Never null: anything unmatched is a component. */
    public static String family(String path, boolean blockItem) {
        if (blockItem) {
            return "blocks";
        }
        if (TOOLS.contains(path)) {
            return "tools";
        }
        if (path.endsWith("_bucket")) {
            return "buckets";
        }
        if (path.endsWith("_focus_lens")) {
            return "lenses";
        }
        if (path.endsWith("_drill_head")) {
            return "drill_heads";
        }
        if (path.endsWith("_die")) {
            return "dies";
        }
        if (path.endsWith("_module")) {
            return "modules";
        }
        if (path.endsWith("_cell")) {
            return "fuel";
        }
        if (MATERIALS.contains(path)) {
            return "materials";
        }
        for (String suffix : MATERIAL_SUFFIXES) {
            if (path.endsWith(suffix)) {
                return "materials";
            }
        }
        return "components";
    }

    /** Pack items into chests: families in {@link #FAMILIES} order, input order within a family. */
    public static List<Chest> plan(List<Entry> items) {
        Map<String, List<String>> byFamily = new LinkedHashMap<>();
        for (String family : FAMILIES) {
            byFamily.put(family, new ArrayList<>());
        }
        for (Entry entry : items) {
            byFamily.get(family(entry.path(), entry.blockItem())).add(entry.path());
        }
        List<Chest> chests = new ArrayList<>();
        byFamily.forEach((family, paths) -> {
            for (int from = 0; from < paths.size(); from += CHEST_SIZE) {
                chests.add(new Chest(family, paths.subList(from, Math.min(paths.size(), from + CHEST_SIZE))));
            }
        });
        return chests;
    }
}
