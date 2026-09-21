package za.co.neroland.nerotech.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.recipe.v1.sync.ClientRecipeSynchronizedEvent;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderingRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;
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
import za.co.neroland.nerotech.compat.jei.JeiSyncedRecipes;
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
        // list NeroTech's machine recipes — 26.x clients hold no full recipe list of their own.
        ClientRecipeSynchronizedEvent.EVENT.register((client, synchronizedRecipes) ->
                JeiSyncedRecipes.accept(synchronizedRecipes.recipes()));
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
            @Override
            public <T extends BlockEntity, S extends BlockEntityRenderState> void register(
                    BlockEntityType<? extends T> type, BlockEntityRendererProvider<T, S> provider) {
                BlockEntityRendererRegistry.register(type, provider);
            }
        });
    }

    /**
     * Sprites for the gas transport fluids. A gas is never placed in the world, so these only show up
     * in other mods' tank and pipe GUIs — but an unregistered fluid draws the missing texture there.
     * Registering the model alone is enough; the tessellation hook is only for fluids in the world.
     */
    private static void registerGasFluidRendering() {
        FluidRenderingRegistry.register(NeroTechFluids.HYDROGEN.get(), gasFluidModel("hydrogen"));
        FluidRenderingRegistry.register(NeroTechFluids.OXYGEN.get(), gasFluidModel("oxygen"));
    }

    private static FluidModel.Unbaked gasFluidModel(String gas) {
        return new FluidModel.Unbaked(
                new Material(Identifier.fromNamespaceAndPath(NeroTechCommon.MOD_ID, "fluid/" + gas + "_still")),
                new Material(Identifier.fromNamespaceAndPath(NeroTechCommon.MOD_ID, "fluid/" + gas + "_flow")),
                null,
                null);
    }

}
