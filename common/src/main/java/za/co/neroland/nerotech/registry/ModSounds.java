package za.co.neroland.nerotech.registry;

import java.util.List;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

import za.co.neroland.nerotech.NeroTechCommon;
import za.co.neroland.nerotech.registry.RegistrationProvider.RegistryEntry;

/**
 * NeroTech's sound events, one per entry in {@code assets/nerotech/sounds.json} (each with a subtitle).
 * Loops ({@link #LOOPS}) are short samples a machine re-plays while it works; the rest are one-shots.
 * Code that plays a sound looks it up through the registry or these entries, never by a hard-coded
 * vanilla fallback, so a resource pack that drops a file only silences it.
 */
public final class ModSounds {

    public static final RegistrationProvider<SoundEvent> SOUNDS =
            RegistrationProvider.get(Registries.SOUND_EVENT, NeroTechCommon.MOD_ID);

    public static final RegistryEntry<SoundEvent> MACHINE_STALL = register("machine.stall");
    public static final RegistryEntry<SoundEvent> MACHINE_OVERHEAT = register("machine.overheat");
    public static final RegistryEntry<SoundEvent> PLASMA_CRUCIBLE_ARC = register("plasma_crucible.arc");
    public static final RegistryEntry<SoundEvent> PLATE_PRESS_STAMP = register("plate_press.stamp");
    public static final RegistryEntry<SoundEvent> FILAMENT_SPOOLER_SPIN = register("filament_spooler.spin");
    public static final RegistryEntry<SoundEvent> CENTRIFUGE_SPIN = register("centrifuge.spin");
    public static final RegistryEntry<SoundEvent> CRYO_DISTILLER_VENT = register("cryo_distiller.vent");
    public static final RegistryEntry<SoundEvent> ATMOSPHERIC_CONDENSER_DRAW = register("atmospheric_condenser.draw");
    public static final RegistryEntry<SoundEvent> REACTION_VAT_BUBBLE = register("reaction_vat.bubble");
    public static final RegistryEntry<SoundEvent> COMPONENT_FORGE_ASSEMBLE = register("component_forge.assemble");
    public static final RegistryEntry<SoundEvent> LASER_ETCHER_ETCH = register("laser_etcher.etch");
    public static final RegistryEntry<SoundEvent> CRYSTAL_GROWER_CHIME = register("crystal_grower.chime");
    public static final RegistryEntry<SoundEvent> FUEL_INJECTOR_PULSE = register("fuel_injector.pulse");
    public static final RegistryEntry<SoundEvent> INDUCTION_CHARGER_HUM = register("induction_charger.hum");
    public static final RegistryEntry<SoundEvent> ANNUNCIATOR_PANEL_ALARM = register("annunciator_panel.alarm");
    public static final RegistryEntry<SoundEvent> BLAST_FOUNDRY_FORM = register("blast_foundry.form");
    public static final RegistryEntry<SoundEvent> BLAST_FOUNDRY_ROAR = register("blast_foundry.roar");
    public static final RegistryEntry<SoundEvent> BLAST_FOUNDRY_POUR = register("blast_foundry.pour");
    public static final RegistryEntry<SoundEvent> HEAT_PIPE_TICK = register("heat_pipe.tick");
    public static final RegistryEntry<SoundEvent> DEEP_CORE_DRILL_GRIND = register("deep_core_drill.grind");
    public static final RegistryEntry<SoundEvent> TOOL_SCAN = register("tool.scan");
    public static final RegistryEntry<SoundEvent> TOOL_POWER_DRILL = register("tool.power_drill");
    public static final RegistryEntry<SoundEvent> TOOL_CUTTING_LANCE = register("tool.cutting_lance");
    public static final RegistryEntry<SoundEvent> TOOL_ARC_WRENCH = register("tool.arc_wrench");

    /** Event ids (path) that are work loops rather than one-shots. */
    public static final List<String> LOOPS = List.of("filament_spooler.spin", "plasma_crucible.arc", "atmospheric_condenser.draw", "laser_etcher.etch", "induction_charger.hum", "blast_foundry.roar", "deep_core_drill.grind");

    private ModSounds() {
    }

    private static RegistryEntry<SoundEvent> register(String path) {
        Identifier id = Identifier.fromNamespaceAndPath(NeroTechCommon.MOD_ID, path);
        return SOUNDS.register(path, key -> SoundEvent.createVariableRangeEvent(id));
    }

    /** Classload-forcing no-op (from {@code ModRegistries.init()}). */
    public static void init() {
    }
}
