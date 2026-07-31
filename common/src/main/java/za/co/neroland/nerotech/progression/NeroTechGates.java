package za.co.neroland.nerotech.progression;

import net.minecraft.resources.Identifier;

import za.co.neroland.nerotech.NeroTechCommon;

/**
 * NeroTech's own Core progression gates — the real, datapack-backed gates that guard the
 * orbit-tier (Advanced Fabricator / Advanced Ore Processor / Fusion Reactor) content, layered
 * ON TOP of the existing tag/recipe gating.
 *
 * <p>These mirror {@link za.co.neroland.nerolandcore.progression.CoreGates}: the ids resolve to
 * datapack JSON under {@code data/nerotech/neroland_gates/<path>.json} (id = namespace + path),
 * and {@link za.co.neroland.nerolandcore.progression.ProgressionGates} reads/opens them. NeroTech
 * only OPENS {@code industrial_power} today (in {@code NeroTechMachineBlock.setPlacedBy}); this
 * adds the two higher gates:
 *
 * <ul>
 *   <li>{@link #ORBIT_FABRICATION} — requires Core's {@code nerolandcore:reached_orbit};
 *       gates USE of the orbit-tier machines and is opened on first use once orbit is reached.</li>
 *   <li>{@link #FUSION_ONLINE} — requires {@link #ORBIT_FABRICATION}; opened for a reactor's owner
 *       the first time their Fusion Reactor ignites.</li>
 * </ul>
 */
public final class NeroTechGates {

    /** Orbit-tier fabrication gate (requires {@code nerolandcore:reached_orbit}). */
    public static final Identifier ORBIT_FABRICATION = id("orbit_fabrication");

    /** Fusion gate (requires {@link #ORBIT_FABRICATION}). */
    public static final Identifier FUSION_ONLINE = id("fusion_online");

    private NeroTechGates() {
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(NeroTechCommon.MOD_ID, path);
    }
}
