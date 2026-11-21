package za.co.neroland.nerotech.machine.tank;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * The {@code nerotech:tank_contents} data component: the share of a broken Process Tank that did not
 * fit back into the rest of its column, carried on the dropped tank item so breaking a tank never
 * loses or creates medium. Placing the item restores the share. Medium id + amount only — no player
 * data (POPIA/GDPR).
 *
 * @param kind   {@code fluid} or {@code gas} ({@link TankMedium.Kind#serialized()})
 * @param medium the fluid or gas registry id
 * @param amount millibuckets
 */
public record TankContents(String kind, String medium, long amount) {

    public static final Codec<TankContents> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.STRING.fieldOf("kind").forGetter(TankContents::kind),
            Codec.STRING.fieldOf("medium").forGetter(TankContents::medium),
            Codec.LONG.fieldOf("amount").forGetter(TankContents::amount)
    ).apply(inst, TankContents::new));

    public TankMedium asMedium() {
        return new TankMedium(TankMedium.Kind.parse(this.kind), this.medium);
    }
}
