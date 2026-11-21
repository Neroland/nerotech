package za.co.neroland.nerotech.link;

import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

/**
 * The static half of the public {@code catalogue} section (link schema 2, decision 007): which
 * machines, materials and chains exist, in what order, and which recipe ids make up each route. The
 * recipes themselves are resolved at runtime from the loaded recipe manager (see
 * {@link CatalogueSection}); a recipe id that is not loaded is skipped, so a datapack that removes or
 * renames a recipe shortens a route instead of breaking the section.
 *
 * <p>Each step lists candidate recipe ids; the first one loaded wins. A few steps run on machines
 * with no recipe type (the Electrolyzer) or on vanilla crafting; those are {@link Step fixed} and are
 * shown only while every id they name is registered.
 *
 * <p>Public data only: identical for every player, no player data of any kind.
 */
final class ChainCatalogue {

    private ChainCatalogue() {
    }

    /** One input or output: {@code kind} is {@code item}, {@code fluid} or {@code gas}; a tag id starts with {@code #}. */
    record Stack(String kind, String id) {
    }

    /** One resolved step: the machine that runs it and what goes in and comes out. */
    record Step(String machine, List<Stack> inputs, List<Stack> outputs) {
    }

    /** A step reference: candidate recipe ids, or a fixed step for machines without a recipe type. */
    record StepRef(List<String> recipeIds, @Nullable Step fixed) {
    }

    /** One product of a chain and its two routes, as ordered steps. An empty route means "no route". */
    record Product(String id, List<StepRef> standalone, List<StepRef> offworld) {
    }

    /** One of the six chains. */
    record Chain(String id, List<Product> products) {
    }

    /** One machine: block id path, family, and the lang key of its one-line description. */
    record Machine(String id, String family) {

        String descKey() {
            return "block.nerotech." + this.id + ".tooltip";
        }
    }

    /** One material: kind, id path and the tags the catalogue reports when the item carries them. */
    record Material(String kind, String id, List<String> tags) {
    }

    static StepRef r(String... recipeIds) {
        return new StepRef(List.of(recipeIds), null);
    }

    static StepRef fixed(String machine, List<Stack> inputs, List<Stack> outputs) {
        return new StepRef(List.of(), new Step(machine, inputs, outputs));
    }

    static Stack item(String id) {
        return new Stack("item", id);
    }

    static Stack fluid(String id) {
        return new Stack("fluid", id);
    }

    static Stack gas(String id) {
        return new Stack("gas", id);
    }

    // --- machines ---------------------------------------------------------------------

    static final List<Machine> MACHINES = List.of(
            new Machine("ore_processor", "processing"),
            new Machine("advanced_ore_processor", "processing"),
            new Machine("chemical_processor", "processing"),
            new Machine("electrolyzer", "processing"),
            new Machine("fabricator", "processing"),
            new Machine("advanced_fabricator", "processing"),
            new Machine("auto_crafter", "processing"),
            new Machine("plasma_crucible", "metallurgy"),
            new Machine("plate_press", "metallurgy"),
            new Machine("filament_spooler", "metallurgy"),
            new Machine("blast_foundry", "metallurgy"),
            new Machine("foundry_heat_port", "metallurgy"),
            new Machine("centrifuge", "chemistry"),
            new Machine("cryo_distiller", "chemistry"),
            new Machine("atmospheric_condenser", "chemistry"),
            new Machine("reaction_vat", "chemistry"),
            new Machine("process_tank", "chemistry"),
            new Machine("component_forge", "components"),
            new Machine("laser_etcher", "components"),
            new Machine("crystal_grower", "components"),
            new Machine("heat_pipe", "heat"),
            new Machine("heat_exchanger", "heat"),
            new Machine("coolant_pump", "heat"),
            new Machine("radiator", "heat"),
            new Machine("deep_core_drill", "extraction"),
            new Machine("drill_derrick", "extraction"),
            new Machine("nero_generator", "power"),
            new Machine("solar_array", "power"),
            new Machine("wind_turbine", "power"),
            new Machine("geothermal_generator", "power"),
            new Machine("bio_generator", "power"),
            new Machine("gas_turbine", "power"),
            new Machine("battery_bank", "power"),
            new Machine("grid_controller", "power"),
            new Machine("wireless_node", "power"),
            new Machine("scrubber", "environment"),
            new Machine("remediator", "environment"),
            new Machine("collider_core", "accelerator"),
            new Machine("fusion_reactor", "fusion"),
            new Machine("fuel_injector", "fusion"),
            new Machine("induction_charger", "utility"),
            new Machine("annunciator_panel", "utility"),
            new Machine("analytics_terminal", "utility"),
            new Machine("item_sorter", "utility"),
            new Machine("robotic_arm", "utility"),
            new Machine("conveyor_belt", "utility"),
            new Machine("singularity_vault", "utility"));

