package za.co.neroland.nerotech.registry;

import java.util.function.Function;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import za.co.neroland.nerotech.NeroTechCommon;
import za.co.neroland.nerotech.guide.TechGuideBlock;
import za.co.neroland.nerotech.machine.AdvancedFabricatorBlock;
import za.co.neroland.nerotech.machine.AnalyticsTerminalBlock;
import za.co.neroland.nerotech.machine.AdvancedOreProcessorBlock;
import za.co.neroland.nerotech.machine.AutoCrafterBlock;
import za.co.neroland.nerotech.machine.FabricatorBlock;
import za.co.neroland.nerotech.machine.FusionReactorBlock;
import za.co.neroland.nerotech.machine.ItemSorterBlock;
import za.co.neroland.nerotech.machine.NeroGeneratorBlock;
import za.co.neroland.nerotech.machine.OreProcessorBlock;
import za.co.neroland.nerotech.machine.RemediatorBlock;
import za.co.neroland.nerotech.machine.ScrubberBlock;
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

    // --- Fusion Reactor multiblock structure (Stage E) -----------------------
    /**
     * Structural shell plate for the Fusion Reactor multiblock — a plain full cube, so unlike the
     * machines it KEEPS full-cube occlusion (no {@code noOcclusion}): shell walls should cull
     * their neighbours' hidden faces normally.
     */
    public static final RegistryEntry<Block> FUSION_CASING =
            BLOCKS.register("fusion_casing", key -> new Block(BlockBehaviour.Properties.of()
                    .setId(key)
                    .mapColor(MapColor.METAL)
                    .strength(3.5F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)));
    /**
     * Translucent containment shell — the vanilla-glass property recipe (non-opaque, never view
     * blocking) so the reactor's plasma reads through the multiblock wall.
     */
    public static final RegistryEntry<TransparentBlock> FUSION_CONTAINMENT_GLASS =
            BLOCKS.register("fusion_containment_glass",
                    key -> new TransparentBlock(BlockBehaviour.Properties.of()
                            .setId(key)
                            .mapColor(MapColor.COLOR_CYAN)
                            .strength(3.5F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.GLASS)
                            .noOcclusion()
                            .isValidSpawn((state, level, pos, type) -> false)
                            .isRedstoneConductor((state, level, pos) -> false)
                            .isSuffocating((state, level, pos) -> false)
                            .isViewBlocking((state, level, pos) -> false)));

    // --- Automation handoff (Stage 5) ---------------------------------------
    public static final RegistryEntry<AutoCrafterBlock> AUTO_CRAFTER =
            register("auto_crafter", AutoCrafterBlock::new);
    public static final RegistryEntry<ItemSorterBlock> ITEM_SORTER =
            register("item_sorter", ItemSorterBlock::new);

    // --- Pollution mitigation (Stage F) --------------------------------------
    public static final RegistryEntry<ScrubberBlock> SCRUBBER =
            register("scrubber", ScrubberBlock::new);
    public static final RegistryEntry<RemediatorBlock> REMEDIATOR =
            register("remediator", RemediatorBlock::new);

    // --- Production analytics (Stage G) ---------------------------------------
    public static final RegistryEntry<AnalyticsTerminalBlock> ANALYTICS_TERMINAL =
            register("analytics_terminal", AnalyticsTerminalBlock::new);

    // --- Tech Guide pedestal (Nerospace Star Guide recipe) --------------------
    /**
     * The Tech Guide pedestal — NeroTech's Star Guide. No {@code requiresCorrectToolForDrops}: with
     * no mineable tag in this repo the self-drop loot must stay reachable by hand. Faint glow so the
     * loaded pedestal reads as powered.
     */
    public static final RegistryEntry<TechGuideBlock> TECH_GUIDE =
            BLOCKS.register("tech_guide", key -> new TechGuideBlock(BlockBehaviour.Properties.of()
                    .setId(key)
                    .mapColor(MapColor.COLOR_CYAN)
                    .strength(3.0F, 6.0F)
                    .lightLevel(s -> 7)
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    private static <B extends Block> RegistryEntry<B> register(String name,
            Function<BlockBehaviour.Properties, B> factory) {
        return BLOCKS.register(name, key -> factory.apply(machineProperties().setId(key)));
    }

    private static BlockBehaviour.Properties machineProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(3.5F)
                .requiresCorrectToolForDrops()
                .sound(SoundType.METAL)
                // Machine models are multi-element, NOT full cubes (Stage D): without this the
                // block still occludes like a full cube, so neighbours cull their touching faces
                // and the world reads see-through around every machine (the gallery x-ray bug).
                .noOcclusion();
    }

    private ModBlocks() {
    }

    /** Force class-load so the static registrations run (eager on Fabric). */
    public static void init() {
    }
}
