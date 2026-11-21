package za.co.neroland.nerotech.compat;

import java.util.Map;
import java.util.function.Supplier;

import net.minecraft.world.level.ItemLike;

import za.co.neroland.nerotech.machine.chemistry.ChemistryContent;
import za.co.neroland.nerotech.machine.components.ComponentsContent;
import za.co.neroland.nerotech.machine.foundry.FoundryContent;
import za.co.neroland.nerotech.machine.metallurgy.MetallurgyContent;
import za.co.neroland.nerotech.registry.ModBlocks;

/**
 * The machine that runs each 0.8.0 process recipe type, keyed by the type's id path: the crafting
 * station (JEI) / workstation (EMI) and the page icon. Shared by both recipe-viewer plugins.
 */
public final class ProcessStations {

    private static final Map<String, Supplier<? extends ItemLike>> STATIONS = Map.ofEntries(
            Map.entry("alloying", MetallurgyContent.PLASMA_CRUCIBLE),
            Map.entry("pressing", MetallurgyContent.PLATE_PRESS),
            Map.entry("drawing", MetallurgyContent.FILAMENT_SPOOLER),
            Map.entry("foundry_casting", FoundryContent.BLAST_FOUNDRY),
            Map.entry("centrifuging", ChemistryContent.CENTRIFUGE),
            Map.entry("cryo_distilling", ChemistryContent.CRYO_DISTILLER),
            Map.entry("condensing", ChemistryContent.ATMOSPHERIC_CONDENSER),
            Map.entry("reacting", ChemistryContent.REACTION_VAT),
            Map.entry("component_forging", ComponentsContent.COMPONENT_FORGE),
            Map.entry("etching", ComponentsContent.LASER_ETCHER),
            Map.entry("crystal_growing", ComponentsContent.CRYSTAL_GROWER));

    private ProcessStations() {
    }

    /** The machine for process type {@code path}; the Process Tank for an unknown type. */
    public static ItemLike station(String path) {
        Supplier<? extends ItemLike> station = STATIONS.get(path);
        return station != null ? station.get() : ModBlocks.PROCESS_TANK.get();
    }
}
