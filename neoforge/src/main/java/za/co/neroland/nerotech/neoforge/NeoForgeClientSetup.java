package za.co.neroland.nerotech.neoforge;

//? if >=26.1 {
import net.minecraft.client.renderer.block.FluidModel;
//?}
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
//? if >=26.1 {
import net.minecraft.client.resources.model.sprite.Material;
//?}
import net.minecraft.resources.Identifier;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
//? if >=1.21.4 {
import net.neoforged.neoforge.client.event.RecipesReceivedEvent;
//?}
//? if >=26.1 {
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;
//?} else if >=1.21 {
/*import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
*///?}
//? if >=1.20.4 {
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
//?}
//? if >=1.21.4 {
import net.neoforged.neoforge.common.NeoForge;
//?}

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
import za.co.neroland.nerotech.NeroTechCommon;
//? if >=1.21.4 {
import za.co.neroland.nerotech.compat.jei.JeiSyncedRecipes;
//?}
//? if >=26.1 {
import za.co.neroland.nerotech.fluid.NeroTechFluids;
//?}
import za.co.neroland.nerotech.registry.ModMenuTypes;

/** NeoForge client-only wiring (machine screens + block-entity renderers). Loaded only behind Dist.CLIENT. */
public final class NeoForgeClientSetup {

    private NeoForgeClientSetup() {
    }

