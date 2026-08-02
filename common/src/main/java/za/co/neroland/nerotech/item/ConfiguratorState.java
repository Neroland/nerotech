package za.co.neroland.nerotech.item;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;

import za.co.neroland.nerolandcore.sideconfig.Channel;

/**
 * The Configurator's per-stack state, stored in the {@code nerotech:configurator_state} data
 * component (the 26.x replacement for stack NBT): the tool's current mode plus an optional
 * copied side-config snapshot ("clipboard"). Immutable — every mutation returns a new value,
 * as data components require.
 *
 * <p>The clipboard holds Core's packed snapshot ({@link Channel} → packed modes int, keyed by
 * {@link Channel#lowerName()} for stable serialisation) plus the source machine's block-entity
 * type id, so paste is only offered onto machines of the same type — matching the Side Config
 * widget's in-GUI clipboard rule. Routing modes and a type id only; no positions, no player
 * data (POPIA/GDPR).
 */
public record ConfiguratorState(boolean copyPaste, Optional<Snapshot> clipboard,
        Optional<Link> linkSource) {

    /** Fresh tool: configure mode, empty clipboard, no pending link (component absent ≡ this). */
    public static final ConfiguratorState DEFAULT =
            new ConfiguratorState(false, Optional.empty(), Optional.empty());

    public static final Codec<ConfiguratorState> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.BOOL.optionalFieldOf("copy_paste", false).forGetter(ConfiguratorState::copyPaste),
            Snapshot.CODEC.optionalFieldOf("clipboard").forGetter(ConfiguratorState::clipboard),
            Link.CODEC.optionalFieldOf("link_source").forGetter(ConfiguratorState::linkSource)
    ).apply(inst, ConfiguratorState::new));

    /** Switch between configure (cycle/read) and copy/paste mode, keeping the clipboard. */
    public ConfiguratorState toggled() {
        return new ConfiguratorState(!this.copyPaste, this.clipboard, this.linkSource);
    }

    /** Replace the clipboard with a freshly copied snapshot. */
    public ConfiguratorState withClipboard(Snapshot snapshot) {
        return new ConfiguratorState(this.copyPaste, Optional.of(snapshot), this.linkSource);
    }

    /** Remember the first end of a Wireless Power Node pairing (Stage D). */
    public ConfiguratorState withLinkSource(Link source) {
        return new ConfiguratorState(this.copyPaste, this.clipboard, Optional.of(source));
    }

    /** Forget the pending pairing — after a successful pair, or when the source node has gone. */
    public ConfiguratorState withoutLinkSource() {
        return new ConfiguratorState(this.copyPaste, this.clipboard, Optional.empty());
    }

    /**
     * The first end of a pending Wireless Power Node pairing: a block position plus the dimension
     * it was taken in, so a stored source can never accidentally match a different node at the same
     * coordinates in another world. World/block data only — no player identity (POPIA/GDPR).
     */
    public record Link(int x, int y, int z, String dimension) {

        public static final Codec<Link> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Codec.INT.fieldOf("x").forGetter(Link::x),
                Codec.INT.fieldOf("y").forGetter(Link::y),
                Codec.INT.fieldOf("z").forGetter(Link::z),
                Codec.STRING.fieldOf("dimension").forGetter(Link::dimension)
        ).apply(inst, Link::new));

        public static Link of(BlockPos pos, String dimension) {
            return new Link(pos.getX(), pos.getY(), pos.getZ(), dimension);
        }

        public BlockPos toPos() {
            return new BlockPos(this.x, this.y, this.z);
        }
    }

    /**
     * A copied machine configuration: source type id, Core's packed per-channel modes, and (since
     * Stage E) the source machine's overclock preset ordinal, or {@link #NO_PRESET} when the source
     * had none. Paste is no longer type-locked — it applies whichever channels the target actually
     * declares, and the preset always.
     */
    public record Snapshot(String typeKey, Map<String, Integer> packed, int preset) {

        /** Sentinel preset ordinal: "the source machine had no preset to copy". */
        public static final int NO_PRESET = -1;

        public static final Codec<Snapshot> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Codec.STRING.fieldOf("type_key").forGetter(Snapshot::typeKey),
                Codec.unboundedMap(Codec.STRING, Codec.INT).fieldOf("packed").forGetter(Snapshot::packed),
                Codec.INT.optionalFieldOf("preset", NO_PRESET).forGetter(Snapshot::preset)
        ).apply(inst, Snapshot::new));

        /** Pack Core's {@code Configurator.snapshot(...)} result plus a preset into serialisable form. */
        public static Snapshot of(String typeKey, Map<Channel, Integer> packed, int preset) {
            Map<String, Integer> keyed = new HashMap<>();
            packed.forEach((channel, modes) -> keyed.put(channel.lowerName(), modes));
            return new Snapshot(typeKey, Map.copyOf(keyed), preset);
        }

        /** Unpack back into the {@link Channel}-keyed map {@code Configurator.apply(...)} expects. */
        public Map<Channel, Integer> toPacked() {
            Map<Channel, Integer> out = new java.util.EnumMap<>(Channel.class);
            for (Channel channel : Channel.VALUES) {
                Integer modes = this.packed.get(channel.lowerName());
                if (modes != null) {
                    out.put(channel, modes);
                }
            }
            return out;
        }
    }
}
