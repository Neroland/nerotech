package za.co.neroland.nerotech.neoforge;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

import za.co.neroland.nerotech.client.AutoCrafterScreen;
import za.co.neroland.nerotech.client.FabricatorScreen;
import za.co.neroland.nerotech.client.ItemSorterScreen;
import za.co.neroland.nerotech.client.NeroGeneratorScreen;
import za.co.neroland.nerotech.client.OreProcessorScreen;
import za.co.neroland.nerotech.client.SolarArrayScreen;
import za.co.neroland.nerotech.registry.ModMenuTypes;

/** NeoForge client-only wiring (machine screen registration). Loaded only behind Dist.CLIENT. */
public final class NeoForgeClientSetup {

    private NeoForgeClientSetup() {
    }

    public static void init(IEventBus modEventBus) {
        modEventBus.addListener(NeoForgeClientSetup::onRegisterScreens);
    }

    private static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.NERO_GENERATOR.get(), NeroGeneratorScreen::new);
        event.register(ModMenuTypes.SOLAR_ARRAY.get(), SolarArrayScreen::new);
        event.register(ModMenuTypes.ORE_PROCESSOR.get(), OreProcessorScreen::new);
        event.register(ModMenuTypes.FABRICATOR.get(), FabricatorScreen::new);
        event.register(ModMenuTypes.AUTO_CRAFTER.get(), AutoCrafterScreen::new);
        event.register(ModMenuTypes.ITEM_SORTER.get(), ItemSorterScreen::new);
    }
}
