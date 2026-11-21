package za.co.neroland.nerotech.recipe.graph;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.junit.jupiter.api.Test;

import za.co.neroland.nerotech.machine.components.RecipeFixture;

/**
 * The recipe-graph test: every shipped recipe and item tag, read from the resources with no registry
 * bootstrap (through {@link RecipeFixture}, so both the 26.x short ingredient form and the 1.21.1 object
 * form parse), turned into a graph of items, fluids and gases, and solved for the cheapest way to make
 * each node in two modes.
 *
 * <h2>Modes</h2>
 * <ul>
 *   <li><b>standalone</b> — a Core + NeroTech server. Only NeroTech's own tags, Core's materials and the
 *       vanilla/loader convention tags in {@link #CONVENTION_TAGS} are populated; every {@code nerospace:}
 *       entry (all written {@code "required": false}) is removed, and only the Overworld and Nether
 *       condenser atmospheres exist ({@link #STANDALONE_ATMOSPHERES}).</li>
 *   <li><b>offworld</b> — Nerospace installed. Its ids are mined leaves; so is every {@code c:} tag it
 *       ships ({@link #NEROSPACE_TAGS}) and every optional tag a {@code nerotech:inputs/*} tag names that
 *       no file here defines (the tags the Nerospace handover adds). Every atmosphere exists.</li>
 * </ul>
 *
 * <h2>Cost model (FE)</h2>
 * <ul>
 *   <li>A process recipe ({@code nerotech:alloying} … {@code nerotech:crystal_growing}) costs
 *       {@code energy × time}. Catalysts must be reachable but cost nothing (never consumed). Heat
 *       ({@code min_heat}) and ambient speed-ups are ignored: both routes pay them alike.</li>
 *   <li>The four single-item machine types cost {@code machineNePerTick × machineBaseProcessTicks}
 *       (3,600); a chemical processing run also takes 2.5 units of
 *       oxygen.</li>
 *   <li>A collision costs what the ring spends getting there: one injection plus one guide pass per
 *       {@code acceleratorBoostPerGuide} of speed, at {@code acceleratorNePerGuide} each. With the
 *       defaults ({@code E = ¼v²}, +2 speed per guide) that is {@code 50 × (1 + ⌈√E⌉)}.</li>
 *   <li>Crafting costs nothing beyond its ingredients; smelting and blasting add an eighth of a leaf of
 *       fuel.</li>
 *   <li>Raw leaves: 100 per vanilla item from {@link #VANILLA_LEAVES} (×9 for the storage
 *       blocks in {@link #NINE_LEAF_BLOCKS}) and per mined Nerospace item or tag;
 *       2,000 per Core material tag ({@link #CORE_TAGS}: Core makes these from vanilla
 *       inputs with its own recipes, outside this graph); 0.1 per mB of water or lava.</li>
 *   <li>Mechanics with no recipe JSON: the Electrolyzer (500 mB water and 8,000 FE → 2 hydrogen + 1
 *       oxygen), filling a bucket from a tank, and a Scrubber dirtying a filter.</li>
 *   <li>A recipe's whole cost is charged to <em>each</em> of its outputs (no split for by-products), per
 *       unit of output: items per item (chance outputs at their expected count), fluids per mB, gases per
 *       unit. Centrifuge cascade stages ({@code "atmosphere": "cascade_n"}) are modelled as a chain: stage
 *       {@code n > 1} only takes gas that left stage {@code n - 1}.</li>
 * </ul>
 * The cheapest cost of every node is the fixpoint of relaxing every edge until nothing gets cheaper; an
 * unreachable node never gets a cost.
 *
 * <p>The ratio contract (decision 003) is {@code 3 ≤ standalone / offworld ≤ 5} per row of {@link #ROWS}.
 * {@link #OWNER_PENDING} is empty since the owner decisions of the chains report were applied: every row
 * must hold the band. A row may only be added there with the reason and an owner decision.
 */
class RecipeGraphTest {

    static final double LEAF = 100.0D;
    static final double CORE_LEAF = 2_000.0D;
    static final double WATER_PER_MB = 0.1D;
    static final double MACHINE_FE = 30.0D * 120.0D;
    static final double CHEMICAL_OXYGEN = 2.5D;
    static final double ELECTROLYZER_FE = 40.0D * 200.0D;
    static final double COLLIDER_NE_PER_GUIDE = 50.0D;
    static final double RATIO_MIN = 3.0D;
    static final double RATIO_MAX = 5.0D;

