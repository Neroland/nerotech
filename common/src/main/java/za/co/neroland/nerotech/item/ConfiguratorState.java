package za.co.neroland.nerotech.item;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

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
public record ConfiguratorState(boolean copyPaste, Optional<Snapshot> clipboard) {

    /** Fresh tool: configure mode, empty clipboard (component absent ≡ this). */
    public static final ConfiguratorState DEFAULT = new ConfiguratorState(false, Optional.empty());

    public static final Codec<ConfiguratorState> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.BOOL.optionalFieldOf("copy_paste", false).forGetter(ConfiguratorState::copyPaste),
            Snapshot.CODEC.optionalFieldOf("clipboard").forGetter(ConfiguratorState::clipboard)
    ).apply(inst, ConfiguratorState::new));

    /** Switch between configure (cycle/read) and copy/paste mode, keeping the clipboard. */
    public ConfiguratorState toggled() {
        return new ConfiguratorState(!this.copyPaste, this.clipboard);
    }

    /** Replace the clipboard with a freshly copied snapshot. */
    public ConfiguratorState withClipboard(Snapshot snapshot) {
        return new ConfiguratorState(this.copyPaste, Optional.of(snapshot));
    }

    /** A copied machine side-configuration: source type id + Core's packed per-channel modes. */
    public record Snapshot(String typeKey, Map<String, Integer> packed) {

        public static final Codec<Snapshot> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Codec.STRING.fieldOf("type_key").forGetter(Snapshot::typeKey),
                Codec.unboundedMap(Codec.STRING, Codec.INT).fieldOf("packed").forGetter(Snapshot::packed)
        ).apply(inst, Snapshot::new));

        /** Pack Core's {@code Configurator.snapshot(...)} result into serialisable form. */
        public static Snapshot of(String typeKey, Map<Channel, Integer> packed) {
            Map<String, Integer> keyed = new HashMap<>();
            packed.forEach((channel, modes) -> keyed.put(channel.lowerName(), modes));
            return new Snapshot(typeKey, Map.copyOf(keyed));
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
