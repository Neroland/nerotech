package za.co.neroland.nerotech.machine.metallurgy;

import java.util.List;
import java.util.function.Supplier;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.entity.BlockEntityType;

import za.co.neroland.nerolandcore.registry.BlockEntityTypeCompat;

import za.co.neroland.nerotech.NeroTechCommon;
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
 * The metallurgy machine family (plasma_crucible, plate_press, filament_spooler). Every block, item,
 * block-entity type, menu, screen and renderer of the family is declared here and nowhere else;
 * {@code registry.FamilyRegistry} wires this class into every central place.
 *
 * <p>Declare static final entries in this order: blocks ({@code ModBlocks.machine(...)}), block items
 * ({@code ModItems.familyBlockItem(...)}), block-entity types ({@code ModBlockEntities.BLOCK_ENTITIES}),
 * menus ({@code ModMenuTypes.MENUS}). They register when this class loads.
 */
public final class MetallurgyContent {

    /** Items the Plate Press die slot accepts. */
    public static final TagKey<Item> PRESS_DIES = TagKey.create(Registries.ITEM,
            Identifier.fromNamespaceAndPath(NeroTechCommon.MOD_ID, "press_dies"));

    // --- blocks ---------------------------------------------------------------------------------------

    public static final RegistryEntry<PlasmaCrucibleBlock> PLASMA_CRUCIBLE =
            ModBlocks.machine("plasma_crucible", PlasmaCrucibleBlock::new);
    public static final RegistryEntry<PlatePressBlock> PLATE_PRESS =
            ModBlocks.machine("plate_press", PlatePressBlock::new);
    public static final RegistryEntry<FilamentSpoolerBlock> FILAMENT_SPOOLER =
            ModBlocks.machine("filament_spooler", FilamentSpoolerBlock::new);

    // --- items ----------------------------------------------------------------------------------------

    public static final RegistryEntry<BlockItem> PLASMA_CRUCIBLE_ITEM =
            ModItems.tooltipBlockItem("plasma_crucible", PLASMA_CRUCIBLE,
                    "block.nerotech.plasma_crucible.tooltip");
    public static final RegistryEntry<BlockItem> PLATE_PRESS_ITEM =
            ModItems.tooltipBlockItem("plate_press", PLATE_PRESS,
                    "block.nerotech.plate_press.tooltip");
    public static final RegistryEntry<BlockItem> FILAMENT_SPOOLER_ITEM =
            ModItems.tooltipBlockItem("filament_spooler", FILAMENT_SPOOLER,
                    "block.nerotech.filament_spooler.tooltip");

    /** Plate Press dies: held in the die slot, never used up. */
    public static final RegistryEntry<Item> PLATE_DIE = ModItems.simpleItem("plate_die", p -> p.stacksTo(1));
    public static final RegistryEntry<Item> ROD_DIE = ModItems.simpleItem("rod_die", p -> p.stacksTo(1));
    public static final RegistryEntry<Item> WAFER_DIE = ModItems.simpleItem("wafer_die", p -> p.stacksTo(1));

    // --- block-entity types ---------------------------------------------------------------------------

    public static final RegistryEntry<BlockEntityType<PlasmaCrucibleBlockEntity>> PLASMA_CRUCIBLE_BE =
            ModBlockEntities.BLOCK_ENTITIES.register("plasma_crucible",
                    key -> BlockEntityTypeCompat.create(PlasmaCrucibleBlockEntity::new,
                            MetallurgyContent.PLASMA_CRUCIBLE.get()));
    public static final RegistryEntry<BlockEntityType<PlatePressBlockEntity>> PLATE_PRESS_BE =
            ModBlockEntities.BLOCK_ENTITIES.register("plate_press",
                    key -> BlockEntityTypeCompat.create(PlatePressBlockEntity::new,
                            MetallurgyContent.PLATE_PRESS.get()));
    public static final RegistryEntry<BlockEntityType<FilamentSpoolerBlockEntity>> FILAMENT_SPOOLER_BE =
            ModBlockEntities.BLOCK_ENTITIES.register("filament_spooler",
                    key -> BlockEntityTypeCompat.create(FilamentSpoolerBlockEntity::new,
                            MetallurgyContent.FILAMENT_SPOOLER.get()));

