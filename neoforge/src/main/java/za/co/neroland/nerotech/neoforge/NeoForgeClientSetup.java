package za.co.neroland.nerotech.neoforge;

import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RecipesReceivedEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.NeoForge;

import za.co.neroland.nerotech.client.AnalyticsTerminalScreen;
import za.co.neroland.nerotech.client.AutoCrafterScreen;
import za.co.neroland.nerotech.client.BatteryBankScreen;
import za.co.neroland.nerotech.client.BioGeneratorScreen;
import za.co.neroland.nerotech.client.ChemicalProcessorScreen;
import za.co.neroland.nerotech.client.ClientBlockEntityRenderers;
import za.co.neroland.nerotech.client.ColliderScreen;
import za.co.neroland.nerotech.client.ElectrolyzerScreen;
import za.co.neroland.nerotech.client.FabricatorScreen;
import za.co.neroland.nerotech.client.GasTurbineScreen;
import za.co.neroland.nerotech.client.GeothermalGeneratorScreen;
import za.co.neroland.nerotech.client.GridControllerScreen;
import za.co.neroland.nerotech.client.ItemSorterScreen;
import za.co.neroland.nerotech.client.NeroGeneratorScreen;
import za.co.neroland.nerotech.client.OreProcessorScreen;
import za.co.neroland.nerotech.client.RemediatorScreen;
import za.co.neroland.nerotech.client.RoboticArmScreen;
import za.co.neroland.nerotech.client.ScrubberScreen;
import za.co.neroland.nerotech.client.SolarArrayScreen;
import za.co.neroland.nerotech.client.TechGuideScreen;
import za.co.neroland.nerotech.client.WindTurbineScreen;
import za.co.neroland.nerotech.client.WirelessNodeScreen;
import za.co.neroland.nerotech.compat.jei.JeiSyncedRecipes;
import za.co.neroland.nerotech.registry.ModMenuTypes;

/** NeoForge client-only wiring (machine screens + block-entity renderers). Loaded only behind Dist.CLIENT. */
public final class NeoForgeClientSetup {

    private NeoForgeClientSetup() {
    }

    public static void init(IEventBus modEventBus) {
        modEventBus.addListener(NeoForgeClientSetup::onRegisterScreens);
        modEventBus.addListener(NeoForgeClientSetup::onRegisterEntityRenderers);
        // Keep the client's copy of the server's synced recipes so recipe viewers (compat.jei) can
        // list NeroTech's machine recipes — 26.x clients hold no full recipe list of their own.
        NeoForge.EVENT_BUS.addListener((RecipesReceivedEvent event) ->
                JeiSyncedRecipes.accept(event.getRecipeMap()));
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
        event.register(ModMenuTypes.COLLIDER.get(), ColliderScreen::new);
        event.register(ModMenuTypes.ELECTROLYZER.get(), ElectrolyzerScreen::new);
        event.register(ModMenuTypes.GAS_TURBINE.get(), GasTurbineScreen::new);
        event.register(ModMenuTypes.CHEMICAL_PROCESSOR.get(), ChemicalProcessorScreen::new);
        event.register(ModMenuTypes.WIND_TURBINE.get(), WindTurbineScreen::new);
        event.register(ModMenuTypes.GEOTHERMAL_GENERATOR.get(), GeothermalGeneratorScreen::new);
        event.register(ModMenuTypes.BIO_GENERATOR.get(), BioGeneratorScreen::new);
        event.register(ModMenuTypes.BATTERY_BANK.get(), BatteryBankScreen::new);
        event.register(ModMenuTypes.GRID_CONTROLLER.get(), GridControllerScreen::new);
        event.register(ModMenuTypes.WIRELESS_NODE.get(), WirelessNodeScreen::new);
        event.register(ModMenuTypes.AUTO_CRAFTER.get(), AutoCrafterScreen::new);
        event.register(ModMenuTypes.ITEM_SORTER.get(), ItemSorterScreen::new);
        event.register(ModMenuTypes.ROBOTIC_ARM.get(), RoboticArmScreen::new);
        event.register(ModMenuTypes.SCRUBBER.get(), ScrubberScreen::new);
        event.register(ModMenuTypes.REMEDIATOR.get(), RemediatorScreen::new);
        event.register(ModMenuTypes.ANALYTICS_TERMINAL.get(), AnalyticsTerminalScreen::new);
        event.register(ModMenuTypes.TECH_GUIDE.get(), TechGuideScreen::new);
    }
}
