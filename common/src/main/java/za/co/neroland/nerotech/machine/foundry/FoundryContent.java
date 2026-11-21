package za.co.neroland.nerotech.machine.foundry;

import java.util.List;
import java.util.function.Supplier;

import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;

import za.co.neroland.nerolandcore.registry.BlockEntityTypeCompat;

import za.co.neroland.nerotech.api.MachineTypeRegistry;
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
 * WP-Foundry — the foundry machine family (blast_foundry, foundry_brick, foundry_heat_port). Owned by its work package: every block, item,
 * block-entity type, menu, screen and renderer of the family is declared here and nowhere else;
 * {@code registry.FamilyRegistry} wires this class into every central place.
 *
 * <p>Declare static final entries in this order: blocks ({@code ModBlocks.machine(...)}), block items
 * ({@code ModItems.familyBlockItem(...)}), block-entity types ({@code ModBlockEntities.BLOCK_ENTITIES}),
 * menus ({@code ModMenuTypes.MENUS}). They register when this class loads.
 */
public final class FoundryContent {

    // --- blocks ----------------------------------------------------------------------------------

    public static final RegistryEntry<BlastFoundryBlock> BLAST_FOUNDRY =
            ModBlocks.machine("blast_foundry", BlastFoundryBlock::new);
    public static final RegistryEntry<FoundryBrickBlock> FOUNDRY_BRICK = ModBlocks.machine("foundry_brick",
            p -> p.mapColor(MapColor.NETHER).sound(SoundType.NETHER_BRICKS).strength(3.0F, 6.0F)
                    .lightLevel(FoundryBrickBlock::lightLevel),
            FoundryBrickBlock::new);
    public static final RegistryEntry<FoundryHeatPortBlock> FOUNDRY_HEAT_PORT = ModBlocks.machine("foundry_heat_port",
            p -> p.mapColor(MapColor.NETHER).strength(3.0F, 6.0F),
            FoundryHeatPortBlock::new);

    // --- block items -----------------------------------------------------------------------------

    public static final RegistryEntry<BlockItem> BLAST_FOUNDRY_ITEM = ModItems.tooltipBlockItem("blast_foundry",
            BLAST_FOUNDRY, "block.nerotech.blast_foundry.tooltip");
    public static final RegistryEntry<BlockItem> FOUNDRY_BRICK_ITEM = ModItems.tooltipBlockItem("foundry_brick",
            FOUNDRY_BRICK, "block.nerotech.foundry_brick.tooltip");
    public static final RegistryEntry<BlockItem> FOUNDRY_HEAT_PORT_ITEM = ModItems.tooltipBlockItem(
            "foundry_heat_port", FOUNDRY_HEAT_PORT, "block.nerotech.foundry_heat_port.tooltip");

    // --- block-entity types ----------------------------------------------------------------------

    public static final RegistryEntry<BlockEntityType<BlastFoundryBlockEntity>> BLAST_FOUNDRY_BE =
            ModBlockEntities.BLOCK_ENTITIES.register("blast_foundry",
                    key -> BlockEntityTypeCompat.create(BlastFoundryBlockEntity::new,
                            FoundryContent.BLAST_FOUNDRY.get()));
    public static final RegistryEntry<BlockEntityType<FoundryHeatPortBlockEntity>> FOUNDRY_HEAT_PORT_BE =
            ModBlockEntities.BLOCK_ENTITIES.register("foundry_heat_port",
                    key -> BlockEntityTypeCompat.create(FoundryHeatPortBlockEntity::new,
                            FoundryContent.FOUNDRY_HEAT_PORT.get()));

    // --- menus -----------------------------------------------------------------------------------

    public static final RegistryEntry<MenuType<ProcessingMenu>> BLAST_FOUNDRY_MENU = ModMenuTypes.MENUS.register(
            "blast_foundry", key -> new MenuType<>((id, inv) -> new ProcessingMenu(FoundryContent.BLAST_FOUNDRY_MENU,
                    id, inv, BlastFoundryBlockEntity.LAYOUT), FeatureFlags.VANILLA_SET));

    private FoundryContent() {
    }

    /** Force class-load so the static registrations run (called once by {@code FamilyRegistry.init()}). */
    public static void init() {
    }

    /**
     * This family's block-entity types per capability surface (energy / item / gas / fluid). The heat
     * port is not a machine (no energy, items or tanks); heat reaches it through its own API.
     */
    public static List<Supplier<BlockEntityType<? extends NeroTechMachineBlockEntity>>> machineTypes(
            MachineTypeRegistry.Surface surface) {
        return switch (surface) {
            case ENERGY -> List.of(BLAST_FOUNDRY_BE::get);
            case ITEM -> List.of(BLAST_FOUNDRY_BE::get);
            case GAS -> List.of();
            case FLUID -> List.of();
        };
    }

    /** Client only: register this family's menu screens ({@code sink.register(MENU.get(), Screen::new)}). */
    public static void screens(ScreenSink sink) {
        sink.<ProcessingMenu, BlastFoundryScreen>register(BLAST_FOUNDRY_MENU.get(), BlastFoundryScreen::create);
    }

    /** Client only: register this family's renderers ({@code sink.add(TYPE.get(), Renderer::new)}). */
    public static void renderers(RendererSink sink) {
        sink.add(BLAST_FOUNDRY_BE.get(), za.co.neroland.nerotech.client.render.BlastFoundryRenderer::new);
        sink.add(FOUNDRY_HEAT_PORT_BE.get(), za.co.neroland.nerotech.client.render.FoundryHeatPortRenderer::new);
    }

    /** This family's creative-tab entries, in display order. */
    public static List<ItemLike> creativeContents() {
        return List.of(BLAST_FOUNDRY_ITEM.get(), FOUNDRY_BRICK_ITEM.get(), FOUNDRY_HEAT_PORT_ITEM.get());
    }
}
