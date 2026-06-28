package za.co.neroland.nerotech.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;

import za.co.neroland.nerotech.NeroTechCommon;
import za.co.neroland.nerotech.menu.FabricatorMenu;
import za.co.neroland.nerotech.menu.NeroGeneratorMenu;
import za.co.neroland.nerotech.menu.OreProcessorMenu;
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

    private ModMenuTypes() {
    }

    public static void init() {
    }
}
