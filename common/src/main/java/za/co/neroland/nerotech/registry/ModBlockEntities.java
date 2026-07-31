package za.co.neroland.nerotech.registry;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;

import za.co.neroland.nerotech.NeroTechCommon;
import za.co.neroland.nerotech.guide.TechGuideBlockEntity;
import za.co.neroland.nerotech.machine.NeroTechMachineBlockEntity;
import za.co.neroland.nerotech.machine.AdvancedFabricatorBlockEntity;
import za.co.neroland.nerotech.machine.AdvancedOreProcessorBlockEntity;
import za.co.neroland.nerotech.machine.AnalyticsTerminalBlockEntity;
import za.co.neroland.nerotech.machine.AutoCrafterBlockEntity;
import za.co.neroland.nerotech.machine.BatteryBankBlockEntity;
import za.co.neroland.nerotech.machine.BioGeneratorBlockEntity;
import za.co.neroland.nerotech.machine.ChemicalProcessorBlockEntity;
import za.co.neroland.nerotech.machine.ColliderCoreBlockEntity;
import za.co.neroland.nerotech.machine.CoolantPumpBlockEntity;
import za.co.neroland.nerotech.machine.ElectrolyzerBlockEntity;
import za.co.neroland.nerotech.machine.FabricatorBlockEntity;
import za.co.neroland.nerotech.machine.GasTurbineBlockEntity;
import za.co.neroland.nerotech.machine.GeothermalGeneratorBlockEntity;
import za.co.neroland.nerotech.machine.GridControllerBlockEntity;
import za.co.neroland.nerotech.machine.FusionReactorBlockEntity;
import za.co.neroland.nerotech.machine.ItemSorterBlockEntity;
import za.co.neroland.nerotech.machine.NeroGeneratorBlockEntity;
import za.co.neroland.nerotech.machine.OreProcessorBlockEntity;
import za.co.neroland.nerotech.machine.RemediatorBlockEntity;
import za.co.neroland.nerotech.machine.RoboticArmBlockEntity;
import za.co.neroland.nerotech.machine.ScrubberBlockEntity;
import za.co.neroland.nerotech.machine.SingularityVaultBlockEntity;
import za.co.neroland.nerotech.machine.SolarArrayBlockEntity;
import za.co.neroland.nerotech.machine.WindTurbineBlockEntity;
import za.co.neroland.nerotech.machine.WirelessNodeBlockEntity;
import za.co.neroland.nerotech.registry.RegistrationProvider.RegistryEntry;

/** Block-entity types for NeroTech's Tier-1 machines, registered cross-loader via {@link RegistrationProvider}. */
public final class ModBlockEntities {

    public static final RegistrationProvider<BlockEntityType<?>> BLOCK_ENTITIES =
            RegistrationProvider.get(Registries.BLOCK_ENTITY_TYPE, NeroTechCommon.MOD_ID);

    public static final RegistryEntry<BlockEntityType<NeroGeneratorBlockEntity>> NERO_GENERATOR =
            BLOCK_ENTITIES.register("nero_generator",
                    key -> new BlockEntityType<>(NeroGeneratorBlockEntity::new, Set.of(ModBlocks.NERO_GENERATOR.get())));

    public static final RegistryEntry<BlockEntityType<SolarArrayBlockEntity>> SOLAR_ARRAY =
            BLOCK_ENTITIES.register("solar_array",
                    key -> new BlockEntityType<>(SolarArrayBlockEntity::new, Set.of(ModBlocks.SOLAR_ARRAY.get())));

    public static final RegistryEntry<BlockEntityType<OreProcessorBlockEntity>> ORE_PROCESSOR =
            BLOCK_ENTITIES.register("ore_processor",
                    key -> new BlockEntityType<>(OreProcessorBlockEntity::new, Set.of(ModBlocks.ORE_PROCESSOR.get())));

    public static final RegistryEntry<BlockEntityType<FabricatorBlockEntity>> FABRICATOR =
            BLOCK_ENTITIES.register("fabricator",
                    key -> new BlockEntityType<>(FabricatorBlockEntity::new, Set.of(ModBlocks.FABRICATOR.get())));

    public static final RegistryEntry<BlockEntityType<FusionReactorBlockEntity>> FUSION_REACTOR =
            BLOCK_ENTITIES.register("fusion_reactor",
                    key -> new BlockEntityType<>(FusionReactorBlockEntity::new, Set.of(ModBlocks.FUSION_REACTOR.get())));

