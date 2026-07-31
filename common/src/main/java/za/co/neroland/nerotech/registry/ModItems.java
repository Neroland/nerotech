package za.co.neroland.nerotech.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;

import za.co.neroland.nerolandcore.upgrade.UpgradeType;

import za.co.neroland.nerotech.NeroTechCommon;
import za.co.neroland.nerotech.item.ConfiguratorItem;
import za.co.neroland.nerotech.item.TechGuideDatapadItem;
import za.co.neroland.nerotech.registry.RegistrationProvider.RegistryEntry;
import za.co.neroland.nerotech.upgrade.UpgradeModuleItem;

/**
 * NeroTech's items: Tier-1 components, processing dusts, machine block-items, the Configurator wrench
 * and upgrade modules, registered cross-loader through the {@link RegistrationProvider} seam over the
 * vanilla item registry.
 *
 * <p>NeroTech does not re-register Core's shared materials (Nero Alloy, Starsteel, Void Crystal,
 * Plasma Glass) — those live in Core. Every item is appended to Core's shared "Neroland" creative tab
 * via {@link CoreCreativeTab#add}.
 *
 * <p>Tagging (hand-authored, no datagen): dusts carry {@code c:dusts/<metal>}; components carry the
 * internal {@code nerotech:machine_components} tag; modules are classified by item, not tag.
 */
public final class ModItems {

    public static final RegistrationProvider<Item> ITEMS =
            RegistrationProvider.get(Registries.ITEM, NeroTechCommon.MOD_ID);

    // --- Machine crafting components ----------------------------------------
    public static final RegistryEntry<Item> MACHINE_FRAME = item("machine_frame");
    public static final RegistryEntry<Item> CIRCUIT_BOARD = item("circuit_board");
    public static final RegistryEntry<Item> NERO_COIL = item("nero_coil");

    // --- Earth-metal processing dusts (ore processor output; c:dusts/<m>) ----
    public static final RegistryEntry<Item> IRON_DUST = item("iron_dust");
    public static final RegistryEntry<Item> COPPER_DUST = item("copper_dust");
    public static final RegistryEntry<Item> GOLD_DUST = item("gold_dust");

    // --- Machine block-items (Tier-1 Earth machines) ------------------------
    public static final RegistryEntry<BlockItem> NERO_GENERATOR_ITEM = blockItem("nero_generator", ModBlocks.NERO_GENERATOR);
    /** Carries the niche tooltip distinguishing it from Nerospace's tiered Solar Panels. */
    public static final RegistryEntry<BlockItem> SOLAR_ARRAY_ITEM = ITEMS.register("solar_array",
            key -> new SolarArrayBlockItem(ModBlocks.SOLAR_ARRAY.get(), new Item.Properties().setId(key)));
    public static final RegistryEntry<BlockItem> ORE_PROCESSOR_ITEM = blockItem("ore_processor", ModBlocks.ORE_PROCESSOR);
    public static final RegistryEntry<BlockItem> FABRICATOR_ITEM = blockItem("fabricator", ModBlocks.FABRICATOR);

    // --- Tier 2/3 (gated behind orbit + Starsteel) --------------------------
    /** Fusion Reactor fuel; tagged into {@code nerotech:fusion_fuels}. */
    public static final RegistryEntry<Item> FUSION_CELL = item("fusion_cell");
    public static final RegistryEntry<BlockItem> FUSION_REACTOR_ITEM = blockItem("fusion_reactor", ModBlocks.FUSION_REACTOR);
    public static final RegistryEntry<BlockItem> ADVANCED_ORE_PROCESSOR_ITEM = blockItem("advanced_ore_processor", ModBlocks.ADVANCED_ORE_PROCESSOR);
    public static final RegistryEntry<BlockItem> ADVANCED_FABRICATOR_ITEM = blockItem("advanced_fabricator", ModBlocks.ADVANCED_FABRICATOR);

    // --- Fusion Reactor multiblock structure (Stage E) -----------------------
    public static final RegistryEntry<BlockItem> FUSION_CASING_ITEM = blockItem("fusion_casing", ModBlocks.FUSION_CASING);
    public static final RegistryEntry<BlockItem> FUSION_CONTAINMENT_GLASS_ITEM = blockItem("fusion_containment_glass", ModBlocks.FUSION_CONTAINMENT_GLASS);
    /** Tier-2 Fusion Reactor fuel; tagged into {@code nerotech:fusion_fuel/tier2} (+ {@code fusion_fuels}). */
    public static final RegistryEntry<Item> PLASMA_CELL = item("plasma_cell", p -> p.stacksTo(16));
    /** Tier-3 Fusion Reactor fuel; tagged into {@code nerotech:fusion_fuel/tier3} (+ {@code fusion_fuels}). */
    public static final RegistryEntry<Item> STELLAR_CELL = item("stellar_cell", p -> p.stacksTo(16));

    // --- Automation handoff (Stage 5) ---------------------------------------
    public static final RegistryEntry<BlockItem> AUTO_CRAFTER_ITEM = blockItem("auto_crafter", ModBlocks.AUTO_CRAFTER);
    public static final RegistryEntry<BlockItem> ITEM_SORTER_ITEM = blockItem("item_sorter", ModBlocks.ITEM_SORTER);

    // --- Pollution mitigation (Stage F) --------------------------------------
    /** Consumable scrubbing medium; fouls into a {@link #DIRTY_FILTER} at capacity. */
    public static final RegistryEntry<Item> FILTER_CARTRIDGE = item("filter_cartridge");
    /** Spent cartridge — reprocessable in the Ore Processor for a partial iron refund. */
    public static final RegistryEntry<Item> DIRTY_FILTER = item("dirty_filter");
    public static final RegistryEntry<BlockItem> SCRUBBER_ITEM = blockItem("scrubber", ModBlocks.SCRUBBER);
    public static final RegistryEntry<BlockItem> REMEDIATOR_ITEM = blockItem("remediator", ModBlocks.REMEDIATOR);

