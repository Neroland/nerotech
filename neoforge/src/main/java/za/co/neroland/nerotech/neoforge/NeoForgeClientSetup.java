package za.co.neroland.nerotech.neoforge;

import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

import za.co.neroland.nerotech.client.AnalyticsTerminalScreen;
import za.co.neroland.nerotech.client.AutoCrafterScreen;
import za.co.neroland.nerotech.client.ClientBlockEntityRenderers;
import za.co.neroland.nerotech.client.FabricatorScreen;
import za.co.neroland.nerotech.client.ItemSorterScreen;
import za.co.neroland.nerotech.client.NeroGeneratorScreen;
import za.co.neroland.nerotech.client.OreProcessorScreen;
import za.co.neroland.nerotech.client.RemediatorScreen;
import za.co.neroland.nerotech.client.ScrubberScreen;
import za.co.neroland.nerotech.client.SolarArrayScreen;
import za.co.neroland.nerotech.client.TechGuideScreen;
import za.co.neroland.nerotech.registry.ModMenuTypes;

/** NeoForge client-only wiring (machine screens + block-entity renderers). Loaded only behind Dist.CLIENT. */
public final class NeoForgeClientSetup {

    private NeoForgeClientSetup() {
    }

    public static void init(IEventBus modEventBus) {
        modEventBus.addListener(NeoForgeClientSetup::onRegisterScreens);
        modEventBus.addListener(NeoForgeClientSetup::onRegisterEntityRenderers);
    }

    /** Machine BERs through the shared cross-loader seam (Nerospace pattern). */
    private static void onRegisterEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        ClientBlockEntityRenderers.registerAll(new ClientBlockEntityRenderers.Sink() {
            @Override
            public <T extends BlockEntity, S extends BlockEntityRenderState> void register(
                    BlockEntityType<? extends T> type, BlockEntityRendererProvider<T, S> provider) {
                event.registerBlockEntityRenderer(type, provider);
            }
        });
    }

    private static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.NERO_GENERATOR.get(), NeroGeneratorScreen::new);
        event.register(ModMenuTypes.SOLAR_ARRAY.get(), SolarArrayScreen::new);
        event.register(ModMenuTypes.ORE_PROCESSOR.get(), OreProcessorScreen::new);
        event.register(ModMenuTypes.FABRICATOR.get(), FabricatorScreen::new);
        event.register(ModMenuTypes.AUTO_CRAFTER.get(), AutoCrafterScreen::new);
        event.register(ModMenuTypes.ITEM_SORTER.get(), ItemSorterScreen::new);
        event.register(ModMenuTypes.SCRUBBER.get(), ScrubberScreen::new);
        event.register(ModMenuTypes.REMEDIATOR.get(), RemediatorScreen::new);
        event.register(ModMenuTypes.ANALYTICS_TERMINAL.get(), AnalyticsTerminalScreen::new);
        event.register(ModMenuTypes.TECH_GUIDE.get(), TechGuideScreen::new);
    }
}
