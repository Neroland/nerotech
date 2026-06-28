package za.co.neroland.nerotech.registry;

import java.util.List;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;

import za.co.neroland.nerolandcore.registry.CoreCreativeTab;
import za.co.neroland.nerolandcore.upgrade.UpgradeType;

import za.co.neroland.nerotech.NeroTechCommon;
import za.co.neroland.nerotech.registry.RegistrationProvider.RegistryEntry;
import za.co.neroland.nerotech.upgrade.UpgradeModuleItem;

/**
 * NeroTech's items: Tier-1 components, processing dusts, machine block-items and upgrade modules,
 * registered cross-loader through the {@link RegistrationProvider} seam over the vanilla item registry.
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
    public static final RegistryEntry<BlockItem> SOLAR_ARRAY_ITEM = blockItem("solar_array", ModBlocks.SOLAR_ARRAY);
    public static final RegistryEntry<BlockItem> ORE_PROCESSOR_ITEM = blockItem("ore_processor", ModBlocks.ORE_PROCESSOR);
    public static final RegistryEntry<BlockItem> FABRICATOR_ITEM = blockItem("fabricator", ModBlocks.FABRICATOR);

    // --- Tier 2/3 (gated behind orbit + Starsteel) --------------------------
    /** Fusion Reactor fuel; tagged into {@code nerotech:fusion_fuels}. */
    public static final RegistryEntry<Item> FUSION_CELL = item("fusion_cell");
    public static final RegistryEntry<BlockItem> FUSION_REACTOR_ITEM = blockItem("fusion_reactor", ModBlocks.FUSION_REACTOR);
    public static final RegistryEntry<BlockItem> ADVANCED_ORE_PROCESSOR_ITEM = blockItem("advanced_ore_processor", ModBlocks.ADVANCED_ORE_PROCESSOR);
    public static final RegistryEntry<BlockItem> ADVANCED_FABRICATOR_ITEM = blockItem("advanced_fabricator", ModBlocks.ADVANCED_FABRICATOR);

    // --- Upgrade modules (Core UpgradeType; classified by item) -------------
    public static final RegistryEntry<Item> SPEED_MODULE = module("speed_module", UpgradeType.SPEED);
    public static final RegistryEntry<Item> EFFICIENCY_MODULE = module("efficiency_module", UpgradeType.EFFICIENCY);
    public static final RegistryEntry<Item> CAPACITY_MODULE = module("capacity_module", UpgradeType.CAPACITY);
    public static final RegistryEntry<Item> RANGE_MODULE = module("range_module", UpgradeType.RANGE);

    /** Every NeroTech item, in display order, for the shared {@link CoreCreativeTab}. */
    private static List<RegistryEntry<? extends ItemLike>> creativeOrder() {
        return List.of(
                MACHINE_FRAME, CIRCUIT_BOARD, NERO_COIL,
                IRON_DUST, COPPER_DUST, GOLD_DUST,
                NERO_GENERATOR_ITEM, SOLAR_ARRAY_ITEM, ORE_PROCESSOR_ITEM, FABRICATOR_ITEM,
                FUSION_REACTOR_ITEM, ADVANCED_ORE_PROCESSOR_ITEM, ADVANCED_FABRICATOR_ITEM, FUSION_CELL,
                SPEED_MODULE, EFFICIENCY_MODULE, CAPACITY_MODULE, RANGE_MODULE);
    }

    private static RegistryEntry<Item> item(String name) {
        return ITEMS.register(name, key -> new Item(new Item.Properties().setId(key)));
    }

    private static RegistryEntry<Item> module(String name, UpgradeType type) {
        return ITEMS.register(name, key -> new UpgradeModuleItem(new Item.Properties().setId(key), type));
    }

    private static RegistryEntry<BlockItem> blockItem(String name, RegistryEntry<? extends Block> block) {
        return ITEMS.register(name, key -> new BlockItem(block.get(), new Item.Properties().setId(key)));
    }

    /** Append every NeroTech item to Core's shared Neroland creative tab. */
    public static void addToCreativeTab() {
        creativeOrder().forEach(entry -> CoreCreativeTab.add(entry::get));
    }

    private ModItems() {
    }

    /** Force class-load so the static registrations run (eager on Fabric). */
    public static void init() {
    }
}
