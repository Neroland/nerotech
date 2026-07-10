package za.co.neroland.nerotech.forge;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import za.co.neroland.nerotech.client.AutoCrafterScreen;
import za.co.neroland.nerotech.client.ClientBlockEntityRenderers;
import za.co.neroland.nerotech.client.FabricatorScreen;
import za.co.neroland.nerotech.client.ItemSorterScreen;
import za.co.neroland.nerotech.client.NeroGeneratorScreen;
import za.co.neroland.nerotech.client.OreProcessorScreen;
import za.co.neroland.nerotech.client.RemediatorScreen;
import za.co.neroland.nerotech.client.ScrubberScreen;
import za.co.neroland.nerotech.client.SolarArrayScreen;
import za.co.neroland.nerotech.registry.ModMenuTypes;

/** Forge client-only wiring (machine screens + block-entity renderers). */
public final class ForgeClientSetup {

    private ForgeClientSetup() {
    }

    public static void init(BusGroup modBusGroup) {
        FMLClientSetupEvent.getBus(modBusGroup).addListener(ForgeClientSetup::onClientSetup);
        EntityRenderersEvent.RegisterRenderers.BUS.addListener(ForgeClientSetup::onRegisterEntityRenderers);
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

    private static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(ForgeClientSetup::registerScreens);
    }

    private static void registerScreens() {
        MenuScreens.register(ModMenuTypes.NERO_GENERATOR.get(), NeroGeneratorScreen::new);
        MenuScreens.register(ModMenuTypes.SOLAR_ARRAY.get(), SolarArrayScreen::new);
        MenuScreens.register(ModMenuTypes.ORE_PROCESSOR.get(), OreProcessorScreen::new);
        MenuScreens.register(ModMenuTypes.FABRICATOR.get(), FabricatorScreen::new);
        MenuScreens.register(ModMenuTypes.AUTO_CRAFTER.get(), AutoCrafterScreen::new);
        MenuScreens.register(ModMenuTypes.ITEM_SORTER.get(), ItemSorterScreen::new);
        MenuScreens.register(ModMenuTypes.SCRUBBER.get(), ScrubberScreen::new);
        MenuScreens.register(ModMenuTypes.REMEDIATOR.get(), RemediatorScreen::new);
    }
}
