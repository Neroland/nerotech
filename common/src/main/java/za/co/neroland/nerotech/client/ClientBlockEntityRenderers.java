package za.co.neroland.nerotech.client;

import java.util.function.Supplier;

import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import za.co.neroland.nerotech.client.render.AnalyticsTerminalRenderer;
import za.co.neroland.nerotech.client.render.AutoCrafterRenderer;
import za.co.neroland.nerotech.client.render.FabricatorRenderer;
import za.co.neroland.nerotech.client.render.FusionReactorRenderer;
import za.co.neroland.nerotech.client.render.ItemSorterRenderer;
import za.co.neroland.nerotech.client.render.NeroGeneratorRenderer;
import za.co.neroland.nerotech.client.render.OreProcessorRenderer;
import za.co.neroland.nerotech.client.render.RemediatorRenderer;
import za.co.neroland.nerotech.client.render.ScrubberRenderer;
import za.co.neroland.nerotech.client.render.SolarArrayRenderer;
import za.co.neroland.nerotech.client.render.TechGuideHologramRenderer;
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
        //? if >=1.21.9 {
        <T extends BlockEntity, S extends BlockEntityRenderState> void register(
                BlockEntityType<? extends T> type, BlockEntityRendererProvider<T, S> provider);
        //?} else {
        /*<T extends BlockEntity> void register(
                BlockEntityType<? extends T> type, BlockEntityRendererProvider<T> provider);
        *///?}
    }

    private ClientBlockEntityRenderers() {
    }

    /**
     * Registers one renderer. Before Minecraft 1.21.9 there is no render-state renderer API, so the
     * renderer (written against the client.render.legacy look-alikes there) is wrapped in an adapter.
     */
    private static <T extends BlockEntity, S extends BlockEntityRenderState> void add(Sink sink,
            BlockEntityType<? extends T> type, Supplier<? extends BlockEntityRenderer<T, S>> renderer) {
        //? if >=1.21.9 {
        sink.register(type, context -> renderer.get());
        //?} else {
        /*sink.register(type, context -> new za.co.neroland.nerotech.client.render.legacy.LegacyBlockEntityRenderer<T, S>(renderer.get()));
        *///?}
    }

    public static void registerAll(Sink sink) {
        // Nero Generator: turbine ring behind the intake grille + heat-lerped glow.
        add(sink, ModBlockEntities.NERO_GENERATOR.get(), NeroGeneratorRenderer::new);
        // Solar Array: the sun-tracking deck above the pedestal, folded flat at night.
        add(sink, ModBlockEntities.SOLAR_ARRAY.get(), SolarArrayRenderer::new);
        // Ore Processors: twin crusher drums (the Advanced BE adds the plasma arc) — one renderer, two types.
        add(sink, ModBlockEntities.ORE_PROCESSOR.get(), OreProcessorRenderer::new);
        add(sink, ModBlockEntities.ADVANCED_ORE_PROCESSOR.get(), OreProcessorRenderer::new);
        // Fabricators: the eased traversal arm (the Advanced BE adds a second arm + crystal) — shared too.
        add(sink, ModBlockEntities.FABRICATOR.get(), FabricatorRenderer::new);
        add(sink, ModBlockEntities.ADVANCED_FABRICATOR.get(), FabricatorRenderer::new);
        // Fusion Reactor: the plasma torus in the viewport + the meltdown warning strobe.
        add(sink, ModBlockEntities.FUSION_REACTOR.get(), FusionReactorRenderer::new);
        // Auto Crafter: the locked-template hologram + press-stamp pulse.
        add(sink, ModBlockEntities.AUTO_CRAFTER.get(), AutoCrafterRenderer::new);
        // Item Sorter: six side-config-tinted port caps + the sort-pulse brightening.
        add(sink, ModBlockEntities.ITEM_SORTER.get(), ItemSorterRenderer::new);
        // Scrubber: intake fan cross + fouling-darkened filter cartridge + heat-lerped LEDs.
        add(sink, ModBlockEntities.SCRUBBER.get(), ScrubberRenderer::new);
        // Remediator: BER-drawn spray booms (sweeping while active) + faint plasma mist.
        add(sink, ModBlockEntities.REMEDIATOR.get(), RemediatorRenderer::new);
        // Analytics Terminal: faint holographic shimmer above the console while machines are in range.
        add(sink, ModBlockEntities.ANALYTICS_TERMINAL.get(), AnalyticsTerminalRenderer::new);
        // Tech Guide: the spinning next-step hologram above a loaded pedestal (Star Guide recipe).
        add(sink, ModBlockEntities.TECH_GUIDE.get(), TechGuideHologramRenderer::new);
    }
}
