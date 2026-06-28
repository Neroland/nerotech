package za.co.neroland.nerotech.registry;

import java.util.function.Function;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import za.co.neroland.nerotech.NeroTechCommon;
import za.co.neroland.nerotech.machine.AdvancedFabricatorBlock;
import za.co.neroland.nerotech.machine.AdvancedOreProcessorBlock;
import za.co.neroland.nerotech.machine.FabricatorBlock;
import za.co.neroland.nerotech.machine.FusionReactorBlock;
import za.co.neroland.nerotech.machine.NeroGeneratorBlock;
import za.co.neroland.nerotech.machine.OreProcessorBlock;
import za.co.neroland.nerotech.machine.SolarArrayBlock;
import za.co.neroland.nerotech.registry.RegistrationProvider.RegistryEntry;

/**
 * NeroTech's Tier-1 machine blocks, registered cross-loader through the {@link RegistrationProvider}
 * seam over the vanilla block registry.
 */
public final class ModBlocks {

    public static final RegistrationProvider<Block> BLOCKS =
            RegistrationProvider.get(Registries.BLOCK, NeroTechCommon.MOD_ID);

    public static final RegistryEntry<NeroGeneratorBlock> NERO_GENERATOR =
            register("nero_generator", NeroGeneratorBlock::new);
    public static final RegistryEntry<SolarArrayBlock> SOLAR_ARRAY =
            register("solar_array", SolarArrayBlock::new);
    public static final RegistryEntry<OreProcessorBlock> ORE_PROCESSOR =
            register("ore_processor", OreProcessorBlock::new);
    public static final RegistryEntry<FabricatorBlock> FABRICATOR =
            register("fabricator", FabricatorBlock::new);

    // --- Tier 2/3 (gated behind orbit + Starsteel) --------------------------
    public static final RegistryEntry<FusionReactorBlock> FUSION_REACTOR =
            register("fusion_reactor", FusionReactorBlock::new);
    public static final RegistryEntry<AdvancedOreProcessorBlock> ADVANCED_ORE_PROCESSOR =
            register("advanced_ore_processor", AdvancedOreProcessorBlock::new);
    public static final RegistryEntry<AdvancedFabricatorBlock> ADVANCED_FABRICATOR =
            register("advanced_fabricator", AdvancedFabricatorBlock::new);

    private static <B extends Block> RegistryEntry<B> register(String name,
            Function<BlockBehaviour.Properties, B> factory) {
        return BLOCKS.register(name, key -> factory.apply(machineProperties().setId(key)));
    }

    private static BlockBehaviour.Properties machineProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(3.5F)
                .requiresCorrectToolForDrops()
                .sound(SoundType.METAL);
    }

    private ModBlocks() {
    }

    /** Force class-load so the static registrations run (eager on Fabric). */
    public static void init() {
    }
}
