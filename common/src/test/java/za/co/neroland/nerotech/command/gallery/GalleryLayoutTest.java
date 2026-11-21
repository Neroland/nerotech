package za.co.neroland.nerotech.command.gallery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import za.co.neroland.nerotech.command.gallery.GalleryLayout.Bay;
import za.co.neroland.nerotech.command.gallery.GalleryLayout.Kind;
import za.co.neroland.nerotech.command.gallery.GalleryLayout.Part;

/** The 0.8.0 gallery plant as pure data: placement, the clear volume, coverage of every block and item. */
class GalleryLayoutTest {

    /** The 22 blocks this release adds: each must run live in a bay. */
    private static final Set<String> NEW_BLOCKS = Set.of("plasma_crucible", "plate_press", "filament_spooler",
            "blast_foundry", "foundry_brick", "foundry_heat_port", "centrifuge", "cryo_distiller",
            "atmospheric_condenser", "reaction_vat", "process_tank", "component_forge", "laser_etcher",
            "crystal_grower", "heat_pipe", "heat_exchanger", "deep_core_drill", "drill_derrick", "fuel_injector",
            "europium_flux_lattice", "induction_charger", "annunciator_panel");

    /**
     * Pre-0.8.0 blocks placed live by the rotunda exhibits in {@code NeroTechCommands} (and, like every placeable
     * NeroTech block, also on the floating display grid, which walks the block registry).
     */
    private static final Set<String> LEGACY_LIVE = Set.of("accelerator_coil", "advanced_fabricator",
            "advanced_ore_processor", "analytics_terminal", "auto_crafter", "battery_bank", "bio_generator",
            "chemical_processor", "collider_core", "conveyor_belt", "coolant_pump", "electrolyzer", "fabricator",
            "fusion_casing", "fusion_containment_glass", "fusion_reactor", "gas_turbine", "geothermal_generator",
            "grid_controller", "item_sorter", "nero_generator", "ore_processor", "radiator", "remediator",
            "robotic_arm", "scrubber", "singularity_vault", "solar_array", "tech_guide", "wind_turbine",
            "wireless_node");

    @Test
    void noTwoPartsShareAPosition() {
        Map<String, Part> seen = new HashMap<>();
        for (Part part : GalleryLayout.allParts()) {
            Part before = seen.put(part.x() + "," + part.y() + "," + part.z(), part);
            assertTrue(before == null, "overlap at " + part + " and " + before);
        }
    }

    @Test
    void everyPartIsInsideTheClearedVolumeAndOutsideTheRotunda() {
        for (Part part : GalleryLayout.allParts()) {
            assertTrue(GalleryLayout.inVolume(part.x(), part.y(), part.z()), "outside the clear volume: " + part);
            // Labels float two (four over the gas reactor) above a part: still inside the volume.
            assertTrue(GalleryLayout.inVolume(part.x(), part.y() + 4, part.z()), "label out of volume: " + part);
            assertFalse(GalleryLayout.inLegacyFootprint(part.x(), part.z()), "inside the rotunda: " + part);
        }
    }

    @Test
    void baysAreDisjointAndHoldTheirOwnParts() {
        List<Bay> bays = GalleryLayout.bays();
        assertEquals(GalleryLayout.BAY_IDS.size(), bays.size());
        for (int i = 0; i < bays.size(); i++) {
            Bay a = bays.get(i);
            assertEquals(GalleryLayout.BAY_IDS.get(i), a.id());
            for (Part part : a.parts()) {
                assertTrue(part.x() >= a.x0() && part.x() < a.x0() + a.width() && part.z() >= a.z0()
                        && part.z() < a.z0() + a.depth(), a.id() + " part outside its footprint: " + part);
            }
            assertTrue(GalleryLayout.inVolume(a.stopX(), 1, a.stopZ()), "tour stop out of volume: " + a.id());
            for (int j = i + 1; j < bays.size(); j++) {
                Bay b = bays.get(j);
                boolean apart = a.x0() + a.width() <= b.x0() || b.x0() + b.width() <= a.x0()
                        || a.z0() + a.depth() <= b.z0() || b.z0() + b.depth() <= a.z0();
                assertTrue(apart, a.id() + " overlaps " + b.id());
            }
        }
    }

    @Test
    void everyNewBlockRunsLiveInABay() {
        Set<String> missing = new TreeSet<>(NEW_BLOCKS);
        missing.removeAll(GalleryLayout.liveBlockPaths());
        assertTrue(missing.isEmpty(), "new blocks not in the plant: " + missing);
    }