    /** Vanilla items a recipe may take as a free leaf: Overworld, Nether and End items, or crafted from them. */
    static final Set<String> VANILLA_LEAVES = Set.of(
            "minecraft:amethyst_shard", "minecraft:anvil", "minecraft:blaze_powder", "minecraft:blue_ice",
            "minecraft:book", "minecraft:bucket", "minecraft:cauldron", "minecraft:charcoal", "minecraft:clay_ball",
            "minecraft:coal", "minecraft:comparator", "minecraft:composter", "minecraft:copper_block",
            "minecraft:copper_ingot", "minecraft:copper_ore", "minecraft:crafting_table",
            "minecraft:deepslate_copper_ore", "minecraft:deepslate_gold_ore", "minecraft:deepslate_iron_ore",
            "minecraft:diamond", "minecraft:echo_shard", "minecraft:ender_pearl", "minecraft:flint",
            "minecraft:furnace", "minecraft:glass", "minecraft:glass_pane", "minecraft:glowstone_dust",
            "minecraft:gold_ingot", "minecraft:gold_nugget", "minecraft:gold_ore", "minecraft:gravel",
            "minecraft:gunpowder", "minecraft:hay_block", "minecraft:hopper", "minecraft:iron_block",
            "minecraft:iron_ingot", "minecraft:iron_ore", "minecraft:nether_brick", "minecraft:netherite_scrap",
            "minecraft:oak_log", "minecraft:obsidian", "minecraft:packed_ice", "minecraft:paper",
            "minecraft:piston", "minecraft:quartz", "minecraft:raw_copper", "minecraft:raw_copper_block",
            "minecraft:raw_gold", "minecraft:raw_gold_block", "minecraft:raw_iron", "minecraft:raw_iron_block",
            "minecraft:redstone", "minecraft:redstone_lamp", "minecraft:snow_block", "minecraft:sugar",
            "minecraft:tinted_glass", "minecraft:water_bucket", "minecraft:dried_kelp_block");

    /** Storage blocks worth nine leaves (nine ingots, raw ores or wheat). */
    static final Set<String> NINE_LEAF_BLOCKS = Set.of("minecraft:raw_iron_block", "minecraft:raw_gold_block",
            "minecraft:raw_copper_block", "minecraft:iron_block", "minecraft:copper_block", "minecraft:hay_block");

    /** Vanilla and loader convention tags every server has, reduced to the vanilla members recipes need. */
    static final Map<String, List<String>> CONVENTION_TAGS = Map.of(
            "c:ingots/iron", List.of("minecraft:iron_ingot"),
            "c:ingots/copper", List.of("minecraft:copper_ingot"),
            "c:ingots/gold", List.of("minecraft:gold_ingot"),
            "c:gems/quartz", List.of("minecraft:quartz"),
            "c:gems/diamond", List.of("minecraft:diamond"),
            "c:dusts/redstone", List.of("minecraft:redstone"),
            "minecraft:logs", List.of("minecraft:oak_log"),
            "minecraft:coals", List.of("minecraft:coal", "minecraft:charcoal"));

    /** Core-owned material tags (Nero Alloy, Starsteel, Void Crystal, Plasma Glass are never re-registered). */
    static final Set<String> CORE_TAGS = Set.of("c:gems/void_crystal", "c:ingots/starsteel",
            "c:ingots/nero_alloy", "c:glass_blocks/plasma");

    /** {@code c:} item tags Nerospace ships today (its resource audit); mined leaves offworld. */
    static final Set<String> NEROSPACE_TAGS = Set.of("c:gems/cindrite", "c:gems/glacite", "c:gems/xertz_quartz",
            "c:gems/voidglass", "c:gems/crystal_shard", "c:gems/deuterium", "c:gems/oxidant", "c:dusts/sulfur",
            "c:dusts/salt", "c:dusts/ash", "c:raw_materials/hematite", "c:raw_materials/ilmenite",
            "c:raw_materials/sulfide", "c:raw_materials/nerosium", "c:raw_materials/nerosteel", "c:ingots/titanium",
            "c:ingots/nerosium", "c:ingots/nerosteel", "c:ores/hematite", "c:ores/ilmenite", "c:ores/methane",
            "c:ores/sulfide", "c:ores/europium", "c:ores/salt", "c:ores/deuterium", "c:ores/cindrite",
            "c:ores/glacite", "c:ores/xertz_quartz", "c:ores/voidglass", "c:ores/amber", "c:ores/oxidant");

    /** Condenser atmospheres a Core + NeroTech server has: the Overworld's air and the Nether's. */
    static final Set<String> STANDALONE_ATMOSPHERES = Set.of("EARTHLIKE", "SULFUROUS");

    /** Items allowed to be unreachable standalone, with the reason. */
    static final Map<String, String> STANDALONE_EXEMPT = Map.of(
            "nerotech:titanium_plate", "decision 003: optional material; every recipe taking it has a cindersteel twin");

