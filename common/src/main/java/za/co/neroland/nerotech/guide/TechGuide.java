package za.co.neroland.nerotech.guide;

import java.util.List;
import java.util.function.Supplier;

import net.minecraft.resources.Identifier;
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
 * pollution) → Mitigation (Scrubber/Remediator) → Automation → Analytics → Fusion. Every step maps
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
                    step("advanced_fabricator", () -> ModBlocks.ADVANCED_FABRICATOR.get(), "advanced_fabricator"))),
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
                    step("stellar_fuel", () -> ModItems.STELLAR_CELL.get(), "stellar_fuel"))));

    public static final int CHAPTER_COUNT = CHAPTERS.size();

    private TechGuide() {
    }

    /** Total step count across all chapters (sanity bound for menu button ids). */
    public static int totalSteps() {
        return CHAPTERS.stream().mapToInt(c -> c.steps().size()).sum();
    }
}