    // --- menus ----------------------------------------------------------------------------------------

    public static final RegistryEntry<MenuType<ProcessingMenu>> PLASMA_CRUCIBLE_MENU =
            ModMenuTypes.MENUS.register("plasma_crucible",
                    key -> new MenuType<>((id, inv) -> new ProcessingMenu(MetallurgyContent.PLASMA_CRUCIBLE_MENU, id,
                            inv, PlasmaCrucibleBlockEntity.LAYOUT), FeatureFlags.VANILLA_SET));
    public static final RegistryEntry<MenuType<ProcessingMenu>> PLATE_PRESS_MENU =
            ModMenuTypes.MENUS.register("plate_press",
                    key -> new MenuType<>((id, inv) -> new ProcessingMenu(MetallurgyContent.PLATE_PRESS_MENU, id,
                            inv, PlatePressBlockEntity.LAYOUT), FeatureFlags.VANILLA_SET));
    public static final RegistryEntry<MenuType<ProcessingMenu>> FILAMENT_SPOOLER_MENU =
            ModMenuTypes.MENUS.register("filament_spooler",
                    key -> new MenuType<>((id, inv) -> new ProcessingMenu(MetallurgyContent.FILAMENT_SPOOLER_MENU, id,
                            inv, FilamentSpoolerBlockEntity.LAYOUT), FeatureFlags.VANILLA_SET));

    private MetallurgyContent() {
    }

    /** Force class-load so the static registrations run (called once by {@code FamilyRegistry.init()}). */
    public static void init() {
    }

    /** This family's block-entity types per capability surface (energy / item / gas / fluid). */
    public static List<Supplier<BlockEntityType<? extends NeroTechMachineBlockEntity>>> machineTypes(
            MachineTypeRegistry.Surface surface) {
        return switch (surface) {
            case ENERGY, ITEM -> List.of(PLASMA_CRUCIBLE_BE::get, PLATE_PRESS_BE::get, FILAMENT_SPOOLER_BE::get);
            case GAS -> List.of();
            case FLUID -> List.of(PLATE_PRESS_BE::get);
        };
    }

    /** Client only: register this family's menu screens ({@code sink.register(MENU.get(), Screen::new)}). */
    public static void screens(ScreenSink sink) {
        sink.<ProcessingMenu, ProcessingScreen<ProcessingMenu>>register(PLASMA_CRUCIBLE_MENU.get(), ProcessingScreen::create);
        sink.<ProcessingMenu, ProcessingScreen<ProcessingMenu>>register(PLATE_PRESS_MENU.get(), ProcessingScreen::create);
        sink.<ProcessingMenu, ProcessingScreen<ProcessingMenu>>register(FILAMENT_SPOOLER_MENU.get(), ProcessingScreen::create);
    }

    /** Client only: register this family's renderers ({@code sink.add(TYPE.get(), Renderer::new)}). */
    public static void renderers(RendererSink sink) {
        sink.add(PLASMA_CRUCIBLE_BE.get(), za.co.neroland.nerotech.client.render.PlasmaCrucibleRenderer::new);
        sink.add(PLATE_PRESS_BE.get(), za.co.neroland.nerotech.client.render.PlatePressRenderer::new);
        sink.add(FILAMENT_SPOOLER_BE.get(), za.co.neroland.nerotech.client.render.FilamentSpoolerRenderer::new);
    }

    /** This family's creative-tab entries, in display order. */
    public static List<ItemLike> creativeContents() {
        return List.of(PLASMA_CRUCIBLE_ITEM.get(), PLATE_PRESS_ITEM.get(), FILAMENT_SPOOLER_ITEM.get(),
                PLATE_DIE.get(), ROD_DIE.get(), WAFER_DIE.get());
    }
}