    /** Decision 003's rows: label → the node whose cost is compared. */
    static final Map<String, String> ROWS = rows();

    /** Rows allowed outside the band, with the reason (none: every row holds 3-5x). */
    static final Map<String, String> OWNER_PENDING = Map.of();

    /** New 0.8.0 materials that must have two uses: a NeroTech recipe input and a sibling-facing tag. */
    static final List<String> NEW_MATERIALS = List.of("cindersteel_ingot", "cindersteel_plate", "cindersteel_rod",
            "titanium_plate", "synthetic_glacite", "crystal_seed", "crystal_boule", "wafer", "sulfur_dust",
            "polymer_sheet", "europium_dust", "logic_processor", "quantum_processor", "copper_wire",
            "cindersteel_wire", "superconductor_wire", "copper_coil", "cindersteel_coil", "superconductor_coil",
            "servo", "capacitor", "heat_sink", "quartz_focus_lens", "glacite_focus_lens", "ember_focus_lens",
            "void_focus_lens");

    /** New liquids: a NeroTech recipe input and a fluid tag. */
    static final List<String> NEW_LIQUIDS = List.of("cryo_coolant", "sulfuric_acid", "polymer_resin");

    /** New gases: a NeroTech recipe input, or a machine that burns them (no gas tag registry exists). */
    static final List<String> NEW_GASES = List.of("deuterium", "helium_3", "methane", "nitrogen", "carbon_dioxide");
    static final Map<String, String> GAS_MACHINE_CONSUMERS = Map.of(
            "nerotech:helium_3", "Fuel Injector", "nerotech:deuterium", "Fuel Injector");

    enum Mode { STANDALONE, OFFWORLD }

    /** One input: any of {@code alternatives}, {@code amount} of it. */
    record Input(Set<String> alternatives, double amount) {
    }

    record Output(String node, double amount) {
    }

    record Edge(String name, List<Input> inputs, List<Set<String>> catalysts, List<Output> outputs, double energy) {
    }

    record Solution(Map<String, Double> cost, Map<String, String> via, List<Edge> edges) {
    }

    private static final Map<Mode, Solution> SOLVED = new EnumMap<>(Mode.class);

    // --- tests -----------------------------------------------------------------------------------

    @Test
    void everyItemIsReachableStandalone() {
        Solution standalone = solve(Mode.STANDALONE);
        List<String> missing = new ArrayList<>();
        for (String id : itemIds()) {
            if (!standalone.cost().containsKey("item:" + id) && !STANDALONE_EXEMPT.containsKey(id)) {
                missing.add(id);
            }
        }
        assertTrue(missing.isEmpty(), "not reachable on a Core + NeroTech server: " + missing);
        for (String id : STANDALONE_EXEMPT.keySet()) {
            assertTrue(solve(Mode.OFFWORLD).cost().containsKey("item:" + id), id + " must be reachable offworld");
        }
    }

    @Test
    void everyItemIsReachableOffworld() {
        Solution offworld = solve(Mode.OFFWORLD);
        List<String> missing = itemIds().stream().filter(id -> !offworld.cost().containsKey("item:" + id)).toList();
        assertTrue(missing.isEmpty(), "not reachable with Nerospace installed: " + missing);
    }

    @Test
    void everyBlockHasACraftingRecipe() {
        Set<String> crafted = new HashSet<>();
        for (Map.Entry<String, JsonObject> recipe : recipes().entrySet()) {
            String type = recipe.getValue().get("type").getAsString();
            if (type.startsWith("minecraft:crafting")) {
                crafted.add(resultId(recipe.getValue().get("result")));
            }
        }
        Set<String> blocks = new TreeSet<>(names("assets/nerotech/blockstates"));
        blocks.retainAll(names("assets/nerotech/items"));
        List<String> missing = blocks.stream().filter(b -> !crafted.contains("nerotech:" + b)).toList();
        assertTrue(missing.isEmpty(), "blocks with no crafting recipe: " + missing);
    }

