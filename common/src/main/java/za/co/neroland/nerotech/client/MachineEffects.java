package za.co.neroland.nerotech.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Predicate;
import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerotech.config.NeroTechConfig;
import za.co.neroland.nerotech.machine.MachineClientHook;
import za.co.neroland.nerotech.machine.NeroTechMachineBlock;
import za.co.neroland.nerotech.machine.NeroTechMachineBlockEntity;
import za.co.neroland.nerotech.machine.Readout;
import za.co.neroland.nerotech.machine.chemistry.ChemistryContent;
import za.co.neroland.nerotech.machine.components.ComponentsContent;
import za.co.neroland.nerotech.machine.extraction.DeepCoreDrillBlockEntity;
import za.co.neroland.nerotech.machine.extraction.ExtractionContent;
import za.co.neroland.nerotech.machine.foundry.BlastFoundryBlockEntity;
import za.co.neroland.nerotech.machine.foundry.FoundryContent;
import za.co.neroland.nerotech.machine.fusion.FusionContent;
import za.co.neroland.nerotech.machine.heat.HeatContent;
import za.co.neroland.nerotech.machine.metallurgy.MetallurgyContent;
import za.co.neroland.nerotech.machine.utility.InductionChargerBlockEntity;
import za.co.neroland.nerotech.machine.utility.UtilityContent;
import za.co.neroland.nerotech.registry.ModParticles;
import za.co.neroland.nerotech.registry.ModSounds;

/**
 * Client only. Sounds and particles for the 0.8.0 machines (art direction §8), driven entirely by the
 * synced readout surface — {@code readout()}, {@code completions()}, {@code renderPulse()} and a few
 * per-machine extras — compared once per client tick, so no extra packet exists for any of it.
 *
 * <ul>
 * <li><b>Work loops</b> (≤ 2 s samples, 12-block attenuation) play as looping sound instances while the
 * machine reads WORK, and stop themselves when it leaves WORK, unloads or the player walks off. A
 * per-chunk voice cap ({@value #VOICES_PER_CHUNK}) keeps a dense factory from flooding the mixer.</li>
 * <li><b>One-shots</b>: {@code machine.stall} once on entering STALL, {@code machine.overheat} once on
 * entering HOT, plus each machine's completion / start / pulse sound once per synced change. Nothing
 * plays on chunk load (the first sight only records the state).</li>
 * <li><b>Particles</b>: {@code steam_wisp} on HOT for every machine here, and the machine's work particle
 * ({@code spark}, {@code frost_mote}, {@code rock_chip}) while it works — at most one spawn every
 * {@value #PARTICLE_GAP} ticks per machine (≤ 2/s), none with {@code renderAnimationsEnabled=false}.</li>
 * </ul>
 *
 * <p>The Annunciator Panel's alarm is played by the server once per newly lit alarm (its config switch
 * lives there), so it is not repeated here. Machine-scoped state only; no player data.</p>
 */
public final class MachineEffects implements MachineClientHook.Handler {

    /** Most looping machine voices one chunk may hold at once. */
    static final int VOICES_PER_CHUNK = 3;
    /** Ticks between two particle spawns of one machine (2 per second at most). */
    static final int PARTICLE_GAP = 10;
    /** Squared distances: sounds start within 16 blocks (12-block attenuation), particles within 32. */
    private static final double HEAR_SQR = 16.0 * 16.0;
    private static final double SEE_SQR = 32.0 * 32.0;
    private static final float LOOP_VOLUME = 0.6F;
    private static final float ONE_SHOT_VOLUME = 0.7F;

    /** What one machine type does. Any field may be null (nothing of that kind). */
    private record Profile(@Nullable Supplier<SoundEvent> loop, Predicate<NeroTechMachineBlockEntity> looping,
            @Nullable Supplier<SoundEvent> done, @Nullable Supplier<SoundEvent> start,
            @Nullable Supplier<SoundEvent> pulse, @Nullable Supplier<SoundEvent> ambient,
            @Nullable Supplier<SimpleParticleType> particle, Predicate<NeroTechMachineBlockEntity> emitting,
            float px, float py, float pz, float spread) {
    }

    private static final class Builder {
        private Supplier<SoundEvent> loop;
        private Predicate<NeroTechMachineBlockEntity> looping = MachineEffects::working;
        private Supplier<SoundEvent> done;
        private Supplier<SoundEvent> start;
        private Supplier<SoundEvent> pulse;
        private Supplier<SoundEvent> ambient;
        private Supplier<SimpleParticleType> particle;
        private Predicate<NeroTechMachineBlockEntity> emitting = MachineEffects::working;
        private float px = 0.5F;
        private float py = 1.0F;
        private float pz = 0.5F;
        private float spread = 0.2F;

