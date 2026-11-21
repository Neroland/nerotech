package za.co.neroland.nerotech.machine.heat;

import java.util.List;
import java.util.function.Supplier;

import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
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
import za.co.neroland.nerotech.registry.ModItems;
import za.co.neroland.nerotech.registry.ModMenuTypes;
import za.co.neroland.nerotech.registry.RegistrationProvider.RegistryEntry;

/**
 * WP-Heat — the heat machine family (heat_pipe, heat_exchanger). Owned by its work package: every block, item,
 * block-entity type, menu, screen and renderer of the family is declared here and nowhere else;
 * {@code registry.FamilyRegistry} wires this class into every central place.
 *
 * <p>The Heat Pipe has no block entity (decision 012): its runs are cached in {@link HeatPipeNetwork}.
 */
public final class HeatContent {

    // --- blocks ---------------------------------------------------------------------------------------

    public static final RegistryEntry<HeatPipeBlock> HEAT_PIPE =
            ModBlocks.machine("heat_pipe", p -> p.strength(2.0F), HeatPipeBlock::new);
    public static final RegistryEntry<HeatExchangerBlock> HEAT_EXCHANGER =
            ModBlocks.machine("heat_exchanger", HeatExchangerBlock::new);

    // --- items ----------------------------------------------------------------------------------------

    public static final RegistryEntry<BlockItem> HEAT_PIPE_ITEM =
            ModItems.tooltipBlockItem("heat_pipe", HEAT_PIPE, "block.nerotech.heat_pipe.tooltip");
    public static final RegistryEntry<BlockItem> HEAT_EXCHANGER_ITEM =
            ModItems.tooltipBlockItem("heat_exchanger", HEAT_EXCHANGER, "block.nerotech.heat_exchanger.tooltip");

    // --- block-entity types ---------------------------------------------------------------------------

    public static final RegistryEntry<BlockEntityType<HeatExchangerBlockEntity>> HEAT_EXCHANGER_BE =
            ModBlockEntities.BLOCK_ENTITIES.register("heat_exchanger",
                    key -> BlockEntityTypeCompat.create(HeatExchangerBlockEntity::new,
                            HeatContent.HEAT_EXCHANGER.get()));

    // --- menus ----------------------------------------------------------------------------------------

    public static final RegistryEntry<MenuType<ProcessingMenu>> HEAT_EXCHANGER_MENU =
            ModMenuTypes.MENUS.register("heat_exchanger",
                    key -> new MenuType<>((id, inv) -> new ProcessingMenu(HeatContent.HEAT_EXCHANGER_MENU, id,
                            inv, HeatExchangerBlockEntity.LAYOUT), FeatureFlags.VANILLA_SET));

    private HeatContent() {
    }

    /** Force class-load so the static registrations run (called once by {@code FamilyRegistry.init()}). */
    public static void init() {
    }

    /** This family's block-entity types per capability surface (energy / item / gas / fluid). */
    public static List<Supplier<BlockEntityType<? extends NeroTechMachineBlockEntity>>> machineTypes(
            MachineTypeRegistry.Surface surface) {
        return switch (surface) {
            // Energy: the shared machine surface (side config, upgrades); the exchanger itself draws none.
            case ENERGY, ITEM -> List.of(HEAT_EXCHANGER_BE::get);
            case GAS -> List.of();
            case FLUID -> List.of();
        };
    }

    /** Client only: register this family's menu screens ({@code sink.register(MENU.get(), Screen::new)}). */
    public static void screens(ScreenSink sink) {
        sink.<ProcessingMenu, HeatExchangerScreen>register(HEAT_EXCHANGER_MENU.get(), HeatExchangerScreen::create);
    }

    /** Client only: register this family's renderers ({@code sink.add(TYPE.get(), Renderer::new)}). */
    public static void renderers(RendererSink sink) {
        sink.add(HEAT_EXCHANGER_BE.get(), za.co.neroland.nerotech.client.render.HeatExchangerRenderer::new);
        // The Heat Pipe has no block entity and no BER.
    }

    /** This family's creative-tab entries, in display order. */
    public static List<ItemLike> creativeContents() {
        return List.of(HEAT_PIPE_ITEM.get(), HEAT_EXCHANGER_ITEM.get());
    }
}
