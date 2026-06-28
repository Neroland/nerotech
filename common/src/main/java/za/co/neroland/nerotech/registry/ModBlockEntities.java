package za.co.neroland.nerotech.registry;

import java.util.Set;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;

import za.co.neroland.nerotech.NeroTechCommon;
import za.co.neroland.nerotech.machine.AdvancedFabricatorBlockEntity;
import za.co.neroland.nerotech.machine.AdvancedOreProcessorBlockEntity;
import za.co.neroland.nerotech.machine.FabricatorBlockEntity;
import za.co.neroland.nerotech.machine.FusionReactorBlockEntity;
import za.co.neroland.nerotech.machine.NeroGeneratorBlockEntity;
import za.co.neroland.nerotech.machine.OreProcessorBlockEntity;
import za.co.neroland.nerotech.machine.SolarArrayBlockEntity;
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

    private ModBlockEntities() {
    }

    public static void init() {
    }
}