    public static final RegistryEntry<BlockEntityType<AdvancedOreProcessorBlockEntity>> ADVANCED_ORE_PROCESSOR =
            BLOCK_ENTITIES.register("advanced_ore_processor",
                    key -> new BlockEntityType<>(AdvancedOreProcessorBlockEntity::new, Set.of(ModBlocks.ADVANCED_ORE_PROCESSOR.get())));

    public static final RegistryEntry<BlockEntityType<AdvancedFabricatorBlockEntity>> ADVANCED_FABRICATOR =
            BLOCK_ENTITIES.register("advanced_fabricator",
                    key -> new BlockEntityType<>(AdvancedFabricatorBlockEntity::new, Set.of(ModBlocks.ADVANCED_FABRICATOR.get())));

    public static final RegistryEntry<BlockEntityType<ColliderCoreBlockEntity>> COLLIDER_CORE =
            BLOCK_ENTITIES.register("collider_core",
                    key -> new BlockEntityType<>(ColliderCoreBlockEntity::new, Set.of(ModBlocks.COLLIDER_CORE.get())));

    // --- Fluid & gas chain + coolant loop (Stage C) --------------------------
    public static final RegistryEntry<BlockEntityType<ElectrolyzerBlockEntity>> ELECTROLYZER =
            BLOCK_ENTITIES.register("electrolyzer",
                    key -> new BlockEntityType<>(ElectrolyzerBlockEntity::new, Set.of(ModBlocks.ELECTROLYZER.get())));

    public static final RegistryEntry<BlockEntityType<GasTurbineBlockEntity>> GAS_TURBINE =
            BLOCK_ENTITIES.register("gas_turbine",
                    key -> new BlockEntityType<>(GasTurbineBlockEntity::new, Set.of(ModBlocks.GAS_TURBINE.get())));

    public static final RegistryEntry<BlockEntityType<ChemicalProcessorBlockEntity>> CHEMICAL_PROCESSOR =
            BLOCK_ENTITIES.register("chemical_processor",
                    key -> new BlockEntityType<>(ChemicalProcessorBlockEntity::new,
                            Set.of(ModBlocks.CHEMICAL_PROCESSOR.get())));

    public static final RegistryEntry<BlockEntityType<CoolantPumpBlockEntity>> COOLANT_PUMP =
            BLOCK_ENTITIES.register("coolant_pump",
                    key -> new BlockEntityType<>(CoolantPumpBlockEntity::new, Set.of(ModBlocks.COOLANT_PUMP.get())));

    // --- Power tech (Stage D: the absorbed NeroPower feature set) -------------
    public static final RegistryEntry<BlockEntityType<WindTurbineBlockEntity>> WIND_TURBINE =
            BLOCK_ENTITIES.register("wind_turbine",
                    key -> new BlockEntityType<>(WindTurbineBlockEntity::new, Set.of(ModBlocks.WIND_TURBINE.get())));

    public static final RegistryEntry<BlockEntityType<GeothermalGeneratorBlockEntity>> GEOTHERMAL_GENERATOR =
            BLOCK_ENTITIES.register("geothermal_generator",
                    key -> new BlockEntityType<>(GeothermalGeneratorBlockEntity::new,
                            Set.of(ModBlocks.GEOTHERMAL_GENERATOR.get())));

    public static final RegistryEntry<BlockEntityType<BioGeneratorBlockEntity>> BIO_GENERATOR =
            BLOCK_ENTITIES.register("bio_generator",
                    key -> new BlockEntityType<>(BioGeneratorBlockEntity::new, Set.of(ModBlocks.BIO_GENERATOR.get())));

    public static final RegistryEntry<BlockEntityType<BatteryBankBlockEntity>> BATTERY_BANK =
            BLOCK_ENTITIES.register("battery_bank",
                    key -> new BlockEntityType<>(BatteryBankBlockEntity::new, Set.of(ModBlocks.BATTERY_BANK.get())));

    public static final RegistryEntry<BlockEntityType<GridControllerBlockEntity>> GRID_CONTROLLER =
            BLOCK_ENTITIES.register("grid_controller",
                    key -> new BlockEntityType<>(GridControllerBlockEntity::new,
                            Set.of(ModBlocks.GRID_CONTROLLER.get())));

