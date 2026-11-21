package za.co.neroland.nerotech.machine.chemistry;

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
 * The chemistry machine family (centrifuge, cryo_distiller, atmospheric_condenser, reaction_vat). Every block, item,
 * block-entity type, menu, screen and renderer of the family is declared here and nowhere else;
 * {@code registry.FamilyRegistry} wires this class into every central place. (The Process Tank, also a
 * chemistry block, lives in {@code machine.tank}.)
 *
 * <p>Declare static final entries in this order: blocks ({@code ModBlocks.machine(...)}), block items
 * ({@code ModItems.familyBlockItem(...)}), block-entity types ({@code ModBlockEntities.BLOCK_ENTITIES}),
 * menus ({@code ModMenuTypes.MENUS}). They register when this class loads.
 */
public final class ChemistryContent {

    // --- blocks ------------------------------------------------------------------------------------

    public static final RegistryEntry<CentrifugeBlock> CENTRIFUGE =
            ModBlocks.machine("centrifuge", CentrifugeBlock::new);
    public static final RegistryEntry<CryoDistillerBlock> CRYO_DISTILLER =
            ModBlocks.machine("cryo_distiller", CryoDistillerBlock::new);
    public static final RegistryEntry<AtmosphericCondenserBlock> ATMOSPHERIC_CONDENSER =
            ModBlocks.machine("atmospheric_condenser", AtmosphericCondenserBlock::new);
    public static final RegistryEntry<ReactionVatBlock> REACTION_VAT =
            ModBlocks.machine("reaction_vat", ReactionVatBlock::new);

    // --- block items -------------------------------------------------------------------------------

    public static final RegistryEntry<BlockItem> CENTRIFUGE_ITEM = ModItems.tooltipBlockItem("centrifuge", CENTRIFUGE,
            "block.nerotech.centrifuge.tooltip");
    public static final RegistryEntry<BlockItem> CRYO_DISTILLER_ITEM =
            ModItems.tooltipBlockItem("cryo_distiller", CRYO_DISTILLER,
                    "block.nerotech.cryo_distiller.tooltip");
    public static final RegistryEntry<BlockItem> ATMOSPHERIC_CONDENSER_ITEM =
            ModItems.tooltipBlockItem("atmospheric_condenser", ATMOSPHERIC_CONDENSER,
                    "block.nerotech.atmospheric_condenser.tooltip");
    public static final RegistryEntry<BlockItem> REACTION_VAT_ITEM =
            ModItems.tooltipBlockItem("reaction_vat", REACTION_VAT,
                    "block.nerotech.reaction_vat.tooltip");

    // --- block-entity types ------------------------------------------------------------------------

    public static final RegistryEntry<BlockEntityType<CentrifugeBlockEntity>> CENTRIFUGE_BE =
            ModBlockEntities.BLOCK_ENTITIES.register("centrifuge",
                    key -> BlockEntityTypeCompat.create(CentrifugeBlockEntity::new, ChemistryContent.CENTRIFUGE.get()));
    public static final RegistryEntry<BlockEntityType<CryoDistillerBlockEntity>> CRYO_DISTILLER_BE =
            ModBlockEntities.BLOCK_ENTITIES.register("cryo_distiller",
                    key -> BlockEntityTypeCompat.create(CryoDistillerBlockEntity::new,
                            ChemistryContent.CRYO_DISTILLER.get()));
    public static final RegistryEntry<BlockEntityType<AtmosphericCondenserBlockEntity>> ATMOSPHERIC_CONDENSER_BE =
            ModBlockEntities.BLOCK_ENTITIES.register("atmospheric_condenser",
                    key -> BlockEntityTypeCompat.create(AtmosphericCondenserBlockEntity::new,
                            ChemistryContent.ATMOSPHERIC_CONDENSER.get()));
    public static final RegistryEntry<BlockEntityType<ReactionVatBlockEntity>> REACTION_VAT_BE =
            ModBlockEntities.BLOCK_ENTITIES.register("reaction_vat",
                    key -> BlockEntityTypeCompat.create(ReactionVatBlockEntity::new, ChemistryContent.REACTION_VAT.get()));

    // --- menus -------------------------------------------------------------------------------------