    // --- materials (decision 001) -------------------------------------------------------

    static final List<Material> MATERIALS = List.of(
            new Material("item", "cindersteel_ingot", List.of("c:ingots/cindersteel")),
            new Material("item", "cindersteel_plate", List.of("c:plates/cindersteel")),
            new Material("item", "cindersteel_rod", List.of("c:rods/cindersteel")),
            new Material("item", "titanium_plate", List.of("c:plates/titanium")),
            new Material("item", "synthetic_glacite", List.of("c:gems/glacite")),
            new Material("item", "crystal_seed", List.of()),
            new Material("item", "crystal_boule", List.of()),
            new Material("item", "wafer", List.of()),
            new Material("item", "sulfur_dust", List.of("c:dusts/sulfur", "c:dusts")),
            new Material("item", "polymer_sheet", List.of("c:plates/polymer")),
            new Material("item", "europium_dust", List.of("c:dusts/europium", "c:dusts")),
            new Material("item", "logic_chip", List.of()),
            new Material("item", "quantum_processor", List.of()),
            new Material("item", "copper_wire", List.of("c:wires/copper")),
            new Material("item", "cindersteel_wire", List.of("c:wires/cindersteel")),
            new Material("item", "superconductor_wire", List.of("c:wires/superconductor")),
            new Material("item", "copper_coil", List.of()),
            new Material("item", "cindersteel_coil", List.of()),
            new Material("item", "superconductor_coil", List.of()),
            new Material("item", "servo", List.of()),
            new Material("item", "capacitor", List.of()),
            new Material("item", "heat_sink", List.of()),
            new Material("item", "quartz_focus_lens", List.of()),
            new Material("item", "glacite_focus_lens", List.of()),
            new Material("item", "ember_focus_lens", List.of()),
            new Material("item", "void_focus_lens", List.of()),
            new Material("item", "iron_drill_head", List.of()),
            new Material("item", "cindersteel_drill_head", List.of()),
            new Material("item", "glacite_drill_head", List.of()),
            new Material("fluid", "cryo_coolant", List.of()),
            new Material("fluid", "sulfuric_acid", List.of()),
            new Material("fluid", "polymer_resin", List.of()),
            new Material("gas", "hydrogen", List.of()),
            new Material("gas", "oxygen", List.of()),
            new Material("gas", "deuterium", List.of()),
            new Material("gas", "helium_3", List.of()),
            new Material("gas", "methane", List.of()),
            new Material("gas", "nitrogen", List.of()),
            new Material("gas", "carbon_dioxide", List.of()));

    // --- recipe type -> machine -----------------------------------------------------------

