package za.co.neroland.nerotech.api;

import za.co.neroland.nerotech.config.NeroTechConfig;

/**
 * Read-only access to the NeroTech settings an add-on machine has to match, so add-ons do not import
 * NeroTech's internal config class. Values are read live (they follow config reloads). Machine and
 * client-render settings only; no player data.
 */
public final class MachineSettings {

    private MachineSettings() {
    }

    /** The NE a machine moves through one face in one tick ({@code machineMaxTransfer}). */
    public static int machineMaxTransfer() {
        return NeroTechConfig.machineMaxTransfer();
    }

    /**
     * Whether block-entity renderers should animate ({@code renderAnimationsEnabled}). A client-local
     * quality toggle: false means static parked frames. Never affects gameplay or balance.
     */
    public static boolean renderAnimationsEnabled() {
        return NeroTechConfig.renderAnimationsEnabled();
    }
}
