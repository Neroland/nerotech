package za.co.neroland.nerotech.registry;

import java.util.List;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;

import za.co.neroland.nerotech.NeroTechCommon;
import za.co.neroland.nerotech.registry.RegistrationProvider.RegistryEntry;

/**
 * NeroTech's four machine particles (sprites from {@code particles/<id>.json}): {@code steam_wisp} (a hot
 * machine venting), {@code frost_mote} (cryo machines), {@code spark} (arcs and grinding) and
 * {@code rock_chip} (the Deep Core Drill). Each loader registers the client providers through
 * {@code client.particle.NeroTechParticles}. A particle carries no data beyond its position and velocity.
 */
public final class ModParticles {

    public static final RegistrationProvider<ParticleType<?>> PARTICLES =
            RegistrationProvider.get(Registries.PARTICLE_TYPE, NeroTechCommon.MOD_ID);

    public static final RegistryEntry<SimpleParticleType> STEAM_WISP = PARTICLES.register("steam_wisp", key -> simple());
    public static final RegistryEntry<SimpleParticleType> FROST_MOTE = PARTICLES.register("frost_mote", key -> simple());
    public static final RegistryEntry<SimpleParticleType> SPARK = PARTICLES.register("spark", key -> simple());
    public static final RegistryEntry<SimpleParticleType> ROCK_CHIP = PARTICLES.register("rock_chip", key -> simple());

    /** Every particle id path, in registration order. */
    public static final List<String> IDS = List.of("steam_wisp", "frost_mote", "spark", "rock_chip");

    private ModParticles() {
    }

    private static SimpleParticleType simple() {
        // The constructor is protected outside NeoForge's access transformer: an empty subclass works everywhere.
        // alwaysShow=false: machine ambience respects the particle setting and distance culling.
        return new SimpleParticleType(false) {
        };
    }

    /** Classload-forcing no-op (from {@code ModRegistries.init()}). */
    public static void init() {
    }
}