    // --- Production analytics (Stage G) ---------------------------------------
    public static final RegistryEntry<BlockItem> ANALYTICS_TERMINAL_ITEM =
            blockItem("analytics_terminal", ModBlocks.ANALYTICS_TERMINAL);

    // --- Tech Guide (Nerospace Star Guide recipe) ------------------------------
    public static final RegistryEntry<BlockItem> TECH_GUIDE_ITEM = blockItem("tech_guide", ModBlocks.TECH_GUIDE);
    /** The pedestal's key + a handheld copy of the guide (opens the same progress-backed menu). */
    public static final RegistryEntry<Item> TECH_GUIDE_DATAPAD = ITEMS.register("tech_guide_datapad",
            key -> new TechGuideDatapadItem(new Item.Properties().stacksTo(1).setId(key)));

    // --- Tools ---------------------------------------------------------------
    /**
     * The side-config wrench (Core ships the API, NeroTech ships the item — see
     * {@link za.co.neroland.nerotech.item.ConfiguratorItem}). Single-stack tool.
     */
    public static final RegistryEntry<Item> CONFIGURATOR = ITEMS.register("configurator",
            key -> new ConfiguratorItem(new Item.Properties().setId(key).stacksTo(1)));

    // --- Upgrade modules (Core UpgradeType; classified by item) -------------
    public static final RegistryEntry<Item> SPEED_MODULE = module("speed_module", UpgradeType.SPEED);
    public static final RegistryEntry<Item> EFFICIENCY_MODULE = module("efficiency_module", UpgradeType.EFFICIENCY);
    public static final RegistryEntry<Item> CAPACITY_MODULE = module("capacity_module", UpgradeType.CAPACITY);
    public static final RegistryEntry<Item> RANGE_MODULE = module("range_module", UpgradeType.RANGE);

    /** Every NeroTech item, in display order, for NeroTech's own creative tab. */
    private static List<RegistryEntry<? extends ItemLike>> creativeOrder() {
        return List.of(
                MACHINE_FRAME, CIRCUIT_BOARD, NERO_COIL,
                IRON_DUST, COPPER_DUST, GOLD_DUST,
                NERO_GENERATOR_ITEM, SOLAR_ARRAY_ITEM, ORE_PROCESSOR_ITEM, FABRICATOR_ITEM,
                FUSION_REACTOR_ITEM, FUSION_CASING_ITEM, FUSION_CONTAINMENT_GLASS_ITEM,
                ADVANCED_ORE_PROCESSOR_ITEM, ADVANCED_FABRICATOR_ITEM,
                FUSION_CELL, PLASMA_CELL, STELLAR_CELL,
                AUTO_CRAFTER_ITEM, ITEM_SORTER_ITEM,
                SCRUBBER_ITEM, REMEDIATOR_ITEM, FILTER_CARTRIDGE, DIRTY_FILTER,
                ANALYTICS_TERMINAL_ITEM,
                TECH_GUIDE_ITEM, TECH_GUIDE_DATAPAD,
                CONFIGURATOR,
                SPEED_MODULE, EFFICIENCY_MODULE, CAPACITY_MODULE, RANGE_MODULE);
    }

    private static RegistryEntry<Item> item(String name) {
        return ITEMS.register(name, key -> new Item(new Item.Properties().setId(key)));
    }

    private static RegistryEntry<Item> item(String name, UnaryOperator<Item.Properties> props) {
        return ITEMS.register(name, key -> new Item(props.apply(new Item.Properties().setId(key))));
    }

    private static RegistryEntry<Item> module(String name, UpgradeType type) {
        return ITEMS.register(name, key -> new UpgradeModuleItem(new Item.Properties().setId(key), type));
    }

    private static RegistryEntry<BlockItem> blockItem(String name, RegistryEntry<? extends Block> block) {
        return ITEMS.register(name, key -> new BlockItem(block.get(), new Item.Properties().setId(key)));
    }

    /**
     * Solar Array block-item with the niche tooltip: NeroTech's basic single-block panel vs
     * Nerospace's tiered, poolable Solar Panels (26.x blocks expose no hover-text hook, so the
     * tooltip lives on the item).
     */
    private static final class SolarArrayBlockItem extends BlockItem {
        SolarArrayBlockItem(Block block, Item.Properties properties) {
            super(block, properties);
        }

        @Override
        public void appendHoverText(net.minecraft.world.item.ItemStack stack, Item.TooltipContext context,
                net.minecraft.world.item.component.TooltipDisplay display,
                java.util.function.Consumer<net.minecraft.network.chat.Component> tooltip,
                net.minecraft.world.item.TooltipFlag flag) {
            super.appendHoverText(stack, context, display, tooltip, flag);
            tooltip.accept(net.minecraft.network.chat.Component
                    .translatable("block.nerotech.solar_array.tooltip")
                    .withStyle(net.minecraft.ChatFormatting.GRAY));
            tooltip.accept(net.minecraft.network.chat.Component
                    .translatable("block.nerotech.solar_array.tooltip.nerospace")
                    .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        }
    }

    /** Every NeroTech item as {@link ItemLike}, in display order — drained into NeroTech's creative tab. */
    public static List<ItemLike> creativeContents() {
        List<ItemLike> out = new ArrayList<>();
        for (RegistryEntry<? extends ItemLike> entry : creativeOrder()) {
            out.add(entry.get());
        }
        return out;
    }

    private ModItems() {
    }

    /** Force class-load so the static registrations run (eager on Fabric). */
    public static void init() {
    }
}