    @Test
    void standaloneRoutesCostThreeToFiveTimesOffworld() {
        Solution standalone = solve(Mode.STANDALONE);
        Solution offworld = solve(Mode.OFFWORLD);
        List<String> problems = new ArrayList<>();
        StringBuilder table = new StringBuilder("decision 003 ratios (standalone / offworld FE):\n");
        for (Map.Entry<String, String> row : ROWS.entrySet()) {
            Double s = standalone.cost().get(row.getValue());
            Double o = offworld.cost().get(row.getValue());
            if (s == null || o == null) {
                problems.add(row.getKey() + " unreachable (standalone " + s + ", offworld " + o + ")");
                continue;
            }
            double ratio = s / o;
            boolean pending = OWNER_PENDING.containsKey(row.getKey());
            table.append(String.format(Locale.ROOT, "  %-20s %10.0f / %9.0f = %5.2f  %s via %s / %s%n", row.getKey(),
                    s, o, ratio, pending ? "PENDING" : "ok", standalone.via().get(row.getValue()),
                    offworld.via().get(row.getValue())));
            if (!pending && (ratio < RATIO_MIN || ratio > RATIO_MAX)) {
                problems.add(String.format(Locale.ROOT, "%s ratio %.2f outside [%.0f, %.0f]", row.getKey(), ratio,
                        RATIO_MIN, RATIO_MAX));
            }
        }
        System.out.print(table);
        assertTrue(OWNER_PENDING.keySet().stream().allMatch(ROWS::containsKey), "pending rows must be rows");
        assertTrue(problems.isEmpty(), problems + "\n" + table);
    }

    @Test
    void everyNewMaterialHasTwoUses() {
        Set<String> inputs = recipeInputItems();
        Set<String> tagged = siblingTaggedItems();
        List<String> problems = new ArrayList<>();
        for (String name : NEW_MATERIALS) {
            String id = "nerotech:" + name;
            if (!inputs.contains(id)) {
                problems.add(id + " is no NeroTech recipe's input");
            }
            if (!tagged.contains(id)) {
                problems.add(id + " carries no sibling-facing c:/nerotech: tag");
            }
        }
        Set<String> fluidInputs = new HashSet<>();
        Set<String> gasInputs = new HashSet<>();
        for (JsonObject recipe : recipes().values()) {
            if (recipe.has("fluid_in")) {
                fluidInputs.addAll(expand(recipe.getAsJsonObject("fluid_in").get("id").getAsString(), "fluid"));
            }
            if (recipe.has("gas_in")) {
                gasInputs.add(recipe.getAsJsonObject("gas_in").get("id").getAsString());
            }
        }
        for (String name : NEW_LIQUIDS) {
            String id = "nerotech:" + name;
            if (!fluidInputs.contains(id)) {
                problems.add(id + " is no NeroTech recipe's fluid input");
            }
            boolean tag = RecipeFixture.resource("data/c/tags/fluid/" + name + ".json").isPresent()
                    || RecipeFixture.resource("data/nerotech/tags/fluid/" + name + ".json").isPresent();
            if (!tag) {
                problems.add(id + " has no fluid tag");
            }
        }
        for (String name : NEW_GASES) {
            String id = "nerotech:" + name;
            if (!gasInputs.contains(id) && !GAS_MACHINE_CONSUMERS.containsKey(id)) {
                problems.add(id + " is no recipe's gas input and no machine burns it");
            }
        }
        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }

    @Test
    void everyVanillaIngredientIsAnAllowedLeaf() {
        Set<String> unknown = new TreeSet<>();
        for (Map.Entry<String, JsonObject> recipe : recipes().entrySet()) {
            for (JsonElement ingredient : ingredientElements(recipe.getValue())) {
                for (String ref : RecipeFixture.references(ingredient)) {
                    if (ref.startsWith("minecraft:") && !VANILLA_LEAVES.contains(ref)) {
                        unknown.add(ref + " (" + recipe.getKey() + ")");
                    }
                }
            }
        }
        assertTrue(unknown.isEmpty(), "vanilla ingredients missing from VANILLA_LEAVES: " + unknown);
    }

    // --- graph -----------------------------------------------------------------------------------

    static synchronized Solution solve(Mode mode) {
        return SOLVED.computeIfAbsent(mode, RecipeGraphTest::solveUncached);
    }

    private static Solution solveUncached(Mode mode) {
        List<Edge> edges = edges(mode);
        Map<String, Double> cost = new HashMap<>();
        Map<String, String> via = new HashMap<>();
        for (Edge edge : edges) {
            for (Input input : edge.inputs()) {
                for (String node : input.alternatives()) {
                    leafCost(node, mode).ifPresent(c -> {
                        cost.put(node, c);
                        via.put(node, "leaf");
                    });
                }
            }
            for (Set<String> catalyst : edge.catalysts()) {
                for (String node : catalyst) {
                    leafCost(node, mode).ifPresent(c -> {
                        cost.put(node, c);
                        via.put(node, "leaf");
                    });
                }
            }
        }
        for (int pass = 0; pass < 1_000; pass++) {
            boolean changed = false;
            for (Edge edge : edges) {
                double total = edge.energy();
                boolean ready = true;
                for (Input input : edge.inputs()) {
                    Double best = cheapest(input.alternatives(), cost);
                    if (best == null) {
                        ready = false;
                        break;
                    }
                    total += best * input.amount();
                }
                if (!ready || edge.catalysts().stream().anyMatch(c -> cheapest(c, cost) == null)) {
                    continue;
                }
                for (Output output : edge.outputs()) {
                    if (output.amount() <= 0.0D) {
                        continue;
                    }
                    double unit = total / output.amount();
                    Double known = cost.get(output.node());
                    if (known == null || unit < known * (1.0D - 1e-9D)) {
                        cost.put(output.node(), unit);
                        via.put(output.node(), edge.name());
                        changed = true;
                    }
                }
            }
            if (!changed) {
                break;
            }
        }
        return new Solution(cost, via, edges);
    }

