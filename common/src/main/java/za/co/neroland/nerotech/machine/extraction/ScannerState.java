package za.co.neroland.nerotech.machine.extraction;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * The Survey Scanner's item data: stored NE and the game time it may scan again. Item data only,
 * no player data.
 */
public record ScannerState(int energy, long readyAt) {

    public static final ScannerState EMPTY = new ScannerState(0, 0L);

    public static final Codec<ScannerState> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("energy", 0).forGetter(ScannerState::energy),
            Codec.LONG.optionalFieldOf("ready_at", 0L).forGetter(ScannerState::readyAt))
            .apply(i, ScannerState::new));

    public ScannerState {
        energy = Math.max(0, energy);
    }

    public ScannerState withEnergy(int value) {
        return new ScannerState(value, this.readyAt);
    }
}