        Builder loop(Supplier<SoundEvent> sound) {
            this.loop = sound;
            return this;
        }

        Builder loopWhile(Predicate<NeroTechMachineBlockEntity> when) {
            this.looping = when;
            return this;
        }

        Builder done(Supplier<SoundEvent> sound) {
            this.done = sound;
            return this;
        }

        Builder start(Supplier<SoundEvent> sound) {
            this.start = sound;
            return this;
        }

        Builder pulse(Supplier<SoundEvent> sound) {
            this.pulse = sound;
            return this;
        }

        Builder ambient(Supplier<SoundEvent> sound) {
            this.ambient = sound;
            return this;
        }

        Builder particle(Supplier<SimpleParticleType> type, float x, float y, float z, float spreadXz) {
            this.particle = type;
            this.px = x;
            this.py = y;
            this.pz = z;
            this.spread = spreadXz;
            return this;
        }

        Builder emitWhile(Predicate<NeroTechMachineBlockEntity> when) {
            this.emitting = when;
            return this;
        }

        Profile build() {
            return new Profile(this.loop, this.looping, this.done, this.start, this.pulse, this.ambient,
                    this.particle, this.emitting, this.px, this.py, this.pz, this.spread);
        }
    }

    /** Client-side memory per machine (entries go with the block entity). */
    private static final class Track {
        boolean seen;
        Readout readout = Readout.REST;
        int completions;
        int pulse;
        int formed;
        @Nullable
        LoopSound loop;
        long nextParticle;
        long nextAmbient;
    }

    @Nullable
    private Map<BlockEntityType<?>, Profile> profiles;
    private final Map<NeroTechMachineBlockEntity, Track> tracks = new WeakHashMap<>();
    /** Active loop voices per chunk (pruned of finished instances whenever a new one asks for room). */
    private final Map<Long, List<LoopSound>> voices = new HashMap<>();

    private static boolean working(NeroTechMachineBlockEntity machine) {
        return machine.readout() == Readout.WORK;
    }

    /** Built on first use (registries are frozen by then; the ticker asks only once a level loads). */
    private Map<BlockEntityType<?>, Profile> profiles() {
        Map<BlockEntityType<?>, Profile> built = this.profiles;
        if (built != null) {
            return built;
        }
        built = new IdentityHashMap<>();
        built.put(MetallurgyContent.PLASMA_CRUCIBLE_BE.get(), new Builder()
                .loop(() -> ModSounds.PLASMA_CRUCIBLE_ARC.get())
                .particle(() -> ModParticles.SPARK.get(), 0.5F, 0.95F, 0.5F, 0.2F).build());
        built.put(MetallurgyContent.PLATE_PRESS_BE.get(), new Builder()
                .done(() -> ModSounds.PLATE_PRESS_STAMP.get()).build());
        built.put(MetallurgyContent.FILAMENT_SPOOLER_BE.get(), new Builder()
                .loop(() -> ModSounds.FILAMENT_SPOOLER_SPIN.get()).build());
        built.put(ChemistryContent.CENTRIFUGE_BE.get(), new Builder()
                .start(() -> ModSounds.CENTRIFUGE_SPIN.get()).build());
        built.put(ChemistryContent.CRYO_DISTILLER_BE.get(), new Builder()
                .done(() -> ModSounds.CRYO_DISTILLER_VENT.get())
                .particle(() -> ModParticles.FROST_MOTE.get(), 0.5F, 0.55F, 0.5F, 0.45F).build());
        built.put(ChemistryContent.ATMOSPHERIC_CONDENSER_BE.get(), new Builder()
                .loop(() -> ModSounds.ATMOSPHERIC_CONDENSER_DRAW.get()).build());
        built.put(ChemistryContent.REACTION_VAT_BE.get(), new Builder()
                .ambient(() -> ModSounds.REACTION_VAT_BUBBLE.get()).build());
        built.put(ComponentsContent.COMPONENT_FORGE_BE.get(), new Builder()
                .done(() -> ModSounds.COMPONENT_FORGE_ASSEMBLE.get())
                .particle(() -> ModParticles.SPARK.get(), 0.5F, 0.75F, 0.5F, 0.12F).build());
        built.put(ComponentsContent.LASER_ETCHER_BE.get(), new Builder()
                .loop(() -> ModSounds.LASER_ETCHER_ETCH.get()).build());
        built.put(ComponentsContent.CRYSTAL_GROWER_BE.get(), new Builder()
                .done(() -> ModSounds.CRYSTAL_GROWER_CHIME.get()).build());
        built.put(FoundryContent.BLAST_FOUNDRY_BE.get(), new Builder()
                .loop(() -> ModSounds.BLAST_FOUNDRY_ROAR.get())
                .done(() -> ModSounds.BLAST_FOUNDRY_POUR.get()).build());
        built.put(ExtractionContent.DEEP_CORE_DRILL_BE.get(), new Builder()
                .loop(() -> ModSounds.DEEP_CORE_DRILL_GRIND.get())
                .loopWhile(MachineEffects::drilling)
                .particle(() -> ModParticles.ROCK_CHIP.get(), 0.5F, -2.85F, 0.5F, 0.3F)
                .emitWhile(MachineEffects::drilling).build());
        built.put(FusionContent.FUEL_INJECTOR_BE.get(), new Builder()
                .pulse(() -> ModSounds.FUEL_INJECTOR_PULSE.get()).build());
        built.put(UtilityContent.INDUCTION_CHARGER_BE.get(), new Builder()
                .loop(() -> ModSounds.INDUCTION_CHARGER_HUM.get())
                .loopWhile(machine -> machine instanceof InductionChargerBlockEntity charger && charger.charging())
                .build());
        built.put(HeatContent.HEAT_EXCHANGER_BE.get(), new Builder().build());
        this.profiles = built;
        return built;
    }