    private static Double cheapest(Set<String> alternatives, Map<String, Double> cost) {
        Double best = null;
        for (String node : alternatives) {
            Double c = cost.get(node);
            if (c != null && (best == null || c < best)) {
                best = c;
            }
        }
        return best;
    }

    private static Optional<Double> leafCost(String node, Mode mode) {
        int colon = node.indexOf(':');
        String kind = node.substring(0, colon);
        String id = node.substring(colon + 1);
        switch (kind) {
            case "item":
                if (VANILLA_LEAVES.contains(id)) {
                    return Optional.of(NINE_LEAF_BLOCKS.contains(id) ? 9.0D * LEAF : LEAF);
                }
                return mode == Mode.OFFWORLD && id.startsWith("nerospace:") ? Optional.of(LEAF) : Optional.empty();
            case "mined":
                return mode == Mode.OFFWORLD ? Optional.of(LEAF) : Optional.empty();
            case "core":
                return Optional.of(CORE_LEAF);
            case "fluid":
                return "minecraft:water".equals(id) || "minecraft:lava".equals(id)
                        ? Optional.of(WATER_PER_MB) : Optional.empty();
            default:
                return Optional.empty();
        }
    }

    private static List<Edge> edges(Mode mode) {
        List<Edge> out = new ArrayList<>();
        for (Map.Entry<String, JsonObject> entry : recipes().entrySet()) {
            Edge edge = edge(entry.getKey(), entry.getValue(), mode);
            if (edge != null) {
                out.add(edge);
            }
        }
        out.add(new Edge("mechanic:electrolyzer", List.of(new Input(Set.of("fluid:minecraft:water"), 500.0D)), List.of(),
                List.of(new Output("gas:nerotech:hydrogen", 2.0D), new Output("gas:nerospace:oxygen", 1.0D)),
                ELECTROLYZER_FE));
        for (String liquid : NEW_LIQUIDS) {
            out.add(new Edge("mechanic:fill_bucket_" + liquid,
                    List.of(new Input(Set.of("fluid:nerotech:" + liquid), 1_000.0D),
                            new Input(Set.of("item:minecraft:bucket"), 1.0D)),
                    List.of(), List.of(new Output("item:nerotech:" + liquid + "_bucket", 1.0D)), 0.0D));
        }
        out.add(new Edge("mechanic:scrubber_filter", List.of(new Input(Set.of("item:nerotech:filter_cartridge"), 1.0D)),
                List.of(), List.of(new Output("item:nerotech:dirty_filter", 1.0D)), 0.0D));
        return out;
    }

