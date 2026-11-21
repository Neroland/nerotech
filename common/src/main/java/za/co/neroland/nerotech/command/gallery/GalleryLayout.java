package za.co.neroland.nerotech.command.gallery;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The walkable plant that {@code /nerotech gallery} builds beside the pre-0.8.0 exhibits, as pure data: every
 * bay, every block it places (in placement order), the tour stop for each bay, and the bounded volume that
 * {@code /nerotech gallery clear} wipes. No Minecraft types, so the layout is checked by plain unit tests
 * (no overlaps, everything inside the volume, every new block placed live); {@link GalleryPlant} turns a
 * {@link Part} into a block and its contents.
 *
 * <p>Coordinates are relative to the gallery origin (the block the player stood on): {@code y = 0} is the
 * exhibit floor layer, machines stand at {@code y = 1}. Bays sit in three columns east of the pre-0.8.0
 * rotunda ({@code x > }{@value #LEGACY_MAX_X}). Inside a bay the walkway is {@code lz 0..2}, the machine row
 * {@code lz 3} facing north toward the walkway, and the Core Creative Batteries behind it at {@code lz 4};
 * machines in a chain touch, so they hand off by adjacency.
 *
 * <p>Holds no player data.
 */
public final class GalleryLayout {

    /** The pre-0.8.0 exhibits' footprint (unchanged): x, z bounds and height. */
    public static final int LEGACY_MIN_X = -34;
    public static final int LEGACY_MAX_X = 46;
    public static final int LEGACY_MIN_Z = -34;
    public static final int LEGACY_MAX_Z = 36;

    /** The whole gallery volume (legacy exhibits plus the plant), inclusive, relative to the origin. */
    public static final int MIN_X = LEGACY_MIN_X;
    public static final int MAX_X = 140;
    public static final int MIN_Z = LEGACY_MIN_Z;
    public static final int MAX_Z = LEGACY_MAX_Z;
    public static final int MIN_Y = 0;
    public static final int MAX_Y = 22;

    /** Catalogue display-chest slots (two rows of eight). */
    public static final int CATALOGUE_SLOTS = 16;

    private static final int COL_A = 52;
    private static final int COL_B = 80;
    private static final int COL_C = 110;
    private static final int[] ROWS = {-32, -18, -4, 10, 24};

    /** What a part is. {@code blockPath} is the NeroTech block id path it places, or null for other blocks. */
    public enum Kind {
        FLOOR(null), CASING(null), CREATIVE_BATTERY(null), BATTERY(null), CHEST(null),
        PLASMA_CRUCIBLE("plasma_crucible"), PLATE_PRESS("plate_press"), FILAMENT_SPOOLER("filament_spooler"),
        BLAST_FOUNDRY("blast_foundry"), FOUNDRY_BRICK("foundry_brick"), FOUNDRY_HEAT_PORT("foundry_heat_port"),
        CENTRIFUGE("centrifuge"), CRYO_DISTILLER("cryo_distiller"), ATMOSPHERIC_CONDENSER("atmospheric_condenser"),
        REACTION_VAT("reaction_vat"), PROCESS_TANK("process_tank"),
        COMPONENT_FORGE("component_forge"), LASER_ETCHER("laser_etcher"), CRYSTAL_GROWER("crystal_grower"),
        HEAT_PIPE("heat_pipe"), HEAT_EXCHANGER("heat_exchanger"),
        DEEP_CORE_DRILL("deep_core_drill"), DRILL_DERRICK("drill_derrick"),
        FUEL_INJECTOR("fuel_injector"), EUROPIUM_FLUX_LATTICE("europium_flux_lattice"),
        INDUCTION_CHARGER("induction_charger"), ANNUNCIATOR_PANEL("annunciator_panel"),
        NERO_GENERATOR("nero_generator"), ORE_PROCESSOR("ore_processor"), FUSION_REACTOR("fusion_reactor"),
        FUSION_CASING("fusion_casing"), FUSION_GLASS("fusion_containment_glass"), COOLANT_PUMP("coolant_pump"),
        RADIATOR("radiator"), AIR(null);

        private final String blockPath;

        Kind(String blockPath) {
            this.blockPath = blockPath;
        }

        /** The NeroTech block id path this kind places, or null (floor, Core batteries, chests, air). */
        public String blockPath() {
            return this.blockPath;
        }
    }

    /**
     * One placed block. {@code facing} is one of {@code N E S W} (horizontal machines) or {@code -};
     * {@code role} names the contents {@link GalleryPlant} fills it with (empty for none).
     */
    public record Part(Kind kind, int x, int y, int z, char facing, String role) {
    }

    /** One bay: its id (lang keys {@code gallery.nerotech.bay.<id>.*}), footprint, tour stop and parts. */
    public record Bay(String id, int x0, int z0, int width, int depth, int stopX, int stopZ, List<Part> parts) {

        public Bay {
            parts = List.copyOf(parts);
        }

        /** The label position for the bay title (centre of the machine row, above it). */
        public int labelX() {
            return this.x0 + this.width / 2;
        }
    }

    /** Bay ids in tour order. */
    public static final List<String> BAY_IDS = List.of("structural", "superconductor", "semiconductor", "optics",
            "chemistry", "fusion_fuel", "heat_loop", "drilling", "alarms_tools", "gas_fusion", "catalogue",
            "tank_farm");

    private static volatile List<Bay> cached;

    private GalleryLayout() {
    }

    /** Every bay of the plant, in tour order. */
    public static List<Bay> bays() {
        List<Bay> out = cached;
        if (out == null) {
            out = Collections.unmodifiableList(build());
            cached = out;
        }
        return out;
    }

    /** Every part of every bay, in placement order. */
    public static List<Part> allParts() {
        List<Part> out = new ArrayList<>();
        for (Bay bay : bays()) {
            out.addAll(bay.parts());
        }
        return out;
    }

    /** NeroTech block id paths the plant places live. */
    public static Set<String> liveBlockPaths() {
        Set<String> out = new LinkedHashSet<>();
        for (Part part : allParts()) {
            if (part.kind().blockPath() != null) {
                out.add(part.kind().blockPath());
            }
        }
        return out;
    }

    /** Whether a relative position lies inside the gallery volume. */
    public static boolean inVolume(int x, int y, int z) {
        return x >= MIN_X && x <= MAX_X && y >= MIN_Y && y <= MAX_Y && z >= MIN_Z && z <= MAX_Z;
    }

    /** Whether a relative position lies inside the pre-0.8.0 exhibits' footprint. */
    public static boolean inLegacyFootprint(int x, int z) {
        return x >= LEGACY_MIN_X && x <= LEGACY_MAX_X && z >= LEGACY_MIN_Z && z <= LEGACY_MAX_Z;
    }

    /** The drilling rig's controller (its chunk is the gallery's demo deposit). */
    public static Part drillController() {
        for (Part part : allParts()) {
            if (part.kind() == Kind.DEEP_CORE_DRILL) {
                return part;
            }
        }
        throw new IllegalStateException("layout has no drill");
    }

    // --- the bays ---------------------------------------------------------------------------------------------

    private static List<Bay> build() {
        List<Bay> bays = new ArrayList<>();
        bays.add(structural(COL_A, ROWS[0]));
        bays.add(superconductor(COL_A, ROWS[1]));
        bays.add(semiconductor(COL_A, ROWS[2]));
        bays.add(optics(COL_A, ROWS[3]));
        bays.add(chemistry(COL_A, ROWS[4]));
        bays.add(fusionFuel(COL_B, ROWS[0]));
        bays.add(heatLoop(COL_B, ROWS[1]));
        bays.add(drilling(COL_B, ROWS[2]));
        bays.add(alarmsAndTools(COL_B, ROWS[3]));
        bays.add(gasFusion(COL_B, ROWS[4]));
        bays.add(catalogue(COL_C, ROWS[0]));
        bays.add(tankFarm(COL_C, ROWS[1]));
        return bays;
    }

    /** Iron dust, coal and blaze powder → Plasma Crucible → Plate Press (plate, rod) → Filament Spooler. */
    private static Bay structural(int x0, int z0) {
        B b = new B("structural", x0, z0, 24, 6);
        b.chest(1, "structural.raw");
        b.machine(Kind.PLASMA_CRUCIBLE, 2, "structural.crucible");
        b.machine(Kind.PLATE_PRESS, 3, "structural.plate");
        b.machine(Kind.PLATE_PRESS, 4, "structural.rod");
        b.machine(Kind.FILAMENT_SPOOLER, 5, "structural.wire");
        b.chest(6, "product");
        // A warm Heat Pipe stub on the self-heating crucible: one of the three pipe heat levels on show.
        b.part(Kind.HEAT_PIPE, 2, 2, 3, '-', "pipe.warm");
        b.part(Kind.HEAT_PIPE, 2, 3, 3, '-', "pipe.warm");
        return b.bay();
    }

    /** Void Crystal + snow + ice → synthetic glacite → superconductor wire → superconductor coil. */
    private static Bay superconductor(int x0, int z0) {
        B b = new B("superconductor", x0, z0, 24, 6);
        b.chest(1, "superconductor.raw");
        b.machine(Kind.CRYSTAL_GROWER, 2, "superconductor.glacite");
        b.machine(Kind.FILAMENT_SPOOLER, 3, "superconductor.wire");
        b.machine(Kind.COMPONENT_FORGE, 4, "superconductor.coil");
        b.chest(5, "product");
        return b.bay();
    }

    /** Seed + quartz → boule → wafer → Logic Chip → Quantum Processor. */
    private static Bay semiconductor(int x0, int z0) {
        B b = new B("semiconductor", x0, z0, 24, 6);
        b.chest(1, "semiconductor.raw");
        b.machine(Kind.CRYSTAL_GROWER, 2, "semiconductor.boule");
        b.machine(Kind.PLATE_PRESS, 3, "semiconductor.wafer");
        b.machine(Kind.LASER_ETCHER, 4, "semiconductor.logic");
        b.machine(Kind.LASER_ETCHER, 5, "semiconductor.quantum");
        b.chest(6, "product");
        return b.bay();
    }

    /** Quartz and Void Crystal → focus lenses. */
    private static Bay optics(int x0, int z0) {
        B b = new B("optics", x0, z0, 24, 6);
        b.chest(1, "optics.raw");
        b.machine(Kind.CRYSTAL_GROWER, 2, "optics.quartz");
        b.machine(Kind.CRYSTAL_GROWER, 3, "optics.void");
        b.chest(4, "product");
        return b.bay();
    }

    /** Gunpowder → sulfur → acid (into a tank column); air; coal → CO2; logs + methane → resin → polymer sheet. */
    private static Bay chemistry(int x0, int z0) {
        B b = new B("chemistry", x0, z0, 24, 6);
        b.chest(1, "chemistry.raw");
        b.machine(Kind.CENTRIFUGE, 2, "chemistry.sulfur");
        b.machine(Kind.REACTION_VAT, 3, "chemistry.acid");
        b.part(Kind.PROCESS_TANK, 4, 1, 3, '-', "tank.sulfuric_acid");
        b.part(Kind.PROCESS_TANK, 4, 2, 3, '-', "tank.sulfuric_acid");
        b.machine(Kind.ATMOSPHERIC_CONDENSER, 5, "chemistry.air");
        b.machine(Kind.CRYO_DISTILLER, 6, "chemistry.co2");
        b.machine(Kind.REACTION_VAT, 7, "chemistry.resin");
        b.machine(Kind.PLATE_PRESS, 8, "chemistry.sheet");
        b.chest(9, "product");
        return b.bay();
    }

    /** Hydrogen + water → deuterium; a three-stage cascade to helium-3; glowstone → europium → lattice. */
    private static Bay fusionFuel(int x0, int z0) {
        B b = new B("fusion_fuel", x0, z0, 26, 6);
        b.chest(1, "fusion_fuel.raw");
        b.machine(Kind.CENTRIFUGE, 2, "fusion_fuel.deuterium");
        // lx 3 stays empty so the head centrifuge is not counted into the cascade.
        b.machine(Kind.CENTRIFUGE, 4, "fusion_fuel.cascade");
        b.machine(Kind.CENTRIFUGE, 5, "fusion_fuel.cascade");
        b.machine(Kind.CENTRIFUGE, 6, "fusion_fuel.cascade");
        b.machine(Kind.CENTRIFUGE, 8, "fusion_fuel.europium");
        b.machine(Kind.COMPONENT_FORGE, 9, "fusion_fuel.lattice");
        b.chest(10, "product");
        return b.bay();
    }

    /**
     * A coal-fired Nero Generator → five Heat Pipes → the Heat Port of a formed 3³ Blast Foundry; beside it a
     * Heat Exchanger dumping the same generator's heat into a water Process Tank; and a cold, unattached pipe.
     */
    private static Bay heatLoop(int x0, int z0) {
        B b = new B("heat_loop", x0, z0, 26, 6);
        // 3³ foundry shell lx 14..16, ly 1..3, lz 2..4; controller at the north wall centre facing north.
        for (int dx = 14; dx <= 16; dx++) {
            for (int dy = 1; dy <= 3; dy++) {
                for (int dz = 2; dz <= 4; dz++) {
                    if (dx == 15 && dy == 2 && dz == 3) {
                        b.part(Kind.AIR, dx, dy, dz, '-', "");
                    } else if (dx == 15 && dy == 2 && dz == 2) {
                        continue; // the controller, placed after the shell
                    } else if (dx == 14 && dy == 2 && dz == 3) {
                        b.part(Kind.FOUNDRY_HEAT_PORT, dx, dy, dz, '-', "foundry.port");
                    } else {
                        b.part(Kind.FOUNDRY_BRICK, dx, dy, dz, '-', "");
                    }
                }
            }
        }
        b.part(Kind.BLAST_FOUNDRY, 15, 2, 2, 'N', "foundry.batch");
        b.part(Kind.CREATIVE_BATTERY, 15, 2, 1, '-', "");
        // Pipes from the port end toward the generator (each new pipe reconnects the previous one), source last.
        for (int lx = 13; lx >= 9; lx--) {
            b.part(Kind.HEAT_PIPE, lx, 2, 3, '-', "pipe.hot");
        }
        b.part(Kind.CASING, 8, 1, 3, '-', "");
        b.part(Kind.NERO_GENERATOR, 8, 2, 3, 'N', "heat.generator");
        b.part(Kind.CASING, 8, 1, 4, '-', "");
        b.part(Kind.BATTERY, 8, 2, 4, '-', "");
        b.part(Kind.CASING, 7, 1, 3, '-', "");
        b.part(Kind.HEAT_EXCHANGER, 7, 2, 3, 'W', "");
        b.part(Kind.CASING, 6, 1, 3, '-', "");
        b.part(Kind.PROCESS_TANK, 6, 2, 3, '-', "tank.water");
        // A cold run touching nothing: level 0.
        b.part(Kind.HEAT_PIPE, 2, 1, 3, '-', "pipe.cold");
        b.part(Kind.HEAT_PIPE, 3, 1, 3, '-', "pipe.cold");
        return b.bay();
    }

    /** A Deep Core Drill on its three-block derrick, tapping the gallery's demo deposit. */
    private static Bay drilling(int x0, int z0) {
        B b = new B("drilling", x0, z0, 26, 6);
        for (int dy = 1; dy <= 3; dy++) {
            b.part(Kind.DRILL_DERRICK, 5, dy, 3, '-', "");
            b.part(Kind.CASING, 6, dy, 3, '-', "");
        }
        b.part(Kind.DEEP_CORE_DRILL, 5, 4, 3, 'N', "drill.head");
        b.part(Kind.CREATIVE_BATTERY, 6, 4, 3, '-', "");
        b.chest(8, "drill.heads");
        return b.bay();
    }

    /** One Annunciator Panel per alarm, each on a machine held in that state, then the tool rack and charger. */
    private static Bay alarmsAndTools(int x0, int z0) {
        B b = new B("alarms_tools", x0, z0, 26, 6);
        b.machine(Kind.ORE_PROCESSOR, 1, "alarm.overheat");
        b.part(Kind.ANNUNCIATOR_PANEL, 1, 1, 2, 'N', "");
        b.part(Kind.ORE_PROCESSOR, 5, 1, 3, 'N', "alarm.starved"); // no battery on purpose
        b.part(Kind.ANNUNCIATOR_PANEL, 5, 1, 2, 'N', "");
        b.machine(Kind.ORE_PROCESSOR, 9, "alarm.full");
        b.part(Kind.ANNUNCIATOR_PANEL, 9, 1, 2, 'N', "");
        b.part(Kind.NERO_GENERATOR, 13, 1, 3, 'N', "alarm.polluting");
        b.part(Kind.BATTERY, 13, 1, 4, '-', "");
        b.part(Kind.ANNUNCIATOR_PANEL, 13, 1, 2, 'N', "");
        // A 3³ reactor held hot and unfuelled (lx 16..18, ly 1..3, lz 3..5), controller on the north wall.
        fusionShell(b, 16, 1, 3, 3, List.of(), List.of());
        b.part(Kind.FUSION_REACTOR, 17, 2, 3, 'N', "alarm.failing");
        b.part(Kind.ANNUNCIATOR_PANEL, 17, 2, 2, 'N', "");
        // Tool rack + Induction Charger (stand by it to top a held tool up).
        b.chest(21, "rack.tools");
        b.chest(22, "rack.spares");
        b.part(Kind.INDUCTION_CHARGER, 23, 1, 3, 'N', "charger");
        b.part(Kind.CREATIVE_BATTERY, 23, 1, 4, '-', "");
        return b.bay();
    }

    /** A 5³ reactor fed by two Fuel Injectors, four Europium Flux Lattices in its shell, a coolant pump at the console. */
    private static Bay gasFusion(int x0, int z0) {
        B b = new B("gas_fusion", x0, z0, 26, 8);
        List<int[]> lattices = List.of(new int[] {2, 3, 4}, new int[] {6, 3, 4}, new int[] {4, 5, 4},
                new int[] {4, 1, 4});
        List<int[]> injectors = List.of(new int[] {3, 3, 2}, new int[] {5, 3, 2});
        fusionShell(b, 2, 1, 2, 5, lattices, injectors);
        b.part(Kind.FUSION_REACTOR, 4, 3, 2, 'N', "fusion.gas");
        b.part(Kind.FUSION_CASING, 4, 1, 1, '-', "");
        b.part(Kind.FUSION_CASING, 4, 2, 1, '-', "");
        b.part(Kind.COOLANT_PUMP, 4, 3, 1, '-', "coolant");
        b.part(Kind.RADIATOR, 4, 4, 1, '-', "");
        b.part(Kind.RADIATOR, 4, 5, 1, '-', "");
        b.part(Kind.CREATIVE_BATTERY, 5, 3, 1, '-', "");
        return b.bay();
    }

    /** Display chests: every NeroTech item, one family per chest ({@link GalleryCatalogue}). */
    private static Bay catalogue(int x0, int z0) {
        B b = new B("catalogue", x0, z0, 26, 7);
        for (int i = 0; i < CATALOGUE_SLOTS; i++) {
            int lx = 1 + (i % 8) * 3;
            int lz = i < 8 ? 3 : 6;
            b.part(Kind.CHEST, lx, 1, lz, 'N', "catalogue." + i);
        }
        return b.bay();
    }

    /** Process Tank columns in seven media, liquid and gas. */
    private static Bay tankFarm(int x0, int z0) {
        B b = new B("tank_farm", x0, z0, 26, 6);
        String[] media = {"water", "cryo_coolant", "sulfuric_acid", "polymer_resin", "hydrogen", "deuterium",
            "helium_3"};
        for (int i = 0; i < media.length; i++) {
            int lx = 1 + i * 3;
            int height = i % 2 == 0 ? 3 : 2;
            for (int dy = 1; dy <= height; dy++) {
                b.part(Kind.PROCESS_TANK, lx, dy, 3, '-', "tank." + media[i]);
            }
        }
        return b.bay();
    }

    /**
     * A hollow fusion shell of {@code size} at local ({@code lx}, {@code ly}, {@code lz}) min corner, casing with
     * a glass equator, the given lattice and injector positions, air inside. The controller position (north
     * wall centre) is skipped: callers place it after the shell.
     */
    private static void fusionShell(B b, int lx, int ly, int lz, int size, List<int[]> lattices,
            List<int[]> injectors) {
        int half = (size - 1) / 2;
        int cx = lx + half;
        int cy = ly + half;
        for (int dx = 0; dx < size; dx++) {
            for (int dy = 0; dy < size; dy++) {
                for (int dz = 0; dz < size; dz++) {
                    int x = lx + dx;
                    int y = ly + dy;
                    int z = lz + dz;
                    if (x == cx && y == cy && z == lz) {
                        continue;
                    }
                    boolean surface = dx == 0 || dx == size - 1 || dy == 0 || dy == size - 1 || dz == 0
                            || dz == size - 1;
                    if (!surface) {
                        b.part(Kind.AIR, x, y, z, '-', "");
                    } else if (contains(lattices, x, y, z)) {
                        b.part(Kind.EUROPIUM_FLUX_LATTICE, x, y, z, '-', "");
                    } else if (contains(injectors, x, y, z)) {
                        b.part(Kind.FUEL_INJECTOR, x, y, z, 'N', "fusion.injector");
                    } else {
                        b.part(y == cy ? Kind.FUSION_GLASS : Kind.FUSION_CASING, x, y, z, '-', "");
                    }
                }
            }
        }
    }

    private static boolean contains(List<int[]> list, int x, int y, int z) {
        for (int[] p : list) {
            if (p[0] == x && p[1] == y && p[2] == z) {
                return true;
            }
        }
        return false;
    }

    /** Collects one bay's parts in local coordinates and lays its floor. */
    private static final class B {
        private final String id;
        private final int x0;
        private final int z0;
        private final int width;
        private final int depth;
        private final List<Part> parts = new ArrayList<>();

        B(String id, int x0, int z0, int width, int depth) {
            this.id = id;
            this.x0 = x0;
            this.z0 = z0;
            this.width = width;
            this.depth = depth;
            for (int lx = 0; lx < width; lx++) {
                for (int lz = 0; lz < depth; lz++) {
                    this.parts.add(new Part(Kind.FLOOR, x0 + lx, 0, z0 + lz, '-', ""));
                }
            }
        }

        void part(Kind kind, int lx, int ly, int lz, char facing, String role) {
            this.parts.add(new Part(kind, this.x0 + lx, ly, this.z0 + lz, facing, role));
        }

        /** A powered machine on the row facing the walkway, its Creative Battery behind it. */
        void machine(Kind kind, int lx, String role) {
            part(kind, lx, 1, 3, 'N', role);
            part(Kind.CREATIVE_BATTERY, lx, 1, 4, '-', "");
        }

        void chest(int lx, String role) {
            part(Kind.CHEST, lx, 1, 3, 'N', role);
        }

        Bay bay() {
            return new Bay(this.id, this.x0, this.z0, this.width, this.depth, this.x0 + this.width / 2, this.z0,
                    this.parts);
        }
    }
}