    public static final RegistryEntry<BlockEntityType<WirelessNodeBlockEntity>> WIRELESS_NODE =
            BLOCK_ENTITIES.register("wireless_node",
                    key -> new BlockEntityType<>(WirelessNodeBlockEntity::new, Set.of(ModBlocks.WIRELESS_NODE.get())));

    public static final RegistryEntry<BlockEntityType<AutoCrafterBlockEntity>> AUTO_CRAFTER =
            BLOCK_ENTITIES.register("auto_crafter",
                    key -> new BlockEntityType<>(AutoCrafterBlockEntity::new, Set.of(ModBlocks.AUTO_CRAFTER.get())));

    public static final RegistryEntry<BlockEntityType<ItemSorterBlockEntity>> ITEM_SORTER =
            BLOCK_ENTITIES.register("item_sorter",
                    key -> new BlockEntityType<>(ItemSorterBlockEntity::new, Set.of(ModBlocks.ITEM_SORTER.get())));

    // --- Automation & QoL (Stage E) / exotic endgame (Stage F) -----------------
    public static final RegistryEntry<BlockEntityType<RoboticArmBlockEntity>> ROBOTIC_ARM =
            BLOCK_ENTITIES.register("robotic_arm",
                    key -> new BlockEntityType<>(RoboticArmBlockEntity::new, Set.of(ModBlocks.ROBOTIC_ARM.get())));

    public static final RegistryEntry<BlockEntityType<SingularityVaultBlockEntity>> SINGULARITY_VAULT =
            BLOCK_ENTITIES.register("singularity_vault",
                    key -> new BlockEntityType<>(SingularityVaultBlockEntity::new,
                            Set.of(ModBlocks.SINGULARITY_VAULT.get())));

    public static final RegistryEntry<BlockEntityType<ScrubberBlockEntity>> SCRUBBER =
            BLOCK_ENTITIES.register("scrubber",
                    key -> new BlockEntityType<>(ScrubberBlockEntity::new, Set.of(ModBlocks.SCRUBBER.get())));

    public static final RegistryEntry<BlockEntityType<RemediatorBlockEntity>> REMEDIATOR =
            BLOCK_ENTITIES.register("remediator",
                    key -> new BlockEntityType<>(RemediatorBlockEntity::new, Set.of(ModBlocks.REMEDIATOR.get())));

    public static final RegistryEntry<BlockEntityType<AnalyticsTerminalBlockEntity>> ANALYTICS_TERMINAL =
            BLOCK_ENTITIES.register("analytics_terminal",
                    key -> new BlockEntityType<>(AnalyticsTerminalBlockEntity::new, Set.of(ModBlocks.ANALYTICS_TERMINAL.get())));

    public static final RegistryEntry<BlockEntityType<TechGuideBlockEntity>> TECH_GUIDE =
            BLOCK_ENTITIES.register("tech_guide",
                    key -> new BlockEntityType<>(TechGuideBlockEntity::new, Set.of(ModBlocks.TECH_GUIDE.get())));

    /**
     * The single source of truth for which machines get an energy capability/lookup registration on
     * every loader. Each loader entry point ({@code NeroTechFabric}, {@code NeroTechNeoForge}) iterates
     * this list instead of hand-listing types, so a new machine is wired everywhere at once.
     *
     * <p><b>Adding a machine?</b> Register its {@link BlockEntityType} above and add it here — that is
     * all the cross-loader wiring it needs. Two block entities are excluded <i>by design</i>: the
     * Analytics Terminal (zero NE, no slots) and the Tech Guide pedestal (no NE, no slots); exposing an
     * empty buffer on them would advertise a power surface that does not exist. The Stage-D Grid
     * Controller joins them for the same reason: it is a passive supervisor that consumes no NE.
     */
    public static List<Supplier<BlockEntityType<? extends NeroTechMachineBlockEntity>>> energyMachineTypes() {
        return List.of(
                NERO_GENERATOR::get,
                SOLAR_ARRAY::get,
                // Stage-D power tech: three generators, a buffer and a wireless relay — all five
                // live or die on the energy surface, so all five are registered here.
                WIND_TURBINE::get,
                GEOTHERMAL_GENERATOR::get,
                BIO_GENERATOR::get,
                BATTERY_BANK::get,
                WIRELESS_NODE::get,
                ORE_PROCESSOR::get,
                FABRICATOR::get,
                FUSION_REACTOR::get,
                ADVANCED_ORE_PROCESSOR::get,
                ADVANCED_FABRICATOR::get,
                COLLIDER_CORE::get,
                AUTO_CRAFTER::get,
                ITEM_SORTER::get,
                // Stage-E Robotic Arm: a pure NE sink (it pays per item moved). The Conveyor Belt has
                // no block entity at all, and the Stage-F Singularity Vault declares a zero NE buffer —
                // neither belongs on the energy surface.
                ROBOTIC_ARM::get,
                // Stage-C fluid/gas machines: the Electrolyzer, Chemical Processor and Coolant Pump are
                // NE sinks, the Gas Turbine is a source — all four need the energy surface.
                ELECTROLYZER::get,
                GAS_TURBINE::get,
                CHEMICAL_PROCESSOR::get,
                COOLANT_PUMP::get,
                // Stage-F pollution machines are pure NE sinks: without these registrations generators
                // can never push power to them on Fabric/NeoForge.
                SCRUBBER::get,
                REMEDIATOR::get);
    }

