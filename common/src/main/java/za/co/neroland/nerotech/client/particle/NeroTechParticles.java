package za.co.neroland.nerotech.client.particle;

import java.util.function.Function;

import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;

import za.co.neroland.nerotech.registry.ModParticles;

/** Client provider wiring for NeroTech's machine particles, shared by the three loaders. Client only. */
public final class NeroTechParticles {

    /** A loader's sprite-set provider registration. */
    public interface Sink {
        void register(SimpleParticleType type, Function<SpriteSet, ParticleProvider<SimpleParticleType>> factory);
    }

    private NeroTechParticles() {
    }

    public static void registerAll(Sink sink) {
        sink.register(ModParticles.STEAM_WISP.get(), s -> new MachineParticle.Provider(s, MachineParticle.Kind.STEAM_WISP));
        sink.register(ModParticles.FROST_MOTE.get(), s -> new MachineParticle.Provider(s, MachineParticle.Kind.FROST_MOTE));
        sink.register(ModParticles.SPARK.get(), s -> new MachineParticle.Provider(s, MachineParticle.Kind.SPARK));
        sink.register(ModParticles.ROCK_CHIP.get(), s -> new MachineParticle.Provider(s, MachineParticle.Kind.ROCK_CHIP));
    }
}
