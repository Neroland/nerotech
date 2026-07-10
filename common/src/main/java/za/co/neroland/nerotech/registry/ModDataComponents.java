package za.co.neroland.nerotech.registry;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;

import za.co.neroland.nerotech.NeroTechCommon;
import za.co.neroland.nerotech.item.ConfiguratorState;
import za.co.neroland.nerotech.registry.RegistrationProvider.RegistryEntry;

/**
 * NeroTech's item data components, registered cross-loader through the {@link RegistrationProvider}
 * seam over the vanilla data-component registry — 26.x's replacement for ad-hoc stack NBT.
 *
 * <p>V1 ships one component: the Configurator's tool mode + copied side-config clipboard. The
 * network stream codec is derived from the persistent codec by the builder, so the component
 * syncs with the stack automatically.
 */
public final class ModDataComponents {

    public static final RegistrationProvider<DataComponentType<?>> COMPONENTS =
            RegistrationProvider.get(Registries.DATA_COMPONENT_TYPE, NeroTechCommon.MOD_ID);

    /** Configurator tool mode + clipboard; routing modes only, no player data (POPIA/GDPR). */
    public static final RegistryEntry<DataComponentType<ConfiguratorState>> CONFIGURATOR_STATE =
            COMPONENTS.register("configurator_state", key -> DataComponentType.<ConfiguratorState>builder()
                    .persistent(ConfiguratorState.CODEC)
                    .build());

    private ModDataComponents() {
    }

    /** Force class-load so the static registrations run (eager on Fabric). */
    public static void init() {
    }
}
