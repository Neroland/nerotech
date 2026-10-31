package za.co.neroland.nerotech.fabric;

import net.fabricmc.api.ClientModInitializer;
//? if >=1.21.9 {
import net.fabricmc.fabric.api.client.recipe.v1.sync.ClientRecipeSynchronizedEvent;
//?}
//? if >=26.1 {
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderingRegistry;
//?} else {
/*import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.fabricmc.fabric.api.client.render.fluid.v1.SimpleFluidRenderHandler;
*///?}
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.minecraft.client.gui.screens.MenuScreens;
//? if >=26.1 {
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;
//?}
import net.minecraft.resources.Identifier;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import za.co.neroland.nerotech.NeroTechCommon;
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
//? if >=1.21.9 {
import za.co.neroland.nerotech.compat.jei.JeiSyncedRecipes;
//?}
import za.co.neroland.nerotech.fluid.NeroTechFluids;
import za.co.neroland.nerotech.registry.ModMenuTypes;

/** Fabric client entry point for NeroTech — registers the machine screens + block-entity renderers. */
public final class NeroTechFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        NeroTechCommon.LOGGER.info("[NeroTech] Fabric client bootstrap");
        registerGasFluidRendering();
        // Clientbound receivers for NeroTech's own payloads (client-only API, so wired here).
        FabricNetwork.registerClient();
        // Keep the client's copy of the server's synced recipes so recipe viewers (compat.jei) can
        // list NeroTech's machine recipes — 26.x clients hold no full recipe list of their own. Fabric API's
        // recipe sync starts at 1.21.9; 1.21.1 still receives every recipe (JeiSyncedRecipes reads the client
        // level there), and on 1.21.2-1.21.8 Fabric clients get no machine recipes to list.
        //? if >=1.21.9 {
        ClientRecipeSynchronizedEvent.EVENT.register((client, synchronizedRecipes) ->
                JeiSyncedRecipes.accept(synchronizedRecipes.recipes()));
        //?}
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

        // Machine BERs through the shared cross-loader seam (Nerospace pattern).
        ClientBlockEntityRenderers.registerAll(new ClientBlockEntityRenderers.Sink() {
            //? if >=1.21.9 {
            @Override
            public <T extends BlockEntity, S extends BlockEntityRenderState> void register(
                    BlockEntityType<? extends T> type, BlockEntityRendererProvider<T, S> provider) {
                BlockEntityRendererRegistry.register(type, provider);
            }
            //?} else {
            /*@Override
            public <T extends BlockEntity> void register(
                    BlockEntityType<? extends T> type, BlockEntityRendererProvider<T> provider) {
                BlockEntityRendererRegistry.register(type, provider);
            }
            *///?}
        });
    }

    /**
     * Sprites for the gas transport fluids. A gas is never placed in the world, so these only show up
     * in other mods' tank and pipe GUIs — but an unregistered fluid draws the missing texture there.
     * Registering the model alone is enough; the tessellation hook is only for fluids in the world.
     */
    private static void registerGasFluidRendering() {
        //? if >=26.1 {
        FluidRenderingRegistry.register(NeroTechFluids.HYDROGEN.get(), gasFluidModel("hydrogen"));
        FluidRenderingRegistry.register(NeroTechFluids.OXYGEN.get(), gasFluidModel("oxygen"));
        //?} else {
        /*FluidRenderHandlerRegistry.INSTANCE.register(NeroTechFluids.HYDROGEN.get(), gasRenderHandler("hydrogen"));
        FluidRenderHandlerRegistry.INSTANCE.register(NeroTechFluids.OXYGEN.get(), gasRenderHandler("oxygen"));
        *///?}
    }

    // Sprites live in textures/block/ so the vanilla block atlas stitches them; a separate
    // textures/fluid/ directory is only stitched if the minecraft:blocks atlas definition lists it.
    //? if >=26.1 {
    private static FluidModel.Unbaked gasFluidModel(String gas) {
        return new FluidModel.Unbaked(
                new Material(Identifier.fromNamespaceAndPath(NeroTechCommon.MOD_ID, "block/" + gas + "_still")),
                new Material(Identifier.fromNamespaceAndPath(NeroTechCommon.MOD_ID, "block/" + gas + "_flow")),
                null,
                null);
    }
    //?} else {
    /*private static SimpleFluidRenderHandler gasRenderHandler(String gas) {
        return new SimpleFluidRenderHandler(
                Identifier.fromNamespaceAndPath(NeroTechCommon.MOD_ID, "block/" + gas + "_still"),
                Identifier.fromNamespaceAndPath(NeroTechCommon.MOD_ID, "block/" + gas + "_flow"));
    }
    *///?}

}
