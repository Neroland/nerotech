package za.co.neroland.nerotech.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;

import za.co.neroland.nerotech.NeroTechCommon;
import za.co.neroland.nerotech.guide.TechGuideMenu;
import za.co.neroland.nerotech.menu.AnalyticsTerminalMenu;
import za.co.neroland.nerotech.menu.AutoCrafterMenu;
import za.co.neroland.nerotech.menu.FabricatorMenu;
import za.co.neroland.nerotech.menu.ItemSorterMenu;
import za.co.neroland.nerotech.menu.NeroGeneratorMenu;
import za.co.neroland.nerotech.menu.OreProcessorMenu;
import za.co.neroland.nerotech.menu.RemediatorMenu;
import za.co.neroland.nerotech.menu.ScrubberMenu;
import za.co.neroland.nerotech.menu.SolarArrayMenu;
import za.co.neroland.nerotech.registry.RegistrationProvider.RegistryEntry;

/** Container menu types for NeroTech's machines, registered cross-loader via {@link RegistrationProvider}. */
public final class ModMenuTypes {

    public static final RegistrationProvider<MenuType<?>> MENUS =
            RegistrationProvider.get(Registries.MENU, NeroTechCommon.MOD_ID);

    public static final RegistryEntry<MenuType<NeroGeneratorMenu>> NERO_GENERATOR =
            MENUS.register("nero_generator", key -> new MenuType<>(NeroGeneratorMenu::new, FeatureFlags.VANILLA_SET));
    public static final RegistryEntry<MenuType<SolarArrayMenu>> SOLAR_ARRAY =
            MENUS.register("solar_array", key -> new MenuType<>(SolarArrayMenu::new, FeatureFlags.VANILLA_SET));
    public static final RegistryEntry<MenuType<OreProcessorMenu>> ORE_PROCESSOR =
            MENUS.register("ore_processor", key -> new MenuType<>(OreProcessorMenu::new, FeatureFlags.VANILLA_SET));
    public static final RegistryEntry<MenuType<FabricatorMenu>> FABRICATOR =
            MENUS.register("fabricator", key -> new MenuType<>(FabricatorMenu::new, FeatureFlags.VANILLA_SET));
    public static final RegistryEntry<MenuType<AutoCrafterMenu>> AUTO_CRAFTER =
            MENUS.register("auto_crafter", key -> new MenuType<>(AutoCrafterMenu::new, FeatureFlags.VANILLA_SET));
    public static final RegistryEntry<MenuType<ItemSorterMenu>> ITEM_SORTER =
            MENUS.register("item_sorter", key -> new MenuType<>(ItemSorterMenu::new, FeatureFlags.VANILLA_SET));
    public static final RegistryEntry<MenuType<ScrubberMenu>> SCRUBBER =
            MENUS.register("scrubber", key -> new MenuType<>(ScrubberMenu::new, FeatureFlags.VANILLA_SET));
    public static final RegistryEntry<MenuType<RemediatorMenu>> REMEDIATOR =
            MENUS.register("remediator", key -> new MenuType<>(RemediatorMenu::new, FeatureFlags.VANILLA_SET));
    public static final RegistryEntry<MenuType<AnalyticsTerminalMenu>> ANALYTICS_TERMINAL =
            MENUS.register("analytics_terminal", key -> new MenuType<>(AnalyticsTerminalMenu::new, FeatureFlags.VANILLA_SET));
    public static final RegistryEntry<MenuType<TechGuideMenu>> TECH_GUIDE =
            MENUS.register("tech_guide", key -> new MenuType<>(TechGuideMenu::new, FeatureFlags.VANILLA_SET));

    private ModMenuTypes() {
    }

    public static void init() {
    }
}
