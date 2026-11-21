package za.co.neroland.nerotech.machine.fusion;

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
import za.co.neroland.nerotech.machine.fusion.client.FuelInjectorScreen;
import za.co.neroland.nerotech.registry.ModBlockEntities;
import za.co.neroland.nerotech.registry.ModBlocks;
import za.co.neroland.nerotech.registry.ModItems;
import za.co.neroland.nerotech.registry.ModMenuTypes;
import za.co.neroland.nerotech.registry.RegistrationProvider.RegistryEntry;

/**
 * The fusion machine family (fuel_injector, europium_flux_lattice). Every block, item,
 * block-entity type, menu, screen and renderer of the family is declared here and nowhere else;
 * {@code registry.FamilyRegistry} wires this class into every central place.
 *
 * <p>Declare static final entries in this order: blocks ({@code ModBlocks.machine(...)}), block items
 * ({@code ModItems.familyBlockItem(...)}), block-entity types ({@code ModBlockEntities.BLOCK_ENTITIES}),
 * menus ({@code ModMenuTypes.MENUS}). They register when this class loads.
 */
public final class FusionContent {

    // --- blocks -------------------------------------------------------------------------------------

    /** A reactor shell port feeding deuterium and helium-3: the gas-fed top fuel tier. */
    public static final RegistryEntry<FuelInjectorBlock> FUEL_INJECTOR =
            ModBlocks.machine("fuel_injector", FuelInjectorBlock::new);
    /** A shell block that widens the reactor's stable heat band, up to a cap. No block entity. */
    public static final RegistryEntry<EuropiumFluxLatticeBlock> EUROPIUM_FLUX_LATTICE =
            ModBlocks.machine("europium_flux_lattice", p -> p, EuropiumFluxLatticeBlock::new);

    // --- block items --------------------------------------------------------------------------------

    public static final RegistryEntry<BlockItem> FUEL_INJECTOR_ITEM =
            ModItems.tooltipBlockItem("fuel_injector", FUEL_INJECTOR, "block.nerotech.fuel_injector.tooltip");
    public static final RegistryEntry<BlockItem> EUROPIUM_FLUX_LATTICE_ITEM =
            ModItems.tooltipBlockItem("europium_flux_lattice", EUROPIUM_FLUX_LATTICE,
                    "block.nerotech.europium_flux_lattice.tooltip");

    // --- block-entity types -------------------------------------------------------------------------

    public static final RegistryEntry<BlockEntityType<FuelInjectorBlockEntity>> FUEL_INJECTOR_BE =
            ModBlockEntities.BLOCK_ENTITIES.register("fuel_injector",
                    key -> BlockEntityTypeCompat.create(FuelInjectorBlockEntity::new,
                            FusionContent.FUEL_INJECTOR.get()));

    // --- menus --------------------------------------------------------------------------------------

    public static final RegistryEntry<MenuType<FuelInjectorMenu>> FUEL_INJECTOR_MENU =
            ModMenuTypes.MENUS.register("fuel_injector",
                    key -> new MenuType<>(FuelInjectorMenu::new, FeatureFlags.VANILLA_SET));

    private FusionContent() {
    }

    /** Force class-load so the static registrations run (called once by {@code FamilyRegistry.init()}). */
    public static void init() {
    }

    /**
     * This family's block-entity types per capability surface (energy / item / gas / fluid). The injector
     * is gas only: no power, no item slot (so no cell, fuel rod or pellet can be fed to it).
     */
    public static List<Supplier<BlockEntityType<? extends NeroTechMachineBlockEntity>>> machineTypes(
            MachineTypeRegistry.Surface surface) {
        return switch (surface) {
            case ENERGY -> List.of();
            case ITEM -> List.of();
            case GAS -> List.of(FUEL_INJECTOR_BE::get);
            case FLUID -> List.of();
        };
    }

    /** Client only: register this family's menu screens ({@code sink.register(MENU.get(), Screen::new)}). */
    public static void screens(ScreenSink sink) {
        sink.register(FUEL_INJECTOR_MENU.get(), FuelInjectorScreen::new);
    }

    /** Client only: register this family's renderers ({@code sink.add(TYPE.get(), Renderer::new)}). */
    public static void renderers(RendererSink sink) {
        sink.add(FUEL_INJECTOR_BE.get(), za.co.neroland.nerotech.client.render.FuelInjectorRenderer::new);
    }

    /** This family's creative-tab entries, in display order. */
    public static List<ItemLike> creativeContents() {
        return List.of(FUEL_INJECTOR_ITEM.get(), EUROPIUM_FLUX_LATTICE_ITEM.get());
    }
}
