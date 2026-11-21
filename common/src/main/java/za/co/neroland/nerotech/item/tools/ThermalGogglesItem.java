package za.co.neroland.nerotech.item.tools;

import za.co.neroland.nerotech.config.NeroTechConfig;

/**
 * Thermal Goggles — worn in the head slot while charged, they tint the NeroTech machines and Heat Pipes in
 * view by heat (client only, {@code client.ThermalGogglesView}, reading the heat bucket and pipe heat level
 * the client already has from the normal block-entity and block-state sync; no packet is added). Worn
 * goggles drain {@code thermalGogglesDrainPerSecond} on the server ({@link WornTools}). No player data.
 */
public class ThermalGogglesItem extends HeadToolItem {

    public ThermalGogglesItem(Properties properties) {
        super(properties);
    }

    @Override
    public int capacity() {
        return NeroTechConfig.thermalGogglesCapacity();
    }
}
