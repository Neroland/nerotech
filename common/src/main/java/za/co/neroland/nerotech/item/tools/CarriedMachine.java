package za.co.neroland.nerotech.item.tools;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A machine the Arc Wrench is carrying: its block state and its block entity's own saved data (items,
 * energy, heat, tanks, side configuration, preset). The owner UUID, if the machine had one, is stripped
 * before it is stored, so the item holds no player data; whoever places it back is recorded the usual way
 * (only when attribution or operating charges are switched on).
 */
public record CarriedMachine(BlockState state, CompoundTag data) {

    public static final Codec<CarriedMachine> CODEC = RecordCodecBuilder.create(i -> i.group(
            BlockState.CODEC.fieldOf("state").forGetter(CarriedMachine::state),
            CompoundTag.CODEC.fieldOf("data").forGetter(CarriedMachine::data))
            .apply(i, CarriedMachine::new));
}
