package za.co.neroland.nerotech.item.tools;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * A NeroTech tool's item data: stored NE, the game time it may act again and a small mode number (the
 * Power Drill's single / area / vein). Item data only, no player data. Same shape as the Survey
 * Scanner's state so the Induction Charger fills every tool the same way.
 */
public record ToolCharge(int energy, long readyAt, int mode) {

    public static final ToolCharge EMPTY = new ToolCharge(0, 0L, 0);

    public static final Codec<ToolCharge> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("energy", 0).forGetter(ToolCharge::energy),
            Codec.LONG.optionalFieldOf("ready_at", 0L).forGetter(ToolCharge::readyAt),
            Codec.INT.optionalFieldOf("mode", 0).forGetter(ToolCharge::mode))
            .apply(i, ToolCharge::new));

    public ToolCharge {
        energy = Math.max(0, energy);
        mode = Math.max(0, mode);
    }

    public ToolCharge withEnergy(int value) {
        return new ToolCharge(value, this.readyAt, this.mode);
    }

    public ToolCharge withMode(int value) {
        return new ToolCharge(this.energy, this.readyAt, value);
    }
}