    private static Edge edge(String name, JsonObject json, Mode mode) {
        String type = json.get("type").getAsString();
        List<Input> inputs = new ArrayList<>();
        List<Output> outputs = new ArrayList<>();
        switch (type) {
            case "minecraft:crafting_shaped": {
                Map<Character, Integer> counts = new HashMap<>();
                for (JsonElement row : json.getAsJsonArray("pattern")) {
                    for (char c : row.getAsString().toCharArray()) {
                        if (c != ' ') {
                            counts.merge(c, 1, Integer::sum);
                        }
                    }
                }
                JsonObject key = json.getAsJsonObject("key");
                counts.forEach((c, n) -> inputs.add(new Input(items(key.get(String.valueOf(c)), mode), n)));
                outputs.add(new Output("item:" + resultId(json.get("result")), resultCount(json.get("result"))));
                return new Edge(name, inputs, List.of(), outputs, 0.0D);
            }
            case "minecraft:crafting_shapeless": {
                for (JsonElement ingredient : json.getAsJsonArray("ingredients")) {
                    inputs.add(new Input(items(ingredient, mode), 1.0D));
                }
                outputs.add(new Output("item:" + resultId(json.get("result")), resultCount(json.get("result"))));
                return new Edge(name, inputs, List.of(), outputs, 0.0D);
            }
            case "minecraft:smelting":
            case "minecraft:blasting":
                inputs.add(new Input(items(json.get("ingredient"), mode), 1.0D));
                outputs.add(new Output("item:" + resultId(json.get("result")), resultCount(json.get("result"))));
                return new Edge(name, inputs, List.of(), outputs, LEAF / 8.0D);
            case "nerotech:ore_processing":
            case "nerotech:fabricating":
            case "nerotech:advanced_fabricating":
            case "nerotech:chemical_processing":
                inputs.add(new Input(items(json.get("ingredient"), mode), 1.0D));
                if ("nerotech:chemical_processing".equals(type)) {
                    inputs.add(new Input(Set.of("gas:nerospace:oxygen"), CHEMICAL_OXYGEN));
                }
                outputs.add(new Output("item:" + resultId(json.get("result")), resultCount(json.get("result"))));
                return new Edge(name, inputs, List.of(), outputs, MACHINE_FE);
            case "nerotech:collider": {
                int minEnergy = json.has("min_energy") ? json.get("min_energy").getAsInt() : 0;
                inputs.add(new Input(items(json.get("input_a"), mode), 1.0D));
                inputs.add(new Input(items(json.get("input_b"), mode), 1.0D));
                outputs.add(new Output("item:" + resultId(json.get("result")), resultCount(json.get("result"))));
                double fe = COLLIDER_NE_PER_GUIDE * (1.0D + Math.ceil(Math.sqrt(minEnergy)));
                return new Edge(name, inputs, List.of(), outputs, fe);
            }
            default:
                return type.startsWith("nerotech:") ? processEdge(name, type, json, mode) : null;
        }
    }

    private static Edge processEdge(String name, String type, JsonObject json, Mode mode) {
        List<Input> inputs = new ArrayList<>();
        List<Set<String>> catalysts = new ArrayList<>();
        List<Output> outputs = new ArrayList<>();
        String atmosphere = json.has("atmosphere") ? json.get("atmosphere").getAsString() : null;
        if ("nerotech:condensing".equals(type) && atmosphere != null && mode == Mode.STANDALONE
                && !STANDALONE_ATMOSPHERES.contains(atmosphere.toUpperCase(Locale.ROOT))) {
            return null;
        }
        String gasInSuffix = "";
        String gasOutStage = null;
        if ("nerotech:centrifuging".equals(type) && atmosphere != null && atmosphere.startsWith("cascade_")) {
            int stage = Integer.parseInt(atmosphere.substring("cascade_".length()));
            gasInSuffix = stage > 1 ? "@cascade_" + stage : "";
            gasOutStage = "@cascade_" + (stage + 1);
        }
        if (json.has("items")) {
            for (JsonElement element : json.getAsJsonArray("items")) {
                JsonObject entry = element.getAsJsonObject();
                inputs.add(new Input(items(entry.get("ingredient"), mode), entry.has("count") ? entry.get("count").getAsInt() : 1));
            }
        }
        if (json.has("catalyst")) {
            catalysts.add(items(json.get("catalyst"), mode));
        }
        if (json.has("fluid_in")) {
            JsonObject fluid = json.getAsJsonObject("fluid_in");
            Set<String> nodes = new HashSet<>();
            for (String id : expand(fluid.get("id").getAsString(), "fluid")) {
                nodes.add("fluid:" + id);
            }
            inputs.add(new Input(nodes, fluid.get("amount").getAsInt()));
        }
        if (json.has("gas_in")) {
            JsonObject gas = json.getAsJsonObject("gas_in");
            inputs.add(new Input(Set.of("gas:" + gas.get("id").getAsString() + gasInSuffix), gas.get("amount").getAsInt()));
        }
        if (json.has("results")) {
            for (JsonElement element : json.getAsJsonArray("results")) {
                JsonObject result = element.getAsJsonObject();
                double count = result.has("count") ? result.get("count").getAsInt() : 1;
                double chance = result.has("chance_permille") ? result.get("chance_permille").getAsInt() : 1_000;
                outputs.add(new Output("item:" + result.get("item").getAsString(), count * chance / 1_000.0D));
            }
        }
        if (json.has("fluid_out")) {
            JsonObject fluid = json.getAsJsonObject("fluid_out");
            outputs.add(new Output("fluid:" + fluid.get("id").getAsString(), fluid.get("amount").getAsInt()));
        }
        if (json.has("gas_out")) {
            JsonObject gas = json.getAsJsonObject("gas_out");
            String id = gas.get("id").getAsString();
            outputs.add(new Output("gas:" + id, gas.get("amount").getAsInt()));
            if (gasOutStage != null) {
                outputs.add(new Output("gas:" + id + gasOutStage, gas.get("amount").getAsInt()));
            }
        }
        double energy = json.get("energy").getAsDouble() * json.get("time").getAsDouble();
        return new Edge(name, inputs, catalysts, outputs, energy);
    }