    private static boolean drilling(NeroTechMachineBlockEntity machine) {
        return machine instanceof DeepCoreDrillBlockEntity drill && drill.drilling()
                && machine.readout() != Readout.HOT;
    }

    @Override
    public boolean handles(BlockEntityType<?> type) {
        // Every machine type gets a client ticker so the Thermal Goggles can tint it (ThermalGogglesView
        // returns at once unless the local player wears charged goggles).
        return true;
    }

    @Override
    public void tick(NeroTechMachineBlockEntity machine) {
        Level level = machine.getLevel();
        if (level == null || !level.isClientSide()) {
            return;
        }
        ThermalGogglesView.tick(machine);
        Profile profile = profiles().get(machine.getType());
        if (profile == null) {
            return;
        }
        Track track = this.tracks.computeIfAbsent(machine, key -> new Track());
        Readout readout = machine.readout();
        int completions = machine.completions();
        int pulse = machine.renderPulse();
        int formed = machine instanceof BlastFoundryBlockEntity foundry ? foundry.formedSize() : 0;
        BlockPos pos = machine.getBlockPos();
        double cx = pos.getX() + 0.5;
        double cy = pos.getY() + 0.5;
        double cz = pos.getZ() + 0.5;
        Player player = Minecraft.getInstance().player;
        double distance = player == null ? Double.MAX_VALUE : player.distanceToSqr(cx, cy, cz);
        boolean hear = distance <= HEAR_SQR;
        long now = level.getGameTime();

        if (!track.seen) {
            // First sight (chunk load, rejoin): record only, so nothing plays for a state already held.
            track.seen = true;
        } else if (hear) {
            if (readout != track.readout) {
                if (readout == Readout.STALL) {
                    play(level, cx, cy, cz, ModSounds.MACHINE_STALL.get(), 1.0F);
                } else if (readout == Readout.HOT) {
                    play(level, cx, cy, cz, ModSounds.MACHINE_OVERHEAT.get(), 1.0F);
                } else if (readout == Readout.WORK && profile.start() != null) {
                    play(level, cx, cy, cz, profile.start().get(), 1.0F);
                }
            }
            if (completions != track.completions) {
                if (profile.done() != null) {
                    play(level, cx, cy, cz, profile.done().get(), 1.0F);
                }
                if (profile.start() != null && readout == Readout.WORK && readout == track.readout) {
                    play(level, cx, cy, cz, profile.start().get(), 1.0F); // the next run spins up
                }
            }
            if (pulse != track.pulse && profile.pulse() != null) {
                play(level, cx, cy, cz, profile.pulse().get(), 1.0F);
            }
            if (formed > 0 && track.formed == 0) {
                play(level, cx, cy, cz, ModSounds.BLAST_FOUNDRY_FORM.get(), 1.0F);
            }
        }
        track.readout = readout;
        track.completions = completions;
        track.pulse = pulse;
        track.formed = formed;

        if (profile.loop() != null && hear && profile.looping().test(machine)
                && (track.loop == null || !Minecraft.getInstance().getSoundManager().isActive(track.loop))) {
            startLoop(machine, profile, track, pos);
        }
        if (profile.ambient() != null && hear && readout == Readout.WORK && now >= track.nextAmbient) {
            RandomSource random = level.getRandom();
            if (track.nextAmbient != 0L) {
                play(level, cx, cy, cz, profile.ambient().get(), 0.8F + random.nextFloat() * 0.4F);
            }
            track.nextAmbient = now + 40L + random.nextInt(40);
        }

        if (NeroTechConfig.renderAnimationsEnabled() && distance <= SEE_SQR && now >= track.nextParticle) {
            Direction facing = facing(machine.getBlockState());
            if (readout == Readout.HOT) {
                spawn(level, pos, facing, ModParticles.STEAM_WISP.get(), 0.5F, 1.02F, 0.5F, 0.3F, 0.03);
                track.nextParticle = now + PARTICLE_GAP;
            } else if (profile.particle() != null && profile.emitting().test(machine)) {
                spawn(level, pos, facing, profile.particle().get(), profile.px(), profile.py(), profile.pz(),
                        profile.spread(), 0.02);
                track.nextParticle = now + PARTICLE_GAP;
            }
        }
    }

