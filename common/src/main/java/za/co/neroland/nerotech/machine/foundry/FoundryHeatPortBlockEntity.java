package za.co.neroland.nerotech.machine.foundry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A Foundry Heat Port's heat buffer. Public surface for heat sources (the Heat Pipe and Heat Exchanger
 * call {@link #acceptHeat(int)} or {@link #offerHeat(Level, BlockPos, int)}); only the Blast Foundry
 * controller drains it. Heat goes in, never back out, so a port can never be used as a heat source.
 * Never ticks; syncs to clients only when its glow bucket changes. World data only, no player data.
 */
public class FoundryHeatPortBlockEntity extends BlockEntity {

    /** Collar glow buckets the art reads ({@link #glowBucket()}); 0 is the "too cold" tell. */
    public static final int GLOW_BUCKETS = 4;

    private int heat;
    private int sentBucket = -1;

    public FoundryHeatPortBlockEntity(BlockPos pos, BlockState state) {
        super(FoundryContent.FOUNDRY_HEAT_PORT_BE.get(), pos, state);
    }

    /**
     * Offer {@code amount} heat to the port at {@code pos}. Returns the heat it took (0 when there is no
     * port there, the buffer is full, or {@code amount} is not positive). Server side only.
     */
    public static int offerHeat(Level level, BlockPos pos, int amount) {
        if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof FoundryHeatPortBlockEntity port)) {
            return 0;
        }
        return port.acceptHeat(amount);
    }

    /** Put up to {@code amount} heat into the buffer; returns what was taken. */
    public int acceptHeat(int amount) {
        int taken = FoundryHeat.portAccept(this.heat, heatCapacity(), amount);
        if (taken > 0) {
            this.heat += taken;
            changed();
        }
        return taken;
    }

    /** Heat in the buffer now. */
    public int heatStored() {
        return this.heat;
    }

    /** Buffer size ({@code foundryHeatPortCapacity}). */
    public int heatCapacity() {
        return FoundrySettings.heatPortCapacity();
    }

    /** Room left, so a source can size its push. */
    public int heatRoom() {
        return Math.max(0, heatCapacity() - this.heat);
    }

    /** Collar glow, {@code 0 .. GLOW_BUCKETS - 1} (client-visible; synced on change). */
    public int glowBucket() {
        return FoundryHeat.bucket(this.heat, heatCapacity(), GLOW_BUCKETS);
    }

    /** Controller only: take up to {@code max} heat out of the buffer. */
    int drainHeat(int max) {
        int taken = Math.max(0, Math.min(max, this.heat));
        if (taken > 0) {
            this.heat -= taken;
            changed();
        }
        return taken;
    }

    private void changed() {
        setChanged();
        int bucket = glowBucket();
        if (bucket != this.sentBucket && this.level != null && !this.level.isClientSide()) {
            this.sentBucket = bucket;
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    //? if >=1.21.6 {
    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
    //?} else {
    /*@Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ValueOutput output = ValueOutput.of(tag, registries);
    *///?}
        output.putInt("Heat", this.heat);
    }

    //? if >=1.21.6 {
    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
    //?} else {
    /*@Override
    protected void loadAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ValueInput input = ValueInput.of(tag, registries);
    *///?}
        this.heat = Math.max(0, Math.min(heatCapacity(), input.getIntOr("Heat", 0)));
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }
}
