package za.co.neroland.nerotech.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
//? if >=1.21.9 {
import net.minecraft.client.particle.SingleQuadParticle;
//?} else {
/*import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
*///?}
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

/**
 * The four machine particles, one class tuned per {@link Kind}. The spawner passes the initial velocity.
 * Client only.
 */
//? if >=1.21.9 {
public class MachineParticle extends SingleQuadParticle {
//?} else {
/*public class MachineParticle extends TextureSheetParticle {
*///?}

    public enum Kind {
        /** Rises and slows, grows a little, plays its four frames once and fades. */
        STEAM_WISP(0.18F, 24),
        /** Drifts down slowly with a slight sway, fades out. */
        FROST_MOTE(0.06F, 40),
        /** Fast, falls under gravity, full-bright, three frames. */
        SPARK(0.05F, 10),
        /** A random chip sprite thrown up and falling back with physics. */
        ROCK_CHIP(0.07F, 30);

        final float size;
        final int life;

        Kind(float size, int life) {
            this.size = size;
            this.life = life;
        }
    }

    private final SpriteSet sprites;
    private final Kind kind;

    protected MachineParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd,
            SpriteSet sprites, Kind kind) {
        //? if >=1.21.9 {
        super(level, x, y, z, sprites.first());
        //?} else {
        /*super(level, x, y, z);
        *///?}
        this.sprites = sprites;
        this.kind = kind;
        this.xd = xd;
        this.yd = yd;
        this.zd = zd;
        this.quadSize = kind.size * (0.8F + this.random.nextFloat() * 0.4F);
        this.lifetime = kind.life + this.random.nextInt(Math.max(1, kind.life / 3));
        this.hasPhysics = false;
        this.gravity = 0.0F;
        this.friction = 0.96F;
        switch (kind) {
            case STEAM_WISP -> {
                this.gravity = -0.01F;
                this.friction = 0.9F;
            }
            case FROST_MOTE -> {
                this.gravity = 0.003F;
                this.friction = 0.98F;
            }
            case SPARK -> {
                this.gravity = 0.06F;
                this.friction = 0.94F;
            }
            case ROCK_CHIP -> {
                this.gravity = 0.08F;
                this.friction = 0.98F;
                this.hasPhysics = true;
            }
        }
        if (kind == Kind.ROCK_CHIP) {
            this.setSprite(sprites.get(this.random));
        } else {
            this.setSpriteFromAge(sprites);
        }
    }

    //? if >=1.21.9 {
    @Override
    public SingleQuadParticle.Layer getLayer() {
        return this.kind == Kind.ROCK_CHIP ? SingleQuadParticle.Layer.OPAQUE : SingleQuadParticle.Layer.TRANSLUCENT;
    }
    //?} else {
    /*@Override
    public ParticleRenderType getRenderType() {
        return this.kind == Kind.ROCK_CHIP ? ParticleRenderType.PARTICLE_SHEET_OPAQUE
                : ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }
    *///?}

    @Override
    public void tick() {
        super.tick();
        if (this.removed) {
            return;
        }
        float t = this.age / (float) Math.max(1, this.lifetime);
        switch (this.kind) {
            case STEAM_WISP -> {
                this.setSpriteFromAge(this.sprites);
                this.quadSize *= 1.01F;
                this.setAlpha(Math.max(0F, 0.8F * (1F - t)));
            }
            case FROST_MOTE -> {
                this.xd += (this.random.nextFloat() - 0.5F) * 0.002F;
                this.zd += (this.random.nextFloat() - 0.5F) * 0.002F;
                this.setAlpha(t < 0.6F ? 1F : Math.max(0F, (1F - t) / 0.4F));
            }
            case SPARK -> this.setSpriteFromAge(this.sprites);
            case ROCK_CHIP -> {
            }
        }
    }

    @Override
    //? if >=26.1 {
    protected int getLightCoords(float partialTick) {
        return this.kind == Kind.SPARK ? 0xF000F0 : super.getLightCoords(partialTick);
    }
    //?} else {
    /*protected int getLightColor(float partialTick) {
        return this.kind == Kind.SPARK ? 0xF000F0 : super.getLightColor(partialTick);
    }
    *///?}

    /** One provider per type, created by each loader with the type's sprite set. */
    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        private final Kind kind;

        public Provider(SpriteSet sprites, Kind kind) {
            this.sprites = sprites;
            this.kind = kind;
        }

        @Override
        @org.jetbrains.annotations.Nullable
        //? if >=1.21.9 {
        public Particle createParticle(SimpleParticleType options, ClientLevel level, double x, double y, double z,
                double xd, double yd, double zd, RandomSource random) {
        //?} else {
        /*public Particle createParticle(SimpleParticleType options, ClientLevel level, double x, double y, double z,
                double xd, double yd, double zd) {
        *///?}
            return new MachineParticle(level, x, y, z, xd, yd, zd, this.sprites, this.kind);
        }
    }
}
