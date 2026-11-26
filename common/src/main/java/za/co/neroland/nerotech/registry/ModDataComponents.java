package za.co.neroland.nerotech.registry;

//? if >=1.20.5 {
import net.minecraft.core.component.DataComponentType;
//?} else {
/*import za.co.neroland.nerotech.registry.LegacyItemComponent;
*///?}
import net.minecraft.core.registries.Registries;

import za.co.neroland.nerotech.NeroTechCommon;
import za.co.neroland.nerotech.item.ConfiguratorState;
import za.co.neroland.nerotech.machine.VaultContents;
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

    //? if >=1.20.5 {
    public static final RegistrationProvider<DataComponentType<?>> COMPONENTS =
            RegistrationProvider.get(Registries.DATA_COMPONENT_TYPE, NeroTechCommon.MOD_ID);
    //?}

    /** Configurator tool mode + clipboard; routing modes only, no player data (POPIA/GDPR). */
    //? if >=1.20.5 {
    public static final RegistryEntry<DataComponentType<ConfiguratorState>> CONFIGURATOR_STATE =
            COMPONENTS.register("configurator_state", key -> DataComponentType.<ConfiguratorState>builder()
                    .persistent(ConfiguratorState.CODEC)
                    .build());
    //?} else {
    /*public static final LegacyItemComponent<ConfiguratorState> CONFIGURATOR_STATE =
            new LegacyItemComponent<>("nerotech:configurator_state", ConfiguratorState.CODEC);
    *///?}

    /** A broken Singularity Vault's bulk store, when too large to spill; item type + count only. */
    //? if >=1.20.5 {
    public static final RegistryEntry<DataComponentType<VaultContents>> VAULT_CONTENTS =
            COMPONENTS.register("vault_contents", key -> DataComponentType.<VaultContents>builder()
                    .persistent(VaultContents.CODEC)
                    .build());
    //?} else {
    /*public static final LegacyItemComponent<VaultContents> VAULT_CONTENTS =
            new LegacyItemComponent<>("nerotech:vault_contents", VaultContents.CODEC);
    *///?}

    /** A broken Process Tank's share that did not fit back into its column; medium id + amount only. */
    //? if >=1.20.5 {
    public static final RegistryEntry<DataComponentType<za.co.neroland.nerotech.machine.tank.TankContents>> TANK_CONTENTS =
            COMPONENTS.register("tank_contents",
                    key -> DataComponentType.<za.co.neroland.nerotech.machine.tank.TankContents>builder()
                            .persistent(za.co.neroland.nerotech.machine.tank.TankContents.CODEC)
                            .build());
    //?} else {
    /*public static final LegacyItemComponent<za.co.neroland.nerotech.machine.tank.TankContents> TANK_CONTENTS =
            new LegacyItemComponent<>("nerotech:tank_contents", za.co.neroland.nerotech.machine.tank.TankContents.CODEC);
    *///?}

    private ModDataComponents() {
    }

    /** Force class-load so the static registrations run (eager on Fabric). */
    public static void init() {
    }
}
