package za.co.neroland.nerotech.forge;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.client.event.EntityRenderersEvent;
//? if >=1.21.6 {
import net.minecraftforge.eventbus.api.bus.BusGroup;
//?} else {
/*import net.minecraftforge.eventbus.api.IEventBus;
*///?}
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

    //? if >=1.21.9 {
    public static void init(BusGroup modBusGroup) {
        FMLClientSetupEvent.getBus(modBusGroup).addListener(ForgeClientSetup::onClientSetup);
        EntityRenderersEvent.RegisterRenderers.BUS.addListener(ForgeClientSetup::onRegisterEntityRenderers);
        net.minecraftforge.client.event.RegisterParticleProvidersEvent.BUS.addListener(ForgeClientSetup::onRegisterParticles);
        //? if >=26.1 {
        net.minecraftforge.client.event.ModelEvent.BakeFluidModels.BUS.addListener(ForgeClientSetup::onBakeFluidModels);
        //?}
    }
    //?}
    //? if >=1.21.6 && <1.21.9 {
    /*public static void init(BusGroup modBusGroup) {
        FMLClientSetupEvent.getBus(modBusGroup).addListener(ForgeClientSetup::onClientSetup);
        EntityRenderersEvent.RegisterRenderers.getBus(modBusGroup).addListener(ForgeClientSetup::onRegisterEntityRenderers);
        net.minecraftforge.client.event.RegisterParticleProvidersEvent.getBus(modBusGroup).addListener(ForgeClientSetup::onRegisterParticles);
    }
    *///?}
    //? if <1.21.6 {
    /*public static void init(IEventBus modEventBus) {
        modEventBus.addListener(ForgeClientSetup::onClientSetup);
        modEventBus.addListener(ForgeClientSetup::onRegisterEntityRenderers);
        modEventBus.addListener(ForgeClientSetup::onRegisterParticles);
    }
    *///?}

    /** Machine particle providers (sprite sets from {@code particles/<id>.json}). */
    private static void onRegisterParticles(net.minecraftforge.client.event.RegisterParticleProvidersEvent event) {
        za.co.neroland.nerotech.client.particle.NeroTechParticles.registerAll(
                (type, factory) -> event.registerSpriteSet(type, factory::apply));
    }

    /** Machine BERs through the shared cross-loader seam (Nerospace pattern). */
    private static void onRegisterEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        ClientBlockEntityRenderers.registerAll(new ClientBlockEntityRenderers.Sink() {
            //? if >=1.21.9 {
            @Override
            public <T extends BlockEntity, S extends BlockEntityRenderState> void register(
                    BlockEntityType<? extends T> type, BlockEntityRendererProvider<T, S> provider) {
                event.registerBlockEntityRenderer(type, provider);
            }
            //?} else {
            /*@Override
            public <T extends BlockEntity> void register(
                    BlockEntityType<? extends T> type, BlockEntityRendererProvider<T> provider) {
                event.registerBlockEntityRenderer(type, provider);
            }
            *///?}
        });
    }

    /**
     * From 26.1 Forge takes a fluid's sprites from a baked fluid model, not its type's client extension:
     * NeroTech's gases (transport identities, seen in tank and pipe GUIs) and its three placeable
     * liquids (source and flowing share one model).
     */
    //? if >=26.1 {
    private static void onBakeFluidModels(net.minecraftforge.client.event.ModelEvent.BakeFluidModels event) {
        for (net.minecraft.resources.Identifier gas : za.co.neroland.nerotech.gas.NeroTechGases.ALL) {
            net.minecraft.world.level.material.Fluid fluid = za.co.neroland.nerotech.fluid.NeroTechFluids.fluidFor(gas);
            event.register(fluid, bakeFluidModel(event, "block/" + gas.getPath() + "_still",
                    "block/" + gas.getPath() + "_flow", gas.getPath()));
        }
        for (za.co.neroland.nerotech.fluid.NeroTechLiquid liquid : za.co.neroland.nerotech.fluid.NeroTechLiquid.values()) {
            net.minecraft.client.renderer.block.FluidModel model =
                    bakeFluidModel(event, liquid.stillTexture(), liquid.flowTexture(), liquid.id());
            event.register(liquid.source(), model);
            event.register(liquid.flowing(), model);
        }
    }

    private static net.minecraft.client.renderer.block.FluidModel bakeFluidModel(
            net.minecraftforge.client.event.ModelEvent.BakeFluidModels event, String still, String flow, String name) {
        net.minecraft.client.resources.model.sprite.Material stillMaterial = new net.minecraft.client.resources.model.sprite.Material(
                net.minecraft.resources.Identifier.fromNamespaceAndPath(za.co.neroland.nerotech.NeroTechCommon.MOD_ID, still));
        net.minecraft.client.resources.model.sprite.Material flowMaterial = new net.minecraft.client.resources.model.sprite.Material(
                net.minecraft.resources.Identifier.fromNamespaceAndPath(za.co.neroland.nerotech.NeroTechCommon.MOD_ID, flow));
        return new net.minecraft.client.renderer.block.FluidModel.Unbaked(stillMaterial, flowMaterial, stillMaterial,
                net.minecraft.client.color.block.BlockTintSources.constant(0xFFFFFFFF))
                .bake(event.materials(), () -> za.co.neroland.nerotech.NeroTechCommon.MOD_ID + ":" + name);
    }
    //?}

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
        // 0.8.0 machine families: each family's Content.screens(...) registers its own screens.
        za.co.neroland.nerotech.registry.FamilyRegistry.screens(new za.co.neroland.nerotech.client.ScreenSink() {
            @Override
            public <M extends net.minecraft.world.inventory.AbstractContainerMenu,
                    U extends net.minecraft.client.gui.screens.Screen
                            & net.minecraft.client.gui.screens.inventory.MenuAccess<M>> void register(
                    net.minecraft.world.inventory.MenuType<? extends M> type,
                    net.minecraft.client.gui.screens.MenuScreens.ScreenConstructor<M, U> constructor) {
                MenuScreens.register(type, constructor);
            }
        });
    }
}