    /**
     * The machines whose inventories are exposed on each loader's standard item-handling surface, so
     * NeroLogistics / pipes / hoppers move items in and out with no NeroTech dependency.
     *
     * <p>Same list as {@link #energyMachineTypes()} minus the Remediator, which is slotless (it consumes
     * NE and cleans terrain, and has nothing to insert into or extract from), and minus the Analytics
     * Terminal and Tech Guide for the reasons given there. <b>New machines with slots MUST be added
     * here</b> — they are then wired on every loader automatically.
     */
    public static List<Supplier<BlockEntityType<? extends NeroTechMachineBlockEntity>>> itemMachineTypes() {
        return List.of(
                NERO_GENERATOR::get,
                SOLAR_ARRAY::get,
                // Stage-D: only the Bio Generator has a slot (its feedstock) — the Wind Turbine,
                // Geothermal Generator, Battery Bank, Grid Controller and Wireless Node are slotless.
                BIO_GENERATOR::get,
                ORE_PROCESSOR::get,
                FABRICATOR::get,
                FUSION_REACTOR::get,
                ADVANCED_ORE_PROCESSOR::get,
                ADVANCED_FABRICATOR::get,
                COLLIDER_CORE::get,
                AUTO_CRAFTER::get,
                ITEM_SORTER::get,
                // Stage-F Singularity Vault: its two-slot facade (input / stocked output) IS the
                // automation surface — pipes and hoppers must reach it. The Robotic Arm is absent by
                // design: its one slot is a GUI-only filter that no face ever exposes.
                SINGULARITY_VAULT::get,
                // The Chemical Processor has the usual input/output pair; the Electrolyzer, Gas Turbine
                // and Coolant Pump are slotless (fluid/gas in, gas or NE out) and stay off this list.
                CHEMICAL_PROCESSOR::get,
                // Scrubber cartridge/dirty-filter slots must be hopper/pipe-automatable like every
                // other machine.
                SCRUBBER::get);
    }

    /**
     * The machines that expose a gas tank on Core's shared {@code nerolandcore:gas} capability, so
     * NeroTech's gas chain interoperates with Core's Gas Tank — and any other mod on that surface —
     * with no cross-mod dependency. Each loader entry point iterates this list; a machine's own
     * per-face view comes from {@code NeroTechMachineBlockEntity.gasStorage(side)}.
     *
     * <p><b>Adding a gas machine?</b> Override {@code gasStorage} and add it here.
     */
    public static List<Supplier<BlockEntityType<? extends NeroTechMachineBlockEntity>>> gasMachineTypes() {
        return List.of(
                ELECTROLYZER::get,
                GAS_TURBINE::get,
                CHEMICAL_PROCESSOR::get);
    }

    /**
     * The machines that expose a fluid tank on Core's shared {@code nerolandcore:fluid} capability.
     * Currently just the Electrolyzer's water tank ({@code fluidStorage(side)}), so a Core Fluid Tank
     * or a future fluid pipe can fill it without the bucket.
     */
    public static List<Supplier<BlockEntityType<? extends NeroTechMachineBlockEntity>>> fluidMachineTypes() {
        return List.of(ELECTROLYZER::get);
    }

    private ModBlockEntities() {
    }

    public static void init() {
    }
}