    public static void init(IEventBus modEventBus) {
        //? if >=1.20.4 {
        modEventBus.addListener((RegisterMenuScreensEvent event) -> registerScreens(new za.co.neroland.nerotech.client.ScreenSink() {
            @Override
            public <M extends net.minecraft.world.inventory.AbstractContainerMenu,
                    U extends net.minecraft.client.gui.screens.Screen
                            & net.minecraft.client.gui.screens.inventory.MenuAccess<M>> void register(
                    net.minecraft.world.inventory.MenuType<? extends M> type,
                    net.minecraft.client.gui.screens.MenuScreens.ScreenConstructor<M, U> constructor) {
                event.register(type, constructor);
            }
        }));
        //?} else {
        /*// NeoForge 20.2 / 20.3 have no RegisterMenuScreensEvent: register on the client-setup work queue.
        modEventBus.addListener((net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) -> event.enqueueWork(() ->
                registerScreens(new za.co.neroland.nerotech.client.ScreenSink() {
            @Override
            public <M extends net.minecraft.world.inventory.AbstractContainerMenu,
                    U extends net.minecraft.client.gui.screens.Screen
                            & net.minecraft.client.gui.screens.inventory.MenuAccess<M>> void register(
                    net.minecraft.world.inventory.MenuType<? extends M> type,
                    net.minecraft.client.gui.screens.MenuScreens.ScreenConstructor<M, U> constructor) {
                net.minecraft.client.gui.screens.MenuScreens.register(type, constructor);
            }
        })));
        *///?}
        modEventBus.addListener(NeoForgeClientSetup::onRegisterEntityRenderers);
        modEventBus.addListener((net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent event) ->
                za.co.neroland.nerotech.client.particle.NeroTechParticles.registerAll(
                        (type, factory) -> event.registerSpriteSet(type, factory::apply)));
        //? if >=26.1 {
        modEventBus.addListener(NeoForgeClientSetup::onRegisterFluidModels);
        //?} else if >=1.21 {
        /*// Before 26.1 the gas sprites come from the fluid types' client extensions.
        modEventBus.addListener(NeoForgeClientSetup::onRegisterClientExtensions);
        *///?}
        // Keep the client's copy of the server's synced recipes so recipe viewers (compat.jei) can
        // list NeroTech's machine recipes — from 1.21.2 clients hold no full recipe list of their own
        // (1.21.1 still receives every recipe; JeiSyncedRecipes reads the client level there).
        //? if >=1.21.4 {
        NeoForge.EVENT_BUS.addListener((RecipesReceivedEvent event) ->
                JeiSyncedRecipes.accept(event.getRecipeMap()));
        //?}
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

    private static void registerScreens(za.co.neroland.nerotech.client.ScreenSink sink) {
        sink.register(ModMenuTypes.NERO_GENERATOR.get(), NeroGeneratorScreen::new);
        sink.register(ModMenuTypes.SOLAR_ARRAY.get(), SolarArrayScreen::new);
        sink.register(ModMenuTypes.ORE_PROCESSOR.get(), OreProcessorScreen::new);
        sink.register(ModMenuTypes.FABRICATOR.get(), FabricatorScreen::new);
        sink.register(ModMenuTypes.COLLIDER.get(), ColliderScreen::new);
        sink.register(ModMenuTypes.ELECTROLYZER.get(), ElectrolyzerScreen::new);
        sink.register(ModMenuTypes.GAS_TURBINE.get(), GasTurbineScreen::new);
        sink.register(ModMenuTypes.CHEMICAL_PROCESSOR.get(), ChemicalProcessorScreen::new);
        sink.register(ModMenuTypes.WIND_TURBINE.get(), WindTurbineScreen::new);
        sink.register(ModMenuTypes.GEOTHERMAL_GENERATOR.get(), GeothermalGeneratorScreen::new);
        sink.register(ModMenuTypes.BIO_GENERATOR.get(), BioGeneratorScreen::new);
        sink.register(ModMenuTypes.BATTERY_BANK.get(), BatteryBankScreen::new);
        sink.register(ModMenuTypes.GRID_CONTROLLER.get(), GridControllerScreen::new);
        sink.register(ModMenuTypes.WIRELESS_NODE.get(), WirelessNodeScreen::new);
        sink.register(ModMenuTypes.AUTO_CRAFTER.get(), AutoCrafterScreen::new);
        sink.register(ModMenuTypes.ITEM_SORTER.get(), ItemSorterScreen::new);
        sink.register(ModMenuTypes.ROBOTIC_ARM.get(), RoboticArmScreen::new);
        sink.register(ModMenuTypes.SCRUBBER.get(), ScrubberScreen::new);
        sink.register(ModMenuTypes.REMEDIATOR.get(), RemediatorScreen::new);
        sink.register(ModMenuTypes.ANALYTICS_TERMINAL.get(), AnalyticsTerminalScreen::new);
        sink.register(ModMenuTypes.TECH_GUIDE.get(), TechGuideScreen::new);
        // 0.8.0 machine families: each family's Content.screens(...) registers its own screens.
        za.co.neroland.nerotech.registry.FamilyRegistry.screens(sink);
    }

    /**
     * Sprites for the gas transport fluids. A gas is never placed in the world, so these only show up
     * in other mods' tank and pipe GUIs — but a fluid with no model draws the missing texture there
     * (and NeoForge logs a warning for it at bake time). Sprites only: the fog and overlay extensions
     * are for a camera inside the fluid, which a gas with no fluid block can never manage.
     */
    //? if >=26.1 {
    private static void onRegisterFluidModels(RegisterFluidModelsEvent event) {
        event.register(gasFluidModel("hydrogen"), NeroTechFluids.HYDROGEN.get());
        event.register(gasFluidModel("oxygen"), NeroTechFluids.OXYGEN.get());
        // 0.8.0 gases: transport identities, sprites only.
        for (Identifier gas : za.co.neroland.nerotech.gas.NeroTechGases.ALL) {
            if (!gas.equals(za.co.neroland.nerotech.gas.NeroTechGases.HYDROGEN)
                    && !gas.equals(za.co.neroland.nerotech.gas.NeroTechGases.OXYGEN)) {
                event.register(gasFluidModel(gas.getPath()), NeroTechFluids.fluidFor(gas));
            }
        }
        // 0.8.0 liquids: placeable, so the model covers both the source and the flowing fluid. Block
        // state JSON does nothing for fluids on 26.x; this registration is what renders them.
        for (za.co.neroland.nerotech.fluid.NeroTechLiquid liquid : za.co.neroland.nerotech.fluid.NeroTechLiquid.values()) {
            Material still = new Material(Identifier.fromNamespaceAndPath(NeroTechCommon.MOD_ID, liquid.stillTexture()));
            Material flow = new Material(Identifier.fromNamespaceAndPath(NeroTechCommon.MOD_ID, liquid.flowTexture()));
            event.register(new FluidModel.Unbaked(still, flow, still,
                            net.neoforged.neoforge.client.fluid.FluidTintSources.constant(0xFFFFFFFF)),
                    liquid.source(), liquid.flowing());
        }
    }

    // Sprites live in textures/block/ so the vanilla block atlas stitches them; a separate
    // textures/fluid/ directory is only stitched if the minecraft:blocks atlas definition lists it.
    private static FluidModel.Unbaked gasFluidModel(String gas) {
        return new FluidModel.Unbaked(
                new Material(Identifier.fromNamespaceAndPath(NeroTechCommon.MOD_ID, "block/" + gas + "_still")),
                new Material(Identifier.fromNamespaceAndPath(NeroTechCommon.MOD_ID, "block/" + gas + "_flow")),
                null,
                null);
    }
    //?} else if >=1.21 {
    /*private static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        NeoForgeFluidTypes.gasTypes().forEach((gas, type) ->
                event.registerFluidType(NeoForgeFluidTypes.textureExtensions(gas.getPath()), type.get()));
        for (za.co.neroland.nerotech.fluid.NeroTechLiquid liquid : za.co.neroland.nerotech.fluid.NeroTechLiquid.values()) {
            event.registerFluidType(NeoForgeFluidTypes.textureExtensions(liquid.id()), NeoForgeFluidTypes.forLiquid(liquid));
        }
    }
    *///?}

}