    @Test
    void everyRegisteredBlockIsPlacedOrExplicitlyExcluded() {
        Set<String> blocks = names("blockstates");
        assertTrue(blocks.containsAll(NEW_BLOCKS), "a new block id is not registered");
        Set<String> uncovered = new TreeSet<>(blocks);
        uncovered.removeAll(GalleryLayout.liveBlockPaths());
        uncovered.removeAll(LEGACY_LIVE);
        uncovered.removeAll(GalleryCatalogue.UNPLACEABLE_BLOCKS);
        assertTrue(uncovered.isEmpty(), "blocks the gallery never places: " + uncovered);
        Set<String> stale = new TreeSet<>(GalleryCatalogue.UNPLACEABLE_BLOCKS);
        stale.removeAll(blocks);
        assertTrue(stale.isEmpty(), "exclusions that are not blocks: " + stale);
    }

    @Test
    void everyItemIsOnDisplayExactlyOnceAndTheCatalogueFits() {
        Set<String> blocks = names("blockstates");
        List<GalleryCatalogue.Entry> entries = new ArrayList<>();
        for (String id : names("items")) {
            entries.add(new GalleryCatalogue.Entry(id, blocks.contains(id)));
        }
        List<GalleryCatalogue.Chest> chests = GalleryCatalogue.plan(entries);
        assertTrue(chests.size() <= GalleryLayout.CATALOGUE_SLOTS, "catalogue needs " + chests.size() + " chests");
        Set<String> shown = new HashSet<>();
        for (GalleryCatalogue.Chest chest : chests) {
            assertTrue(chest.items().size() <= GalleryCatalogue.CHEST_SIZE);
            assertTrue(GalleryCatalogue.FAMILIES.contains(chest.family()));
            for (String item : chest.items()) {
                assertTrue(shown.add(item), "shown twice: " + item);
            }
        }
        assertEquals(names("items"), new TreeSet<>(shown));
        long catalogueSlots = GalleryLayout.allParts().stream().filter(p -> p.role().startsWith("catalogue.")).count();
        assertEquals(GalleryLayout.CATALOGUE_SLOTS, catalogueSlots);
        // The rack's tools are registered items, and the buckets of the excluded fluid blocks are on display.
        assertTrue(shown.containsAll(GalleryCatalogue.TOOLS), "a rack tool is not an item");
        for (String fluid : GalleryCatalogue.UNPLACEABLE_BLOCKS) {
            assertTrue(shown.contains(fluid + "_bucket"), fluid + " has no bucket on display");
        }
    }

    @Test
    void clearCoversTheDrillAndEveryAlarm() {
        Part drill = GalleryLayout.drillController();
        assertEquals(Kind.DEEP_CORE_DRILL, drill.kind());
        assertTrue(GalleryLayout.inVolume(drill.x(), drill.y(), drill.z()));
        Set<String> alarms = new TreeSet<>();
        int panels = 0;
        for (Part part : GalleryLayout.allParts()) {
            if (part.role().startsWith("alarm.")) {
                alarms.add(part.role().substring("alarm.".length()));
            }
            if (part.kind() == Kind.ANNUNCIATOR_PANEL) {
                panels++;
            }
        }
        assertEquals(new TreeSet<>(List.of("overheat", "starved", "full", "polluting", "failing")), alarms);
        assertEquals(5, panels);
    }

    @Test
    void theTourVisitsEveryBayInOrder() {
        List<Bay> bays = GalleryLayout.bays();
        for (int i = 0; i < bays.size(); i++) {
            assertEquals(i, GalleryTour.nearestStop(bays.get(i).stopX(), bays.get(i).stopZ()));
            assertEquals(i + 1 < bays.size() ? i + 1 : -1, GalleryTour.nextStop(i));
        }
    }

    @Test
    void familiesAreDeterministic() {
        assertEquals("lenses", GalleryCatalogue.family("void_focus_lens", false));
        assertEquals("drill_heads", GalleryCatalogue.family("glacite_drill_head", false));
        assertEquals("dies", GalleryCatalogue.family("wafer_die", false));
        assertEquals("buckets", GalleryCatalogue.family("cryo_coolant_bucket", false));
        assertEquals("modules", GalleryCatalogue.family("heat_sink_module", false));
        assertEquals("materials", GalleryCatalogue.family("cindersteel_ingot", false));
        assertEquals("tools", GalleryCatalogue.family("survey_scanner", false));
        assertEquals("components", GalleryCatalogue.family("quantum_chip", false));
        assertEquals("blocks", GalleryCatalogue.family("heat_pipe", true));
    }

    private static Set<String> names(String folder) {
        Path dir = assets().resolve(folder);
        Set<String> out = new TreeSet<>();
        try (Stream<Path> list = Files.list(dir)) {
            list.map(p -> p.getFileName().toString()).filter(n -> n.endsWith(".json"))
                    .forEach(n -> out.add(n.substring(0, n.length() - ".json".length())));
        } catch (IOException e) {
            throw new IllegalStateException(dir.toString(), e);
        }
        return out;
    }

    private static Path assets() {
        Path dir = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        for (Path p = dir; p != null; p = p.getParent()) {
            Path candidate = p.resolve("common/src/main/resources/assets/nerotech");
            if (Files.isDirectory(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("assets not found above " + dir);
    }
}
