package za.co.neroland.nerotech.machine.utility;

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
 * The utility machine family (induction_charger, annunciator_panel). Every block, item,
 * block-entity type, menu, screen and renderer of the family is declared here and nowhere else;
 * {@code registry.FamilyRegistry} wires this class into every central place.
 *
 * <p>Declare static final entries in this order: blocks ({@code ModBlocks.machine(...)}), block items
 * ({@code ModItems.familyBlockItem(...)}), block-entity types ({@code ModBlockEntities.BLOCK_ENTITIES}),
 * menus ({@code ModMenuTypes.MENUS}). They register when this class loads.
 */
public final class UtilityContent {

    // --- blocks ---------------------------------------------------------------------------------------

    /** Low floor pad: not a full cube, so it must not occlude. */
    public static final RegistryEntry<InductionChargerBlock> INDUCTION_CHARGER =
            ModBlocks.machine("induction_charger", p -> p.noOcclusion(), InductionChargerBlock::new);
    /** Thin wall panel. */
    public static final RegistryEntry<AnnunciatorPanelBlock> ANNUNCIATOR_PANEL =
            ModBlocks.machine("annunciator_panel", p -> p.noOcclusion(), AnnunciatorPanelBlock::new);

    // --- items ----------------------------------------------------------------------------------------

    public static final RegistryEntry<BlockItem> INDUCTION_CHARGER_ITEM =
            ModItems.tooltipBlockItem("induction_charger", INDUCTION_CHARGER,
                    "block.nerotech.induction_charger.tooltip");
    public static final RegistryEntry<BlockItem> ANNUNCIATOR_PANEL_ITEM =
            ModItems.tooltipBlockItem("annunciator_panel", ANNUNCIATOR_PANEL,
                    "block.nerotech.annunciator_panel.tooltip");

    // --- block-entity types ---------------------------------------------------------------------------

    public static final RegistryEntry<BlockEntityType<InductionChargerBlockEntity>> INDUCTION_CHARGER_BE =
            ModBlockEntities.BLOCK_ENTITIES.register("induction_charger",
                    key -> BlockEntityTypeCompat.create(InductionChargerBlockEntity::new,
                            UtilityContent.INDUCTION_CHARGER.get()));
    public static final RegistryEntry<BlockEntityType<AnnunciatorPanelBlockEntity>> ANNUNCIATOR_PANEL_BE =
            ModBlockEntities.BLOCK_ENTITIES.register("annunciator_panel",
                    key -> BlockEntityTypeCompat.create(AnnunciatorPanelBlockEntity::new,
                            UtilityContent.ANNUNCIATOR_PANEL.get()));

    // --- menus ----------------------------------------------------------------------------------------

    public static final RegistryEntry<MenuType<ProcessingMenu>> INDUCTION_CHARGER_MENU =
            ModMenuTypes.MENUS.register("induction_charger",
                    key -> new MenuType<>((id, inv) -> new ProcessingMenu(UtilityContent.INDUCTION_CHARGER_MENU, id,
                            inv, InductionChargerBlockEntity.LAYOUT), FeatureFlags.VANILLA_SET));
    public static final RegistryEntry<MenuType<ProcessingMenu>> ANNUNCIATOR_PANEL_MENU =
            ModMenuTypes.MENUS.register("annunciator_panel",
                    key -> new MenuType<>((id, inv) -> new ProcessingMenu(UtilityContent.ANNUNCIATOR_PANEL_MENU, id,
                            inv, AnnunciatorPanelBlockEntity.LAYOUT), FeatureFlags.VANILLA_SET));

    private UtilityContent() {
    }

    /** Force class-load so the static registrations run (called once by {@code FamilyRegistry.init()}). */
    public static void init() {
    }

    /**
     * This family's block-entity types per capability surface. The charger takes energy; the panel needs
     * no power and has no slots or tanks, so it exposes nothing.
     */
    public static List<Supplier<BlockEntityType<? extends NeroTechMachineBlockEntity>>> machineTypes(
            MachineTypeRegistry.Surface surface) {
        return switch (surface) {
            case ENERGY -> List.of(INDUCTION_CHARGER_BE::get);
            case ITEM -> List.of();
            case GAS -> List.of();
            case FLUID -> List.of();
        };
    }

    /** Client only: register this family's menu screens. */
    public static void screens(ScreenSink sink) {
        sink.<ProcessingMenu, ProcessingScreen<ProcessingMenu>>register(INDUCTION_CHARGER_MENU.get(),
                UtilityScreen::charger);
        sink.<ProcessingMenu, ProcessingScreen<ProcessingMenu>>register(ANNUNCIATOR_PANEL_MENU.get(),
                UtilityScreen::panel);
    }

    /**
     * Client only: register this family's renderers. {@code InductionChargerRenderer} and
     * {@code AnnunciatorPanelRenderer} come from the art pass (A6) and are registered by the integrator.
     */
    public static void renderers(RendererSink sink) {
        sink.add(INDUCTION_CHARGER_BE.get(), za.co.neroland.nerotech.client.render.InductionChargerRenderer::new);
        sink.add(ANNUNCIATOR_PANEL_BE.get(), za.co.neroland.nerotech.client.render.AnnunciatorPanelRenderer::new);
    }

    /** This family's creative-tab entries, in display order. */
    public static List<ItemLike> creativeContents() {
        return List.of(INDUCTION_CHARGER_ITEM.get(), ANNUNCIATOR_PANEL_ITEM.get());
    }
}