    private static Direction facing(BlockState state) {
        return state.hasProperty(NeroTechMachineBlock.FACING) ? state.getValue(NeroTechMachineBlock.FACING)
                : Direction.NORTH;
    }

    private static void play(Level level, double x, double y, double z, SoundEvent sound, float pitch) {
        level.playLocalSound(x, y, z, sound, SoundSource.BLOCKS, ONE_SHOT_VOLUME, pitch, false);
    }

    /** One particle at a model-space point (machine authored facing north), with a little jitter. */
    private static void spawn(Level level, BlockPos pos, Direction facing, SimpleParticleType type,
            float mx, float my, float mz, float spread, double rise) {
        RandomSource random = level.getRandom();
        float lx = mx + (random.nextFloat() - 0.5F) * 2.0F * spread;
        float lz = mz + (random.nextFloat() - 0.5F) * 2.0F * spread;
        float wx;
        float wz;
        switch (facing) {
            case EAST -> {
                wx = 1.0F - lz;
                wz = lx;
            }
            case SOUTH -> {
                wx = 1.0F - lx;
                wz = 1.0F - lz;
            }
            case WEST -> {
                wx = lz;
                wz = 1.0F - lx;
            }
            default -> {
                wx = lx;
                wz = lz;
            }
        }
        level.addParticle(type, pos.getX() + wx, pos.getY() + my, pos.getZ() + wz,
                (random.nextDouble() - 0.5) * 0.02, rise, (random.nextDouble() - 0.5) * 0.02);
    }

    private void startLoop(NeroTechMachineBlockEntity machine, Profile profile, Track track, BlockPos pos) {
        long chunk = ((long) (pos.getX() >> 4) << 32) | ((pos.getZ() >> 4) & 0xFFFFFFFFL);
        List<LoopSound> inChunk = this.voices.computeIfAbsent(chunk, key -> new ArrayList<>(VOICES_PER_CHUNK));
        inChunk.removeIf(sound -> sound.isStopped() || !Minecraft.getInstance().getSoundManager().isActive(sound));
        if (inChunk.size() >= VOICES_PER_CHUNK) {
            return; // the chunk is full: this machine stays quiet until a voice frees up
        }
        LoopSound sound = new LoopSound(profile.loop().get(), machine, profile.looping());
        track.loop = sound;
        inChunk.add(sound);
        Minecraft.getInstance().getSoundManager().play(sound);
    }

    /** A machine's work loop: follows the machine and stops when it stops working or the player leaves. */
    private static final class LoopSound extends AbstractTickableSoundInstance {

        private static final double STOP_SQR = 20.0 * 20.0;

        private final NeroTechMachineBlockEntity machine;
        private final Predicate<NeroTechMachineBlockEntity> whileOn;

        LoopSound(SoundEvent sound, NeroTechMachineBlockEntity machine, Predicate<NeroTechMachineBlockEntity> whileOn) {
            super(sound, SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
            this.machine = machine;
            this.whileOn = whileOn;
            BlockPos pos = machine.getBlockPos();
            this.x = pos.getX() + 0.5;
            this.y = pos.getY() + 0.5;
            this.z = pos.getZ() + 0.5;
            this.looping = true;
            this.delay = 0;
            this.volume = LOOP_VOLUME;
            this.pitch = 1.0F;
        }

        @Override
        public void tick() {
            Player player = Minecraft.getInstance().player;
            if (this.machine.isRemoved() || this.machine.getLevel() == null || !this.whileOn.test(this.machine)
                    || player == null || player.distanceToSqr(this.x, this.y, this.z) > STOP_SQR) {
                stop();
            }
        }
    }
}
