package za.co.neroland.nerotech.client;

import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import za.co.neroland.nerotech.client.render.AutoCrafterRenderer;
import za.co.neroland.nerotech.client.render.FabricatorRenderer;
import za.co.neroland.nerotech.client.render.FusionReactorRenderer;
import za.co.neroland.nerotech.client.render.ItemSorterRenderer;
import za.co.neroland.nerotech.client.render.NeroGeneratorRenderer;
import za.co.neroland.nerotech.client.render.OreProcessorRenderer;
import za.co.neroland.nerotech.client.render.RemediatorRenderer;
import za.co.neroland.nerotech.client.render.ScrubberRenderer;
import za.co.neroland.nerotech.client.render.SolarArrayRenderer;
import za.co.neroland.nerotech.registry.ModBlockEntities;

/**
 * Cross-loader block-entity-renderer wiring (Nerospace's proven Sink seam). The renderer set is
 * identical on every loader, so it lives here once and each loader passes its own registration
 * function ({@link Sink}) — NeoForge's and Forge's {@code EntityRenderersEvent.RegisterRenderers}
 * ({@code registerBlockEntityRenderer}), Fabric's {@code BlockEntityRendererRegistry.register}.
 * Common stays loader-import-free.
 */
public final class ClientBlockEntityRenderers {

    /** A loader's BER-registration entry point. */
    public interface Sink {
        <T extends BlockEntity, S extends BlockEntityRenderState> void register(
                BlockEntityType<? extends T> type, BlockEntityRendererProvider<T, S> provider);
    }

    private ClientBlockEntityRenderers() {
    }

    public static void registerAll(Sink sink) {
        // Nero Generator: turbine ring behind the intake grille + heat-lerped glow.
        sink.register(ModBlockEntities.NERO_GENERATOR.get(), context -> new NeroGeneratorRenderer());
        // Solar Array: the sun-tracking deck above the pedestal, folded flat at night.
        sink.register(ModBlockEntities.SOLAR_ARRAY.get(), context -> new SolarArrayRenderer());
        // Ore Processors: twin crusher drums (the Advanced BE adds the plasma arc) — one renderer, two types.
        sink.register(ModBlockEntities.ORE_PROCESSOR.get(), context -> new OreProcessorRenderer());
        sink.register(ModBlockEntities.ADVANCED_ORE_PROCESSOR.get(), context -> new OreProcessorRenderer());
        // Fabricators: the eased traversal arm (the Advanced BE adds a second arm + crystal) — shared too.
        sink.register(ModBlockEntities.FABRICATOR.get(), context -> new FabricatorRenderer());
        sink.register(ModBlockEntities.ADVANCED_FABRICATOR.get(), context -> new FabricatorRenderer());
        // Fusion Reactor: the plasma torus in the viewport + the meltdown warning strobe.
        sink.register(ModBlockEntities.FUSION_REACTOR.get(), context -> new FusionReactorRenderer());
        // Auto Crafter: the locked-template hologram + press-stamp pulse.
        sink.register(ModBlockEntities.AUTO_CRAFTER.get(), context -> new AutoCrafterRenderer());
        // Item Sorter: six side-config-tinted port caps + the sort-pulse brightening.
        sink.register(ModBlockEntities.ITEM_SORTER.get(), context -> new ItemSorterRenderer());
        // Scrubber: intake fan cross + fouling-darkened filter cartridge + heat-lerped LEDs.
        sink.register(ModBlockEntities.SCRUBBER.get(), context -> new ScrubberRenderer());
        // Remediator: BER-drawn spray booms (sweeping while active) + faint plasma mist.
        sink.register(ModBlockEntities.REMEDIATOR.get(), context -> new RemediatorRenderer());
    }
}