    public static final RegistryEntry<MenuType<ProcessingMenu>> CENTRIFUGE_MENU = ModMenuTypes.MENUS.register("centrifuge",
            key -> new MenuType<>((id, inv) -> new ProcessingMenu(ChemistryContent.CENTRIFUGE_MENU, id, inv,
                    CentrifugeBlockEntity.LAYOUT), FeatureFlags.VANILLA_SET));
    public static final RegistryEntry<MenuType<ProcessingMenu>> CRYO_DISTILLER_MENU =
            ModMenuTypes.MENUS.register("cryo_distiller",
                    key -> new MenuType<>((id, inv) -> new ProcessingMenu(ChemistryContent.CRYO_DISTILLER_MENU, id,
                            inv, CryoDistillerBlockEntity.LAYOUT), FeatureFlags.VANILLA_SET));
    public static final RegistryEntry<MenuType<ProcessingMenu>> ATMOSPHERIC_CONDENSER_MENU =
            ModMenuTypes.MENUS.register("atmospheric_condenser",
                    key -> new MenuType<>((id, inv) -> new ProcessingMenu(ChemistryContent.ATMOSPHERIC_CONDENSER_MENU,
                            id, inv, AtmosphericCondenserBlockEntity.LAYOUT), FeatureFlags.VANILLA_SET));
    public static final RegistryEntry<MenuType<ProcessingMenu>> REACTION_VAT_MENU =
            ModMenuTypes.MENUS.register("reaction_vat",
                    key -> new MenuType<>((id, inv) -> new ProcessingMenu(ChemistryContent.REACTION_VAT_MENU, id,
                            inv, ReactionVatBlockEntity.LAYOUT), FeatureFlags.VANILLA_SET));

    private ChemistryContent() {
    }

    /** Force class-load so the static registrations run (called once by {@code FamilyRegistry.init()}). */
    public static void init() {
    }

    /** This family's block-entity types per capability surface (energy / item / gas / fluid). */
    public static List<Supplier<BlockEntityType<? extends NeroTechMachineBlockEntity>>> machineTypes(
            MachineTypeRegistry.Surface surface) {
        return switch (surface) {
            case ENERGY, ITEM, GAS -> List.of(CENTRIFUGE_BE::get, CRYO_DISTILLER_BE::get, ATMOSPHERIC_CONDENSER_BE::get,
                    REACTION_VAT_BE::get);
            // The condenser holds no fluid; the other three carry at least one fluid tank.
            case FLUID -> List.of(CENTRIFUGE_BE::get, CRYO_DISTILLER_BE::get, REACTION_VAT_BE::get);
        };
    }

    /** Client only: register this family's menu screens ({@code sink.register(MENU.get(), Screen::new)}). */
    public static void screens(ScreenSink sink) {
        sink.<ProcessingMenu, ProcessingScreen<ProcessingMenu>>register(CENTRIFUGE_MENU.get(), ProcessingScreen::create);
        sink.<ProcessingMenu, ProcessingScreen<ProcessingMenu>>register(CRYO_DISTILLER_MENU.get(), ProcessingScreen::create);
        sink.<ProcessingMenu, ChemistryScreen>register(ATMOSPHERIC_CONDENSER_MENU.get(), ChemistryScreen::create);
        sink.<ProcessingMenu, ProcessingScreen<ProcessingMenu>>register(REACTION_VAT_MENU.get(), ProcessingScreen::create);
    }

    /** Client only: register this family's renderers ({@code sink.add(TYPE.get(), Renderer::new)}). */
    public static void renderers(RendererSink sink) {
        sink.add(CENTRIFUGE_BE.get(), za.co.neroland.nerotech.client.render.CentrifugeRenderer::new);
        sink.add(CRYO_DISTILLER_BE.get(), za.co.neroland.nerotech.client.render.CryoDistillerRenderer::new);
        sink.add(ATMOSPHERIC_CONDENSER_BE.get(), za.co.neroland.nerotech.client.render.AtmosphericCondenserRenderer::new);
        sink.add(REACTION_VAT_BE.get(), za.co.neroland.nerotech.client.render.ReactionVatRenderer::new);
    }

    /** This family's creative-tab entries, in display order. */
    public static List<ItemLike> creativeContents() {
        return List.of(CENTRIFUGE_ITEM.get(), CRYO_DISTILLER_ITEM.get(), ATMOSPHERIC_CONDENSER_ITEM.get(),
                REACTION_VAT_ITEM.get());
    }
}
