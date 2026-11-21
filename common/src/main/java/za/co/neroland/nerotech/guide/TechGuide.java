package za.co.neroland.nerotech.guide;

import java.util.List;
import java.util.function.Supplier;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

import za.co.neroland.nerotech.NeroTechCommon;
import za.co.neroland.nerotech.registry.ModBlocks;
import za.co.neroland.nerotech.registry.ModItems;

/**
 * The Tech Guide content table (chapters → steps, in code) — NeroTech's port of Nerospace's Star
 * Guide. Completion is advancement-driven: each step names the advancement that completes it, and the
 * menu packs per-chapter completion bitmasks from {@code ServerPlayer.getAdvancements()}. Icons are
 * suppliers because the table is built before registry objects exist.
 *
 * <p>The chapter arc mirrors NeroTech's journey: First Power → Processing → Consequences (heat +
 * pollution) → Mitigation (Scrubber/Remediator) → Automation → Analytics → Fusion → Exotic Matter.
 * 0.8.0 appends one chapter per material chain (Structural, Superconductor, Semiconductor, Optics,
 * Chemistry, Fusion fuel) plus Heat, Deposits, the gas-fed fusion tier and Tools; each step's text
 * names the next step. Every step maps
 * onto an advancement under {@code data/nerotech/advancement/} — the guide keeps NO completion state
 * of its own (see {@link TechGuideProgress}).</p>
 */
public final class TechGuide {

    /** One step of a chapter: icon + lang keys + the advancement that completes it. */
    public record Step(String id, Supplier<? extends ItemLike> icon, Identifier advancement) {

        public String titleKey() {
            return "gui.nerotech.tech_guide.step." + this.id;
        }

        public String textKey() {
            return "gui.nerotech.tech_guide.step." + this.id + ".text";
        }

        public ItemStack iconStack() {
            return new ItemStack(this.icon.get());
        }
    }

    /** A chapter: lang key + ordered steps (≤ 16 so the completion bitmask fits a data slot). */
    public record Chapter(String id, List<Step> steps) {

        public String titleKey() {
            return "gui.nerotech.tech_guide.chapter." + this.id;
        }
    }

    private static Identifier adv(String path) {
        return Identifier.fromNamespaceAndPath(NeroTechCommon.MOD_ID, path);
    }

    private static Step step(String id, Supplier<? extends ItemLike> icon, String advancementPath) {
        return new Step(id, icon, adv(advancementPath));
    }

    /** A 0.8.0 step: icon = the item with the same id, completed by the hidden {@code guide/<id>} advancement. */
    private static Step guide(String id) {
        return guide(id, id);
    }

    /** A 0.8.0 step whose icon item id differs from the step id (buckets for fluids). */
    private static Step guide(String id, String iconItem) {
        return new Step(id, item(iconItem), adv("guide/" + id));
    }

    /** A step completed by a milestone gate (see {@link #MILESTONE_STEPS}). */
    private static Step milestone(String gate, String iconItem) {
        return new Step(gate, item(iconItem), adv("guide/" + gate));
    }

    /**
     * Icon by registry id, resolved lazily (the 0.8.0 blocks and items live in per-family content
     * classes). Falls back to the datapad if the id is not registered.
     */
    private static Supplier<ItemLike> item(String path) {
        return () -> {
            Identifier id = Identifier.fromNamespaceAndPath(NeroTechCommon.MOD_ID, path);
            if (BuiltInRegistries.ITEM.containsKey(id)) {
                Item value = BuiltInRegistries.ITEM.getValue(id);
                if (value != Items.AIR) {
                    return value;
                }
            }
            return ModItems.TECH_GUIDE_DATAPAD.get();
        };
    }

