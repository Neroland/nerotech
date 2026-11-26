package za.co.neroland.nerotech.recipe;

import java.util.function.Predicate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;

//? if >=1.20.5 {
import net.minecraft.network.codec.ByteBufCodecs;
//?}
//? if >=1.20.5 {
import net.minecraft.network.codec.StreamCodec;
//?}

/**
 * A fluid or gas amount in a {@link ProcessRecipe}: {@code {"id": "...", "amount": n}}. Fluids are in
 * millibuckets and may name a tag ({@code "#c:sulfuric_acid"}); gases are in NeroTech units
 * ({@code NeroTechGases.UNIT_MB} mB each) and name one gas id. Pure data plus string matching, so it
 * is testable without a Minecraft bootstrap.
 */
public record ProcessMedium(String id, int amount) {

    public static final Codec<ProcessMedium> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.STRING.fieldOf("id").forGetter(ProcessMedium::id),
            Codec.intRange(1, Integer.MAX_VALUE).fieldOf("amount").forGetter(ProcessMedium::amount)
    ).apply(inst, ProcessMedium::new));

    //? if >=1.20.5 {
    public static final StreamCodec<ByteBuf, ProcessMedium> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, ProcessMedium::id,
            ByteBufCodecs.VAR_INT, ProcessMedium::amount,
            ProcessMedium::new);
    //?} else {
    /*public static final za.co.neroland.nerolandcore.network.legacy.LegacyStreamCodec<net.minecraft.network.FriendlyByteBuf, ProcessMedium> STREAM_CODEC =
            za.co.neroland.nerolandcore.network.legacy.LegacyStreamCodec.of((buffer, medium) -> {
                buffer.writeUtf(medium.id());
                buffer.writeVarInt(medium.amount());
            }, buffer -> new ProcessMedium(buffer.readUtf(), buffer.readVarInt()));
    *///?}

    /** Whether {@link #id} names a tag ({@code #namespace:path}). */
    public boolean isTag() {
        return this.id.startsWith("#");
    }

    /** The id without a leading {@code #}. */
    public String key() {
        return isTag() ? this.id.substring(1) : this.id;
    }

    /**
     * Whether a present medium satisfies this requirement: the id matches (or, for a tag, the tag test
     * accepts it) and at least {@code amount × scale} is present.
     *
     * @param presentId     the medium in the machine, as a registry id string (null or empty for none)
     * @param presentAmount how much of it, in mB
     * @param scale         mB per recipe unit (1 for fluids, {@code UNIT_MB} for gases)
     * @param inTag         tests whether the present id is in a given tag id
     */
    public boolean satisfiedBy(String presentId, long presentAmount, long scale, Predicate<String> inTag) {
        if (presentId == null || presentId.isEmpty()) {
            return false;
        }
        boolean idMatches = isTag() ? inTag.test(key()) : this.id.equals(presentId);
        return idMatches && presentAmount >= (long) this.amount * scale;
    }
}
