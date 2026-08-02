package za.co.neroland.nerotech.forge;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

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
        MenuScreens.register(ModMenuTypes.COLLIDER.get(), ColliderScreen::new);
        MenuScreens.register(ModMenuTypes.ELECTROLYZER.get(), ElectrolyzerScreen::new);
        MenuScreens.register(ModMenuTypes.GAS_TURBINE.get(), GasTurbineScreen::new);
        MenuScreens.register(ModMenuTypes.CHEMICAL_PROCESSOR.get(), ChemicalProcessorScreen::new);
        MenuScreens.register(ModMenuTypes.WIND_TURBINE.get(), WindTurbineScreen::new);
        MenuScreens.register(ModMenuTypes.GEOTHERMAL_GENERATOR.get(), GeothermalGeneratorScreen::new);
        MenuScreens.register(ModMenuTypes.BIO_GENERATOR.get(), BioGeneratorScreen::new);
        MenuScreens.register(ModMenuTypes.BATTERY_BANK.get(), BatteryBankScreen::new);
        MenuScreens.register(ModMenuTypes.GRID_CONTROLLER.get(), GridControllerScreen::new);
        MenuScreens.register(ModMenuTypes.WIRELESS_NODE.get(), WirelessNodeScreen::new);
        MenuScreens.register(ModMenuTypes.AUTO_CRAFTER.get(), AutoCrafterScreen::new);
        MenuScreens.register(ModMenuTypes.ITEM_SORTER.get(), ItemSorterScreen::new);
        MenuScreens.register(ModMenuTypes.ROBOTIC_ARM.get(), RoboticArmScreen::new);
        MenuScreens.register(ModMenuTypes.SCRUBBER.get(), ScrubberScreen::new);
        MenuScreens.register(ModMenuTypes.REMEDIATOR.get(), RemediatorScreen::new);
        MenuScreens.register(ModMenuTypes.ANALYTICS_TERMINAL.get(), AnalyticsTerminalScreen::new);
        MenuScreens.register(ModMenuTypes.TECH_GUIDE.get(), TechGuideScreen::new);
    }
}
