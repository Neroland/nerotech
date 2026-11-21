package za.co.neroland.nerotech.machine.components;

import za.co.neroland.nerotech.machine.ProcessingSpec;

/**
 * The Components family's slot and tank shapes, kept apart from the block entities so the rule tests
 * can read them without loading Minecraft classes.
 */
public final class ComponentsSpecs {

    /** Component Forge: six item inputs, one output, one fluid input tank, no catalyst. */
    public static final ProcessingSpec COMPONENT_FORGE = new ProcessingSpec(6, false, 1,
            ComponentsSettings.COMPONENT_FORGE_FLUID_CAPACITY, 0L, 0L, 0L);

    /** Laser Etcher: wafer + two part slots, the lens in the catalyst slot, one output. */
    public static final ProcessingSpec LASER_ETCHER = new ProcessingSpec(3, true, 1, 0L, 0L, 0L, 0L);

    /** Crystal Grower: seed + two feedstock slots, one output, one fluid input tank. */
    public static final ProcessingSpec CRYSTAL_GROWER = new ProcessingSpec(3, false, 1,
            ComponentsSettings.CRYSTAL_GROWER_FLUID_CAPACITY, 0L, 0L, 0L);

    private ComponentsSpecs() {
    }
}