    private static final Map<String, String> MACHINE_BY_TYPE = Map.ofEntries(
            Map.entry("nerotech:ore_processing", "nerotech:ore_processor"),
            Map.entry("nerotech:chemical_processing", "nerotech:chemical_processor"),
            Map.entry("nerotech:fabricating", "nerotech:fabricator"),
            Map.entry("nerotech:advanced_fabricating", "nerotech:advanced_fabricator"),
            Map.entry("nerotech:collider", "nerotech:collider_core"),
            Map.entry("nerotech:alloying", "nerotech:plasma_crucible"),
            Map.entry("nerotech:pressing", "nerotech:plate_press"),
            Map.entry("nerotech:drawing", "nerotech:filament_spooler"),
            Map.entry("nerotech:foundry_casting", "nerotech:blast_foundry"),
            Map.entry("nerotech:centrifuging", "nerotech:centrifuge"),
            Map.entry("nerotech:cryo_distilling", "nerotech:cryo_distiller"),
            Map.entry("nerotech:condensing", "nerotech:atmospheric_condenser"),
            Map.entry("nerotech:reacting", "nerotech:reaction_vat"),
            Map.entry("nerotech:component_forging", "nerotech:component_forge"),
            Map.entry("nerotech:etching", "nerotech:laser_etcher"),
            Map.entry("nerotech:crystal_growing", "nerotech:crystal_grower"),
            Map.entry("minecraft:smelting", "minecraft:furnace"),
            Map.entry("minecraft:blasting", "minecraft:blast_furnace"));

    /** The machine block id that runs a recipe type, or null for a type the catalogue does not show. */
    @Nullable
    static String machineForRecipeType(String recipeTypeId) {
        return MACHINE_BY_TYPE.get(recipeTypeId);
    }

    // --- the six chains (BUILD-PROMPT "six chains", decision 003) -------------------------

    private static final StepRef STANDALONE_CINDERSTEEL = r("alloying_cindersteel_ingot_standalone",
            "collider_cindersteel_ingot");
    private static final StepRef OFFWORLD_CINDERSTEEL = r("foundry_casting_cindersteel_from_hematite",
            "alloying_cindersteel_ingot");
    /** Standalone cryo coolant: CO2 from coal, then the Reaction Vat. */
    private static final List<StepRef> STANDALONE_COOLANT = List.of(r("cryo_distilling_carbon_dioxide_from_coal"),
            r("reacting_cryo_coolant_from_carbon_dioxide", "reacting_cryo_coolant"));
    private static final StepRef ELECTROLYZER = fixed("nerotech:electrolyzer",
            List.of(fluid("minecraft:water")),
            List.of(gas("nerotech:hydrogen"), gas("nerotech:oxygen")));

    private static List<StepRef> route(Object... parts) {
        java.util.ArrayList<StepRef> out = new java.util.ArrayList<>();
        for (Object part : parts) {
            if (part instanceof StepRef ref) {
                out.add(ref);
            } else if (part instanceof List<?> list) {
                for (Object o : list) {
                    out.add((StepRef) o);
                }
            }
        }
        return List.copyOf(out);
    }