    /** The chapters (order = chapter index used by menu completion bitmasks). */
    public static final List<Chapter> CHAPTERS = List.of(
            new Chapter("first_power", List.of(
                    step("machine_frame", () -> ModItems.MACHINE_FRAME.get(), "root"),
                    step("nero_generator", () -> ModBlocks.NERO_GENERATOR.get(), "nero_generator"),
                    step("solar_array", () -> ModBlocks.SOLAR_ARRAY.get(), "solar_array"),
                    step("clear_skies", () -> ModBlocks.SOLAR_ARRAY.get(), "clear_skies"))),
            new Chapter("processing", List.of(
                    step("ore_processor", () -> ModBlocks.ORE_PROCESSOR.get(), "ore_processor"),
                    step("fabricator", () -> ModBlocks.FABRICATOR.get(), "fabricator"),
                    step("advanced_ore_processor", () -> ModBlocks.ADVANCED_ORE_PROCESSOR.get(), "advanced_ore_processor"),
                    step("advanced_fabricator", () -> ModBlocks.ADVANCED_FABRICATOR.get(), "advanced_fabricator"),
                    step("particle_collider", () -> ModBlocks.COLLIDER_CORE.get(), "starsteel_dust"))),
            new Chapter("consequences", List.of(
                    step("heat_management", () -> Items.PACKED_ICE, "heat_management"),
                    step("dirty_filter", () -> ModItems.DIRTY_FILTER.get(), "dirty_filter"))),
            new Chapter("mitigation", List.of(
                    step("scrubber", () -> ModBlocks.SCRUBBER.get(), "scrubber"),
                    step("remediator", () -> ModBlocks.REMEDIATOR.get(), "remediator"))),
            new Chapter("automation", List.of(
                    step("auto_crafter", () -> ModBlocks.AUTO_CRAFTER.get(), "auto_crafter"),
                    step("item_sorter", () -> ModBlocks.ITEM_SORTER.get(), "item_sorter"),
                    step("configurator", () -> ModItems.CONFIGURATOR.get(), "configurator"))),
            new Chapter("analytics", List.of(
                    step("analytics_terminal", () -> ModBlocks.ANALYTICS_TERMINAL.get(), "analytics_terminal"))),
            new Chapter("fusion", List.of(
                    step("fusion_containment", () -> ModBlocks.FUSION_CASING.get(), "fusion_containment"),
                    step("fusion_reactor", () -> ModBlocks.FUSION_REACTOR.get(), "fusion_reactor"),
                    step("stellar_fuel", () -> ModItems.STELLAR_CELL.get(), "stellar_fuel"))),
            new Chapter("exotic_matter", List.of(
                    step("collider_ring", () -> ModBlocks.ACCELERATOR_COIL.get(), "starsteel_dust"),
                    step("antimatter", () -> ModItems.ANTIMATTER_CELL.get(), "antimatter_cell"),
                    step("singularity_vault", () -> ModBlocks.SINGULARITY_VAULT.get(), "singularity_vault"))),
            // --- 0.8.0 "Offworld Industry": appended so existing chapter indices (and the seen masks keyed
            // by them) stay put. Each step completes on a hidden guide/<id> advancement (inventory, or a
            // milestone gate the operator opened, see QuestHooks).
            new Chapter("structural", List.of(
                    guide("plasma_crucible"),
                    guide("cindersteel_ingot"),
                    guide("plate_press"),
                    guide("cindersteel_plate"),
                    guide("blast_foundry"),
                    guide("foundry_brick"),
                    guide("foundry_heat_port"),
                    milestone("first_foundry_pour", "blast_foundry"))),
            new Chapter("superconductor", List.of(
                    guide("filament_spooler"),
                    guide("cindersteel_wire"),
                    guide("cryo_coolant", "cryo_coolant_bucket"),
                    guide("synthetic_glacite"),
                    guide("superconductor_wire"),
                    guide("component_forge"),
                    guide("superconductor_coil"))),
            new Chapter("semiconductor", List.of(
                    guide("crystal_grower"),
                    guide("crystal_boule"),
                    guide("wafer"),
                    guide("laser_etcher"),
                    guide("logic_processor"),
                    milestone("first_quantum_processor", "quantum_processor"))),
            new Chapter("optics", List.of(
                    guide("quartz_focus_lens"),
                    guide("glacite_focus_lens"),
                    guide("ember_focus_lens"),
                    guide("void_focus_lens"))),
            new Chapter("chemistry", List.of(
                    guide("centrifuge"),
                    guide("cryo_distiller"),
                    guide("atmospheric_condenser"),
                    guide("reaction_vat"),
                    guide("process_tank"),
                    guide("sulfuric_acid", "sulfuric_acid_bucket"),
                    guide("polymer_sheet"))),
            new Chapter("fusion_fuel", List.of(
                    step("fuel_electrolysis", () -> ModBlocks.ELECTROLYZER.get(), "electrolyzer"),
                    new Step("centrifuge_cascade", item("centrifuge"), adv("guide/centrifuge")),
                    guide("europium_dust"))),
            new Chapter("heat", List.of(
                    guide("heat_pipe"),
                    guide("heat_exchanger"),
                    guide("heat_sink"),
                    guide("heat_sink_module"))),
            new Chapter("deposits", List.of(
                    guide("survey_scanner"),
                    guide("drill_derrick"),
                    guide("deep_core_drill"),
                    guide("iron_drill_head"),
                    guide("cindersteel_drill_head"),
                    guide("glacite_drill_head"),
                    milestone("first_deposit_tapped", "deep_core_drill"))),
            new Chapter("fusion_tier", List.of(
                    guide("fuel_injector"),
                    guide("europium_flux_lattice"),
                    milestone("gas_fed_ignition", "fuel_injector"))),
            new Chapter("tools", List.of(
                    guide("power_drill"),
                    guide("cutting_lance"),
                    guide("arc_wrench"),
                    guide("multimeter"),
                    guide("thermal_goggles"),
                    guide("filter_mask"),
                    guide("scrubber_module"),
                    guide("induction_charger"),
                    guide("annunciator_panel"))));

    /**
     * The milestone gates that complete a guide step: the gate id path is also the step id and the
     * {@code guide/<id>} advancement {@link za.co.neroland.nerotech.progression.QuestHooks} awards when
     * the gate opens for an online player.
     */
    public static final List<String> MILESTONE_STEPS = List.of(
            "first_foundry_pour", "first_quantum_processor", "first_deposit_tapped", "gas_fed_ignition");

    public static final int CHAPTER_COUNT = CHAPTERS.size();

    private TechGuide() {
    }

    /** Total step count across all chapters (sanity bound for menu button ids). */
    public static int totalSteps() {
        return CHAPTERS.stream().mapToInt(c -> c.steps().size()).sum();
    }
}
