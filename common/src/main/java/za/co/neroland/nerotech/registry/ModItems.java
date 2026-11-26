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
import za.co.neroland.nerotech.upgrade.NeroTechModule;
import za.co.neroland.nerotech.upgrade.UpgradeModuleItem;

/**
 * NeroTech's items: Tier-1 components, processing dusts, machine block-items, the Configurator wrench
 * and upgrade modules, registered cross-loader through the {@link RegistrationProvider} seam over the
 * vanilla item registry.
 *
 * <p>NeroTech does not re-register Core's shared materials (Nero Alloy, Starsteel, Void Crystal,
 * Plasma Glass) — those live in Core. Every item is listed in NeroTech's own creative tab
 * ({@link ModCreativeTab}); nothing is added to Core's tab.
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

    // --- Tier 2/3 (no gate: paced by Starsteel-tier recipes alone) ----------
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

    // --- Particle Accelerator (Stage B; free-form rework) --------------------
    /** Carries the tooltip naming the right-click bend cycle (26.x blocks expose no hover-text hook). */
    public static final RegistryEntry<BlockItem> ACCELERATOR_COIL_ITEM = ITEMS.register("accelerator_coil",
            key -> new TooltipBlockItem(ModBlocks.ACCELERATOR_COIL.get(), new Item.Properties().setId(key),
                    "block.nerotech.accelerator_coil.tooltip"));
    /** Carries the tooltip explaining how a free-form ring is built. */
    public static final RegistryEntry<BlockItem> COLLIDER_CORE_ITEM = ITEMS.register("collider_core",
            key -> new ColliderCoreBlockItem(ModBlocks.COLLIDER_CORE.get(), new Item.Properties().setId(key)));

    // --- Fluid & gas chain + coolant loop (Stage C) --------------------------
    public static final RegistryEntry<BlockItem> ELECTROLYZER_ITEM = blockItem("electrolyzer", ModBlocks.ELECTROLYZER);
    public static final RegistryEntry<BlockItem> GAS_TURBINE_ITEM = blockItem("gas_turbine", ModBlocks.GAS_TURBINE);
    /** Carries the tooltip naming the oxygen cost (26.x blocks expose no hover-text hook). */
    public static final RegistryEntry<BlockItem> CHEMICAL_PROCESSOR_ITEM = ITEMS.register("chemical_processor",
            key -> new TooltipBlockItem(ModBlocks.CHEMICAL_PROCESSOR.get(), new Item.Properties().setId(key),
                    "block.nerotech.chemical_processor.tooltip"));
    public static final RegistryEntry<BlockItem> RADIATOR_ITEM = ITEMS.register("radiator",
            key -> new TooltipBlockItem(ModBlocks.RADIATOR.get(), new Item.Properties().setId(key),
                    "block.nerotech.radiator.tooltip"));
    /** Carries the tooltip naming the radiator scaling (the pump has no GUI to explain itself). */
    public static final RegistryEntry<BlockItem> COOLANT_PUMP_ITEM = ITEMS.register("coolant_pump",
            key -> new TooltipBlockItem(ModBlocks.COOLANT_PUMP.get(), new Item.Properties().setId(key),
                    "block.nerotech.coolant_pump.tooltip"));

    // --- Automation handoff (Stage 5) ---------------------------------------
    public static final RegistryEntry<BlockItem> AUTO_CRAFTER_ITEM = blockItem("auto_crafter", ModBlocks.AUTO_CRAFTER);
    public static final RegistryEntry<BlockItem> ITEM_SORTER_ITEM = blockItem("item_sorter", ModBlocks.ITEM_SORTER);

    // --- Automation & QoL (Stage E) ------------------------------------------
    public static final RegistryEntry<BlockItem> CONVEYOR_BELT_ITEM = ITEMS.register("conveyor_belt",
            key -> new TooltipBlockItem(ModBlocks.CONVEYOR_BELT.get(), new Item.Properties().setId(key),
                    "block.nerotech.conveyor_belt.tooltip"));
    public static final RegistryEntry<BlockItem> ROBOTIC_ARM_ITEM = ITEMS.register("robotic_arm",
            key -> new TooltipBlockItem(ModBlocks.ROBOTIC_ARM.get(), new Item.Properties().setId(key),
                    "block.nerotech.robotic_arm.tooltip"));

    // --- Exotic endgame (Stage F) --------------------------------------------
    /** Tier-4 Fusion Reactor fuel; Collider-only, and only the 7³ shell will burn it. */
    public static final RegistryEntry<Item> ANTIMATTER_CELL = item("antimatter_cell", p -> p.stacksTo(16));
    /** Carries the vault tooltip, plus what a broken vault's dropped item still holds (blocks have no hover hook). */
    //? if >=1.20.5 {
    public static final RegistryEntry<BlockItem> SINGULARITY_VAULT_ITEM = ITEMS.register("singularity_vault",
            key -> new SingularityVaultBlockItem(ModBlocks.SINGULARITY_VAULT.get(), new Item.Properties().setId(key)));
    //?} else {
    /*public static final RegistryEntry<BlockItem> SINGULARITY_VAULT_ITEM = ITEMS.register("singularity_vault",
            key -> new SingularityVaultBlockItem(ModBlocks.SINGULARITY_VAULT.get(), new Item.Properties().stacksTo(1)));
    *///?}

    // --- Pollution mitigation (Stage F) --------------------------------------
    /** Consumable scrubbing medium; fouls into a {@link #DIRTY_FILTER} at capacity. */
    public static final RegistryEntry<Item> FILTER_CARTRIDGE = item("filter_cartridge");
    /** Spent cartridge — reprocessable in the Ore Processor for a partial iron refund. */
    public static final RegistryEntry<Item> DIRTY_FILTER = item("dirty_filter");
    public static final RegistryEntry<BlockItem> SCRUBBER_ITEM = blockItem("scrubber", ModBlocks.SCRUBBER);
    public static final RegistryEntry<BlockItem> REMEDIATOR_ITEM = blockItem("remediator", ModBlocks.REMEDIATOR);

    // --- Power tech (Stage D: the absorbed NeroPower feature set) --------------
    public static final RegistryEntry<BlockItem> WIND_TURBINE_ITEM = blockItem("wind_turbine", ModBlocks.WIND_TURBINE);
    public static final RegistryEntry<BlockItem> GEOTHERMAL_GENERATOR_ITEM =
            blockItem("geothermal_generator", ModBlocks.GEOTHERMAL_GENERATOR);
    public static final RegistryEntry<BlockItem> BIO_GENERATOR_ITEM = blockItem("bio_generator", ModBlocks.BIO_GENERATOR);
    public static final RegistryEntry<BlockItem> BATTERY_BANK_ITEM = blockItem("battery_bank", ModBlocks.BATTERY_BANK);
    public static final RegistryEntry<BlockItem> GRID_CONTROLLER_ITEM =
            blockItem("grid_controller", ModBlocks.GRID_CONTROLLER);
    public static final RegistryEntry<BlockItem> WIRELESS_NODE_ITEM = blockItem("wireless_node", ModBlocks.WIRELESS_NODE);

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
    // 0.8.0: NeroTech-classified modules (Core's UpgradeType is closed; see upgrade.NeroTechModule)
    public static final RegistryEntry<Item> HEAT_SINK_MODULE = ITEMS.register("heat_sink_module",
            key -> new UpgradeModuleItem(new Item.Properties().setId(key), NeroTechModule.HEAT_SINK));
    public static final RegistryEntry<Item> SCRUBBER_MODULE = ITEMS.register("scrubber_module",
            key -> new UpgradeModuleItem(new Item.Properties().setId(key), NeroTechModule.SCRUBBER));

    // --- 0.8.0 materials: plain items, consumed by tag in recipes -----------------
    // Structural
    public static final RegistryEntry<Item> CINDERSTEEL_INGOT = item("cindersteel_ingot");
    public static final RegistryEntry<Item> CINDERSTEEL_PLATE = item("cindersteel_plate");
    public static final RegistryEntry<Item> CINDERSTEEL_ROD = item("cindersteel_rod");
    /** Only ever an alternative to the cindersteel plate (no titanium ingot is registered). */
    public static final RegistryEntry<Item> TITANIUM_PLATE = item("titanium_plate");
    // Superconductor — standalone stand-in, tagged c:gems/glacite like Nerospace's glacite
    public static final RegistryEntry<Item> SYNTHETIC_GLACITE = item("synthetic_glacite");
    // Semiconductor
    public static final RegistryEntry<Item> CRYSTAL_SEED = item("crystal_seed");
    public static final RegistryEntry<Item> CRYSTAL_BOULE = item("crystal_boule");
    public static final RegistryEntry<Item> WAFER = item("wafer");
    // Chemistry — sulfur dust is a standalone stand-in, tagged c:dusts/sulfur
    public static final RegistryEntry<Item> SULFUR_DUST = item("sulfur_dust");
    public static final RegistryEntry<Item> POLYMER_SHEET = item("polymer_sheet");
    // Fusion fuel
    public static final RegistryEntry<Item> EUROPIUM_DUST = item("europium_dust");
    // Component ladder — processors sit a tier above circuit_board, never replace it
    public static final RegistryEntry<Item> LOGIC_CHIP = item("logic_chip");
    public static final RegistryEntry<Item> QUANTUM_CHIP = item("quantum_chip");
    public static final RegistryEntry<Item> COPPER_WIRE = item("copper_wire");
    public static final RegistryEntry<Item> CINDERSTEEL_WIRE = item("cindersteel_wire");
    public static final RegistryEntry<Item> SUPERCONDUCTOR_WIRE = item("superconductor_wire");
    public static final RegistryEntry<Item> COPPER_COIL = item("copper_coil");
    public static final RegistryEntry<Item> CINDERSTEEL_COIL = item("cindersteel_coil");
    public static final RegistryEntry<Item> SUPERCONDUCTOR_COIL = item("superconductor_coil");
    public static final RegistryEntry<Item> SERVO = item("servo");
    public static final RegistryEntry<Item> CAPACITOR = item("capacitor");
    public static final RegistryEntry<Item> HEAT_SINK = item("heat_sink");
    // Optics (Laser Etcher catalysts — never consumed)
    public static final RegistryEntry<Item> QUARTZ_FOCUS_LENS = item("quartz_focus_lens", p -> p.stacksTo(16));
    public static final RegistryEntry<Item> GLACITE_FOCUS_LENS = item("glacite_focus_lens", p -> p.stacksTo(16));
    public static final RegistryEntry<Item> EMBER_FOCUS_LENS = item("ember_focus_lens", p -> p.stacksTo(16));
    public static final RegistryEntry<Item> VOID_FOCUS_LENS = item("void_focus_lens", p -> p.stacksTo(16));
    // Drilling
    // One per stack: each head carries its own wear (machine.extraction.DrillHeadItem).
    public static final RegistryEntry<Item> IRON_DRILL_HEAD = drillHead("iron_drill_head");
    public static final RegistryEntry<Item> CINDERSTEEL_DRILL_HEAD = drillHead("cindersteel_drill_head");
    public static final RegistryEntry<Item> GLACITE_DRILL_HEAD = drillHead("glacite_drill_head");

    // --- 0.8.0 chemistry: Process Tank + liquid buckets -------------------------------------------
    public static final RegistryEntry<BlockItem> PROCESS_TANK_ITEM = ITEMS.register("process_tank",
            key -> new TooltipBlockItem(ModBlocks.PROCESS_TANK.get(), new Item.Properties().setId(key),
                    "block.nerotech.process_tank.tooltip"));
    public static final RegistryEntry<Item> CRYO_COOLANT_BUCKET =
            bucket(za.co.neroland.nerotech.fluid.NeroTechLiquid.CRYO_COOLANT);
    public static final RegistryEntry<Item> SULFURIC_ACID_BUCKET =
            bucket(za.co.neroland.nerotech.fluid.NeroTechLiquid.SULFURIC_ACID);
    public static final RegistryEntry<Item> POLYMER_RESIN_BUCKET =
            bucket(za.co.neroland.nerotech.fluid.NeroTechLiquid.POLYMER_RESIN);

    /** Every NeroTech item, in display order, for NeroTech's own creative tab. */
    private static List<RegistryEntry<? extends ItemLike>> creativeOrder() {
        return List.of(
                MACHINE_FRAME, CIRCUIT_BOARD, NERO_COIL,
                IRON_DUST, COPPER_DUST, GOLD_DUST,
                NERO_GENERATOR_ITEM, SOLAR_ARRAY_ITEM, ORE_PROCESSOR_ITEM, FABRICATOR_ITEM,
                FUSION_REACTOR_ITEM, FUSION_CASING_ITEM, FUSION_CONTAINMENT_GLASS_ITEM,
                ADVANCED_ORE_PROCESSOR_ITEM, ADVANCED_FABRICATOR_ITEM,
                FUSION_CELL, PLASMA_CELL, STELLAR_CELL, ANTIMATTER_CELL,
                COLLIDER_CORE_ITEM, ACCELERATOR_COIL_ITEM,
                ELECTROLYZER_ITEM, GAS_TURBINE_ITEM, CHEMICAL_PROCESSOR_ITEM,
                RADIATOR_ITEM, COOLANT_PUMP_ITEM,
                WIND_TURBINE_ITEM, GEOTHERMAL_GENERATOR_ITEM, BIO_GENERATOR_ITEM,
                BATTERY_BANK_ITEM, GRID_CONTROLLER_ITEM, WIRELESS_NODE_ITEM,
                AUTO_CRAFTER_ITEM, ITEM_SORTER_ITEM, CONVEYOR_BELT_ITEM, ROBOTIC_ARM_ITEM,
                SINGULARITY_VAULT_ITEM,
                SCRUBBER_ITEM, REMEDIATOR_ITEM, FILTER_CARTRIDGE, DIRTY_FILTER,
                ANALYTICS_TERMINAL_ITEM,
                TECH_GUIDE_ITEM, TECH_GUIDE_DATAPAD,
                CONFIGURATOR,
                SPEED_MODULE, EFFICIENCY_MODULE, CAPACITY_MODULE, RANGE_MODULE,
                HEAT_SINK_MODULE, SCRUBBER_MODULE,
                // 0.8.0 materials
                CINDERSTEEL_INGOT, CINDERSTEEL_PLATE, CINDERSTEEL_ROD, TITANIUM_PLATE,
                SYNTHETIC_GLACITE, CRYSTAL_SEED, CRYSTAL_BOULE, WAFER,
                SULFUR_DUST, POLYMER_SHEET, EUROPIUM_DUST,
                LOGIC_CHIP, QUANTUM_CHIP,
                COPPER_WIRE, CINDERSTEEL_WIRE, SUPERCONDUCTOR_WIRE,
                COPPER_COIL, CINDERSTEEL_COIL, SUPERCONDUCTOR_COIL,
                SERVO, CAPACITOR, HEAT_SINK,
                QUARTZ_FOCUS_LENS, GLACITE_FOCUS_LENS, EMBER_FOCUS_LENS, VOID_FOCUS_LENS,
                IRON_DRILL_HEAD, CINDERSTEEL_DRILL_HEAD, GLACITE_DRILL_HEAD,
                // 0.8.0 chemistry
                PROCESS_TANK_ITEM, CRYO_COOLANT_BUCKET, SULFURIC_ACID_BUCKET, POLYMER_RESIN_BUCKET);
    }

    private static RegistryEntry<Item> item(String name) {
        return ITEMS.register(name, key -> new Item(new Item.Properties().setId(key)));
    }

    private static RegistryEntry<Item> drillHead(String name) {
        return ITEMS.register(name, key -> new za.co.neroland.nerotech.machine.extraction.DrillHeadItem(
                new Item.Properties().setId(key).stacksTo(1)));
    }

    private static RegistryEntry<Item> item(String name, UnaryOperator<Item.Properties> props) {
        return ITEMS.register(name, key -> new Item(props.apply(new Item.Properties().setId(key))));
    }

    /** A vanilla bucket of a NeroTech liquid: places the liquid block, empties back to a bucket. */
    private static RegistryEntry<Item> bucket(za.co.neroland.nerotech.fluid.NeroTechLiquid liquid) {
        return ITEMS.register(liquid.bucketId(), key -> new net.minecraft.world.item.BucketItem(liquid.source(),
                new Item.Properties().setId(key).stacksTo(1)
                        .craftRemainder(net.minecraft.world.item.Items.BUCKET)));
    }

    private static RegistryEntry<Item> module(String name, UpgradeType type) {
        return ITEMS.register(name, key -> new UpgradeModuleItem(new Item.Properties().setId(key), type));
    }

    /**
     * Register a plain item (0.8.0 machine-family seam; see {@code registry.FamilyRegistry}). The
     * family's items land in {@link #ITEMS}, so loader flushing is unchanged.
     */
    public static RegistryEntry<Item> simpleItem(String name) {
        return item(name);
    }

    /** Register an item with adjusted properties (stack size, durability, ...). */
    public static RegistryEntry<Item> simpleItem(String name, UnaryOperator<Item.Properties> props) {
        return item(name, props);
    }

    /** Register a plain block item for a family block (same id as the block). */
    public static RegistryEntry<BlockItem> familyBlockItem(String name, RegistryEntry<? extends Block> block) {
        return blockItem(name, block);
    }

    /** Register a block item carrying one grey tooltip line ({@code tooltipKey}). */
    public static RegistryEntry<BlockItem> tooltipBlockItem(String name, RegistryEntry<? extends Block> block,
            String tooltipKey) {
        return ITEMS.register(name, key -> new TooltipBlockItem(block.get(), new Item.Properties().setId(key),
                tooltipKey));
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

        //? if >=1.21.5 {
        @Override
        public void appendHoverText(net.minecraft.world.item.ItemStack stack, Item.TooltipContext context,
                net.minecraft.world.item.component.TooltipDisplay display,
                java.util.function.Consumer<net.minecraft.network.chat.Component> tooltip,
                net.minecraft.world.item.TooltipFlag flag) {
            super.appendHoverText(stack, context, display, tooltip, flag);
        //?} else {
        /*@Override
        public void appendHoverText(net.minecraft.world.item.ItemStack stack, Item.TooltipContext context,
                java.util.List<net.minecraft.network.chat.Component> lines, net.minecraft.world.item.TooltipFlag flag) {
            super.appendHoverText(stack, context, lines, flag);
            java.util.function.Consumer<net.minecraft.network.chat.Component> tooltip = lines::add;
        *///?}
            tooltip.accept(net.minecraft.network.chat.Component
                    .translatable("block.nerotech.solar_array.tooltip")
                    .withStyle(net.minecraft.ChatFormatting.GRAY));
            tooltip.accept(net.minecraft.network.chat.Component
                    .translatable("block.nerotech.solar_array.tooltip.nerospace")
                    .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        }
    }

    /**
     * Accelerator Controller block-item with the one-line tooltip: how a free-form ring is laid out
     * and what the controller does with it, so the build is discoverable before the block is placed.
     */
    private static final class ColliderCoreBlockItem extends BlockItem {
        ColliderCoreBlockItem(Block block, Item.Properties properties) {
            super(block, properties);
        }

        //? if >=1.21.5 {
        @Override
        public void appendHoverText(net.minecraft.world.item.ItemStack stack, Item.TooltipContext context,
                net.minecraft.world.item.component.TooltipDisplay display,
                java.util.function.Consumer<net.minecraft.network.chat.Component> tooltip,
                net.minecraft.world.item.TooltipFlag flag) {
            super.appendHoverText(stack, context, display, tooltip, flag);
        //?} else {
        /*@Override
        public void appendHoverText(net.minecraft.world.item.ItemStack stack, Item.TooltipContext context,
                java.util.List<net.minecraft.network.chat.Component> lines, net.minecraft.world.item.TooltipFlag flag) {
            super.appendHoverText(stack, context, lines, flag);
            java.util.function.Consumer<net.minecraft.network.chat.Component> tooltip = lines::add;
        *///?}
            tooltip.accept(net.minecraft.network.chat.Component
                    .translatable("block.nerotech.collider_core.tooltip")
                    .withStyle(net.minecraft.ChatFormatting.GRAY));
        }
    }

    /**
     * A block-item carrying one extra grey tooltip line — the Stage C blocks whose whole point is a
     * cost or a scaling rule the GUI cannot show (26.x blocks expose no hover-text hook, so the
     * tooltip lives on the item; the Solar Array / Collider Core recipe, generalised).
     */
    private static final class TooltipBlockItem extends BlockItem {

        private final String tooltipKey;

        TooltipBlockItem(Block block, Item.Properties properties, String tooltipKey) {
            super(block, properties);
            this.tooltipKey = tooltipKey;
        }

        //? if >=1.21.5 {
        @Override
        public void appendHoverText(net.minecraft.world.item.ItemStack stack, Item.TooltipContext context,
                net.minecraft.world.item.component.TooltipDisplay display,
                java.util.function.Consumer<net.minecraft.network.chat.Component> tooltip,
                net.minecraft.world.item.TooltipFlag flag) {
            super.appendHoverText(stack, context, display, tooltip, flag);
        //?} else {
        /*@Override
        public void appendHoverText(net.minecraft.world.item.ItemStack stack, Item.TooltipContext context,
                java.util.List<net.minecraft.network.chat.Component> lines, net.minecraft.world.item.TooltipFlag flag) {
            super.appendHoverText(stack, context, lines, flag);
            java.util.function.Consumer<net.minecraft.network.chat.Component> tooltip = lines::add;
        *///?}
            tooltip.accept(net.minecraft.network.chat.Component.translatable(this.tooltipKey)
                    .withStyle(net.minecraft.ChatFormatting.GRAY));
        }
    }

    /**
     * Singularity Vault block-item: the vault tooltip, plus — on a vault dropped while holding a store
     * too large to spill — a line naming what it carries ({@code nerotech:vault_contents}).
     */
    private static final class SingularityVaultBlockItem extends BlockItem {
        SingularityVaultBlockItem(Block block, Item.Properties properties) {
            super(block, properties);
        }

        //? if >=1.21.5 {
        @Override
        public void appendHoverText(net.minecraft.world.item.ItemStack stack, Item.TooltipContext context,
                net.minecraft.world.item.component.TooltipDisplay display,
                java.util.function.Consumer<net.minecraft.network.chat.Component> tooltip,
                net.minecraft.world.item.TooltipFlag flag) {
            super.appendHoverText(stack, context, display, tooltip, flag);
        //?} else {
        /*@Override
        public void appendHoverText(net.minecraft.world.item.ItemStack stack, Item.TooltipContext context,
                java.util.List<net.minecraft.network.chat.Component> lines, net.minecraft.world.item.TooltipFlag flag) {
            super.appendHoverText(stack, context, lines, flag);
            java.util.function.Consumer<net.minecraft.network.chat.Component> tooltip = lines::add;
        *///?}
            tooltip.accept(net.minecraft.network.chat.Component
                    .translatable("block.nerotech.singularity_vault.tooltip")
                    .withStyle(net.minecraft.ChatFormatting.GRAY));
            za.co.neroland.nerotech.machine.VaultContents contents = stack.get(ModDataComponents.VAULT_CONTENTS.get());
            if (contents != null) {
                tooltip.accept(net.minecraft.network.chat.Component
                        .translatable("block.nerotech.singularity_vault.carrying",
                                contents.prototype().getHoverName(), contents.count())
                        .withStyle(net.minecraft.ChatFormatting.AQUA));
            }
        }
    }

    /** Every NeroTech item as {@link ItemLike}, in display order — drained into NeroTech's creative tab. */
    public static List<ItemLike> creativeContents() {
        List<ItemLike> out = new ArrayList<>();
        for (RegistryEntry<? extends ItemLike> entry : creativeOrder()) {
            out.add(entry.get());
        }
        // 0.8.0 machine families, in family order (FamilyRegistry), after NeroTech's own entries.
        out.addAll(FamilyRegistry.creativeContents());
        // 0.8.0 tools (item.tools), after the families.
        out.addAll(za.co.neroland.nerotech.item.tools.ToolsContent.creativeContents());
        return out;
    }

    private ModItems() {
    }

    /** Force class-load so the static registrations run (eager on Fabric). */
    public static void init() {
    }
}
