package za.co.neroland.nerotech.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import za.co.neroland.nerotech.NeroTechCommon;
import za.co.neroland.nerotech.registry.RegistrationProvider.RegistryEntry;

/**
 * NeroTech's own dedicated creative tab, registered cross-loader via {@link RegistrationProvider} over
 * the vanilla {@code CREATIVE_MODE_TAB} registry (mirroring Nerospace's tab). NeroTech no longer pours
 * its items into Core's shared "Neroland" tab — each mod gets its own tab.
 */
public final class ModCreativeTab {

    public static final RegistrationProvider<CreativeModeTab> TABS =
            RegistrationProvider.get(Registries.CREATIVE_MODE_TAB, NeroTechCommon.MOD_ID);

    // NOTE: vanilla CreativeModeTab.builder takes (Row, column); the no-arg overload and
    // withTabsBefore/After are NeoForge-only extensions, so they are avoided here (common = raw vanilla).
    public static final RegistryEntry<CreativeModeTab> NEROTECH = TABS.register("nerotech",
            key -> CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                    .title(Component.translatable("itemGroup.nerotech"))
                    .icon(() -> new ItemStack(ModItems.NERO_GENERATOR_ITEM.get()))
                    .displayItems((params, output) -> ModItems.creativeContents().forEach(output::accept))
                    .build());

    private ModCreativeTab() {
    }

    /** Force class-load so the static tab registration runs. */
    public static void init() {
    }
}