    static final List<Chain> CHAINS = List.of(
            new Chain("structural", List.of(
                    new Product("nerotech:cindersteel_ingot",
                            route(STANDALONE_CINDERSTEEL),
                            route(OFFWORLD_CINDERSTEEL)),
                    new Product("nerotech:cindersteel_plate",
                            route(STANDALONE_CINDERSTEEL, r("pressing_cindersteel_plate")),
                            route(OFFWORLD_CINDERSTEEL, r("pressing_cindersteel_plate"))),
                    new Product("nerotech:titanium_plate",
                            List.of(),
                            route(r("pressing_titanium_plate"))))),
            new Chain("superconductor", List.of(
                    new Product("nerotech:superconductor_wire",
                            route(STANDALONE_COOLANT, r("crystal_growing_synthetic_glacite"), STANDALONE_CINDERSTEEL,
                                    r("drawing_cindersteel_wire"), r("drawing_superconductor_wire")),
                            route(OFFWORLD_CINDERSTEEL, r("drawing_cindersteel_wire"),
                                    r("drawing_superconductor_wire"))),
                    new Product("nerotech:superconductor_coil",
                            route(STANDALONE_COOLANT, r("crystal_growing_synthetic_glacite"), STANDALONE_CINDERSTEEL,
                                    r("drawing_cindersteel_wire"), r("drawing_superconductor_wire"),
                                    r("component_forging_superconductor_coil")),
                            route(r("cryo_distilling_cryo_ice"), OFFWORLD_CINDERSTEEL, r("drawing_cindersteel_wire"),
                                    r("drawing_superconductor_wire"), r("component_forging_superconductor_coil"))),
                    new Product("nerotech:cryo_coolant",
                            route(STANDALONE_COOLANT),
                            route(r("cryo_distilling_cryo_ice"))))),
            new Chain("semiconductor", List.of(
                    new Product("nerotech:logic_chip",
                            route(r("crystal_growing_crystal_boule"), r("pressing_wafer"),
                                    r("crystal_growing_quartz_focus_lens"), r("drawing_copper_wire"),
                                    r("etching_logic_chip")),
                            route(r("crystal_growing_crystal_boule_salt_doped", "crystal_growing_crystal_boule"),
                                    r("pressing_wafer"), r("crystal_growing_quartz_focus_lens"),
                                    r("drawing_copper_wire"), r("etching_logic_chip"))),
                    new Product("nerotech:quantum_processor",
                            route(r("crystal_growing_crystal_boule"), r("pressing_wafer"),
                                    r("crystal_growing_void_focus_lens_standalone"), r("etching_quantum_processor")),
                            route(r("crystal_growing_crystal_boule_salt_doped", "crystal_growing_crystal_boule"),
                                    r("pressing_wafer"), r("crystal_growing_void_focus_lens"),
                                    r("etching_quantum_processor"))))),
            new Chain("optics", List.of(
                    new Product("nerotech:quartz_focus_lens",
                            route(r("crystal_growing_quartz_focus_lens_standalone", "crystal_growing_quartz_focus_lens")),
                            route(r("crystal_growing_quartz_focus_lens"))),
                    new Product("nerotech:glacite_focus_lens",
                            route(STANDALONE_COOLANT, r("crystal_growing_synthetic_glacite"),
                                    r("crystal_growing_glacite_focus_lens_standalone", "crystal_growing_glacite_focus_lens")),
                            route(r("crystal_growing_glacite_focus_lens"))),
                    new Product("nerotech:ember_focus_lens",
                            route(r("crystal_growing_ember_focus_lens_standalone")),
                            route(r("crystal_growing_ember_focus_lens"))),
                    new Product("nerotech:void_focus_lens",
                            route(r("crystal_growing_void_focus_lens_standalone")),
                            route(r("crystal_growing_void_focus_lens"))))),
            new Chain("chemistry", List.of(
                    new Product("nerotech:sulfuric_acid",
                            route(r("centrifuging_sulfur_from_gunpowder"), r("reacting_sulfuric_acid")),
                            route(r("centrifuging_sulfur_from_sulfide"), r("reacting_sulfuric_acid"))),
                    new Product("nerotech:polymer_sheet",
                            route(r("cryo_distilling_methane_from_coal"), r("reacting_polymer_resin"),
                                    r("pressing_polymer_sheet")),
                            route(r("cryo_distilling_methane"), r("reacting_polymer_resin"),
                                    r("pressing_polymer_sheet"))),
                    new Product("nerotech:carbon_dioxide",
                            route(r("cryo_distilling_carbon_dioxide_from_coal")),
                            route(r("cryo_distilling_carbon_dioxide"))))),
            new Chain("fusion_fuel", List.of(
                    new Product("nerotech:deuterium",
                            route(ELECTROLYZER, r("centrifuging_deuterium_from_hydrogen")),
                            route(r("cryo_distilling_deuterium"))),
                    new Product("nerotech:helium_3",
                            route(ELECTROLYZER, r("centrifuging_deuterium_from_hydrogen"),
                                    r("centrifuging_deuterium_cascade_stage_1"),
                                    r("centrifuging_deuterium_cascade_stage_2"),
                                    r("centrifuging_helium_3_cascade_stage_3")),
                            route(r("cryo_distilling_helium_3"))),
                    new Product("nerotech:europium_flux_lattice",
                            route(r("centrifuging_europium_from_glowstone"),
                                    r("component_forging_europium_flux_lattice")),
                            route(r("centrifuging_europium"), r("component_forging_europium_flux_lattice"))))));
}
