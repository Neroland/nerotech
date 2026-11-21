package za.co.neroland.nerotech.machine.components;

import java.util.List;
import java.util.function.Supplier;

import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.entity.BlockEntityType;

import za.co.neroland.nerolandcore.registry.BlockEntityTypeCompat;

import za.co.neroland.nerotech.api.MachineTypeRegistry;
import za.co.neroland.nerotech.client.ProcessingScreen;
import za.co.neroland.nerotech.client.RendererSink;
import za.co.neroland.nerotech.client.ScreenSink;
import za.co.neroland.nerotech.machine.NeroTechMachineBlockEntity;
import za.co.neroland.nerotech.menu.ProcessingMenu;
import za.co.neroland.nerotech.registry.ModBlockEntities;
import za.co.neroland.nerotech.registry.ModBlocks;
import za.co.neroland.nerotech.registry.ModItems;
import za.co.neroland.nerotech.registry.ModMenuTypes;
import za.co.neroland.nerotech.registry.RegistrationProvider.RegistryEntry;

/**
 * The components machine family (component_forge, laser_etcher, crystal_grower). Every block, item,
 * block-entity type, menu, screen and renderer of the family is declared here and nowhere else;
 * {@code registry.FamilyRegistry} wires this class into every central place.
 *
 * <p>Declare static final entries in this order: blocks ({@code ModBlocks.machine(...)}), block items
 * ({@code ModItems.familyBlockItem(...)}), block-entity types ({@code ModBlockEntities.BLOCK_ENTITIES}),
 * menus ({@code ModMenuTypes.MENUS}). They register when this class loads.
 */
public final class ComponentsContent {

    // --- blocks ------------------------------------------------------------------------------------

    public static final RegistryEntry<ComponentForgeBlock> COMPONENT_FORGE =
            ModBlocks.machine("component_forge", ComponentForgeBlock::new);
    public static final RegistryEntry<LaserEtcherBlock> LASER_ETCHER =
            ModBlocks.machine("laser_etcher", LaserEtcherBlock::new);
    public static final RegistryEntry<CrystalGrowerBlock> CRYSTAL_GROWER =
            ModBlocks.machine("crystal_grower", CrystalGrowerBlock::new);

    // --- block items -------------------------------------------------------------------------------

    public static final RegistryEntry<BlockItem> COMPONENT_FORGE_ITEM =
            ModItems.tooltipBlockItem("component_forge", COMPONENT_FORGE,
                    "block.nerotech.component_forge.tooltip");
    public static final RegistryEntry<BlockItem> LASER_ETCHER_ITEM =
            ModItems.tooltipBlockItem("laser_etcher", LASER_ETCHER,
                    "block.nerotech.laser_etcher.tooltip");
    public static final RegistryEntry<BlockItem> CRYSTAL_GROWER_ITEM =
            ModItems.tooltipBlockItem("crystal_grower", CRYSTAL_GROWER,
                    "block.nerotech.crystal_grower.tooltip");

    // --- block-entity types ------------------------------------------------------------------------

    public static final RegistryEntry<BlockEntityType<ComponentForgeBlockEntity>> COMPONENT_FORGE_BE =
            ModBlockEntities.BLOCK_ENTITIES.register("component_forge",
                    key -> BlockEntityTypeCompat.create(ComponentForgeBlockEntity::new,
                            ComponentsContent.COMPONENT_FORGE.get()));
    public static final RegistryEntry<BlockEntityType<LaserEtcherBlockEntity>> LASER_ETCHER_BE =
            ModBlockEntities.BLOCK_ENTITIES.register("laser_etcher",
                    key -> BlockEntityTypeCompat.create(LaserEtcherBlockEntity::new,
                            ComponentsContent.LASER_ETCHER.get()));
    public static final RegistryEntry<BlockEntityType<CrystalGrowerBlockEntity>> CRYSTAL_GROWER_BE =
            ModBlockEntities.BLOCK_ENTITIES.register("crystal_grower",
                    key -> BlockEntityTypeCompat.create(CrystalGrowerBlockEntity::new,
                            ComponentsContent.CRYSTAL_GROWER.get()));