    /** The graph nodes an item ingredient can be satisfied by, in this mode. */
    private static Set<String> items(JsonElement ingredient, Mode mode) {
        Set<String> out = new HashSet<>();
        for (String ref : RecipeFixture.references(ingredient)) {
            if (ref.startsWith("#")) {
                out.addAll(itemTag(ref.substring(1), mode, false, new HashSet<>()));
            } else if (mode == Mode.OFFWORLD || !ref.startsWith("nerospace:")) {
                out.add("item:" + ref);
            }
        }
        return out;
    }

    private static Set<String> itemTag(String tag, Mode mode, boolean insideInputs, Set<String> seen) {
        Set<String> out = new HashSet<>();
        if (!seen.add(tag)) {
            return out;
        }
        if (CORE_TAGS.contains(tag)) {
            out.add("core:#" + tag);
        }
        List<String> convention = CONVENTION_TAGS.get(tag);
        if (convention != null) {
            convention.forEach(id -> out.add("item:" + id));
        }
        Optional<JsonObject> file = RecipeFixture.resource(tagPath(tag, "item"));
        boolean shippedHere = file.isPresent();
        if (mode == Mode.OFFWORLD && (NEROSPACE_TAGS.contains(tag)
                || (!shippedHere && insideInputs && convention == null && !CORE_TAGS.contains(tag)))) {
            out.add("mined:#" + tag);
        }
        if (!shippedHere) {
            return out;
        }
        boolean childInputs = insideInputs || tag.startsWith("nerotech:inputs/");
        for (JsonElement value : file.get().getAsJsonArray("values")) {
            String id = value.isJsonObject() ? value.getAsJsonObject().get("id").getAsString() : value.getAsString();
            if (id.startsWith("#")) {
                out.addAll(itemTag(id.substring(1), mode, childInputs, seen));
            } else if (mode == Mode.OFFWORLD || !id.startsWith("nerospace:")) {
                out.add("item:" + id);
            }
        }
        return out;
    }

    /** Ids an item or fluid reference resolves to through the shipped tag files (no mode, no leaves). */
    private static Set<String> expand(String ref, String registry) {
        Set<String> out = new HashSet<>();
        expandInto(ref, registry, out, new HashSet<>());
        return out;
    }

    private static void expandInto(String ref, String registry, Set<String> out, Set<String> seen) {
        if (!ref.startsWith("#")) {
            out.add(ref);
            return;
        }
        String tag = ref.substring(1);
        if (!seen.add(tag)) {
            return;
        }
        RecipeFixture.resource(tagPath(tag, registry)).ifPresent(file -> {
            for (JsonElement value : file.getAsJsonArray("values")) {
                expandInto(value.isJsonObject() ? value.getAsJsonObject().get("id").getAsString() : value.getAsString(),
                        registry, out, seen);
            }
        });
    }

    private static String tagPath(String tag, String registry) {
        int colon = tag.indexOf(':');
        return "data/" + tag.substring(0, colon) + "/tags/" + registry + "/" + tag.substring(colon + 1) + ".json";
    }

    // --- uses ------------------------------------------------------------------------------------

    /** Every item any NeroTech recipe consumes or holds as a catalyst (tags expanded through shipped files). */
    private static Set<String> recipeInputItems() {
        Set<String> out = new HashSet<>();
        for (JsonObject recipe : recipes().values()) {
            for (JsonElement ingredient : ingredientElements(recipe)) {
                for (String ref : RecipeFixture.references(ingredient)) {
                    out.addAll(expand(ref, "item"));
                }
            }
        }
        return out;
    }

