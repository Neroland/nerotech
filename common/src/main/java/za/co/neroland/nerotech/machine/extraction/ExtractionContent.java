package za.co.neroland.nerotech.machine.extraction;

import java.util.List;
import java.util.function.Supplier;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.entity.BlockEntityType;

import za.co.neroland.nerolandcore.registry.BlockEntityTypeCompat;

import za.co.neroland.nerotech.api.MachineTypeRegistry;
import za.co.neroland.nerotech.client.RendererSink;
import za.co.neroland.nerotech.client.ScreenSink;
import za.co.neroland.nerotech.machine.NeroTechMachineBlockEntity;
import za.co.neroland.nerotech.menu.ProcessingMenu;
import za.co.neroland.nerotech.registry.ModBlockEntities;
import za.co.neroland.nerotech.registry.ModBlocks;
import za.co.neroland.nerotech.registry.ModDataComponents;
import za.co.neroland.nerotech.registry.ModItems;
import za.co.neroland.nerotech.registry.ModMenuTypes;
import za.co.neroland.nerotech.registry.RegistrationProvider.RegistryEntry;

/**
 * The extraction machine family (deep_core_drill, drill_derrick) and the Survey Scanner. Every block, item,
 * block-entity type, menu, screen and renderer of the family is declared here and nowhere else;
 * {@code registry.FamilyRegistry} wires this class into every central place.
 *
 * <p>Declare static final entries in this order: blocks ({@code ModBlocks.machine(...)}), block items
 * ({@code ModItems.familyBlockItem(...)}), block-entity types ({@code ModBlockEntities.BLOCK_ENTITIES}),
 * menus ({@code ModMenuTypes.MENUS}). They register when this class loads.
 */
public final class ExtractionContent {

    // --- data components (item data only, no player data) --------------------------------------------

    /** The Survey Scanner's stored NE and next-scan time. */
    public static final RegistryEntry<DataComponentType<ScannerState>> SCANNER_STATE =
            ModDataComponents.COMPONENTS.register("survey_scanner_state",
                    key -> DataComponentType.<ScannerState>builder().persistent(ScannerState.CODEC).build());

    /**
     * Operations a drill head has used. Wear lives on the head item, so it leaves the drill with the head
     * and cannot be reset by taking the head out or by breaking and re-placing the controller.
     */
    public static final RegistryEntry<DataComponentType<Integer>> DRILL_HEAD_WEAR =
            ModDataComponents.COMPONENTS.register("drill_head_wear",
                    key -> DataComponentType.<Integer>builder()
                            .persistent(com.mojang.serialization.Codec.intRange(0, Integer.MAX_VALUE)).build());

    // --- blocks ---------------------------------------------------------------------------------------

    public static final RegistryEntry<DeepCoreDrillBlock> DEEP_CORE_DRILL =
            ModBlocks.machine("deep_core_drill", DeepCoreDrillBlock::new);
    public static final RegistryEntry<DrillDerrickBlock> DRILL_DERRICK =
            ModBlocks.machine("drill_derrick", DrillDerrickBlock::new);

    // --- items ----------------------------------------------------------------------------------------

    public static final RegistryEntry<BlockItem> DEEP_CORE_DRILL_ITEM =
            ModItems.tooltipBlockItem("deep_core_drill", DEEP_CORE_DRILL, "block.nerotech.deep_core_drill.tooltip");
    public static final RegistryEntry<BlockItem> DRILL_DERRICK_ITEM =
            ModItems.tooltipBlockItem("drill_derrick", DRILL_DERRICK, "block.nerotech.drill_derrick.tooltip");
    public static final RegistryEntry<Item> SURVEY_SCANNER = ModItems.ITEMS.register("survey_scanner",
            key -> new SurveyScannerItem(new Item.Properties().setId(key).stacksTo(1)));

    // --- block-entity types ---------------------------------------------------------------------------

    public static final RegistryEntry<BlockEntityType<DeepCoreDrillBlockEntity>> DEEP_CORE_DRILL_BE =
            ModBlockEntities.BLOCK_ENTITIES.register("deep_core_drill",
                    key -> BlockEntityTypeCompat.create(DeepCoreDrillBlockEntity::new,
                            ExtractionContent.DEEP_CORE_DRILL.get()));

    // --- menus ----------------------------------------------------------------------------------------

    public static final RegistryEntry<MenuType<ProcessingMenu>> DEEP_CORE_DRILL_MENU =
            ModMenuTypes.MENUS.register("deep_core_drill",
                    key -> new MenuType<>((id, inv) -> new ProcessingMenu(ExtractionContent.DEEP_CORE_DRILL_MENU, id,
                            inv, DeepCoreDrillBlockEntity.LAYOUT), FeatureFlags.VANILLA_SET));

    private ExtractionContent() {
    }

    /** Force class-load so the static registrations run (called once by {@code FamilyRegistry.init()}). */
    public static void init() {
    }

    /** This family's block-entity types per capability surface (energy / item / gas / fluid). */
    public static List<Supplier<BlockEntityType<? extends NeroTechMachineBlockEntity>>> machineTypes(
            MachineTypeRegistry.Surface surface) {
        return switch (surface) {
            case ENERGY, ITEM -> List.of(DEEP_CORE_DRILL_BE::get);
            case GAS -> List.of();
            case FLUID -> List.of();
        };
    }

    /** Client only: register this family's menu screens ({@code sink.register(MENU.get(), Screen::new)}). */
    public static void screens(ScreenSink sink) {
        sink.<ProcessingMenu, DeepCoreDrillScreen>register(DEEP_CORE_DRILL_MENU.get(), DeepCoreDrillScreen::new);
    }

    /** Client only: register this family's renderers ({@code sink.add(TYPE.get(), Renderer::new)}). */
    public static void renderers(RendererSink sink) {
        sink.add(DEEP_CORE_DRILL_BE.get(), za.co.neroland.nerotech.client.render.DeepCoreDrillRenderer::new);
    }

    /** This family's creative-tab entries, in display order. */
    public static List<ItemLike> creativeContents() {
        return List.of(DEEP_CORE_DRILL_ITEM.get(), DRILL_DERRICK_ITEM.get(), SURVEY_SCANNER.get());
    }
}