    // --- menus -------------------------------------------------------------------------------------

    public static final RegistryEntry<MenuType<ProcessingMenu>> COMPONENT_FORGE_MENU =
            ModMenuTypes.MENUS.register("component_forge",
                    key -> new MenuType<>((id, inv) -> new ProcessingMenu(ComponentsContent.COMPONENT_FORGE_MENU, id,
                            inv, ComponentForgeBlockEntity.LAYOUT), FeatureFlags.VANILLA_SET));
    public static final RegistryEntry<MenuType<ProcessingMenu>> LASER_ETCHER_MENU =
            ModMenuTypes.MENUS.register("laser_etcher",
                    key -> new MenuType<>((id, inv) -> new ProcessingMenu(ComponentsContent.LASER_ETCHER_MENU, id,
                            inv, LaserEtcherBlockEntity.LAYOUT), FeatureFlags.VANILLA_SET));
    public static final RegistryEntry<MenuType<ProcessingMenu>> CRYSTAL_GROWER_MENU =
            ModMenuTypes.MENUS.register("crystal_grower",
                    key -> new MenuType<>((id, inv) -> new ProcessingMenu(ComponentsContent.CRYSTAL_GROWER_MENU, id,
                            inv, CrystalGrowerBlockEntity.LAYOUT), FeatureFlags.VANILLA_SET));

    private ComponentsContent() {
    }

    /** Force class-load so the static registrations run (called once by {@code FamilyRegistry.init()}). */
    public static void init() {
    }

    /** This family's block-entity types per capability surface (energy / item / gas / fluid). */
    public static List<Supplier<BlockEntityType<? extends NeroTechMachineBlockEntity>>> machineTypes(
            MachineTypeRegistry.Surface surface) {
        return switch (surface) {
            case ENERGY -> List.of(COMPONENT_FORGE_BE::get, LASER_ETCHER_BE::get, CRYSTAL_GROWER_BE::get);
            case ITEM -> List.of(COMPONENT_FORGE_BE::get, LASER_ETCHER_BE::get, CRYSTAL_GROWER_BE::get);
            case GAS -> List.of();
            case FLUID -> List.of(COMPONENT_FORGE_BE::get, CRYSTAL_GROWER_BE::get);
        };
    }

    /** Client only: register this family's menu screens ({@code sink.<ProcessingMenu, ProcessingScreen<ProcessingMenu>>register(MENU.get(), Screen::new)}). */
    public static void screens(ScreenSink sink) {
        sink.<ProcessingMenu, ProcessingScreen<ProcessingMenu>>register(COMPONENT_FORGE_MENU.get(), ProcessingScreen::create);
        sink.<ProcessingMenu, ProcessingScreen<ProcessingMenu>>register(LASER_ETCHER_MENU.get(), ProcessingScreen::create);
        sink.<ProcessingMenu, ProcessingScreen<ProcessingMenu>>register(CRYSTAL_GROWER_MENU.get(), ProcessingScreen::create);
    }

    /**
     * Client only: register this family's renderers ({@code sink.add(TYPE.get(), Renderer::new)}). The
     * art package (A4) writes {@code ComponentForgeRenderer}, {@code LaserEtcherRenderer} and
     * {@code CrystalGrowerRenderer}; until then the status lamp is the only renderer.
     */
    public static void renderers(RendererSink sink) {
        sink.add(COMPONENT_FORGE_BE.get(), za.co.neroland.nerotech.client.render.ComponentForgeRenderer::new);
        sink.add(LASER_ETCHER_BE.get(), za.co.neroland.nerotech.client.render.LaserEtcherRenderer::new);
        sink.add(CRYSTAL_GROWER_BE.get(), za.co.neroland.nerotech.client.render.CrystalGrowerRenderer::new);
    }

    /** This family's creative-tab entries, in display order. */
    public static List<ItemLike> creativeContents() {
        return List.of(COMPONENT_FORGE_ITEM.get(), LASER_ETCHER_ITEM.get(), CRYSTAL_GROWER_ITEM.get());
    }
}