    /** Every item in a {@code c:} or {@code nerotech:} item tag a sibling can name (not the input tags). */
    private static Set<String> siblingTaggedItems() {
        Set<String> out = new HashSet<>();
        for (String namespace : List.of("c", "nerotech")) {
            Path root = resourceRoot().resolve("data/" + namespace + "/tags/item");
            if (!Files.isDirectory(root)) {
                continue;
            }
            try (Stream<Path> walk = Files.walk(root)) {
                for (Path file : walk.filter(p -> p.toString().endsWith(".json")).toList()) {
                    String path = root.relativize(file).toString().replace('\\', '/');
                    path = path.substring(0, path.length() - ".json".length());
                    if (path.startsWith("inputs/")) {
                        continue;
                    }
                    out.addAll(expand("#" + namespace + ":" + path, "item"));
                }
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
        return out;
    }

    /** Every ingredient element of a recipe, in any of the shapes the shipped recipes use. */
    private static List<JsonElement> ingredientElements(JsonObject recipe) {
        List<JsonElement> out = new ArrayList<>();
        if (recipe.has("key")) {
            recipe.getAsJsonObject("key").entrySet().forEach(e -> out.add(e.getValue()));
        }
        if (recipe.has("ingredients")) {
            recipe.getAsJsonArray("ingredients").forEach(out::add);
        }
        if (recipe.has("items")) {
            recipe.getAsJsonArray("items").forEach(e -> out.add(e.getAsJsonObject().get("ingredient")));
        }
        for (String field : List.of("ingredient", "input_a", "input_b", "catalyst")) {
            if (recipe.has(field)) {
                out.add(recipe.get(field));
            }
        }
        return out;
    }

    // --- resources -------------------------------------------------------------------------------

    private static Map<String, JsonObject> recipesCache;

    /** Every NeroTech recipe JSON by file name (without {@code .json}), sorted. */
    static synchronized Map<String, JsonObject> recipes() {
        if (recipesCache == null) {
            Map<String, JsonObject> out = new LinkedHashMap<>();
            for (String name : names("data/nerotech/recipe")) {
                out.put(name, RecipeFixture.resource("data/nerotech/recipe/" + name + ".json").orElseThrow());
            }
            recipesCache = out;
        }
        return recipesCache;
    }

    /** NeroTech's item ids, one per client item definition (every registered item ships one). */
    static List<String> itemIds() {
        return names("assets/nerotech/items").stream().map(n -> "nerotech:" + n).toList();
    }

    private static Set<String> names(String dir) {
        Path path = resourceRoot().resolve(dir);
        Set<String> out = new TreeSet<>();
        try (Stream<Path> list = Files.list(path)) {
            list.map(p -> p.getFileName().toString()).filter(n -> n.endsWith(".json"))
                    .forEach(n -> out.add(n.substring(0, n.length() - ".json".length())));
        } catch (IOException e) {
            throw new IllegalStateException(path.toString(), e);
        }
        return out;
    }

    private static Path resourceRoot() {
        URL marker = RecipeGraphTest.class.getClassLoader().getResource("data/nerotech/tags/item/inputs/glacite.json");
        assumeTrue(marker != null && "file".equals(marker.getProtocol()), "resources not on the classpath as files");
        try {
            return Path.of(marker.toURI()).getParent().getParent().getParent().getParent().getParent().getParent();
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String resultId(JsonElement result) {
        if (result.isJsonPrimitive()) {
            return result.getAsString();
        }
        JsonObject object = result.getAsJsonObject();
        return object.has("id") ? object.get("id").getAsString() : object.get("item").getAsString();
    }

    private static double resultCount(JsonElement result) {
        return result.isJsonObject() && result.getAsJsonObject().has("count")
                ? result.getAsJsonObject().get("count").getAsInt() : 1.0D;
    }

    private static Map<String, String> rows() {
        Map<String, String> rows = new LinkedHashMap<>();
        rows.put("cindersteel_ingot", "item:nerotech:cindersteel_ingot");
        // Glacite itself is mined offworld (a leaf), so its row is measured at its two first products.
        rows.put("superconductor_wire", "item:nerotech:superconductor_wire");
        rows.put("glacite_focus_lens", "item:nerotech:glacite_focus_lens");
        rows.put("cryo_coolant", "fluid:nerotech:cryo_coolant");
        rows.put("crystal_boule", "item:nerotech:crystal_boule");
        rows.put("quartz_focus_lens", "item:nerotech:quartz_focus_lens");
        rows.put("ember_focus_lens", "item:nerotech:ember_focus_lens");
        rows.put("void_focus_lens", "item:nerotech:void_focus_lens");
        // The acid's two oxygen units dominate both routes, so the sulfur row is measured at the dust
        // (Cindara sulfur is a leaf offworld; gunpowder in the Centrifuge is the standalone route).
        rows.put("sulfur_dust", "item:nerotech:sulfur_dust");
        rows.put("polymer_resin", "fluid:nerotech:polymer_resin");
        rows.put("carbon_dioxide", "gas:nerotech:carbon_dioxide");
        rows.put("methane", "gas:nerotech:methane");
        rows.put("nitrogen", "gas:nerotech:nitrogen");
        rows.put("deuterium", "gas:nerotech:deuterium");
        rows.put("helium_3", "gas:nerotech:helium_3");
        rows.put("europium_dust", "item:nerotech:europium_dust");
        return rows;
    }
}
